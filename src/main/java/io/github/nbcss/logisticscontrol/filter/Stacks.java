package io.github.nbcss.logisticscontrol.filter;

import com.simibubi.create.content.logistics.BigItemStack;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.FluidStack;

import java.util.List;

/** Value equality for the stack lists that plans and orders hold (Create's {@link BigItemStack} has none). */
public final class Stacks {
    private Stacks() {}

    public static boolean same(BigItemStack a, BigItemStack b) {
        return a.count == b.count && ItemStack.isSameItemSameComponents(a.stack, b.stack);
    }

    public static boolean sameBig(List<BigItemStack> a, List<BigItemStack> b) {
        if (a.size() != b.size()) return false;
        for (int i = 0; i < a.size(); i++)
            if (!same(a.get(i), b.get(i))) return false;
        return true;
    }

    public static boolean sameFluids(List<FluidStack> a, List<FluidStack> b) {
        if (a.size() != b.size()) return false;
        for (int i = 0; i < a.size(); i++)
            if (a.get(i).getAmount() != b.get(i).getAmount() || !FluidStack.isSameFluidSameComponents(a.get(i), b.get(i)))
                return false;
        return true;
    }

    public static int hashBig(List<BigItemStack> list) {
        int h = 1;
        for (BigItemStack b : list) h = 31 * (31 * h + ItemStack.hashItemAndComponents(b.stack)) + b.count;
        return h;
    }

    public static int hashFluids(List<FluidStack> list) {
        int h = 1;
        for (FluidStack f : list) h = 31 * (31 * h + FluidStack.hashFluidAndComponents(f)) + f.getAmount();
        return h;
    }

    public static List<BigItemStack> copyBig(List<BigItemStack> list) {
        return list.stream().map(b -> new BigItemStack(b.stack.copy(), b.count)).toList();
    }
}
