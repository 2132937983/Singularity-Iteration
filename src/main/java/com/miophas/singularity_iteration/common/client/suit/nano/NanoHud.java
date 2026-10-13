// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.client.suit.nano;

import com.miophas.singularity_iteration.common.client.suit.FcsClientConfig;
import com.miophas.singularity_iteration.common.client.suit.FcsDraw;
import com.miophas.singularity_iteration.common.suit.SuitModuleType;
import com.miophas.singularity_iteration.common.suit.nano.NanoBursts;
import com.miophas.singularity_iteration.common.suit.nano.NanoPackets;
import java.util.Locale;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;

/**
 * Nano core HUD around the crosshair (0.1.8.5). No visor is needed: the nano suit projects it.
 *
 * <pre>
 *            [HF SYNC ~~~]
 *      .-'                '-.
 *  RSV |  reserve    heat   | HEAT
 *  arc |       +            | arc (cooling chevrons)
 *      '-.                .-'
 *     [V DASH]        [B EMP ]   (cooldown sweeps)
 * </pre>
 * The left arc is the burst reserve with cost markers of the two keys, the right arc the heat
 * (cyan, amber, red; red pulse and VENTING when overheated). The burst chips show their key,
 * the unit, a cooldown sweep, a ready pulse, and a red cross when a press was refused. The HUD
 * dims when nothing happens and lights up on any change. Full-screen cues: camouflage edge
 * shimmer and timer, dash speed lines, EMP flash, overheat border.
 */
public final class NanoHud {
    private NanoHud() {}

    private static final int CYAN = 0x5CEBFF, AMBER = 0xFFB347, RED = 0xFF3B30, VIOLET = 0xB98CFF, ELECTRIC = 0x7FB2FF;
    private static float alpha = 1, lastReserve = -1, lastHeat = -1;
    private static int lastChange;

