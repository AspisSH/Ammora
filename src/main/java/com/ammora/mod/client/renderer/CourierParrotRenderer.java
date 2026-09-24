package com.ammora.mod.client.renderer;

import com.ammora.mod.client.model.CourierParrotModel;
import com.ammora.mod.entity.CourierParrotEntity;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.ParrotRenderer;
import net.minecraft.resources.ResourceLocation;

/**
 * Entity renderer for the Courier Parrot with held item and postal uniform cap.
 */
public class CourierParrotRenderer extends MobRenderer<CourierParrotEntity, CourierParrotModel> {

    public CourierParrotRenderer(EntityRendererProvider.Context context) {
        super(context, new CourierParrotModel(context.bakeLayer(ModelLayers.PARROT)), 0.3F);
        this.addLayer(new CourierParrotHeldItemLayer(this, context.getItemRenderer()));
        this.addLayer(new CourierParrotAccessoryLayer(this, context.getModelSet()));
    }

    @Override
    public ResourceLocation getTextureLocation(CourierParrotEntity entity) {
        return ParrotRenderer.getVariantTexture(entity.getVariant());
    }
}
