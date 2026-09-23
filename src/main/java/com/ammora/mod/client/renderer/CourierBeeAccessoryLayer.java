package com.ammora.mod.client.renderer;

import com.ammora.mod.AmmoraMod;
import com.ammora.mod.client.model.CourierBeeAccessoryModel;
import com.ammora.mod.entity.CourierBeeEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.BeeModel;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;

/**
 * Renders the postal cap and mail satchel on the courier bee.
 */
public class CourierBeeAccessoryLayer extends RenderLayer<CourierBeeEntity, BeeModel<CourierBeeEntity>> {

    private static final ResourceLocation ACCESSORY_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(AmmoraMod.MODID, "textures/entity/courier_accessories.png");

    private final CourierBeeAccessoryModel accessoryModel;

    public CourierBeeAccessoryLayer(RenderLayerParent<CourierBeeEntity, BeeModel<CourierBeeEntity>> renderer, EntityModelSet modelSet) {
        super(renderer);
        this.accessoryModel = new CourierBeeAccessoryModel(modelSet.bakeLayer(CourierBeeAccessoryModel.LAYER_LOCATION));
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource buffer, int packedLight,
                       CourierBeeEntity bee, float limbSwing, float limbSwingAmount,
                       float partialTicks, float ageInTicks, float netHeadYaw, float headPitch) {
        if (bee.isInvisible()) return;

        this.accessoryModel.setupAnim(bee, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);
        VertexConsumer vertexConsumer = buffer.getBuffer(RenderType.entityCutoutNoCull(ACCESSORY_TEXTURE));
        this.accessoryModel.renderToBuffer(poseStack, vertexConsumer, packedLight, OverlayTexture.NO_OVERLAY);
    }
}
