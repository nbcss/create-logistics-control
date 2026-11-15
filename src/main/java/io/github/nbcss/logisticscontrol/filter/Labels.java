package io.github.nbcss.logisticscontrol.filter;

import com.simibubi.create.content.logistics.filter.FilterItem;
import io.github.nbcss.logisticscontrol.compat.fluid.FluidLabels;
import io.github.nbcss.logisticscontrol.filter.fluid.FilterManifestItem;
import net.minecraft.world.item.ItemStack;

/** The single place a filter is made: every filter entering a plan or a package goes through {@link #normalize}. */
public final class Labels {
    private Labels() {}

    /**
     * The filter for an expected output: a single item, with an addon's fluid item converted to a
     * {@link FilterManifestItem}; empty when the output can't be a filter (nothing, a Create filter item, or an unreadable
     * fluid item).
     */
    public static ItemStack normalize(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return ItemStack.EMPTY;
        if (FluidLabels.isFluidItem(stack)) return FilterManifestItem.of(FluidLabels.fluidOf(stack));
        return canLabel(stack) ? stack.copyWithCount(1) : ItemStack.EMPTY;
    }

    public static boolean canLabel(ItemStack stack) {
        return stack != null && !stack.isEmpty() && !(stack.getItem() instanceof FilterItem);
    }
}
