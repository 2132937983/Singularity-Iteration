// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.client.screen.panel;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/**
 * 0.1.10.21: chart pieces of the data panels (the exchange panel; the grid monitoring panel reuses them): candles with
 * volume, line overlays, sparklines, horizontal bars and number formatting. Immediate mode, GUI coordinates.
 * Colours: rising teal / falling coral by default; {@link #colourBlind} switches to blue / orange.
 */
@OnlyIn(Dist.CLIENT)
public final class PanelCharts {
    private PanelCharts() {}

    public static final int PANEL = 0xF0161B22, PANEL_LINE = 0xFF2C3440, GRID = 0x30FFFFFF, TEXT = 0xFFE6EDF3, DIM = 0xFF8B97A6;
    public static final int ACCENT = 0xFF4FC3F7, WARN = 0xFFFFC857;
    public static boolean colourBlind;

    public static int up() { return colourBlind || PanelStyle.colourBlind ? 0xFF4F8DFF : 0xFF2EC4A6; }

    public static int down() { return colourBlind || PanelStyle.colourBlind ? 0xFFFF9F43 : 0xFFFF6B6B; }

    public static void panel(GuiGraphics g, int x, int y, int w, int h) {
        g.fill(x, y, x + w, y + h, PANEL);
        g.renderOutline(x, y, w, h, PANEL_LINE);
    }

    /** Candles of {@code open/high/low/close} (oldest first) with volume bars in the lowest fifth of the box. */
    public static void candles(GuiGraphics g, int x, int y, int w, int h, long[] open, long[] high, long[] low, long[] close, long[] volume,
                               long overlayLow, long overlayHigh) {
        int n = close.length;
        if (n == 0) return;
        long min = Long.MAX_VALUE, max = Long.MIN_VALUE, vmax = 1;
        for (int i = 0; i < n; i++) { min = Math.min(min, low[i]); max = Math.max(max, high[i]); vmax = Math.max(vmax, volume[i]); }
        if (overlayLow > 0) min = Math.min(min, overlayLow);
        if (overlayHigh > 0) max = Math.max(max, overlayHigh);
        if (max == min) { max++; min = Math.max(0, min - 1); }
        int priceH = h * 4 / 5 - 2, volTop = y + h * 4 / 5;
        for (int k = 1; k < 4; k++) g.fill(x, y + priceH * k / 4, x + w, y + priceH * k / 4 + 1, GRID);
        double step = w / (double) n;
        int body = Math.max(1, (int) (step * 0.6));
        for (int i = 0; i < n; i++) {
            int cx = x + (int) (step * i + step / 2);
            int colour = close[i] >= open[i] ? up() : down();
            int yh = yOf(high[i], min, max, y, priceH), yl = yOf(low[i], min, max, y, priceH);
            int yo = yOf(open[i], min, max, y, priceH), yc = yOf(close[i], min, max, y, priceH);
            g.fill(cx, yh, cx + 1, yl + 1, colour);
            g.fill(cx - body / 2, Math.min(yo, yc), cx - body / 2 + body, Math.max(yo, yc) + 1, colour);
            int vh = (int) ((h / 5 - 2) * volume[i] / (double) vmax);
            if (vh > 0) g.fill(cx - body / 2, y + h - vh, cx - body / 2 + body, y + h, (colour & 0x00FFFFFF) | 0x80000000);
        }
        g.fill(x, volTop - 1, x + w, volTop, PANEL_LINE);
    }

    /** A line over the candle box (same scale): the moving average, the fundamental value. */
    public static void line(GuiGraphics g, int x, int y, int w, int h, double[] values, long min, long max, int colour, boolean dashed) {
        int n = values.length, priceH = h * 4 / 5 - 2;
        if (n == 0 || max <= min) return;
        double step = w / (double) n;
        for (int i = 1; i < n; i++) {
            if (Double.isNaN(values[i - 1]) || Double.isNaN(values[i])) continue;
            if (dashed && i % 2 == 0) continue;
            int x0 = x + (int) (step * (i - 1) + step / 2), x1 = x + (int) (step * i + step / 2);
            int y0 = yOf(values[i - 1], min, max, y, priceH), y1 = yOf(values[i], min, max, y, priceH);
            segment(g, x0, y0, x1, y1, colour);
        }
    }

    /** The scale the candles used, for {@link #line} and labels: {min, max} ({@link PanelStyle#scale}). */
    public static long[] scale(long[] low, long[] high, long overlayLow, long overlayHigh) {
        return PanelStyle.scale(low, high, overlayLow, overlayHigh);
    }

    /** Small line of values in a box, coloured by the last change. */
    public static void sparkline(GuiGraphics g, int x, int y, int w, int h, long[] values) {
        int n = values.length;
        if (n < 2) return;
        long min = Long.MAX_VALUE, max = Long.MIN_VALUE;
        for (long v : values) { min = Math.min(min, v); max = Math.max(max, v); }
        if (max == min) { max++; min--; }
        int colour = values[n - 1] >= values[0] ? up() : down();
        double step = (w - 1) / (double) (n - 1);
        for (int i = 1; i < n; i++)
            segment(g, x + (int) (step * (i - 1)), y + h - 1 - (int) ((h - 1) * (values[i - 1] - min) / (double) (max - min)),
                x + (int) (step * i), y + h - 1 - (int) ((h - 1) * (values[i] - min) / (double) (max - min)), colour);
    }

    /** A horizontal bar of {@code fraction} (0..1) from the right ({@code right}) or the left. */
    public static void bar(GuiGraphics g, int x, int y, int w, int h, double fraction, int colour, boolean right) {
        int len = (int) Math.round(w * Math.max(0, Math.min(1, fraction)));
        if (len <= 0) return;
        if (right) g.fill(x + w - len, y, x + w, y + h, colour); else g.fill(x, y, x + len, y + h, colour);
    }

    static int yOf(double value, long min, long max, int y, int h) {
        return y + h - (int) Math.round(h * (value - min) / (double) (max - min));
    }

    /** A one-pixel line (stepped). */
    public static void segment(GuiGraphics g, int x0, int y0, int x1, int y1, int colour) {
        int dx = Math.abs(x1 - x0), dy = Math.abs(y1 - y0), steps = Math.max(1, Math.max(dx, dy));
        for (int i = 0; i <= steps; i++) {
            int px = x0 + (x1 - x0) * i / steps, py = y0 + (y1 - y0) * i / steps;
            g.fill(px, py, px + 1, py + 1, colour);
        }
    }

    /** 12,345 / 1.2M style (0.1.10.33: {@link PanelStyle#number}). */
    public static String number(long value) {
        return PanelStyle.number(value);
    }

    /** +1.2 % from permille ({@link PanelStyle#percent}). */
    public static String percent(int permille) {
        return PanelStyle.percent(permille);
    }

    /** Text right-aligned at {@code right}. */
    public static void right(GuiGraphics g, Font font, String text, int right, int y, int colour) {
        g.drawString(font, text, right - font.width(text), y, colour, false);
    }
}
