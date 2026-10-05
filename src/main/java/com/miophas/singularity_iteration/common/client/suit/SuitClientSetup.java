// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.client.suit;

import com.miophas.singularity_iteration.common.Singularity_Iteration;
import com.miophas.singularity_iteration.common.suit.SuitRegistry;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

/** Client registrations of the suit upgrade system (mod bus). */
@EventBusSubscriber(modid = Singularity_Iteration.MOD_ID, value = Dist.CLIENT)
public final class SuitClientSetup {
    private SuitClientSetup() {}

    @SubscribeEvent
    public static void onScreens(RegisterMenuScreensEvent event) {
        event.register(SuitRegistry.STATION_MENU.get(), QuantumModStationScreen::new);
    }

    @SubscribeEvent
    public static void onRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(SuitRegistry.STATION_ENTITY.get(), QuantumModStationRenderer::new);
    }
}
