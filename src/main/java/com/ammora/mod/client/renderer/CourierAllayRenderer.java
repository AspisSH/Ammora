package com.ammora.mod.client.renderer;

import com.ammora.mod.client.model.CourierAllayModel;
import com.ammora.mod.entity.CourierAllayEntity;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;

/**
 * Entity renderer for the Courier Allay holding the parcel directly in its hands.
 */
public class CourierAllayRenderer extends MobRenderer<CourierAllayEntity, CourierAllayModel> {

    private static final ResourceLocation ALLAY_TEXTURE =
            ResourceLocation.withDefaultNamespace("textures/entity/allay/allay.png");

    public CourierAllayRenderer(EntityRendererProvider.Context context) {
        super(context, new CourierAllayModel(context.bakeLayer(ModelLayers.ALLAY)), 0.4F);
        this.addLayer(new CourierAllayHeldItemLayer(this, context.getItemRenderer()));
        this.addLayer(new CourierAllayAccessoryLayer(this, context.getModelSet()));
    }

    @Override
    public ResourceLocation getTextureLocation(CourierAllayEntity entity) {
        return ALLAY_TEXTURE;
    }

    @Override
    protected int getBlockLightLevel(CourierAllayEntity entity, BlockPos pos) {
        return 15;
    }
}
