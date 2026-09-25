package io.github.nbcss.logisticscontrol.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.simibubi.create.content.logistics.stockTicker.PackageOrderWithCrafts;
import io.github.nbcss.logisticscontrol.content.helper.FilterDispatch;
import io.github.nbcss.logisticscontrol.content.helper.JeiFilterCarry;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;


/**
 * Add JEI recipe to Create: Phantom's stock keeper item requested packages.
 */
@Pseudo
@Mixin(targets = "com.yision.phantom.network.ticker.TunablePortableTickerSendOrderPacket", remap = false)
public abstract class PhantomTickerOrderMixin {

    @Shadow @Final @Mutable private PackageOrderWithCrafts order;

    @WrapMethod(method = "applySettings")
    private void clc$stampPhantomFilter(ServerPlayer player, Operation<Void> original) {
        PackageOrderWithCrafts cleaned = JeiFilterCarry.prepare(order, player.level());
        if (cleaned == null) {
            original.call(player);
            return;
        }
        order = cleaned;
        try {
            original.call(player);
        } finally {
            FilterDispatch.clear();
        }
    }
}
