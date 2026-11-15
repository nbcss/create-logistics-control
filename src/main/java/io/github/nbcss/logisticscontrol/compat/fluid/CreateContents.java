package io.github.nbcss.logisticscontrol.compat.fluid;

import com.simibubi.create.content.logistics.box.PackageItem;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.items.ItemStackHandler;

import java.util.ArrayList;
import java.util.List;

/** Plain Create packages: items only. Other mods' package items (and their fluid data) are left alone. */
class CreateContents implements PackageContents {

    @Override
    public boolean canRead(ItemStack box) {
        return PackageItem.isPackage(box) && "create".equals(BuiltInRegistries.ITEM.getKey(box.getItem()).getNamespace());
    }

    @Override
    public Contents read(ItemStack box) {
        List<ItemStack> items = new ArrayList<>();
        ItemStackHandler contents = PackageItem.getContents(box);
        for (int slot = 0; slot < contents.getSlots(); slot++) {
            ItemStack stack = contents.getStackInSlot(slot);
            if (!stack.isEmpty()) items.add(stack.copy());
        }
        return new Contents(items, List.of());
    }

    @Override
    public List<ItemStack> build(List<ItemStack> items, List<FluidStack> fluids) {
        if (!fluids.isEmpty()) throw new IllegalStateException("no fluid addon to put fluid in a package");
        return packItems(items).stream().map(PackageItem::containing).toList();
    }

    /** {@code items} (counts may exceed a stack) laid into 9-slot package inventories. */
    static List<ItemStackHandler> packItems(List<ItemStack> items) {
        List<ItemStackHandler> boxes = new ArrayList<>();
        ItemStackHandler box = null;
        int slot = 9;
        for (ItemStack stack : items)
            for (int left = stack.getCount(); left > 0; ) {
                int count = Math.min(left, stack.getMaxStackSize());
                if (slot == 9) {
                    box = new ItemStackHandler(9);
                    boxes.add(box);
                    slot = 0;
                }
                box.setStackInSlot(slot++, stack.copyWithCount(count));
                left -= count;
            }
        return boxes;
    }
}
