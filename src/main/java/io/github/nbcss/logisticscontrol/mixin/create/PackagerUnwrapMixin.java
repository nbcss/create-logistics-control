package io.github.nbcss.logisticscontrol.mixin.create;

import com.simibubi.create.content.logistics.packager.PackagerBlockEntity;
import io.github.nbcss.logisticscontrol.link.FilterApplier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Filter Link: apply a package's filter to the link's targets when its packager unwraps it. */
@Mixin(value = PackagerBlockEntity.class, remap = false)
public abstract class PackagerUnwrapMixin {

    @Inject(method = "unwrapBox", at = @At("RETURN"))
    private void clc$applyFilterOnUnwrap(ItemStack box, boolean simulate, CallbackInfoReturnable<Boolean> cir) {
        if (simulate || !Boolean.TRUE.equals(cir.getReturnValue())) return;
        PackagerBlockEntity self = (PackagerBlockEntity) (Object) this;
        Level level = self.getLevel();
        if (level == null || level.isClientSide) return;
        FilterApplier.applyFromBox(self, box);
    }
}
