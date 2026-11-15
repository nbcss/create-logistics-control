package io.github.nbcss.logisticscontrol.order;

import com.simibubi.create.content.logistics.stockTicker.PackageOrderWithCrafts;
import io.github.nbcss.logisticscontrol.filter.Stacks;

import java.util.List;
import java.util.Objects;

/** Whether the order a ticker sent is the one a plan was built for: same address, stacks and crafting entries. */
public final class OrderMatch {
    private OrderMatch() {}

    public static boolean same(PackageOrderWithCrafts a, String addressA, PackageOrderWithCrafts b, String addressB) {
        if (!Objects.equals(addressA, addressB) || !Stacks.sameBig(a.stacks(), b.stacks())) return false;
        List<PackageOrderWithCrafts.CraftingEntry> ca = a.orderedCrafts(), cb = b.orderedCrafts();
        if (ca.size() != cb.size()) return false;
        for (int i = 0; i < ca.size(); i++)
            if (ca.get(i).count() != cb.get(i).count()
                || !Stacks.sameBig(ca.get(i).pattern().stacks(), cb.get(i).pattern().stacks()))
                return false;
        return true;
    }
}
