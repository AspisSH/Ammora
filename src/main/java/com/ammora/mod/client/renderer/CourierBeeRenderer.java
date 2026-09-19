package com.ammora.mod.client.renderer;

import com.ammora.mod.entity.CourierBeeEntity;
import net.minecraft.client.model.BeeModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

/**
 * Entity renderer for the courier bee with held item visualization.
 */
public class CourierBeeRenderer extends MobRenderer<CourierBeeEntity, BeeModel<CourierBeeEntity>> {

    private static final ResourceLocation BEE_TEXTURE =
            ResourceLocation.withDefaultNamespace("textures/entity/bee/bee.png");

    public CourierBeeRenderer(EntityRendererProvider.Context context) {
        super(context, new BeeModel<>(context.bakeLayer(ModelLayers.BEE)), 0.4F);
        this.addLayer(new CourierBeeHeldItemLayer(this, context.getItemRenderer()));
    }

    @Override
    public ResourceLocation getTextureLocation(CourierBeeEntity entity) {
        return BEE_TEXTURE;
    }
}
