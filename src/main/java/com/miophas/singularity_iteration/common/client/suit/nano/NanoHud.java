// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.client.suit.nano;

import com.miophas.singularity_iteration.common.client.suit.AsciiHud;
import com.miophas.singularity_iteration.common.client.suit.FcsClientConfig;
import com.miophas.singularity_iteration.common.client.suit.FcsDraw;
import com.miophas.singularity_iteration.common.client.suit.ManeuverModeClient;
import com.miophas.singularity_iteration.common.client.suit.VisorHudPrimitives;
import com.miophas.singularity_iteration.common.suit.SuitModuleType;
import com.miophas.singularity_iteration.common.suit.nano.NanoBursts;
import com.miophas.singularity_iteration.common.suit.nano.NanoConfig;
import com.miophas.singularity_iteration.common.suit.nano.NanoPackets;
import java.util.Locale;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.Mth;

/**
 * Curved Helmet-Visor Nano HUD (Plan E + F):
 * <ul>
 *   <li><b>100% open center crosshair</b>: moves all Nano gauges out of the aim reticle into a spherical
 *       curved visor console arched cleanly above the survival health/armor/hunger bars.</li>
 *   <li><b>Left curved wing</b>: supercapacitor reserve with oscilloscope phosphor persistence tail and
 *       key cost notches ({@code V}/{@code B}).</li>
 *   <li><b>Right curved wing</b>: thermodynamic core heat with projected next-burst thermal ghost,
 *       70% caution notch, hysteresis resume threshold, and peripheral visor edge thermal shimmer.</li>
 *   <li><b>Center-bottom skill cards</b>: vector skill icons + crisp localized short names + cooldown
 *       hairlines + 0.25s lock-in focus pulse on ready transition.</li>
 *   <li><b>Top curved brow</b>: unified translucent status pill for active timers (camo, mesh, HF sync,
 *       recon contacts, passive EU/s generation).</li>
 * </ul>
 */
public final class NanoHud {
    private NanoHud() {}

    private static final int CYAN = VisorHudPrimitives.CYAN, AMBER = VisorHudPrimitives.AMBER,
        RED = VisorHudPrimitives.RED, VIOLET = VisorHudPrimitives.VIOLET, ELECTRIC = 0x7FB2FF;
    private static float alpha = 1, lastReserve = -1, lastHeat = -1;
    private static int lastChange;
    private static final VisorHudPrimitives.PhosphorTrail RESERVE_TRAIL = new VisorHudPrimitives.PhosphorTrail();
    private static final float[] PARALLAX = new float[2];
    private static final boolean[] WAS_READY = new boolean[2];
    private static final float[] READY_AT = {-100F, -100F};
    /** Shared flag so QuantumHud can stack its top brow pill cleanly below NanoHud's pill when both are active. */
    static boolean nanoTopBrowDrawn;

    public static void render(GuiGraphics g, DeltaTracker delta) {
        nanoTopBrowDrawn = false;
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || mc.options.hideGui || !FcsClientConfig.nanoHud() || !FcsClientConfig.hudEnabled()) return;
        float partial = delta.getGameTimeDeltaPartialTick(false);
        float t = NanoClient.ticks + partial;
        int w = g.guiWidth(), h = g.guiHeight();
        VisorHudPrimitives.updateParallax(player, partial, PARALLAX);

        coreBurstChip(g, player, t, w, h);
        NanoPackets.CoreSync s = NanoClient.sync;
        if (s == null || !s.online()) return;

        screenCues(g, s, t, w, h);

        boolean busy = s.reserve() < 99.5F || s.heat() > 0.5F || s.primaryCooldown() > 0 || s.secondaryCooldown() > 0
            || s.camoTicks() > 0 || s.meshTicks() > 0 || s.reconTicks() > 0 || NanoClient.hfSync
            || t - NanoClient.usedAt[0] < 40 || t - NanoClient.usedAt[1] < 40
            || t - NanoClient.deniedAt[0] < 40 || t - NanoClient.deniedAt[1] < 40;
        if (Math.abs(s.reserve() - lastReserve) > 0.2F || Math.abs(s.heat() - lastHeat) > 0.2F) lastChange = NanoClient.ticks;
        lastReserve = s.reserve();
        lastHeat = s.heat();
        float target = busy || NanoClient.ticks - lastChange < 60 ? 1F : 0F;
        alpha += (target - alpha) * 0.08F;
        float a = alpha;
        if (a < 0.02F) { g.flush(); return; }

