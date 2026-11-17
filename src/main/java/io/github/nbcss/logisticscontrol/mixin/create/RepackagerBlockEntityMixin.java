package io.github.nbcss.logisticscontrol.mixin.create;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.simibubi.create.content.logistics.BigItemStack;
import com.simibubi.create.content.logistics.packager.repackager.PackageRepackageHelper;
import com.simibubi.create.content.logistics.packager.repackager.RepackagerBlockEntity;
import io.github.nbcss.logisticscontrol.packaging.RepackPostProcessor;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.List;

/**
 * Post-processes what the Re-Packager's helper produced for an order, however other mods change it (CFL's own helper up
 * to 1.3.2 and its wrap of {@code repack} after, Deployer's wrap of this same call). Nothing inside the helpers is hooked.
 */
@Mixin(value = RepackagerBlockEntity.class, remap = false)
public abstract class RepackagerBlockEntityMixin {

    @WrapOperation(method = "attemptToRepackage", at = @At(value = "INVOKE",
        target = "Lcom/simibubi/create/content/logistics/packager/repackager/PackageRepackageHelper;repack(ILnet/minecraft/util/RandomSource;)Ljava/util/List;"))
    private List<BigItemStack> clc$postProcess(PackageRepackageHelper helper, int orderId, RandomSource random,
            Operation<List<BigItemStack>> original) {
        List<BigItemStack> outputs = original.call(helper, orderId, random);
        List<ItemStack> inputs = ((PackageRepackageHelperAccessor) helper).clc$getCollectedPackages().get(orderId);
        return RepackPostProcessor.process(outputs, inputs, orderId, ((RepackagerBlockEntity) (Object) this).getLevel());
    }
}
