// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.client.suit;

import com.miophas.singularity_iteration.common.suit.SuitModuleType;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

/**
 * Shared optical-visor drawing primitives for the curved helmet-visor HUD (Plan E + F):
 * three-layer collimated lines/arcs (dark optical backing + soft glow + sharp core),
 * parabolic curved visor bars, oscilloscope phosphor trails, helmet parallax lag,
 * peripheral visor edge physical cues (thermal shimmer &amp; quantum interference fringes),
 * and crisp vector skill icons.
 */
public final class VisorHudPrimitives {
    private VisorHudPrimitives() {}

    public static final int BACKING = 0x040A12;
    public static final int GLASS_BG = 0x08101A;
    public static final int WHITE = 0xEAF6FF;
    public static final int CYAN = 0x78EEFF;
    public static final int VIOLET = 0xC398FF;
    public static final int AMBER = 0xFFBE4E;
    public static final int RED = 0xFF4D45;
    public static final int GOLD = 0xFFD67A;

    // ------------------------------------------------------------------ Helmet Visor Parallax

    private static float lastYaw = Float.NaN, lastPitch = Float.NaN;
    private static float parX, parY;

    /** Updates and returns subtle clamped [-2.5, +2.5] px helmet-visor inertia lag from head turn velocity. */
    public static void updateParallax(LocalPlayer player, float partial, float[] out) {
        if (player == null) {
            parX = parY = 0F;
            lastYaw = lastPitch = Float.NaN;
            out[0] = out[1] = 0F;
            return;
        }
        float yaw = player.getViewYRot(partial);
        float pitch = player.getViewXRot(partial);
        if (Float.isNaN(lastYaw)) {
            lastYaw = yaw;
            lastPitch = pitch;
        }
        float dy = Mth.wrapDegrees(yaw - lastYaw);
        float dp = pitch - lastPitch;
        lastYaw = yaw;
        lastPitch = pitch;
        float targetX = Mth.clamp(-dy * 0.22F, -2.5F, 2.5F);
        float targetY = Mth.clamp(-dp * 0.18F, -2.0F, 2.0F);
        parX += (targetX - parX) * 0.18F;
        parY += (targetY - parY) * 0.18F;
        if (Math.abs(parX) < 0.02F) parX = 0F;
        if (Math.abs(parY) < 0.02F) parY = 0F;
        out[0] = parX;
        out[1] = parY;
    }

    // ------------------------------------------------------------------ Oscilloscope Phosphor Trail

    /** Tracks a decaying white phosphor persistence tail whenever a bar/ring value drops. */
    public static final class PhosphorTrail {
        private float trail = -1F;
        private float lastVal = -1F;

        public float sample(float value) {
            if (trail < 0F || lastVal < 0F) {
                trail = lastVal = value;
                return value;
            }
            if (value < lastVal - 0.5F) {
                trail = Math.max(trail, lastVal);
            }
            if (trail > value) {
                trail = Math.max(value, trail - Math.max(0.35F, (trail - value) * 0.10F));
            } else {
                trail = value;
            }
            lastVal = value;
            return trail;
        }
    }

    // ------------------------------------------------------------------ 3-Layer Collimated Optical Primitives

    /** Three-layer optical line: dark backing for bright-sky contrast, soft glow, sharp luminous core. */
    public static void opticalLine(GuiGraphics g, float x1, float y1, float x2, float y2, float width, int rgb, float alpha) {
        if (alpha <= 0.01F) return;
        FcsDraw.line(g, x1, y1, x2, y2, width + 1.5F, FcsDraw.argb(BACKING, 0.50F * alpha));
        FcsDraw.line(g, x1, y1, x2, y2, width + 2.4F, FcsDraw.argb(rgb, 0.22F * alpha));
        FcsDraw.line(g, x1, y1, x2, y2, width, FcsDraw.argb(rgb, alpha));
    }

    /** Three-layer circular optical arc (radians, 0 = up, clockwise). */
    public static void opticalArc(GuiGraphics g, float cx, float cy, float r, float a0, float a1, float width, int rgb, float alpha) {
        if (alpha <= 0.01F || Math.abs(a1 - a0) < 1.0E-4F) return;
        FcsDraw.arc(g, cx, cy, r, a0, a1, width + 1.6F, FcsDraw.argb(BACKING, 0.48F * alpha));
        FcsDraw.arc(g, cx, cy, r, a0, a1, width + 2.6F, FcsDraw.argb(rgb, 0.22F * alpha));
        FcsDraw.arc(g, cx, cy, r, a0, a1, width, FcsDraw.argb(rgb, alpha));
    }

