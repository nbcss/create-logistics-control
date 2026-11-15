package io.github.nbcss.logisticscontrol.mixin.create;

import com.google.common.collect.Multimap;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.simibubi.create.content.logistics.packager.IdentifiedInventory;
import com.simibubi.create.content.logistics.packager.PackagerBlockEntity;
import com.simibubi.create.content.logistics.packager.PackagingRequest;
import com.simibubi.create.content.logistics.packagerLink.LogisticsManager;
import com.simibubi.create.content.logistics.stockTicker.PackageOrderWithCrafts;
import io.github.nbcss.logisticscontrol.order.Dispatch;
import org.spongepowered.asm.mixin.Mixin;

import java.util.UUID;

/**
 * Every Create dispatch picks its order id here: Stock Keeper and Phantom orders, Factory Gauges (both directly and
 * through {@code broadcastPackageRequest}), Deployer, and Mobile Packages' fluid orders.
 */
@Mixin(value = LogisticsManager.class, remap = false)
public abstract class LogisticsManagerMixin {

    @WrapMethod(method = "findPackagersForRequest")
    private static Multimap<PackagerBlockEntity, PackagingRequest> clc$assignPlan(UUID freqId,
            PackageOrderWithCrafts order, IdentifiedInventory ignoredHandler, String address,
            Operation<Multimap<PackagerBlockEntity, PackagingRequest>> original) {
        Multimap<PackagerBlockEntity, PackagingRequest> requests = original.call(freqId, order, ignoredHandler, address);
        Dispatch.onPackagersFound(order, address,
            requests.values().stream().map(PackagingRequest::orderId).distinct().toList());
        return requests;
    }
}
