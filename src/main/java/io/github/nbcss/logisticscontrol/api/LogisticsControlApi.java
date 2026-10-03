package io.github.nbcss.logisticscontrol.api;

import io.github.nbcss.logisticscontrol.content.helper.FilterDispatch;
import net.minecraft.world.item.ItemStack;

/**
 * Public API of Create: Logistics Control. Lets another mod's request source (e.g. a virtual gauge) participate in the
 * Filter Link — the only integration point Factory Controller needs.
 */
public final class LogisticsControlApi {

    private LogisticsControlApi() {}

    /**
     * Begin stamping {@code filter} onto every package created on this thread. MUST be paired with {@link #endDispatch()}
     * in a {@code finally}. Use when the dispatching code can't be a lambda function.
     */
    public static void beginDispatch(ItemStack filter) {
        FilterDispatch.set(filter);
    }

    public static void endDispatch() {
        FilterDispatch.clear();
    }

    /** Convenience form of {@link #beginDispatch}/{@link #endDispatch} for a self-contained dispatch. */
    public static void dispatchWithFilter(ItemStack filter, Runnable dispatch) {
        beginDispatch(filter);
        try {
            dispatch.run();
        } finally {
            endDispatch();
        }
    }
}
