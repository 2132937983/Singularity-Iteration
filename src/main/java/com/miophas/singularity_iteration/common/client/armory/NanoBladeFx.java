// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.client.armory;

import com.miophas.singularity_iteration.common.armory.blade.BladeFxPacket;
import com.miophas.singularity_iteration.common.client.render.FxDraw;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector3f;

/**
 * Nano Blade 3D geometry FX: high-frequency industrial violence.
 * <ul>
 *   <li>Continuous 3D piezoelectric standing-wave ribbons + jagged node arcs</li>
 *   <li>Wide-arc glowing crescent slash web (0° / ±30° / ±15° + Just vertical fissure & heat ring)</li>
 *   <li>White-hot piezo hit slashes & red-hot thermal flares</li>
 *   <li>Overheat pressurized twin steam-vent cones & hot-edge micro-arcs</li>
 * </ul>
 */
final class NanoBladeFx {
    private NanoBladeFx() {}

    // Palette: white-hot core -> molten gold -> industrial orange -> dark scorched backing
    private static final float WR = 1.00F, WG = 0.96F, WB = 0.86F;
    private static final float GR = 1.00F, GG = 0.77F, GB = 0.42F;
    private static final float OR = 1.00F, OG = 0.42F, OB = 0.08F;
    private static final float DR = 0.55F, DG = 0.13F, DB = 0.03F;

    private record Wave(Vec3 origin, Vec3 dir, float length, int halfWaves, float start, float life, long seed) {}
    private record Slash(Vec3 origin, Vec3 dir, float rollDeg, float radius, boolean justVertical, boolean hot, float start, float life, long seed) {}
    private record Hit(Vec3 pos, Vec3 dir, boolean redHot, float start, float life, long seed) {}
    private record Steam(Vec3 pos, Vec3 dir, float start, float life, long seed) {}
    private record HeatArc(Vec3 pos, Vec3 dir, float heat, float start, float life, long seed) {}

    private static final List<Wave> WAVES = new ArrayList<>();
    private static final List<Slash> SLASHES = new ArrayList<>();
    private static final List<Hit> HITS = new ArrayList<>();
    private static final List<Steam> STEAMS = new ArrayList<>();
    private static final List<HeatArc> HEAT_ARCS = new ArrayList<>();

    static void accept(BladeFxPacket p, float now) {
        long seed = Double.doubleToLongBits(p.x() * 31.7 + p.y() * 17.3 + p.z() * 13.1) ^ ((long) Float.floatToIntBits(now) << 17);
        switch (p.kind()) {
            case BladeFxPacket.NANO_WAVE -> WAVES.add(new Wave(
                p.pos(), p.dir().lengthSqr() > 1.0E-6 ? p.dir().normalize() : new Vec3(0, 0, 1),
                Math.max(4F, p.p1()), Math.max(2, Math.round(p.p2())), now, 12F, seed));
            case BladeFxPacket.NANO_SLASH -> {
                boolean just = (p.flags() & BladeFxPacket.FLAG_JUST) != 0;
                boolean hot = (p.flags() & BladeFxPacket.FLAG_RED_HOT) != 0 || (p.flags() & BladeFxPacket.FLAG_SUPER) != 0 || just;
                SLASHES.add(new Slash(
                    p.pos(), p.dir().lengthSqr() > 1.0E-6 ? p.dir().normalize() : new Vec3(0, 0, 1),
                    p.p1(), Math.max(2.4F, p.p2()), just, hot, now, just ? 11.5F : 8.5F, seed));
            }
            case BladeFxPacket.NANO_HIT -> HITS.add(new Hit(
                p.pos(), p.dir().lengthSqr() > 1.0E-6 ? p.dir().normalize() : new Vec3(0, 1, 0),
                (p.flags() & BladeFxPacket.FLAG_RED_HOT) != 0, now, 6.0F, seed));
            case BladeFxPacket.NANO_OVERHEAT -> STEAMS.add(new Steam(
                p.pos(), p.dir().lengthSqr() > 1.0E-6 ? p.dir().normalize() : new Vec3(0, 0, 1),
                now, 16.0F, seed));
            case BladeFxPacket.NANO_HEAT_ARC -> HEAT_ARCS.add(new HeatArc(
                p.pos(), p.dir().lengthSqr() > 1.0E-6 ? p.dir().normalize() : new Vec3(0, 0, 1),
                p.p1(), now, 5.0F, seed));
            default -> {}
        }
    }

