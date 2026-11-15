package io.github.nbcss.logisticscontrol.compat.fluid;

import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.FluidStack;

import java.util.List;

/**
 * Reads and builds packages for the Re-Packager's per-recipe split, hiding how the installed fluid addon stores fluid in
 * a box. A box the backend can't fully read is never split: it passes through untouched.
 */
public interface PackageContents {

    record Contents(List<ItemStack> items, List<FluidStack> fluids) {}

    boolean canRead(ItemStack box);

    Contents read(ItemStack box);

    /** Fresh boxes holding exactly {@code items} and {@code fluids} (fluids in mB), without address or order data. */
    List<ItemStack> build(List<ItemStack> items, List<FluidStack> fluids);

    /** The backend for the installed addons, picked once. */
    static PackageContents active() {
        return Holder.ACTIVE;
    }

    final class Holder {
        private static final PackageContents ACTIVE = switch (CflApi.get().mode()) {
            case NONE -> new CreateContents();
            case UNSUPPORTED -> new Unreadable();
            case CARRIER -> new CflCarrierContents(CflApi.get());
            case RESOURCE -> new CflResourceContents(CflApi.get());
        };

        private Holder() {}
    }

    /** A fluid addon we can't read: no package can be safely repacked, so every one passes through untouched. */
    final class Unreadable implements PackageContents {
        @Override
        public boolean canRead(ItemStack box) {
            return false;
        }

        @Override
        public Contents read(ItemStack box) {
            throw new IllegalStateException("unreadable package");
        }

        @Override
        public List<ItemStack> build(List<ItemStack> items, List<FluidStack> fluids) {
            throw new IllegalStateException("cannot build packages");
        }
    }
}
