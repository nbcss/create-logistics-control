package io.github.nbcss.logisticscontrol.mixin.create;

import com.llamalad7.mixinextras.sugar.Local;
import com.simibubi.create.content.logistics.stockTicker.PackageOrderWithCrafts;
import com.simibubi.create.content.logistics.stockTicker.StockKeeperRequestScreen;
import io.github.nbcss.logisticscontrol.order.client.PlanSender;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Sends the plan of a JEI order just before the screen builds its order packet. Also covers Create: Phantom's portable
 * ticker (a subclass that keeps {@code sendIt}) and Deployer, which swaps the packet but still builds this one first.
 * The order itself is never changed.
 */
@Mixin(value = StockKeeperRequestScreen.class, remap = false)
public abstract class StockKeeperScreenMixin {

    @Shadow boolean encodeRequester;

    @Inject(method = "sendIt", at = @At(value = "NEW",
        target = "com/simibubi/create/content/logistics/stockTicker/PackageOrderRequestPacket"))
    private void clc$sendPlan(CallbackInfo ci, @Local(name = "order") PackageOrderWithCrafts order) {
        if (encodeRequester) return;
        StockKeeperRequestScreen self = (StockKeeperRequestScreen) (Object) this;
        PlanSender.beforeOrder(order, self.addressBox.getValue(), self.itemsToOrder, self.recipesToOrder);
    }
}