        float cx = w / 2F + PARALLAX[0];
        float yBase = h - 62F + PARALLAX[1];
        float spanRef = Math.min(w * 0.44F, 190F);
        float curveH = 13F;
        float halfWingOuter = Math.min(w * 0.36F, 154F);
        float lx0 = Math.max(12F, cx - halfWingOuter), lx1 = cx - 64F;
        float rx0 = cx + 64F, rx1 = Math.min(w - 12F, cx + halfWingOuter);

        // Subtle curved helmet-visor horizon rail
        VisorHudPrimitives.curvedHairline(g, cx, yBase + 11F, lx0 - 6F, rx1 + 6F, curveH, spanRef, 0.8F, CYAN, 0.26F * a, 32);

        // ---- Left Curved Wing: Supercapacitor Reserve + Phosphor Persistence Trail + Cost Notches
        float shownReserve = NanoClient.SHOWN_RESERVE.get(partial, s.reserve());
        float res = Mth.clamp(shownReserve / 100F, 0F, 1F);
        float trailRes = Mth.clamp(RESERVE_TRAIL.sample(shownReserve) / 100F, res, 1F);
        VisorHudPrimitives.curvedBar(g, cx, yBase, lx0, lx1, curveH, spanRef, 3.0F, CYAN, 0.16F * a, 16);
        float lxFill = Mth.lerp(res, lx1, lx0);
        float lxTrail = Mth.lerp(trailRes, lx1, lx0);
        if (trailRes > res + 0.006F) {
            VisorHudPrimitives.curvedBar(g, cx, yBase, lxTrail, lxFill, curveH, spanRef, 3.0F, VisorHudPrimitives.WHITE, 0.65F * a, 8);
        }
        VisorHudPrimitives.curvedBar(g, cx, yBase, lxFill, lx1, curveH, spanRef, 3.0F, CYAN, 0.92F * a, 16);

        String vk = NanoClient.keyLabel(NanoClient.PRIMARY_KEY, "V"), bk = NanoClient.keyLabel(NanoClient.SECONDARY_KEY, "B");
        reserveNotch(g, cx, yBase, lx0, lx1, curveH, spanRef, s.primaryKey(), s.reserve(), a);
        reserveNotch(g, cx, yBase, lx0, lx1, curveH, spanRef, s.secondaryKey(), s.reserve(), a);
        float lyLbl = VisorHudPrimitives.curveY(lx0 + 4F, cx, yBase, curveH, spanRef) - 9.5F;
        AsciiHud.left(g, "hud.mio_icif.nano.reserve", lx0 + 2F, lyLbl, FcsDraw.argb(CYAN, 0.92F * a), 0.60F,
            String.format(Locale.ROOT, "%d%%", Math.round(shownReserve)));

        // ---- Right Curved Wing: Thermodynamic Heat + Projected Thermal Ghost + Caution/Resume Notches
        float shownHeat = NanoClient.SHOWN_HEAT.get(partial, s.heat());
        float heatCap = Math.max(1F, s.heatCap());
        float heat = Mth.clamp(shownHeat / heatCap, 0F, 1F);
        int heatCol = s.overheated() ? RED : heat > 0.70F ? RED : heat > 0.40F ? AMBER : CYAN;
        float pulse = s.overheated() ? 0.55F + 0.45F * (float) Math.sin(t * 0.6F) : 1F;