    static boolean isEmpty() {
        return WAVES.isEmpty() && SLASHES.isEmpty() && HITS.isEmpty() && STEAMS.isEmpty() && HEAT_ARCS.isEmpty();
    }

    static void clear() {
        WAVES.clear();
        SLASHES.clear();
        HITS.clear();
        STEAMS.clear();
        HEAT_ARCS.clear();
    }

    static void prune(int ticks) {
        WAVES.removeIf(w -> ticks - w.start > w.life + 1);
        SLASHES.removeIf(s -> ticks - s.start > s.life + 1);
        HITS.removeIf(h -> ticks - h.start > h.life + 1);
        STEAMS.removeIf(s -> ticks - s.start > s.life + 1);
        HEAT_ARCS.removeIf(a -> ticks - a.start > a.life + 1);
    }

    static void renderTranslucent(VertexConsumer vc, Matrix4f m, Vec3 cam, float now) {
        // Dark scorched-orange backing behind each slash crescent for daytime contrast
        for (Slash s : SLASHES) {
            float age = now - s.start;
            if (age < 0 || age > s.life) continue;
            drawSlashCrescent(vc, m, s, age, true);
        }
        // Overheat pressurized twin steam-vent cones
        for (Steam st : STEAMS) {
            float age = now - st.start;
            if (age < 0 || age > st.life) continue;
            drawOverheatSteam(vc, m, st, age);
        }
    }

    static void renderGlow(VertexConsumer vc, Matrix4f m, Vec3 cam, Vector3f camLeft, Vector3f camUp, float now, boolean reducedFlashes) {
        for (Wave w : WAVES) {
            float age = now - w.start;
            if (age < 0 || age > w.life) continue;
            drawStandingWave(vc, m, cam, camLeft, camUp, w, age, reducedFlashes);
        }
        for (Slash s : SLASHES) {
            float age = now - s.start;
            if (age < 0 || age > s.life) continue;
            drawSlashCrescent(vc, m, s, age, false);
            drawSlashSparks(vc, m, cam, s, age);
            if (s.justVertical) drawJustFissureAndRing(vc, m, cam, s, age, reducedFlashes);
        }
        for (Hit h : HITS) {
            float age = now - h.start;
            if (age < 0 || age > h.life) continue;
            drawPiezoHit(vc, m, cam, h, age, reducedFlashes);
        }
        for (HeatArc ha : HEAT_ARCS) {
            float age = now - ha.start;
            if (age < 0 || age > ha.life) continue;
            drawHeatArc(vc, m, cam, ha, age);
        }
    }

    // ------------------------------------------------------------------ standing wave

