package io.github.nbcss.logisticscontrol.packaging;

import com.mojang.logging.LogUtils;
import com.simibubi.create.content.logistics.BigItemStack;
import com.simibubi.create.content.logistics.box.PackageItem;
import com.simibubi.create.content.logistics.stockTicker.PackageOrderWithCrafts;
import io.github.nbcss.logisticscontrol.CreateLogisticsControl;
import io.github.nbcss.logisticscontrol.ServerConfig;
import io.github.nbcss.logisticscontrol.compat.fluid.PackageContents;
import io.github.nbcss.logisticscontrol.filter.FilterPlan;
import io.github.nbcss.logisticscontrol.filter.PackageFilter;
import io.github.nbcss.logisticscontrol.filter.Pattern;
import io.github.nbcss.logisticscontrol.filter.Source;
import io.github.nbcss.logisticscontrol.filter.legacy.CraftOutputs;
import io.github.nbcss.logisticscontrol.filter.legacy.LegacyPlans;
import io.github.nbcss.logisticscontrol.filter.legacy.NonCraftGroups;
import io.github.nbcss.logisticscontrol.order.RecipeMatching;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.fluids.FluidStack;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;

/**
 * Runs on whatever a Re-Packager's helper (Create's, CFL's, ...) produced for an order, never inside it. Labels the
 * crafting packages, labels the delivery packages with the order's filter, and, when the order carries a plan (more than
 * one filter), repacks its delivery packages one recipe per box. Our working data is removed from every package that
 * leaves, so delivered packages carry only their filter.
 */
public final class RepackPostProcessor {
    private static final Logger LOGGER = LogUtils.getLogger();

    private RepackPostProcessor() {}

    public static List<BigItemStack> process(List<BigItemStack> outputs, @Nullable List<ItemStack> inputs,
            int orderId, @Nullable Level level) {
        if (outputs == null || outputs.isEmpty()) return outputs;
        List<BigItemStack> result = new ArrayList<>(outputs.size());
        if (!ServerConfig.filterLinkEnabled()) {
            result.addAll(outputs);
            strip(result);
            return result;
        }

        Carried carried = Carried.of(inputs == null ? List.of() : inputs);
        CraftingFilters crafting = new CraftingFilters(carried.plan);
        List<BigItemStack> delivery = new ArrayList<>();
        for (BigItemStack out : outputs) {
            if (out == null || out.stack.isEmpty()) continue;
            List<BigItemStack> grid = craftingGrid(out.stack, orderId);
            if (grid == null) {
                delivery.add(out);
                continue;
            }
            ItemStack filter = carried.plan != null ? crafting.match(grid, out.count) : carried.single;
            if (filter.isEmpty() && level != null) filter = RecipeMatching.craftingResult(grid, level);
            if (!filter.isEmpty()) out.stack.set(CreateLogisticsControl.PACKAGE_FILTER.get(), PackageFilter.of(filter));
            result.add(out);
        }

        if (carried.plan != null) {
            result.addAll(split(delivery, carried.plan, orderId));
        } else {
            if (!carried.single.isEmpty())
                for (BigItemStack out : delivery)
                    if (PackageItem.isPackage(out.stack))
                        out.stack.set(CreateLogisticsControl.PACKAGE_FILTER.get(), PackageFilter.of(carried.single));
            result.addAll(delivery);
        }
        strip(result);
        return result;
    }

    /** What the order's input packages carry: a plan (several filters) or a single filter. */
    private record Carried(@Nullable FilterPlan plan, ItemStack single) {
        static Carried of(List<ItemStack> inputs) {
            FilterPlan plan = null;
            NonCraftGroups groups = null;
            CraftOutputs craftOutputs = null;
            PackageOrderWithCrafts context = null;
            ItemStack single = ItemStack.EMPTY;
            for (ItemStack box : inputs) {
                if (plan == null) plan = box.get(CreateLogisticsControl.FILTER_PLAN.get());
                NonCraftGroups g = box.get(CreateLogisticsControl.NONCRAFT_GROUPS.get());
                if (groups == null && g != null && !g.groups().isEmpty()) groups = g;
                CraftOutputs c = box.get(CreateLogisticsControl.CRAFT_OUTPUTS.get());
                if (craftOutputs == null && c != null && !c.outputs().isEmpty()) craftOutputs = c;
                PackageFilter f = box.get(CreateLogisticsControl.PACKAGE_FILTER.get());
                if (single.isEmpty() && f != null && !f.isKeep()) single = f.stack();
                PackageOrderWithCrafts ctx = PackageItem.getOrderContext(box);
                if (ctx != null && !ctx.isEmpty()) context = ctx;
            }
            if (plan == null) plan = LegacyPlans.convert(groups, craftOutputs, context);   // boxes from 0.1.x
            if (plan != null && plan.filters().size() < 2) {
                if (single.isEmpty()) single = plan.uniformFilter();
                plan = null;
            }
            return new Carried(plan, single);
        }
    }

