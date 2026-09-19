package com.ammora.mod.client.renderer;

import com.ammora.mod.entity.CourierBeeEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.model.BeeModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

/**
 * Renders the purchased item physically held under the bee's paws during flight.
 */
public class CourierBeeHeldItemLayer extends RenderLayer<CourierBeeEntity, BeeModel<CourierBeeEntity>> {

    private final ItemRenderer itemRenderer;

    public CourierBeeHeldItemLayer(RenderLayerParent<CourierBeeEntity, BeeModel<CourierBeeEntity>> renderer, ItemRenderer itemRenderer) {
        super(renderer);
        this.itemRenderer = itemRenderer;
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource buffer, int packedLight,
                       CourierBeeEntity bee, float limbSwing, float limbSwingAmount,
                       float partialTicks, float ageInTicks, float netHeadYaw, float headPitch) {
        ItemStack heldItem = bee.getDeliveredItem();
        if (heldItem.isEmpty()) return;

        poseStack.pushPose();

        // Position parcel beneath bee's belly
        poseStack.translate(0.0D, 1.12D, -0.12D);
        poseStack.scale(0.38F, 0.38F, 0.38F);

        // Gentle forward tilt matching flight angle
        poseStack.mulPose(Axis.XP.rotationDegrees(20.0F));

        // Subtle aerial hover bobbing
        float bob = Mth.sin((ageInTicks + partialTicks) * 0.2F) * 0.04F;
        poseStack.translate(0.0D, bob, 0.0D);

        this.itemRenderer.renderStatic(
                heldItem,
                ItemDisplayContext.FIXED,
                packedLight,
                OverlayTexture.NO_OVERLAY,
                poseStack,
                buffer,
                bee.level(),
                bee.getId()
        );

        poseStack.popPose();
    }
}