        VisorHudPrimitives.curvedBar(g, cx, yBase, rx0, rx1, curveH, spanRef, 3.0F, heatCol, 0.16F * a, 16);
        float rxFill = Mth.lerp(s.overheated() ? 1F : heat, rx0, rx1);
        // Projected thermal ghost for primary burst heat cost (shows if next burst crosses caution/overheat!)
        float nextHeat = nextBurstHeat(s) / heatCap;
        if (!s.overheated() && nextHeat > 0.02F && heat < 0.98F) {
            float rxGhost = Mth.lerp(Math.min(1F, heat + nextHeat), rx0, rx1);
            int ghostCol = heat + nextHeat >= 1F ? RED : AMBER;
            VisorHudPrimitives.curvedBar(g, cx, yBase, rxFill, rxGhost, curveH, spanRef, 3.0F, ghostCol, 0.42F * a, 8);
        }
        VisorHudPrimitives.curvedBar(g, cx, yBase, rx0, rxFill, curveH, spanRef, 3.0F, heatCol, 0.92F * a * pulse, 16);

        // 70% thermal caution notch + hysteresis resume notch
        heatNotch(g, cx, yBase, rx0, rx1, curveH, spanRef, 0.70F, RED, 0.85F * a);
        if (s.resumeHeat() > 0 && s.resumeHeat() < heatCap) {
            heatNotch(g, cx, yBase, rx0, rx1, curveH, spanRef, s.resumeHeat() / heatCap, CYAN, (s.overheated() ? 0.95F : 0.45F) * a);
        }
        float ryLbl = VisorHudPrimitives.curveY(rx1 - 4F, cx, yBase, curveH, spanRef) - 9.5F;
        AsciiHud.right(g, s.overheated() ? "hud.mio_icif.nano.vent" : "hud.mio_icif.nano.heat", rx1 - 2F, ryLbl,
            FcsDraw.argb(heatCol, 0.92F * a * pulse), 0.60F, String.format(Locale.ROOT, "%d%%", Math.round(heat * 100F)));

        // ---- Plan F Peripheral Thermal Shimmer along right visor edge
        VisorHudPrimitives.thermalEdgeShimmer(g, w, h, heat, s.overheated(), t, a * FcsClientConfig.nanoFx());

        // ---- Center-Bottom Icon-First Skill Cards
        chip(g, cx - 31F, yBase - 2F, 0, s.primaryKey(), s.primaryCooldown(), s.primaryCooldownMax(), s, t, a, vk);
        chip(g, cx + 31F, yBase - 2F, 1, s.secondaryKey(), s.secondaryCooldown(), s.secondaryCooldownMax(), s, t, a, bk);

        // ---- Top Curved Visor Brow (unified pill for active timers, HF sync, recon, generator income)
        renderTopBrow(g, cx, w, s, a);

