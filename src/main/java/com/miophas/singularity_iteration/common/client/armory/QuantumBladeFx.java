// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.client.armory;

import com.miophas.singularity_iteration.common.armory.blade.BladeFxPacket;
import com.miophas.singularity_iteration.common.client.render.FxDraw;
import com.mojang.blaze3d.vertex.VertexConsumer;
import it.unimi.dsi.fastutil.ints.Int2FloatOpenHashMap;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector3f;

/**
 * Quantum Blade 3D geometry FX: restrained precision hard-sci-fi.
 * <ul>
 *   <li>Ground holographic interference rings (sqrt(k) spacing, expand then snap inward)</li>
 *   <li>Jittering translucent superposition brackets on each locked/entangled target + phase tether pulses</li>
 *   <li>Crisp white-cyan planar space slices & star-flare on collapse (zero clutter)</li>
 *   <li>Minimal rotating double-slit diamond marker on entangled targets</li>
 * </ul>
 */
final class QuantumBladeFx {
    private QuantumBladeFx() {}

    // Palette: pure white core -> crisp cyan -> deep cyan -> restrained violet accent
    private static final float WR = 0.93F, WG = 0.99F, WB = 1.00F;
    private static final float CR = 0.30F, CG = 0.90F, CB = 1.00F;
    private static final float VR = 0.61F, VG = 0.49F, VB = 1.00F;

    private record Cast(int userId, Vec3 fallbackPos, int[] targets, float collapseTick, boolean superMode, float start, float life, long seed) {}
    private record Collapse(Vec3 pos, boolean critical, float start, float life, long seed) {}
    private record EntangleHit(Vec3 pos, float start, float life) {}

    private static final List<Cast> CASTS = new ArrayList<>();
    private static final List<Collapse> COLLAPSES = new ArrayList<>();
    private static final List<EntangleHit> ENTANGLE_HITS = new ArrayList<>();
    /** entityId -> last-seen client tick for active entanglement marker rings. */
    private static final Int2FloatOpenHashMap ENTANGLED = new Int2FloatOpenHashMap();

    static void accept(BladeFxPacket p, float now) {
        long seed = Double.doubleToLongBits(p.x() * 19.3 + p.y() * 7.1 + p.z() * 23.9) ^ ((long) Float.floatToIntBits(now) << 13);
        switch (p.kind()) {
            case BladeFxPacket.QUANTUM_CAST -> {
                int[] raw = p.entityIds();
                int userId = raw.length > 0 ? raw[0] : -1;
                int[] targets = raw.length > 1 ? java.util.Arrays.copyOfRange(raw, 1, raw.length) : BladeFxPacket.EMPTY_IDS;
                float collapseTick = Math.max(6F, p.p2());
                boolean sup = (p.flags() & BladeFxPacket.FLAG_SUPER) != 0;
                CASTS.add(new Cast(userId, p.pos(), targets, collapseTick, sup, now, collapseTick + (sup ? 9F : 2F), seed));
            }
            case BladeFxPacket.QUANTUM_COLLAPSE -> {
                boolean crit = (p.flags() & BladeFxPacket.FLAG_CRITICAL) != 0;
                // Each collapse packet may carry target positions via entityIds (resolved at render/accept) or single pos
                int[] ids = p.entityIds();
                if (ids.length == 0) {
                    COLLAPSES.add(new Collapse(p.pos(), crit, now, 6.5F, seed));
                } else {
                    ClientLevel level = net.minecraft.client.Minecraft.getInstance().level;
                    for (int i = 0; i < ids.length; i++) {
                        Entity e = level != null ? level.getEntity(ids[i]) : null;
                        Vec3 at = e != null ? e.position().add(0, e.getBbHeight() * 0.5, 0) : p.pos();
                        COLLAPSES.add(new Collapse(at, crit, now, 6.5F, seed + i * 71L));
                    }
                }
            }
            case BladeFxPacket.QUANTUM_ENTANGLE_HIT -> ENTANGLE_HITS.add(new EntangleHit(p.pos(), now, 5.5F));
            case BladeFxPacket.QUANTUM_ENTANGLE_MARK -> {
                for (int id : p.entityIds()) ENTANGLED.put(id, now);
            }
            default -> {}
        }
    }