    /** Evaluates the parabolic spherical visor Y coordinate at screen X. */
    public static float curveY(float x, float cx, float yBase, float curveH, float spanRef) {
        float u = (x - cx) / Math.max(1F, spanRef);
        return yBase - curveH * u * u;
    }

    /** Parabolic curved helmet-visor bar with 3-layer optical rendering. */
    public static void curvedBar(GuiGraphics g, float cx, float yBase, float x0, float x1, float curveH, float spanRef,
                                 float width, int rgb, float alpha, int segs) {
        if (alpha <= 0.01F || Math.abs(x1 - x0) < 0.5F) return;
        int n = Math.max(2, segs);
        int backCol = FcsDraw.argb(BACKING, 0.50F * alpha);
        int glowCol = FcsDraw.argb(rgb, 0.22F * alpha);
        int coreCol = FcsDraw.argb(rgb, alpha);
        float px = x0, py = curveY(x0, cx, yBase, curveH, spanRef);
        for (int i = 1; i <= n; i++) {
            float x = Mth.lerp(i / (float) n, x0, x1);
            float y = curveY(x, cx, yBase, curveH, spanRef);
            FcsDraw.line(g, px, py, x, y, width + 1.6F, backCol);
            FcsDraw.line(g, px, py, x, y, width + 2.6F, glowCol);
            FcsDraw.line(g, px, py, x, y, width, coreCol);
            px = x;
            py = y;
        }
    }

    /** Subtle curved helmet-visor glass horizon hairline (no heavy backing). */
    public static void curvedHairline(GuiGraphics g, float cx, float yBase, float x0, float x1, float curveH, float spanRef,
                                      float width, int rgb, float alpha, int segs) {
        if (alpha <= 0.01F) return;
        int n = Math.max(4, segs);
        int col = FcsDraw.argb(rgb, alpha);
        float px = x0, py = curveY(x0, cx, yBase, curveH, spanRef);
        for (int i = 1; i <= n; i++) {
            float x = Mth.lerp(i / (float) n, x0, x1);
            float y = curveY(x, cx, yBase, curveH, spanRef);
            FcsDraw.line(g, px, py, x, y, width, col);
            px = x;
            py = y;
        }
    }

    /** Real-time standing-wave harmonic ripple riding along the Quantum curved coherence bar. */
    public static void curvedWave(GuiGraphics g, float cx, float yBase, float x0, float x1, float curveH, float spanRef,
                                  float amp, float freq, float phase, float jitter, int rgb, float alpha, int segs) {
        if (alpha <= 0.01F || Math.abs(x1 - x0) < 2F) return;
        int n = Math.max(6, segs);
        int col = FcsDraw.argb(rgb, alpha);
        float px = 0, py = 0;
        for (int i = 0; i <= n; i++) {
            float u = i / (float) n;
            float x = Mth.lerp(u, x0, x1);
            float env = (float) Math.sin(u * Math.PI);
            float wave = (float) Math.sin(u * freq + phase) * amp * env;
            if (jitter > 0.01F) wave += (float) Math.sin(u * 29F + phase * 3.7F) * jitter * 2.2F;
            float y = curveY(x, cx, yBase, curveH, spanRef) + wave;
            if (i > 0) FcsDraw.line(g, px, py, x, y, 0.9F, col);
            px = x;
            py = y;
        }
    }

