// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.client.armory;

import com.miophas.singularity_iteration.common.Singularity_Iteration;
import com.miophas.singularity_iteration.common.armory.blade.BladeFxPacket;
import com.miophas.singularity_iteration.common.armory.blade.SingularityFxPacket;
import com.miophas.singularity_iteration.common.client.render.IrisCompat;
import com.miophas.singularity_iteration.common.client.render.SiFxBuffers;
import com.miophas.singularity_iteration.common.client.render.SiRenderTypes;
import com.miophas.singularity_iteration.common.client.suit.FcsClientConfig;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.joml.Matrix4f;
import org.joml.Vector3f;

/**
 * Unified world-space 3D geometry renderer for the three SlashBlade collab blades (Nano, Quantum, Singularity).
 * Renders on {@link IrisCompat#isFxStage(RenderLevelStageEvent)} via {@link SiFxBuffers}, so every blade looks crisp
 * both in vanilla and under Iris shader packs.
 */
public final class BladeFxRenderer {
    private BladeFxRenderer() {}

    private static int ticks;

    public static void accept(BladeFxPacket p) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        float now = ticks;
        switch (p.kind()) {
            case BladeFxPacket.NANO_WAVE, BladeFxPacket.NANO_SLASH, BladeFxPacket.NANO_HIT,
                 BladeFxPacket.NANO_OVERHEAT, BladeFxPacket.NANO_HEAT_ARC -> NanoBladeFx.accept(p, now);
            case BladeFxPacket.QUANTUM_CAST, BladeFxPacket.QUANTUM_COLLAPSE,
                 BladeFxPacket.QUANTUM_ENTANGLE_HIT, BladeFxPacket.QUANTUM_ENTANGLE_MARK -> QuantumBladeFx.accept(p, now);
            case BladeFxPacket.SINGULARITY_TIDAL, BladeFxPacket.SINGULARITY_UNSTABLE -> SingularityBladeFx.accept(p, now);
            default -> {}
        }
    }

    public static void acceptSingularity(SingularityFxPacket p) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        SingularityBladeFx.acceptHorizon(p, ticks);
    }

    public static void tick(Minecraft mc) {
        if (mc.level == null || mc.player == null) {
            NanoBladeFx.clear();
            QuantumBladeFx.clear();
            SingularityBladeFx.clear();
            return;
        }
        ticks++;
        NanoBladeFx.prune(ticks);
        QuantumBladeFx.prune(ticks);
        SingularityBladeFx.prune(ticks);
    }

    public static void render(RenderLevelStageEvent event) {
        if (IrisCompat.isShadowPass()) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return;
        if (NanoBladeFx.isEmpty() && QuantumBladeFx.isEmpty() && SingularityBladeFx.isEmpty()) return;

        float partial = event.getPartialTick().getGameTimeDeltaPartialTick(false);
        float now = ticks + partial;
        Camera camera = event.getCamera();
        Vec3 cam = camera.getPosition();
        Vector3f camLeft = camera.getLeftVector();
        Vector3f camUp = camera.getUpVector();
        boolean reducedFlashes = FcsClientConfig.flashScale() < 0.5F;

        PoseStack pose = event.getPoseStack();
        MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();
        pose.pushPose();
        pose.translate(-cam.x, -cam.y, -cam.z);
        Matrix4f m = pose.last().pose();

        // Pass 1: opaque/dark matte (e.g. singularity event horizon black core)
        if (!SingularityBladeFx.isEmpty()) {
            VertexConsumer dark = IrisCompat.shaderPackInUse()
                ? SiFxBuffers.translucent(buffers)
                : buffers.getBuffer(SiRenderTypes.FLAT);
            SingularityBladeFx.renderDark(dark, m, now);
            if (IrisCompat.shaderPackInUse()) SiFxBuffers.endTranslucent(buffers);
            else buffers.endBatch(SiRenderTypes.FLAT);
        }

        // Pass 2: alpha-blended translucent backing (nano dark-orange blade undercoat, overheat steam, accretion base)
        VertexConsumer trans = SiFxBuffers.translucent(buffers);
        NanoBladeFx.renderTranslucent(trans, m, cam, now);
        SingularityBladeFx.renderTranslucent(trans, m, cam, now);
        SiFxBuffers.endTranslucent(buffers);

        // Pass 3: additive glow (wave ribbons, slash crescents, arcs, interference rings, space cuts, photon rings, jets)
        VertexConsumer glow = SiFxBuffers.glow(buffers);
        NanoBladeFx.renderGlow(glow, m, cam, camLeft, camUp, now, reducedFlashes);
        QuantumBladeFx.renderGlow(glow, m, mc.level, cam, camLeft, camUp, now, partial, reducedFlashes);
        SingularityBladeFx.renderGlow(glow, m, cam, camLeft, camUp, now, reducedFlashes);
        SiFxBuffers.endGlow(buffers);

        pose.popPose();
    }

    // ------------------------------------------------------------------ shared geometry helpers

    /** Builds an orthonormal basis (right, up) perpendicular to {@code forward}. */
    static Vec3[] basis(Vec3 forward) {
        Vec3 f = forward.lengthSqr() > 1.0E-6 ? forward.normalize() : new Vec3(0, 0, 1);
        Vec3 ref = Math.abs(f.y) > 0.92 ? new Vec3(1, 0, 0) : new Vec3(0, 1, 0);
        Vec3 right = f.cross(ref).normalize();
        Vec3 up = right.cross(f).normalize();
        return new Vec3[]{f, right, up};
    }

    /** Draws a jagged electrical arc between {@code a} and {@code b} with deterministic jitter from {@code seed}. */
    static void arc(VertexConsumer vc, Matrix4f m, Vec3 a, Vec3 b, Vec3 cam, int segments, float jitter,
                    float width, float r, float g, float bl, float alpha, long seed) {
        Vec3 diff = b.subtract(a);
        if (diff.lengthSqr() < 1.0E-6) return;
        Vec3[] b3 = basis(diff);
        Vec3 right = b3[1], up = b3[2];
        Vec3 prev = a;
        for (int i = 1; i <= segments; i++) {
            float t = i / (float) segments;
            Vec3 next = a.lerp(b, t);
            if (i < segments) {
                float env = Mth.sin(t * Mth.PI);
                float j1 = (hash(seed + i * 31L) - 0.5F) * 2F * jitter * env;
                float j2 = (hash(seed + i * 97L + 7L) - 0.5F) * 2F * jitter * env;
                next = next.add(right.scale(j1)).add(up.scale(j2));
            }
            com.miophas.singularity_iteration.common.client.render.FxDraw.line(vc, m, prev, next, cam, width, r, g, bl, alpha);
            prev = next;
        }
    }

    /** Draws a double-sided UV-less quad strip segment between (p0, p1) and (q0, q1). */
    static void quad(VertexConsumer vc, Matrix4f m, Vec3 p0, Vec3 p1, Vec3 q1, Vec3 q0,
                     float r0, float g0, float b0, float a0, float r1, float g1, float b1, float a1) {
        float ca0 = Mth.clamp(a0, 0F, 1F), ca1 = Mth.clamp(a1, 0F, 1F);
        vc.addVertex(m, (float) p0.x, (float) p0.y, (float) p0.z).setColor(r0, g0, b0, ca0);
        vc.addVertex(m, (float) p1.x, (float) p1.y, (float) p1.z).setColor(r1, g1, b1, ca1);
        vc.addVertex(m, (float) q1.x, (float) q1.y, (float) q1.z).setColor(r1, g1, b1, ca1);
        vc.addVertex(m, (float) q0.x, (float) q0.y, (float) q0.z).setColor(r0, g0, b0, ca0);
    }

    /** Draws a low-poly UV sphere of quads at {@code c} with radius {@code radius}. */
    static void sphere(VertexConsumer vc, Matrix4f m, Vec3 c, float radius, int stacks, int slices,
                       float r, float g, float b, float alpha) {
        float a = Mth.clamp(alpha, 0F, 1F);
        for (int i = 0; i < stacks; i++) {
            double phi0 = Math.PI * i / stacks - Math.PI * 0.5;
            double phi1 = Math.PI * (i + 1) / stacks - Math.PI * 0.5;
            double y0 = Math.sin(phi0) * radius, r0 = Math.cos(phi0) * radius;
            double y1 = Math.sin(phi1) * radius, r1 = Math.cos(phi1) * radius;
            for (int j = 0; j < slices; j++) {
                double th0 = Math.PI * 2 * j / slices;
                double th1 = Math.PI * 2 * (j + 1) / slices;
                double c0 = Math.cos(th0), s0 = Math.sin(th0);
                double c1 = Math.cos(th1), s1 = Math.sin(th1);
                vc.addVertex(m, (float) (c.x + r0 * c0), (float) (c.y + y0), (float) (c.z + r0 * s0)).setColor(r, g, b, a);
                vc.addVertex(m, (float) (c.x + r1 * c0), (float) (c.y + y1), (float) (c.z + r1 * s0)).setColor(r, g, b, a);
                vc.addVertex(m, (float) (c.x + r1 * c1), (float) (c.y + y1), (float) (c.z + r1 * s1)).setColor(r, g, b, a);
                vc.addVertex(m, (float) (c.x + r0 * c1), (float) (c.y + y0), (float) (c.z + r0 * s1)).setColor(r, g, b, a);
            }
        }
    }

    /** Deterministic pseudo-random float in [0, 1). */
    static float hash(long seed) {
        long z = (seed ^ 0x9E3779B97F4A7C15L) * 0xBF58476D1CE4E5B9L;
        z = (z ^ (z >>> 27)) * 0x94D049BB133111EBL;
        z = z ^ (z >>> 31);
        return ((z >>> 40) & 0xFFFFFF) / (float) 0x1000000;
    }

    @EventBusSubscriber(modid = Singularity_Iteration.MOD_ID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
    static final class Setup {
        private Setup() {}

        @SubscribeEvent
        static void setup(FMLClientSetupEvent event) {
            BladeFxPacket.HOOK = BladeFxRenderer::accept;
        }
    }
}
