package com.ammora.mod.client.renderer;

import com.ammora.mod.AmmoraMod;
import com.ammora.mod.entity.CourierBeeEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.BeeModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

/**
 * Entity renderer for the courier bee and heavy bumblebee with held item and postal accessories.
 */
public class CourierBeeRenderer extends MobRenderer<CourierBeeEntity, BeeModel<CourierBeeEntity>> {

    private static final ResourceLocation BEE_TEXTURE =
            ResourceLocation.withDefaultNamespace("textures/entity/bee/bee.png");

    private static final ResourceLocation BUMBLEBEE_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(AmmoraMod.MODID, "textures/entity/bumblebee.png");

    public CourierBeeRenderer(EntityRendererProvider.Context context) {
        super(context, new BeeModel<>(context.bakeLayer(ModelLayers.BEE)), 0.4F);
        this.addLayer(new CourierBeeHeldItemLayer(this, context.getItemRenderer()));
        this.addLayer(new CourierBeeAccessoryLayer(this, context.getModelSet()));
    }

    @Override
    protected void scale(CourierBeeEntity entity, PoseStack poseStack, float partialTickTime) {
        if (entity.isHeavy()) {
            poseStack.scale(2.75F, 2.75F, 2.75F);
            this.shadowRadius = 1.1F;
        } else {
            this.shadowRadius = 0.4F;
        }
    }

    @Override
    protected void renderNameTag(CourierBeeEntity entity, Component displayName, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, float partialTick) {
        if (entity.isHeavy()) {
            poseStack.pushPose();
            poseStack.translate(0.0D, 0.85D, 0.0D);
            super.renderNameTag(entity, displayName, poseStack, bufferSource, packedLight, partialTick);
            poseStack.popPose();
        } else {
            super.renderNameTag(entity, displayName, poseStack, bufferSource, packedLight, partialTick);
        }
    }

    @Override
    public ResourceLocation getTextureLocation(CourierBeeEntity entity) {
        return entity.isHeavy() ? BUMBLEBEE_TEXTURE : BEE_TEXTURE;
    }
}