    /** Sleek chamfered translucent glass pill/card background + 1px optical border. */
    public static void glassCard(GuiGraphics g, float x0, float y0, float w, float h, int borderRgb, float borderA, float fillA) {
        float c = 2.5F;
        int bg = FcsDraw.argb(GLASS_BG, fillA);
        FcsDraw.quad(g, x0 + c, y0, x0 + w, y0, x0 + w, y0 + h - c, x0 + c, y0 + h - c, bg);
        FcsDraw.quad(g, x0, y0 + c, x0 + c, y0 + c, x0 + c, y0 + h, x0, y0 + h, bg);
        FcsDraw.triangle(g, x0, y0 + c, x0 + c, y0, x0 + c, y0 + c, bg);
        FcsDraw.quad(g, x0 + c, y0 + h - c, x0 + w - c, y0 + h - c, x0 + w - c, y0 + h, x0 + c, y0 + h, bg);
        FcsDraw.triangle(g, x0 + w - c, y0 + h - c, x0 + w, y0 + h - c, x0 + w - c, y0 + h, bg);
        int edge = FcsDraw.argb(borderRgb, borderA);
        FcsDraw.line(g, x0 + c, y0, x0 + w, y0, 0.8F, edge);
        FcsDraw.line(g, x0 + w, y0, x0 + w, y0 + h - c, 0.8F, edge);
        FcsDraw.line(g, x0 + w, y0 + h - c, x0 + w - c, y0 + h, 0.8F, edge);
        FcsDraw.line(g, x0 + w - c, y0 + h, x0, y0 + h, 0.8F, edge);
        FcsDraw.line(g, x0, y0 + h, x0, y0 + c, 0.8F, edge);
        FcsDraw.line(g, x0, y0 + c, x0 + c, y0, 0.8F, edge);
    }

    // ------------------------------------------------------------------ Peripheral Physical Phenomena (Plan F)

    /** Warm rising heat-shimmer waves along the far right visor edge when suit heat climbs. */
    public static void thermalEdgeShimmer(GuiGraphics g, int w, int h, float heatRatio, boolean overheated, float t, float alpha) {
        if (alpha <= 0.01F || (!overheated && heatRatio < 0.42F)) return;
        float intensity = overheated ? 0.85F + 0.15F * (float) Math.sin(t * 0.5F) : Mth.clamp((heatRatio - 0.42F) / 0.58F, 0F, 1F);
        int rgb = overheated ? RED : AMBER;
        int yStart = (int) (h * 0.24F), yEnd = h - 54;
        int steps = 20;
        for (int k = 0; k < 2; k++) {
            float a = (0.36F - k * 0.12F) * intensity * alpha;
            int col = FcsDraw.argb(rgb, a);
            int glow = FcsDraw.argb(rgb, a * 0.45F);
            float px = 0, py = 0;
            for (int i = 0; i <= steps; i++) {
                float u = i / (float) steps;
                float y = Mth.lerp(u, yStart, yEnd);
                float curve = (u - 0.5F) * (u - 0.5F) * 14F;
                float wave = (float) Math.sin(u * 10F + t * 0.22F + k * 1.5F) * 1.6F;
                float x = w - 5F - k * 4.5F - curve + wave;
                if (i > 0) {
                    FcsDraw.line(g, px, py, x, y, 3.2F, glow);
                    FcsDraw.line(g, px, py, x, y, 1.0F, col);
                }
                px = x;
                py = y;
            }
        }
    }

    /** Cyan-violet curved optical interference fringes along the far left visor edge on coherence drop / phase shift. */
    public static void quantumEdgeFringes(GuiGraphics g, int w, int h, float instability, boolean phaseActive, float t, float alpha) {
        if (alpha <= 0.01F || (!phaseActive && instability < 0.12F)) return;
        float intensity = phaseActive ? 0.9F : Mth.clamp(instability, 0F, 1F);
        int yStart = (int) (h * 0.18F), yEnd = h - 62;
        int steps = 22;
        for (int k = 0; k < 2; k++) {
            int rgb = k == 0 ? CYAN : VIOLET;
            float a = (0.34F - k * 0.10F) * intensity * alpha;
            int col = FcsDraw.argb(rgb, a);
            int glow = FcsDraw.argb(rgb, a * 0.45F);
            float px = 0, py = 0;
            for (int i = 0; i <= steps; i++) {
                float u = i / (float) steps;
                float y = Mth.lerp(u, yStart, yEnd);
                float curve = (u - 0.5F) * (u - 0.5F) * 14F;
                float wave = (float) Math.sin(u * 11F - t * 0.18F + k * 1.7F) * (1.5F + intensity);
                float x = 5F + k * 4.5F + curve + wave;
                if (i > 0) {
                    FcsDraw.line(g, px, py, x, y, 3.0F, glow);
                    FcsDraw.line(g, px, py, x, y, 1.0F, col);
                }
                px = x;
                py = y;
            }
        }
    }

