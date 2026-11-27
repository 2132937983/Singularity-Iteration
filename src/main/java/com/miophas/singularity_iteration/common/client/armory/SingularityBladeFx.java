// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.client.armory;

import com.miophas.singularity_iteration.common.armory.blade.BladeFxPacket;
import com.miophas.singularity_iteration.common.armory.blade.SingularityFxPacket;
import com.miophas.singularity_iteration.common.client.render.FxDraw;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector3f;

/**
 * Singularity Blade 3D geometry FX: cosmic spectacle, complete even under Iris shader packs.
 * <ul>
 *   <li>Event horizon: opaque pure-black sphere core (with pre-burst pinch), Fresnel magenta photon ring,
 *       tilted spinning spiral accretion disk, 8–10 block inward gravitational streamlines</li>
 *   <li>Hawking burst: dual-shell expanding shockwave sphere + twin relativistic polar jets + white-magenta flash</li>
 *   <li>Tidal shear hit: inward magenta pull arc; unstable containment: escaping magenta fracture arcs</li>
 * </ul>
 */
final class SingularityBladeFx {
    private SingularityBladeFx() {}

    // Palette: pure black -> white-pink core -> vivid magenta -> hot outer orange
    private static final float WR = 1.00F, WG = 0.90F, WB = 0.98F;
    private static final float MR = 1.00F, MG = 0.17F, MB = 0.84F;
    private static final float OR = 1.00F, OG = 0.54F, OB = 0.24F;
    private static final float DR = 0.35F, DG = 0.04F, DB = 0.28F;

    private record Horizon(Vec3 center, float radius, int burstTick, float start, long seed) {}
    private record Tidal(Vec3 pos, Vec3 toUser, float mass, float start, float life, long seed) {}
    private record Unstable(Vec3 pos, float mass, float start, float life, long seed) {}

    private static final List<Horizon> HORIZONS = new ArrayList<>();
    private static final List<Tidal> TIDALS = new ArrayList<>();
    private static final List<Unstable> UNSTABLES = new ArrayList<>();

    static void acceptHorizon(SingularityFxPacket p, float now) {
        long seed = Double.doubleToLongBits(p.x() * 41.3 + p.y() * 13.7 + p.z() * 29.1) ^ ((long) Float.floatToIntBits(now) << 11);
        HORIZONS.add(new Horizon(p.center(), p.radius(), p.burstTick(), now, seed));
    }

    static void accept(BladeFxPacket p, float now) {
        long seed = Double.doubleToLongBits(p.x() * 17.7 + p.y() * 23.1 + p.z() * 11.3) ^ ((long) Float.floatToIntBits(now) << 15);
        switch (p.kind()) {
            case BladeFxPacket.SINGULARITY_TIDAL -> TIDALS.add(new Tidal(
                p.pos(), p.dir().lengthSqr() > 1.0E-6 ? p.dir().normalize() : new Vec3(0, 1, 0),
                p.p1(), now, 6.0F, seed));
            case BladeFxPacket.SINGULARITY_UNSTABLE -> UNSTABLES.add(new Unstable(p.pos(), p.p1(), now, 6.0F, seed));
            default -> {}
        }
    }

    static boolean isEmpty() {
        return HORIZONS.isEmpty() && TIDALS.isEmpty() && UNSTABLES.isEmpty();
    }

    static void clear() {
        HORIZONS.clear();
        TIDALS.clear();
        UNSTABLES.clear();
    }

    static void prune(int ticks) {
        HORIZONS.removeIf(h -> ticks - h.start > h.burstTick + 14F);
        TIDALS.removeIf(t -> ticks - t.start > t.life + 1);
        UNSTABLES.removeIf(u -> ticks - u.start > u.life + 1);
    }

    /** Horizon core scale: quick open, hold, dramatic 2-tick inward pinch just before Hawking burst. */
    private static float coreScale(float age, int burstTick) {
        if (age < 0 || age >= burstTick + 0.5F) return 0F;
        if (age < 3.0F) {
            float t = age / 3.0F;
            return 1F - (1F - t) * (1F - t);
        }
        float pinchStart = burstTick - 2.5F;
        if (age > pinchStart) {
            float p = Mth.clamp((age - pinchStart) / 2.5F, 0F, 1F);
            return Math.max(0.12F, 1F - 0.85F * p * p);
        }
        return 1F + 0.04F * Mth.sin(age * 0.9F);
    }