    static boolean isEmpty() {
        return CASTS.isEmpty() && COLLAPSES.isEmpty() && ENTANGLE_HITS.isEmpty() && ENTANGLED.isEmpty();
    }

    static void clear() {
        CASTS.clear();
        COLLAPSES.clear();
        ENTANGLE_HITS.clear();
        ENTANGLED.clear();
    }

    static void prune(int ticks) {
        CASTS.removeIf(c -> ticks - c.start > c.life + 1);
        COLLAPSES.removeIf(c -> ticks - c.start > c.life + 1);
        ENTANGLE_HITS.removeIf(e -> ticks - e.start > e.life + 1);
        ENTANGLED.int2FloatEntrySet().removeIf(e -> ticks - e.getFloatValue() > 16F);
    }

    static void renderGlow(VertexConsumer vc, Matrix4f m, ClientLevel level, Vec3 cam, Vector3f camLeft, Vector3f camUp,
                           float now, float partial, boolean reducedFlashes) {
        for (Cast c : CASTS) {
            float age = now - c.start;
            if (age < 0 || age > c.life) continue;
            drawCast(vc, m, level, cam, c, age, partial);
        }
        for (Collapse col : COLLAPSES) {
            float age = now - col.start;
            if (age < 0 || age > col.life) continue;
            drawCollapseSlice(vc, m, cam, camLeft, camUp, col, age, reducedFlashes);
        }
        for (EntangleHit eh : ENTANGLE_HITS) {
            float age = now - eh.start;
            if (age < 0 || age > eh.life) continue;
            drawEntangleHit(vc, m, eh, age);
        }
        if (!ENTANGLED.isEmpty()) {
            drawEntangledMarkers(vc, m, level, cam, now, partial);
        }
    }

    // ------------------------------------------------------------------ cast: interference rings + superposition brackets + phase tethers

