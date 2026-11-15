package io.github.nbcss.logisticscontrol.order;

import com.simibubi.create.content.logistics.stockTicker.PackageOrderWithCrafts;
import io.github.nbcss.logisticscontrol.CreateLogisticsControl;
import io.github.nbcss.logisticscontrol.filter.FilterPlan;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Sent by the client right before a ticker's own order packet: the plan for that order, and a copy of the order so the
 * server can tell which dispatch it belongs to. The ticker's packet itself is never touched. Handled on the network
 * thread, so it is stored before the order packet (queued for the main thread) is even read.
 */
public record OrderPlanPacket(PackageOrderWithCrafts order, String address, FilterPlan plan)
        implements CustomPacketPayload {
    public static final Type<OrderPlanPacket> TYPE =
        new Type<>(ResourceLocation.fromNamespaceAndPath(CreateLogisticsControl.MODID, "order_plan"));

    public static final StreamCodec<RegistryFriendlyByteBuf, OrderPlanPacket> STREAM_CODEC = StreamCodec.composite(
        PackageOrderWithCrafts.STREAM_CODEC, OrderPlanPacket::order,
        ByteBufCodecs.STRING_UTF8, OrderPlanPacket::address,
        FilterPlan.STREAM_CODEC, OrderPlanPacket::plan,
        OrderPlanPacket::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(OrderPlanPacket packet, IPayloadContext ctx) {
        PendingPlans.offer(ctx.player().getUUID(), packet.order(), packet.address(), packet.plan());
    }
}
