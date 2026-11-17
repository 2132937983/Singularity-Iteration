// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.client.render;

import com.miophas.singularity_iteration.common.Singularity_Iteration;
import com.miophas.singularity_iteration.common.client.suit.FcsClientConfig;
import com.miophas.singularity_iteration.common.client.suit.ManeuverModeClient;
import com.miophas.singularity_iteration.common.suit.WarpRules;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.PostChain;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

/**
 * 0.1.11.73 (phase warp style phase W3, decision two: made, on by default, can be turned off): the screen effect of the
 * own warp, a post chain ({@code shaders/post/phase_warp.json}) run over the level after it is drawn (before the hand and
 * the HUD): a radial blur out from the centre, the colours split at the edge, a dark violet edge and a lit centre, as
 * strong as {@link WarpRules#postStrength}; the gather before the barrier splits the colours a little and closes the edge
 * in. Option {@code warp_post}; skipped while a shader pack is in use ({@link IrisCompat}) and in the classic style; turned
 * off for the session if the chain fails to load.
 */
@EventBusSubscriber(modid = Singularity_Iteration.MOD_ID, value = Dist.CLIENT)
public final class WarpPost {
    private WarpPost() {}

    private static final ResourceLocation CHAIN = ResourceLocation.fromNamespaceAndPath(Singularity_Iteration.MOD_ID, "shaders/post/phase_warp.json");
    private static PostChain chain;
    private static boolean failed;
    private static int width, height;

    @SubscribeEvent
    static void render(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_LEVEL || failed) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || !FcsClientConfig.warpPost() || FcsClientConfig.classicFlightFx() || IrisCompat.shaderPackInUse()) return;
        float partial = event.getPartialTick().getGameTimeDeltaPartialTick(false);
        float strength = WarpRules.postStrength(ManeuverModeClient.warpSince(mc.player, partial));
        float gather = ManeuverModeClient.warpGather(mc.player, partial);
        if (strength <= 0.005F && gather <= 0.005F) return;
        if (chain == null) {
            try {
                chain = new PostChain(mc.getTextureManager(), mc.getResourceManager(), mc.getMainRenderTarget(), CHAIN);
                width = height = -1;
            } catch (Exception e) {
                failed = true;
                Singularity_Iteration.LOGGER.warn("Phase warp screen effect off: the post chain did not load", e);
                return;
            }
        }
        var target = mc.getMainRenderTarget();
        if (target.width != width || target.height != height) {
            width = target.width;
            height = target.height;
            chain.resize(width, height);
        }
        chain.setUniform("Strength", strength);
        chain.setUniform("Gather", gather);
        RenderSystem.disableBlend();
        RenderSystem.disableDepthTest();
        RenderSystem.resetTextureMatrix();
        chain.process(partial);
        target.bindWrite(false);
        RenderSystem.enableDepthTest();
    }

    /** Drops the chain (resources reloaded, or the option changed): it loads again when next needed. */
    public static void reset() {
        if (chain != null) chain.close();
        chain = null;
        failed = false;
    }

    /** Drops the chain when the resources reload (its shaders and targets are rebuilt from the new ones). */
    @EventBusSubscriber(modid = Singularity_Iteration.MOD_ID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
    static final class Reload {
        private Reload() {}

        @SubscribeEvent
        static void register(net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent event) {
            event.registerReloadListener((net.minecraft.server.packs.resources.ResourceManagerReloadListener) manager -> reset());
        }
    }
}
