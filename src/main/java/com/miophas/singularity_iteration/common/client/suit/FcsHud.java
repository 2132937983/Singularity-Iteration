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
        if (hud) {
            if (FcsState.active(SuitModuleType.ORE_SCANNER)) {
                float pulse = 0.8F + 0.2F * (float) Math.sin(System.currentTimeMillis() / 320.0);
                OreScanner.renderHud(g, FcsProjection.camera(), pulse);
            }
            frame(g, mc, player, w, h, primary, partial);
            if (FcsState.active(SuitModuleType.ENTITY_ESP) || FcsState.active(SuitModuleType.BEHAVIOR_PREDICTOR)) tags(g, primary);
            if (FcsState.active(SuitModuleType.BALLISTIC)) ballistic(g, w, h, primary, partial);
            if (FcsState.active(SuitModuleType.BLAST_WARNING)) blast(g, w, h, primary, partial);
            // in flight the aircraft HUD owns the middle band: the status and telemetry panels step aside
            if (FcsState.active(SuitModuleType.GRID_TELEMETRY) && !flight) telemetry(g, w, h, primary);
            if (FcsState.active(SuitModuleType.THREAT_SENSOR)) threats(g, mc, w, h, partial);
            if (FcsState.active(SuitModuleType.HOLOMAP)) holomap(g, w, h, primary, player, partial);
            if (!flight) status(g, player, w, h, primary);
        }
        if (flight) ManeuverModeClient.renderHud(g, w, h, primary, partial);
        g.flush();
    }

    static float[] centre(FcsClientConfig.Panel panel, int w, int h) {
        return new float[]{(float) (panel.x() * w), (float) (panel.y() * h)};
    }

    // ------------------------------------------------------------------ sight frame

    private static void frame(GuiGraphics g, Minecraft mc, LocalPlayer player, int w, int h, int primary, float partial) {
        int inset = 8;
        FcsDraw.brackets(g, inset, inset, w - inset * 2, h - inset * 2, 22, 1.2F, FcsDraw.argb(primary, 0.55F));
        float cx = w / 2F, top = 12;
        if (FcsClientConfig.showCompass()) heading(g, player, cx, top, w, primary, partial);
        // laser range finder next to the aim point
        rangeFinder(g, player, cx, h, primary, partial);
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
        FcsDraw.panel(g, x, y, pw, ph, "FCS  " + (SuitModules.hasVisor(player) ? "VISOR OK" : "NO VISOR"), primary);
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
            FcsDraw.textRight(g, type.drainPerTick() > 0 ? type.drainPerTick() + "" : "HIT", x + pw - 3, ry, FcsDraw.argb(primary, 0.8F), 0.55F);
            row++;
        }
    }

    // ------------------------------------------------------------------ ESP / predictor tags

    private static void tags(GuiGraphics g, int primary) {
        boolean esp = FcsState.active(SuitModuleType.ENTITY_ESP);
        boolean predict = FcsState.active(SuitModuleType.BEHAVIOR_PREDICTOR);
        LocalPlayer player = Minecraft.getInstance().player;
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
                shown++;
            }
            if (predict && t.intent() != null) {
                int ic = t.intentHot() ? FcsDraw.RED : 0xFF9F1C;
                float blink = t.intentHot() ? (float) (0.6 + 0.4 * Math.sin(System.currentTimeMillis() / 90.0)) : 1F;
                float ix = top[0] + wid / 2 + 3, iy = top[1];
                FcsDraw.triangle(g, ix, iy, ix + 5, iy + 3, ix, iy + 6, FcsDraw.argb(ic, blink));
                FcsDraw.text(g, t.intent(), ix + 6, iy, FcsDraw.argb(ic, blink), 0.55F);
            }
        }
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
                double range = s.impact().distanceTo(path.get(0));
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
        // slim data panel
        float[] pc = centre(FcsClientConfig.Panel.BALLISTIC, w, h);
        int pw = 92, ph = 40;
        int x = (int) (pc[0] - pw / 2F), y = (int) (pc[1] - ph / 2F);
        FcsDraw.panel(g, x, y, pw, ph, "BALLISTIC  " + s.profile().name(), c);
        double range = s.impact() != null ? s.impact().distanceTo(path.get(0)) : -1;
        row(g, x, y + 13, pw, "TOF", String.format(Locale.ROOT, "%.2fs", s.ticks() / 20.0));
        row(g, x, y + 21, pw, "RNG", range < 0 ? "OUT" : String.format(Locale.ROOT, "%.1fm", range));
        row(g, x, y + 29, pw, "DROP", String.format(Locale.ROOT, "%.1fm", s.drop()));
        String state = onTarget ? "ON TGT" : s.profile().ready() ? "READY" : "CHARGE";
        FcsDraw.textRight(g, state, x + pw - 4, y + 2, FcsDraw.argb(c, 1F), 0.5F);
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
        for (SensorTracker.Blast b : blasts) {
            double d = Math.sqrt(b.entity().distanceToSqr(player));
            boolean in = d < b.radius();
            inside |= in;
            float burnt = b.burnt();
            int col = b.seconds() < 1 || in ? FcsDraw.RED : FcsDraw.YELLOW;
            float flash = b.seconds() < 1 ? (float) (0.6 + 0.4 * Math.sin(now / 55.0)) : 1F;
            Vec3 at = b.entity().getPosition(partial);
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
        // compact list
        float[] c = centre(FcsClientConfig.Panel.BLAST, w, h);
        int rows = Math.min(4, blasts.size());
        int pw = 128, ph = 13 + rows * 9;
        int x = (int) (c[0] - pw / 2F), y = (int) (c[1] - ph / 2F);
        FcsDraw.panel(g, x, y, pw, ph, inside ? "BLAST  IN RADIUS" : "BLAST WARNING", inside ? FcsDraw.RED : FcsDraw.YELLOW);
        for (int i = 0; i < rows; i++) {
            SensorTracker.Blast b = blasts.get(i);
            int ry = y + 12 + i * 9;
            double d = Math.sqrt(b.entity().distanceToSqr(player));
            int col = b.seconds() < 1 || d < b.radius() ? FcsDraw.RED : FcsDraw.YELLOW;
            FcsDraw.text(g, b.label(), x + 5, ry, FcsDraw.argb(col, 1F), 0.55F);
            FcsDraw.text(g, String.format(Locale.ROOT, "%.0fm", d), x + 46, ry, FcsDraw.argb(FcsDraw.WHITE, 0.9F), 0.55F);
            // segmented fuse bar
            int segs = 10, filled = Math.round((1 - b.burnt()) * segs);
            for (int k = 0; k < segs; k++) {
                g.fill(x + 64 + k * 4, ry + 1, x + 67 + k * 4, ry + 5, k < filled ? FcsDraw.argb(col, 0.95F) : 0x40FFFFFF);
            }
            FcsDraw.textRight(g, String.format(Locale.ROOT, "%.1f", Math.max(0, b.seconds())), x + pw - 4, ry, FcsDraw.argb(col, 1F), 0.55F);
        }
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
        FcsDraw.chip(g, String.format(Locale.ROOT, "%.1f", Math.max(0, b.seconds())), x + r + 2, y - 4, col, 0.5F);
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

    private static void telemetry(GuiGraphics g, int w, int h, int primary) {
        float[] c = centre(FcsClientConfig.Panel.TELEMETRY, w, h);
        int pw = 112, ph = 74;
        int x = (int) (c[0] - pw / 2F), y = (int) (c[1] - ph / 2F);
        int color = FcsDraw.YELLOW;
        FcsDraw.panel(g, x, y, pw, ph, "GRID TELEMETRY", color);
        MeterHudPacket.Reply reply = MeterHudPacket.latest();
        var pos = FcsClient.telemetryPos();
        int ty = y + 11;
        if (pos != null && reply != null && reply.pos().equals(pos)) {
            String tier = NetworkWalker.tierName(reply.voltage());
            FcsDraw.text(g, "LOS " + tier + "  " + FcsDraw.si(reply.voltage()) + " EU", x + 4, ty, FcsDraw.argb(FcsDraw.WHITE, 0.95F), 0.55F);
            FcsDraw.text(g, "FLOW " + FcsDraw.si(reply.throughput()) + " EU/t", x + 4, ty + 8, FcsDraw.argb(FcsDraw.WHITE, 0.95F), 0.55F);
            String rated = reply.ratedPacket() > 0 ? NetworkWalker.tierName(reply.ratedPacket()) + " " + FcsDraw.si(reply.ratedPacket()) : "-";
            FcsDraw.text(g, "LINE " + rated + "  N" + reply.endpoints(), x + 4, ty + 16, FcsDraw.argb(FcsDraw.WHITE, 0.95F), 0.55F);
            boolean over = reply.ratedPacket() > 0 && reply.voltage() > reply.ratedPacket();
            String state = reply.throughput() <= 0 ? "IDLE" : over ? "OVER" : "LIVE";
            FcsDraw.textRight(g, state, x + pw - 4, ty, FcsDraw.argb(over ? FcsDraw.RED : primary, 1F), 0.55F);
        } else {
            FcsDraw.text(g, "LOS  NO GRID NODE", x + 4, ty, FcsDraw.argb(FcsDraw.WHITE, 0.55F), 0.55F);
        }
        g.fill(x + 3, ty + 25, x + pw - 3, ty + 26, FcsDraw.argb(color, 0.4F));
        SuitPackets.ChunkGrid grid = SuitSensorData.chunkGrid();
        int cy = ty + 28;
        if (grid == null) {
            FcsDraw.text(g, "CHUNK  ---", x + 4, cy, FcsDraw.argb(FcsDraw.WHITE, 0.55F), 0.55F);
            return;
        }
        FcsDraw.text(g, String.format(Locale.ROOT, "CHUNK %d,%d  MAX %s", grid.chunkX(), grid.chunkZ(), NetworkWalker.tierName(grid.maxVoltage())),
            x + 4, cy, FcsDraw.argb(FcsDraw.WHITE, 0.95F), 0.55F);
        FcsDraw.text(g, String.format(Locale.ROOT, "GEN %d  USE %d  ESS %d  CBL %d", grid.generators(), grid.consumers(), grid.storages(), grid.cables()),
            x + 4, cy + 8, FcsDraw.argb(FcsDraw.WHITE, 0.95F), 0.55F);
        FcsDraw.text(g, "+" + FcsDraw.si(grid.generated()) + "  -" + FcsDraw.si(grid.consumed()) + " EU/t", x + 4, cy + 16,
            FcsDraw.argb(FcsDraw.WHITE, 0.95F), 0.55F);
        FcsDraw.bar(g, x + 4, cy + 25, pw - 8, 5, grid.capacity() > 0 ? grid.stored() / (float) grid.capacity() : 0, color);
        if (grid.overvolted() > 0) {
            FcsDraw.textRight(g, "OV " + grid.overvolted(), x + pw - 4, cy, FcsDraw.argb(FcsDraw.RED, 1F), 0.55F);
        }
    }

    // ------------------------------------------------------------------ threats

    private static void threats(GuiGraphics g, Minecraft mc, int w, int h, float partial) {
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
        String banner = (red > 0 ? "LOCK " + red : "") + (red > 0 && yellow > 0 ? "  " : "") + (yellow > 0 ? "WATCH " + yellow : "");
        if (!banner.isEmpty()) {
            float bw = FcsDraw.width(banner, 0.7F) + 8;
            float by = 40;
            g.fill((int) (cx - bw / 2), (int) by, (int) (cx + bw / 2), (int) by + 10, red > 0 ? FcsDraw.argb(FcsDraw.RED, 0.35F * pulse) : 0x50403000);
            FcsDraw.textCentered(g, banner, cx, by + 2, FcsDraw.argb(red > 0 ? FcsDraw.RED : FcsDraw.YELLOW, 1F), 0.7F);
        }
    }

    // ------------------------------------------------------------------ holomap

    private static void holomap(GuiGraphics g, int w, int h, int primary, LocalPlayer player, float partial) {
        float[] c = centre(FcsClientConfig.Panel.HOLOMAP, w, h);
        float size = 112 * FcsClientConfig.holomapScale();
        // keep the whole box on screen at any size
        c[0] = Mth.clamp(c[0], size / 2 + 4, w - size / 2 - 4);
        c[1] = Mth.clamp(c[1], size * 0.46F + 4, h - size * 0.46F - 4);
        HoloMap.render(g, c[0], c[1], size, player.getViewYRot(partial), primary, partial);
    }
}