    private static void drawStandingWave(VertexConsumer vc, Matrix4f m, Vec3 cam, Vector3f camLeft, Vector3f camUp,
                                         Wave w, float age, boolean reducedFlashes) {
        float tNorm = age / w.life;
        float attack = Mth.clamp(age / 1.8F, 0F, 1F);
        float decay = (1F - tNorm) * (1F - tNorm * 0.4F);
        float alpha = attack * decay;
        if (alpha <= 0.01F) return;

        Vec3[] b = BladeFxRenderer.basis(w.dir);
        Vec3 fwd = b[0], right = b[1], up = b[2];
        // ~28Hz visual oscillation with partialTick smoothness
        float osc = Mth.sin(age * 4.4F);
        float maxAmp = 0.52F * attack * (0.45F + 0.55F * Math.abs(osc)) * (1F - 0.35F * tNorm);

        int segs = 40;
        for (int sign = -1; sign <= 1; sign += 2) {
            Vec3 prev = null;
            for (int i = 0; i <= segs; i++) {
                float u = i / (float) segs;
                float env = Mth.sin(Mth.PI * w.halfWaves * u);
                float amp = sign * maxAmp * env * (osc >= 0 ? 1F : -1F);
                Vec3 pt = w.origin.add(fwd.scale(w.length * u)).add(up.scale(amp));
                if (prev != null) {
                    float hot = Math.abs(env);
                    float r = Mth.lerp(hot, OR, GR), g = Mth.lerp(hot, OG, GG), bl = Mth.lerp(hot, OB, GB);
                    FxDraw.line(vc, m, prev, pt, cam, 0.11F * decay + 0.03F, r, g, bl, alpha * 0.78F);
                    // crisp white-hot filament core
                    FxDraw.line(vc, m, prev, pt, cam, 0.035F * decay + 0.012F, WR, WG, WB, alpha * 0.85F);
                }
                prev = pt;
            }
        }

        // Jagged piezo arcs + stars at the wave nodes
        long arcGen = w.seed + ((long) (age * 0.5F)) * 131L;
        for (int k = 0; k <= w.halfWaves; k++) {
            Vec3 node = w.origin.add(fwd.scale(w.length * k / w.halfWaves));
            if (!reducedFlashes && age < 6.0F) {
                float starAlpha = alpha * (1F - age / 6.0F) * 0.85F;
                FxDraw.star(vc, m, node, camLeft, camUp, 0.32F + 0.22F * attack, WR, WG, WB, starAlpha);
            }
            for (int a = 0; a < 2; a++) {
                long s = arcGen + k * 41L + a * 17L;
                float ang = BladeFxRenderer.hash(s) * Mth.TWO_PI;
                float rad = 0.38F + 0.35F * BladeFxRenderer.hash(s + 5L);
                Vec3 tip = node.add(right.scale(Mth.cos(ang) * rad)).add(up.scale(Mth.sin(ang) * rad))
                    .add(fwd.scale((BladeFxRenderer.hash(s + 9L) - 0.5F) * 0.45F));
                BladeFxRenderer.arc(vc, m, node, tip, cam, 4, 0.14F, 0.028F, WR, WG, WB, alpha * 0.9F, s);
                BladeFxRenderer.arc(vc, m, node, tip, cam, 4, 0.14F, 0.065F, OR, OG, OB, alpha * 0.38F, s);
            }
        }
    }

    // ------------------------------------------------------------------ 3D crescent slash web

