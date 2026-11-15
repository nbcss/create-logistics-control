package io.github.nbcss.logisticscontrol.compat.fluid;

import com.simibubi.create.content.logistics.box.PackageItem;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/** Create and CFL packages, read through CFL's {@code PackageResources.inspectPackage}. */
abstract class CflContents implements PackageContents {
    protected final CflApi cfl;

    CflContents(CflApi cfl) {
        this.cfl = cfl;
    }

    @Override
    public boolean canRead(ItemStack box) {
        if (!PackageItem.isPackage(box)) return false;
        String namespace = BuiltInRegistries.ITEM.getKey(box.getItem()).getNamespace();
        if (!"create".equals(namespace) && !"fluidlogistics".equals(namespace)) return false;
        CflApi.Inspection inspection = cfl.inspect(box);
        return inspection != null && !inspection.otherResources();
    }

    @Override
    public Contents read(ItemStack box) {
        CflApi.Inspection inspection = cfl.inspect(box);
        if (inspection == null) throw new IllegalStateException("unreadable package");
        return new Contents(inspection.ordinaryItems().stream().map(ItemStack::copy).toList(),
            List.copyOf(inspection.fluids()));
    }
}
