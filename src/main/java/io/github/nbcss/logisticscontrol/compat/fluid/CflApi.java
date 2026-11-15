package io.github.nbcss.logisticscontrol.compat.fluid;

import com.mojang.logging.LogUtils;
import net.minecraft.world.item.ItemStack;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.fluids.FluidStack;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Create: Fluid Logistics' public {@code api.packager} package, called through {@link CflBridge} (compiled against it).
 * Which way a box with fluid is built is detected from the API itself, not the version number:
 * {@code PackageResourceType.createCarrier} (fluid as tank items in the box, 1.2.5 – 1.2.x) or
 * {@code setPackageResources} (fluid as package data, from the 1.3 line). CFL before 1.2.5 has no such API.
 * <p>
 * Every call is guarded: if a later CFL changes a method we use, reads come back empty (so its packages pass through
 * untouched) and builds throw {@link IllegalStateException} (so the Re-Packager split is abandoned), never a crash.
 */
public final class CflApi {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String API_PROBE = "com.yision.fluidlogistics.api.packager.PackageResourceTypes";

    /**
     * {@code NONE}: no CFL, or one from before the API (its fluid is plain tank items in a box). {@code UNSUPPORTED}: a
     * CFL whose API we can't use, so we can't tell where its fluid is in a box and must not repack its packages.
     */
    public enum Mode { NONE, UNSUPPORTED, CARRIER, RESOURCE }

    /** A fluid in a package, as CFL reports it. */
    public record Inspection(List<ItemStack> ordinaryItems, List<FluidStack> fluids, boolean otherResources) {}

    private static final CflApi INSTANCE = bind();

    private final Mode mode;
    @Nullable private final CflBridge bridge;
    private final AtomicBoolean warned = new AtomicBoolean();

    private CflApi(Mode mode, @Nullable CflBridge bridge) {
        this.mode = mode;
        this.bridge = bridge;
    }

    public static CflApi get() {
        return INSTANCE;
    }

    public Mode mode() {
        return mode;
    }

    private static CflApi bind() {
        if (!ModList.get().isLoaded("fluidlogistics")) return new CflApi(Mode.NONE, null);
        try {
            Class.forName(API_PROBE);
        } catch (ClassNotFoundException | LinkageError e) {
            LOGGER.info("Create: Fluid Logistics predates its packager API; its tanks are handled as plain items");
            return new CflApi(Mode.NONE, null);
        }
        try {
            CflBridge bridge = new CflBridge();
            if (bridge.mode == Mode.UNSUPPORTED)
                LOGGER.warn("Create: Fluid Logistics packager API has no known way to build fluid packages; orders "
                    + "carrying fluid are not repacked per recipe");
            else
                LOGGER.info("Create: Fluid Logistics packager API bound, fluid boxes built as {}", bridge.mode);
            return new CflApi(bridge.mode, bridge);
        } catch (RuntimeException | LinkageError e) {
            LOGGER.warn("Create: Fluid Logistics packager API changed ({}); orders are not repacked per recipe",
                e.toString());
            return new CflApi(Mode.UNSUPPORTED, null);
        }
    }

    /** Whether the fluid readers are bound (tanks and fluid keys can be read through the API). */
    public boolean canReadFluids() {
        return bridge != null;
    }

    /** The fluid a CFL tank or fluid key stands for (empty if it is neither, or the API is missing). */
    public FluidStack fluidOf(ItemStack stack) {
        if (bridge == null) return FluidStack.EMPTY;
        try {
            return bridge.fluidOf(stack);
        } catch (RuntimeException | LinkageError e) {
            warnOnce(e);
            return FluidStack.EMPTY;
        }
    }

    /** What a package holds, or null if CFL can't read it. */
    @Nullable
    public Inspection inspect(ItemStack box) {
        if (bridge == null) return null;
        try {
            return bridge.inspect(box);
        } catch (RuntimeException | LinkageError e) {
            warnOnce(e);
            return null;
        }
    }

    /** The most fluid one package (or tank) may hold. */
    public int maxPerPackage(FluidStack fluid) {
        try {
            return bound().maxPerPackage(fluid);
        } catch (RuntimeException | LinkageError e) {
            throw new IllegalStateException("CFL maxPerPackage failed", e);
        }
    }

    /** {@link Mode#CARRIER}: a tank item holding exactly {@code fluid}. */
    public ItemStack createCarrier(FluidStack fluid) {
        try {
            return bound().createCarrier(fluid);
        } catch (RuntimeException | LinkageError e) {
            throw new IllegalStateException("CFL createCarrier failed", e);
        }
    }

    /** {@link Mode#RESOURCE}: put {@code fluids} into {@code box} as package data. */
    public void setFluids(ItemStack box, List<FluidStack> fluids) {
        try {
            bound().setFluids(box, fluids);
        } catch (RuntimeException | LinkageError e) {
            throw new IllegalStateException("CFL setPackageResources failed", e);
        }
    }

    private CflBridge bound() {
        if (bridge == null) throw new IllegalStateException("CFL packager API not bound");
        return bridge;
    }

    private void warnOnce(Throwable t) {
        if (warned.compareAndSet(false, true))
            LOGGER.warn("Create: Fluid Logistics packager API call failed; its packages are left as they are", t);
    }
}
