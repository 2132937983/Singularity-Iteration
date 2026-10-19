// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.client.suit.nano;

import com.miophas.singularity_iteration.common.client.suit.FcsClientConfig;
import com.miophas.singularity_iteration.common.client.suit.FcsDraw;
import com.miophas.singularity_iteration.common.suit.nano.NanoPackets;
import com.miophas.singularity_iteration.common.suit.quantum.QuantumActions;
import com.miophas.singularity_iteration.common.suit.quantum.QuantumPackets;
import java.util.Locale;
import java.util.Random;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;

/**
 * Quantum core HUD around the crosshair (0.1.10.0).
 *
 * <pre>
 *              . - ~ ~ - .        coherence ring (cyan to violet), cost ticks of V and B,
 *          .'   . . . .   '.      an interference ring turning inside it, a comet at the
 *         :   .           .   :   filling edge while it regains
 *         :   .     +     .   :
 *          '.   . . . .   .'      DECOHERENCE: red ring, glitch tearing, recovery bar
 *     [V BLNK]  ' - ~ - '  [B PHAS]   (chips only without a nano core; blink charge pips)
 *          PHASE  ████░░░  0.6s     timers of running actions
 * </pre>
 * Full-screen cues: blink flash and streaks, phase interference edges, time dilation vignette and
 * ripples, the decoherence glitch (client option decoherence_glitch tones it down).
 */
public final class QuantumHud {
    private QuantumHud() {}

    static final int CYAN = 0x5CEBFF, VIOLET = 0xB98CFF, RED = 0xFF3B30, PHASE = 0xD06BFF, FIELD = 0x5AB8FF, TIME = 0xC77DFF, GOLD = 0xFFD27A;
    private static float alpha = 1, lastCoherence = -1;
    private static int lastChange;

