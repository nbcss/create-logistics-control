package io.github.nbcss.logisticscontrol.packaging;

import io.github.nbcss.logisticscontrol.CreateLogisticsControl;
import io.github.nbcss.logisticscontrol.filter.FilterPlan;
import io.github.nbcss.logisticscontrol.filter.PackageFilter;
import io.github.nbcss.logisticscontrol.order.Dispatch;
import net.minecraft.world.item.ItemStack;

/**
 * Puts an order's filter data on each of its packages as a packager gives it the order id: the filter itself when the
 * order has one, the whole plan when it has several (a Re-Packager splits those). Item and fluid packages of an order
 * always get identical data, which is what lets CFL's Re-Packager merge them.
 */
public final class PackageStamper {
    private PackageStamper() {}

    public static void onOrderSet(ItemStack box, int orderId) {
        FilterPlan plan = Dispatch.planOf(orderId);
        if (plan == null) return;
        ItemStack filter = plan.uniformFilter();
        if (!filter.isEmpty()) {
            box.set(CreateLogisticsControl.PACKAGE_FILTER.get(), PackageFilter.of(filter));
            box.remove(CreateLogisticsControl.FILTER_PLAN.get());
        } else {
            box.remove(CreateLogisticsControl.PACKAGE_FILTER.get());
            box.set(CreateLogisticsControl.FILTER_PLAN.get(), plan);
        }
    }
}
