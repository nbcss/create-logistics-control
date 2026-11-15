package io.github.nbcss.logisticscontrol.mixin.create;

import com.simibubi.create.content.logistics.box.PackageItem;
import com.simibubi.create.content.logistics.stockTicker.PackageOrderWithCrafts;
import io.github.nbcss.logisticscontrol.packaging.PackageStamper;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Every packager gives its packages their order id here: Create's, CFL's fluid packagers and CreateFluid's (which
 * redirect the call but still make it). The body is hooked, not a call site, so redirects elsewhere don't matter.
 */
@Mixin(value = PackageItem.class, remap = false)
public abstract class PackageItemMixin {

    @Inject(method = "setOrder", at = @At("TAIL"))
    private static void clc$stampFilter(ItemStack box, int orderId, int linkIndex, boolean isFinalLink,
            int fragmentIndex, boolean isFinal, PackageOrderWithCrafts orderContext, CallbackInfo ci) {
        PackageStamper.onOrderSet(box, orderId);
    }
}
