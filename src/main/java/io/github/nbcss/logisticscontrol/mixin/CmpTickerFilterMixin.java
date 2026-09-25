package io.github.nbcss.logisticscontrol.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.simibubi.create.content.logistics.BigItemStack;
import com.simibubi.create.content.logistics.stockTicker.CraftableBigItemStack;
import com.simibubi.create.content.logistics.stockTicker.PackageOrderWithCrafts;
import io.github.nbcss.logisticscontrol.content.helper.RecipeFilterEncode;
import net.minecraft.client.Minecraft;
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
 * Add JEI recipe to Create: Mobile Package's stock keeper item requested packages.
 */
@Pseudo
@Mixin(targets = "de.theidler.create_mobile_packages.items.portable_stock_ticker.PortableStockTickerScreen", remap = false)
public abstract class CmpTickerFilterMixin {

    @Shadow public List<BigGenericStack> itemsToOrder;
    @Shadow public List<CraftableGenericStack> recipesToOrder;

    @WrapOperation(method = "sendIt", at = @At(value = "INVOKE",
        target = "Lru/zznty/create_factory_abstractions/generic/support/GenericOrder;of(Lcom/simibubi/create/content/logistics/stockTicker/PackageOrderWithCrafts;)Lru/zznty/create_factory_abstractions/generic/support/GenericOrder;"))
    private GenericOrder clc$encodeCmpFilter(PackageOrderWithCrafts order, Operation<GenericOrder> original) {
        List<BigItemStack> items = new ArrayList<>();
        for (BigGenericStack s : itemsToOrder) items.add(s.asStack());
        List<CraftableBigItemStack> recipes = new ArrayList<>();
        for (CraftableGenericStack r : recipesToOrder) recipes.add(r.asStack());
        PackageOrderWithCrafts encoded = RecipeFilterEncode.encode(order, items, recipes, Minecraft.getInstance().level);
        return original.call(encoded);
    }
}
