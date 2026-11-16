// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.client.screen;

import com.miophas.singularity_iteration.common.client.suit.AsciiHud;
import com.miophas.singularity_iteration.common.client.suit.FcsDraw;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

/**
 * 0.1.11.16: drawing of the plant mimic screens of the steam chain (a control room's mimic panel): pipes coloured by
 * temperature with the flow moving along them, equipment boxes, a turbine casing, a generator, a condenser, a vent stack,
 * tank bars and the shared reading lines. Text is in the HUD font at whole pixels.
 */
final class PlantMimic {
    private PlantMimic() {}

    static final int OUTLINE = 0xFF33393A, BODY = 0xFFE3E6E4, WATER = 0xFF4F7FB0, DISTILLED = 0xFF7FB3D6, STEAM = 0xFFB9C3C8,
        SUPER = 0xFFD9792B, HEAT = 0xFFC0392B, SHAFT = 0xFF5B6867;

    /** Pipe colour of a temperature (blue cold, through grey, to orange-red hot). */
    static int colour(double celsius) {
        double f = Math.max(0, Math.min(1, (celsius - 20) / 460));
        int r = (int) (70 + 175 * f), gr = (int) (120 - 40 * f), b = (int) (180 - 150 * f);
        return 0xFF000000 | r << 16 | gr << 8 | b;
    }

    static double time(float partialTick) {
        var mc = Minecraft.getInstance();
        return (mc.level != null ? mc.level.getGameTime() : 0) + partialTick;
    }

    /** A pipe along the points (x, y, x, y, ...); with {@code flow} the dashes move along it at that speed. */
    static void pipe(GuiGraphics g, float[] pts, int argb, float width, double flow, double time) {
        for (int i = 0; i + 3 < pts.length; i += 2) FcsDraw.line(g, pts[i], pts[i + 1], pts[i + 2], pts[i + 3], width, argb);
        if (flow <= 0) return;
        double offset = (time * Math.min(1.5, 0.2 + flow)) % 8;
        for (int i = 0; i + 3 < pts.length; i += 2) {
            float x1 = pts[i], y1 = pts[i + 1], x2 = pts[i + 2], y2 = pts[i + 3];
            double len = Math.hypot(x2 - x1, y2 - y1);
            if (len < 1) continue;
            for (double d = offset; d < len; d += 8) {
                double e = Math.min(len, d + 2.5);
                FcsDraw.line(g, (float) (x1 + (x2 - x1) * d / len), (float) (y1 + (y2 - y1) * d / len),
                    (float) (x1 + (x2 - x1) * e / len), (float) (y1 + (y2 - y1) * e / len), Math.max(0.4F, width * 0.35F), 0xD0FFFFFF);
            }
        }
    }

    static void box(GuiGraphics g, float x, float y, float w, float h, int fill) {
        g.fill((int) x - 1, (int) y - 1, (int) (x + w) + 1, (int) (y + h) + 1, OUTLINE);
        g.fill((int) x, (int) y, (int) (x + w), (int) (y + h), fill);
    }

    /** A turbine casing: narrow at the inlet (left), wide at the exhaust (right). */
    static void turbine(GuiGraphics g, float x, float y, float w, float h, double spin) {
        float mid = y + h / 2;
        FcsDraw.quad(g, x, mid - h * 0.22F, x, mid + h * 0.22F, x + w, y + h, x + w, y, 0xFFC9CED0);
        FcsDraw.line(g, x, mid - h * 0.22F, x + w, y, 0.6F, OUTLINE);
        FcsDraw.line(g, x, mid + h * 0.22F, x + w, y + h, 0.6F, OUTLINE);
        FcsDraw.line(g, x, mid - h * 0.22F, x, mid + h * 0.22F, 0.6F, OUTLINE);
        FcsDraw.line(g, x + w, y, x + w, y + h, 0.6F, OUTLINE);
        // blade rows, shimmering while it turns
        for (int k = 1; k <= 4; k++) {
            float bx = x + w * k / 5F, half = h * (0.22F + 0.28F * k / 5F) - 1;
            int c = spin > 0 && ((int) (spin * 3 + k)) % 2 == 0 ? 0xFF7A8488 : 0xFF9AA3A6;
            FcsDraw.line(g, bx, mid - half, bx, mid + half, 0.8F, c);
        }
    }

    /** A generator (or a shaft coupling with {@code label}) as a circle with its letter. */
    static void machine(GuiGraphics g, float cx, float cy, float r, String label, int fill, float u) {
        FcsDraw.disc(g, cx, cy, r, fill);
        FcsDraw.circle(g, cx, cy, r, 0.6F, OUTLINE);
        Component c = Component.literal(label);
        PumpCrossSection.text(g, c, cx - AsciiHud.width(c, u) / 2, cy - 4 * u, 0x1C2524, u);
    }

    /** A vent stack with steam puffs rising when it vents. */
    static void vent(GuiGraphics g, float x, float y, float h, boolean venting, double time) {
        g.fill((int) x, (int) y, (int) x + 4, (int) (y + h), 0xFF8A8F94);
        if (!venting) return;
        for (int k = 0; k < 3; k++) {
            double t = (time / 10 + k / 3.0) % 1;
            FcsDraw.disc(g, x + 2 + (float) Math.sin(t * 6 + k) * 2, y - (float) (t * 10), 1.2F + (float) t * 2, 0x80C8CDD0);
        }
    }

    /** A vertical tank bar filled to {@code share}. */
    static void tank(GuiGraphics g, float x, float y, float w, float h, double share, int argb) {
        g.fill((int) x - 1, (int) y - 1, (int) (x + w) + 1, (int) (y + h) + 1, OUTLINE);
        g.fill((int) x, (int) y, (int) (x + w), (int) (y + h), 0xFFF4F6F4);
        float f = (float) Math.max(0, Math.min(1, share));
        WindRoseView.fill(g, x, y + h * (1 - f), w, h * f, argb);
    }

    static void label(GuiGraphics g, Component c, float cx, float y, int rgb) {
        float u = WindRoseView.u();
        PumpCrossSection.text(g, c, cx - AsciiHud.width(c, u) / 2, y, rgb, u);
    }

    static void shaft(GuiGraphics g, float x1, float y1, float x2, float y2) {
        FcsDraw.line(g, x1, y1, x2, y2, 1.6F, SHAFT);
    }
}
