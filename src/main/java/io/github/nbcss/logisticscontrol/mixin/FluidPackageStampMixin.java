package io.github.nbcss.logisticscontrol.mixin;

import io.github.nbcss.logisticscontrol.CreateLogisticsControl;
import io.github.nbcss.logisticscontrol.content.helper.FilterDispatch;
import io.github.nbcss.logisticscontrol.content.helper.PackageFilter;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "com.yision.fluidlogistics.api.packager.PackageResources", remap = false)
public abstract class FluidPackageStampMixin {

    @Inject(method = "createPackage", at = @At("RETURN"))
    private static void clc$stampFilterOnFluidPackage(ItemStack resource, int count,
                                                      CallbackInfoReturnable<ItemStack> cir) {
        ItemStack filter = FilterDispatch.get();
        if (!PackageFilter.canLabel(filter)) return;
        ItemStack pkg = cir.getReturnValue();
        if (pkg == null || pkg.isEmpty()) return;
        pkg.set(CreateLogisticsControl.PACKAGE_FILTER.get(), PackageFilter.of(filter));
    }
}
