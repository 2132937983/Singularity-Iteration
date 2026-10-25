// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.client.builder;

import com.miophas.singularity_iteration.common.Singularity_Iteration;
import com.miophas.singularity_iteration.common.builder.BuilderRegistry;
import com.miophas.singularity_iteration.common.builder.RtsDroneAssist;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

/** Client registrations of the Blueprint Builder system (0.1.8.0). */
public final class BuilderClient {
    private BuilderClient() {}

    @EventBusSubscriber(modid = Singularity_Iteration.MOD_ID, value = Dist.CLIENT)
    public static final class ModEvents {
        @SubscribeEvent
        static void screens(RegisterMenuScreensEvent event) {
            event.register(BuilderRegistry.BUILDER_MENU.get(), BlueprintBuilderScreen::new);
        }

        @SubscribeEvent
        static void renderers(EntityRenderersEvent.RegisterRenderers event) {
            event.registerBlockEntityRenderer(BuilderRegistry.BUILDER_ENTITY.get(), BlueprintBuilderRenderer::new);
        }

        @SubscribeEvent
        static void setup(net.neoforged.fml.event.lifecycle.FMLClientSetupEvent event) {
            RtsDroneAssist.CLIENT_HOOK = RtsDroneVisuals::accept;
        }
    }

    @EventBusSubscriber(modid = Singularity_Iteration.MOD_ID, value = Dist.CLIENT)
    public static final class GameEvents {
        @SubscribeEvent
        static void tick(ClientTickEvent.Post event) { RtsDroneVisuals.tick(event); }

        @SubscribeEvent
        static void render(RenderLevelStageEvent event) { RtsDroneVisuals.render(event); }
    }
}
