package com.ammora.mod.client.renderer;

import com.ammora.mod.AmmoraMod;
import com.ammora.mod.client.model.CourierDroneModel;
import com.ammora.mod.entity.CourierDroneEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

/**
 * Entity renderer for the Courier Drone with spinning rotors and suspended cargo layer.
 */
public class CourierDroneRenderer extends MobRenderer<CourierDroneEntity, CourierDroneModel> {

    private static final ResourceLocation DRONE_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(AmmoraMod.MODID, "textures/entity/drone.png");

    public CourierDroneRenderer(EntityRendererProvider.Context context) {
        super(context, new CourierDroneModel(context.bakeLayer(CourierDroneModel.LAYER_LOCATION)), 0.4F);
        this.addLayer(new CourierDroneHeldItemLayer(this, context.getItemRenderer()));
    }

    @Override
    public ResourceLocation getTextureLocation(CourierDroneEntity entity) {
        return DRONE_TEXTURE;
    }
}
