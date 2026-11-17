package io.github.nbcss.logisticscontrol.filter.legacy;

import com.mojang.serialization.Codec;
import com.simibubi.create.content.logistics.BigItemStack;
import io.github.nbcss.logisticscontrol.filter.Stacks;
import net.createmod.catnip.codecs.stream.CatnipStreamCodecBuilders;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;

import java.util.List;

/**
 * Read-only: the 0.1.x carry for processing recipes, one group per recipe laid out as [output, ...ingredient totals]
 * (a fluid as a filter manifest counting its mB). Still on boxes stored by older versions; {@link LegacyPlans} turns it
 * into a plan. Never written again, never unregistered.
 */
public record NonCraftGroups(List<List<BigItemStack>> groups) {
    public static final Codec<NonCraftGroups> CODEC =
        BigItemStack.CODEC.listOf().listOf().xmap(NonCraftGroups::new, NonCraftGroups::groups);

    public static final StreamCodec<RegistryFriendlyByteBuf, NonCraftGroups> STREAM_CODEC =
        CatnipStreamCodecBuilders.list(CatnipStreamCodecBuilders.list(BigItemStack.STREAM_CODEC))
            .map(NonCraftGroups::new, NonCraftGroups::groups);

    // By value, like 0.1.x: boxes of one order must compare (and hash) equal for a Re-Packager to merge them, and
    // BigItemStack hashes its stack by identity.
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof NonCraftGroups other) || groups.size() != other.groups.size()) return false;
        for (int i = 0; i < groups.size(); i++)
            if (!Stacks.sameBig(groups.get(i), other.groups.get(i))) return false;
        return true;
    }

    @Override
    public int hashCode() {
        int h = 1;
        for (List<BigItemStack> group : groups) h = 31 * h + Stacks.hashBig(group);
        return h;
    }
}