    private static void drawSlashCrescent(VertexConsumer vc, Matrix4f m, Slash s, float age, boolean backing) {
        float tNorm = age / s.life;
        // Fast 2-tick sweep across the full arc, then lingering luminous web
        float sweep = Mth.clamp(age / 2.1F, 0F, 1F);
        float easeSweep = 1F - (1F - sweep) * (1F - sweep);
        float fade = tNorm < 0.25F ? 1F : (1F - (tNorm - 0.25F) / 0.75F);
        fade = fade * fade;
        if (fade <= 0.01F) return;

        Vec3[] b = BladeFxRenderer.basis(s.dir);
        Vec3 fwd = b[0], right = b[1], up = b[2];
        double radRoll = Math.toRadians(s.rollDeg);
        double cr = Math.cos(radRoll), sr = Math.sin(radRoll);
        // Plane axes: u along rolled right, v along forward (blade cuts through the forward semicircle)
        Vec3 uAxis = right.scale(cr).add(up.scale(sr));
        Vec3 vAxis = fwd;
        Vec3 center = s.origin.add(fwd.scale(0.35));

        float halfSpan = (float) Math.toRadians(s.justVertical ? 88F : 76F);
        float startAng = -halfSpan;
        float endAng = Mth.lerp(easeSweep, -halfSpan + 0.18F, halfSpan);
        float outerR = s.radius * (1F + 0.16F * tNorm);
        float maxWidth = (s.justVertical ? 1.35F : 1.05F) * (1F - 0.35F * tNorm);

        int segs = 28;
        Vec3 prevOut = null, prevIn = null, prevCoreIn = null;
        float prevProfile = 0F;
        for (int i = 0; i <= segs; i++) {
            float f = i / (float) segs;
            float ang = Mth.lerp(f, startAng, endAng);
            // Crescent width tapers sharply at both tips, thickest near leading edge
            float profile = Mth.sin(f * Mth.PI) * (0.45F + 0.55F * f);
            float w = maxWidth * profile;
            float c = Mth.cos(ang), sn = Mth.sin(ang);
            Vec3 radial = vAxis.scale(c).add(uAxis.scale(sn));
            Vec3 pOut = center.add(radial.scale(outerR));
            Vec3 pIn = center.add(radial.scale(Math.max(0.25F, outerR - w)));
            Vec3 pCoreIn = center.add(radial.scale(Math.max(0.25F, outerR - Math.max(0.08F, w * 0.22F))));

            if (prevOut != null) {
                float segFade = fade * (0.35F + 0.65F * Math.max(prevProfile, profile));
                if (backing) {
                    BladeFxRenderer.quad(vc, m, prevOut, prevIn, pIn, pOut,
                        DR, DG, DB, segFade * 0.42F, DR, DG, DB, 0F);
                } else {
                    float bodyR = s.hot ? GR : OR, bodyG = s.hot ? GG : OG, bodyB = s.hot ? GB : OB;
                    // Wide molten-orange blade body
                    BladeFxRenderer.quad(vc, m, prevOut, prevIn, pIn, pOut,
                        bodyR, bodyG, bodyB, segFade * 0.78F, OR, OG, OB, 0F);
                    // Razor-sharp white-hot leading edge
                    BladeFxRenderer.quad(vc, m, prevOut, prevCoreIn, pCoreIn, pOut,
                        WR, WG, WB, segFade * 0.96F, GR, GG, GB, segFade * 0.18F);
                }
            }
            prevOut = pOut;
            prevIn = pIn;
            prevCoreIn = pCoreIn;
            prevProfile = profile;
        }
    }

    private static void drawSlashSparks(VertexConsumer vc, Matrix4f m, Vec3 cam, Slash s, float age) {
        float tNorm = age / s.life;
        float alpha = (1F - tNorm) * (1F - tNorm);
        if (alpha <= 0.02F) return;

        Vec3[] b = BladeFxRenderer.basis(s.dir);
        Vec3 fwd = b[0], right = b[1], up = b[2];
        double radRoll = Math.toRadians(s.rollDeg);
        Vec3 uAxis = right.scale(Math.cos(radRoll)).add(up.scale(Math.sin(radRoll)));
        Vec3 nAxis = right.scale(-Math.sin(radRoll)).add(up.scale(Math.cos(radRoll)));
        Vec3 center = s.origin.add(fwd.scale(0.35));

        int count = s.justVertical ? 14 : 9;
        for (int i = 0; i < count; i++) {
            long hs = s.seed + i * 53L;
            float ang = (BladeFxRenderer.hash(hs) - 0.5F) * (float) Math.toRadians(135F);
            Vec3 radial = fwd.scale(Mth.cos(ang)).add(uAxis.scale(Mth.sin(ang)));
            Vec3 tangent = fwd.scale(-Mth.sin(ang)).add(uAxis.scale(Mth.cos(ang)));
            float speed = 0.22F + 0.32F * BladeFxRenderer.hash(hs + 3L);
            float drift = (BladeFxRenderer.hash(hs + 7L) - 0.5F) * 0.25F;
            Vec3 base = center.add(radial.scale(s.radius * (0.88F + 0.16F * BladeFxRenderer.hash(hs + 11L))))
                .add(tangent.scale(age * speed))
                .add(radial.scale(age * speed * 0.35F))
                .add(nAxis.scale(drift * age));
            Vec3 tail = base.subtract(tangent.scale(0.32F + 0.28F * (1F - tNorm)));
            boolean white = (i % 3 == 0);
            FxDraw.line(vc, m, tail, base, cam, white ? 0.032F : 0.045F,
                white ? WR : GR, white ? WG : GG, white ? WB : GB, alpha * 0.9F);
        }
    }

