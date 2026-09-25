package io.github.nbcss.logisticscontrol.mixin;

import com.simibubi.create.content.logistics.BigItemStack;
import com.simibubi.create.content.logistics.stockTicker.CraftableBigItemStack;
import com.simibubi.create.content.logistics.stockTicker.PackageOrderWithCrafts;
import io.github.nbcss.logisticscontrol.content.helper.RecipeFilterEncode;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import java.util.List;

/**
 * Add JEI recipe to Create: Phantom's stock keeper item requested packages.
 */
@Pseudo
@Mixin(targets = "com.yision.phantom.item.ticker.TunablePortableTickerScreen", remap = false)
public abstract class PhantomTickerFilterMixin {

    @Shadow public List<BigItemStack> itemsToOrder;
    @Shadow public List<CraftableBigItemStack> recipesToOrder;

    @ModifyVariable(method = "sendIt", at = @At("STORE"), name = "order")
    private PackageOrderWithCrafts clc$encodePhantomFilter(PackageOrderWithCrafts order) {
        return RecipeFilterEncode.encode(order, itemsToOrder, recipesToOrder, Minecraft.getInstance().level);
    }
}
