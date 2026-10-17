package io.github.nbcss.logisticscontrol.content.helper;

import com.simibubi.create.content.logistics.BigItemStack;
import com.simibubi.create.content.logistics.stockTicker.CraftableBigItemStack;
import com.simibubi.create.content.logistics.stockTicker.PackageOrder;
import com.simibubi.create.content.logistics.stockTicker.PackageOrderWithCrafts;
import com.simibubi.create.content.processing.recipe.ProcessingRecipe;
import io.github.nbcss.logisticscontrol.content.compat.fluids.FluidCompat;
import io.github.nbcss.logisticscontrol.content.item.FilterManifestItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.crafting.SizedFluidIngredient;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Carries each JEI recipe's output as its package filter, by appending sentinel data to the order. A fluid output (an
 * addon's virtual fluid item, e.g. a Basin mixing recipe) is carried like any other, as a {@code FilterManifestItem}.
 */
public final class RecipeFilterEncode {
    private RecipeFilterEncode() {}

    public static PackageOrderWithCrafts encode(PackageOrderWithCrafts order, List<BigItemStack> itemsToOrder,
            List<CraftableBigItemStack> recipesToOrder, Level level) {
        if (recipesToOrder == null || recipesToOrder.isEmpty() || level == null) return order;
        List<ItemStack> craftOutputs = craftOutputsPerEntry(order.orderedCrafts(), recipesToOrder, level);
        List<PackageOrderWithCrafts.CraftingEntry> crafts = new ArrayList<>(order.orderedCrafts());
        List<ItemStack> nonCraftOutputs = new ArrayList<>();
        List<BigItemStack> items = itemsToOrder == null ? List.of() : itemsToOrder;
        List<BigItemStack> pool = remainingAfterCrafting(items, order.orderedCrafts());
        for (CraftableBigItemStack cbis : recipesToOrder) {
            if (cbis.recipe == null || cbis.recipe instanceof CraftingRecipe) continue;
            if (cbis.stack == null || cbis.stack.isEmpty()) continue;
            int craftCount = Math.max(1, cbis.count / Math.max(1, cbis.getOutputCount(level)));
            List<BigItemStack> totals = allocateIngredients(cbis, craftCount, pool, items);
            if (totals.isEmpty()) continue;
            // the cells carry this recipe's ingredient totals; the entry count is only a (> 0) placeholder
            crafts.add(new PackageOrderWithCrafts.CraftingEntry(new PackageOrder(totals), 1));
            nonCraftOutputs.add(PackageFilter.normalize(cbis.stack));
        }
        if (nonCraftOutputs.isEmpty() && craftOutputs.isEmpty()) return order;
        PackageOrderWithCrafts result = new PackageOrderWithCrafts(order.orderedStacks(), crafts);
        if (!nonCraftOutputs.isEmpty()) result = FilterOrderCodec.encodeList(result, nonCraftOutputs);
        if (!craftOutputs.isEmpty()) result = FilterOrderCodec.encodeCraftOutputs(result, craftOutputs);
        return result;
    }

    private static List<ItemStack> craftOutputsPerEntry(List<PackageOrderWithCrafts.CraftingEntry> entries,
            List<CraftableBigItemStack> recipesToOrder, Level level) {
        List<CraftableBigItemStack> crafting = new ArrayList<>();
        for (CraftableBigItemStack cbis : recipesToOrder)
            if (cbis.recipe instanceof CraftingRecipe) crafting.add(cbis);
        int[] remaining = new int[crafting.size()];
        for (int k = 0; k < remaining.length; k++)
            remaining[k] = crafting.get(k).count / Math.max(1, crafting.get(k).getOutputCount(level));

        List<ItemStack> outputs = new ArrayList<>(entries.size());
        int current = 0;
        for (PackageOrderWithCrafts.CraftingEntry entry : entries) {
            int match = findRecipe(crafting, remaining, current, entry, level, true);
            if (match < 0) match = findRecipe(crafting, remaining, current, entry, level, false);
            if (match < 0) {
                outputs.add(ItemStack.EMPTY);   // unattributable: the re-packager falls back to deriving it
                continue;
            }
            current = match;
            remaining[match] -= entry.count();
            outputs.add(PackageFilter.normalize(crafting.get(match).stack));
        }
        return outputs;
    }

