package io.github.nbcss.logisticscontrol;

import io.github.nbcss.logisticscontrol.link.network.FilterLinkConfigurePacket;
import io.github.nbcss.logisticscontrol.link.network.FilterLinkPlacedPacket;
import io.github.nbcss.logisticscontrol.order.OrderPlanPacket;
import net.neoforged.neoforge.network.registration.HandlerThread;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public final class NetworkHandler {

    private NetworkHandler() {}

    public static void register(PayloadRegistrar registrar) {
        registrar.playToServer(FilterLinkConfigurePacket.TYPE, FilterLinkConfigurePacket.STREAM_CODEC,
            FilterLinkConfigurePacket::handle);
        registrar.playToClient(FilterLinkPlacedPacket.TYPE, FilterLinkPlacedPacket.STREAM_CODEC,
            FilterLinkPlacedPacket::handle);
        // On the network thread: stored before the order packet that follows it reaches the main thread.
        registrar.executesOn(HandlerThread.NETWORK).playToServer(OrderPlanPacket.TYPE, OrderPlanPacket.STREAM_CODEC,
            OrderPlanPacket::handle);
    }
}
