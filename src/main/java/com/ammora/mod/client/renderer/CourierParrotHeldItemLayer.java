package com.ammora.mod.client.renderer;

import com.ammora.mod.client.model.CourierParrotModel;
import com.ammora.mod.entity.CourierParrotEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

/**
 * Renders the purchased parcel carried under the Courier Parrot during flight.
 */
public class CourierParrotHeldItemLayer extends RenderLayer<CourierParrotEntity, CourierParrotModel> {

    private final ItemRenderer itemRenderer;

    public CourierParrotHeldItemLayer(RenderLayerParent<CourierParrotEntity, CourierParrotModel> renderer, ItemRenderer itemRenderer) {
        super(renderer);
        this.itemRenderer = itemRenderer;
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource buffer, int packedLight,
                       CourierParrotEntity parrot, float limbSwing, float limbSwingAmount,
                       float partialTicks, float ageInTicks, float netHeadYaw, float headPitch) {
        ItemStack heldItem = parrot.getDeliveredItem();
        if (heldItem.isEmpty()) return;

        poseStack.pushPose();

        // Position parcel under parrot's feet / claws
        poseStack.translate(0.0D, 1.30D, -0.05D);
        poseStack.scale(0.30F, 0.30F, 0.30F);

        // Gentle forward tilt matching flight angle
        poseStack.mulPose(Axis.XP.rotationDegrees(15.0F));

        // Subtle aerial hovering bob
        float bob = Mth.sin((ageInTicks + partialTicks) * 0.25F) * 0.03F;
        poseStack.translate(0.0D, bob, 0.0D);

        this.itemRenderer.renderStatic(
                heldItem,
                ItemDisplayContext.FIXED,
                packedLight,
                OverlayTexture.NO_OVERLAY,
                poseStack,
                buffer,
                parrot.level(),
                parrot.getId()
        );

        poseStack.popPose();
    }
}