    private static void drawCast(VertexConsumer vc, Matrix4f m, ClientLevel level, Vec3 cam, Cast c, float age, float partial) {
        Entity user = c.userId >= 0 ? level.getEntity(c.userId) : null;
        Vec3 foot = user != null ? user.getPosition(partial).add(0, 0.06, 0) : c.fallbackPos.add(0, 0.06, 0);
        Vec3 chest = user != null ? user.getPosition(partial).add(0, user.getBbHeight() * 0.58, 0) : c.fallbackPos.add(0, 1.0, 0);

        // Two cycles for Super (0..collapseTick, then collapseTick+2..collapseTick+8), one cycle otherwise
        float phase;
        if (age <= c.collapseTick) {
            phase = age / c.collapseTick;
        } else if (c.superMode && age >= c.collapseTick + 2F && age <= c.collapseTick + 8F) {
            phase = (age - (c.collapseTick + 2F)) / 6F;
        } else {
            return;
        }

        // 1. Ground concentric interference rings: expand smoothly (0..0.55), then snap sharply inward (0.55..1.0)
        float scale = phase < 0.55F
            ? Mth.sin((phase / 0.55F) * Mth.HALF_PI)
            : (1F - (phase - 0.55F) / 0.45F) * (1F - (phase - 0.55F) / 0.45F);
        float ringAlpha = (0.35F + 0.65F * Mth.sin(phase * Mth.PI));
        Vec3 up = new Vec3(0, 1, 0);
        for (int k = 1; k <= 6; k++) {
            float r = (float) (1.25 * Math.sqrt(k)) * Math.max(0.08F, scale);
            boolean bright = (k % 2 == 1);
            float w = bright ? 0.075F : 0.045F;
            float a = ringAlpha * (bright ? 0.82F : 0.36F) * (1F - k * 0.08F);
            FxDraw.ring(vc, m, foot, up, r, w, bright ? CR : VR, bright ? CG : VG, bright ? CB : VB, a, 36);
            if (bright && k <= 3) {
                FxDraw.ring(vc, m, foot, up, r, 0.022F, WR, WG, WB, a * 0.9F, 36);
            }
        }

        // 2. Superposition target brackets & crisp phase tether with travelling pulse
        for (int i = 0; i < c.targets.length; i++) {
            Entity target = level.getEntity(c.targets[i]);
            if (target == null || !target.isAlive()) continue;
            Vec3 center = target.getPosition(partial).add(0, target.getBbHeight() * 0.52, 0);

            // Thin phase tether from blade wielder to target + travelling white-cyan pulse
            FxDraw.line(vc, m, chest, center, cam, 0.018F, CR, CG, CB, ringAlpha * 0.38F);
            Vec3 pulseA = chest.lerp(center, Mth.clamp(phase - 0.08F, 0F, 1F));
            Vec3 pulseB = chest.lerp(center, Mth.clamp(phase + 0.04F, 0F, 1F));
            FxDraw.line(vc, m, pulseA, pulseB, cam, 0.042F, WR, WG, WB, ringAlpha * 0.92F);

            // 2 slightly offset jittering diamond/square superposition frames closing in on target
            float baseSize = Math.max(0.55F, Math.max(target.getBbWidth(), target.getBbHeight()) * 0.65F);
            float tighten = Mth.lerp(phase * phase, 1.45F, 0.22F);
            Vec3 toCam = cam.subtract(center);
            Vec3[] b = BladeFxRenderer.basis(toCam);
            Vec3 rx = b[1], ry = b[2];
            long s0 = c.seed + i * 43L + ((long) (age * 2F)) * 17L;
            for (int layer = 0; layer < 2; layer++) {
                float jitter = (1F - phase) * (layer == 0 ? 0.14F : 0.24F);
                Vec3 off = rx.scale((BladeFxRenderer.hash(s0 + layer * 11L) - 0.5F) * 2F * jitter)
                    .add(ry.scale((BladeFxRenderer.hash(s0 + layer * 23L + 5L) - 0.5F) * 2F * jitter));
                float sz = baseSize * tighten * (layer == 0 ? 1.0F : 1.18F);
                float rot = layer == 0 ? 0F : Mth.PI * 0.25F;
                float a = ringAlpha * (layer == 0 ? 0.85F : 0.42F);
                drawFrameQuad(vc, m, center.add(off), rx, ry, cam, sz, rot,
                    layer == 0 ? CR : VR, layer == 0 ? CG : VG, layer == 0 ? CB : VB, a, 0.026F);
            }
        }
    }

    private static void drawFrameQuad(VertexConsumer vc, Matrix4f m, Vec3 c, Vec3 rx, Vec3 ry, Vec3 cam,
                                      float size, float rot, float r, float g, float b, float alpha, float width) {
        Vec3[] pts = new Vec3[4];
        for (int k = 0; k < 4; k++) {
            float ang = rot + k * Mth.HALF_PI + Mth.PI * 0.25F;
            pts[k] = c.add(rx.scale(Mth.cos(ang) * size)).add(ry.scale(Mth.sin(ang) * size));
        }
        for (int k = 0; k < 4; k++) {
            FxDraw.line(vc, m, pts[k], pts[(k + 1) & 3], cam, width, r, g, b, alpha);
        }
    }

    // ------------------------------------------------------------------ collapse: razor-sharp space slices

