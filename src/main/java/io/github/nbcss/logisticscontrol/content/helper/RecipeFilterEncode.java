package io.github.nbcss.logisticscontrol.content.helper;

import com.simibubi.create.content.logistics.BigItemStack;
import com.simibubi.create.content.logistics.stockTicker.CraftableBigItemStack;
import com.simibubi.create.content.logistics.stockTicker.PackageOrder;
import com.simibubi.create.content.logistics.stockTicker.PackageOrderWithCrafts;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
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
        for (CraftableBigItemStack cbis : recipesToOrder) {
            if (cbis.recipe == null || cbis.recipe instanceof CraftingRecipe) continue;
            if (cbis.stack == null || cbis.stack.isEmpty()) continue;
            List<BigItemStack> pattern = resolvePattern(cbis, itemsToOrder);
            if (pattern.stream().allMatch(b -> b.stack.isEmpty())) continue;
            int outputCount = Math.max(1, cbis.getOutputCount(level));
            int count = Math.max(1, cbis.count / outputCount);
            crafts.add(new PackageOrderWithCrafts.CraftingEntry(new PackageOrder(pattern), count));
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

    /** Rebuild a recipe's ingredient pattern, preferring the exact item variant the player already put in the order. */
    private static List<BigItemStack> resolvePattern(CraftableBigItemStack cbis, List<BigItemStack> itemsToOrder) {
        List<BigItemStack> pattern = new ArrayList<>();
        List<BigItemStack> items = itemsToOrder == null ? List.of() : itemsToOrder;
        for (Ingredient ing : cbis.recipe.getIngredients()) {
            ItemStack chosen = ItemStack.EMPTY;
            if (!ing.isEmpty()) {
                for (BigItemStack b : items)
                    if (!b.stack.isEmpty() && ing.test(b.stack)) { chosen = b.stack.copyWithCount(1); break; }
                if (chosen.isEmpty()) {
                    ItemStack[] itemArray = ing.getItems();
                    if (itemArray.length > 0 && !itemArray[0].isEmpty()) chosen = itemArray[0].copyWithCount(1);
                }
            }
            pattern.add(new BigItemStack(chosen));
        }
        return pattern;
    }
}
