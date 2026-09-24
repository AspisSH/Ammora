package com.ammora.mod.client.renderer;

import com.ammora.mod.client.model.CourierDroneModel;
import com.ammora.mod.entity.CourierDroneEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

/**
 * Renders the delivered item suspended under the drone's magnetic cargo clamp.
 */
public class CourierDroneHeldItemLayer extends RenderLayer<CourierDroneEntity, CourierDroneModel> {

    private final ItemRenderer itemRenderer;

    public CourierDroneHeldItemLayer(RenderLayerParent<CourierDroneEntity, CourierDroneModel> renderer, ItemRenderer itemRenderer) {
        super(renderer);
        this.itemRenderer = itemRenderer;
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource buffer, int packedLight,
                       CourierDroneEntity drone, float limbSwing, float limbSwingAmount,
                       float partialTicks, float ageInTicks, float netHeadYaw, float headPitch) {
        ItemStack heldItem = drone.getDeliveredItem();
        if (heldItem.isEmpty()) return;

        poseStack.pushPose();

        // Follow fuselage orientation and hover
        ModelPart fuselage = this.getParentModel().getFuselage();
        fuselage.translateAndRotate(poseStack);

        // Position under magnetic cargo clamp between skids
        poseStack.translate(0.0D, 0.30D, 0.0D);
        poseStack.scale(0.35F, 0.35F, 0.35F);

        this.itemRenderer.renderStatic(
                heldItem,
                ItemDisplayContext.FIXED,
                packedLight,
                OverlayTexture.NO_OVERLAY,
                poseStack,
                buffer,
                drone.level(),
                drone.getId()
        );

        poseStack.popPose();
    }
}
