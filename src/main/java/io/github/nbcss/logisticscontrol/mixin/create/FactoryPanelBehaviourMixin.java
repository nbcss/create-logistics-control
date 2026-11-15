package io.github.nbcss.logisticscontrol.mixin.create;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.simibubi.create.content.logistics.factoryBoard.FactoryPanelBehaviour;
import io.github.nbcss.logisticscontrol.filter.FilterPlan;
import io.github.nbcss.logisticscontrol.order.PlanScope;
import org.spongepowered.asm.mixin.Mixin;

/** A Factory Gauge's requests carry the gauge's filter. */
@Mixin(value = FactoryPanelBehaviour.class, remap = false)
public abstract class FactoryPanelBehaviourMixin {

    @WrapMethod(method = "tickRequests")
    private void clc$scopePanelFilter(Operation<Void> original) {
        // getFilter() is inherited from FilteringBehaviour, so call it via the (runtime) target rather than @Shadow.
        PlanScope.run(FilterPlan.single(((FactoryPanelBehaviour) (Object) this).getFilter()), original::call);
    }
}
