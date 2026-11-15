package io.github.nbcss.logisticscontrol.order;

import com.simibubi.create.content.logistics.BigItemStack;
import com.simibubi.create.content.logistics.stockTicker.CraftableBigItemStack;
import com.simibubi.create.content.logistics.stockTicker.PackageOrderWithCrafts;
import com.simibubi.create.content.processing.recipe.ProcessingRecipe;
import io.github.nbcss.logisticscontrol.compat.fluid.FluidLabels;
import io.github.nbcss.logisticscontrol.filter.FilterPlan;
import io.github.nbcss.logisticscontrol.filter.Labels;
import io.github.nbcss.logisticscontrol.filter.Pattern;
import io.github.nbcss.logisticscontrol.filter.Source;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.crafting.SizedFluidIngredient;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Builds the {@link FilterPlan} of a JEI order from the recipes the player picked: a pattern per crafting entry of the
 * order (filtered with the recipe that entry crafts) and one per processing recipe (its ingredient totals).
 */
public final class PlanBuilder {
    private PlanBuilder() {}

    /** The plan for {@code order}, or null when the picked recipes give it nothing to filter. */
    @Nullable
    public static FilterPlan build(PackageOrderWithCrafts order, List<BigItemStack> itemsToOrder,
            List<CraftableBigItemStack> recipesToOrder, Level level) {
        if (recipesToOrder == null || recipesToOrder.isEmpty() || level == null) return null;
        List<BigItemStack> items = itemsToOrder == null ? List.of() : itemsToOrder;
        List<Pattern> patterns = new ArrayList<>(craftingPatterns(order.orderedCrafts(), recipesToOrder, level));
        List<BigItemStack> pool = remainingAfterCrafting(items, order.orderedCrafts());
        for (CraftableBigItemStack cbis : recipesToOrder) {
            if (cbis.recipe == null || cbis.recipe instanceof CraftingRecipe) continue;
            if (cbis.stack == null || cbis.stack.isEmpty()) continue;
            int crafts = Math.max(1, cbis.count / Math.max(1, cbis.getOutputCount(level)));
            Source.Ingredients ingredients = allocateIngredients(cbis, crafts, pool, items);
            if (ingredients.items().isEmpty() && ingredients.fluids().isEmpty()) continue;
            patterns.add(new Pattern(Labels.normalize(cbis.stack), ingredients));
        }
        return patterns.isEmpty() ? null : FilterPlan.of(patterns);
    }

    /**
     * A pattern per crafting entry, in order. Create can split one recipe into several entries (a new grid when an
     * ingredient variant runs out), and different recipes can share a grid, so entries are walked together with the
     * crafting recipes, each recipe with a budget of crafts.
     */
    private static List<Pattern> craftingPatterns(List<PackageOrderWithCrafts.CraftingEntry> entries,
            List<CraftableBigItemStack> recipesToOrder, Level level) {
        List<CraftableBigItemStack> crafting = new ArrayList<>();
        for (CraftableBigItemStack cbis : recipesToOrder)
            if (cbis.recipe instanceof CraftingRecipe) crafting.add(cbis);
        int[] remaining = new int[crafting.size()];
        for (int k = 0; k < remaining.length; k++)
            remaining[k] = crafting.get(k).count / Math.max(1, crafting.get(k).getOutputCount(level));

        List<Pattern> patterns = new ArrayList<>(entries.size());
        int current = 0;
        for (PackageOrderWithCrafts.CraftingEntry entry : entries) {
            int match = findRecipe(crafting, remaining, current, entry, level, true);
            if (match < 0) match = findRecipe(crafting, remaining, current, entry, level, false);
            ItemStack filter = ItemStack.EMPTY;   // unattributable: the Re-Packager works it out from the grid
            if (match >= 0) {
                current = match;
                remaining[match] -= entry.count();
                filter = Labels.normalize(crafting.get(match).stack);
            }
            patterns.add(new Pattern(filter, new Source.Crafting(entry.pattern().stacks(), entry.count())));
        }
        return patterns;
    }

    private static int findRecipe(List<CraftableBigItemStack> crafting, int[] remaining, int from,
            PackageOrderWithCrafts.CraftingEntry entry, Level level, boolean withinBudget) {
        for (int k = from; k < crafting.size(); k++)
            if ((!withinBudget || remaining[k] > 0)
                && RecipeMatching.matches((CraftingRecipe) crafting.get(k).recipe, entry.pattern().stacks(), level))
                return k;
        return -1;
    }

    /**
     * Count-aware copies of the ordered items, minus what the crafting entries consume (the Re-Packager builds those
     * boxes first), so processing recipes only claim stacks that are still there, the same way Create's own
     * {@code sendIt} allocates crafting grids from {@link BigItemStack#duplicateWrappers}.
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
     * The recipe's ingredient totals for {@code crafts} crafts, drawn from {@code pool} (which is decremented), so
     * overlapping ingredients and other recipes never claim the same stack twice. Ingredients with the fewest candidates
     * draw first, so a narrow ingredient isn't starved by a broad one. Only what the order carries is claimed, since the
     * Re-Packager can't pack anything else. A processing recipe's fluid ingredients are added in mB.
     */
    private static Source.Ingredients allocateIngredients(CraftableBigItemStack cbis, int crafts,
            List<BigItemStack> pool, List<BigItemStack> items) {
        List<Ingredient> ingredients = new ArrayList<>(cbis.recipe.getIngredients());
        ingredients.removeIf(Ingredient::isEmpty);
        ingredients.sort(Comparator.comparingInt(ing -> candidates(ing, pool)));
        List<BigItemStack> totals = new ArrayList<>();
        for (Ingredient ing : ingredients) {
            int need = crafts;
            for (BigItemStack b : pool) {
                if (need <= 0) break;
                if (b.count <= 0 || b.stack.isEmpty() || !ing.test(b.stack)) continue;
                int take = Math.min(need, b.count);
                b.count -= take;
                need -= take;
                addTotal(totals, b.stack, take);
            }
        }
        List<FluidStack> fluids = new ArrayList<>();
        if (cbis.recipe instanceof ProcessingRecipe<?, ?> processing)
            for (SizedFluidIngredient ing : processing.getFluidIngredients()) {
                FluidStack fluid = resolveFluid(ing, items);
                if (!fluid.isEmpty()) fluids.add(fluid.copyWithAmount(ing.amount() * crafts));
            }
        return new Source.Ingredients(totals, fluids);
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
            FluidStack fluid = FluidLabels.fluidOf(b.stack);
            if (!fluid.isEmpty() && ing.ingredient().test(fluid)) return fluid;
        }
        FluidStack[] fluids = ing.getFluids();
        return fluids.length > 0 ? fluids[0] : FluidStack.EMPTY;
    }
}