        g.flush();
    }

    private static void renderTopBrow(GuiGraphics g, float cx, int w, NanoPackets.CoreSync s, float a) {
        MutableComponent line = null;
        int accent = CYAN;
        if (s.overheated()) {
            line = appendSep(line, Component.translatable("hud.mio_icif.nano.overheat"));
            accent = RED;
        }
        if (s.camoTicks() > 0) {
            line = appendSep(line, Component.translatable("hud.mio_icif.nano.camo",
                String.format(Locale.ROOT, "%.1f", Math.max(0, s.camoMax() - s.camoTicks()) / 20F)));
        }
        if (s.meshTicks() > 0) {
            line = appendSep(line, Component.translatable("hud.mio_icif.nano.mesh",
                String.format(Locale.ROOT, "%.1f", s.meshTicks() / 20F)));
            if (accent == CYAN) accent = AMBER;
        }
        if (NanoClient.hfSync) {
            line = appendSep(line, Component.translatable("hud.mio_icif.nano.hf_sync", NanoClient.hfLevel));
            if (accent == CYAN) accent = ELECTRIC;
        }
        if (s.reconTicks() > 0) {
            line = appendSep(line, Component.translatable("hud.mio_icif.nano.recon", NanoFx.reconCount()));
        }
        if (s.generated() > 0) {
            line = appendSep(line, Component.literal("+" + s.generated() + " EU/s"));
        }
        if (line == null) return;
        float topY = ManeuverModeClient.isFlying() ? 26F : 7F;
        VisorHudPrimitives.topBrowPill(g, cx, topY, w, line, accent, a);
        nanoTopBrowDrawn = true;
    }

    private static MutableComponent appendSep(MutableComponent base, Component part) {
        if (base == null) return part.copy();
        return base.append(Component.literal("  |  ")).append(part);
    }

    private static void reserveNotch(GuiGraphics g, float cx, float yBase, float lx0, float lx1, float curveH, float spanRef,
                                     String key, float reserve, float a) {
        int cost = cost(key);
        if (cost <= 0) return;
        float x = Mth.lerp(Mth.clamp(cost / 100F, 0F, 1F), lx1, lx0);
        float y = VisorHudPrimitives.curveY(x, cx, yBase, curveH, spanRef);
        int col = reserve >= cost ? VisorHudPrimitives.WHITE : RED;
        FcsDraw.line(g, x, y - 3.5F, x, y + 3.5F, 0.9F, FcsDraw.argb(col, 0.85F * a));
    }

    private static void heatNotch(GuiGraphics g, float cx, float yBase, float rx0, float rx1, float curveH, float spanRef,
                                  float frac, int rgb, float a) {
        float x = Mth.lerp(Mth.clamp(frac, 0F, 1F), rx0, rx1);
        float y = VisorHudPrimitives.curveY(x, cx, yBase, curveH, spanRef);
        FcsDraw.line(g, x, y - 4.0F, x, y + 4.0F, 1.0F, FcsDraw.argb(rgb, a));
    }

    private static float nextBurstHeat(NanoPackets.CoreSync s) {
        SuitModuleType type = SuitModuleType.byId(s.primaryKey());
        if (type == null) return 0F;
        var spec = NanoConfig.burst(type);
        return spec == null ? 0F : spec.heat();
    }

    private static int cost(String key) {
        SuitModuleType type = SuitModuleType.byId(key);
        if (type == null) return 0;
        var spec = NanoConfig.burst(type);
        return spec == null ? 0 : spec.reserveCost();
    }

    static float keyScale(String keyName, float max) {
        return Math.min(max, 6.4F / Math.max(1F, FcsDraw.width(keyName, 1F)));
    }

    static int color(String key) {
        SuitModuleType type = SuitModuleType.byId(key);
        if (type == SuitModuleType.DEFLECTOR) return 0x5AB8FF;
        if (type == SuitModuleType.BEHAVIOR_PREDICTOR) return 0xC77DFF;
        if (type != null && com.miophas.singularity_iteration.common.suit.quantum.QuantumActions.isActionKey(key)) return type.color();
        if (type == SuitModuleType.KINETIC_MESH) return 0xFFB300;
        if (type == SuitModuleType.RECON_CLOUD) return 0x64FFDA;
        return type == SuitModuleType.EMP_EMITTER ? ELECTRIC : type == SuitModuleType.OPTICAL_CAMO ? 0xBFF6FF
            : type == SuitModuleType.MYOFIBER ? 0x3DFFB4 : VIOLET;
    }

    /** Plan E icon-first translucent skill card shared by NanoHud and QuantumHud. */
    static void chip(GuiGraphics g, float x, float y, int slot, String key, int cd, int cdMax, NanoPackets.CoreSync s, float t, float a, String keyName) {
        float w = 56F, hgt = 20F;
        float x0 = x - w / 2F, y0 = y - hgt / 2F;
        boolean empty = key.isEmpty();
        int col = empty ? 0x6A7A86 : color(key);
        boolean quantum = com.miophas.singularity_iteration.common.suit.quantum.QuantumActions.isActionKey(key);
        boolean activeToggle = SuitModuleType.byId(key) == SuitModuleType.OPTICAL_CAMO && s != null && s.camoTicks() > 0
            || quantum && QuantumClient.togglesOff(key);
        boolean meshOn = SuitModuleType.byId(key) == SuitModuleType.KINETIC_MESH && s != null && s.meshTicks() > 0;
        boolean ready = !empty && (quantum ? QuantumClient.ready(key, cd) : s != null && cd <= 0 && !s.overheated() && s.reserve() >= cost(key));

        // 0.25s lock-in focus pulse when a skill transitions from unready -> ready
        if (slot >= 0 && slot < 2) {
            if (ready && !WAS_READY[slot]) READY_AT[slot] = t;
            WAS_READY[slot] = ready;
        }
        float sinceReady = (slot >= 0 && slot < 2) ? t - READY_AT[slot] : 100F;
        float sinceDeny = (slot >= 0 && slot < 2) ? t - NanoClient.deniedAt[slot] : 100F;
        float sinceUse = (slot >= 0 && slot < 2) ? t - NanoClient.usedAt[slot] : 100F;
        float shake = sinceDeny < 8 ? (float) Math.sin(sinceDeny * 3.2F) * (8 - sinceDeny) * 0.35F : 0F;
        x0 += shake;

        float edgeA = activeToggle || meshOn ? 0.92F : ready ? 0.78F : 0.34F;
        VisorHudPrimitives.glassCard(g, x0, y0, w, hgt, sinceDeny < 10 ? RED : col, edgeA * a, 0.64F * a);

        // Vector skill icon on the left
        SuitModuleType type = SuitModuleType.byId(key);
        VisorHudPrimitives.skillIcon(g, type, x0 + 9.5F, y0 + hgt / 2F, col, (ready || activeToggle ? 0.95F : 0.45F) * a);

        // Compact keycap badge at top-left corner
        g.fill((int) (x0 - 2), (int) (y0 - 3), (int) (x0 + 6), (int) (y0 + 5), FcsDraw.argb(col, (empty ? 0.35F : 0.92F) * a));
        float ks = keyScale(keyName, 0.48F);
        FcsDraw.textCentered(g, keyName, x0 + 2.0F, y0 - 1.5F + (0.48F - ks) * 3.5F, FcsDraw.argb(0x041014, 0.95F * a), ks);

        // Crisp localized short name + status subline
        Component title = VisorHudPrimitives.skillName(key);
        int txtCol = FcsDraw.argb(ready || activeToggle ? VisorHudPrimitives.WHITE : 0x8A9AA6, 0.95F * a);
        FcsDraw.text(g, AsciiHud.t(title), x0 + 18F, y0 + 2.5F, txtCol, 0.60F);

        Component sub;
        int subCol;
        if (empty) {
            sub = Component.literal("--");
            subCol = FcsDraw.argb(0x6A7A86, 0.7F * a);
        } else if (activeToggle) {
            sub = Component.translatable("hud.mio_icif.skill.active");
            subCol = FcsDraw.argb(col, 0.95F * a);
        } else if (cd > 0) {
            sub = Component.literal(String.format(Locale.ROOT, "%.1fs", cd / 20F));
            subCol = FcsDraw.argb(0xD8E8F4, 0.85F * a);
        } else if (ready) {
            sub = Component.translatable("hud.mio_icif.skill.ready");
            subCol = FcsDraw.argb(col, 0.90F * a);
        } else {
            sub = Component.translatable("hud.mio_icif.skill.standby");
            subCol = FcsDraw.argb(0x8A9AA6, 0.75F * a);
        }
        FcsDraw.text(g, AsciiHud.t(sub), x0 + 18F, y0 + 10.5F, subCol, 0.52F);

        // Bottom cooldown/ready hairline
        float prog = (cd > 0 && cdMax > 0) ? 1F - Mth.clamp(cd / (float) cdMax, 0F, 1F) : (ready ? 1F : 0F);
        if (prog > 0.01F) {
            g.fill((int) (x0 + 2), (int) (y0 + hgt - 2), (int) (x0 + 2 + (w - 4) * prog), (int) (y0 + hgt - 1),
                FcsDraw.argb(col, (ready ? 0.90F : 0.75F) * a));
        }

        // Ready lock-in focus bracket (0.25s contraction)
        if (sinceReady >= 0F && sinceReady < 5F) {
            float k = 1F - sinceReady / 5F;
            FcsDraw.brackets(g, x0 - 3F * k, y0 - 3F * k, w + 6F * k, hgt + 6F * k, 4F, 0.9F, FcsDraw.argb(VisorHudPrimitives.WHITE, k * 0.85F * a));
        }
        // Use flash: expanding bracket
        if (sinceUse >= 0F && sinceUse < 8F) {
            float k = sinceUse / 8F;
            FcsDraw.brackets(g, x0 - 3F * k, y0 - 3F * k, w + 6F * k, hgt + 6F * k, 4F, 0.9F, FcsDraw.argb(col, (1F - k) * a));
        }
        // Refusal: red cross + localized reason right above card
        if (sinceDeny >= 0F && sinceDeny < 16F) {
            float k = 1F - sinceDeny / 16F;
            FcsDraw.line(g, x0 + 2, y0 + 2, x0 + w - 2, y0 + hgt - 2, 1F, FcsDraw.argb(RED, 0.75F * k * a));
            FcsDraw.line(g, x0 + 2, y0 + hgt - 2, x0 + w - 2, y0 + 2, 1F, FcsDraw.argb(RED, 0.75F * k * a));
            String why = switch (NanoClient.deniedReason[slot]) {
                case NanoBursts.DENY_OVERHEAT -> "hud.mio_icif.deny.overheat";
                case NanoBursts.DENY_COOLDOWN -> "hud.mio_icif.deny.cooldown";
                case NanoBursts.DENY_RESERVE -> "hud.mio_icif.deny.reserve";
                case NanoBursts.DENY_OFF -> "hud.mio_icif.deny.offline";
                case NanoBursts.DENY_CONFLICT -> "hud.mio_icif.deny.conflict";
                case com.miophas.singularity_iteration.common.suit.quantum.QuantumActions.DENY_DECOHERENT -> "hud.mio_icif.deny.decoherent";
                case com.miophas.singularity_iteration.common.suit.quantum.QuantumActions.DENY_COHERENCE -> "hud.mio_icif.deny.coherence";
                case com.miophas.singularity_iteration.common.suit.quantum.QuantumActions.DENY_BLOCKED -> "hud.mio_icif.deny.blocked";
                case com.miophas.singularity_iteration.common.suit.quantum.QuantumActions.DENY_NO_TARGET -> "hud.mio_icif.deny.no_target";
                case com.miophas.singularity_iteration.common.suit.quantum.QuantumActions.DENY_NO_WINGMAN -> "hud.mio_icif.deny.no_wingman";
                default -> "hud.mio_icif.deny.no_unit";
            };
            AsciiHud.centred(g, why, x + shake, y0 - 8F, FcsDraw.argb(RED, k * a), 0.56F);
        }
    }

    // ------------------------------------------------------------------ full-screen cues

    private static void screenCues(GuiGraphics g, NanoPackets.CoreSync s, float t, int w, int h) {
        float f = FcsClientConfig.nanoFx() * FcsClientConfig.flashScale();
        if (s.camoTicks() > 0) {
            float k = Mth.clamp(s.camoTicks() / 10F, 0, 1);
            int edge = (int) (22 * k);
            for (int i = 0; i < edge; i += 2) {
                float aa = 0.08F * k * (1 - i / (float) edge) * f;
                int c = FcsDraw.argb(CYAN, aa);
                g.fill(i, i, w - i, i + 1, c);
                g.fill(i, h - i - 1, w - i, h - i, c);
                g.fill(i, i, i + 1, h - i, c);
                g.fill(w - i - 1, i, w - i, h - i, c);
            }
            int band = (int) ((t * 3) % h);
            g.fill(0, band, w, band + 1, FcsDraw.argb(CYAN, 0.04F * f));
        }
        if (s.meshTicks() > 0) {
            int edge = 10;
            for (int i = 0; i < edge; i += 2) {
                int c = FcsDraw.argb(0xFFB300, 0.06F * (1 - i / (float) edge) * f);
                g.fill(i, i, w - i, i + 1, c);
                g.fill(i, h - i - 1, w - i, h - i, c);
            }
        }
        if (NanoClient.dashTick >= 0) {
            float k = 1 - NanoClient.dashTick / 7F;
            java.util.Random r = new java.util.Random(NanoClient.ticks * 17L);
            float cx = w / 2F, cy = h / 2F;
            for (int i = 0; i < 36; i++) {
                double ang = r.nextDouble() * Math.PI * 2;
                float r0 = Math.max(w, h) * (0.34F + 0.18F * r.nextFloat()), r1 = r0 + 30 + 40 * r.nextFloat();
                FcsDraw.line(g, cx + (float) Math.cos(ang) * r0, cy + (float) Math.sin(ang) * r0 * 0.6F,
                    cx + (float) Math.cos(ang) * r1, cy + (float) Math.sin(ang) * r1 * 0.6F, 0.8F, FcsDraw.argb(0xDFFBFF, 0.35F * k * f));
            }
        }
        float sinceEmp = t - NanoFxEmpClock.lastSelfEmp();
        if (sinceEmp >= 0 && sinceEmp < 10) {
            float k = 1 - sinceEmp / 10F;
            g.fill(0, 0, w, h, FcsDraw.argb(0xCFE3FF, 0.20F * k * k * f));
            FcsDraw.circle(g, w / 2F, h / 2F, 20 + sinceEmp * Math.max(w, h) * 0.08F, 2F, FcsDraw.argb(ELECTRIC, 0.5F * k * f));
        }
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
        }
    }

    /** Advanced propulsion core burst pill neatly positioned above the two bottom skill cards. */
    private static void coreBurstChip(GuiGraphics g, LocalPlayer player, float t, int w, int h) {
        var chest = player.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.CHEST);
        if (com.miophas.singularity_iteration.common.suit.core.FrameCores.core(chest) != com.miophas.singularity_iteration.common.suit.core.FrameCore.PROPULSION) return;
        float cx = w / 2F + PARALLAX[0], y = h - 84F + PARALLAX[1];
        int col = 0xFFB74D;
        float cw = 48F, ch = 12F, x0 = cx - cw / 2F;
        int cd = NanoClient.dischargeCooldownLeft(), cdMax = Math.max(1, com.miophas.singularity_iteration.common.suit.core.CoreConfig.dischargeCooldown());
        float sinceDeny = t - NanoClient.coreDeniedAt;
        VisorHudPrimitives.glassCard(g, x0, y, cw, ch, sinceDeny < 10 ? RED : col, cd <= 0 ? 0.80F : 0.35F, 0.62F);
        String key = NanoClient.keyLabel(NanoClient.CORE_KEY, "R");
        g.fill((int) x0 + 2, (int) y + 2, (int) x0 + 9, (int) y + 10, FcsDraw.argb(col, 0.90F));
        float ks = keyScale(key, 0.45F);
        FcsDraw.textCentered(g, key, x0 + 5.5F, y + 3.2F + (0.45F - ks) * 3.5F, FcsDraw.argb(0x140A02, 0.95F), ks);
        Component lbl = Component.translatable("hud.mio_icif.skill.arc");
        FcsDraw.text(g, AsciiHud.t(lbl), x0 + 12F, y + 2.8F, FcsDraw.argb(cd <= 0 ? 0xFFF3E0 : 0x9A8A7A, 0.95F), 0.56F);
        if (cd > 0) {
            float k = 1 - cd / (float) cdMax;
            g.fill((int) x0 + 2, (int) (y + ch - 2), (int) (x0 + 2 + (cw - 4) * k), (int) (y + ch - 1), FcsDraw.argb(col, 0.85F));
        }
        if (sinceDeny < 16) {
            String why = switch (NanoClient.coreDeniedReason) {
                case com.miophas.singularity_iteration.common.suit.core.CoreEvents.DENY_COOLDOWN -> "hud.mio_icif.deny.cooldown";
                case com.miophas.singularity_iteration.common.suit.core.CoreEvents.DENY_ENERGY -> "hud.mio_icif.deny.reserve";
                default -> "hud.mio_icif.deny.no_unit";
            };
            AsciiHud.centred(g, why, cx, y - 8F, FcsDraw.argb(RED, 1 - sinceDeny / 16F), 0.52F);
        }
        float sinceUse = t - NanoClient.dischargeAt;
        if (sinceUse >= 0 && sinceUse < 8) {
            float k = sinceUse / 8F;
            FcsDraw.brackets(g, x0 - 3 * k, y - 3 * k, cw + 6 * k, ch + 6 * k, 3, 0.8F, FcsDraw.argb(col, 1 - k));
        }
    }

    /** Tick of the last EMP of the local player (set by {@link NanoFx}). */
    static final class NanoFxEmpClock {
        static int last = -100;
        static float lastSelfEmp() { return last; }
    }
}
