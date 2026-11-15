package io.github.nbcss.logisticscontrol.compat.fluid;

import com.adonis.fluid.item.FluidManifestItem;
import com.yision.fluidlogistics.item.CompressedTankItem;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.FluidStack;

/**
 * Recognises the items addons use to stand for a fluid in orders and JEI (CFL tanks and fluid keys, CreateFluid
 * manifests), so they can become a {@code FilterManifestItem} filter. The amount is not meaningful here. An addon's
 * classes are only touched once one of its items is seen, so the addon must be installed.
 */
public final class FluidLabels {
    private static final String CFL = "fluidlogistics";
    private static final ResourceLocation CFL_TANK = ResourceLocation.fromNamespaceAndPath(CFL, "compressed_storage_tank");
    private static final ResourceLocation CREATE_FLUID_MANIFEST = ResourceLocation.fromNamespaceAndPath("fluid", "fluid_manifest");

    private FluidLabels() {}

    public static boolean isFluidItem(ItemStack stack) {
        return !fluidOf(stack).isEmpty();
    }

    /** The fluid {@code stack} stands for, or empty when it is no addon's fluid item. */
    public static FluidStack fluidOf(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return FluidStack.EMPTY;
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        if (CFL.equals(id.getNamespace())) {
            CflApi cfl = CflApi.get();
            if (cfl.canReadFluids()) return cfl.fluidOf(stack);     // tanks and 1.3 fluid keys
            return id.equals(CFL_TANK) ? legacyTankFluid(stack) : FluidStack.EMPTY;   // no usable API
        }
        return id.equals(CREATE_FLUID_MANIFEST) ? manifestFluid(stack) : FluidStack.EMPTY;
    }

    /** CFL before its packager API: the tank's own reader (same signature from 1.2.0 to 1.3.2). */
    private static FluidStack legacyTankFluid(ItemStack stack) {
        try {
            return nonEmptyCopy(CompressedTankItem.getFluid(stack));
        } catch (RuntimeException | LinkageError e) {
            return FluidStack.EMPTY;   // addon changed: its fluid items are treated as unlabelable
        }
    }

    private static FluidStack manifestFluid(ItemStack stack) {
        try {
            return nonEmptyCopy(FluidManifestItem.read(stack));
        } catch (RuntimeException | LinkageError e) {
            return FluidStack.EMPTY;   // addon changed: its fluid items are treated as unlabelable
        }
    }

    private static FluidStack nonEmptyCopy(FluidStack fluid) {
        return fluid == null || fluid.isEmpty() ? FluidStack.EMPTY : fluid.copy();
    }
}
