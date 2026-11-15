package io.github.nbcss.logisticscontrol.api;

import io.github.nbcss.logisticscontrol.filter.FilterPlan;
import io.github.nbcss.logisticscontrol.order.PlanScope;
import net.minecraft.world.item.ItemStack;

/**
 * Public API of Create: Logistics Control. Lets another mod's request source (e.g. a virtual gauge) participate in the
 * Filter Link — the only integration point Factory Controller needs.
 */
public final class LogisticsControlApi {

    private LogisticsControlApi() {}

    /**
     * Give {@code filter} to every package of the orders dispatched on this thread until {@link #endDispatch()}. MUST be
     * paired with {@link #endDispatch()} in a {@code finally}. Use when the dispatching code can't be a lambda function.
     */
    public static void beginDispatch(ItemStack filter) {
        PlanScope.push(FilterPlan.single(filter));
    }

    public static void endDispatch() {
        PlanScope.pop();
    }

    /** Convenience form of {@link #beginDispatch}/{@link #endDispatch} for a self-contained dispatch. */
    public static void dispatchWithFilter(ItemStack filter, Runnable dispatch) {
        PlanScope.run(FilterPlan.single(filter), dispatch);
    }
}
