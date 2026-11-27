// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.client.suit;

import com.miophas.singularity_iteration.common.blockentity.wiring.terminal.NetworkWalker;
import com.miophas.singularity_iteration.common.menu.tool.MeterHudPacket;
import com.miophas.singularity_iteration.common.suit.SuitModuleType;
import com.miophas.singularity_iteration.common.suit.SuitModules;
import com.miophas.singularity_iteration.common.suit.SuitPackets;
import com.miophas.singularity_iteration.common.suit.SuitSensorData;
import it.unimi.dsi.fastutil.ints.Int2ByteMap;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * The FCS HUD layer. Layout follows a tank gunner's sight: a framed field of view with a heading
 * tape at the top, a laser range read-out next to the aim point, data panels at the edges and
 * threat markers on the outer ring. All panels can be moved with the HUD layout editor.
 */
public final class FcsHud {
    private FcsHud() {}

    public static void render(GuiGraphics g, DeltaTracker delta) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || mc.options.hideGui || !FcsClientConfig.hudEnabled()) return;
        if (mc.screen instanceof HudLayoutScreen) return;   // the editor draws its own preview
        float partial = delta.getGameTimeDeltaPartialTick(false);
        boolean hud = FcsState.anyHud();
        boolean flight = ManeuverModeClient.isFlying();
        if (!hud && !flight) return;
        int w = g.guiWidth(), h = g.guiHeight();
        int primary = FcsClientConfig.primary();
        // 0.1.10.0: visor boot (the view opens from a line) and the decoherence glitch (the HUD jumps sideways)
        float boot = bootProgress();
        float glitch = com.miophas.singularity_iteration.common.client.suit.nano.QuantumClient.glitch(partial);
        boolean transformed = boot < 1 || glitch > 0;
        if (transformed) {
            g.flush();
            g.pose().pushPose();
            if (boot < 1) {
                float open = Mth.clamp(boot / 0.3F, 0, 1);
                float sy = 0.02F + 0.98F * open * open * (3 - 2 * open);
                g.pose().translate(0, h / 2F, 0);
                g.pose().scale(1, sy, 1);
                g.pose().translate(0, -h / 2F, 0);
            }
            if (glitch > 0) {
                java.util.Random r = new java.util.Random((long) (System.currentTimeMillis() / 45));
                if (r.nextFloat() < glitch * 0.5F) g.pose().translate((r.nextFloat() - 0.5F) * 10 * glitch, (r.nextFloat() - 0.5F) * 3 * glitch, 0);
            }
        }
        if (hud) {
            if (FcsState.active(SuitModuleType.ORE_SCANNER)) {
                float pulse = 0.8F + 0.2F * (float) Math.sin(System.currentTimeMillis() / 320.0);
                OreScanner.renderHud(g, FcsProjection.camera(), pulse);
            }
            frame(g, mc, player, w, h, primary, partial);
            if (glitch > 0.05F) {
                // colour split: red and cyan copies of the sight frame just beside it
                FcsDraw.brackets(g, 8 - 2 * glitch, 8, w - 16, h - 16, 22, 1.2F, FcsDraw.argb(0xFF2050, 0.4F * glitch));
                FcsDraw.brackets(g, 8 + 2 * glitch, 8, w - 16, h - 16, 22, 1.2F, FcsDraw.argb(0x20E0FF, 0.4F * glitch));
            }
            if (FcsState.active(SuitModuleType.ENTITY_ESP) || FcsState.active(SuitModuleType.BEHAVIOR_PREDICTOR)) tags(g, primary);
            if (FcsState.active(SuitModuleType.BALLISTIC)) ballistic(g, w, h, primary, partial);
            if (FcsState.active(SuitModuleType.BLAST_WARNING)) blast(g, w, h, primary, partial);
            // in flight the aircraft HUD owns the middle band: the status and telemetry panels step aside
            // 0.1.11.5: while the detail view is held, the large holomap is the only panel
            boolean detail = FcsClient.detailHeld() && FcsState.active(SuitModuleType.HOLOMAP);
            if (FcsState.active(SuitModuleType.GRID_TELEMETRY) && !flight && !detail) telemetry(g, w, h, primary);
            if (FcsState.active(SuitModuleType.THREAT_SENSOR)) threats(g, mc, w, h, partial);
            if (FcsState.active(SuitModuleType.HOLOMAP)) holomap(g, w, h, primary, player, partial);
            if (detail) detailPanel(g, player, w, h);
            else HudMessages.status(g, h, 0.85F);
            HudMessages.render(g, w, flight || detail ? 46 : 16, 0.9F);
        }
        if (flight) ManeuverModeClient.renderHud(g, w, h, primary, partial);
        if (transformed) {
            g.flush();
            g.pose().popPose();
        }
        if (boot < 1 && hud) bootOverlay(g, w, h, primary, boot);
        g.flush();
    }

    // ------------------------------------------------------------------ 0.1.10.0 visor boot sequence

    /** Duration of the visor boot, ms. */
    static final long BOOT_MS = 900;
    private static long bootStart = -10_000;

    /** Starts the boot sequence (the FCS turns on: helmet on, HUD key, first unit). */
    static void boot() {
        bootStart = System.currentTimeMillis();
    }

    /** 0..1 through the boot, 1 when done (or when the client option is off). */
    static float bootProgress() {
        if (!FcsClientConfig.visorBoot()) return 1;
        return Mth.clamp((System.currentTimeMillis() - bootStart) / (float) BOOT_MS, 0, 1);
    }

    /**
     * Boot overlay: a bright scan line sweeps down, the aim point calibrates (a ring closing in
     * with tick marks), and the boot log types itself at the left.
     */
    private static void bootOverlay(GuiGraphics g, int w, int h, int primary, float k) {
        // scan line (after the view opened)
        if (k > 0.25F && k < 0.8F) {
            float u = (k - 0.25F) / 0.55F;
            float y = u * h;
            g.fill(0, (int) y - 1, w, (int) y + 1, FcsDraw.argb(0xFFFFFF, 0.55F * (1 - u)));
            g.fill(0, (int) y - 6, w, (int) y - 1, FcsDraw.argb(primary, 0.12F * (1 - u)));
            g.fill(0, (int) y + 1, w, (int) y + 5, FcsDraw.argb(primary, 0.06F * (1 - u)));
        }
        // aim calibration
        float cx = w / 2F, cy = h / 2F;
        if (k > 0.35F) {
            float u = Mth.clamp((k - 0.35F) / 0.5F, 0, 1);
            float r = 40 - 30 * u;
            float a = u < 0.85F ? 0.8F : 0.8F * (1 - (u - 0.85F) / 0.15F);
            FcsDraw.circle(g, cx, cy, r, 1F, FcsDraw.argb(primary, a));
            for (int i = 0; i < 12; i++) {
                float ang = (float) (i * Math.PI / 6 + u * Math.PI);
                FcsDraw.line(g, cx + (float) Math.sin(ang) * (r + 2), cy - (float) Math.cos(ang) * (r + 2),
                    cx + (float) Math.sin(ang) * (r + 5), cy - (float) Math.cos(ang) * (r + 5), 1F, FcsDraw.argb(primary, a));
            }
        }
        // boot log
        String[] log = {"FCS VISOR // BOOT", "OPTICS ........ OK", "SENSOR BUS ..... OK", "QUANTUM LINK ... OK", "CALIBRATING AIM", "READY"};
        int lines = (int) Math.min(log.length, Math.floor(k * (log.length + 1)));
        for (int i = 0; i < lines; i++) {
            boolean last = i == lines - 1;
            String s = log[i];
            if (last && k < 1) s = s.substring(0, Math.max(1, (int) (s.length() * ((k * (log.length + 1)) % 1F))));
            FcsDraw.text(g, s, 20, h * 0.3F + i * 7, FcsDraw.argb(i == log.length - 1 ? 0xFFFFFF : primary, 0.85F), 0.55F);
        }
    }

    static float[] centre(FcsClientConfig.Panel panel, int w, int h) {
        return new float[]{(float) (panel.x() * w), (float) (panel.y() * h)};
    }

    // ------------------------------------------------------------------ sight frame

    private static void frame(GuiGraphics g, Minecraft mc, LocalPlayer player, int w, int h, int primary, float partial) {
        int inset = 8;
        FcsDraw.brackets(g, inset, inset, w - inset * 2, h - inset * 2, 22, 1.2F, FcsDraw.argb(primary, 0.35F));
        float cx = w / 2F, top = 12;
        // 0.1.11.5 passive HUD: the heading tape in flight and in the detail view; the range read-out while there is a
        // ballistic solution or in the detail view (they were always on)
        boolean detail = FcsClient.detailHeld();
        if (FcsClientConfig.showCompass() && detail && !ManeuverModeClient.isFlying()) heading(g, player, cx, top, w, primary, partial);
        if (FcsState.ballistic != null || detail) rangeFinder(g, player, cx, h, primary, partial);
    }

    /** Heading tape at the top of the view (can be switched off in the layout editor). */
    private static void heading(GuiGraphics g, LocalPlayer player, float cx, float top, int w, int primary, float partial) {
        float yaw = Mth.wrapDegrees(player.getViewYRot(partial) + 180F);   // 0 = north
        float span = 90, tapeW = Math.min(220, w * 0.42F);
        g.fill((int) (cx - tapeW / 2), (int) top, (int) (cx + tapeW / 2), (int) top + 13, 0x50060A0C);
        for (int d = -60; d <= 420; d += 5) {
            float rel = Mth.wrapDegrees(d - yaw);
            if (Math.abs(rel) > span / 2) continue;
            float x = cx + rel / span * tapeW;
            boolean major = d % 15 == 0;
            FcsDraw.line(g, x, top + 13, x, top + (major ? 7 : 10), 1F, FcsDraw.argb(primary, major ? 0.95F : 0.55F));
            if (d % 45 == 0) {
                String label = switch (Math.floorMod(d, 360)) {
                    case 0 -> "N"; case 45 -> "NE"; case 90 -> "E"; case 135 -> "SE";
                    case 180 -> "S"; case 225 -> "SW"; case 270 -> "W"; default -> "NW";
                };
                FcsDraw.textCentered(g, label, x, top + 1, FcsDraw.argb(primary, 1F), 0.6F);
            }
        }
        FcsDraw.triangle(g, cx, top + 14, cx - 3, top + 19, cx + 3, top + 19, FcsDraw.argb(primary, 1F));
        FcsDraw.textCentered(g, String.format(Locale.ROOT, "%03d", Math.round(Math.floorMod((int) yaw, 360))), cx, top + 21,
            FcsDraw.argb(primary, 1F), 0.6F);
    }

    private static void rangeFinder(GuiGraphics g, LocalPlayer player, float cx, int h, int primary, float partial) {
        HitResult hit = player.pick(160, partial, false);
        String range = hit.getType() == HitResult.Type.MISS ? "RNG ----" :
            String.format(Locale.ROOT, "RNG %05.1f", hit.getLocation().distanceTo(player.getEyePosition(partial)));
        float my = h / 2F;
        FcsDraw.text(g, range, cx + 9, my + 5, FcsDraw.argb(primary, 0.9F), 0.55F);
        // pitch read-out
        FcsDraw.text(g, String.format(Locale.ROOT, "EL %+05.1f", -player.getViewXRot(partial)), cx + 9, my - 9,
            FcsDraw.argb(primary, 0.75F), 0.55F);
        // aim gate
        FcsDraw.brackets(g, cx - 7, my - 7, 14, 14, 3, 1F, FcsDraw.argb(primary, 0.7F));
    }

    // ------------------------------------------------------------------ status

    private static void status(GuiGraphics g, LocalPlayer player, int w, int h, int primary) {
        var units = FcsState.activeSet();
        float[] c = centre(FcsClientConfig.Panel.STATUS, w, h);
        int pw = 104, ph = 22 + units.size() * 8;
        int x = (int) (c[0] - pw / 2F), y = (int) (c[1] - ph / 2F);
        // 0.1.8.5: the nano helmet is a visor for the units the nano suit can carry
        String visorLabel = SuitModules.hasVisor(player) ? "VISOR OK"
            : SuitModules.isNanoPiece(player.getItemBySlot(EquipmentSlot.HEAD)) ? "NANO VISOR" : "NO VISOR";
        FcsDraw.panel(g, x, y, pw, ph, "FCS  " + visorLabel, primary);
        long visor = SuitModules.energy(player.getItemBySlot(EquipmentSlot.HEAD));
        long visorMax = player.getItemBySlot(EquipmentSlot.HEAD).getItem() instanceof com.miophas.singularity_iteration.core.prefab.item.AbstractElectricArmor a
            ? a.getMaxEnergy(player.getItemBySlot(EquipmentSlot.HEAD)) : 1;
        FcsDraw.text(g, "PWR " + FcsDraw.si(FcsState.drain()) + " EU/t", x + 3, y + 11, FcsDraw.argb(primary, 0.95F), 0.55F);
        FcsDraw.bar(g, x + 56, y + 11, pw - 60, 5, visor / (float) Math.max(1, visorMax), primary);
        int row = 0;
        for (SuitModuleType type : units) {
            int ry = y + 20 + row * 8;
            g.fill(x + 4, ry + 1, x + 7, ry + 4, FcsDraw.argb(type.color(), 1F));
            FcsDraw.text(g, Component.translatable(type.nameKey()), x + 10, ry, FcsDraw.argb(FcsDraw.WHITE, 0.9F), 0.55F);
            FcsDraw.textRight(g, type.drainPerTick() > 0 ? type.drainPerTick() + "" : type == SuitModuleType.DEFLECTOR ? "HIT" : "-",
                x + pw - 3, ry, FcsDraw.argb(primary, 0.8F), 0.55F);
            row++;
        }
    }

    // ------------------------------------------------------------------ ESP / predictor tags

    private static void tags(GuiGraphics g, int primary) {
        boolean esp = FcsState.active(SuitModuleType.ENTITY_ESP);
        boolean predict = FcsState.active(SuitModuleType.BEHAVIOR_PREDICTOR);
        LocalPlayer player = Minecraft.getInstance().player;
        // 0.1.10.1: Mk.II ESP reads health and armour; the Threat Sight synergy adds an impact countdown on the hunters
        boolean espMk2 = esp && SuitModules.tier(player, SuitModuleType.ENTITY_ESP) >= 2;
        boolean threatSight = com.miophas.singularity_iteration.common.suit.quantum.QuantumLinks.active(player,
            com.miophas.singularity_iteration.common.suit.quantum.QuantumSynergy.THREAT_SIGHT);
        int shown = 0;
        for (SensorTracker.Track t : SensorTracker.tracks()) {
            LivingEntity e = t.entity();
            if (!esp && t.intent() == null) continue;
            float pt = Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(false);
            Vec3 pos = e.getPosition(pt);
            float[] top = FcsProjection.project(pos.x, pos.y + e.getBbHeight() + 0.1, pos.z);
            float[] bottom = FcsProjection.project(pos.x, pos.y, pos.z);
            if (top == null || bottom == null) continue;
            float hgt = Math.max(6, bottom[1] - top[1]);
            float wid = Math.max(6, hgt * Math.min(1.2F, e.getBbWidth() / Math.max(0.3F, e.getBbHeight())));
            int threat = SuitSensorData.threatLevel(e.getId());
            int color = threat == 2 ? FcsDraw.RED : threat == 1 ? FcsDraw.YELLOW : SensorTracker.color(t.kind());
            if (esp && shown < 16) {
                FcsDraw.brackets(g, top[0] - wid / 2, top[1], wid, hgt, 4, 1F, FcsDraw.argb(color, 0.9F));
                double d = Math.sqrt(e.distanceToSqr(player));
                String label = String.format(Locale.ROOT, "%s %.0fm", tagName(e), d);
                FcsDraw.textCentered(g, label, top[0], top[1] - 7, FcsDraw.argb(color, 0.95F), 0.5F);
                float hp = e.getHealth() / Math.max(1, e.getMaxHealth());
                g.fill((int) (top[0] - wid / 2), (int) (top[1] + hgt + 2), (int) (top[0] - wid / 2 + wid * hp), (int) (top[1] + hgt + 3),
                    FcsDraw.argb(color, 0.85F));
                if (espMk2) {
                    String stats = String.format(Locale.ROOT, "HP %.0f/%.0f  AR %d", e.getHealth(), e.getMaxHealth(), e.getArmorValue());
                    FcsDraw.textCentered(g, stats, top[0], top[1] + hgt + 5, FcsDraw.argb(color, 0.85F), 0.45F);
                }
                shown++;
            }
            if (threatSight && threat == 2) threatEta(g, e, t.velocity(), player, top, bottom, wid, hgt);
            if (predict && t.intent() != null) {
                int ic = t.intentHot() ? FcsDraw.RED : 0xFF9F1C;
                float blink = t.intentHot() ? (float) (0.6 + 0.4 * Math.sin(System.currentTimeMillis() / 90.0)) : 1F;
                float ix = top[0] + wid / 2 + 3, iy = top[1];
                FcsDraw.triangle(g, ix, iy, ix + 5, iy + 3, ix, iy + 6, FcsDraw.argb(ic, blink));
                // 0.1.10.23 (P7): the intent with its countdown ("AIM 0.6s")
                FcsDraw.text(g, t.intentText(), ix + 6, iy, FcsDraw.argb(ic, blink), 0.55F);
            }
            // 0.1.10.23 (P6): priority rank "#1".."#3"; (P2) "LINK" when the path follows the server's path nodes
            if (predict && t.rank() > 0) {
                int rc = t.rank() == 1 ? FcsDraw.RED : 0xFF9F1C;
                FcsDraw.textRight(g, "#" + t.rank(), top[0] - wid / 2 - 3, top[1], FcsDraw.argb(rc, 1F), 0.6F);
            }
            if (predict && t.linked()) {
                FcsDraw.textRight(g, "LINK", top[0] - wid / 2 - 3, top[1] + 7, FcsDraw.argb(SuitModuleType.BEHAVIOR_PREDICTOR.color(), 0.9F), 0.45F);
            }
        }
    }

    /**
     * 0.1.10.1 Threat Sight: a red ring around a hunter that closes in as its attack gets near, with
     * the estimated time to reach the wearer (distance less 1.5 blocks over the closing speed).
     */
    private static void threatEta(GuiGraphics g, LivingEntity e, Vec3 velocity, LocalPlayer player, float[] top, float[] bottom, float wid, float hgt) {
        Vec3 to = player.position().subtract(e.position());
        double d = to.length();
        if (d < 1.0E-3) return;
        double closing = velocity.dot(to.scale(1 / d)) * 20;          // blocks per second toward the wearer
        float eta = closing > 0.3 ? (float) (Math.max(0, d - 1.5) / closing) : -1;
        float cx = top[0], cy = (top[1] + bottom[1]) / 2;
        float base = Math.max(wid, hgt) * 0.6F + 3;
        float k = eta < 0 ? 1F : Mth.clamp(eta / 3F, 0, 1);
        float pulse = eta >= 0 && eta < 0.8F ? (float) (0.55 + 0.45 * Math.sin(System.currentTimeMillis() / 60.0)) : 0.85F;
        FcsDraw.circle(g, cx, cy, base + 14 * k, 1.1F, FcsDraw.argb(FcsDraw.RED, 0.75F * pulse));
        // four inward ticks that tighten with the countdown
        for (int i = 0; i < 4; i++) {
            float a = (float) (i * Math.PI / 2 + System.currentTimeMillis() / 400.0);
            float r0 = base + 14 * k + 2, r1 = r0 + 4;
            FcsDraw.line(g, cx + (float) Math.sin(a) * r0, cy - (float) Math.cos(a) * r0, cx + (float) Math.sin(a) * r1, cy - (float) Math.cos(a) * r1,
                1.2F, FcsDraw.argb(FcsDraw.RED, pulse));
        }
        String label = eta < 0 ? "HUNTING" : String.format(Locale.ROOT, "ETA %.1fs", eta);
        FcsDraw.textCentered(g, label, cx, cy + base + 14 * k + 3, FcsDraw.argb(FcsDraw.RED, pulse), 0.5F);
    }

    private static String tagName(Entity e) {
        String name = e.getType().getDescription().getString();
        if (e.hasCustomName() && e.getCustomName() != null) name = e.getCustomName().getString();
        if (name.length() > 12) name = name.substring(0, 12);
        return name.toUpperCase(Locale.ROOT);
    }

    // ------------------------------------------------------------------ ballistic

    private static final int ARC_READY = 0xFFB347, ARC_CHARGE = 0x9AA7B0;

    /**
     * Ballistic computer: a flowing dotted arc that tapers and brightens towards the impact, a
     * surface-aligned segmented reticle at the impact point with a range chip, target brackets and
     * a lead diamond with a dashed tether. A slim data panel keeps the numbers.
     */
    private static void ballistic(GuiGraphics g, int w, int h, int primary, float partial) {
        BallisticSolver.Solution s = FcsState.ballistic;
        if (s == null) return;
        long now = System.currentTimeMillis();
        boolean onTarget = s.target() != null;
        int c = onTarget ? FcsDraw.RED : s.profile().ready() ? ARC_READY : ARC_CHARGE;
        Vec3 cam = FcsProjection.camera();
        List<Vec3> path = s.path();
        // dotted arc
        double spacing = 0.85, phase = (now % 700) / 700.0 * spacing, run = 0, next = 1.6 + phase;
        int total = path.size();
        float[] prev = null;
        for (int i = 1; i < total; i++) {
            Vec3 a = path.get(i - 1), b = path.get(i);
            double seg = a.distanceTo(b);
            float[] pb = FcsProjection.project(b);
            float t = i / (float) total;
            if (prev != null && pb != null && i % 2 == 0) FcsDraw.line(g, prev[0], prev[1], pb[0], pb[1], 0.8F, FcsDraw.argb(c, 0.18F + 0.35F * t));
            if (pb != null) prev = pb;
            while (run + seg >= next) {
                double f = (next - run) / seg;
                Vec3 q = a.add(b.subtract(a).scale(f));
                float[] pq = FcsProjection.project(q);
                if (pq != null) {
                    double dist = Math.max(1, q.distanceTo(cam));
                    float r = (float) Mth.clamp(26 / dist, 1.1, 3.2);
                    float alpha = 0.35F + 0.6F * t;
                    FcsDraw.quad(g, pq[0], pq[1] - r, pq[0] + r, pq[1], pq[0], pq[1] + r, pq[0] - r, pq[1], FcsDraw.argb(c, alpha));
                }
                next += spacing;
            }
            run += seg;
        }
        // 0.1.10.1 Mk.II: the side shots of a multishot weapon, thin dotted arcs with a small impact diamond
        for (BallisticSolver.Solution side : FcsState.spread) {
            float[] last = null;
            List<Vec3> sp = side.path();
            for (int i = 1; i < sp.size(); i += 2) {
                float[] q = FcsProjection.project(sp.get(i));
                if (q != null && last != null) FcsDraw.line(g, last[0], last[1], q[0], q[1], 0.6F, FcsDraw.argb(c, 0.15F + 0.3F * i / sp.size()));
                if (q != null) last = q;
            }
            if (side.impact() != null) {
                float[] q = FcsProjection.project(side.impact());
                if (q != null) {
                    FcsDraw.quad(g, q[0], q[1] - 2.5F, q[0] + 2.5F, q[1], q[0], q[1] + 2.5F, q[0] - 2.5F, q[1], FcsDraw.argb(side.target() != null ? FcsDraw.RED : c, 0.85F));
                }
            }
        }
        // impact reticle
        if (s.impact() != null) {
            Vec3 n = s.normal() != null ? s.normal() : new Vec3(0, 1, 0);
            Vec3 at = s.impact().add(n.scale(0.03));
            Vec3 tx = Math.abs(n.y) < 0.9 ? n.cross(new Vec3(0, 1, 0)).normalize() : n.cross(new Vec3(1, 0, 0)).normalize();
            Vec3 ty = n.cross(tx).normalize();
            float spin = (now % 3000) / 3000F * (float) Math.PI * 2;
            for (int k = 0; k < 4; k++) {
                float a0 = spin + k * (float) Math.PI / 2;
                worldArc(g, at, tx, ty, 0.62, a0, a0 + 1.05F, 1.6F, FcsDraw.argb(c, 0.95F));
            }
            worldArc(g, at, tx, ty, 0.30, 0, (float) Math.PI * 2, 0.8F, FcsDraw.argb(c, 0.7F));
            for (int k = 0; k < 4; k++) {
                double ang = -spin * 0.5 + k * Math.PI / 2;
                Vec3 dir = tx.scale(Math.cos(ang)).add(ty.scale(Math.sin(ang)));
                float[] p0 = FcsProjection.project(at.add(dir.scale(0.72))), p1 = FcsProjection.project(at.add(dir.scale(0.95)));
                if (p0 != null && p1 != null) FcsDraw.line(g, p0[0], p0[1], p1[0], p1[1], 1.2F, FcsDraw.argb(c, 0.9F));
            }
            float[] pc = FcsProjection.project(at);
            if (pc != null) {
                FcsDraw.disc(g, pc[0], pc[1], 1.6F, FcsDraw.argb(c, 1F));
                double range = path.isEmpty() ? 0 : s.impact().distanceTo(path.get(0));
                FcsDraw.chip(g, String.format(Locale.ROOT, "%.1fm", range), pc[0] + 10, pc[1] - 12, c, 0.5F);
                FcsDraw.text(g, String.format(Locale.ROOT, "%.2fs", s.ticks() / 20.0), pc[0] + 11, pc[1] - 3, FcsDraw.argb(c, 0.9F), 0.5F);
            }
        }
        // target brackets
        if (onTarget) {
            var box = s.target().getBoundingBox();
            float[] lo = null, hi = null;
            float minX = Float.MAX_VALUE, minY = Float.MAX_VALUE, maxX = -Float.MAX_VALUE, maxY = -Float.MAX_VALUE;
            boolean ok = true;
            for (int i = 0; i < 8 && ok; i++) {
                float[] q = FcsProjection.project((i & 1) == 0 ? box.minX : box.maxX, (i & 2) == 0 ? box.minY : box.maxY, (i & 4) == 0 ? box.minZ : box.maxZ);
                if (q == null) { ok = false; break; }
                minX = Math.min(minX, q[0]); maxX = Math.max(maxX, q[0]); minY = Math.min(minY, q[1]); maxY = Math.max(maxY, q[1]);
            }
            if (ok) {
                float pad = 3 + 2 * (float) Math.sin(now / 120.0);
                FcsDraw.brackets(g, minX - pad, minY - pad, maxX - minX + pad * 2, maxY - minY + pad * 2, 6, 1.4F, FcsDraw.argb(FcsDraw.RED, 0.95F));
                FcsDraw.chip(g, "LOCK", minX - pad, maxY + pad + 2, FcsDraw.RED, 0.5F);
            }
        }
        // lead diamond and tether
        if (FcsState.leadPoint != null && FcsState.leadTarget != null) {
            float[] p = FcsProjection.project(FcsState.leadPoint);
            float[] from = FcsProjection.project(FcsState.leadTarget.getBoundingBox().getCenter());
            if (p != null) {
                if (from != null) FcsDraw.dashed(g, from[0], from[1], p[0], p[1], 2.5F, 0.8F, FcsDraw.argb(FcsDraw.YELLOW, 0.7F));
                float r = 4.5F;
                FcsDraw.line(g, p[0], p[1] - r, p[0] + r, p[1], 1.2F, FcsDraw.argb(FcsDraw.YELLOW, 1F));
                FcsDraw.line(g, p[0] + r, p[1], p[0], p[1] + r, 1.2F, FcsDraw.argb(FcsDraw.YELLOW, 1F));
                FcsDraw.line(g, p[0], p[1] + r, p[0] - r, p[1], 1.2F, FcsDraw.argb(FcsDraw.YELLOW, 1F));
                FcsDraw.line(g, p[0] - r, p[1], p[0], p[1] - r, 1.2F, FcsDraw.argb(FcsDraw.YELLOW, 1F));
                FcsDraw.disc(g, p[0], p[1], 1.2F, FcsDraw.argb(FcsDraw.YELLOW, 1F));
                FcsDraw.text(g, "LEAD", p[0] + 7, p[1] - 3, FcsDraw.argb(FcsDraw.YELLOW, 0.95F), 0.5F);
            }
        }
        // 0.1.11.5: the solution as a text card (it shows only while there is a solution)
        float[] pc = centre(FcsClientConfig.Panel.BALLISTIC, w, h);
        double range = s.impact() != null && !path.isEmpty() ? s.impact().distanceTo(path.get(0)) : -1;
        String state = onTarget ? "ON TGT" : s.profile().ready() ? "READY" : "CHARGE";
        String[][] rows = {
            {"TOF", String.format(Locale.ROOT, "%.2f s", s.ticks() / 20.0)},
            {"RNG", range < 0 ? "OUT" : String.format(Locale.ROOT, "%.1f m", range)},
            {"DROP", String.format(Locale.ROOT, "%.1f m", s.drop())}};
        float sc = 0.8F, lh = AsciiHud.LINE * sc, pw = 100, ph = lh * 5;
        float x = pc[0] - pw / 2F, y = pc[1] - ph / 2F;
        AsciiHud.box(g, x, y, pw, ph, Component.literal(s.profile().name()), onTarget ? AsciiHud.OK : AsciiHud.DIM, sc);
        AsciiHud.textRight(g, state, x + pw - AsciiHud.width("\u2502 ", sc), y + lh, onTarget ? AsciiHud.OK : s.profile().ready() ? AsciiHud.TEXT : AsciiHud.WARN, sc);
        for (int i = 0; i < 3; i++) {
            AsciiHud.text(g, rows[i][0], x + AsciiHud.width("\u2502 ", sc), y + lh * (i + 1), AsciiHud.DIM, sc);
            if (i > 0) AsciiHud.textRight(g, rows[i][1], x + pw - AsciiHud.width("\u2502 ", sc), y + lh * (i + 1), AsciiHud.TEXT, sc);
            else AsciiHud.text(g, rows[i][1], x + AsciiHud.width("\u2502 DROP  ", sc), y + lh * (i + 1), AsciiHud.TEXT, sc);
        }
    }

    /** Label left, value right, dotted leader between. */
    private static void row(GuiGraphics g, int x, int y, int pw, String label, String value) {
        FcsDraw.text(g, label, x + 5, y, FcsDraw.argb(FcsDraw.WHITE, 0.6F), 0.55F);
        float lw = FcsDraw.width(label, 0.55F), vw = FcsDraw.width(value, 0.55F);
        for (float dx = x + 8 + lw; dx < x + pw - 6 - vw; dx += 3) g.fill((int) dx, y + 4, (int) dx + 1, y + 5, 0x40FFFFFF);
        FcsDraw.textRight(g, value, x + pw - 4, y, FcsDraw.argb(FcsDraw.WHITE, 0.95F), 0.55F);
    }

    /** Arc of a circle in the world (centre, plane axes tx/ty), drawn in screen space. */
    private static void worldArc(GuiGraphics g, Vec3 c, Vec3 tx, Vec3 ty, double r, float a0, float a1, float width, int argb) {
        int steps = Math.max(3, (int) Math.ceil(Math.abs(a1 - a0) / 0.2F));
        float[] prev = null;
        for (int i = 0; i <= steps; i++) {
            double a = a0 + (a1 - a0) * i / (double) steps;
            float[] q = FcsProjection.project(c.add(tx.scale(Math.cos(a) * r)).add(ty.scale(Math.sin(a) * r)));
            if (q != null && prev != null) FcsDraw.line(g, prev[0], prev[1], q[0], q[1], width, argb);
            prev = q;
        }
    }

    // ------------------------------------------------------------------ blast warning

    /**
     * Blast warning: a ground danger zone that fills as the fuse burns, a fuse-ring icon over each
     * charge, a direction indicator around the aim point for charges close to the wearer, a red
     * screen edge inside the blast radius, and a compact list. The beeps come from FcsClient.
     */
    private static void blast(GuiGraphics g, int w, int h, int primary, float partial) {
        List<SensorTracker.Blast> blasts = SensorTracker.blasts();
        if (blasts.isEmpty()) return;
        LocalPlayer player = Minecraft.getInstance().player;
        long now = System.currentTimeMillis();
        float cx = w / 2F, cy = h / 2F;
        boolean inside = false;
        // 0.1.10.1 Blast Forecast synergy: the blast as a 3D sphere and an escape arrow
        boolean forecast = com.miophas.singularity_iteration.common.suit.quantum.QuantumLinks.active(player,
            com.miophas.singularity_iteration.common.suit.quantum.QuantumSynergy.BLAST_FORECAST);
        for (SensorTracker.Blast b : blasts) {
            double d = Math.sqrt(b.entity().distanceToSqr(player));
            boolean in = d < b.radius();
            inside |= in;
            float burnt = b.burnt();
            int col = b.urgent() || in ? FcsDraw.RED : FcsDraw.YELLOW;
            float flash = b.urgent() ? (float) (0.6 + 0.4 * Math.sin(now / 55.0)) : 1F;
            Vec3 at = b.entity().getPosition(partial);
            if (forecast) blastForecast(g, b, at, player, partial, col, flash, in, w, h);
            // ground danger zone
            dangerZone(g, at.add(0, 0.06, 0), b.radius(), burnt, col, flash);
            // fuse ring icon over the charge
            float[] p = FcsProjection.project(at.x, at.y + b.entity().getBbHeight() + 0.9, at.z);
            if (p != null && p[0] > 0 && p[0] < w && p[1] > 0 && p[1] < h) fuseIcon(g, p[0], p[1], 7, b, col, flash);
            // indicator around the aim point when the charge is near
            if (d < b.radius() * 2.5) {
                Vec3 mid = at.add(0, b.entity().getBbHeight() * 0.5, 0);
                float bearing = FcsProjection.bearing(mid.x, mid.y, mid.z);
                float r = 52;
                float ix = cx + (float) Math.sin(bearing) * r, iy = cy - (float) Math.cos(bearing) * r;
                float bx = (float) Math.sin(bearing), by = (float) -Math.cos(bearing);
                float ax = cx + bx * (r + 13), ay = cy + by * (r + 13);
                FcsDraw.triangle(g, ax + bx * 5, ay + by * 5, ax - by * 4, ay + bx * 4, ax + by * 4, ay - bx * 4, FcsDraw.argb(col, flash));
                fuseIcon(g, ix, iy, 8, b, col, flash);
                FcsDraw.textCentered(g, String.format(Locale.ROOT, "%.0fm", d), ix, iy + 11, FcsDraw.argb(col, 0.9F), 0.5F);
            }
        }
        if (inside) {
            float pulse = (float) (0.5 + 0.5 * Math.sin(now / 80.0));
            for (int k = 0; k < 4; k++) {
                int a = FcsDraw.argb(FcsDraw.RED, (0.26F - k * 0.06F) * pulse);
                int bw = 2 + k * 4;
                g.fill(0, k * 4, w, bw, a); g.fill(0, h - bw, w, h - k * 4, a);
                g.fill(k * 4, bw, bw, h - bw, a); g.fill(w - bw, bw, w - k * 4, h - bw, a);
            }
        }
        // 0.1.11.5: the blast list panel is gone; the "blast" message of HudMessages says the nearest charge
    }

    /**
     * 0.1.10.1 Blast Forecast: the blast as a wire sphere (latitude rings and meridians, the front
     * half brighter), and for a blast that reaches the wearer an escape arrow from the aim point to
     * the nearest safe spot, with the distance to run and the seconds left.
     */
    private static void blastForecast(GuiGraphics g, SensorTracker.Blast b, Vec3 at, LocalPlayer player, float partial, int col, float flash,
                                      boolean in, int w, int h) {
        Vec3 c = at.add(0, b.entity().getBbHeight() * 0.5, 0);
        float r = b.radius();
        float spin = (System.currentTimeMillis() % 6000) / 6000F * (float) Math.PI * 2;
        for (int i = -2; i <= 2; i++) {
            double lat = i * Math.PI / 6;
            Vec3 rc = c.add(0, Math.sin(lat) * r, 0);
            worldArc(g, rc, new Vec3(1, 0, 0), new Vec3(0, 0, 1), Math.cos(lat) * r, 0, (float) Math.PI * 2, i == 0 ? 1.2F : 0.7F,
                FcsDraw.argb(col, (i == 0 ? 0.55F : 0.3F) * flash));
        }
        for (int j = 0; j < 4; j++) {
            double lon = j * Math.PI / 4 + spin * 0.2;
            Vec3 ax = new Vec3(Math.cos(lon), 0, Math.sin(lon));
            worldArc(g, c, ax, new Vec3(0, 1, 0), r, 0, (float) Math.PI * 2, 0.6F, FcsDraw.argb(col, 0.25F * flash));
        }
        if (!in && player.distanceTo(b.entity()) > r * 1.4F) return;
        Vec3 away = player.position().subtract(at);
        away = new Vec3(away.x, 0, away.z);
        if (away.lengthSqr() < 1.0E-4) away = player.getViewVector(partial).multiply(-1, 0, -1);
        away = away.normalize();
        double run = Math.max(0, r - Math.sqrt(player.distanceToSqr(at.x, player.getY(), at.z))) + 1.0;
        Vec3 safe = player.getPosition(partial).add(away.scale(run)).add(0, 0.1, 0);
        float[] p = FcsProjection.project(safe);
        float sx = w / 2F, sy = h / 2F;
        float tx, ty;
        if (p != null) { tx = p[0]; ty = p[1]; }
        else {                                                         // behind the wearer: point to the edge of the view
            float bearing = FcsProjection.bearing(safe.x, safe.y, safe.z);
            tx = sx + (float) Math.sin(bearing) * 70;
            ty = sy - (float) Math.cos(bearing) * 70;
        }
        float dx = tx - sx, dy = ty - sy, len = (float) Math.sqrt(dx * dx + dy * dy);
        if (len < 12) return;
        float ux = dx / len, uy = dy / len;
        float x0 = sx + ux * 14, y0 = sy + uy * 14, x1 = sx + ux * Math.min(len, 90), y1 = sy + uy * Math.min(len, 90);
        FcsDraw.line(g, x0, y0, x1, y1, 2.2F, FcsDraw.argb(0x5CFF9A, 0.9F * flash));
        FcsDraw.triangle(g, x1 + ux * 7, y1 + uy * 7, x1 - uy * 5, y1 + ux * 5, x1 + uy * 5, y1 - ux * 5, FcsDraw.argb(0x5CFF9A, flash));
        FcsDraw.text(g, String.format(Locale.ROOT, "EVADE %.1fm  %ss", run, b.secondsText()), x1 + 8, y1 - 3, FcsDraw.argb(0x5CFF9A, flash), 0.55F);
    }

    /** Disc icon: dark core, warning glyph, ring that empties as the fuse burns, countdown chip. */
    private static void fuseIcon(GuiGraphics g, float x, float y, float r, SensorTracker.Blast b, int col, float flash) {
        FcsDraw.disc(g, x, y, r, 0xB0050808);
        float left = 1 - b.burnt();
        FcsDraw.circle(g, x, y, r, 0.8F, FcsDraw.argb(col, 0.35F));
        FcsDraw.arc(g, x, y, r, 0, left * (float) Math.PI * 2, 1.8F, FcsDraw.argb(col, flash));
        // "!" glyph
        FcsDraw.quad(g, x - 0.9F, y - r * 0.55F, x + 0.9F, y - r * 0.55F, x + 0.6F, y + r * 0.15F, x - 0.6F, y + r * 0.15F, FcsDraw.argb(col, flash));
        FcsDraw.disc(g, x, y + r * 0.42F, 0.9F, FcsDraw.argb(col, flash));
        FcsDraw.chip(g, b.secondsText(), x + r + 2, y - 4, col, 0.5F);
    }

    /** Ground circle of the blast radius: faint fill, a sector that fills with the burnt fuse, rim. */
    private static void dangerZone(GuiGraphics g, Vec3 c, float radius, float burnt, int col, float flash) {
        int steps = 40;
        float[] centre = FcsProjection.project(c);
        float[][] rim = new float[steps + 1][];
        for (int i = 0; i <= steps; i++) {
            double a = i * Math.PI * 2 / steps;
            rim[i] = FcsProjection.project(c.x + Math.sin(a) * radius, c.y, c.z - Math.cos(a) * radius);
        }
        int fill = FcsDraw.argb(col, 0.10F * flash), burn = FcsDraw.argb(col, 0.24F * flash);
        int burnSteps = Math.round(burnt * steps);
        for (int i = 0; i < steps; i++) {
            if (centre == null || rim[i] == null || rim[i + 1] == null) continue;
            FcsDraw.triangle(g, centre[0], centre[1], rim[i][0], rim[i][1], rim[i + 1][0], rim[i + 1][1], i < burnSteps ? burn : fill);
        }
        for (int i = 0; i < steps; i++) {
            if (rim[i] == null || rim[i + 1] == null) continue;
            boolean dash = (i & 1) == 0;
            FcsDraw.line(g, rim[i][0], rim[i][1], rim[i + 1][0], rim[i + 1][1], dash ? 1.4F : 0.7F, FcsDraw.argb(col, (dash ? 0.9F : 0.5F) * flash));
        }
    }

    // ------------------------------------------------------------------ grid telemetry

    /**
     * 0.1.11.5: grid telemetry is a text card beside the aim point, only while the wearer looks at a grid node (it was a
     * fixed panel with the chunk summary all the time). The chunk summary is in the detail view.
     */
    private static void telemetry(GuiGraphics g, int w, int h, int primary) {
        MeterHudPacket.Reply reply = MeterHudPacket.latest();
        var pos = FcsClient.telemetryPos();
        if (pos == null || reply == null || !reply.pos().equals(pos)) return;
        boolean over = reply.ratedPacket() > 0 && reply.voltage() > reply.ratedPacket();
        String state = reply.throughput() <= 0 ? "IDLE" : over ? "OVER" : "LIVE";
        String rated = reply.ratedPacket() > 0 ? NetworkWalker.tierName(reply.ratedPacket()) + " " + FcsDraw.si(reply.ratedPacket()) : "-";
        String[][] rows = {
            {"LOS", NetworkWalker.tierName(reply.voltage()) + "  " + FcsDraw.si(reply.voltage()) + " EU"},
            {"FLOW", FcsDraw.si(reply.throughput()) + " EU/t"},
            {"LINE", rated + "  N" + reply.endpoints()}};
        float sc = 0.8F, lh = AsciiHud.LINE * sc, side = AsciiHud.width("\u2502 ", sc), label = AsciiHud.width("FLOW  ", sc);
        float pw = 0;
        for (String[] r : rows) pw = Math.max(pw, side * 2 + label + AsciiHud.width(r[1], sc));
        pw = Math.max(pw, AsciiHud.width(Component.translatable("hud.mio_icif.telemetry.title"), sc) + side * 2 + AsciiHud.width(state, sc) + 20);
        float ph = lh * 5, x = w / 2F - 22 - pw, y = h / 2F + 10;   // below left of the aim point (the ballistic card is on the right)
        AsciiHud.box(g, x, y, pw, ph, Component.translatable("hud.mio_icif.telemetry.title"), over ? AsciiHud.CRIT : AsciiHud.WARN, sc);
        AsciiHud.textRight(g, state, x + pw - side, y + lh, over ? AsciiHud.CRIT : AsciiHud.OK, sc);
        for (int i = 0; i < 3; i++) {
            AsciiHud.text(g, rows[i][0], x + side, y + lh * (i + 1), AsciiHud.DIM, sc);
            if (i > 0 || AsciiHud.width(rows[i][1], sc) + label + side * 2 + AsciiHud.width(state, sc) < pw)
                AsciiHud.text(g, rows[i][1], x + side + label, y + lh * (i + 1), AsciiHud.TEXT, sc);
        }
    }

    /**
     * 0.1.11.5: the left panel of the detail view (held detail key): visor energy and drain, flight, the active units
     * with their drain, the chunk grid summary (grid telemetry), and the recent messages.
     */
    private static void detailPanel(GuiGraphics g, LocalPlayer player, int w, int h) {
        float sc = 0.85F, lh = AsciiHud.LINE * sc, side = AsciiHud.width("\u2502 ", sc);
        List<Object[]> rows = new java.util.ArrayList<>();   // {label, value, colour, right}
        var helmet = player.getItemBySlot(EquipmentSlot.HEAD);
        long visor = SuitModules.energy(helmet);
        long visorMax = helmet.getItem() instanceof com.miophas.singularity_iteration.core.prefab.item.AbstractElectricArmor a ? a.getMaxEnergy(helmet) : 1;
        float f = visor / (float) Math.max(1, visorMax);
        rows.add(new Object[] {Component.translatable("hud.mio_icif.detail.energy"), AsciiHud.bar(f, 10) + " " + Math.round(f * 100) + "%",
            f < 0.2F ? AsciiHud.WARN : AsciiHud.TEXT, "-" + FcsDraw.si(FcsState.drain()) + " EU/t"});
        rows.add(new Object[] {Component.translatable("hud.mio_icif.detail.flight"), ManeuverModeClient.isFlying()
            ? Math.round(ManeuverModeClient.speed() * 3.6) + " km/h" : Component.translatable("hud.mio_icif.detail.idle").getString(), AsciiHud.TEXT, ""});
        rows.add(null);
        rows.add(new Object[] {Component.translatable("hud.mio_icif.detail.units"), "", AsciiHud.DIM, "EU/t"});
        for (SuitModuleType type : FcsState.activeSet()) {
            rows.add(new Object[] {Component.translatable(type.nameKey()), "", AsciiHud.TEXT,
                type.drainPerTick() > 0 ? String.valueOf(type.drainPerTick()) : "-"});
        }
        SuitPackets.ChunkGrid grid = SuitSensorData.chunkGrid();
        if (grid != null && FcsState.active(SuitModuleType.GRID_TELEMETRY)) {
            rows.add(null);
            rows.add(new Object[] {Component.translatable("hud.mio_icif.detail.chunk"), String.format(Locale.ROOT, "%d,%d  %s",
                grid.chunkX(), grid.chunkZ(), NetworkWalker.tierName(grid.maxVoltage())), AsciiHud.TEXT,
                "+" + FcsDraw.si(grid.generated()) + " -" + FcsDraw.si(grid.consumed())});
        }
        var history = HudMessages.history();
        if (!history.isEmpty()) {
            rows.add(null);
            rows.add(new Object[] {Component.translatable("hud.mio_icif.detail.recent"), "", AsciiHud.DIM, ""});
            long now = player.level().getGameTime();
            for (var m : history) {
                int col = m.level() == HudMessages.CRIT ? AsciiHud.CRIT : m.level() == HudMessages.WARN ? AsciiHud.WARN : AsciiHud.OK;
                rows.add(new Object[] {m.label(), m.detail().getString(), col, "-" + Math.max(0, (now - m.gameTime()) / 20) + "s"});
            }
        }
        float labelW = 0, valueW = 0, rightW = 0;
        for (Object[] r : rows) {
            if (r == null) continue;
            labelW = Math.max(labelW, AsciiHud.width((Component) r[0], sc));
            valueW = Math.max(valueW, AsciiHud.width((String) r[1], sc));
            rightW = Math.max(rightW, AsciiHud.width((String) r[3], sc));
        }
        float gap = AsciiHud.width("  ", sc);
        Component title = Component.translatable("hud.mio_icif.detail.title");
        // 0.1.11.230: the detail key, or (unbound by default) the panel key held
        var detailKey = FcsClient.DETAIL_KEY != null && !FcsClient.DETAIL_KEY.isUnbound() ? FcsClient.DETAIL_KEY
            : com.miophas.singularity_iteration.common.client.mio_icif_ClientEvents.ARMOR_FEATURES_KEY;
        Component hint = Component.translatable("hud.mio_icif.detail.hold", detailKey == null ? Component.literal("Alt") : detailKey.getTranslatedKeyMessage());
        float pw = Math.max(side * 2 + labelW + gap + valueW + gap + rightW, AsciiHud.width(title, sc) + AsciiHud.width(hint, sc) + side * 4);
        float ph = lh * (rows.size() + 3);
        float x = 12, y = 34;
        AsciiHud.box(g, x, y, pw, ph, title, AsciiHud.DIM, sc);
        AsciiHud.text(g, hint, x + pw - side - AsciiHud.width(hint, sc), y + lh, AsciiHud.DIM, sc);
        float ry = y + lh * 2;
        for (Object[] r : rows) {
            if (r != null) {
                int col = (int) r[2];
                AsciiHud.text(g, (Component) r[0], x + side, ry, col == AsciiHud.TEXT ? AsciiHud.DIM : col, sc);
                AsciiHud.text(g, (String) r[1], x + side + labelW + gap, ry, col, sc);
                AsciiHud.textRight(g, (String) r[3], x + pw - side, ry, AsciiHud.DIM, sc);
            } else {
                FcsDraw.line(g, x + side, ry + lh * 0.45F, x + pw - side, ry + lh * 0.45F, 1F, FcsDraw.argb(AsciiHud.DIM, 0.5F));
            }
            ry += lh;
        }
    }

    // ------------------------------------------------------------------ threats

    /** 0.1.10.23 (P7): colour of the incoming projectile markers. */
    private static final int INCOMING = 0xFFB347;

    /**
     * 0.1.10.23 (P7): the incoming projectiles ({@link ProjectileWatch}): a marker on the ring at the bearing of each
     * with the seconds to its closest approach; one that hits flashes red and the soonest hit gets an arrow beside the
     * aim point that shows which way to step; a banner "INCOMING n".
     */
    private static void incoming(GuiGraphics g, Minecraft mc, int w, int h, float partial) {
        List<ProjectileWatch.Alert> alerts = ProjectileWatch.alerts();
        if (alerts.isEmpty() || mc.player == null) return;
        float cx = w / 2F, cy = h / 2F, r = Math.min(w, h) * 0.34F;
        float flash = (float) (0.55 + 0.45 * Math.sin(System.currentTimeMillis() / 60.0));
        ProjectileWatch.Alert soonestHit = null;
        int hits = 0;
        for (ProjectileWatch.Alert a : alerts) {
            Vec3 p = a.projectile().getPosition(partial);
            float bearing = FcsProjection.bearing(p.x, p.y, p.z);
            boolean hit = a.warning().hit();
            if (hit) { hits++; if (soonestHit == null) soonestHit = a; }
            int c = hit ? FcsDraw.RED : INCOMING;
            float alpha = hit ? flash : 0.8F;
            float bx = (float) Math.sin(bearing), by = (float) -Math.cos(bearing);
            float tx = cx + bx * r, ty = cy + by * r;
            // an inward chevron at the bearing
            FcsDraw.triangle(g, tx - bx * 6, ty - by * 6, tx - by * 4, ty + bx * 4, tx + by * 4, ty - bx * 4, FcsDraw.argb(c, alpha));
            FcsDraw.textCentered(g, String.format(Locale.ROOT, "%.1fs", a.seconds()), tx + bx * 8, ty + by * 8 - 3, FcsDraw.argb(c, alpha), 0.5F);
        }
        if (soonestHit != null) {
            // step-aside arrow beside the aim point (world direction -> screen bearing)
            LocalPlayer player = mc.player;
            var ww = soonestHit.warning();
            Vec3 eye = player.getEyePosition(partial);
            float b = FcsProjection.bearing(eye.x + ww.evadeX() * 3, eye.y, eye.z + ww.evadeZ() * 3);
            float bx = (float) Math.sin(b), by = (float) -Math.cos(b);
            float ax = cx + bx * 26, ay = cy + by * 26;
            int col = FcsDraw.argb(FcsDraw.RED, flash);
            FcsDraw.line(g, cx + bx * 14, cy + by * 14, ax, ay, 2F, col);
            FcsDraw.triangle(g, ax + bx * 7, ay + by * 7, ax - by * 4, ay + bx * 4, ax + by * 4, ay - bx * 4, col);
            FcsDraw.textCentered(g, "EVADE", ax + bx * 14, ay + by * 14 - 3, col, 0.5F);
        }
        // 0.1.11.5: the "INCOMING n" banner is the "incoming" message of HudMessages now
    }

    private static void threats(GuiGraphics g, Minecraft mc, int w, int h, float partial) {
        incoming(g, mc, w, h, partial);
        Int2ByteMap map = SuitSensorData.threats();
        if (map.isEmpty()) return;
        float cx = w / 2F, cy = h / 2F;
        float r = Math.min(w, h) * 0.40F;
        int red = 0, yellow = 0;
        float pulse = (float) (0.55 + 0.45 * Math.sin(System.currentTimeMillis() / 110.0));
        for (Int2ByteMap.Entry entry : map.int2ByteEntrySet()) {
            Entity e = mc.level.getEntity(entry.getIntKey());
            if (e == null) continue;
            int level = entry.getByteValue();
            if (level == 2) red++; else yellow++;
            Vec3 p = e.getPosition(partial).add(0, e.getBbHeight() * 0.6, 0);
            float bearing = FcsProjection.bearing(p.x, p.y, p.z);
            int c = level == 2 ? FcsDraw.RED : FcsDraw.YELLOW;
            float a = level == 2 ? pulse : 0.85F;
            FcsDraw.arc(g, cx, cy, r, bearing - 0.16F, bearing + 0.16F, 2.4F, FcsDraw.argb(c, a));
            float tx = cx + (float) Math.sin(bearing) * (r + 7), ty = cy - (float) Math.cos(bearing) * (r + 7);
            float bx = (float) Math.sin(bearing), by = (float) -Math.cos(bearing);
            FcsDraw.triangle(g, tx + bx * 5, ty + by * 5, tx - by * 3.5F, ty + bx * 3.5F, tx + by * 3.5F, ty - bx * 3.5F, FcsDraw.argb(c, a));
        }
        // outer frame tint and banner
        if (red > 0) {
            int edge = FcsDraw.argb(FcsDraw.RED, 0.22F * pulse);
            g.fill(0, 0, w, 3, edge);
            g.fill(0, h - 3, w, h, edge);
            g.fill(0, 0, 3, h, edge);
            g.fill(w - 3, 0, w, h, edge);
        }
        // 0.1.11.5: the "LOCK n  WATCH n" banner is the "threat" message of HudMessages now
    }

    // ------------------------------------------------------------------ holomap

    /**
     * 0.1.11.5 (H3, H4): the holomap is passive. It shows small in its panel place only while a hostile is in range or a
     * sonar result is fresh (and {@link HoloMap#LINGER_MS} after), and large in the middle of the screen while the detail
     * key is held (wheel: zoom, Shift + wheel: elevation). It used to show all the time.
     */
    private static void holomap(GuiGraphics g, int w, int h, int primary, LocalPlayer player, float partial) {
        if (FcsClient.detailHeld()) {
            // above the hotbar (about 44 GUI pixels) and below the heading tape
            // the right side: the detail panel takes the left
            float size = Math.min(w * 0.5F, (h - 96) / 0.92F);
            HoloMap.render(g, w - size / 2F - 12, 44 + size * 0.46F, size, player.getViewYRot(partial), primary, partial, true);
            return;
        }
        if (!HoloMap.popupVisible()) return;
        float[] c = centre(FcsClientConfig.Panel.HOLOMAP, w, h);
        float size = 112 * FcsClientConfig.holomapScale();
        // keep the whole box on screen at any size
        c[0] = Mth.clamp(c[0], size / 2 + 4, w - size / 2 - 4);
        c[1] = Mth.clamp(c[1], size * 0.46F + 4, h - size * 0.46F - 4);
        HoloMap.render(g, c[0], c[1], size, player.getViewYRot(partial), primary, partial, false);
    }
}
