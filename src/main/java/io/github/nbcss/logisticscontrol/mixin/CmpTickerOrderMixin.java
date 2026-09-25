package io.github.nbcss.logisticscontrol.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.simibubi.create.content.logistics.packager.IdentifiedInventory;
import com.simibubi.create.content.logistics.packagerLink.LogisticallyLinkedBehaviour.RequestType;
import com.simibubi.create.content.logistics.stockTicker.PackageOrderWithCrafts;
import io.github.nbcss.logisticscontrol.content.helper.FilterDispatch;
import io.github.nbcss.logisticscontrol.content.helper.JeiFilterCarry;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import ru.zznty.create_factory_abstractions.generic.support.GenericOrder;

import java.util.UUID;

/**
 * Add JEI recipe to Create: Mobile Package's stock keeper item requested packages.
 */
@Pseudo
@Mixin(targets = "de.theidler.create_mobile_packages.items.portable_stock_ticker.StockCheckingItem", remap = false)
public abstract class CmpTickerOrderMixin {

    @WrapOperation(method = "broadcastPackageRequest", at = @At(value = "INVOKE",
        target = "Lru/zznty/create_factory_abstractions/generic/support/GenericLogisticsManager;broadcastPackageRequest(Ljava/util/UUID;Lcom/simibubi/create/content/logistics/packagerLink/LogisticallyLinkedBehaviour$RequestType;Lru/zznty/create_factory_abstractions/generic/support/GenericOrder;Lcom/simibubi/create/content/logistics/packager/IdentifiedInventory;Ljava/lang/String;)Z"))
    private boolean clc$stampCmpGenericFilter(UUID freqId, RequestType type, GenericOrder order,
                                              IdentifiedInventory ignored, String address, Operation<Boolean> original) {
        Level level = clc$serverLevel();
        PackageOrderWithCrafts cleaned = level == null ? null : JeiFilterCarry.prepare(order.asCrafting(), level);
        if (cleaned == null)
            return original.call(freqId, type, order, ignored, address);
        try {
            // Keep the generic stacks as-is (they may hold non-item keys); only the crafts carried the sentinels.
            return original.call(freqId, type, new GenericOrder(order.stacks(), cleaned.orderedCrafts()), ignored, address);
        } finally {
            FilterDispatch.clear();
        }
    }

    @WrapOperation(method = "broadcastPackageRequest", at = @At(value = "INVOKE",
        target = "Lcom/simibubi/create/content/logistics/packagerLink/LogisticsManager;broadcastPackageRequest(Ljava/util/UUID;Lcom/simibubi/create/content/logistics/packagerLink/LogisticallyLinkedBehaviour$RequestType;Lcom/simibubi/create/content/logistics/stockTicker/PackageOrderWithCrafts;Lcom/simibubi/create/content/logistics/packager/IdentifiedInventory;Ljava/lang/String;)Z"))
    private boolean clc$stampCmpFluidFilter(UUID freqId, RequestType type, PackageOrderWithCrafts order,
                                            IdentifiedInventory ignored, String address, Operation<Boolean> original) {
        Level level = clc$serverLevel();
        PackageOrderWithCrafts cleaned = level == null ? null : JeiFilterCarry.prepare(order, level);
        if (cleaned == null)
            return original.call(freqId, type, order, ignored, address);
        try {
            return original.call(freqId, type, cleaned, ignored, address);
        } finally {
            FilterDispatch.clear();
        }
    }

    @Unique
    @Nullable
    private static Level clc$serverLevel() {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        return server == null ? null : server.overworld();
    }
}
