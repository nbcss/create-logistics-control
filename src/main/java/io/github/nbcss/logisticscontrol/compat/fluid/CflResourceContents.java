package io.github.nbcss.logisticscontrol.compat.fluid;

import com.simibubi.create.content.logistics.box.PackageItem;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.items.ItemStackHandler;

import java.util.ArrayList;
import java.util.List;

/**
 * CFL with {@code setPackageResources}: fluid travels as package data next to the items. Boxes are filled the way CFL's
 * own Re-Packager combines them: at most {@value #MAX_RESOURCES} fluid entries and one package's worth of fluid each.
 */
class CflResourceContents extends CflContents {
    private static final int MAX_RESOURCES = 10;

    CflResourceContents(CflApi cfl) {
        super(cfl);
    }

    @Override
    public List<ItemStack> build(List<ItemStack> items, List<FluidStack> fluids) {
        List<ItemStackHandler> itemBoxes = CreateContents.packItems(items);
        List<List<FluidStack>> fluidBoxes = new ArrayList<>();
        List<Integer> fluidTotals = new ArrayList<>();
        for (FluidStack fluid : fluids) {
            int max = cfl.maxPerPackage(fluid);
            for (int left = fluid.getAmount(); left > 0; ) {
                int last = fluidBoxes.size() - 1;
                if (last < 0 || fluidTotals.get(last) >= max || fluidBoxes.get(last).size() >= MAX_RESOURCES) {
                    fluidBoxes.add(new ArrayList<>());
                    fluidTotals.add(0);
                    last++;
                }
                int amount = Math.min(left, max - fluidTotals.get(last));
                fluidBoxes.get(last).add(fluid.copyWithAmount(amount));
                fluidTotals.set(last, fluidTotals.get(last) + amount);
                left -= amount;
            }
        }
        List<ItemStack> boxes = new ArrayList<>();
        for (int i = 0; i < Math.max(itemBoxes.size(), fluidBoxes.size()); i++) {
            ItemStack box = PackageItem.containing(i < itemBoxes.size() ? itemBoxes.get(i) : new ItemStackHandler(9));
            if (i < fluidBoxes.size()) cfl.setFluids(box, fluidBoxes.get(i));
            boxes.add(box);
        }
        return boxes;
    }
}
