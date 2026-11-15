package io.github.nbcss.logisticscontrol.compat.fluid;

import com.simibubi.create.content.logistics.box.PackageItem;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.FluidStack;

import java.util.ArrayList;
import java.util.List;

/** CFL with {@code createCarrier}: fluid travels as tank items inside the box, made with the exact amount needed. */
class CflCarrierContents extends CflContents {

    CflCarrierContents(CflApi cfl) {
        super(cfl);
    }

    @Override
    public List<ItemStack> build(List<ItemStack> items, List<FluidStack> fluids) {
        List<ItemStack> all = new ArrayList<>(items);
        for (FluidStack fluid : fluids) {
            int max = cfl.maxPerPackage(fluid);
            for (int left = fluid.getAmount(); left > 0; ) {
                int amount = Math.min(left, max);
                all.add(cfl.createCarrier(fluid.copyWithAmount(amount)));
                left -= amount;
            }
        }
        return CreateContents.packItems(all).stream().map(PackageItem::containing).toList();
    }
}