    public static void render(GuiGraphics g, DeltaTracker delta) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || mc.options.hideGui || !FcsClientConfig.quantumHud() || !FcsClientConfig.hudEnabled()) return;
        QuantumPackets.CoreSync q = QuantumClient.sync;
        if (q == null || !q.online() && q.phaseTicks() <= 0 && q.dilationTicks() <= 0 && !q.field()) return;
        float partial = delta.getGameTimeDeltaPartialTick(false);
        float t = NanoClient.ticks + partial;
        int w = g.guiWidth(), h = g.guiHeight();
        float cx = w / 2F, cy = h / 2F;
        screenCues(g, q, t, w, h, partial);
        if (!q.online()) { g.flush(); return; }
        var keys = NanoClient.sync;

        boolean busy = q.coherence() < q.max() - 0.5F || q.decoherent() > 0 || q.phaseTicks() > 0 || q.field() || q.dilationTicks() > 0
            || t - NanoClient.usedAt[0] < 40 || t - NanoClient.usedAt[1] < 40 || t - NanoClient.deniedAt[0] < 40 || t - NanoClient.deniedAt[1] < 40;
        if (Math.abs(q.coherence() - lastCoherence) > 0.2F) lastChange = NanoClient.ticks;
        lastCoherence = q.coherence();
        float target = busy || NanoClient.ticks - lastChange < 60 ? 1F : 0.4F;
        alpha += (target - alpha) * 0.08F;
        float a = alpha;
        float glitch = QuantumClient.glitch(partial);
        // glitch: the whole ring jumps sideways for a frame now and then
        float jx = 0;
        if (glitch > 0 && new Random((long) (t * 3)).nextFloat() < glitch * 0.4F) jx = (new Random((long) (t * 7)).nextFloat() - 0.5F) * 6 * glitch;
        coherenceRing(g, q, keys, cx + jx, cy, t, a, glitch, NanoClient.SHOWN_COHERENCE.get(partial, q.coherence()));
        bandwidth(g, cx + jx, cy - 47, t, a);                      // 0.1.10.1 compute bandwidth
        linkChips(g, cx - 52, cy - 14, t, a);                      // 0.1.10.1 working synergies
        comboBanner(g, w, h, t);                                   // 0.1.10.1 chain combos

        // chips: only without a nano core (the nano HUD draws them otherwise)
        if (keys != null && !keys.online() && keys.quantum()) {
            String vk = NanoClient.keyLabel(NanoClient.PRIMARY_KEY, "V"), bk = NanoClient.keyLabel(NanoClient.SECONDARY_KEY, "B");
            NanoHud.chip(g, cx - 30, cy + 26, 0, keys.primaryKey(), keys.primaryCooldown(), keys.primaryCooldownMax(), keys, t, a, vk);
            NanoHud.chip(g, cx + 30, cy + 26, 1, keys.secondaryKey(), keys.secondaryCooldown(), keys.secondaryCooldownMax(), keys, t, a, bk);
            chargePips(g, q, keys.primaryKey(), cx - 30, cy + 19, a);
            chargePips(g, q, keys.secondaryKey(), cx + 30, cy + 19, a);
        }
        timers(g, q, cx, cy + 40, t, a);
        g.flush();
    }

    // ------------------------------------------------------------------ 0.1.10.1 bandwidth, links, combos

    /** Bandwidth meter: QB load over budget in 12 segments; above the budget it turns red and reads SATURATED. */
    private static void bandwidth(GuiGraphics g, float cx, float y, float t, float a) {
        int load = QuantumClient.bandwidthLoad, budget = Math.max(1, QuantumClient.bandwidthBudget);
        if (load <= 0) return;
        boolean sat = load > budget;
        float pulse = sat ? 0.6F + 0.4F * (float) Math.sin(t * 0.6F) : 1F;
        int segs = 12;
        float w = 40, x0 = cx - w / 2, sw = w / segs;
        float fill = Math.min(1F, load / (float) budget);
        for (int i = 0; i < segs; i++) {
            float k = (i + 0.5F) / segs;
            int col = sat ? RED : k > 0.85F ? 0xFFB347 : CYAN;
            g.fill((int) (x0 + i * sw), (int) y, (int) (x0 + (i + 1) * sw - 1), (int) y + 2, FcsDraw.argb(col, (k <= fill ? 0.85F * pulse : 0.15F) * a));
        }
        String label = sat ? String.format(java.util.Locale.ROOT, "SATURATED %d/%d QB", load, budget) : String.format(java.util.Locale.ROOT, "QB %d/%d", load, budget);
        FcsDraw.textCentered(g, label, cx, y - 6, FcsDraw.argb(sat ? RED : 0x9FDFFF, 0.8F * a * pulse), 0.45F);
    }

    /** Working synergies: a small chip per link with its code, in its colour, with a slow light sweep. */
    private static void linkChips(GuiGraphics g, float x, float y, float t, float a) {
        java.util.List<String> links = QuantumClient.activeLinks;
        for (int i = 0; i < links.size() && i < 4; i++) {
            var s = com.miophas.singularity_iteration.common.suit.quantum.QuantumSynergy.byId(links.get(i));
            String code = s != null ? s.code : "LNK";
            int col = s != null ? s.color : 0x9EF0FF;
            float yy = y + i * 9;
            g.fill((int) x - 12, (int) yy, (int) x + 10, (int) yy + 7, FcsDraw.argb(0x060A12, 0.55F * a));
            FcsDraw.line(g, x - 12, yy, x - 12, yy + 7, 1.2F, FcsDraw.argb(col, 0.95F * a));
            float sweep = ((t * 0.04F + i * 0.3F) % 1F);
            g.fill((int) (x - 11 + 21 * sweep), (int) yy, (int) (x - 10 + 21 * sweep), (int) yy + 7, FcsDraw.argb(col, 0.35F * a));
            FcsDraw.text(g, code, x - 9, yy + 1.5F, FcsDraw.argb(col, 0.95F * a), 0.45F);
        }
    }

    private static final String[] COMBO_NAMES = {"QUANTUM TUNNEL", "FROZEN VOLLEY", "STATIC BARRAGE"};
    private static final int[] COMBO_COLORS = {0xB98CFF, 0xFFD27A, 0x5AB8FF};

    /** Combo banner: the name slides in on a slanted light bar and fades over 1.5 s. */
    private static void comboBanner(GuiGraphics g, int w, int h, float t) {
        int kind = QuantumClient.comboKind;
        float age = t - QuantumClient.comboAt;
        if (kind < 0 || kind >= COMBO_NAMES.length || age < 0 || age > 30) return;
        float in = Mth.clamp(age / 4F, 0, 1), out = Mth.clamp((30 - age) / 8F, 0, 1), a = in * out;
        int col = COMBO_COLORS[kind];
        float cx = w / 2F, y = h * 0.28F;
        float slide = (1 - in) * 40;
        float bw = 120;
        FcsDraw.quad(g, cx - bw / 2 - 8 + slide, y + 10, cx - bw / 2 + slide, y - 2, cx + bw / 2 + 8 + slide, y - 2, cx + bw / 2 + slide, y + 10,
            FcsDraw.argb(col, 0.22F * a));
        FcsDraw.line(g, cx - bw / 2 + slide, y - 2, cx + bw / 2 + 8 + slide, y - 2, 1F, FcsDraw.argb(col, 0.9F * a));
        FcsDraw.line(g, cx - bw / 2 - 8 + slide, y + 10, cx + bw / 2 + slide, y + 10, 1F, FcsDraw.argb(col, 0.9F * a));
        FcsDraw.textCentered(g, "COMBO // " + COMBO_NAMES[kind], cx + slide, y + 1, FcsDraw.argb(0xFFFFFF, a), 0.7F);
    }

    // ------------------------------------------------------------------ coherence ring

    /** @param shown the coherence to draw (0.1.10.8: eased toward the synced value, see HudLerp) */
    private static void coherenceRing(GuiGraphics g, QuantumPackets.CoreSync q, NanoPackets.CoreSync keys, float cx, float cy, float t, float a,
                                      float glitch, float shown) {
        float r = 24;
        int segs = 36;
        float fill = Mth.clamp(shown / Math.max(1, q.max()), 0, 1);
        boolean dec = q.decoherent() > 0;
        float step = (float) (Math.PI * 2 / segs);
        for (int i = 0; i < segs; i++) {
            float a0 = i * step, a1 = a0 + step * 0.7F;
            float k = (i + 0.5F) / segs;
            int col = dec ? RED : lerp(CYAN, VIOLET, k);
            boolean on = k <= fill;
            float alphaSeg = on ? 0.88F : 0.13F;
            if (dec) alphaSeg = (0.25F + 0.35F * (float) Math.abs(Math.sin(t * 0.9F + i))) * (on ? 1 : 0.6F);
            FcsDraw.arc(g, cx, cy, r, a0, a1, 1.8F, FcsDraw.argb(col, alphaSeg * a));
        }
        // interference ring: two dashed rings turning against each other, their overlaps brighten
        for (int ring = 0; ring < 2; ring++) {
            float rr = r - 3.2F - ring * 0.9F, spin = (ring == 0 ? 1 : -1) * t * 0.035F;
            for (int i = 0; i < 24; i++) {
                float a0 = spin + i * (float) (Math.PI * 2 / 24);
                float beat = 0.5F + 0.5F * (float) Math.sin(i * 1.7F + t * 0.21F * (ring == 0 ? 1 : -1.3F));
                FcsDraw.arc(g, cx, cy, rr, a0, a0 + 0.11F, 0.7F, FcsDraw.argb(dec ? RED : ring == 0 ? CYAN : VIOLET, (0.12F + 0.3F * beat) * a));
            }
        }
        // the comet at the filling edge while the coherence regains
        if (q.regenerating() && fill < 1) {
            float ang = fill * (float) Math.PI * 2;
            for (int k = 0; k < 5; k++) {
                float back = ang - k * 0.05F;
                float x = cx + (float) Math.sin(back) * r, y = cy - (float) Math.cos(back) * r;
                g.fill((int) x, (int) y, (int) x + 1, (int) y + 1, FcsDraw.argb(0xFFFFFF, (0.9F - k * 0.17F) * a));
            }
        }
        // cost ticks of the two keys (0.1.10.4: labelled with the bound keys)
        if (keys != null) {
            costTick(g, cx, cy, r, keys.primaryKey(), NanoClient.keyLabel(NanoClient.PRIMARY_KEY, "V"), q, a);
            costTick(g, cx, cy, r, keys.secondaryKey(), NanoClient.keyLabel(NanoClient.SECONDARY_KEY, "B"), q, a);
        }
        // read-out
        String text = dec ? "DECOHERENT" : String.format(Locale.ROOT, "COH %03d", Math.round(shown));
        FcsDraw.textCentered(g, text, cx, cy - r - 8, FcsDraw.argb(dec ? RED : q.max() > 100 ? GOLD : CYAN, 0.85F * a), 0.5F);
        if (dec) {
            float left = q.decoherent() / (float) Math.max(1, q.decoherentMax());
            g.fill((int) (cx - 18), (int) (cy - r - 2), (int) (cx + 18), (int) (cy - r - 1), FcsDraw.argb(0x160606, 0.6F));
            g.fill((int) (cx - 18), (int) (cy - r - 2), (int) (cx - 18 + 36 * (1 - left)), (int) (cy - r - 1), FcsDraw.argb(RED, 0.9F));
        }
    }

    private static void costTick(GuiGraphics g, float cx, float cy, float r, String key, String label, QuantumPackets.CoreSync q, float a) {
        if (!QuantumActions.isActionKey(key)) return;
        var spec = QuantumActions.spec(com.miophas.singularity_iteration.common.suit.UnitRef.byKey(key));
        if (spec == null || spec.coherenceCost() <= 0) return;
        float ang = Mth.clamp(spec.coherenceCost() / Math.max(1, q.max()), 0, 1) * (float) Math.PI * 2;
        boolean ok = q.coherence() >= spec.coherenceCost();
        int col = ok ? 0xFFFFFF : RED;
        float s = (float) Math.sin(ang), c = (float) Math.cos(ang);
        FcsDraw.line(g, cx + s * (r + 1.5F), cy - c * (r + 1.5F), cx + s * (r + 5), cy - c * (r + 5), 0.8F, FcsDraw.argb(col, 0.8F * a));
        FcsDraw.textCentered(g, label, cx + s * (r + 8.5F), cy - c * (r + 8.5F) - 2, FcsDraw.argb(col, 0.75F * a), 0.45F);
    }

    private static void chargePips(GuiGraphics g, QuantumPackets.CoreSync q, String key, float x, float y, float a) {
        if (QuantumActions.kind(com.miophas.singularity_iteration.common.suit.UnitRef.byKey(key)) != QuantumActions.Kind.BLINK || q.blinkMax() < 2) return;
        for (int i = 0; i < q.blinkMax(); i++) {
            float px = x - (q.blinkMax() - 1) * 3 + i * 6;
            boolean full = i < q.blinkCharges();
            int c = FcsDraw.argb(full ? 0x8F7CFF : 0x3A3060, (full ? 0.95F : 0.7F) * a);
            FcsDraw.triangle(g, px, y - 2, px - 2, y, px + 2, y, c);
            FcsDraw.triangle(g, px - 2, y, px + 2, y, px, y + 2, c);
        }
        if (q.blinkCharges() < q.blinkMax() && q.blinkRecharge() > 0) {
            float k = 1 - q.blinkRecharge() / (float) Math.max(1, q.blinkRechargeMax());
            g.fill((int) (x - 8), (int) y + 3, (int) (x - 8 + 16 * k), (int) y + 4, FcsDraw.argb(0x8F7CFF, 0.8F * a));
        }
    }

    // ------------------------------------------------------------------ timers

    private static void timers(GuiGraphics g, QuantumPackets.CoreSync q, float cx, float y, float t, float a) {
        if (q.phaseTicks() > 0) {
            bar(g, cx, y, "PHASE SHIFT", q.phaseTicks() / 20F, q.phaseTicks() / (float) Math.max(1, q.phaseMax()), PHASE, t, a);
            y += 9;
        }
        if (q.field()) {
            float pulse = 0.6F + 0.4F * (float) Math.sin(t * 0.3F);
            FcsDraw.textCentered(g, String.format(Locale.ROOT, "FORCE FIELD  %.1fs", q.fieldTicks() / 20F), cx, y, FcsDraw.argb(FIELD, 0.9F * a * pulse), 0.5F);
            y += 9;
        }
        if (q.dilationTicks() > 0) {
            bar(g, cx, y, "TIME DILATION", q.dilationTicks() / 20F, q.dilationTicks() / (float) Math.max(1, q.dilationMax()), TIME, t, a);
        }
    }

    private static void bar(GuiGraphics g, float cx, float y, String label, float seconds, float left, int col, float t, float a) {
        g.fill((int) (cx - 24), (int) y, (int) (cx + 24), (int) y + 2, FcsDraw.argb(0x0A0614, 0.55F));
        g.fill((int) (cx - 24), (int) y, (int) (cx - 24 + 48 * Mth.clamp(left, 0, 1)), (int) y + 2, FcsDraw.argb(col, 0.9F * a));
        FcsDraw.textCentered(g, String.format(Locale.ROOT, "%s  %.1fs", label, seconds), cx, y + 3.5F, FcsDraw.argb(col, 0.85F * a), 0.5F);
    }

    // ------------------------------------------------------------------ full-screen cues

    private static void screenCues(GuiGraphics g, QuantumPackets.CoreSync q, float t, int w, int h, float partial) {
        float f = FcsClientConfig.quantumFx() * FcsClientConfig.flashScale();
        // blink: a white flash and radial streaks
        float sinceBlink = t - QuantumClient.blinkAt;
        if (sinceBlink >= 0 && sinceBlink < 7) {
            float k = 1 - sinceBlink / 7F;
            g.fill(0, 0, w, h, FcsDraw.argb(0xE8E0FF, 0.28F * k * k * f));
            Random r = new Random(QuantumClient.blinkAt * 31L);
            float cx = w / 2F, cy = h / 2F;
            for (int i = 0; i < 42; i++) {
                double ang = r.nextDouble() * Math.PI * 2;
                float r0 = Math.max(w, h) * (0.12F + 0.5F * (1 - k) + 0.1F * r.nextFloat()), r1 = r0 + 20 + 50 * r.nextFloat();
                FcsDraw.line(g, cx + (float) Math.cos(ang) * r0, cy + (float) Math.sin(ang) * r0, cx + (float) Math.cos(ang) * r1,
                    cy + (float) Math.sin(ang) * r1, 0.8F, FcsDraw.argb(i % 3 == 0 ? VIOLET : 0xF2F0FF, 0.5F * k * f));
            }
        }
        // phase shift: interference fringes creep in from the edges, chromatic edge lines
        if (q.phaseTicks() > 0) {
            float k = Mth.clamp(q.phaseTicks() / 4F, 0, 1) * Mth.clamp((t - QuantumClient.phaseAt) / 4F, 0, 1);
            for (int i = 0; i < 18; i++) {
                float band = 0.5F + 0.5F * (float) Math.sin(i * 0.9F - t * 0.8F);
                int c = FcsDraw.argb(i % 2 == 0 ? PHASE : CYAN, 0.08F * band * k * (1 - i / 18F) * f);
                int e = i * 2;
                g.fill(e, e, w - e, e + 1, c);
                g.fill(e, h - e - 1, w - e, h - e, c);
                g.fill(e, e, e + 1, h - e, c);
                g.fill(w - e - 1, e, w - e, h - e, c);
            }
            FcsDraw.textCentered(g, "OUT OF PHASE", w / 2F, h / 2F - 44, FcsDraw.argb(PHASE, (0.55F + 0.45F * (float) Math.sin(t * 0.7F)) * k), 0.6F);
        }
        // time dilation: a violet vignette, slow ripples from the centre, a turning clock hand
        float dil = 0;
        if (q.dilationTicks() > 0) dil = Mth.clamp((t - QuantumClient.dilationOnAt) / 8F, 0, 1) * Mth.clamp(q.dilationTicks() / 8F, 0, 1);
        if (dil > 0) {
            for (int i = 0; i < 28; i += 2) {
                int c = FcsDraw.argb(0x2A1450, 0.10F * dil * (1 - i / 28F) * f);
                g.fill(i, i, w - i, i + 1, c);
                g.fill(i, h - i - 1, w - i, h - i, c);
                g.fill(i, i, i + 1, h - i, c);
                g.fill(w - i - 1, i, w - i, h - i, c);
            }
            g.fill(0, 0, w, h, FcsDraw.argb(0x1A1030, 0.12F * dil * f));
            float cx = w / 2F, cy = h / 2F;
            for (int k = 0; k < 3; k++) {
                float u = ((t * 0.012F) + k / 3F) % 1F;
                FcsDraw.circle(g, cx, cy, 30 + u * Math.max(w, h) * 0.6F, 1.2F, FcsDraw.argb(TIME, 0.22F * (1 - u) * dil * f));
            }
            float hand = t * 0.02F;
            FcsDraw.line(g, cx, cy, cx + (float) Math.sin(hand) * 34, cy - (float) Math.cos(hand) * 34, 0.6F, FcsDraw.argb(TIME, 0.35F * dil));
        }
        // decoherence glitch: tearing bands, colour split lines, noise blocks
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

    private static int lerp(int c0, int c1, float k) {
        int r = (int) Mth.lerp(k, c0 >> 16 & 255, c1 >> 16 & 255), gr = (int) Mth.lerp(k, c0 >> 8 & 255, c1 >> 8 & 255), b = (int) Mth.lerp(k, c0 & 255, c1 & 255);
        return r << 16 | gr << 8 | b;
    }
}
