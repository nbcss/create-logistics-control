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

    // By value, like 0.1.x: boxes of one order must compare (and hash) equal for a Re-Packager to merge them, and
    // ItemStack has no equals of its own.
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CraftOutputs other) || outputs.size() != other.outputs.size()) return false;
        for (int i = 0; i < outputs.size(); i++)
            if (!ItemStack.isSameItemSameComponents(outputs.get(i), other.outputs.get(i))) return false;
        return true;
    }

    @Override
    public int hashCode() {
        int h = 1;
        for (ItemStack s : outputs) h = 31 * h + ItemStack.hashItemAndComponents(s);
        return h;
    }
}
