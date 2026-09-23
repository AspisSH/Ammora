package com.ammora.mod.client.renderer;

import com.ammora.mod.entity.CourierPhantomEntity;
import net.minecraft.client.model.PhantomModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

/**
 * Entity renderer for the Courier Phantom with held item layer.
 */
public class CourierPhantomRenderer extends MobRenderer<CourierPhantomEntity, PhantomModel<CourierPhantomEntity>> {

    private static final ResourceLocation PHANTOM_TEXTURE =
            ResourceLocation.withDefaultNamespace("textures/entity/phantom.png");

    public CourierPhantomRenderer(EntityRendererProvider.Context context) {
        super(context, new PhantomModel<>(context.bakeLayer(ModelLayers.PHANTOM)), 0.6F);
        this.addLayer(new CourierPhantomHeldItemLayer(this, context.getItemRenderer()));
        this.addLayer(new CourierPhantomAccessoryLayer(this, context.getModelSet()));
    }

    @Override
    public ResourceLocation getTextureLocation(CourierPhantomEntity entity) {
        return PHANTOM_TEXTURE;
    }
}
