package io.github.nbcss.logisticscontrol.filter;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.createmod.catnip.codecs.stream.CatnipStreamCodecBuilders;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * Which filter each part of an order carries. Built once where the order is made (JEI on the client, a Factory Gauge,
 * an API caller) and, for an order with more than one filter, carried on its boxes until a Re-Packager splits them.
 * Boxes may wait in storage for years, so this format must stay readable: add fields as optional, bump
 * {@link #VERSION}, never remove a source type.
 */
public record FilterPlan(int version, List<Pattern> patterns) {
    public static final int VERSION = 1;

    public FilterPlan {
        patterns = List.copyOf(patterns);
    }

    private static final Codec<FilterPlan> RAW_CODEC = RecordCodecBuilder.create(i -> i.group(
        Codec.INT.optionalFieldOf("v", VERSION).forGetter(FilterPlan::version),
        Pattern.CODEC.listOf().fieldOf("patterns").forGetter(FilterPlan::patterns)
    ).apply(i, FilterPlan::new));

    /** A damaged plan (e.g. an item from a removed mod) reads as no plan at all, never as a partial one. */
    public static final Codec<FilterPlan> CODEC = new Codec<>() {
        @Override
        public <T> DataResult<Pair<FilterPlan, T>> decode(DynamicOps<T> ops, T input) {
            DataResult<Pair<FilterPlan, T>> result = RAW_CODEC.decode(ops, input);
            return result.error().isPresent()
                ? DataResult.error(() -> "unreadable filter plan: " + result.error().get().message())
                : result;
        }

        @Override
        public <T> DataResult<T> encode(FilterPlan plan, DynamicOps<T> ops, T prefix) {
            return RAW_CODEC.encode(plan, ops, prefix);
        }
    };

    public static final StreamCodec<RegistryFriendlyByteBuf, FilterPlan> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.VAR_INT, FilterPlan::version,
        CatnipStreamCodecBuilders.list(Pattern.STREAM_CODEC), FilterPlan::patterns,
        FilterPlan::new);

    public static FilterPlan of(List<Pattern> patterns) {
        return new FilterPlan(VERSION, patterns);
    }

    /** The plan of a Factory Gauge or API dispatch: one filter for the whole order. */
    public static FilterPlan single(ItemStack filter) {
        return of(List.of(new Pattern(Labels.normalize(filter), Source.Everything.INSTANCE)));
    }

    /** The distinct non-empty filters, in pattern order. */
    public List<ItemStack> filters() {
        List<ItemStack> filters = new ArrayList<>();
        for (Pattern p : patterns) {
            if (p.filter().isEmpty()) continue;
            if (filters.stream().noneMatch(f -> ItemStack.isSameItemSameComponents(f, p.filter())))
                filters.add(p.filter());
        }
        return filters;
    }

    /** The filter of every package of the order when it has exactly one, else empty. */
    public ItemStack uniformFilter() {
        List<ItemStack> filters = filters();
        return filters.size() == 1 ? filters.getFirst().copy() : ItemStack.EMPTY;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof FilterPlan p && version == p.version && patterns.equals(p.patterns);
    }

    @Override
    public int hashCode() {
        return 31 * version + patterns.hashCode();
    }
}
