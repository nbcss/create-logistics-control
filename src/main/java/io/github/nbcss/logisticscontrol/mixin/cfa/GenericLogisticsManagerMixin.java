package io.github.nbcss.logisticscontrol.mixin.cfa;

import com.google.common.collect.Multimap;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.simibubi.create.content.logistics.packager.IdentifiedInventory;
import io.github.nbcss.logisticscontrol.order.Dispatch;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import ru.zznty.create_factory_abstractions.generic.support.GenericOrder;
import ru.zznty.create_factory_abstractions.generic.support.GenericPackageTarget;
import ru.zznty.create_factory_abstractions.generic.support.GenericRequest;

import java.util.UUID;

/** Create Factory Abstractions' dispatch picks its own order id; Create: Mobile Packages sends item orders through it. */
@Pseudo
@Mixin(targets = "ru.zznty.create_factory_abstractions.generic.support.GenericLogisticsManager", remap = false)
public abstract class GenericLogisticsManagerMixin {

    @WrapMethod(method = "findPackagersForRequest")
    private static Multimap<GenericPackageTarget, GenericRequest> clc$assignPlan(UUID freqId, GenericOrder order,
            IdentifiedInventory ignoredHandler, String address,
            Operation<Multimap<GenericPackageTarget, GenericRequest>> original) {
        Multimap<GenericPackageTarget, GenericRequest> requests = original.call(freqId, order, ignoredHandler, address);
        Dispatch.onPackagersFound(order.asCrafting(), address,
            requests.values().stream().map(GenericRequest::orderId).distinct().toList());
        return requests;
    }
}
