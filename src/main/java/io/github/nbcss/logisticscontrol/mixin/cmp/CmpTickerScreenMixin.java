package io.github.nbcss.logisticscontrol.mixin.cmp;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.simibubi.create.content.logistics.AddressEditBox;
import com.simibubi.create.content.logistics.BigItemStack;
import com.simibubi.create.content.logistics.stockTicker.CraftableBigItemStack;
import com.simibubi.create.content.logistics.stockTicker.PackageOrderWithCrafts;
import io.github.nbcss.logisticscontrol.order.client.PlanSender;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import ru.zznty.create_factory_abstractions.generic.support.BigGenericStack;
import ru.zznty.create_factory_abstractions.generic.support.CraftableGenericStack;
import ru.zznty.create_factory_abstractions.generic.support.GenericOrder;

import java.util.ArrayList;
import java.util.List;

/**
 * Sends the plan of a JEI order just before Create: Mobile Packages' portable ticker sends it. The order is read where
 * {@code sendIt} converts it for its packet, and passed on unchanged.
 */
@Pseudo
@Mixin(targets = "de.theidler.create_mobile_packages.items.portable_stock_ticker.PortableStockTickerScreen", remap = false)
public abstract class CmpTickerScreenMixin {

    @Shadow public AddressEditBox addressBox;
    @Shadow public List<BigGenericStack> itemsToOrder;
    @Shadow public List<CraftableGenericStack> recipesToOrder;

    @WrapOperation(method = "sendIt", at = @At(value = "INVOKE",
        target = "Lru/zznty/create_factory_abstractions/generic/support/GenericOrder;of(Lcom/simibubi/create/content/logistics/stockTicker/PackageOrderWithCrafts;)Lru/zznty/create_factory_abstractions/generic/support/GenericOrder;"))
    private GenericOrder clc$sendPlan(PackageOrderWithCrafts order, Operation<GenericOrder> original) {
        List<BigItemStack> items = new ArrayList<>();
        for (BigGenericStack s : itemsToOrder) items.add(s.asStack());
        List<CraftableBigItemStack> recipes = new ArrayList<>();
        for (CraftableGenericStack r : recipesToOrder) recipes.add(r.asStack());
        PlanSender.beforeOrder(order, addressBox.getValue(), items, recipes);
        return original.call(order);
    }
}
