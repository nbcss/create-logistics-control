package io.github.nbcss.logisticscontrol.order;

import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * The player whose order packet is being handled on this thread, when known (Create's block-entity configuration
 * packets: Stock Keeper, Phantom's envelope, Deployer). Lets a dispatch take that player's plan without guessing.
 */
public final class RequestingPlayer {
    private static final ThreadLocal<UUID> CURRENT = new ThreadLocal<>();

    private RequestingPlayer() {}

    public static void during(@Nullable ServerPlayer player, Runnable handle) {
        UUID previous = CURRENT.get();
        CURRENT.set(player == null ? null : player.getUUID());
        try {
            handle.run();
        } finally {
            CURRENT.set(previous);
        }
    }

    @Nullable
    public static UUID current() {
        return CURRENT.get();
    }
}