    /** The grid of a crafting package Create built for this order (it gives each one a fresh order id), else null. */
    @Nullable
    private static List<BigItemStack> craftingGrid(ItemStack box, int orderId) {
        if (!PackageItem.isPackage(box) || PackageItem.getOrderId(box) == orderId) return null;
        PackageOrderWithCrafts context = PackageItem.getOrderContext(box);
        if (context == null || context.orderedCrafts().size() != 1) return null;
        return context.orderedCrafts().getFirst().pattern().stacks();
    }

    /** Hands out the plan's crafting filters by grid, in order, each pattern within its budget of crafts. */
    private static final class CraftingFilters {
        private final List<Pattern> patterns = new ArrayList<>();
        private final List<Integer> remaining = new ArrayList<>();

        CraftingFilters(@Nullable FilterPlan plan) {
            if (plan == null) return;
            for (Pattern p : plan.patterns())
                if (p.source() instanceof Source.Crafting c) {
                    patterns.add(p);
                    remaining.add(c.crafts());
                }
        }

        ItemStack match(List<BigItemStack> grid, int crafts) {
            for (int i = 0; i < patterns.size(); i++)
                if (remaining.get(i) > 0 && sameGrid(((Source.Crafting) patterns.get(i).source()).grid(), grid)) {
                    remaining.set(i, remaining.get(i) - crafts);
                    return patterns.get(i).filter();
                }
            for (Pattern p : patterns)
                if (sameGrid(((Source.Crafting) p.source()).grid(), grid)) return p.filter();
            return ItemStack.EMPTY;
        }

        private static boolean sameGrid(List<BigItemStack> a, List<BigItemStack> b) {
            int size = Math.max(a.size(), b.size());
            for (int i = 0; i < size; i++) {
                ItemStack x = i < a.size() ? a.get(i).stack : ItemStack.EMPTY;
                ItemStack y = i < b.size() ? b.get(i).stack : ItemStack.EMPTY;
                if (!ItemStack.isSameItemSameComponents(x, y)) return false;
            }
            return true;
        }
    }

    /**
     * Repacks the delivery packages one recipe per box ({@code KEEP} on the leftovers), numbered as one order. Packages
     * the backend can't fully read (another mod's package type) pass through untouched. If anything doesn't add up, the
     * helper's packages are delivered as they were, without filters.
     */
    private static List<BigItemStack> split(List<BigItemStack> delivery, FilterPlan plan, int orderId) {
        PackageContents backend = PackageContents.active();
        List<BigItemStack> readable = new ArrayList<>();
        List<BigItemStack> passThrough = new ArrayList<>();
        for (BigItemStack out : delivery)
            (out.count == 1 && backend.canRead(out.stack) ? readable : passThrough).add(out);
        if (readable.isEmpty()) return delivery;

        List<ItemStack> boxes = new ArrayList<>();
        try {
            Pool pool = new Pool();
            for (BigItemStack out : readable) pool.add(backend.read(out.stack));
            Pool before = pool.copy();
            for (Pattern pattern : plan.patterns()) {
                if (!(pattern.source() instanceof Source.Ingredients ingredients)) continue;
                List<ItemStack> items = pool.takeItems(ingredients.items());
                List<FluidStack> fluids = pool.takeFluids(ingredients.fluids());
                if (items.isEmpty() && fluids.isEmpty()) continue;
                PackageFilter filter = pattern.filter().isEmpty() ? PackageFilter.KEEP : PackageFilter.of(pattern.filter());
                for (ItemStack box : backend.build(items, fluids)) {
                    box.set(CreateLogisticsControl.PACKAGE_FILTER.get(), filter);
                    boxes.add(box);
                }
            }
            List<ItemStack> leftItems = pool.items();
            List<FluidStack> leftFluids = pool.fluids();
            if (!leftItems.isEmpty() || !leftFluids.isEmpty())
                for (ItemStack box : backend.build(leftItems, leftFluids)) {
                    box.set(CreateLogisticsControl.PACKAGE_FILTER.get(), PackageFilter.KEEP);
                    boxes.add(box);
                }
            Pool after = new Pool();
            for (ItemStack box : boxes) after.add(backend.read(box));
            if (!after.sameAs(before)) throw new IllegalStateException("contents changed while splitting");
        } catch (RuntimeException e) {
            LOGGER.error("Could not split order {} per recipe; delivering it unfiltered", orderId, e);
            return delivery;
        }

        String address = PackageItem.getAddress(readable.getFirst().stack);
        PackageOrderWithCrafts context = null;
        for (BigItemStack out : readable) {
            PackageOrderWithCrafts c = PackageItem.getOrderContext(out.stack);
            if (c != null) context = c;
        }
        List<BigItemStack> result = new ArrayList<>(boxes.size() + passThrough.size());
        for (int i = 0; i < boxes.size(); i++) {
            ItemStack box = boxes.get(i);
            boolean last = i == boxes.size() - 1;
            PackageItem.addAddress(box, address);
            PackageItem.setOrder(box, orderId, 0, true, i, last, last ? context : null);
            result.add(new BigItemStack(box, 1));
        }
        result.addAll(passThrough);
        return result;
    }

