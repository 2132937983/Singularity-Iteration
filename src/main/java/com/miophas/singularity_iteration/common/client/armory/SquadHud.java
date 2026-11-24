// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.client.armory;

import com.miophas.singularity_iteration.common.armory.WingmanCommandPackets.Unit;
import com.miophas.singularity_iteration.common.armory.WingmanEntity;
import com.miophas.singularity_iteration.common.armory.WingmanMode;
import com.miophas.singularity_iteration.common.armory.WingmanOrder;
import com.miophas.singularity_iteration.common.client.suit.FcsClientConfig;
import com.miophas.singularity_iteration.common.client.suit.FcsDraw;
import com.miophas.singularity_iteration.common.client.suit.FcsProjection;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/**
 * Squad HUD of the quantum command (0.1.10.2), while the command link works and the owner has
 * Wingmen (client option {@code squad_hud}).
 *
 * <ul>
 *   <li><b>Squad panel</b> (left): one row per Wingman — name in its mode colour, mode, order with the
 *       distance to the order point, a health bar, the distance to the owner; "UNLOADED" or "OTHER
 *       DIM" when it cannot take orders. The header shows the data link and the quantum recall
 *       cooldown. The selected Wingman (command wheel, holo table) is marked.</li>
 *   <li><b>Edge markers</b>: a Wingman out of view gets a chevron at the screen edge pointing to it,
 *       with its name and distance; one in view gets a small bracket.</li>
 * </ul>
 */
public final class SquadHud {
    private SquadHud() {}

    /** 0.1.11.40 (U-4): every text of the HUD comes from the language files (before: English abbreviations). */
    private static String tr(String key, Object... args) {
        return Component.translatable(key, args).getString();
    }

    /** Cuts {@code text} to {@code width} GUI pixels at {@code scale} (a long translation must not run into the next column). */
    private static String fit(String text, float width, float scale) {
        if (FcsDraw.width(text, scale) <= width) return text;
        String t = text;
        while (t.length() > 1 && FcsDraw.width(t + ".", scale) > width) t = t.substring(0, t.length() - 1);
        return t + ".";
    }