    // ------------------------------------------------------------------ Top Curved Visor Brow

    /** Draws a subtle curved top visor arc and a centered translucent status pill. */
    public static void topBrowPill(GuiGraphics g, float cx, float y, int w, Component text, int rgb, float alpha) {
        if (alpha <= 0.01F) return;
        float half = Math.min(w * 0.40F, 175F);
        curvedHairline(g, cx, y + 11F, cx - half, cx + half, -8F, half, 0.9F, rgb, 0.28F * alpha, 24);
        float sc = 0.62F;
        float tw = AsciiHud.width(text, sc);
        float pw = tw + 12F, ph = 11F;
        glassCard(g, cx - pw / 2F, y, pw, ph, rgb, 0.55F * alpha, 0.62F * alpha);
        FcsDraw.text(g, AsciiHud.t(text), cx - tw / 2F, y + 2.2F, FcsDraw.argb(WHITE, 0.95F * alpha), sc);
    }

    // ------------------------------------------------------------------ Vector Skill Icons

    /** Draws a crisp 10x10 vector glyph representing a Nano or Quantum active skill inside a skill card. */
    public static void skillIcon(GuiGraphics g, SuitModuleType type, float cx, float cy, int rgb, float alpha) {
        int col = FcsDraw.argb(rgb, alpha);
        int dim = FcsDraw.argb(rgb, 0.35F * alpha);
        FcsDraw.circle(g, cx, cy, 5.5F, 0.7F, dim);
        if (type == SuitModuleType.MYOFIBER) {
            // Lightning dash bolt
            FcsDraw.triangle(g, cx + 1.0F, cy - 3.8F, cx - 2.6F, cy + 0.4F, cx + 0.4F, cy + 0.4F, col);
            FcsDraw.triangle(g, cx - 0.4F, cy - 0.4F, cx + 2.6F, cy - 0.4F, cx - 1.0F, cy + 3.8F, col);
        } else if (type == SuitModuleType.EMP_EMITTER) {
            // Radial EMP pulse core + arcs
            FcsDraw.fill(g, cx - 1.2F, cy - 1.2F, cx + 1.2F, cy + 1.2F, col);
            FcsDraw.arc(g, cx, cy, 3.6F, 0.4F, 2.7F, 0.9F, col);
            FcsDraw.arc(g, cx, cy, 3.6F, 3.5F, 5.8F, 0.9F, col);
        } else if (type == SuitModuleType.OPTICAL_CAMO) {
            // Stealth eye / optical lens
            FcsDraw.arc(g, cx, cy + 2.5F, 4.2F, -0.8F, 0.8F, 0.9F, col);
            FcsDraw.arc(g, cx, cy - 2.5F, 4.2F, (float) Math.PI - 0.8F, (float) Math.PI + 0.8F, 0.9F, col);
            FcsDraw.fill(g, cx - 1.0F, cy - 1.0F, cx + 1.0F, cy + 1.0F, col);
        } else if (type == SuitModuleType.KINETIC_MESH) {
            // Hexagonal kinetic armor shield
            float r = 3.5F;
            for (int i = 0; i < 6; i++) {
                float a0 = i * (float) (Math.PI / 3), a1 = (i + 1) * (float) (Math.PI / 3);
                FcsDraw.line(g, cx + (float) Math.sin(a0) * r, cy - (float) Math.cos(a0) * r,
                    cx + (float) Math.sin(a1) * r, cy - (float) Math.cos(a1) * r, 0.9F, col);
            }
        } else if (type == SuitModuleType.RECON_CLOUD) {
            // Radar sweep + target ping
            FcsDraw.arc(g, cx, cy, 3.4F, 0F, 4.5F, 0.8F, col);
            FcsDraw.line(g, cx, cy, cx + 2.6F, cy - 2.2F, 0.9F, col);
            FcsDraw.fill(g, cx + 1.8F, cy - 2.8F, cx + 3.2F, cy - 1.4F, col);
        } else if (type == SuitModuleType.PHASE_JUMP_DRIVE) {
            // Twin spatial-rift diamonds
            FcsDraw.triangle(g, cx + 1.4F, cy - 3.4F, cx - 0.6F, cy, cx + 3.4F, cy, col);
            FcsDraw.triangle(g, cx - 0.6F, cy, cx + 3.4F, cy, cx + 1.4F, cy + 3.4F, col);
            FcsDraw.line(g, cx - 2.2F, cy - 2.6F, cx - 3.8F, cy, 0.8F, col);
            FcsDraw.line(g, cx - 3.8F, cy, cx - 2.2F, cy + 2.6F, 0.8F, col);
        } else if (type == SuitModuleType.PHASE_MODULATOR) {
            // Phase sine wave inside ring
            FcsDraw.circle(g, cx, cy, 3.5F, 0.8F, col);
            FcsDraw.line(g, cx - 2.8F, cy, cx - 1.0F, cy - 2.0F, 0.9F, col);
            FcsDraw.line(g, cx - 1.0F, cy - 2.0F, cx + 1.0F, cy + 2.0F, 0.9F, col);
            FcsDraw.line(g, cx + 1.0F, cy + 2.0F, cx + 2.8F, cy, 0.9F, col);
        } else if (type == SuitModuleType.DEFLECTOR) {
            // Curved deflector barrier
            FcsDraw.arc(g, cx, cy, 3.8F, -1.2F, 1.2F, 1.1F, col);
            FcsDraw.arc(g, cx, cy, 2.0F, -1.0F, 1.0F, 0.9F, col);
        } else if (type == SuitModuleType.BEHAVIOR_PREDICTOR) {
            // Time dilation clock hands
            FcsDraw.circle(g, cx, cy, 3.6F, 0.8F, col);
            FcsDraw.line(g, cx, cy, cx, cy - 2.6F, 0.9F, col);
            FcsDraw.line(g, cx, cy, cx + 2.2F, cy + 1.0F, 0.9F, col);
        } else if (type == SuitModuleType.ENTANGLEMENT_RELAY) {
            // Twin entangled nodes
            FcsDraw.fill(g, cx - 3.2F, cy - 1.2F, cx - 1.2F, cy + 0.8F, col);
            FcsDraw.fill(g, cx + 1.2F, cy - 0.8F, cx + 3.2F, cy + 1.2F, col);
            FcsDraw.line(g, cx - 1.5F, cy - 0.2F, cx + 1.5F, cy + 0.2F, 0.9F, col);
        } else {
            // Default / ARC discharge diamond
            FcsDraw.triangle(g, cx, cy - 3.2F, cx - 2.6F, cy, cx + 2.6F, cy, col);
            FcsDraw.triangle(g, cx - 2.6F, cy, cx + 2.6F, cy, cx, cy + 3.2F, col);
        }
    }

