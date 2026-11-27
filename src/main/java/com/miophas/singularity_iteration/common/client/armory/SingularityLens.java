// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.client.armory;

import com.miophas.singularity_iteration.common.Singularity_Iteration;
import com.miophas.singularity_iteration.common.armory.blade.SingularityFxPacket;
import com.miophas.singularity_iteration.common.client.render.IrisCompat;
import com.miophas.singularity_iteration.common.client.suit.FcsClientConfig;
import com.mojang.blaze3d.systems.RenderSystem;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.PostChain;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.joml.Matrix4f;
import org.joml.Vector4f;

/**
 * 0.1.11.236 (SlashBlade collab B4): the singularity blade's event horizon on the screen, a post chain
 * ({@code shaders/post/singularity_lens.json}, the same way as the warp's {@code WarpPost}) run over the level after it is
 * drawn: the picture bends round the micro-singularity into an Einstein ring, the horizon is black with a magenta photon
 * ring, and the Hawking burst flashes magenta and white. One horizon at a time (the nearest open one). Off with the warp
 * screen effect option, while a shader pack is in use, and for the session if the chain fails to load.
 */
@EventBusSubscriber(modid = Singularity_Iteration.MOD_ID, value = Dist.CLIENT)
public final class SingularityLens {
    private SingularityLens() {}

    private static final ResourceLocation CHAIN = ResourceLocation.fromNamespaceAndPath(Singularity_Iteration.MOD_ID, "shaders/post/singularity_lens.json");
    private static PostChain chain;
    private static boolean failed;
    private static int width, height;

    private record Horizon(Vec3 center, float radius, int burstTick, long start) {}

    private static final List<Horizon> OPEN = new ArrayList<>();

    static void accept(SingularityFxPacket p) {
        var level = Minecraft.getInstance().level;
        if (level == null) return;
        OPEN.add(new Horizon(p.center(), p.radius(), p.burstTick(), level.getGameTime()));
    }

    @SubscribeEvent
    static void render(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_LEVEL || failed || OPEN.isEmpty()) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) {
            OPEN.clear();
            return;
        }
        float partial = event.getPartialTick().getGameTimeDeltaPartialTick(false);
        long now = mc.level.getGameTime();
        Vec3 cam = event.getCamera().getPosition();
        Horizon best = null;
        double bestDist = Double.MAX_VALUE;
        for (var it = OPEN.iterator(); it.hasNext(); ) {
            Horizon h = it.next();
            float age = now - h.start + partial;
            if (!SingularityFxPacket.alive(age, h.burstTick)) {
                it.remove();
                continue;
            }
            double d = h.center.distanceToSqr(cam);
            if (d < bestDist) {
                bestDist = d;
                best = h;
            }
        }
        if (best == null || !FcsClientConfig.warpPost() || IrisCompat.shaderPackInUse()) return;
        float age = now - best.start + partial;
        float strength = SingularityFxPacket.strength(age, best.burstTick), burst = SingularityFxPacket.burst(age, best.burstTick);
        if (strength <= 0.005F && burst <= 0.005F) return;
        // the centre on the screen and the horizon's size in screen heights
        Matrix4f view = new Matrix4f(event.getModelViewMatrix()), proj = new Matrix4f(event.getProjectionMatrix());
        Vec3 rel = best.center.subtract(cam);
        Vector4f clip = new Vector4f((float) rel.x, (float) rel.y, (float) rel.z, 1F);
        view.transform(clip);
        proj.transform(clip);
        if (clip.w <= 0.05F) return;
        float sx = (clip.x / clip.w) * 0.5F + 0.5F, sy = (clip.y / clip.w) * 0.5F + 0.5F;
        float radius = best.radius * proj.m11() / clip.w * 0.5F;
        if (chain == null) {
            try {
                chain = new PostChain(mc.getTextureManager(), mc.getResourceManager(), mc.getMainRenderTarget(), CHAIN);
                width = height = -1;
            } catch (Exception e) {
                failed = true;
                Singularity_Iteration.LOGGER.warn("Singularity lens screen effect off: the post chain did not load", e);
                return;
            }
        }
        var target = mc.getMainRenderTarget();
        if (target.width != width || target.height != height) {
            width = target.width;
            height = target.height;
            chain.resize(width, height);
        }
        chain.setUniform("CenterX", sx);
        chain.setUniform("CenterY", sy);
        chain.setUniform("Radius", Math.min(radius, 0.5F));
        chain.setUniform("Strength", strength);
        chain.setUniform("Burst", burst);
        RenderSystem.disableBlend();
        RenderSystem.disableDepthTest();
        RenderSystem.resetTextureMatrix();
        chain.process(partial);
        target.bindWrite(false);
        RenderSystem.enableDepthTest();
    }

    @EventBusSubscriber(modid = Singularity_Iteration.MOD_ID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
    static final class Setup {
        private Setup() {}

        @SubscribeEvent
        static void setup(FMLClientSetupEvent event) {
            SingularityFxPacket.HOOK = SingularityLens::accept;
        }

        @SubscribeEvent
        static void reload(RegisterClientReloadListenersEvent event) {
            event.registerReloadListener((net.minecraft.server.packs.resources.ResourceManagerReloadListener) manager -> {
                if (chain != null) chain.close();
                chain = null;
                failed = false;
            });
        }
    }
}
