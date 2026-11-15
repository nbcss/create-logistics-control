package io.github.nbcss.logisticscontrol.order.client;

import com.simibubi.create.content.logistics.BigItemStack;
import com.simibubi.create.content.logistics.stockTicker.CraftableBigItemStack;
import com.simibubi.create.content.logistics.stockTicker.PackageOrderWithCrafts;
import io.github.nbcss.logisticscontrol.filter.FilterPlan;
import io.github.nbcss.logisticscontrol.order.OrderPlanPacket;
import io.github.nbcss.logisticscontrol.order.PlanBuilder;
import net.minecraft.client.Minecraft;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.List;

/** Called by the ticker screens just before they send their order: sends that order's plan first. */
public final class PlanSender {
    private PlanSender() {}

    public static void beforeOrder(PackageOrderWithCrafts order, String address, List<BigItemStack> itemsToOrder,
            List<CraftableBigItemStack> recipesToOrder) {
        FilterPlan plan = PlanBuilder.build(order, itemsToOrder, recipesToOrder, Minecraft.getInstance().level);
        if (plan != null) PacketDistributor.sendToServer(new OrderPlanPacket(order, address, plan));
    }
}
