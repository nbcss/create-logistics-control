package io.github.nbcss.logisticscontrol.filter;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.simibubi.create.content.logistics.BigItemStack;
import net.createmod.catnip.codecs.stream.CatnipStreamCodecBuilders;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.neoforged.neoforge.fluids.FluidStack;

import java.util.List;

/** What part of an order a {@link Pattern}'s filter belongs to. */
public sealed interface Source permits Source.Crafting, Source.Ingredients, Source.Everything {

    /** One of the order's crafting entries. Create builds its boxes (each carrying {@code grid}); we only label them. */
    record Crafting(List<BigItemStack> grid, int crafts) implements Source {
        public Crafting {
            grid = Stacks.copyBig(grid);
        }

        static final MapCodec<Crafting> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            BigItemStack.CODEC.listOf().fieldOf("grid").forGetter(Crafting::grid),
            Codec.INT.fieldOf("crafts").forGetter(Crafting::crafts)
        ).apply(i, Crafting::new));

        static final StreamCodec<RegistryFriendlyByteBuf, Crafting> STREAM_CODEC = StreamCodec.composite(
            CatnipStreamCodecBuilders.list(BigItemStack.STREAM_CODEC), Crafting::grid,
            ByteBufCodecs.VAR_INT, Crafting::crafts,
            Crafting::new);

        @Override
        public boolean equals(Object o) {
            return o instanceof Crafting c && crafts == c.crafts && Stacks.sameBig(grid, c.grid);
        }

        @Override
        public int hashCode() {
            return 31 * Stacks.hashBig(grid) + crafts;
        }
    }

    /** A processing recipe's ingredient totals (fluids in mB); the Re-Packager packs them into their own boxes. */
    record Ingredients(List<BigItemStack> items, List<FluidStack> fluids) implements Source {
        public Ingredients {
            items = Stacks.copyBig(items);
            fluids = fluids.stream().map(FluidStack::copy).toList();
        }

        static final MapCodec<Ingredients> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            BigItemStack.CODEC.listOf().fieldOf("items").forGetter(Ingredients::items),
            FluidStack.CODEC.listOf().fieldOf("fluids").forGetter(Ingredients::fluids)
        ).apply(i, Ingredients::new));

        static final StreamCodec<RegistryFriendlyByteBuf, Ingredients> STREAM_CODEC = StreamCodec.composite(
            CatnipStreamCodecBuilders.list(BigItemStack.STREAM_CODEC), Ingredients::items,
            CatnipStreamCodecBuilders.list(FluidStack.STREAM_CODEC), Ingredients::fluids,
            Ingredients::new);

        @Override
        public boolean equals(Object o) {
            return o instanceof Ingredients g && Stacks.sameBig(items, g.items) && Stacks.sameFluids(fluids, g.fluids);
        }

        @Override
        public int hashCode() {
            return 31 * Stacks.hashBig(items) + Stacks.hashFluids(fluids);
        }
    }

    /** The whole order (Factory Gauge, API callers). */
    record Everything() implements Source {
        static final Everything INSTANCE = new Everything();
        static final MapCodec<Everything> CODEC = MapCodec.unit(INSTANCE);
        static final StreamCodec<RegistryFriendlyByteBuf, Everything> STREAM_CODEC = StreamCodec.unit(INSTANCE);
    }

    Codec<Source> CODEC = Codec.STRING
        .validate(t -> switch (t) {
            case "crafting", "ingredients", "everything" -> DataResult.success(t);
            default -> DataResult.error(() -> "unknown filter source: " + t);
        })
        .dispatch("type", Source::typeName, Source::codecFor);

    StreamCodec<RegistryFriendlyByteBuf, Source> STREAM_CODEC = ByteBufCodecs.STRING_UTF8.<RegistryFriendlyByteBuf>cast()
        .dispatch(Source::typeName, Source::streamCodecFor);

    private static String typeName(Source source) {
        return switch (source) {
            case Crafting c -> "crafting";
            case Ingredients g -> "ingredients";
            case Everything e -> "everything";
        };
    }

    private static MapCodec<? extends Source> codecFor(String type) {
        return switch (type) {
            case "crafting" -> Crafting.CODEC;
            case "ingredients" -> Ingredients.CODEC;
            case "everything" -> Everything.CODEC;
            default -> throw new IllegalArgumentException("unknown filter source: " + type);
        };
    }

    private static StreamCodec<RegistryFriendlyByteBuf, ? extends Source> streamCodecFor(String type) {
        return switch (type) {
            case "crafting" -> Crafting.STREAM_CODEC;
            case "ingredients" -> Ingredients.STREAM_CODEC;
            case "everything" -> Everything.STREAM_CODEC;
            default -> throw new IllegalArgumentException("unknown filter source: " + type);
        };
    }
}
