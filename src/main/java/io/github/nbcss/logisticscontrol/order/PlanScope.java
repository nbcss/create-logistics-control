package io.github.nbcss.logisticscontrol.order;

import io.github.nbcss.logisticscontrol.filter.FilterPlan;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * The plan of a dispatch whose source is on the server (a Factory Gauge, an API caller), active on this thread while the
 * dispatch runs. Nested scopes restore the outer one when they end.
 */
public final class PlanScope {
    private static final ThreadLocal<Deque<FilterPlan>> STACK = ThreadLocal.withInitial(ArrayDeque::new);

    private PlanScope() {}

    public static void push(FilterPlan plan) {
        STACK.get().push(plan);
    }

    public static void pop() {
        STACK.get().poll();
    }

    @Nullable
    public static FilterPlan current() {
        return STACK.get().peek();
    }

    public static void run(FilterPlan plan, Runnable dispatch) {
        push(plan);
        try {
            dispatch.run();
        } finally {
            pop();
        }
    }
}
