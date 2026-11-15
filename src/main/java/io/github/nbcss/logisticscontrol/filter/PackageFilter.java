package io.github.nbcss.logisticscontrol.filter;

import com.mojang.serialization.Codec;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;

/**
 * The filter a package puts on its Filter Link's targets when unpacked. A package without this component clears them;
 * {@link #KEEP} (an empty stack) leaves them alone, and is only put on the leftovers of a per-recipe split.
 */
public record PackageFilter(ItemStack stack) {
    public static final PackageFilter KEEP = new PackageFilter(ItemStack.EMPTY);

    public static final Codec<PackageFilter> CODEC =
        ItemStack.OPTIONAL_CODEC.xmap(PackageFilter::new, PackageFilter::stack);

    public static final StreamCodec<RegistryFriendlyByteBuf, PackageFilter> STREAM_CODEC =
        ItemStack.OPTIONAL_STREAM_CODEC.map(PackageFilter::new, PackageFilter::stack);

    public PackageFilter {
        stack = stack == null || stack.isEmpty() ? ItemStack.EMPTY : stack.copyWithCount(1);
    }

    /** A filter for a non-empty {@code stack}; use {@link #KEEP} for "leave the targets alone". */
    public static PackageFilter of(ItemStack stack) {
        if (stack == null || stack.isEmpty()) throw new IllegalArgumentException("empty filter; use KEEP");
        return new PackageFilter(stack);
    }

    public boolean isKeep() {
        return stack.isEmpty();
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
