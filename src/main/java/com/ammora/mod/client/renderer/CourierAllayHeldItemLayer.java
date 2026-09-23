package com.ammora.mod.client.renderer;

import com.ammora.mod.client.model.CourierAllayModel;
import com.ammora.mod.entity.CourierAllayEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

/**
 * Renders the delivered parcel held securely in the Allay's hands.
 */
public class CourierAllayHeldItemLayer extends RenderLayer<CourierAllayEntity, CourierAllayModel> {

    private final ItemRenderer itemRenderer;

    public CourierAllayHeldItemLayer(RenderLayerParent<CourierAllayEntity, CourierAllayModel> renderer, ItemRenderer itemRenderer) {
        super(renderer);
        this.itemRenderer = itemRenderer;
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource buffer, int packedLight,
                       CourierAllayEntity allay, float limbSwing, float limbSwingAmount,
                       float partialTicks, float ageInTicks, float netHeadYaw, float headPitch) {
        ItemStack heldItem = allay.getDeliveredItem();
        if (heldItem.isEmpty()) return;

        poseStack.pushPose();

        // Translate and rotate with Allay's root and body so parcel follows every motion
        ModelPart allayRoot = this.getParentModel().root().getChild("root");
        allayRoot.translateAndRotate(poseStack);
        ModelPart body = allayRoot.getChild("body");
        body.translateAndRotate(poseStack);

        // Position item directly in the Allay's hands in front of chest
        poseStack.translate(0.0D, 0.14D, -0.18D);
        poseStack.scale(0.38F, 0.38F, 0.38F);

        // Gentle tilt so item rests flat across palms
        poseStack.mulPose(Axis.XP.rotationDegrees(20.0F));

        this.itemRenderer.renderStatic(
                heldItem,
                ItemDisplayContext.FIXED,
                packedLight,
                OverlayTexture.NO_OVERLAY,
                poseStack,
                buffer,
                allay.level(),
                allay.getId()
        );

        poseStack.popPose();
    }
}
