package io.github.nbcss.logisticscontrol.compat.fluid;

import com.yision.fluidlogistics.api.packager.PackageInspection;
import com.yision.fluidlogistics.api.packager.PackageResource;
import com.yision.fluidlogistics.api.packager.PackageResourceType;
import com.yision.fluidlogistics.api.packager.PackageResourceTypes;
import com.yision.fluidlogistics.api.packager.PackageResources;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.FluidStack;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Direct calls into Create: Fluid Logistics' {@code api.packager}, compiled against 1.3.2. Everything used here has the
 * same shape from 1.2.5 on, except how a box with fluid is built, which is found at runtime by asking whether the method
 * exists: {@code setPackageResources} (1.3) is only called in {@link CflApi.Mode#RESOURCE}, and {@code createCarrier}
 * (1.2.5 – 1.2.x, gone in 1.3) only in {@link CflApi.Mode#CARRIER}, through {@link CflCarrier} (compiled against 1.2.9).
 * Only loaded through {@link CflApi}, once CFL's API is known to exist.
 */
final class CflBridge {
    final CflApi.Mode mode;

    CflBridge() {
        mode = hasMethod("setPackageResources", ItemStack.class, List.class) ? CflApi.Mode.RESOURCE
            : hasMethod("createCarrier", ItemStack.class, int.class) ? CflApi.Mode.CARRIER : CflApi.Mode.UNSUPPORTED;
    }

    private static boolean hasMethod(String name, Class<?>... parameters) {
        try {
            PackageResourceType.class.getMethod(name, parameters);
            return true;
        } catch (NoSuchMethodException e) {
            return false;
        }
    }

    FluidStack fluidOf(ItemStack stack) {
        FluidStack fluid = PackageResourceTypes.getFluid(stack);
        return fluid == null ? FluidStack.EMPTY : fluid;
    }

    @Nullable
    CflApi.Inspection inspect(ItemStack box) {
        PackageInspection inspection = PackageResources.inspectPackage(box);
        if (inspection == null) return null;
        ResourceLocation fluidId = PackageResourceTypes.fluid().id();
        List<FluidStack> fluids = new ArrayList<>();
        boolean other = false;
        for (PackageResource res : inspection.resources()) {
            if (!fluidId.equals(res.typeId())) {
                other = true;
                continue;
            }
            FluidStack fluid = PackageResourceTypes.getFluid(res.key());
            if (!fluid.isEmpty()) fluids.add(fluid.copyWithAmount(res.amount()));
        }
        return new CflApi.Inspection(inspection.ordinaryItems(), fluids, other);
    }

    int maxPerPackage(FluidStack fluid) {
        return PackageResourceTypes.fluid().maxPerPackage(PackageResourceTypes.createFluidKey(fluid));
    }

    ItemStack createCarrier(FluidStack fluid) {
        return CflCarrier.create(fluid);
    }

    void setFluids(ItemStack box, List<FluidStack> fluids) {
        PackageResourceType type = PackageResourceTypes.fluid();
        List<PackageResource> resources = new ArrayList<>();
        for (FluidStack fluid : fluids)
            resources.add(new PackageResource(type.id(), PackageResourceTypes.createFluidKey(fluid), fluid.getAmount()));
        type.setPackageResources(box, resources);
    }
}