    // ------------------------------------------------------------------ Pass 1: pure-black event horizon core

    static void renderDark(VertexConsumer vc, Matrix4f m, float now) {
        for (Horizon h : HORIZONS) {
            float age = now - h.start;
            float sc = coreScale(age, h.burstTick);
            if (sc <= 0.01F) continue;
            BladeFxRenderer.sphere(vc, m, h.center, h.radius * 0.72F * sc, 10, 16, 0F, 0F, 0F, 0.98F);
        }
    }

    // ------------------------------------------------------------------ Pass 2: translucent deep-magenta accretion base

    static void renderTranslucent(VertexConsumer vc, Matrix4f m, Vec3 cam, float now) {
        for (Horizon h : HORIZONS) {
            float age = now - h.start;
            float sc = coreScale(age, h.burstTick);
            if (sc <= 0.01F) continue;
            drawAccretionDisk(vc, m, h, age, sc, true);
        }
    }

    // ------------------------------------------------------------------ Pass 3: photon ring, accretion glow, streamlines, Hawking burst

    static void renderGlow(VertexConsumer vc, Matrix4f m, Vec3 cam, Vector3f camLeft, Vector3f camUp, float now, boolean reducedFlashes) {
        for (Horizon h : HORIZONS) {
            float age = now - h.start;
            if (age < 0) continue;
            float sc = coreScale(age, h.burstTick);
            if (sc > 0.01F) {
                drawPhotonRing(vc, m, cam, h, age, sc);
                drawAccretionDisk(vc, m, h, age, sc, false);
                drawGravitationalStreamlines(vc, m, cam, h, age, sc);
            }
            if (age >= h.burstTick) {
                drawHawkingBurst(vc, m, cam, camLeft, camUp, h, age - h.burstTick, reducedFlashes);
            }
        }
        for (Tidal t : TIDALS) {
            float age = now - t.start;
            if (age < 0 || age > t.life) continue;
            drawTidalArc(vc, m, cam, t, age);
        }
        for (Unstable u : UNSTABLES) {
            float age = now - u.start;
            if (age < 0 || age > u.life) continue;
            drawUnstableArcs(vc, m, cam, u, age);
        }
    }

    private static void drawPhotonRing(VertexConsumer vc, Matrix4f m, Vec3 cam, Horizon h, float age, float sc) {
        Vec3 toCam = cam.subtract(h.center);
        Vec3 normal = toCam.lengthSqr() > 1.0E-4 ? toCam.normalize() : new Vec3(0, 1, 0);
        float r = h.radius * 0.74F * sc;
        float pulse = 0.85F + 0.15F * Mth.sin(age * 2.2F);
        FxDraw.ring(vc, m, h.center, normal, r, r * 0.32F, MR, MG, MB, 0.85F * pulse, 32);
        FxDraw.ring(vc, m, h.center, normal, r * 0.99F, r * 0.09F, WR, WG, WB, 0.95F * pulse, 32);
    }