    private static int findRecipe(List<CraftableBigItemStack> crafting, int[] remaining, int from,
            PackageOrderWithCrafts.CraftingEntry entry, Level level, boolean withinBudget) {
        for (int k = from; k < crafting.size(); k++)
            if ((!withinBudget || remaining[k] > 0)
                && RecipeFilters.matches((CraftingRecipe) crafting.get(k).recipe, entry.pattern().stacks(), level))
                return k;
        return -1;
    }

    /**
     * Count-aware copies of the ordered items, minus what Create's crafting entries consume (the re-packager splits
     * those off first), so non-crafting recipes only claim stacks that are still there, the same way Create's own
     * {@code sendIt} allocates crafting patterns from {@link BigItemStack#duplicateWrappers}.
     */
    private static List<BigItemStack> remainingAfterCrafting(List<BigItemStack> items,
            List<PackageOrderWithCrafts.CraftingEntry> craftingEntries) {
        List<BigItemStack> pool = BigItemStack.duplicateWrappers(items);
        for (PackageOrderWithCrafts.CraftingEntry entry : craftingEntries)
            for (BigItemStack cell : entry.pattern().stacks()) {
                int need = cell.stack.isEmpty() ? 0 : entry.count();
                for (BigItemStack b : pool) {
                    if (need <= 0) break;
                    if (b.count <= 0 || !ItemStack.isSameItemSameComponents(b.stack, cell.stack)) continue;
                    int take = Math.min(need, b.count);
                    b.count -= take;
                    need -= take;
                }
            }
        return pool;
    }

    /**
     * The recipe's ingredient totals for {@code craftCount} crafts, drawn from {@code pool} (which is decremented), so
     * overlapping ingredients and other recipes never claim the same stack twice. Ingredients with the fewest candidates
     * draw first, so a narrow ingredient isn't starved by a broad one. Only what the order carries is claimed, since the
     * re-packager can't bundle anything else. A processing recipe's fluid ingredients follow as manifest cells counting
     * their total mB, so the re-packager bundles the recipe's fluid into the same box as its items.
     */
    private static List<BigItemStack> allocateIngredients(CraftableBigItemStack cbis, int craftCount,
            List<BigItemStack> pool, List<BigItemStack> items) {
        List<Ingredient> ingredients = new ArrayList<>(cbis.recipe.getIngredients());
        ingredients.removeIf(Ingredient::isEmpty);
        ingredients.sort(Comparator.comparingInt(ing -> candidates(ing, pool)));
        List<BigItemStack> totals = new ArrayList<>();
        for (Ingredient ing : ingredients) {
            int need = craftCount;
            for (BigItemStack b : pool) {
                if (need <= 0) break;
                if (b.count <= 0 || b.stack.isEmpty() || !ing.test(b.stack)) continue;
                int take = Math.min(need, b.count);
                b.count -= take;
                need -= take;
                addTotal(totals, b.stack, take);
            }
        }
        if (cbis.recipe instanceof ProcessingRecipe<?, ?> processing)
            for (SizedFluidIngredient ing : processing.getFluidIngredients()) {
                ItemStack manifest = FilterManifestItem.of(resolveFluid(ing, items));
                if (!manifest.isEmpty()) totals.add(new BigItemStack(manifest, ing.amount() * craftCount));
            }
        return totals;
    }

    private static int candidates(Ingredient ing, List<BigItemStack> pool) {
        int n = 0;
        for (BigItemStack b : pool)
            if (b.count > 0 && !b.stack.isEmpty() && ing.test(b.stack)) n++;
        return n;
    }

    private static void addTotal(List<BigItemStack> totals, ItemStack stack, int count) {
        for (BigItemStack t : totals)
            if (ItemStack.isSameItemSameComponents(t.stack, stack)) {
                t.count += count;
                return;
            }
        totals.add(new BigItemStack(stack.copyWithCount(1), count));
    }

    /** The fluid the player already put in the order for {@code ing}, else the ingredient's first fluid. */
    private static FluidStack resolveFluid(SizedFluidIngredient ing, List<BigItemStack> items) {
        for (BigItemStack b : items) {
            FluidStack fluid = FluidCompat.virtualFluid(b.stack);
            if (!fluid.isEmpty() && ing.ingredient().test(fluid)) return fluid;
        }
        FluidStack[] fluids = ing.getFluids();
        return fluids.length > 0 ? fluids[0] : FluidStack.EMPTY;
    }
}
