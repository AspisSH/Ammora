package com.ammora.mod.client;

import com.ammora.mod.AmmoraMod;
import com.ammora.mod.client.renderer.CourierBeeRenderer;
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
    }
}
