package io.github.nbcss.logisticscontrol.filter;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;

/** One filter of an order and the part of the order it applies to. An empty filter means "work it out from the grid". */
public record Pattern(ItemStack filter, Source source) {
    public Pattern {
        filter = filter == null || filter.isEmpty() ? ItemStack.EMPTY : filter.copyWithCount(1);
    }

    public static final Codec<Pattern> CODEC = RecordCodecBuilder.create(i -> i.group(
        ItemStack.OPTIONAL_CODEC.fieldOf("filter").forGetter(Pattern::filter),
        Source.CODEC.fieldOf("source").forGetter(Pattern::source)
    ).apply(i, Pattern::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, Pattern> STREAM_CODEC = StreamCodec.composite(
        ItemStack.OPTIONAL_STREAM_CODEC, Pattern::filter,
        Source.STREAM_CODEC, Pattern::source,
        Pattern::new);

    @Override
    public boolean equals(Object o) {
        return o instanceof Pattern p && ItemStack.isSameItemSameComponents(filter, p.filter) && source.equals(p.source);
    }

    @Override
    public int hashCode() {
        return 31 * ItemStack.hashItemAndComponents(filter) + source.hashCode();
    }
}