    private static void drawJustFissureAndRing(VertexConsumer vc, Matrix4f m, Vec3 cam, Slash s, float age, boolean reducedFlashes) {
        float tNorm = age / s.life;
        float alpha = (1F - tNorm) * (1F - tNorm * 0.5F);
        if (alpha <= 0.02F) return;

        Vec3[] b = BladeFxRenderer.basis(s.dir);
        Vec3 fwd = b[0], up = b[2];
        // High-frequency jittering white-hot fissure line along the vertical cleave
        Vec3 from = s.origin.add(fwd.scale(1.1)).add(up.scale(-0.6));
        Vec3 to = s.origin.add(fwd.scale(6.2)).add(up.scale(0.5));
        long jitterSeed = s.seed + ((long) (age * 2F)) * 79L;
        BladeFxRenderer.arc(vc, m, from, to, cam, 8, 0.18F * (1F - tNorm), 0.055F, WR, WG, WB, alpha * (reducedFlashes ? 0.65F : 0.95F), jitterSeed);
        BladeFxRenderer.arc(vc, m, from, to, cam, 8, 0.22F * (1F - tNorm), 0.14F, OR, OG, OB, alpha * 0.42F, jitterSeed);

        // Expanding heat shock ring travelling forward
        Vec3 ringCenter = s.origin.add(fwd.scale(1.4 + age * 0.42));
        float ringR = 0.55F + age * 0.24F;
        FxDraw.ring(vc, m, ringCenter, fwd, ringR, 0.35F * (1F - tNorm) + 0.08F, GR, GG, GB, alpha * 0.72F, 24);
        FxDraw.ring(vc, m, ringCenter, fwd, ringR * 0.96F, 0.08F, WR, WG, WB, alpha * 0.85F, 24);
    }

    // ------------------------------------------------------------------ piezo hit & red-hot

    private static void drawPiezoHit(VertexConsumer vc, Matrix4f m, Vec3 cam, Hit h, float age, boolean reducedFlashes) {
        float tNorm = age / h.life;
        float stretch = Mth.clamp(age / 1.4F, 0F, 1F);
        float alpha = (1F - tNorm) * (1F - tNorm);
        if (alpha <= 0.02F) return;

        Vec3[] b = BladeFxRenderer.basis(h.dir);
        float tilt = (BladeFxRenderer.hash(h.seed) - 0.5F) * 1.4F;
        Vec3 axis = b[1].scale(Mth.cos(tilt)).add(b[2].scale(Mth.sin(tilt))).normalize();
        float halfLen = (h.redHot ? 1.35F : 1.05F) * (0.45F + 0.55F * stretch);
        Vec3 p0 = h.pos.subtract(axis.scale(halfLen));
        Vec3 p1 = h.pos.add(axis.scale(halfLen));
        float width = (h.redHot ? 0.13F : 0.095F) * (1F - 0.75F * tNorm);
        FxDraw.line(vc, m, p0, p1, cam, width * 2.2F, OR, OG, OB, alpha * 0.55F);
        FxDraw.line(vc, m, p0, p1, cam, width, WR, WG, WB, alpha * (reducedFlashes ? 0.7F : 0.95F));

        // 2 short piezo arcs hugging the hit point
        for (int i = 0; i < 2; i++) {
            long s = h.seed + i * 29L + ((long) age) * 13L;
            Vec3 end = h.pos.add(
                (BladeFxRenderer.hash(s) - 0.5F) * 1.2F,
                (BladeFxRenderer.hash(s + 3L) - 0.5F) * 1.2F,
                (BladeFxRenderer.hash(s + 7L) - 0.5F) * 1.2F);
            BladeFxRenderer.arc(vc, m, h.pos, end, cam, 3, 0.12F, 0.024F, WR, WG, WB, alpha * 0.85F, s);
        }

        if (h.redHot) {
            Vec3 toCam = cam.subtract(h.pos);
            FxDraw.disc(vc, m, h.pos, toCam.lengthSqr() > 1.0E-4 ? toCam : new Vec3(0, 1, 0),
                0.65F + 0.45F * tNorm, OR, OG, OB, alpha * 0.45F, 14);
            for (int i = 0; i < 4; i++) {
                long s = h.seed + 100L + i * 19L;
                Vec3 dir = new Vec3(BladeFxRenderer.hash(s) - 0.5F, BladeFxRenderer.hash(s + 2L) - 0.2F, BladeFxRenderer.hash(s + 4L) - 0.5F).normalize();
                Vec3 head = h.pos.add(dir.scale(0.35 + age * 0.28));
                Vec3 tail = head.subtract(dir.scale(0.32));
                FxDraw.line(vc, m, tail, head, cam, 0.03F, GR, GG, GB, alpha * 0.9F);
            }
        }
    }