    private static void drawCollapseSlice(VertexConsumer vc, Matrix4f m, Vec3 cam, Vector3f camLeft, Vector3f camUp,
                                          Collapse col, float age, boolean reducedFlashes) {
        float tNorm = age / col.life;
        float snap = Mth.clamp(age / 1.2F, 0F, 1F);
        float alpha = (1F - tNorm) * (1F - tNorm);
        if (alpha <= 0.015F) return;

        // Instant white-cyan core star flash on tick 0..2.5
        if (!reducedFlashes && age < 2.8F) {
            float flash = 1F - age / 2.8F;
            FxDraw.star(vc, m, col.pos, camLeft, camUp, (col.critical ? 1.85F : 1.35F) * (0.5F + 0.5F * snap),
                WR, WG, WB, flash * 0.95F);
        }

        // 2-3 intersecting razor-thin planar space slices that elongate then thin to a hairline
        int slices = col.critical ? 3 : 2;
        for (int i = 0; i < slices; i++) {
            long s = col.seed + i * 37L;
            Vec3 axis;
            if (col.critical && i == 2) {
                axis = new Vec3(0, 1, 0); // crisp vertical crit line
            } else {
                float yaw = BladeFxRenderer.hash(s) * Mth.TWO_PI;
                float pitch = (BladeFxRenderer.hash(s + 5L) - 0.5F) * 1.3F;
                axis = new Vec3(Mth.cos(yaw) * Mth.cos(pitch), Mth.sin(pitch), Mth.sin(yaw) * Mth.cos(pitch)).normalize();
            }
            float halfLen = (col.critical ? 2.45F : 1.95F) * (0.4F + 0.6F * snap);
            Vec3 a = col.pos.subtract(axis.scale(halfLen));
            Vec3 b = col.pos.add(axis.scale(halfLen));
            float wOuter = (col.critical ? 0.14F : 0.10F) * (1F - 0.85F * tNorm);
            float wCore = wOuter * 0.35F;
            FxDraw.line(vc, m, a, b, cam, wOuter, CR, CG, CB, alpha * 0.72F);
            FxDraw.line(vc, m, a, b, cam, wCore, WR, WG, WB, alpha * 0.98F);
        }

        // Subtle expanding precision ring on the cut plane
        Vec3 toCam = cam.subtract(col.pos);
        Vec3 normal = toCam.lengthSqr() > 1.0E-4 ? toCam.normalize() : new Vec3(0, 1, 0);
        FxDraw.ring(vc, m, col.pos, normal, 0.35F + 1.15F * tNorm, 0.045F * (1F - tNorm), CR, CG, CB, alpha * 0.65F, 24);
    }

    // ------------------------------------------------------------------ entanglement hit & minimal diamond ring

    private static void drawEntangleHit(VertexConsumer vc, Matrix4f m, EntangleHit eh, float age) {
        float tNorm = age / eh.life;
        float alpha = (1F - tNorm) * (1F - tNorm);
        if (alpha <= 0.02F) return;
        Vec3 up = new Vec3(0, 1, 0);
        float r = 0.45F + 0.55F * tNorm;
        FxDraw.ring(vc, m, eh.pos.add(0, 0.08, 0), up, r, 0.04F, CR, CG, CB, alpha * 0.85F, 20);
        FxDraw.ring(vc, m, eh.pos.add(0, -0.08, 0), up, r * 0.82F, 0.03F, WR, WG, WB, alpha * 0.75F, 20);
    }

    private static void drawEntangledMarkers(VertexConsumer vc, Matrix4f m, ClientLevel level, Vec3 cam, float now, float partial) {
        for (var entry : ENTANGLED.int2FloatEntrySet()) {
            float age = now - entry.getFloatValue();
            if (age > 15F) continue;
            Entity e = level.getEntity(entry.getIntKey());
            if (e == null || !e.isAlive()) continue;
            float fade = age > 10F ? 1F - (age - 10F) / 5F : 1F;
            Vec3 top = e.getPosition(partial).add(0, e.getBbHeight() + 0.38, 0);
            Vec3 toCam = cam.subtract(top);
            Vec3[] b = BladeFxRenderer.basis(toCam);
            float rot = now * 0.09F;
            drawFrameQuad(vc, m, top, b[1], b[2], cam, 0.24F, rot, CR, CG, CB, 0.75F * fade, 0.022F);
            // Tiny double-slit inner ticks
            Vec3 sl = b[1].scale(0.06);
            Vec3 sv = b[2].scale(0.11);
            FxDraw.line(vc, m, top.add(sl).subtract(sv), top.add(sl).add(sv), cam, 0.018F, WR, WG, WB, 0.85F * fade);
            FxDraw.line(vc, m, top.subtract(sl).subtract(sv), top.subtract(sl).add(sv), cam, 0.018F, WR, WG, WB, 0.85F * fade);
        }
    }
}
