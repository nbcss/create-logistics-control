package io.github.nbcss.logisticscontrol.mixin.create;

import com.simibubi.create.content.logistics.packager.repackager.PackageRepackageHelper;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.List;
import java.util.Map;

@Mixin(value = PackageRepackageHelper.class, remap = false)
public interface PackageRepackageHelperAccessor {

    @Accessor("collectedPackages")
    Map<Integer, List<ItemStack>> clc$getCollectedPackages();
}