    public static void render(GuiGraphics g, DeltaTracker delta) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || mc.options.hideGui || !FcsClientConfig.squadHud() || !FcsClientConfig.hudEnabled()) return;
        List<Unit> units = SquadClient.units();
        if (!SquadClient.commander() || units.isEmpty() || mc.screen instanceof CommandWheelScreen) return;
        float partial = delta.getGameTimeDeltaPartialTick(false);
        float t = SquadClient.ticks + partial;
        int w = g.guiWidth(), h = g.guiHeight();
        panel(g, player, units, h, t, partial);
        edgeMarkers(g, player, units, w, h, t, partial);
    }

    private static void panel(GuiGraphics g, LocalPlayer player, List<Unit> units, int h, float t, float partial) {
        // Visor-aligned compact tactical cards in upper-left (clear of bottom curved Quantum/Nano Visor HUD)
        int x = 10, y = 26, pw = 128, cardH = 15, gap = 2;
        // Header strip
        g.fill(x, y, x + pw, y + 11, 0x8006101A);
        g.fill(x, y, x + pw, y + 1, FcsDraw.argb(0x5CEBFF, 0.75F));
        FcsDraw.text(g, tr("gui.mio_icif.command.squad"), x + 4, y + 3, FcsDraw.argb(0xDFFBFF, 0.92F), 0.50F);
        int cd = SquadClient.recallCooldown();
        if (cd > 0) {
            FcsDraw.textRight(g, tr("gui.mio_icif.command.hud.recall_cd", (cd + 19) / 20), x + pw - 4, y + 3,
                FcsDraw.argb(0x9B7CFF, 0.9F), 0.45F);
        } else if (SquadClient.datalink()) {
            float pulse = 0.65F + 0.35F * Mth.sin(t * 0.18F);
            g.fill(x + pw - 7, y + 4, x + pw - 4, y + 7, FcsDraw.argb(0x6FF3D6, pulse));
        }
        int ry = y + 13;
        Unit selected = SquadClient.selectedUnit();
        for (Unit u : units) {
            boolean sel = selected != null && selected.id().equals(u.id());
            int col = u.mode() >= 0 ? CommandWheelScreen.modeColor(u.mode()) : 0x8A96A0;
            // Card glass background + stance color left accent bar
            g.fill(x, ry, x + pw, ry + cardH, sel ? 0x980A1D2B : 0x70050E17);
            g.fill(x, ry, x + 2, ry + cardH, FcsDraw.argb(col, sel ? 1.0F : 0.85F));
            if (sel) {
                g.fill(x + 2, ry, x + pw, ry + 1, FcsDraw.argb(0x5CEBFF, 0.65F));
                g.fill(x + 2, ry + cardH - 1, x + pw, ry + cardH, FcsDraw.argb(0x5CEBFF, 0.65F));
            }
            // Top line: callsign + order badge + distance
            FcsDraw.text(g, SquadClient.label(u), x + 5, ry + 2, FcsDraw.argb(col, 1F), 0.55F);
            if (u.where() != 0) {
                FcsDraw.text(g, fit(SquadClient.whereText(u), 92, 0.48F), x + 24, ry + 4, FcsDraw.argb(0xFF9F4D, 0.92F), 0.48F);
                ry += cardH + gap;
                continue;
            }
            WingmanOrder o = SquadClient.order(u);
            Vec3 at = SquadClient.position(u, partial);
            String order = switch (o) {
                case NONE -> tr(u.state() == WingmanEntity.RETURNING ? "gui.mio_icif.wingman.order.rtb" : o.key());
                case FOCUS -> tr(o.key());
                default -> {
                    Vec3 p = SquadClient.orderPoint(u);
                    double d = p == null ? 0 : at.distanceTo(p);
                    yield d > 6 ? String.format(Locale.ROOT, "%s %.0fm", tr(o.key()), d) : tr(o.key());
                }
            };
            float[] oc = CommandFx.orderColor(o);
            int orderCol = o == WingmanOrder.NONE ? 0x9AA8B0 : ((int) (oc[0] * 255) << 16 | (int) (oc[1] * 255) << 8 | (int) (oc[2] * 255));
            FcsDraw.text(g, fit(order, 62, 0.48F), x + 24, ry + 2, FcsDraw.argb(orderCol, 0.92F), 0.48F);
            FcsDraw.textRight(g, String.format(Locale.ROOT, "%.0fm", at.distanceTo(player.position())), x + pw - 4, ry + 2,
                FcsDraw.argb(0xDFFBFF, 0.72F), 0.45F);
            // Bottom line: thin HP bar + endurance readout
            int bx = x + 5, bw = 82;
            g.fill(bx, ry + 10, bx + bw, ry + 12, 0x60000000);
            g.fill(bx, ry + 10, bx + (int) (bw * Mth.clamp(u.health(), 0, 1)), ry + 12,
                FcsDraw.argb(u.health() < 0.35F ? 0xFF5A4C : 0x5CFF9A, 0.92F));
            String left = SquadClient.enduranceText(u.endurance());
            if (!left.isEmpty()) FcsDraw.textRight(g, fit(left, 32, 0.44F), x + pw - 4, ry + 9,
                FcsDraw.argb(SquadClient.enduranceColor(u.endurance()), 0.88F), 0.44F);
            ry += cardH + gap;
        }
    }

    private static void edgeMarkers(GuiGraphics g, LocalPlayer player, List<Unit> units, int w, int h, float t, float partial) {
        float margin = 18;
        for (Unit u : units) {
            if (u.where() != 0) continue;
            Vec3 at = SquadClient.position(u, partial).add(0, 1.0, 0);
            double dist = at.distanceTo(player.getEyePosition(partial));
            if (dist < 3) continue;
            int col = u.mode() >= 0 && u.mode() < WingmanMode.VALUES.length ? CommandWheelScreen.modeColor(u.mode()) : 0x8A96A0;
            float[] p = FcsProjection.project(at);
            String label = String.format(Locale.ROOT, "%s %.0fm", SquadClient.label(u), dist);
            if (p != null && p[0] > margin && p[0] < w - margin && p[1] > margin && p[1] < h - margin) {
                float s = 5;
                FcsDraw.brackets(g, p[0] - s, p[1] - s, s * 2, s * 2, 3, 1F, FcsDraw.argb(col, 0.8F));
                FcsDraw.text(g, label, p[0] + s + 2, p[1] - 3, FcsDraw.argb(col, 0.8F), 0.45F);
                continue;
            }
            // off screen: a chevron on the edge ellipse, pointing to the Wingman
            float b = FcsProjection.bearing(at.x, at.y, at.z);
            float rx = w / 2F - margin, ry = h / 2F - margin;
            float ex = w / 2F + (float) Math.sin(b) * rx, ey = h / 2F - (float) Math.cos(b) * ry;
            float sx = (float) Math.sin(b), sy = -(float) Math.cos(b);
            float px = -sy, py = sx;
            float pulse = 0.65F + 0.35F * Mth.sin(t * 0.25F + u.entityId());
            FcsDraw.triangle(g, ex + sx * 6, ey + sy * 6, ex - sx * 2 + px * 5, ey - sy * 2 + py * 5, ex - sx * 2 - px * 5, ey - sy * 2 - py * 5,
                FcsDraw.argb(col, 0.85F * pulse));
            FcsDraw.textCentered(g, label, ex - sx * 12, ey - sy * 12 - 3, FcsDraw.argb(col, 0.8F), 0.45F);
        }
    }
}
