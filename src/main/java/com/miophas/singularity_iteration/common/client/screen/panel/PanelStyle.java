// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.client.screen.panel;

import java.util.Locale;

/**
 * 0.1.10.33: the colours and number formats of the data panels without any Minecraft type (moved out of {@link PanelCharts},
 * which keeps its methods), so that the exchange panel's pages ({@link FuturePanel}) run in the offline layout harness.
 */
public final class PanelStyle {
    private PanelStyle() {}

    public static final int PANEL = 0xF0161B22, PANEL_LINE = 0xFF2C3440, GRID = 0x30FFFFFF, TEXT = 0xFFE6EDF3, DIM = 0xFF8B97A6;
    public static final int ACCENT = 0xFF4FC3F7, WARN = 0xFFFFC857;
    public static boolean colourBlind;

    public static int up() { return colourBlind ? 0xFF4F8DFF : 0xFF2EC4A6; }

    public static int down() { return colourBlind ? 0xFFFF9F43 : 0xFFFF6B6B; }

    /** 12,345 / 1.2M style. */
    public static String number(long value) {
        long a = Math.abs(value);
        // 0.1.10.33: 10,000.0M and more read 10.0B
        if (a >= 10_000_000_000L) return String.format(Locale.ROOT, "%.1fB", value / 1.0e9);
        if (a >= 10_000_000L) return String.format(Locale.ROOT, "%.1fM", value / 1.0e6);
        return String.format(Locale.ROOT, "%,d", value);
    }

    /** 0.1.10.33: a short form for narrow places: 9,999 / 12.3k / 1.23M / 1.23B. */
    public static String compact(long value) {
        long a = Math.abs(value);
        if (a < 10_000) return String.format(Locale.ROOT, "%,d", value);
        if (a < 1_000_000) return String.format(Locale.ROOT, "%.1fk", value / 1.0e3);
        if (a < 1_000_000_000) return String.format(Locale.ROOT, "%.2fM", value / 1.0e6);
        return String.format(Locale.ROOT, "%.2fB", value / 1.0e9);
    }

    /** +1.2 % from permille. */
    public static String percent(int permille) {
        return String.format(Locale.ROOT, "%+.1f%%", permille / 10.0);
    }

    /** The scale of candles and their overlays: {min, max}. */
    public static long[] scale(long[] low, long[] high, long overlayLow, long overlayHigh) {
        long min = Long.MAX_VALUE, max = Long.MIN_VALUE;
        for (int i = 0; i < low.length; i++) { min = Math.min(min, low[i]); max = Math.max(max, high[i]); }
        if (overlayLow > 0) min = Math.min(min, overlayLow);
        if (overlayHigh > 0) max = Math.max(max, overlayHigh);
        if (low.length == 0) { min = 0; max = 1; }
        if (max == min) { max++; min = Math.max(0, min - 1); }
        return new long[] {min, max};
    }
}
