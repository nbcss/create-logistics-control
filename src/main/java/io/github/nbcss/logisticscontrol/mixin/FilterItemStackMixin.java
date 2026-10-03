package io.github.nbcss.logisticscontrol.mixin;

import com.simibubi.create.content.logistics.filter.FilterItemStack;
import io.github.nbcss.logisticscontrol.content.item.FilterManifestItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.fluids.FluidStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = FilterItemStack.class, remap = false)
public abstract class FilterItemStackMixin {
    @Shadow @Final private ItemStack filterItemStack;
    @Shadow private boolean fluidExtracted;
    @Shadow private FluidStack filterFluidStack;

    @Inject(method = "resolveFluid", at = @At("HEAD"), cancellable = true)
    private void clc$resolveManifestFluid(Level level, CallbackInfo ci) {
        if (fluidExtracted || !FilterManifestItem.isManifest(filterItemStack)) return;
        fluidExtracted = true;
        filterFluidStack = FilterManifestItem.fluid(filterItemStack);
        ci.cancel();
    }
}