    private static void drawAccretionDisk(VertexConsumer vc, Matrix4f m, Horizon h, float age, float sc, boolean backing) {
        // 20-degree tilted disk axes
        double tilt = Math.toRadians(20);
        Vec3 uAxis = new Vec3(1, 0, 0);
        Vec3 vAxis = new Vec3(0, Math.sin(tilt), Math.cos(tilt));
        float spin = age * 0.42F;
        float rIn = h.radius * 0.95F * sc;
        float rMid = h.radius * 1.95F * sc;
        float rOut = h.radius * 3.15F * sc;
        int segs = 36;

        for (int i = 0; i < segs; i++) {
            float a0 = spin + Mth.TWO_PI * i / segs;
            float a1 = spin + Mth.TWO_PI * (i + 1) / segs;
            // Two-arm spiral brightness modulation
            float mod0 = 0.55F + 0.45F * Mth.sin(a0 * 2F - age * 0.75F);
            float mod1 = 0.55F + 0.45F * Mth.sin(a1 * 2F - age * 0.75F);

            Vec3 d0 = uAxis.scale(Mth.cos(a0)).add(vAxis.scale(Mth.sin(a0)));
            Vec3 d1 = uAxis.scale(Mth.cos(a1)).add(vAxis.scale(Mth.sin(a1)));

            Vec3 pIn0 = h.center.add(d0.scale(rIn)), pIn1 = h.center.add(d1.scale(rIn));
            Vec3 pMid0 = h.center.add(d0.scale(rMid)), pMid1 = h.center.add(d1.scale(rMid));
            Vec3 pOut0 = h.center.add(d0.scale(rOut)), pOut1 = h.center.add(d1.scale(rOut));

            if (backing) {
                BladeFxRenderer.quad(vc, m, pIn0, pOut0, pOut1, pIn1,
                    DR, DG, DB, 0.42F * mod0, DR, DG, DB, 0F);
            } else {
                // Inner band: white-pink -> vivid magenta
                BladeFxRenderer.quad(vc, m, pIn0, pMid0, pMid1, pIn1,
                    WR, WG, WB, 0.72F * mod0, MR, MG, MB, 0.65F * mod1);
                // Outer band: vivid magenta -> hot orange fade
                BladeFxRenderer.quad(vc, m, pMid0, pOut0, pOut1, pMid1,
                    MR, MG, MB, 0.58F * mod0, OR, OG, OB, 0F);
            }
        }
    }

    private static void drawGravitationalStreamlines(VertexConsumer vc, Matrix4f m, Vec3 cam, Horizon h, float age, float sc) {
        double tilt = Math.toRadians(20);
        Vec3 uAxis = new Vec3(1, 0, 0);
        Vec3 vAxis = new Vec3(0, Math.sin(tilt), Math.cos(tilt));
        Vec3 nAxis = new Vec3(0, Math.cos(tilt), -Math.sin(tilt));
        float maxRange = h.radius > 1.1F ? 9.5F : 7.5F;

        int streams = 8;
        for (int i = 0; i < streams; i++) {
            long s = h.seed + i * 67L;
            float phase = (age * 0.085F + BladeFxRenderer.hash(s)) % 1.0F;
            float rHead = Mth.lerp(phase * phase, maxRange, h.radius * 0.9F * sc);
            float rTail = Math.min(maxRange, rHead + 1.4F * (1F - phase * 0.5F));
            float baseAng = BladeFxRenderer.hash(s + 3L) * Mth.TWO_PI + phase * 2.6F;
            float elev = (BladeFxRenderer.hash(s + 7L) - 0.5F) * 2.2F * (1F - phase);

            Vec3 prev = null;
            int segs = 5;
            for (int k = 0; k <= segs; k++) {
                float f = k / (float) segs;
                float r = Mth.lerp(f, rTail, rHead);
                float a = baseAng - (1F - f) * 0.45F;
                Vec3 pt = h.center.add(uAxis.scale(Mth.cos(a) * r)).add(vAxis.scale(Mth.sin(a) * r)).add(nAxis.scale(elev * (1F - f * 0.4F)));
                if (prev != null) {
                    float alpha = Mth.sin(phase * Mth.PI) * (0.25F + 0.75F * f) * 0.72F;
                    boolean core = (i % 3 == 0);
                    FxDraw.line(vc, m, prev, pt, cam, core ? 0.032F : 0.048F,
                        core ? WR : MR, core ? WG : MG, core ? WB : MB, alpha);
                }
                prev = pt;
            }
        }
    }