    // ------------------------------------------------------------------ overheat steam vent & hot-edge micro arcs

    private static void drawOverheatSteam(VertexConsumer vc, Matrix4f m, Steam st, float age) {
        float tNorm = age / st.life;
        float alpha = (1F - tNorm) * (1F - tNorm * 0.4F) * 0.52F;
        if (alpha <= 0.01F) return;

        Vec3[] b = BladeFxRenderer.basis(st.dir);
        Vec3 right = b[1], up = b[2], fwd = b[0];
        // Twin pressurized steam jets venting left & right from the blade core
        for (int side = -1; side <= 1; side += 2) {
            Vec3 jetDir = right.scale(side * 0.92).add(up.scale(0.28)).add(fwd.scale(-0.25)).normalize();
            for (int k = 0; k < 5; k++) {
                float dist = 0.22F + (age * 0.14F) * (0.45F + k * 0.22F);
                float rad = 0.10F + dist * 0.24F;
                Vec3 c = st.pos.add(jetDir.scale(dist));
                float ringA = alpha * (1F - k * 0.14F);
                FxDraw.disc(vc, m, c, jetDir, rad, 0.92F, 0.94F, 0.96F, ringA, 12);
            }
        }
    }

    private static void drawHeatArc(VertexConsumer vc, Matrix4f m, Vec3 cam, HeatArc ha, float age) {
        float tNorm = age / ha.life;
        float alpha = (1F - tNorm) * Mth.clamp((ha.heat - 45F) / 55F, 0.35F, 1F);
        if (alpha <= 0.02F) return;
        Vec3[] b = BladeFxRenderer.basis(ha.dir);
        long s = ha.seed + ((long) (age * 1.5F)) * 37L;
        Vec3 a = ha.pos.add(b[0].scale(0.15)).add(b[2].scale((BladeFxRenderer.hash(s) - 0.5F) * 0.2F));
        Vec3 c = a.add(b[0].scale(0.42)).add(b[1].scale((BladeFxRenderer.hash(s + 5L) - 0.5F) * 0.25F))
            .add(b[2].scale((BladeFxRenderer.hash(s + 9L) - 0.5F) * 0.25F));
        BladeFxRenderer.arc(vc, m, a, c, cam, 3, 0.07F, 0.02F, WR, WG, WB, alpha * 0.85F, s);
        BladeFxRenderer.arc(vc, m, a, c, cam, 3, 0.07F, 0.045F, OR, OG, OB, alpha * 0.42F, s);
    }
}
