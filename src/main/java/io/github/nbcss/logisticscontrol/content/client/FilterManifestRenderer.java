package io.github.nbcss.logisticscontrol.content.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.foundation.item.render.CustomRenderedItemModel;
import com.simibubi.create.foundation.item.render.CustomRenderedItemModelRenderer;
import com.simibubi.create.foundation.item.render.PartialItemModelRenderer;
import io.github.nbcss.logisticscontrol.content.item.FilterManifestItem;
import net.createmod.catnip.platform.NeoForgeCatnipServices;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.FluidStack;

public class FilterManifestRenderer extends CustomRenderedItemModelRenderer {
    @Override
    protected void render(ItemStack stack, CustomRenderedItemModel model, PartialItemModelRenderer renderer,
                          ItemDisplayContext context, PoseStack poseStack, MultiBufferSource buffer,
                          int light, int overlay) {
        FluidStack fluid = FilterManifestItem.fluid(stack);
        if (fluid.isEmpty()) {
            renderer.render(model.getOriginalModel(), light);
            return;
        }

        if (context == ItemDisplayContext.FIXED) {
            renderFluidBlock(fluid, poseStack, buffer, light);
            return;
        }

        if (!rendersAsFluidIcon(context)) {
            renderer.render(model.getOriginalModel(), light);
            return;
        }

        float halfSize = 0.5f;
        NeoForgeCatnipServices.FLUID_RENDERER.renderFluidBox(fluid,
            -halfSize, -halfSize, -1 / 32f,
            halfSize, halfSize, 0,
            buffer, poseStack, light, true, false);
    }

    private static void renderFluidBlock(FluidStack fluid, PoseStack poseStack,
                                         MultiBufferSource buffer, int light) {
        poseStack.pushPose();
        try {
            poseStack.scale(1.015625f, 1.015625f, 1.015625f);
            NeoForgeCatnipServices.FLUID_RENDERER.renderFluidBox(fluid,
                -0.5f, -0.5f, -0.5f,
                0.5f, 0.5f, 0.5f,
                buffer, poseStack, light, true, true);
        } finally {
            poseStack.popPose();
        }
    }

    private static boolean rendersAsFluidIcon(ItemDisplayContext context) {
        return context == ItemDisplayContext.GUI
            || context == ItemDisplayContext.GROUND
            || context == ItemDisplayContext.HEAD;
    }
}
