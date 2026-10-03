package io.github.nbcss.logisticscontrol.content.item;

import io.github.nbcss.logisticscontrol.CreateLogisticsControl;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.SimpleFluidContent;

/**
 * A package's filter label, and the filter it puts on Filter Link targets
 */
public class FilterManifestItem extends Item {
    public FilterManifestItem(Properties properties) {
        super(properties);
    }

    /** A manifest for {@code fluid}'s type (the amount is normalised to 1), or empty for an empty fluid. */
    public static ItemStack of(FluidStack fluid) {
        if (fluid == null || fluid.isEmpty()) return ItemStack.EMPTY;
        ItemStack stack = new ItemStack(CreateLogisticsControl.FILTER_MANIFEST_ITEM.get());
        stack.set(CreateLogisticsControl.FILTER_MANIFEST.get(), SimpleFluidContent.copyOf(fluid.copyWithAmount(1)));
        return stack;
    }

    /** The fluid a manifest stands for, or {@link FluidStack#EMPTY} if {@code stack} is not a manifest. */
    public static FluidStack fluid(ItemStack stack) {
        if (!isManifest(stack)) return FluidStack.EMPTY;
        SimpleFluidContent content = stack.get(CreateLogisticsControl.FILTER_MANIFEST.get());
        return content == null ? FluidStack.EMPTY : content.copy();
    }

    public static boolean isManifest(ItemStack stack) {
        return stack != null && stack.is(CreateLogisticsControl.FILTER_MANIFEST_ITEM.get());
    }

    @Override
    public Component getName(ItemStack stack) {
        FluidStack fluid = fluid(stack);
        return fluid.isEmpty() ? super.getName(stack) : fluid.getHoverName();
    }
}
