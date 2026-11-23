// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.client.screen;

import com.miophas.singularity_iteration.common.blockentity.kuentity.kugenerator.WaterPhysics;
import com.miophas.singularity_iteration.common.client.suit.FcsDraw;
import com.miophas.singularity_iteration.common.menu.generator.WaterKineticGeneratorMenu;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/**
 * 0.1.11.18: the water kinetic generator's rotor cross-section: the disc seen from upstream, one cell per block of the
 * layer in front (moving water, still water, air, solid), the rotor turning at the water's speed; the flow and the share
 * of the disc that is water as gauges; a day's tide (at sea) with the time now. Values as in
 * {@link WaterKineticGeneratorMenu}.
 *
 * <p>0.1.11.181 (176 wide, merged): the rotor slot at the hub of the disc in front, drawn on the panel; the bars and the day of the current beside it (their figures are in the readouts).
 */
@OnlyIn(Dist.CLIENT)
@SuppressWarnings("null")
public class WaterRotorScreen extends MimicScreen<WaterKineticGeneratorMenu> {
    static final String K = "gui.mio_icif.water.";
    static final int MOVING = 0xFF4F7FB0, STILL = 0xFF2F5677, AIR = 0xFFF4F6F4, SOLID = 0xFF8D8F92;

    public WaterRotorScreen(WaterKineticGeneratorMenu menu, Inventory inv, Component title) { super(menu, inv, title); }

    private int v(int i) { return menu.value(i); }

    static String sourceKey(int source) {
        return switch (source) {
            case 1 -> "source.river";
            case 2 -> "source.ocean";
            case 3 -> "source.canal";
            default -> "source.none";
        };
    }

    @Override
    protected void drawMachine(GuiGraphics g, int x, int y, double time) {
        int d = v(8), source = v(9), ku = v(3);
        double speed = v(10) / 1000.0;
        // 0.1.11.181: the disc in front (a cell a block), the rotor slot at its hub, the blades turning over it
        if (d > 0) {
            int s = Math.max(2, 56 / d), gx = DISC_X - d * s / 2, gy = DISC_Y - d * s / 2;
            for (int i = 0; i < d * d; i++) {
                int kind = WaterPhysics.unpack(v(18 + i / 8), i % 8);
                if (kind == WaterPhysics.OUTSIDE) continue;
                int cx = gx + (i % d) * s, cy = gy + (i / d) * s;
                int c = kind == WaterPhysics.MOVING ? MOVING : kind == WaterPhysics.STILL ? STILL : kind == WaterPhysics.AIR ? AIR : SOLID;
                FcsDraw.fill(g, x + cx, y + cy, x + cx + s - 1, y + cy + s - 1, c);
                if (kind == WaterPhysics.MOVING && s >= 4) {
                    int dx = (int) ((time * Math.max(0.2, speed) * 0.5 + i) % (s - 2));
                    FcsDraw.fill(g, x + cx + dx, y + cy + s / 2 - 1, x + cx + dx + 1, y + cy + s / 2, 0xC0FFFFFF);
                }
            }
            float r = d * s / 2F;
            FcsDraw.circle(g, x + DISC_X, y + DISC_Y, r, 1F, MachineArt.IRON_DARK);
            double a = ku > 0 ? time * Math.min(1.5, speed) * 0.12 : 0;
            for (int k = 0; k < 3; k++) {
                double b = a + k * 2 * Math.PI / 3;
                FcsDraw.line(g, x + DISC_X, y + DISC_Y, x + DISC_X + (float) Math.cos(b) * (r - 2), y + DISC_Y + (float) Math.sin(b) * (r - 2), 2F,
                    MachineArt.IRON);
            }
        }
        // the flow and the share of the disc in water, the day of the current (the tide at sea, a flat line elsewhere)
        int bx = x + 76, bw = 90;
        gauge(g, bx, y + 18, bw, Component.translatable(K + "flow"), Math.min(1, speed / 1.6), null, MOVING, 1F);
        double share = v(12) > 0 ? v(11) / (double) v(12) : 0;
        gauge(g, bx, y + 28, bw, Component.translatable(K + "disc"), share, null, share < 0.8 ? 0xFFC0602A : 0xFF2F6E4F, 1F);
        int tx = bx, ty = y + 46, tw = bw, th = 26;
        FcsDraw.fill(g, tx - 1, ty - 1, tx + tw + 1, ty + th + 1, 0xFFB8C0C2);
        FcsDraw.fill(g, tx, ty, tx + tw, ty + th, 0xFFFBFCFC);
        double spring = WaterPhysics.spring(v(14));
        float px = tx, py = ty + th;
        for (int i = 0; i <= 48; i++) {
            double t = i * 500.0;
            double vv = source == 2 ? WaterPhysics.tideSpeed((long) t, v(14)) : source == 0 ? 0 : speed;
            float nx = tx + tw * i / 48F, ny = (float) (ty + th - th * Math.min(1, vv * vv * vv / 3.0));
            if (i > 0) FcsDraw.line(g, px, py, nx, ny, 1F, MOVING);
            px = nx;
            py = ny;
        }
        float now = tx + tw * v(15) / 24000F;
        FcsDraw.fill(g, (int) now, ty, (int) now + 1, ty + th, 0xFFC0392B);
        if (source == 2) {
            Component tide = Component.translatable(spring >= 1.15 ? K + "spring" : spring <= 0.85 ? K + "neap" : K + "mid");
            label(g, tide, tx + tw - PumpCrossSection.width(tide, 1F), y + 37, WindRoseView.DIM);
        }

        line(g, x, y, 0, Component.translatable(K + "line_source", Component.translatable(K + sourceKey(source)), f("%.2f", speed), v(11), v(12),
            Math.round(share * 100) + "%"), WindRoseView.INK);
        line(g, x, y, 1, Component.translatable(K + "line_output", ku, Math.round(v(16) * share), d, Math.round(v(13) / 10.0) + "%"), WindRoseView.DIM);
        String state; int colour = 0xA0602A;
        if (d == 0) state = "state.no_rotor";
        else if (v(5) == 0) state = "state.not_in_water";
        else if (source == 0) state = "state.still";
        else if (source == 2 && v(17) < 150) state = "state.slack";
        else if (ku <= 0) state = "state.still";
        else if (share < 0.8) state = "state.blocked";
        else if (v(13) < 950) state = "state.wake";
        else { state = "state.running"; colour = 0x2F6E4F; }
        ReadoutPanel.headline(Component.literal(ku + " KU/t"), Component.translatable(K + state), colour);   // 0.1.11.145: the output leads, the state beside it
    }

