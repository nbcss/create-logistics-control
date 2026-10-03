package io.github.nbcss.logisticscontrol.content.compat.fluids;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.FluidStack;
import org.jetbrains.annotations.Nullable;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.Map;

public final class FluidCompat {

    private static final Map<ResourceLocation, Reader> READERS = Map.of(
        ResourceLocation.fromNamespaceAndPath("fluidlogistics", "compressed_storage_tank"),
        new Reader("com.yision.fluidlogistics.item.CompressedTankItem", "getFluid"),
        ResourceLocation.fromNamespaceAndPath("fluid", "fluid_manifest"),
        new Reader("com.adonis.fluid.item.FluidManifestItem", "read"));

    private FluidCompat() {}

    public static boolean isVirtualFluid(ItemStack stack) {
        return reader(stack) != null;
    }

    public static FluidStack virtualFluid(ItemStack stack) {
        Reader reader = reader(stack);
        return reader == null ? FluidStack.EMPTY : reader.read(stack);
    }

    @Nullable
    private static Reader reader(ItemStack stack) {
        return stack == null || stack.isEmpty() ? null : READERS.get(BuiltInRegistries.ITEM.getKey(stack.getItem()));
    }

    /** {@code static FluidStack <method>(ItemStack)} on an addon class, resolved on first use. */
    private static final class Reader {
        private final String className;
        private final String methodName;
        private volatile boolean resolved;
        @Nullable private volatile MethodHandle handle;

        Reader(String className, String methodName) {
            this.className = className;
            this.methodName = methodName;
        }

        FluidStack read(ItemStack stack) {
            MethodHandle h = resolve();
            if (h == null) return FluidStack.EMPTY;
            try {
                FluidStack fluid = (FluidStack) h.invoke(stack);
                return fluid == null || fluid.isEmpty() ? FluidStack.EMPTY : fluid.copyWithAmount(1);
            } catch (Throwable t) {
                return FluidStack.EMPTY;
            }
        }

        @Nullable
        private MethodHandle resolve() {
            if (!resolved) {
                try {
                    handle = MethodHandles.publicLookup().findStatic(Class.forName(className), methodName,
                        MethodType.methodType(FluidStack.class, ItemStack.class));
                } catch (ReflectiveOperationException | LinkageError ignored) {
                    handle = null;   // addon API changed: its virtual fluids are treated as unlabelable
                }
                resolved = true;
            }
            return handle;
        }
    }
}
