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
        boolean flight = ViltrumFlightClient.isFlying();
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
            if (FcsState.active(SuitModuleType.ORE_SCANNER)) oreList(g, w, h, primary);
            if (FcsState.active(SuitModuleType.BALLISTIC)) ballistic(g, w, h, primary);
            if (FcsState.active(SuitModuleType.BLAST_WARNING)) blast(g, w, h, primary, partial);
            if (FcsState.active(SuitModuleType.GRID_TELEMETRY)) telemetry(g, w, h, primary);
            if (FcsState.active(SuitModuleType.THREAT_SENSOR)) threats(g, mc, w, h, partial);
            if (FcsState.active(SuitModuleType.HOLOMAP)) holomap(g, w, h, primary, player, partial);
            status(g, player, w, h, primary);
        }
        if (flight) ViltrumFlightClient.renderHud(g, w, h, primary, partial);
        g.flush();
    }

    static float[] centre(FcsClientConfig.Panel panel, int w, int h) {
        return new float[]{(float) (panel.x() * w), (float) (panel.y() * h)};
    }

    // ------------------------------------------------------------------ sight frame

    private static void frame(GuiGraphics g, Minecraft mc, LocalPlayer player, int w, int h, int primary, float partial) {
        int inset = 8;
        FcsDraw.brackets(g, inset, inset, w - inset * 2, h - inset * 2, 22, 1.2F, FcsDraw.argb(primary, 0.55F));
        // heading tape
        float yaw = Mth.wrapDegrees(player.getViewYRot(partial) + 180F);   // 0 = north
        float cx = w / 2F, top = 12;
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
        // laser range finder next to the aim point
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

    // ------------------------------------------------------------------ ore list

    private static void oreList(GuiGraphics g, int w, int h, int primary) {
        List<OreScanner.Hit> near = OreScanner.nearestPerKind(5);
        float[] c = centre(FcsClientConfig.Panel.STATUS, w, h);
        int x = (int) (c[0] - 52), y = (int) (c[1] + 14 + FcsState.activeSet().size() * 8 + 6);
        int ph = 12 + Math.max(1, near.size()) * 8;
        FcsDraw.panel(g, x, y, 104, ph, "SEISMIC  " + OreScanner.hits().size() + " CELLS", 0x3FD4FF);
        if (near.isEmpty()) {
            FcsDraw.text(g, "NO ORE IN R" + OreScanner.RADIUS, x + 4, y + 11, FcsDraw.argb(FcsDraw.WHITE, 0.6F), 0.55F);
            return;
        }
        Vec3 eye = Minecraft.getInstance().player.position();
        for (int i = 0; i < near.size(); i++) {
            OreScanner.Hit hit = near.get(i);
            int ry = y + 11 + i * 8;
            g.fill(x + 4, ry + 1, x + 7, ry + 4, FcsDraw.argb(hit.color(), 1F));
            FcsDraw.text(g, hit.label(), x + 10, ry, FcsDraw.argb(FcsDraw.WHITE, 0.9F), 0.55F);
            Vec3 d = Vec3.atCenterOf(hit.pos()).subtract(eye);
            FcsDraw.textRight(g, String.format(Locale.ROOT, "%.0fm %+d", d.length(), (int) Math.round(d.y)), x + 100, ry,
                FcsDraw.argb(0x3FD4FF, 0.9F), 0.55F);
        }
    }

    // ------------------------------------------------------------------ ballistic

    private static void ballistic(GuiGraphics g, int w, int h, int primary) {
        BallisticSolver.Solution s = FcsState.ballistic;
        if (s == null) return;
        int c = s.profile().ready() ? FcsDraw.RED : 0xFF9F1C;
        if (s.impact() != null) {
            float[] p = FcsProjection.project(s.impact());
            if (p != null) {
                FcsDraw.circle(g, p[0], p[1], 5, 1F, FcsDraw.argb(c, 0.95F));
                FcsDraw.line(g, p[0] - 9, p[1], p[0] - 5, p[1], 1F, FcsDraw.argb(c, 0.95F));
                FcsDraw.line(g, p[0] + 5, p[1], p[0] + 9, p[1], 1F, FcsDraw.argb(c, 0.95F));
                FcsDraw.line(g, p[0], p[1] + 5, p[0], p[1] + 9, 1F, FcsDraw.argb(c, 0.95F));
            }
        }
        if (FcsState.leadPoint != null) {
            float[] p = FcsProjection.project(FcsState.leadPoint);
            if (p != null) {
                FcsDraw.quad(g, p[0], p[1] - 4, p[0] + 4, p[1], p[0], p[1] + 4, p[0] - 4, p[1], FcsDraw.argb(FcsDraw.YELLOW, 0.35F));
                FcsDraw.text(g, "LEAD", p[0] + 6, p[1] - 3, FcsDraw.argb(FcsDraw.YELLOW, 0.95F), 0.5F);
            }
        }
        float[] pc = centre(FcsClientConfig.Panel.BALLISTIC, w, h);
        int pw = 92, ph = 44;
        int x = (int) (pc[0] - pw / 2F), y = (int) (pc[1] - ph / 2F);
        FcsDraw.panel(g, x, y, pw, ph, "BALLISTIC  " + s.profile().name(), c);
        double tof = s.ticks() / 20.0;
        double range = s.impact() != null ? s.impact().distanceTo(s.path().get(0)) : -1;
        FcsDraw.text(g, String.format(Locale.ROOT, "TOF %.2fs", tof), x + 4, y + 11, FcsDraw.argb(FcsDraw.WHITE, 0.9F), 0.55F);
        FcsDraw.text(g, range < 0 ? "RNG  OUT" : String.format(Locale.ROOT, "RNG %.1fm", range), x + 4, y + 19, FcsDraw.argb(FcsDraw.WHITE, 0.9F), 0.55F);
        FcsDraw.text(g, String.format(Locale.ROOT, "DROP %.1fm", s.drop()), x + 4, y + 27, FcsDraw.argb(FcsDraw.WHITE, 0.9F), 0.55F);
        String state = s.target() != null ? "ON TGT" : s.profile().ready() ? "READY" : "CHARGE";
        FcsDraw.textRight(g, state, x + pw - 4, y + 11, FcsDraw.argb(c, 1F), 0.55F);
        if (FcsState.leadTarget != null) {
            FcsDraw.text(g, "TGT " + tagName(FcsState.leadTarget), x + 4, y + 35, FcsDraw.argb(FcsDraw.YELLOW, 0.95F), 0.55F);
        }
    }

    // ------------------------------------------------------------------ blast warning

    private static void blast(GuiGraphics g, int w, int h, int primary, float partial) {
        List<SensorTracker.Blast> blasts = SensorTracker.blasts();
        if (blasts.isEmpty()) return;
        LocalPlayer player = Minecraft.getInstance().player;
        boolean inside = false;
        for (SensorTracker.Blast b : blasts) {
            double d = Math.sqrt(b.entity().distanceToSqr(player));
            if (d < b.radius() * 2) inside = true;
            Vec3 at = b.entity().getPosition(partial);
            float[] p = FcsProjection.project(at.x, at.y + b.entity().getBbHeight() + 0.6, at.z);
            if (p != null) {
                int c = b.seconds() < 1 ? FcsDraw.RED : FcsDraw.YELLOW;
                String t = String.format(Locale.ROOT, "T-%.2f", Math.max(0, b.seconds()));
                float tw = FcsDraw.width(t, 0.75F);
                g.fill((int) (p[0] - tw / 2 - 2), (int) p[1] - 2, (int) (p[0] + tw / 2 + 2), (int) p[1] + 8, 0x90000000);
                FcsDraw.textCentered(g, t, p[0], p[1], FcsDraw.argb(c, 1F), 0.75F);
            }
        }
        float[] c = centre(FcsClientConfig.Panel.BLAST, w, h);
        int rows = Math.min(4, blasts.size());
        int pw = 128, ph = 12 + rows * 9;
        int x = (int) (c[0] - pw / 2F), y = (int) (c[1] - ph / 2F);
        float flash = inside ? (float) (0.55 + 0.45 * Math.sin(System.currentTimeMillis() / 70.0)) : 1F;
        FcsDraw.panel(g, x, y, pw, ph, inside ? "BLAST WARNING  IN RADIUS" : "BLAST WARNING", inside ? FcsDraw.RED : FcsDraw.YELLOW);
        if (inside) g.fill(x + 1, y + 1, x + pw - 1, y + 9, FcsDraw.argb(FcsDraw.RED, 0.35F * flash));
        for (int i = 0; i < rows; i++) {
            SensorTracker.Blast b = blasts.get(i);
            int ry = y + 11 + i * 9;
            double d = Math.sqrt(b.entity().distanceToSqr(player));
            int col = b.seconds() < 1 || d < b.radius() ? FcsDraw.RED : FcsDraw.YELLOW;
            FcsDraw.text(g, b.label(), x + 4, ry, FcsDraw.argb(col, 1F), 0.55F);
            FcsDraw.text(g, String.format(Locale.ROOT, "%.0fm", d), x + 44, ry, FcsDraw.argb(FcsDraw.WHITE, 0.9F), 0.55F);
            FcsDraw.bar(g, x + 64, ry, 34, 5, Math.min(1, b.seconds() / 4F), col);
            FcsDraw.textRight(g, String.format(Locale.ROOT, "%.2fs", Math.max(0, b.seconds())), x + pw - 4, ry, FcsDraw.argb(col, 1F), 0.55F);
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
        HoloMap.render(g, c[0], c[1], size, player.getViewYRot(partial), primary, partial);
    }
}
