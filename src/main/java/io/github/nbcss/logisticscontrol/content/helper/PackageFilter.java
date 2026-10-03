package io.github.nbcss.logisticscontrol.content.helper;

import com.mojang.serialization.Codec;
import com.simibubi.create.content.logistics.filter.FilterItem;
import io.github.nbcss.logisticscontrol.content.compat.fluids.FluidCompat;
import io.github.nbcss.logisticscontrol.content.item.FilterManifestItem;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;

public record PackageFilter(ItemStack stack) {
    public static final Codec<PackageFilter> CODEC =
        ItemStack.CODEC.xmap(PackageFilter::new, PackageFilter::stack);

    public static final StreamCodec<RegistryFriendlyByteBuf, PackageFilter> STREAM_CODEC =
        ItemStack.STREAM_CODEC.map(PackageFilter::new, PackageFilter::stack);

    public static PackageFilter of(ItemStack stack) {
        return new PackageFilter(stack.copyWithCount(1));
    }

    /**
     * The label to carry for an expected output: a single item, with an addon's virtual fluid item converted to this
     * mod's {@link FilterManifestItem}; empty when the output can't be a label (nothing, a Create filter item, or an
     * unreadable virtual fluid). Every label entering the package flow goes through here.
     */
    public static ItemStack normalize(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return ItemStack.EMPTY;
        if (FluidCompat.isVirtualFluid(stack)) return FilterManifestItem.of(FluidCompat.virtualFluid(stack));
        return canLabel(stack) ? stack.copyWithCount(1) : ItemStack.EMPTY;
    }

    public static boolean canLabel(ItemStack stack) {
        return stack != null && !stack.isEmpty() && !(stack.getItem() instanceof FilterItem);
    }

    @Override
    public boolean equals(Object o) {
        return this == o || (o instanceof PackageFilter other && ItemStack.isSameItemSameComponents(stack, other.stack));
    }

    @Override
    public int hashCode() {
        return ItemStack.hashItemAndComponents(stack);
    }
}
