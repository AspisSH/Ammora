package com.ammora.mod.client.renderer;

import com.ammora.mod.AmmoraMod;
import com.ammora.mod.client.model.CourierPhantomAccessoryModel;
import com.ammora.mod.entity.CourierPhantomEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.PhantomModel;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;

/**
 * Renders the postal cap on the Courier Phantom.
 */
public class CourierPhantomAccessoryLayer extends RenderLayer<CourierPhantomEntity, PhantomModel<CourierPhantomEntity>> {

    private static final ResourceLocation ACCESSORY_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(AmmoraMod.MODID, "textures/entity/courier_accessories.png");

    private final CourierPhantomAccessoryModel accessoryModel;

    public CourierPhantomAccessoryLayer(RenderLayerParent<CourierPhantomEntity, PhantomModel<CourierPhantomEntity>> renderer, EntityModelSet modelSet) {
        super(renderer);
        this.accessoryModel = new CourierPhantomAccessoryModel(modelSet.bakeLayer(CourierPhantomAccessoryModel.LAYER_LOCATION));
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource buffer, int packedLight,
                       CourierPhantomEntity phantom, float limbSwing, float limbSwingAmount,
                       float partialTicks, float ageInTicks, float netHeadYaw, float headPitch) {
        if (phantom.isInvisible()) return;

        this.accessoryModel.setupAnim(phantom, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);
        VertexConsumer vertexConsumer = buffer.getBuffer(RenderType.entityCutoutNoCull(ACCESSORY_TEXTURE));
        this.accessoryModel.renderToBuffer(poseStack, vertexConsumer, packedLight, OverlayTexture.NO_OVERLAY);
    }
}
