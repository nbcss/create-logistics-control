package io.github.nbcss.logisticscontrol.mixin.create;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.simibubi.create.foundation.networking.BlockEntityConfigurationPacket;
import io.github.nbcss.logisticscontrol.order.RequestingPlayer;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;

/**
 * Marks whose order is being handled, so the dispatch takes that player's plan. Covers Create's Stock Keeper packet,
 * Create: Phantom's portable ticker (it runs this packet inside its envelope) and Deployer's order packet.
 */
@Mixin(value = BlockEntityConfigurationPacket.class, remap = false)
public abstract class BlockEntityConfigurationPacketMixin {

    @WrapMethod(method = "handle")
    private void clc$markRequestingPlayer(ServerPlayer player, Operation<Void> original) {
        RequestingPlayer.during(player, () -> original.call(player));
    }
}