    public static void render(GuiGraphics g, DeltaTracker delta) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || mc.options.hideGui || !FcsClientConfig.nanoHud() || !FcsClientConfig.hudEnabled()) return;
        coreBurstChip(g, player, delta);                      // 0.1.9.0: arc discharge of the advanced propulsion core
        NanoPackets.CoreSync s = NanoClient.sync;
        if (s == null || !s.online()) return;
        float partial = delta.getGameTimeDeltaPartialTick(false);
        float t = NanoClient.ticks + partial;
        int w = g.guiWidth(), h = g.guiHeight();
        float cx = w / 2F, cy = h / 2F;

        screenCues(g, s, t, w, h);

        // activity: full brightness on change, dim when idle
        boolean busy = s.reserve() < 99.5F || s.heat() > 0.5F || s.primaryCooldown() > 0 || s.secondaryCooldown() > 0
            || s.camoTicks() > 0 || s.meshTicks() > 0 || s.reconTicks() > 0 || NanoClient.hfSync || t - NanoClient.usedAt[0] < 40 || t - NanoClient.usedAt[1] < 40
            || t - NanoClient.deniedAt[0] < 40 || t - NanoClient.deniedAt[1] < 40;
        if (Math.abs(s.reserve() - lastReserve) > 0.2F || Math.abs(s.heat() - lastHeat) > 0.2F) lastChange = NanoClient.ticks;
        lastReserve = s.reserve();
        lastHeat = s.heat();
        float target = busy || NanoClient.ticks - lastChange < 60 ? 1F : 0.38F;
        alpha += (target - alpha) * 0.08F;
        float a = alpha;

        // ---- reserve arc (left), heat arc (right)
        float r = 17;
        float l0 = (float) Math.toRadians(200), l1 = (float) Math.toRadians(340);    // bottom-left .. top-left
        float h0 = (float) Math.toRadians(160), h1 = (float) Math.toRadians(20);     // bottom-right .. top-right
        int segs = 14;
        float res = Mth.clamp(s.reserve() / 100F, 0, 1);
        segmented(g, cx, cy, r, l0, l1, segs, 1F, 2.2F, FcsDraw.argb(CYAN, 0.16F * a));
        segmented(g, cx, cy, r, l0, l1, segs, res, 2.2F, FcsDraw.argb(CYAN, 0.9F * a));
        // cost markers of the two keys on the reserve arc
        costMarker(g, cx, cy, r, l0, l1, s.primaryKey(), "V", a, s.reserve());
        costMarker(g, cx, cy, r, l0, l1, s.secondaryKey(), "B", a, s.reserve());
        float heat = Mth.clamp(s.heat() / Math.max(1, s.heatCap()), 0, 1);
        int heatCol = s.overheated() ? RED : heat > 0.7F ? RED : heat > 0.4F ? AMBER : CYAN;
        float pulse = s.overheated() ? 0.55F + 0.45F * (float) Math.sin(t * 0.6F) : 1F;
        segmented(g, cx, cy, r, h0, h1, segs, 1F, 2.2F, FcsDraw.argb(heatCol, 0.14F * a));
        segmented(g, cx, cy, r, h0, h1, segs, s.overheated() ? 1F : heat, 2.2F, FcsDraw.argb(heatCol, 0.9F * a * pulse));
        // cooling chevrons sliding down the heat arc
        if (s.heat() > 0.5F) {
            for (int k = 0; k < 3; k++) {
                float u = ((t * 0.03F) + k / 3F) % 1F;
                float ang = Mth.lerp(u, h1, h0);
                float x = cx + (float) Math.sin(ang) * (r + 4.5F), y = cy - (float) Math.cos(ang) * (r + 4.5F);
                FcsDraw.triangle(g, x - 1.6F, y - 1.2F, x + 1.6F, y - 1.2F, x, y + 1.4F, FcsDraw.argb(0x9FE7FF, 0.7F * a * (1 - u)));
            }
        }
        // resume marker on the heat arc while venting
        if (s.overheated()) {
            float resume = s.resumeHeat() / Math.max(1, s.heatCap());
            float ang = Mth.lerp(resume, h0, h1);
            FcsDraw.line(g, cx + (float) Math.sin(ang) * (r - 3), cy - (float) Math.cos(ang) * (r - 3),
                cx + (float) Math.sin(ang) * (r + 3), cy - (float) Math.cos(ang) * (r + 3), 1F, FcsDraw.argb(0xFFFFFF, 0.9F * a));
        }
        // read-outs
        FcsDraw.textRight(g, String.format(Locale.ROOT, "RSV %02d", Math.round(s.reserve())), cx - r - 5, cy + 9, FcsDraw.argb(CYAN, 0.85F * a), 0.5F);
        String heatText = s.overheated() ? "VENT" : String.format(Locale.ROOT, "HEAT %02d", Math.round(s.heat()));
        FcsDraw.text(g, heatText, cx + r + 5, cy + 9, FcsDraw.argb(heatCol, 0.85F * a * pulse), 0.5F);

        // ---- burst chips
        chip(g, cx - 30, cy + 22, 0, s.primaryKey(), s.primaryCooldown(), s.primaryCooldownMax(), s, t, a, "V");
        chip(g, cx + 30, cy + 22, 1, s.secondaryKey(), s.secondaryCooldown(), s.secondaryCooldownMax(), s, t, a, "B");

        // ---- HF sync
        if (NanoClient.hfSync) hfSync(g, cx, cy - 27, t, a);
        // ---- 0.1.8.6: passive unit strip, generator income, recon contacts
        passives(g, player, cx, cy + 31, a, s);
        if (s.reconTicks() > 0) {
            float k = s.reconTicks() / (float) Math.max(1, s.reconMax());
            float y = cy - 40;
            FcsDraw.textCentered(g, String.format(Locale.ROOT, "RECON  %d CONTACTS", NanoFx.reconCount()), cx, y - 6, FcsDraw.argb(0x64FFDA, 0.9F * a), 0.5F);
            g.fill((int) (cx - 22), (int) y, (int) (cx + 22), (int) y + 1, FcsDraw.argb(0x061016, 0.5F));
            g.fill((int) (cx - 22), (int) y, (int) (cx - 22 + 44 * k), (int) y + 1, FcsDraw.argb(0x64FFDA, 0.85F * a));
        }
        g.flush();
    }

    // ------------------------------------------------------------------ pieces

    private static void segmented(GuiGraphics g, float cx, float cy, float r, float a0, float a1, int segs, float fill, float width, int argb) {
        if (fill <= 0) return;
        float span = a1 - a0;
        float step = span / segs;
        int full = (int) Math.ceil(fill * segs - 1.0E-4F);
        for (int i = 0; i < full; i++) {
            float s0 = a0 + i * step, s1 = s0 + step * 0.72F;
            if (i == full - 1) {
                float part = fill * segs - i;
                s1 = s0 + step * 0.72F * Math.min(1, part);
            }
            FcsDraw.arc(g, cx, cy, r, s0, s1, width, argb);
        }
    }

    private static void costMarker(GuiGraphics g, float cx, float cy, float r, float a0, float a1, String key, String label, float a, float reserve) {
        int cost = cost(key);
        if (cost <= 0) return;
        float ang = Mth.lerp(cost / 100F, a0, a1);
        boolean ok = reserve >= cost;
        int col = ok ? 0xFFFFFF : RED;
        float x0 = cx + (float) Math.sin(ang) * (r + 2), y0 = cy - (float) Math.cos(ang) * (r + 2);
        float x1 = cx + (float) Math.sin(ang) * (r + 6), y1 = cy - (float) Math.cos(ang) * (r + 6);
        FcsDraw.line(g, x0, y0, x1, y1, 0.8F, FcsDraw.argb(col, 0.8F * a));
        FcsDraw.textCentered(g, label, cx + (float) Math.sin(ang) * (r + 9), cy - (float) Math.cos(ang) * (r + 9) - 2, FcsDraw.argb(col, 0.75F * a), 0.45F);
    }

    /** Reserve cost of a built-in burst unit after the config; 0 for none or unknown. */
    private static int cost(String key) {
        SuitModuleType type = SuitModuleType.byId(key);
        if (type == null) return 0;
        var spec = com.miophas.singularity_iteration.common.suit.nano.NanoConfig.burst(type);
        return spec == null ? 0 : spec.reserveCost();
    }

    private static String label(String key) {
        SuitModuleType type = SuitModuleType.byId(key);
        if (type == SuitModuleType.MYOFIBER) return "DASH";
        if (type == SuitModuleType.EMP_EMITTER) return "EMP";
        if (type == SuitModuleType.OPTICAL_CAMO) return "CAMO";
        if (type == SuitModuleType.KINETIC_MESH) return "MESH";
        if (type == SuitModuleType.RECON_CLOUD) return "SCAN";
        if (key.isEmpty()) return "----";
        String path = key.contains(":") ? key.substring(key.indexOf(':') + 1) : key;
        return path.length() > 4 ? path.substring(0, 4).toUpperCase(Locale.ROOT) : path.toUpperCase(Locale.ROOT);
    }

    private static int color(String key) {
        SuitModuleType type = SuitModuleType.byId(key);
        if (type == SuitModuleType.KINETIC_MESH) return 0xFFB300;
        if (type == SuitModuleType.RECON_CLOUD) return 0x64FFDA;
        return type == SuitModuleType.EMP_EMITTER ? ELECTRIC : type == SuitModuleType.OPTICAL_CAMO ? 0xBFF6FF : type == SuitModuleType.MYOFIBER ? 0x3DFFB4 : VIOLET;
    }

    private static void chip(GuiGraphics g, float x, float y, int slot, String key, int cd, int cdMax, NanoPackets.CoreSync s, float t, float a, String keyName) {
        float w = 30, hgt = 11;
        float x0 = x - w / 2, y0 = y - hgt / 2;
        boolean empty = key.isEmpty();
        int col = empty ? 0x6A7A86 : color(key);
        boolean camoOn = SuitModuleType.byId(key) == SuitModuleType.OPTICAL_CAMO && s.camoTicks() > 0;
        boolean meshOn = SuitModuleType.byId(key) == SuitModuleType.KINETIC_MESH && s.meshTicks() > 0;
        boolean ready = !empty && cd <= 0 && !s.overheated() && s.reserve() >= cost(key);
        float sinceDeny = t - NanoClient.deniedAt[slot], sinceUse = t - NanoClient.usedAt[slot];
        float shake = sinceDeny < 8 ? (float) Math.sin(sinceDeny * 3.2F) * (8 - sinceDeny) * 0.35F : 0;
        x0 += shake;
        // body: chamfered plate
        int body = FcsDraw.argb(0x061016, 0.62F * a);
        FcsDraw.quad(g, x0 + 3, y0, x0 + w, y0, x0 + w, y0 + hgt - 3, x0 + 3, y0 + hgt - 3, body);
        FcsDraw.quad(g, x0, y0 + 3, x0 + 3, y0 + 3, x0 + 3, y0 + hgt, x0, y0 + hgt, body);
        FcsDraw.triangle(g, x0, y0 + 3, x0 + 3, y0, x0 + 3, y0 + 3, body);
        FcsDraw.quad(g, x0 + 3, y0 + hgt - 3, x0 + w - 3, y0 + hgt - 3, x0 + w - 3, y0 + hgt, x0 + 3, y0 + hgt, body);
        FcsDraw.triangle(g, x0 + w - 3, y0 + hgt - 3, x0 + w, y0 + hgt - 3, x0 + w - 3, y0 + hgt, body);
        // cooldown sweep (fills from the left as it recovers)
        if (cd > 0 && cdMax > 0) {
            float k = 1 - Mth.clamp(cd / (float) cdMax, 0, 1);
            g.fill((int) (x0 + 1), (int) (y0 + hgt - 2), (int) (x0 + 1 + (w - 2) * k), (int) (y0 + hgt - 1), FcsDraw.argb(col, 0.8F * a));
            FcsDraw.textRight(g, String.format(Locale.ROOT, "%.1f", cd / 20F), x0 + w - 2, y0 - 6, FcsDraw.argb(0xFFFFFF, 0.7F * a), 0.45F);
        }
        float edgeA = ready ? 0.6F + 0.4F * (float) Math.sin(t * 0.25F) : 0.35F;
        if (camoOn || meshOn) edgeA = 0.9F;
        int edge = FcsDraw.argb(sinceDeny < 10 ? RED : col, edgeA * a);
        FcsDraw.line(g, x0 + 3, y0, x0 + w, y0, 0.8F, edge);
        FcsDraw.line(g, x0 + w, y0, x0 + w, y0 + hgt - 3, 0.8F, edge);
        FcsDraw.line(g, x0 + w, y0 + hgt - 3, x0 + w - 3, y0 + hgt, 0.8F, edge);
        FcsDraw.line(g, x0 + w - 3, y0 + hgt, x0, y0 + hgt, 0.8F, edge);
        FcsDraw.line(g, x0, y0 + hgt, x0, y0 + 3, 0.8F, edge);
        FcsDraw.line(g, x0, y0 + 3, x0 + 3, y0, 0.8F, edge);
        // key cap
        g.fill((int) x0 + 2, (int) y0 + 2, (int) x0 + 9, (int) y0 + 9, FcsDraw.argb(col, (empty ? 0.25F : 0.85F) * a));
        FcsDraw.textCentered(g, keyName, x0 + 5.5F, y0 + 3.2F, FcsDraw.argb(0x041014, 0.95F * a), 0.5F);
        FcsDraw.text(g, camoOn ? "OFF" : label(key), x0 + 11, y0 + 3.2F, FcsDraw.argb(ready || camoOn ? 0xEAF6FF : 0x8A9AA6, 0.95F * a), 0.5F);
        // use flash: expanding bracket
        if (sinceUse < 8) {
            float k = sinceUse / 8F;
            FcsDraw.brackets(g, x0 - 3 * k, y0 - 3 * k, w + 6 * k, hgt + 6 * k, 3, 0.8F, FcsDraw.argb(col, (1 - k) * a));
        }
        // refusal: red cross and the reason
        if (sinceDeny < 16) {
            float k = 1 - sinceDeny / 16F;
            FcsDraw.line(g, x0 + 1, y0 + 1, x0 + w - 1, y0 + hgt - 1, 1F, FcsDraw.argb(RED, 0.8F * k * a));
            FcsDraw.line(g, x0 + 1, y0 + hgt - 1, x0 + w - 1, y0 + 1, 1F, FcsDraw.argb(RED, 0.8F * k * a));
            String why = switch (NanoClient.deniedReason[slot]) {
                case NanoBursts.DENY_OVERHEAT -> "OVERHEAT";
                case NanoBursts.DENY_COOLDOWN -> "COOLDOWN";
                case NanoBursts.DENY_RESERVE -> "LOW RSV";
                case NanoBursts.DENY_OFF -> "OFFLINE";
                case NanoBursts.DENY_CONFLICT -> "CONFLICT";
                default -> "NO UNIT";
            };
            FcsDraw.textCentered(g, why, x + shake, y0 + hgt + 2, FcsDraw.argb(RED, k * a), 0.45F);
        }
    }

    private static void hfSync(GuiGraphics g, float cx, float y, float t, float a) {
        float since = NanoClient.ticks - NanoClient.hfSyncSince;
        float in = Mth.clamp(since / 6F, 0, 1);
        float w = 44 * in;
        float x0 = cx - w / 2;
        g.fill((int) x0, (int) y - 4, (int) (x0 + w), (int) y + 4, FcsDraw.argb(0x06101A, 0.55F * a));
        FcsDraw.line(g, x0, y - 4, x0 + w, y - 4, 0.6F, FcsDraw.argb(ELECTRIC, 0.7F * a));
        FcsDraw.line(g, x0, y + 4, x0 + w, y + 4, 0.6F, FcsDraw.argb(ELECTRIC, 0.7F * a));
        if (in < 1) return;
        FcsDraw.text(g, "HF", x0 + 2, y - 2.4F, FcsDraw.argb(0xEAF6FF, 0.95F * a), 0.5F);
        // the blade frequency: a fast, slightly jittering sine wave
        float px = 0, py = 0;
        for (int i = 0; i <= 26; i++) {
            float x = x0 + 12 + i;
            float v = (float) Math.sin(i * 1.4F + t * 2.9F) * 2.4F * (0.8F + 0.2F * (float) Math.sin(t * 7.1F + i));
            if (i > 0) FcsDraw.line(g, px, py, x, y + v, 0.6F, FcsDraw.argb(ELECTRIC, 0.9F * a));
            px = x;
            py = y + v;
        }
        FcsDraw.text(g, "SYNC", x0 + w + 2, y - 2.4F, FcsDraw.argb(ELECTRIC, 0.8F * a), 0.45F);
        // 0.1.8.6: sync level pips (myofiber and kinetic mesh sources, Mk.II counts double)
        for (int i = 0; i < 3; i++) {
            boolean on = i < NanoClient.hfLevel;
            g.fill((int) (x0 + w + 2 + i * 4), (int) y + 3, (int) (x0 + w + 5 + i * 4), (int) y + 5, FcsDraw.argb(on ? ELECTRIC : 0x2A3A50, (on ? 0.95F : 0.6F) * a));
        }
    }

    /** Short tags of the passive nano units that run now (Mk.II units get a violet underline) and the generator income. */
    private static void passives(GuiGraphics g, LocalPlayer player, float cx, float y, float a, NanoPackets.CoreSync s) {
        java.util.List<SuitModuleType> on = new java.util.ArrayList<>();
        for (SuitModuleType type : PASSIVE) {
            if (com.miophas.singularity_iteration.common.suit.nano.NanoSuit.active(player, type)) on.add(type);
        }
        if (on.isEmpty() && s.generated() <= 0) return;
        float scale = 0.42F;
        float total = 0;
        for (SuitModuleType type : on) total += FcsDraw.width(tag(type), scale) + 4;
        String income = s.generated() > 0 ? "+" + s.generated() + " EU/s" : "";
        if (!income.isEmpty()) total += FcsDraw.width(income, scale) + 4;
        float x = cx - total / 2;
        for (SuitModuleType type : on) {
            String tag = tag(type);
            float tw = FcsDraw.width(tag, scale);
            FcsDraw.text(g, tag, x, y, FcsDraw.argb(type.color(), 0.75F * a), scale);
            if (com.miophas.singularity_iteration.common.suit.SuitModules.tier(player, type) >= 2) {
                g.fill((int) x, (int) (y + 4.5F), (int) Math.ceil(x + tw), (int) (y + 5.5F), FcsDraw.argb(0xB98CFF, 0.85F * a));
            }
            x += tw + 4;
        }
        if (!income.isEmpty()) FcsDraw.text(g, income, x, y, FcsDraw.argb(0xFFEE58, 0.85F * a), scale);
    }

    private static final SuitModuleType[] PASSIVE = {SuitModuleType.THERMOREGULATOR, SuitModuleType.TOXIN_FILTER, SuitModuleType.GECKO_SETAE,
        SuitModuleType.SHOCK_GEL, SuitModuleType.HEAT_SINK_FINS, SuitModuleType.THERMAL_CAMO, SuitModuleType.JAMMER, SuitModuleType.SELF_REPAIR,
        SuitModuleType.HYDROPHOBIC, SuitModuleType.KINETIC_GENERATOR, SuitModuleType.SOLAR_SKIN};

    private static String tag(SuitModuleType type) {
        return switch (type) {
            case THERMOREGULATOR -> "THRM";
            case TOXIN_FILTER -> "TOX";
            case GECKO_SETAE -> "GRIP";
            case SHOCK_GEL -> "GEL";
            case HEAT_SINK_FINS -> "FINS";
            case THERMAL_CAMO -> "IR";
            case JAMMER -> "JAM";
            case SELF_REPAIR -> "RPR";
            case HYDROPHOBIC -> "HYD";
            case KINETIC_GENERATOR -> "KGEN";
            case SOLAR_SKIN -> "SOL";
            default -> type.id().toUpperCase(Locale.ROOT);
        };
    }

    // ------------------------------------------------------------------ full-screen cues

    private static void screenCues(GuiGraphics g, NanoPackets.CoreSync s, float t, int w, int h) {
        float f = FcsClientConfig.nanoFx() * FcsClientConfig.flashScale();
        // camouflage: cyan edge shimmer, scanlines, timer
        if (s.camoTicks() > 0) {
            float k = Mth.clamp(s.camoTicks() / 10F, 0, 1);
            int edge = (int) (26 * k);
            for (int i = 0; i < edge; i += 2) {
                float aa = 0.10F * k * (1 - i / (float) edge) * f;
                int c = FcsDraw.argb(CYAN, aa);
                g.fill(i, i, w - i, i + 1, c);
                g.fill(i, h - i - 1, w - i, h - i, c);
                g.fill(i, i, i + 1, h - i, c);
                g.fill(w - i - 1, i, w - i, h - i, c);
            }
            int band = (int) ((t * 3) % h);
            g.fill(0, band, w, band + 1, FcsDraw.argb(CYAN, 0.05F * f));
            float left = 1 - Mth.clamp(s.camoTicks() / (float) Math.max(1, s.camoMax()), 0, 1);
            float cx = w / 2F, y = h / 2F + 34;
            g.fill((int) (cx - 24), (int) y, (int) (cx + 24), (int) y + 2, FcsDraw.argb(0x061016, 0.5F));
            g.fill((int) (cx - 24), (int) y, (int) (cx - 24 + 48 * left), (int) y + 2, FcsDraw.argb(0xBFF6FF, 0.85F));
            FcsDraw.textCentered(g, String.format(Locale.ROOT, "OPTICAL CAMO  %.1fs", (s.camoMax() - s.camoTicks()) / 20F), cx, y + 4,
                FcsDraw.argb(0xBFF6FF, 0.85F), 0.5F);
        }
        // kinetic armor mesh: amber timer and a faint hex edge
        if (s.meshTicks() > 0) {
            float left = s.meshTicks() / (float) Math.max(1, s.meshMax());
            float cx = w / 2F, y = h / 2F + 38;
            g.fill((int) (cx - 24), (int) y, (int) (cx + 24), (int) y + 2, FcsDraw.argb(0x161006, 0.5F));
            g.fill((int) (cx - 24), (int) y, (int) (cx - 24 + 48 * left), (int) y + 2, FcsDraw.argb(0xFFB300, 0.9F));
            FcsDraw.textCentered(g, String.format(Locale.ROOT, "KINETIC MESH  %.1fs", s.meshTicks() / 20F), cx, y + 4, FcsDraw.argb(0xFFD180, 0.85F), 0.5F);
            int edge = 10;
            for (int i = 0; i < edge; i += 2) {
                int c = FcsDraw.argb(0xFFB300, 0.06F * (1 - i / (float) edge) * f);
                g.fill(i, i, w - i, i + 1, c);
                g.fill(i, h - i - 1, w - i, h - i, c);
            }
        }
        // dash: radial speed lines
        if (NanoClient.dashTick >= 0) {
            float k = 1 - NanoClient.dashTick / 7F;
            java.util.Random r = new java.util.Random(NanoClient.ticks * 17L);
            float cx = w / 2F, cy = h / 2F;
            for (int i = 0; i < 36; i++) {
                double ang = r.nextDouble() * Math.PI * 2;
                float r0 = Math.max(w, h) * (0.32F + 0.18F * r.nextFloat()), r1 = r0 + 30 + 40 * r.nextFloat();
                FcsDraw.line(g, cx + (float) Math.cos(ang) * r0, cy + (float) Math.sin(ang) * r0 * 0.6F,
                    cx + (float) Math.cos(ang) * r1, cy + (float) Math.sin(ang) * r1 * 0.6F, 0.8F, FcsDraw.argb(0xDFFBFF, 0.35F * k * f));
            }
        }
        // own EMP: blue-white flash and a ring rushing out
        float sinceEmp = t - NanoFxEmpClock.lastSelfEmp();
        if (sinceEmp >= 0 && sinceEmp < 10) {
            float k = 1 - sinceEmp / 10F;
            g.fill(0, 0, w, h, FcsDraw.argb(0xCFE3FF, 0.22F * k * k * f));
            FcsDraw.circle(g, w / 2F, h / 2F, 20 + sinceEmp * Math.max(w, h) * 0.08F, 2F, FcsDraw.argb(ELECTRIC, 0.5F * k * f));
        }
        // overheat border
        float sinceHeat = t - NanoClient.overheatAt;
        if (s.overheated() || sinceHeat < 20) {
            float k = s.overheated() ? 0.5F + 0.5F * (float) Math.sin(t * 0.5F) : 1 - sinceHeat / 20F;
            for (int i = 0; i < 10; i += 2) {
                int c = FcsDraw.argb(RED, 0.12F * k * (1 - i / 10F) * f);
                g.fill(i, i, w - i, i + 1, c);
                g.fill(i, h - i - 1, w - i, h - i, c);
                g.fill(i, i, i + 1, h - i, c);
                g.fill(w - i - 1, i, w - i, h - i, c);
            }
            if (s.overheated()) FcsDraw.textCentered(g, "OVERHEAT // VENTING", w / 2F, h / 2F - 40, FcsDraw.argb(RED, 0.6F + 0.4F * k), 0.6F);
        }
    }

    /** 0.1.9.0: core burst chip under the burst chips: key, ARC, cooldown, refusal. Shown with an advanced propulsion core. */
    private static void coreBurstChip(GuiGraphics g, LocalPlayer player, DeltaTracker delta) {
        var chest = player.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.CHEST);
        if (com.miophas.singularity_iteration.common.suit.core.FrameCores.core(chest) != com.miophas.singularity_iteration.common.suit.core.FrameCore.PROPULSION) return;
        float t = NanoClient.ticks + delta.getGameTimeDeltaPartialTick(false);
        float cx = g.guiWidth() / 2F, y = g.guiHeight() / 2F + 48;
        int col = 0xFFB74D;
        float w = 34, h = 10, x0 = cx - w / 2;
        int cd = NanoClient.dischargeCooldownLeft(), cdMax = Math.max(1, com.miophas.singularity_iteration.common.suit.core.CoreConfig.dischargeCooldown());
        float sinceDeny = t - NanoClient.coreDeniedAt;
        g.fill((int) x0, (int) y, (int) (x0 + w), (int) (y + h), FcsDraw.argb(0x160C04, 0.62F));
        float edge = cd <= 0 ? 0.6F + 0.4F * (float) Math.sin(t * 0.25F) : 0.35F;
        int ec = FcsDraw.argb(sinceDeny < 10 ? RED : col, edge);
        FcsDraw.line(g, x0, y, x0 + w, y, 0.8F, ec);
        FcsDraw.line(g, x0, y + h, x0 + w, y + h, 0.8F, ec);
        FcsDraw.line(g, x0, y, x0, y + h, 0.8F, ec);
        FcsDraw.line(g, x0 + w, y, x0 + w, y + h, 0.8F, ec);
        String key = NanoClient.CORE_KEY == null ? "R" : NanoClient.CORE_KEY.getTranslatedKeyMessage().getString();
        if (key.length() > 3) key = key.substring(0, 3);
        g.fill((int) x0 + 2, (int) y + 2, (int) x0 + 9, (int) y + 8, FcsDraw.argb(col, 0.85F));
        FcsDraw.textCentered(g, key, x0 + 5.5F, y + 2.6F, FcsDraw.argb(0x140A02, 0.95F), 0.45F);
        FcsDraw.text(g, "ARC", x0 + 11, y + 2.8F, FcsDraw.argb(cd <= 0 ? 0xFFF3E0 : 0x9A8A7A, 0.95F), 0.5F);
        if (cd > 0) {
            float k = 1 - cd / (float) cdMax;
            g.fill((int) x0 + 1, (int) (y + h - 2), (int) (x0 + 1 + (w - 2) * k), (int) (y + h - 1), FcsDraw.argb(col, 0.85F));
        }
        if (sinceDeny < 16) {
            String why = switch (NanoClient.coreDeniedReason) {
                case com.miophas.singularity_iteration.common.suit.core.CoreEvents.DENY_COOLDOWN -> "COOLDOWN";
                case com.miophas.singularity_iteration.common.suit.core.CoreEvents.DENY_ENERGY -> "LOW EU";
                default -> "NO CORE";
            };
            FcsDraw.textCentered(g, why, cx, y + h + 2, FcsDraw.argb(RED, 1 - sinceDeny / 16F), 0.45F);
        }
        float sinceUse = t - NanoClient.dischargeAt;
        if (sinceUse >= 0 && sinceUse < 8) {
            float k = sinceUse / 8F;
            FcsDraw.brackets(g, x0 - 3 * k, y - 3 * k, w + 6 * k, h + 6 * k, 3, 0.8F, FcsDraw.argb(col, 1 - k));
        }
    }

    /** Tick of the last EMP of the local player (set by {@link NanoFx}). */
    static final class NanoFxEmpClock {
        static int last = -100;
        static float lastSelfEmp() { return last; }
    }
}
