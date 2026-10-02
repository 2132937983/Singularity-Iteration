// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.client.screen;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Mth;

/**
 * Infographic drawing kit for the "holographic display" panels (energy terminal, equipment
 * console statistics): a dark navy screen set into the light IC2 frame, thin cyan frame
 * corners, a faint grid, tick-marked meters, arc gauges and sparkline/area charts.
 */
public final class DspUi {
    private DspUi() {}

    public static final int BG = 0xFF0C1622, BG_HI = 0xFF13212F, GRID = 0x1A7FD8F5, FRAME = 0xFF2B4A63;
    public static final int CYAN = 0xFF5FD3F5, CYAN_DIM = 0xFF3F8BA6, ORANGE = 0xFFF5A23C, GREEN = 0xFF63D88A,
        RED = 0xFFF05A5A, VIOLET = 0xFFA98BF5, TEXT = 0xFFE6F4FA, TEXT_DIM = 0xFF7F9AAB;

    /** Dark display screen with a 1px frame, corner brackets and a faint grid. */
    public static void screen(GuiGraphics g, int x, int y, int w, int h) {
        g.fill(x, y, x + w, y + h, 0xFF56606A);
        g.fill(x + 1, y + 1, x + w - 1, y + h - 1, BG);
        g.fillGradient(x + 1, y + 1, x + w - 1, y + Math.min(h - 1, 12), BG_HI, BG);
        for (int gx = x + 8; gx < x + w - 1; gx += 8) g.fill(gx, y + 1, gx + 1, y + h - 1, GRID);
        for (int gy = y + 8; gy < y + h - 1; gy += 8) g.fill(x + 1, gy, x + w - 1, gy + 1, GRID);
        corner(g, x + 1, y + 1, 1, 1);
        corner(g, x + w - 2, y + 1, -1, 1);
        corner(g, x + 1, y + h - 2, 1, -1);
        corner(g, x + w - 2, y + h - 2, -1, -1);
    }

    private static void corner(GuiGraphics g, int x, int y, int dx, int dy) {
        for (int i = 0; i < 4; i++) {
            g.fill(x + i * dx, y, x + i * dx + 1, y + 1, CYAN_DIM);
            g.fill(x, y + i * dy, x + 1, y + i * dy + 1, CYAN_DIM);
        }
    }

    /** Horizontal meter: track, gradient fill, end cap and quarter ticks. */
    public static void meter(GuiGraphics g, int x, int y, int w, int h, float fraction, int color) {
        g.fill(x, y, x + w, y + h, 0xFF1C2C3B);
        int f = Math.round(Mth.clamp(fraction, 0, 1) * w);
        if (f > 0) {
            g.fillGradient(x, y, x + f, y + h, brighten(color), color);
            g.fill(x + f - 1, y - 1, x + f, y + h + 1, 0xFFFFFFFF);
        }
        for (int q = 1; q < 4; q++) g.fill(x + q * w / 4, y + h, x + q * w / 4 + 1, y + h + 2, CYAN_DIM);
    }

    /** 270-degree arc gauge drawn from pixel blocks (centre cx,cy, radius r, thickness t). */
    public static void arc(GuiGraphics g, int cx, int cy, int r, int t, float fraction, int color) {
        float f = Mth.clamp(fraction, 0, 1);
        int steps = Math.max(24, r * 6);
        for (int i = 0; i <= steps; i++) {
            float p = i / (float) steps;
            double ang = Math.toRadians(135 + p * 270);
            int c = p <= f ? color : 0xFF1F3243;
            for (int k = 0; k < t; k++) {
                int px = cx + (int) Math.round(Math.cos(ang) * (r - k)), py = cy + (int) Math.round(Math.sin(ang) * (r - k));
                g.fill(px, py, px + 1, py + 1, c);
            }
        }
    }

    /** Filled area chart of {@code values} (0..max) with a bright top line. */
    public static void area(GuiGraphics g, int x, int y, int w, int h, float[] values, float max, int color) {
        int n = values.length;
        float step = w / (float) n;
        int prev = -1;
        for (int i = 0; i < n; i++) {
            int x0 = x + (int) (i * step), x1 = Math.max(x0 + 1, x + (int) ((i + 1) * step));
            int v = Math.round(h * Mth.clamp(values[i] / Math.max(1e-6F, max), 0, 1));
            if (v > 0) g.fill(x0, y + h - v, x1, y + h, (color & 0x00FFFFFF) | 0x38000000);
            int top = y + h - v;
            if (prev >= 0) g.fill(x0, Math.min(prev, top), x0 + 1, Math.max(prev, top) + 1, color);
            g.fill(x0, top, x1, top + 1, color);
            prev = top;
        }
    }

    /** Dotted line chart (used for the storage charge curve). */
    public static void line(GuiGraphics g, int x, int y, int w, int h, float[] values, float max, int color) {
        int n = values.length;
        float step = w / (float) n;
        for (int i = 0; i < n; i++) {
            int x0 = x + (int) (i * step);
            int v = y + h - 1 - Math.round((h - 1) * Mth.clamp(values[i] / Math.max(1e-6F, max), 0, 1));
            if (i % 2 == 0) g.fill(x0, v, x0 + 1, v + 1, color);
        }
    }

    public static void small(GuiGraphics g, Font font, String text, int x, int y, int color) {
        g.pose().pushPose();
        g.pose().translate(x, y, 0);
        g.pose().scale(0.75F, 0.75F, 1);
        g.drawString(font, text, 0, 0, color, false);
        g.pose().popPose();
    }

    public static int smallWidth(Font font, String text) {
        return (int) Math.ceil(font.width(text) * 0.75F);
    }

    public static int brighten(int argb) {
        int r = Math.min(255, ((argb >> 16) & 255) + 50), gg = Math.min(255, ((argb >> 8) & 255) + 50), b = Math.min(255, (argb & 255) + 50);
        return (argb & 0xFF000000) | (r << 16) | (gg << 8) | b;
    }

    /** Compact number: 12.3k, 4.56M ... */
    public static String compact(double v) {
        double a = Math.abs(v);
        if (a >= 1e9) return String.format("%.2fG", v / 1e9);
        if (a >= 1e6) return String.format("%.2fM", v / 1e6);
        if (a >= 1e4) return String.format("%.1fk", v / 1e3);
        if (a >= 100) return String.format("%.0f", v);
        return String.format("%.1f", v);
    }

    /** m:ss / h:mm:ss for a duration in seconds; "∞" when not depleting. */
    public static String duration(double seconds) {
        if (!Double.isFinite(seconds) || seconds < 0) return "∞";
        long s = Math.round(seconds);
        if (s >= 360000) return ">99h";
        if (s >= 3600) return String.format("%d:%02d:%02d", s / 3600, (s / 60) % 60, s % 60);
        return String.format("%d:%02d", s / 60, s % 60);
    }
}
