package com.ammora.mod.client.renderer;

import com.ammora.mod.AmmoraMod;
import com.ammora.mod.client.model.CourierParrotAccessoryModel;
import com.ammora.mod.client.model.CourierParrotModel;
import com.ammora.mod.entity.CourierParrotEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;

/**
 * Renders the postal uniform cap securely attached to the Courier Parrot's head.
 */
public class CourierParrotAccessoryLayer extends RenderLayer<CourierParrotEntity, CourierParrotModel> {

    private static final ResourceLocation ACCESSORY_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(AmmoraMod.MODID, "textures/entity/courier_accessories.png");

    private final CourierParrotAccessoryModel accessoryModel;

    public CourierParrotAccessoryLayer(RenderLayerParent<CourierParrotEntity, CourierParrotModel> renderer, EntityModelSet modelSet) {
        super(renderer);
        this.accessoryModel = new CourierParrotAccessoryModel(modelSet.bakeLayer(CourierParrotAccessoryModel.LAYER_LOCATION));
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource buffer, int packedLight,
                       CourierParrotEntity parrot, float limbSwing, float limbSwingAmount,
                       float partialTicks, float ageInTicks, float netHeadYaw, float headPitch) {
        if (parrot.isInvisible()) return;

        poseStack.pushPose();

        // Translate and rotate directly with the Parrot's animated head
        ModelPart head = this.getParentModel().getHead();
        head.translateAndRotate(poseStack);

        VertexConsumer vertexConsumer = buffer.getBuffer(RenderType.entityCutoutNoCull(ACCESSORY_TEXTURE));
        this.accessoryModel.renderToBuffer(poseStack, vertexConsumer, packedLight, OverlayTexture.NO_OVERLAY);

        poseStack.popPose();
    }
}
