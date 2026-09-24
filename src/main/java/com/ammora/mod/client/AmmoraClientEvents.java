package com.ammora.mod.client;

import com.ammora.mod.AmmoraMod;
import com.ammora.mod.client.model.CourierAllayAccessoryModel;
import com.ammora.mod.client.model.CourierBeeAccessoryModel;
import com.ammora.mod.client.model.CourierDroneModel;
import com.ammora.mod.client.model.CourierParrotAccessoryModel;
import com.ammora.mod.client.model.CourierPhantomAccessoryModel;
import com.ammora.mod.client.renderer.CourierAllayRenderer;
import com.ammora.mod.client.renderer.CourierBeeRenderer;
import com.ammora.mod.client.renderer.CourierDroneRenderer;
import com.ammora.mod.client.renderer.CourierParrotRenderer;
import com.ammora.mod.client.renderer.CourierPhantomRenderer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

/**
 * Mod bus event subscriber for client-side registrations.
 */
@EventBusSubscriber(modid = AmmoraMod.MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class AmmoraClientEvents {

    @SubscribeEvent
    public static void registerEntityRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(AmmoraMod.COURIER_BEE.get(), CourierBeeRenderer::new);
        event.registerEntityRenderer(AmmoraMod.COURIER_ALLAY.get(), CourierAllayRenderer::new);
        event.registerEntityRenderer(AmmoraMod.COURIER_PHANTOM.get(), CourierPhantomRenderer::new);
        event.registerEntityRenderer(AmmoraMod.COURIER_PARROT.get(), CourierParrotRenderer::new);
        event.registerEntityRenderer(AmmoraMod.COURIER_DRONE.get(), CourierDroneRenderer::new);
    }

    @SubscribeEvent
    public static void registerLayerDefinitions(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(CourierBeeAccessoryModel.LAYER_LOCATION, CourierBeeAccessoryModel::createBodyLayer);
        event.registerLayerDefinition(CourierAllayAccessoryModel.LAYER_LOCATION, CourierAllayAccessoryModel::createBodyLayer);
        event.registerLayerDefinition(CourierPhantomAccessoryModel.LAYER_LOCATION, CourierPhantomAccessoryModel::createBodyLayer);
        event.registerLayerDefinition(CourierParrotAccessoryModel.LAYER_LOCATION, CourierParrotAccessoryModel::createBodyLayer);
        event.registerLayerDefinition(CourierDroneModel.LAYER_LOCATION, CourierDroneModel::createBodyLayer);
    }
}
