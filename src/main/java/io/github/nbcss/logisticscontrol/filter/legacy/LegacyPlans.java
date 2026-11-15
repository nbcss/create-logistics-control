package io.github.nbcss.logisticscontrol.filter.legacy;

import com.simibubi.create.content.logistics.BigItemStack;
import com.simibubi.create.content.logistics.stockTicker.PackageOrderWithCrafts;
import io.github.nbcss.logisticscontrol.filter.FilterPlan;
import io.github.nbcss.logisticscontrol.filter.Labels;
import io.github.nbcss.logisticscontrol.filter.Pattern;
import io.github.nbcss.logisticscontrol.filter.Source;
import io.github.nbcss.logisticscontrol.filter.fluid.FilterManifestItem;
import net.neoforged.neoforge.fluids.FluidStack;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/** Turns the 0.1.x carry on stored boxes into a {@link FilterPlan}. Must keep working for as long as such boxes exist. */
public final class LegacyPlans {
    private LegacyPlans() {}

    /**
     * @param context the order's crafting entries (Create's order data on the boxes), which 0.1.x crafting outputs are
     *                positionally aligned with; without it those outputs can't be placed and the grid decides
     * @return the plan, or null when there is nothing to convert
     */
    @Nullable
    public static FilterPlan convert(@Nullable NonCraftGroups groups, @Nullable CraftOutputs craftOutputs,
            @Nullable PackageOrderWithCrafts context) {
        List<Pattern> patterns = new ArrayList<>();
        if (craftOutputs != null && context != null) {
            List<PackageOrderWithCrafts.CraftingEntry> entries = context.orderedCrafts();
            for (int i = 0; i < entries.size() && i < craftOutputs.outputs().size(); i++) {
                PackageOrderWithCrafts.CraftingEntry entry = entries.get(i);
                patterns.add(new Pattern(Labels.normalize(craftOutputs.outputs().get(i)),
                    new Source.Crafting(entry.pattern().stacks(), entry.count())));
            }
        }
        if (groups != null)
            for (List<BigItemStack> group : groups.groups()) {
                if (group.isEmpty()) continue;
                List<BigItemStack> items = new ArrayList<>();
                List<FluidStack> fluids = new ArrayList<>();
                for (int i = 1; i < group.size(); i++) {
                    BigItemStack cell = group.get(i);
                    if (cell.stack.isEmpty() || cell.count <= 0) continue;
                    FluidStack fluid = FilterManifestItem.fluid(cell.stack);
                    if (fluid.isEmpty()) items.add(new BigItemStack(cell.stack.copyWithCount(1), cell.count));
                    else fluids.add(fluid.copyWithAmount(cell.count));
                }
                patterns.add(new Pattern(Labels.normalize(group.getFirst().stack), new Source.Ingredients(items, fluids)));
            }
        return patterns.isEmpty() ? null : FilterPlan.of(patterns);
    }
}