    private static void strip(List<BigItemStack> boxes) {
        for (BigItemStack out : boxes) {
            if (out == null) continue;
            out.stack.remove(CreateLogisticsControl.FILTER_PLAN.get());
            out.stack.remove(CreateLogisticsControl.NONCRAFT_GROUPS.get());
            out.stack.remove(CreateLogisticsControl.CRAFT_OUTPUTS.get());
        }
    }

    /** Pooled package contents: items merged by item and data (counts may exceed a stack), fluids merged in mB. */
    private static final class Pool {
        private final List<ItemStack> items = new ArrayList<>();
        private final List<FluidStack> fluids = new ArrayList<>();

        void add(PackageContents.Contents contents) {
            contents.items().forEach(this::addItem);
            contents.fluids().forEach(this::addFluid);
        }

        private void addItem(ItemStack stack) {
            if (stack.isEmpty()) return;
            for (ItemStack s : items)
                if (ItemStack.isSameItemSameComponents(s, stack)) {
                    s.grow(stack.getCount());
                    return;
                }
            items.add(stack.copy());
        }

        private void addFluid(FluidStack fluid) {
            if (fluid.isEmpty()) return;
            for (FluidStack f : fluids)
                if (FluidStack.isSameFluidSameComponents(f, fluid)) {
                    f.grow(fluid.getAmount());
                    return;
                }
            fluids.add(fluid.copy());
        }

        List<ItemStack> takeItems(List<BigItemStack> wanted) {
            List<ItemStack> taken = new ArrayList<>();
            for (BigItemStack want : wanted)
                for (ItemStack s : items)
                    if (!s.isEmpty() && ItemStack.isSameItemSameComponents(s, want.stack)) {
                        int take = Math.min(want.count, s.getCount());
                        if (take > 0) taken.add(s.split(take));
                        break;
                    }
            items.removeIf(ItemStack::isEmpty);
            return taken;
        }

        List<FluidStack> takeFluids(List<FluidStack> wanted) {
            List<FluidStack> taken = new ArrayList<>();
            for (FluidStack want : wanted)
                for (FluidStack f : fluids)
                    if (!f.isEmpty() && FluidStack.isSameFluidSameComponents(f, want)) {
                        int take = Math.min(want.getAmount(), f.getAmount());
                        if (take > 0) taken.add(f.split(take));
                        break;
                    }
            fluids.removeIf(FluidStack::isEmpty);
            return taken;
        }

        List<ItemStack> items() {
            return items.stream().map(ItemStack::copy).toList();
        }

        List<FluidStack> fluids() {
            return fluids.stream().map(FluidStack::copy).toList();
        }

        Pool copy() {
            Pool copy = new Pool();
            items.forEach(copy::addItem);
            fluids.forEach(copy::addFluid);
            return copy;
        }

        boolean sameAs(Pool other) {
            if (items.size() != other.items.size() || fluids.size() != other.fluids.size()) return false;
            for (ItemStack s : items)
                if (other.items.stream().noneMatch(o -> o.getCount() == s.getCount()
                    && ItemStack.isSameItemSameComponents(o, s)))
                    return false;
            for (FluidStack f : fluids)
                if (other.fluids.stream().noneMatch(o -> o.getAmount() == f.getAmount()
                    && FluidStack.isSameFluidSameComponents(o, f)))
                    return false;
            return true;
        }
    }
}
