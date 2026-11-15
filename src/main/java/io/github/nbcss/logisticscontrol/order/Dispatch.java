package io.github.nbcss.logisticscontrol.order;

import com.simibubi.create.content.logistics.stockTicker.PackageOrderWithCrafts;
import io.github.nbcss.logisticscontrol.ServerConfig;
import io.github.nbcss.logisticscontrol.filter.FilterPlan;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Ties an order's plan to the order ids its packages get. Every dispatch path goes through a
 * {@code findPackagersForRequest} (Create's, or CFA's for Mobile Packages), which picks the order id and hands it to the
 * packagers; the packagers create every package of it in the same tick. The registry is cleared at the end of each tick.
 */
public final class Dispatch {
    private static final Map<Integer, FilterPlan> PLANS = new ConcurrentHashMap<>();

    private Dispatch() {}

    /** Called after {@code findPackagersForRequest} picked {@code orderIds} for {@code order}. */
    public static void onPackagersFound(PackageOrderWithCrafts order, String address, Iterable<Integer> orderIds) {
        if (!ServerConfig.filterLinkEnabled()) return;
        FilterPlan plan = PlanScope.current();
        if (plan == null) plan = PendingPlans.claim(order, address, RequestingPlayer.current());
        if (plan == null || plan.filters().isEmpty()) return;
        for (int orderId : orderIds) PLANS.put(orderId, plan);
    }

    @Nullable
    public static FilterPlan planOf(int orderId) {
        return PLANS.isEmpty() ? null : PLANS.get(orderId);
    }

    public static void endTick() {
        PLANS.clear();
    }
}
