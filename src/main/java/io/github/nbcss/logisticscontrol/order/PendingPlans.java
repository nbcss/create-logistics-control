package io.github.nbcss.logisticscontrol.order;

import com.mojang.logging.LogUtils;
import com.simibubi.create.content.logistics.stockTicker.PackageOrderWithCrafts;
import io.github.nbcss.logisticscontrol.filter.FilterPlan;
import net.minecraft.server.MinecraftServer;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Plans clients sent for the order they are about to send, until a dispatch claims them. Written on the network thread,
 * claimed on the main thread. One per player; a plan only lives a couple of seconds.
 */
public final class PendingPlans {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final int TTL_TICKS = 40;

    private record Pending(PackageOrderWithCrafts order, String address, FilterPlan plan, int tick) {}

    private static final Map<UUID, Pending> PENDING = new ConcurrentHashMap<>();

    private PendingPlans() {}

    public static void offer(UUID player, PackageOrderWithCrafts order, String address, FilterPlan plan) {
        PENDING.put(player, new Pending(order, address, plan, now()));
    }

    public static void forget(UUID player) {
        PENDING.remove(player);
    }

    /**
     * The plan sent for this order, if exactly one fits: the ordering player's own when known, else the only matching
     * plan (or one of several identical ones). Several different matching plans are dropped: no filter beats a wrong one.
     */
    @Nullable
    public static FilterPlan claim(PackageOrderWithCrafts order, String address, @Nullable UUID player) {
        int now = now();
        PENDING.values().removeIf(p -> now - p.tick() > TTL_TICKS);
        if (PENDING.isEmpty()) return null;
        if (player != null) {
            Pending own = PENDING.get(player);
            if (own != null && matches(own, order, address) && PENDING.remove(player, own)) return own.plan();
        }
        List<Map.Entry<UUID, Pending>> matching = PENDING.entrySet().stream()
            .filter(e -> matches(e.getValue(), order, address))
            .sorted(Comparator.comparingInt(e -> e.getValue().tick()))
            .toList();
        if (matching.isEmpty()) return null;
        FilterPlan plan = matching.getFirst().getValue().plan();
        if (matching.stream().allMatch(e -> e.getValue().plan().equals(plan))) {
            PENDING.remove(matching.getFirst().getKey(), matching.getFirst().getValue());
            return plan;
        }
        matching.forEach(e -> PENDING.remove(e.getKey(), e.getValue()));
        LOGGER.warn("{} identical orders to '{}' sent different filter plans; dispatching them without filters",
            matching.size(), address);
        return null;
    }

    private static boolean matches(Pending pending, PackageOrderWithCrafts order, String address) {
        return OrderMatch.same(pending.order(), pending.address(), order, address);
    }

    private static int now() {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        return server == null ? 0 : server.getTickCount();
    }
}