    /** Localized short skill name (replaces cryptic 3-4 letter acronyms). */
    public static Component skillName(String key) {
        SuitModuleType type = SuitModuleType.byId(key);
        if (type == null) return Component.literal("----");
        return switch (type) {
            case MYOFIBER -> Component.translatable("hud.mio_icif.skill.dash");
            case EMP_EMITTER -> Component.translatable("hud.mio_icif.skill.emp");
            case OPTICAL_CAMO -> Component.translatable("hud.mio_icif.skill.camo");
            case KINETIC_MESH -> Component.translatable("hud.mio_icif.skill.mesh");
            case RECON_CLOUD -> Component.translatable("hud.mio_icif.skill.recon");
            case PHASE_JUMP_DRIVE -> Component.translatable("hud.mio_icif.skill.blink");
            case PHASE_MODULATOR -> Component.translatable("hud.mio_icif.skill.phase");
            case DEFLECTOR -> Component.translatable("hud.mio_icif.skill.field");
            case BEHAVIOR_PREDICTOR -> Component.translatable("hud.mio_icif.skill.dilation");
            case ENTANGLEMENT_RELAY -> Component.translatable("hud.mio_icif.skill.volley");
            default -> Component.translatable(type.nameKey());
        };
    }

    /** Localized short synergy name (replaces cryptic TNL/THR/VLY codes). */
    public static Component synergyName(String id, String fallbackCode) {
        if (id == null || id.isEmpty()) return Component.literal(fallbackCode);
        String key = "hud.mio_icif.synergy." + id;
        return net.minecraft.locale.Language.getInstance().has(key)
            ? Component.translatable(key)
            : Component.literal(fallbackCode);
    }
}
