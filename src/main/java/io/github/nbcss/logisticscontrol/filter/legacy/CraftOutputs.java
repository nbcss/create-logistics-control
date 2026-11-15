package io.github.nbcss.logisticscontrol.filter.legacy;

import com.mojang.serialization.Codec;
import net.createmod.catnip.codecs.stream.CatnipStreamCodecBuilders;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/**
 * Read-only: the 0.1.x carry for crafting recipes, one output per crafting entry in {@code orderedCrafts} order. Still on
 * boxes stored by older versions; {@link LegacyPlans} turns it into a plan. Never written again, never unregistered.
 */
public record CraftOutputs(List<ItemStack> outputs) {
    public static final Codec<CraftOutputs> CODEC =
        ItemStack.OPTIONAL_CODEC.listOf().xmap(CraftOutputs::new, CraftOutputs::outputs);

    public static final StreamCodec<RegistryFriendlyByteBuf, CraftOutputs> STREAM_CODEC =
        CatnipStreamCodecBuilders.list(ItemStack.OPTIONAL_STREAM_CODEC).map(CraftOutputs::new, CraftOutputs::outputs);
}