    /** 0.1.11.181: the disc's centre (panel coordinates), the rotor slot on it (WaterKineticGeneratorMenu.ROTOR_X/Y). */
    static final int DISC_X = 37, DISC_Y = 45;

    /** A labelled bar: the label, then the bar (sunk, filled to {@code share}); {@code right} after it when given. */
    static void gauge(GuiGraphics g, int x, int y, int w, Component label, double share, String right, int argb, float u) {
        PumpCrossSection.text(g, label, x, y, VanillaFrame.TITLE, u);
        int bx = x + 30;
        int bw = w - 30;
        MachineArt.sunk(g, bx, y, bw, 7, MachineArt.GLASS);
        int fill = (int) Math.round((bw - 2) * Math.max(0, Math.min(1, share)));
        if (fill > 0) FcsDraw.fill(g, bx + 1, y + 1, bx + 1 + fill, y + 6, argb);
        if (right != null) PumpCrossSection.text(g, Component.literal(right), bx + bw + 3, y, 0x1C2524, u);
    }

    @Override
    protected void drawStrip(GuiGraphics g, int x, int y) {
        int life = v(7) > 0 ? (int) Math.round(100.0 * v(6) / v(7)) : 0;
        bar(g, x, y, Component.translatable(K + "rotor"), life / 100.0, Component.literal(v(7) > 0 ? life + "%" : "-"));
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        int rx = mouseX - leftPos, ry = mouseY - topPos;
        // EXPLAIN_TIP_HIDDEN (0.1.11.182): the drawing's how-it-works text, hidden for now
        // if (overMachine(rx, ry))
        //     g.renderComponentTooltip(this.font, List.of(Component.translatable(K + "tip1"), Component.translatable(K + "tip2"),
        //         Component.translatable(K + "tip3"), Component.translatable(K + "tip4")), mouseX, mouseY);
    }
}
