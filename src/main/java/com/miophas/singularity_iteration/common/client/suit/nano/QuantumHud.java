// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.client.suit.nano;

import com.miophas.singularity_iteration.common.client.suit.AsciiHud;
import com.miophas.singularity_iteration.common.client.suit.FcsClientConfig;
import com.miophas.singularity_iteration.common.client.suit.FcsDraw;
import com.miophas.singularity_iteration.common.client.suit.ManeuverModeClient;
import com.miophas.singularity_iteration.common.client.suit.VisorHudPrimitives;
import com.miophas.singularity_iteration.common.suit.nano.NanoPackets;
import com.miophas.singularity_iteration.common.suit.quantum.QuantumActions;
import com.miophas.singularity_iteration.common.suit.quantum.QuantumPackets;
import com.miophas.singularity_iteration.common.suit.quantum.QuantumSynergy;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.Mth;

/**
 * Curved Helmet-Visor Quantum HUD (Plan E + F):
 * <ul>
 *   <li><b>100% open center crosshair</b>: renders a curved bottom visor console arched cleanly above
 *       survival bars; when paired with an active Nano core, nests cleanly as a thin secondary arc
 *       above the Nano wings with zero overlap.</li>
 *   <li><b>Left curved wing</b>: Quantum coherence bar with oscilloscope phosphor trail and live
 *       standing-wave harmonic ripple (smooth when coherent, jittering as decoherence approaches).</li>
 *   <li><b>Right curved wing</b>: QB compute bandwidth bar + localized Synergy pills (replaces cryptic
 *       3-letter codes).</li>
 *   <li><b>Peripheral physics</b>: left visor edge optical interference fringes on coherence drop /
 *       phase shift, plus top curved brow status pill for active timers.</li>
 * </ul>
 */
public final class QuantumHud {
    private QuantumHud() {}

    static final int CYAN = VisorHudPrimitives.CYAN, VIOLET = VisorHudPrimitives.VIOLET,
        RED = VisorHudPrimitives.RED, PHASE = 0xD06BFF, FIELD = 0x5AB8FF, TIME = 0xC77DFF, GOLD = VisorHudPrimitives.GOLD;
    private static float alpha = 1, lastCoherence = -1;
    private static int lastChange;
    private static final VisorHudPrimitives.PhosphorTrail COH_TRAIL = new VisorHudPrimitives.PhosphorTrail();
    private static final float[] PARALLAX = new float[2];

