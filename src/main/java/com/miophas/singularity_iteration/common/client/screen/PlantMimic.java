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
        FcsDraw.fill(g, (int) x - 1, (int) y - 1, (int) (x + w) + 1, (int) (y + h) + 1, OUTLINE);
        FcsDraw.fill(g, (int) x, (int) y, (int) (x + w), (int) (y + h), fill);
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
        PumpCrossSection.text(g, c, cx - PumpCrossSection.width(c, u) / 2, cy - 4 * u, 0x1C2524, u);
    }

    /** A vent stack with steam puffs rising when it vents. */
    static void vent(GuiGraphics g, float x, float y, float h, boolean venting, double time) {
        FcsDraw.fill(g, (int) x, (int) y, (int) x + 4, (int) (y + h), 0xFF8A8F94);
        if (!venting) return;
        for (int k = 0; k < 3; k++) {
            double t = (time / 10 + k / 3.0) % 1;
            FcsDraw.disc(g, x + 2 + (float) Math.sin(t * 6 + k) * 2, y - (float) (t * 10), 1.2F + (float) t * 2, 0x80C8CDD0);
        }
    }

    /** A vertical tank bar filled to {@code share}. */
    static void tank(GuiGraphics g, float x, float y, float w, float h, double share, int argb) {
        FcsDraw.fill(g, (int) x - 1, (int) y - 1, (int) (x + w) + 1, (int) (y + h) + 1, OUTLINE);
        FcsDraw.fill(g, (int) x, (int) y, (int) (x + w), (int) (y + h), 0xFFF4F6F4);
        float f = (float) Math.max(0, Math.min(1, share));
        WindRoseView.fill(g, x, y + h * (1 - f), w, h * f, argb);
    }

    static void label(GuiGraphics g, Component c, float cx, float y, int rgb) {
        float u = WindRoseView.u();
        PumpCrossSection.text(g, c, cx - PumpCrossSection.width(c, u) / 2, y, rgb, u);
    }

    static void shaft(GuiGraphics g, float x1, float y1, float x2, float y2) {
        FcsDraw.line(g, x1, y1, x2, y2, 1.6F, SHAFT);
    }

    /** 0.1.11.17: shaft angle for a speed: one turn every 40 ticks at 1,500 rpm (slowed to be seen). */
    static double angle(double time, double rpm) {
        return time * Math.max(0, rpm) / 1500.0 * Math.PI / 20;
    }

    /** 0.1.11.17: a flywheel (a disc with four spokes) turned to {@code angle}. */
    static void flywheel(GuiGraphics g, float cx, float cy, float r, double angle) {
        FcsDraw.disc(g, cx, cy, r, 0xFFD5DAD8);
        FcsDraw.circle(g, cx, cy, r, 0.6F, OUTLINE);
        for (int k = 0; k < 4; k++) {
            double a = angle + k * Math.PI / 2;
            FcsDraw.line(g, cx, cy, cx + (float) Math.cos(a) * r, cy + (float) Math.sin(a) * r, 0.6F, OUTLINE);
        }
        FcsDraw.disc(g, cx, cy, 1.4F, OUTLINE);
    }

    /**
     * 0.1.11.17: a Stirling engine standing at ({@code x}, {@code y}), 26 wide and 50 high: the heater head on top coloured
     * by its temperature, the cylinder with the displacer and the power piston a quarter turn apart, the cooler at the
     * bottom (fins in air, blue with water).
     */
    static void stirling(GuiGraphics g, float x, float y, double hot, double cold, boolean water, double angle, float u) {
        box(g, x, y, 26, 11, colour(hot));
        Component t = Component.literal(Math.round(hot) + "°");
        PumpCrossSection.text(g, t, x + 13 - PumpCrossSection.width(t, u) / 2, y + 5.5F - 4 * u, 0xFFFFFF, u);
        box(g, x + 3, y + 13, 20, 24, BODY);
        float d = (float) (y + 15 + 7 * (1 + Math.sin(angle)) / 2), p = (float) (y + 25 + 7 * (1 + Math.sin(angle + Math.PI / 2)) / 2);
        FcsDraw.fill(g, (int) x + 5, (int) d, (int) x + 21, (int) d + 3, 0xFF9AA3A6);
        FcsDraw.fill(g, (int) x + 5, (int) p, (int) x + 21, (int) p + 4, OUTLINE);
        box(g, x, y + 39, 26, 10, water ? 0xFFCFE0EF : 0xFFE6E9EA);
        if (!water) for (int k = 0; k < 5; k++) FcsDraw.fill(g, (int) x + 3 + k * 5, (int) y + 40, (int) x + 4 + k * 5, (int) y + 49, 0xFF9AA3A6);
        Component c = Component.literal(Math.round(cold) + "°");
        PumpCrossSection.text(g, c, x + 13 - PumpCrossSection.width(c, u) / 2, y + 44 - 4 * u, 0x1C2524, u);
    }

    /** 0.1.11.17: a thermometer 0..{@code max} °C with a mark at {@code mark}. */
    static void thermometer(GuiGraphics g, float x, float y, float h, double celsius, double max, double mark) {
        FcsDraw.fill(g, (int) x - 1, (int) y - 1, (int) x + 5, (int) (y + h) + 1, OUTLINE);
        FcsDraw.fill(g, (int) x, (int) y, (int) x + 4, (int) (y + h), 0xFFF4F6F4);
        float f = (float) Math.max(0, Math.min(1, celsius / max));
        WindRoseView.fill(g, x, y + h * (1 - f), 4, h * f, colour(celsius));
        if (mark > 0) {
            float m = (float) (y + h * (1 - mark / max));
            FcsDraw.fill(g, (int) x - 2, (int) m, (int) x + 6, (int) m + 1, 0xFF2A5D8A);
        }
    }
}