    private static void drawHawkingBurst(VertexConsumer vc, Matrix4f m, Vec3 cam, Vector3f camLeft, Vector3f camUp,
                                         Horizon h, float bt, boolean reducedFlashes) {
        float life = 11.0F;
        if (bt < 0 || bt > life) return;
        float tNorm = bt / life;
        float easeOut = 1F - (1F - tNorm) * (1F - tNorm) * (1F - tNorm);
        float alpha = (1F - tNorm) * (1F - tNorm);
        float burstRange = h.radius > 1.1F ? 7.0F : 5.0F;

        // 1. Dual expanding spherical shockwave shells
        float outerR = 0.6F + burstRange * easeOut;
        BladeFxRenderer.sphere(vc, m, h.center, outerR, 10, 18, MR, MG, MB, alpha * 0.34F);
        if (tNorm < 0.65F) {
            float innerAlpha = (1F - tNorm / 0.65F);
            BladeFxRenderer.sphere(vc, m, h.center, outerR * 0.72F, 8, 14, WR, WG, WB, innerAlpha * innerAlpha * 0.42F);
        }

        // 2. Twin relativistic polar jets along the tilted disk normal (rapidly elongate then thin out)
        double tilt = Math.toRadians(20);
        Vec3 pole = new Vec3(0, Math.cos(tilt), -Math.sin(tilt)).normalize();
        if (bt < 7.5F) {
            float jt = bt / 7.5F;
            float jetAlpha = (1F - jt) * (1F - jt);
            float jetLen = (burstRange * 1.45F) * Mth.clamp(bt / 1.8F, 0.25F, 1F);
            float jetW = 0.42F * (1F - 0.85F * jt);
            Vec3 top = h.center.add(pole.scale(jetLen));
            Vec3 bot = h.center.subtract(pole.scale(jetLen));
            FxDraw.beam(vc, m, h.center, top, cam, jetW, MR, MG, MB, jetAlpha * 0.85F, 0F);
            FxDraw.beam(vc, m, h.center, bot, cam, jetW, MR, MG, MB, jetAlpha * 0.85F, 0F);
            FxDraw.line(vc, m, bot, top, cam, jetW * 0.38F, WR, WG, WB, jetAlpha * 0.96F);
        }

        // 3. Equatorial shock ring + core flash star
        FxDraw.ring(vc, m, h.center, pole, outerR * 0.92F, 0.45F * (1F - tNorm) + 0.08F, MR, MG, MB, alpha * 0.85F, 36);
        if (!reducedFlashes && bt < 4.0F) {
            float f = 1F - bt / 4.0F;
            FxDraw.star(vc, m, h.center, camLeft, camUp, burstRange * 0.85F * (0.4F + 0.6F * f), WR, WG, WB, f * 0.95F);
        }
    }

    // ------------------------------------------------------------------ tidal shear & unstable arcs

    private static void drawTidalArc(VertexConsumer vc, Matrix4f m, Vec3 cam, Tidal t, float age) {
        float tNorm = age / t.life;
        float alpha = (1F - tNorm) * (1F - tNorm) * Mth.clamp(0.45F + t.mass / 120F, 0.45F, 1F);
        if (alpha <= 0.02F) return;
        Vec3 from = t.pos;
        Vec3 to = t.pos.add(t.toUser.scale(1.6 + 0.5 * tNorm));
        BladeFxRenderer.arc(vc, m, from, to, cam, 5, 0.16F, 0.045F, MR, MG, MB, alpha * 0.85F, t.seed);
        BladeFxRenderer.arc(vc, m, from, to, cam, 5, 0.10F, 0.020F, WR, WG, WB, alpha * 0.90F, t.seed + 3L);
    }

    private static void drawUnstableArcs(VertexConsumer vc, Matrix4f m, Vec3 cam, Unstable u, float age) {
        float tNorm = age / u.life;
        float alpha = (1F - tNorm) * 0.85F;
        if (alpha <= 0.02F) return;
        for (int i = 0; i < 2; i++) {
            long s = u.seed + i * 31L + ((long) age) * 19L;
            Vec3 dir = new Vec3(BladeFxRenderer.hash(s) - 0.5F, BladeFxRenderer.hash(s + 3L) - 0.5F, BladeFxRenderer.hash(s + 7L) - 0.5F).normalize();
            Vec3 end = u.pos.add(dir.scale(0.55 + 0.35 * tNorm));
            BladeFxRenderer.arc(vc, m, u.pos, end, cam, 4, 0.14F, 0.032F, MR, MG, MB, alpha, s);
        }
    }
}