    public static void render(GuiGraphics g, DeltaTracker delta) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || mc.options.hideGui || !FcsClientConfig.quantumHud() || !FcsClientConfig.hudEnabled()) return;
        QuantumPackets.CoreSync q = QuantumClient.sync;
        if (q == null || !q.online() && q.phaseTicks() <= 0 && q.dilationTicks() <= 0 && !q.field()) return;
        float partial = delta.getGameTimeDeltaPartialTick(false);
        float t = NanoClient.ticks + partial;
        int w = g.guiWidth(), h = g.guiHeight();
        screenCues(g, q, t, w, h, partial);
        if (!q.online()) { g.flush(); return; }
        var keys = NanoClient.sync;
        boolean dual = keys != null && keys.online();

        boolean busy = q.coherence() < q.max() - 0.5F || q.decoherent() > 0 || q.phaseTicks() > 0 || q.field() || q.dilationTicks() > 0
            || t - NanoClient.usedAt[0] < 40 || t - NanoClient.usedAt[1] < 40 || t - NanoClient.deniedAt[0] < 40 || t - NanoClient.deniedAt[1] < 40;
        if (Math.abs(q.coherence() - lastCoherence) > 0.2F) lastChange = NanoClient.ticks;
        lastCoherence = q.coherence();
        float target = busy || NanoClient.ticks - lastChange < 60 ? 1F : 0F;
        alpha += (target - alpha) * 0.08F;
        float a = alpha;
        if (a < 0.02F) { g.flush(); return; }

        VisorHudPrimitives.updateParallax(player, partial, PARALLAX);
        float glitch = QuantumClient.glitch(partial);
        float jx = 0F;
        if (glitch > 0 && new Random((long) (t * 3)).nextFloat() < glitch * 0.4F) {
            jx = (new Random((long) (t * 7)).nextFloat() - 0.5F) * 5F * glitch;
        }

        float cx = w / 2F + PARALLAX[0] + jx;
        // Solo Quantum sits at h - 62; Dual-Core nests cleanly as a thin secondary arc 10px above Nano!
        float yBase = (dual ? h - 72F : h - 62F) + PARALLAX[1];
        float spanRef = Math.min(w * 0.44F, 190F);
        float curveH = dual ? 11.5F : 13F;
        float halfWingOuter = Math.min(w * 0.36F, 154F);
        float lx0 = Math.max(12F, cx - halfWingOuter + (dual ? 10F : 0F)), lx1 = cx - 64F;
        float rx0 = cx + 64F, rx1 = Math.min(w - 12F, cx + halfWingOuter - (dual ? 10F : 0F));
        float barW = dual ? 1.8F : 3.0F;

        if (!dual) {
            VisorHudPrimitives.curvedHairline(g, cx, yBase + 11F, lx0 - 6F, rx1 + 6F, curveH, spanRef, 0.8F, VIOLET, 0.26F * a, 32);
        }

        // ---- Left Curved Wing: Quantum Coherence + Phosphor Trail + Live Standing-Wave Ripple
        float shownCoh = NanoClient.SHOWN_COHERENCE.get(partial, q.coherence());
        float maxCoh = Math.max(1F, q.max());
        float fill = Mth.clamp(shownCoh / maxCoh, 0F, 1F);
        float trailFill = Mth.clamp(COH_TRAIL.sample(shownCoh) / maxCoh, fill, 1F);
        boolean dec = q.decoherent() > 0;
        int cohCol = dec ? RED : q.max() > 100 ? GOLD : VIOLET;

        VisorHudPrimitives.curvedBar(g, cx, yBase, lx0, lx1, curveH, spanRef, barW, cohCol, 0.16F * a, 16);
        float lxFill = Mth.lerp(fill, lx1, lx0);
        float lxTrail = Mth.lerp(trailFill, lx1, lx0);
        if (!dec && trailFill > fill + 0.006F) {
            VisorHudPrimitives.curvedBar(g, cx, yBase, lxTrail, lxFill, curveH, spanRef, barW, VisorHudPrimitives.WHITE, 0.65F * a, 8);
        }
        VisorHudPrimitives.curvedBar(g, cx, yBase, lxFill, lx1, curveH, spanRef, barW, cohCol, (dec ? 0.65F : 0.92F) * a, 16);

        // Live harmonic standing wave riding along the filled coherence arc (jitters when coherence drops!)
        if (fill > 0.05F) {
            float instability = dec ? 1F : Mth.clamp(1F - fill, 0F, 1F);
            VisorHudPrimitives.curvedWave(g, cx, yBase, lxFill, lx1, curveH, spanRef,
                dual ? 1.1F : 1.7F, 18F, t * 0.22F, instability * 0.7F, dec ? RED : CYAN, 0.72F * a, 18);
        }
        // Plan F peripheral left-edge quantum interference fringes
        VisorHudPrimitives.quantumEdgeFringes(g, w, h, dec ? 1F : (1F - fill), q.phaseTicks() > 0, t, a * FcsClientConfig.quantumFx());

        float lyLbl = VisorHudPrimitives.curveY(lx0 + 4F, cx, yBase, curveH, spanRef) - (dual ? 8.5F : 9.5F);
        AsciiHud.left(g, dec ? "hud.mio_icif.quantum.decoherent" : "hud.mio_icif.quantum.coherence",
            lx0 + (dual ? 30F : 2F), lyLbl, FcsDraw.argb(cohCol, 0.92F * a), dual ? 0.52F : 0.60F,
            String.format(Locale.ROOT, "%d", Math.round(shownCoh)));

        // ---- Right Curved Wing: Compute Bandwidth QB + Localized Synergy Pills
        renderRightWing(g, cx, yBase, rx0, rx1, curveH, spanRef, barW, dual, t, a);

        // ---- Skill Cards (solo Quantum) + Blink Diamond Charge Pips (both solo & dual)
        if (keys != null && keys.quantum()) {
            float chipY = h - 64F + PARALLAX[1];
            if (!dual) {
                String vk = NanoClient.keyLabel(NanoClient.PRIMARY_KEY, "V"), bk = NanoClient.keyLabel(NanoClient.SECONDARY_KEY, "B");
                NanoHud.chip(g, cx - 31F, chipY, 0, keys.primaryKey(), keys.primaryCooldown(), keys.primaryCooldownMax(), keys, t, a, vk);
                NanoHud.chip(g, cx + 31F, chipY, 1, keys.secondaryKey(), keys.secondaryCooldown(), keys.secondaryCooldownMax(), keys, t, a, bk);
            }
            chargePips(g, q, keys.primaryKey(), cx - 31F, chipY - 13.5F, a);
            chargePips(g, q, keys.secondaryKey(), cx + 31F, chipY - 13.5F, a);
        }

        // ---- Top Curved Visor Brow & Combo Banner
        renderTopBrow(g, cx, w, q, a);
        comboBanner(g, w, h, t);
        g.flush();
    }

    private static void renderRightWing(GuiGraphics g, float cx, float yBase, float rx0, float rx1, float curveH,
                                        float spanRef, float barW, boolean dual, float t, float a) {
        int load = QuantumClient.bandwidthLoad, budget = Math.max(1, QuantumClient.bandwidthBudget);
        boolean sat = load > budget;
        float pulse = sat ? 0.6F + 0.4F * (float) Math.sin(t * 0.6F) : 1F;
        float fill = Mth.clamp(load / (float) budget, 0F, 1F);
        int bwCol = sat ? RED : fill > 0.85F ? VisorHudPrimitives.AMBER : CYAN;

        if (load > 0) {
            VisorHudPrimitives.curvedBar(g, cx, yBase, rx0, rx1, curveH, spanRef, barW, bwCol, 0.16F * a, 14);
            VisorHudPrimitives.curvedBar(g, cx, yBase, rx0, Mth.lerp(fill, rx0, rx1), curveH, spanRef, barW, bwCol, 0.90F * a * pulse, 14);
            float ryLbl = VisorHudPrimitives.curveY(rx1 - 4F, cx, yBase, curveH, spanRef) - (dual ? 8.5F : 9.5F);
            AsciiHud.right(g, sat ? "hud.mio_icif.quantum.saturated" : "hud.mio_icif.quantum.bandwidth",
                rx1 - (dual ? 24F : 2F), ryLbl, FcsDraw.argb(bwCol, 0.90F * a * pulse), dual ? 0.52F : 0.58F, load, budget);
        }

        // Compact localized Synergy pills nestled cleanly above the right wing
        List<String> links = QuantumClient.activeLinks;
        if (!links.isEmpty()) {
            float sx = rx0 + 2F;
            float sy = VisorHudPrimitives.curveY(rx0 + 18F, cx, yBase, curveH, spanRef) - (dual ? 11F : 13F);
            float sc = 0.48F;
            for (int i = 0; i < links.size() && i < 3; i++) {
                QuantumSynergy syn = QuantumSynergy.byId(links.get(i));
                Component name = VisorHudPrimitives.synergyName(links.get(i), syn != null ? syn.code : "LNK");
                int col = syn != null ? syn.color : 0x9EF0FF;
                float pw = AsciiHud.width(name, sc) + 5F;
                if (sx + pw > rx1 - 24F) break;
                VisorHudPrimitives.glassCard(g, sx, sy, pw, 8.5F, col, 0.72F * a, 0.58F * a);
                FcsDraw.text(g, AsciiHud.t(name), sx + 2.5F, sy + 1.5F, FcsDraw.argb(col, 0.95F * a), sc);
                sx += pw + 2.5F;
            }
        }
    }

    private static void renderTopBrow(GuiGraphics g, float cx, int w, QuantumPackets.CoreSync q, float a) {
        MutableComponent line = null;
        int accent = VIOLET;
        if (q.decoherent() > 0) {
            line = appendSep(line, Component.translatable("hud.mio_icif.quantum.decoherent"));
            accent = RED;
        }
        if (q.phaseTicks() > 0) {
            line = appendSep(line, Component.translatable("hud.mio_icif.quantum.phase",
                String.format(Locale.ROOT, "%.1f", q.phaseTicks() / 20F)));
            if (accent == VIOLET) accent = PHASE;
        }
        if (q.field()) {
            line = appendSep(line, Component.translatable("hud.mio_icif.quantum.field",
                String.format(Locale.ROOT, "%.1f", q.fieldTicks() / 20F)));
            if (accent == VIOLET) accent = FIELD;
        }
        if (q.dilationTicks() > 0) {
            line = appendSep(line, Component.translatable("hud.mio_icif.quantum.dilation",
                String.format(Locale.ROOT, "%.1f", q.dilationTicks() / 20F)));
            if (accent == VIOLET) accent = TIME;
        }
        if (line == null) return;
        float topY = (ManeuverModeClient.isFlying() ? 26F : 7F) + (NanoHud.nanoTopBrowDrawn ? 13F : 0F);
        VisorHudPrimitives.topBrowPill(g, cx, topY, w, line, accent, a);
    }

    private static MutableComponent appendSep(MutableComponent base, Component part) {
        if (base == null) return part.copy();
        return base.append(Component.literal("  |  ")).append(part);
    }

    private static final String[] COMBO_NAMES = {"hud.mio_icif.quantum.combo.tunnel", "hud.mio_icif.quantum.combo.volley", "hud.mio_icif.quantum.combo.barrage"};
    private static final int[] COMBO_COLORS = {0xB98CFF, 0xFFD27A, 0x5AB8FF};

    /** Combo banner: the name slides in on a slanted light bar and fades over 1.5 s. */
    private static void comboBanner(GuiGraphics g, int w, int h, float t) {
        int kind = QuantumClient.comboKind;
        float age = t - QuantumClient.comboAt;
        if (kind < 0 || kind >= COMBO_NAMES.length || age < 0 || age > 30) return;
        float in = Mth.clamp(age / 4F, 0, 1), out = Mth.clamp((30 - age) / 8F, 0, 1), a = in * out;
        int col = COMBO_COLORS[kind];
        float cx = w / 2F, y = h * 0.24F;
        float slide = (1 - in) * 40;
        float bw = 120;
        FcsDraw.quad(g, cx - bw / 2 - 8 + slide, y + 10, cx - bw / 2 + slide, y - 2, cx + bw / 2 + 8 + slide, y - 2, cx + bw / 2 + slide, y + 10,
            FcsDraw.argb(col, 0.22F * a));
        FcsDraw.line(g, cx - bw / 2 + slide, y - 2, cx + bw / 2 + 8 + slide, y - 2, 1F, FcsDraw.argb(col, 0.9F * a));
        FcsDraw.line(g, cx - bw / 2 - 8 + slide, y + 10, cx + bw / 2 + slide, y + 10, 1F, FcsDraw.argb(col, 0.9F * a));
        AsciiHud.centred(g, "hud.mio_icif.quantum.combo", cx + slide, y + 1, FcsDraw.argb(0xFFFFFF, a), 0.75F,
            Component.translatable(COMBO_NAMES[kind]));
    }

    private static void chargePips(GuiGraphics g, QuantumPackets.CoreSync q, String key, float x, float y, float a) {
        if (QuantumActions.kind(com.miophas.singularity_iteration.common.suit.UnitRef.byKey(key)) != QuantumActions.Kind.BLINK || q.blinkMax() < 2) return;
        for (int i = 0; i < q.blinkMax(); i++) {
            float px = x - (q.blinkMax() - 1) * 3.5F + i * 7F;
            boolean full = i < q.blinkCharges();
            int c = FcsDraw.argb(full ? VIOLET : 0x3A3060, (full ? 0.95F : 0.7F) * a);
            FcsDraw.triangle(g, px, y - 2.2F, px - 2.2F, y, px + 2.2F, y, c);
            FcsDraw.triangle(g, px - 2.2F, y, px + 2.2F, y, px, y + 2.2F, c);
        }
        if (q.blinkCharges() < q.blinkMax() && q.blinkRecharge() > 0) {
            float k = 1 - q.blinkRecharge() / (float) Math.max(1, q.blinkRechargeMax());
            g.fill((int) (x - 9), (int) (y + 3), (int) (x - 9 + 18 * k), (int) (y + 4), FcsDraw.argb(VIOLET, 0.85F * a));
        }
    }

    // ------------------------------------------------------------------ full-screen cues

    private static void screenCues(GuiGraphics g, QuantumPackets.CoreSync q, float t, int w, int h, float partial) {
        float f = FcsClientConfig.quantumFx() * FcsClientConfig.flashScale();
        float sinceBlink = t - QuantumClient.blinkAt;
        if (sinceBlink >= 0 && sinceBlink < 7) {
            float k = 1 - sinceBlink / 7F;
            g.fill(0, 0, w, h, FcsDraw.argb(0xE8E0FF, 0.24F * k * k * f));
            Random r = new Random(QuantumClient.blinkAt * 31L);
            float cx = w / 2F, cy = h / 2F;
            for (int i = 0; i < 42; i++) {
                double ang = r.nextDouble() * Math.PI * 2;
                float r0 = Math.max(w, h) * (0.14F + 0.5F * (1 - k) + 0.1F * r.nextFloat()), r1 = r0 + 20 + 50 * r.nextFloat();
                FcsDraw.line(g, cx + (float) Math.cos(ang) * r0, cy + (float) Math.sin(ang) * r0, cx + (float) Math.cos(ang) * r1,
                    cy + (float) Math.sin(ang) * r1, 0.8F, FcsDraw.argb(i % 3 == 0 ? VIOLET : 0xF2F0FF, 0.5F * k * f));
            }
        }
        if (q.phaseTicks() > 0) {
            float k = Mth.clamp(q.phaseTicks() / 4F, 0, 1) * Mth.clamp((t - QuantumClient.phaseAt) / 4F, 0, 1);
            for (int i = 0; i < 16; i++) {
                float band = 0.5F + 0.5F * (float) Math.sin(i * 0.9F - t * 0.8F);
                int c = FcsDraw.argb(i % 2 == 0 ? PHASE : CYAN, 0.07F * band * k * (1 - i / 16F) * f);
                int e = i * 2;
                g.fill(e, e, w - e, e + 1, c);
                g.fill(e, h - e - 1, w - e, h - e, c);
                g.fill(e, e, e + 1, h - e, c);
                g.fill(w - e - 1, e, w - e, h - e, c);
            }
        }
        float dil = 0;
        if (q.dilationTicks() > 0) dil = Mth.clamp((t - QuantumClient.dilationOnAt) / 8F, 0, 1) * Mth.clamp(q.dilationTicks() / 8F, 0, 1);
        if (dil > 0) {
            for (int i = 0; i < 24; i += 2) {
                int c = FcsDraw.argb(0x2A1450, 0.10F * dil * (1 - i / 24F) * f);
                g.fill(i, i, w - i, i + 1, c);
                g.fill(i, h - i - 1, w - i, h - i, c);
                g.fill(i, i, i + 1, h - i, c);
                g.fill(w - i - 1, i, w - i, h - i, c);
            }
            g.fill(0, 0, w, h, FcsDraw.argb(0x1A1030, 0.11F * dil * f));
            float cx = w / 2F, cy = h / 2F;
            for (int k = 0; k < 3; k++) {
                float u = ((t * 0.012F) + k / 3F) % 1F;
                FcsDraw.circle(g, cx, cy, 30 + u * Math.max(w, h) * 0.6F, 1.2F, FcsDraw.argb(TIME, 0.22F * (1 - u) * dil * f));
            }
        }
        float gl = QuantumClient.glitch(partial);
        if (gl > 0.02F) {
            Random r = new Random((long) (t * 2.5F) * 7919L);
            int bands = (int) (3 + 9 * gl);
            for (int i = 0; i < bands; i++) {
                int y = r.nextInt(Math.max(1, h)), bh = 1 + r.nextInt(3 + (int) (6 * gl));
                int off = (int) ((r.nextFloat() - 0.5F) * 30 * gl);
                g.fill(Math.max(0, off), y, Math.min(w, w + off), y + bh, FcsDraw.argb(r.nextBoolean() ? 0xFF2050 : 0x20E0FF, 0.10F * gl));
            }
            for (int i = 0; i < 40 * gl; i++) {
                int x = r.nextInt(Math.max(1, w)), y = r.nextInt(Math.max(1, h)), s = 1 + r.nextInt(3);
                g.fill(x, y, x + s, y + s, FcsDraw.argb(r.nextBoolean() ? 0xFFFFFF : RED, 0.25F * gl));
            }
            g.fill(0, 0, 2, h, FcsDraw.argb(0xFF2050, 0.25F * gl));
            g.fill(w - 2, 0, w, h, FcsDraw.argb(0x20E0FF, 0.25F * gl));
        }
    }
}
