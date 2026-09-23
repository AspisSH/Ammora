package com.ammora.mod.client.renderer;

import com.ammora.mod.entity.CourierPhantomEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.PhantomModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

/**
 * Renders the parcel held securely beneath the Phantom courier during flight.
 */
public class CourierPhantomHeldItemLayer extends RenderLayer<CourierPhantomEntity, PhantomModel<CourierPhantomEntity>> {

    private final ItemRenderer itemRenderer;

    public CourierPhantomHeldItemLayer(RenderLayerParent<CourierPhantomEntity, PhantomModel<CourierPhantomEntity>> renderer, ItemRenderer itemRenderer) {
        super(renderer);
        this.itemRenderer = itemRenderer;
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource buffer, int packedLight,
                       CourierPhantomEntity phantom, float limbSwing, float limbSwingAmount,
                       float partialTicks, float ageInTicks, float netHeadYaw, float headPitch) {
        ItemStack heldItem = phantom.getDeliveredItem();
        if (heldItem.isEmpty()) return;

        poseStack.pushPose();

        // Position parcel beneath phantom body
        poseStack.translate(0.0D, 0.48D, 0.05D);
        poseStack.scale(0.36F, 0.36F, 0.36F);

        float bob = Mth.sin((ageInTicks + partialTicks) * 0.2F) * 0.04F;
        poseStack.translate(0.0D, bob, 0.0D);

        this.itemRenderer.renderStatic(
                heldItem,
                ItemDisplayContext.FIXED,
                packedLight,
                OverlayTexture.NO_OVERLAY,
                poseStack,
                buffer,
                phantom.level(),
                phantom.getId()
        );

        poseStack.popPose();
    }
}
