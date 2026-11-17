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
        int x = 10, rowH = 11, pw = 156;   // 0.1.11.41: one more column (flight time)
        int y = (int) (h * 0.38F);
        int ph = 14 + units.size() * rowH;
        FcsDraw.panel(g, x, y, pw, ph, Component.translatable("gui.mio_icif.command.squad").getString(), 0x5CEBFF);
        boolean dl = SquadClient.datalink();
        FcsDraw.textRight(g, tr(dl ? "gui.mio_icif.command.hud.datalink_on" : "gui.mio_icif.command.hud.datalink_off"), x + pw - 4, y + 2, FcsDraw.argb(dl ? 0x6FF3D6 : 0x7A8A94, dl ? 0.8F + 0.2F * Mth.sin(t * 0.2F) : 0.7F), 0.45F);
        int cd = SquadClient.recallCooldown();
        if (cd > 0) FcsDraw.textRight(g, tr("gui.mio_icif.command.hud.recall_cd", (cd + 19) / 20), x + pw - 46, y + 2, FcsDraw.argb(0x9B7CFF, 0.8F), 0.45F);
        int ry = y + 13;
        Unit selected = SquadClient.selectedUnit();
        for (Unit u : units) {
            boolean sel = selected != null && selected.id().equals(u.id());
            int col = u.mode() >= 0 ? CommandWheelScreen.modeColor(u.mode()) : 0x8A96A0;
            if (sel) g.fill(x + 1, ry - 1, x + pw - 1, ry + rowH - 2, FcsDraw.argb(col, 0.18F));
            FcsDraw.text(g, SquadClient.label(u), x + 4, ry + 1, FcsDraw.argb(col, 1F), 0.55F);
            if (u.where() != 0) {
                FcsDraw.text(g, fit(SquadClient.whereText(u), 104, 0.5F), x + 22, ry + 1, FcsDraw.argb(0xFF9F4D, 0.9F), 0.5F);
                ry += rowH;
                continue;
            }
            String mode = u.mode() >= 0 && u.mode() < WingmanMode.VALUES.length
                ? tr("gui.mio_icif.wingman.mode_short." + WingmanMode.byId(u.mode()).name().toLowerCase(Locale.ROOT)) : "---";
            FcsDraw.text(g, fit(mode, 18, 0.5F) + (u.individual() ? "*" : ""), x + 22, ry + 1, FcsDraw.argb(col, 0.75F), 0.5F);
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
            FcsDraw.text(g, fit(order, 46, 0.5F), x + 44, ry + 1, FcsDraw.argb(orderCol, 0.9F), 0.5F);
            // health bar and distance to the owner
            int bx = x + 92, bw = 18;
            g.fill(bx, ry + 3, bx + bw, ry + 6, 0x60000000);
            g.fill(bx, ry + 3, bx + (int) (bw * Mth.clamp(u.health(), 0, 1)), ry + 6, FcsDraw.argb(u.health() < 0.35F ? 0xFF5A4C : 0x5CFF9A, 0.9F));
            FcsDraw.textRight(g, String.format(Locale.ROOT, "%.0f", at.distanceTo(player.position())), x + 128, ry + 1,
                FcsDraw.argb(0xDFFBFF, 0.7F), 0.45F);
            // 0.1.11.41 (decision 1 C): flight time left from the suit charge and the upkeep reserve
            String left = SquadClient.enduranceText(u.endurance());
            if (!left.isEmpty()) FcsDraw.textRight(g, fit(left, 24, 0.45F), x + pw - 4, ry + 1,
                FcsDraw.argb(SquadClient.enduranceColor(u.endurance()), 0.85F), 0.45F);
            ry += rowH;
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
