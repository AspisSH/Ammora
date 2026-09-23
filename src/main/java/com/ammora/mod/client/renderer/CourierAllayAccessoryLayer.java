package com.ammora.mod.client.renderer;

import com.ammora.mod.AmmoraMod;
import com.ammora.mod.client.model.CourierAllayAccessoryModel;
import com.ammora.mod.client.model.CourierAllayModel;
import com.ammora.mod.entity.CourierAllayEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;

/**
 * Renders the postal cap on the Courier Allay.
 */
public class CourierAllayAccessoryLayer extends RenderLayer<CourierAllayEntity, CourierAllayModel> {

    private static final ResourceLocation ACCESSORY_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(AmmoraMod.MODID, "textures/entity/courier_accessories.png");

    private final CourierAllayAccessoryModel accessoryModel;

    public CourierAllayAccessoryLayer(RenderLayerParent<CourierAllayEntity, CourierAllayModel> renderer, EntityModelSet modelSet) {
        super(renderer);
        this.accessoryModel = new CourierAllayAccessoryModel(modelSet.bakeLayer(CourierAllayAccessoryModel.LAYER_LOCATION));
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource buffer, int packedLight,
                       CourierAllayEntity allay, float limbSwing, float limbSwingAmount,
                       float partialTicks, float ageInTicks, float netHeadYaw, float headPitch) {
        if (allay.isInvisible()) return;

        this.accessoryModel.setupAnim(allay, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);
        VertexConsumer vertexConsumer = buffer.getBuffer(RenderType.entityCutoutNoCull(ACCESSORY_TEXTURE));
        this.accessoryModel.renderToBuffer(poseStack, vertexConsumer, packedLight, OverlayTexture.NO_OVERLAY);
    }
}
