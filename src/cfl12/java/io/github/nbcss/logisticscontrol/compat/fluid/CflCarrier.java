package io.github.nbcss.logisticscontrol.compat.fluid;

import com.yision.fluidlogistics.api.packager.PackageResourceTypes;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.FluidStack;

/**
 * Create: Fluid Logistics 1.2.5 – 1.2.x's {@code PackageResourceType.createCarrier}, which 1.3 removed. Compiled against
 * CFL 1.2.9 in the {@code cfl12} source set; only called by {@link CflBridge} when the installed CFL has it.
 */
public final class CflCarrier {
    private CflCarrier() {}

    /** A tank item holding exactly {@code fluid}. */
    public static ItemStack create(FluidStack fluid) {
        return PackageResourceTypes.fluid().createCarrier(PackageResourceTypes.createFluidKey(fluid), fluid.getAmount());
    }
}
