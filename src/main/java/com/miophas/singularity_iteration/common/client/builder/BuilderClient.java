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

        /** 0.1.11.185: turning and mirroring the blueprint while it is placed in the world. */
        @SubscribeEvent
        static void keys(net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent event) {
            BuilderAdjust.ROTATE = new net.minecraft.client.KeyMapping("key.mio_icif.blueprint_rotate", net.neoforged.neoforge.client.settings.KeyConflictContext.IN_GAME,
                com.mojang.blaze3d.platform.InputConstants.Type.KEYSYM, org.lwjgl.glfw.GLFW.GLFW_KEY_R, "key.category.mio_icif");
            BuilderAdjust.MIRROR = new net.minecraft.client.KeyMapping("key.mio_icif.blueprint_mirror", net.neoforged.neoforge.client.settings.KeyConflictContext.IN_GAME,
                com.mojang.blaze3d.platform.InputConstants.Type.KEYSYM, org.lwjgl.glfw.GLFW.GLFW_KEY_M, "key.category.mio_icif");
            event.register(BuilderAdjust.ROTATE);
            event.register(BuilderAdjust.MIRROR);
        }

        @SubscribeEvent
        static void setup(net.neoforged.fml.event.lifecycle.FMLClientSetupEvent event) {
            RtsDroneAssist.CLIENT_HOOK = RtsDroneVisuals::accept;
            com.miophas.singularity_iteration.common.builder.BuilderUploadPackets.CLIENT_HOOK = BlueprintUploadClient::onStatus;   // 0.1.11.182
            com.miophas.singularity_iteration.common.builder.BuilderPlanPackets.CLIENT_HOOK = BuilderGhosts::accept;   // 0.1.11.184
        }
    }

    @EventBusSubscriber(modid = Singularity_Iteration.MOD_ID, value = Dist.CLIENT)
    public static final class GameEvents {
        @SubscribeEvent
        static void tick(ClientTickEvent.Post event) {
            RtsDroneVisuals.tick(event);
            BlueprintUploadClient.tick();   // 0.1.11.182: the pieces of a blueprint upload
            BuilderGhosts.tick();   // 0.1.11.184: the block hologram's check round the plan
            BuilderAdjust.tick();   // 0.1.11.185: placing the blueprint in the world
        }

        @SubscribeEvent
        static void render(RenderLevelStageEvent event) {
            RtsDroneVisuals.render(event);
            BuilderGhosts.render(event);   // 0.1.11.184: the builder's block hologram
        }

        @SubscribeEvent
        static void scroll(net.neoforged.neoforge.client.event.InputEvent.MouseScrollingEvent event) { BuilderAdjust.scroll(event); }

        @SubscribeEvent
        static void interact(net.neoforged.neoforge.client.event.InputEvent.InteractionKeyMappingTriggered event) { BuilderAdjust.interact(event); }

        @SubscribeEvent
        static void hud(net.neoforged.neoforge.client.event.RenderGuiEvent.Post event) { BuilderAdjust.hud(event); }

        @SubscribeEvent
        static void opening(net.neoforged.neoforge.client.event.ScreenEvent.Opening event) { BuilderAdjust.opening(event); }   // 0.1.11.186

        @SubscribeEvent
        static void loggedOut(net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent.LoggingOut event) { BuilderAdjust.clear(); }
    }
}
