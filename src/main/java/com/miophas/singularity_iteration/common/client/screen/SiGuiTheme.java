package com.miophas.singularity_iteration.common.client.screen;

import net.minecraft.client.gui.GuiGraphics;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/**
 * SI "clean modern IC2" GUI theme for code-drawn widgets (matte light grey, thin flat
 * outlines, light inset displays with dark text, one steel-blue accent). Colours match the GUI
 * textures (tools/assetgen si_gui.py) so drawn panels, wells and slots blend in.
 */
@OnlyIn(Dist.CLIENT)
public final class SiGuiTheme {
    private SiGuiTheme() {}

    public static final int OUTLINE = 0xFF585D63;
    public static final int BEVEL_HI = 0xFFF8F9F9;
    public static final int BEVEL_LO = 0xFFAAAEB3;
    public static final int PANEL_TOP = 0xFFE0E2E3;
    public static final int PANEL_BOTTOM = 0xFFD3D5D7;
    public static final int SLOT_FILL = 0xFFA8ACB1;
    public static final int SLOT_SHADOW = 0xFF7A7F85;
    public static final int SLOT_HI = 0xFFF7F8F9;
    public static final int SCREEN_BG = 0xFFEEF0F1;
    public static final int SCREEN_GRID = 0xFFE4E6E8;
    public static final int ACCENT = 0xFF3A6EA5;
    public static final int ACCENT_DIM = 0xFF7EA0C6;
    public static final int TEXT = 0x303438;
    public static final int TEXT_SCREEN = 0xFF2A2E33;
    public static final int TEXT_SCREEN_DIM = 0xFF80868D;
    public static final int GOOD = 0xFF3C9A52;
    public static final int WARN = 0xFFD2902A;
    public static final int BAD = 0xFFC8423C;
    public static final int BAR_FROM = 0xFF6E9ACB;
    public static final int BAR_TO = 0xFF3A6EA5;
    public static final int LED_OFF = 0xFF9AA0A6;

    /** Flat window panel: thin outline, fine inner highlight/shadow. */
    public static void panel(GuiGraphics g, int x, int y, int w, int h) {
        g.fill(x, y, x + w, y + h, OUTLINE);
        g.fillGradient(x + 1, y + 1, x + w - 1, y + h - 1, PANEL_TOP, PANEL_BOTTOM);
        g.fill(x + 1, y + 1, x + w - 1, y + 2, BEVEL_HI);
        g.fill(x + 1, y + 1, x + 2, y + h - 1, BEVEL_HI);
        g.fill(x + 1, y + h - 2, x + w - 1, y + h - 1, BEVEL_LO);
        g.fill(x + w - 2, y + 1, x + w - 1, y + h - 1, BEVEL_LO);
    }

    /** 18x18 inventory slot frame (item drawn at x + 1, y + 1). */
    public static void slot(GuiGraphics g, int x, int y) {
        g.fill(x, y, x + 18, y + 18, SLOT_HI);
        g.fill(x, y, x + 17, y + 17, SLOT_SHADOW);
        g.fill(x + 1, y + 1, x + 17, y + 17, SLOT_FILL);
    }

    /** Light inset display (dark text) with a faint grid. */
    public static void well(GuiGraphics g, int x, int y, int w, int h) {
        g.fill(x, y, x + w, y + h, SLOT_HI);
        g.fill(x, y, x + w - 1, y + h - 1, SLOT_SHADOW);
        g.fill(x + 1, y + 1, x + w - 1, y + h - 1, SCREEN_BG);
        for (int gx = x + 8; gx < x + w - 1; gx += 8) g.fill(gx, y + 1, gx + 1, y + h - 1, SCREEN_GRID);
        for (int gy = y + 8; gy < y + h - 1; gy += 8) g.fill(x + 1, gy, x + w - 1, gy + 1, SCREEN_GRID);
    }

    /** Horizontal gauge inside a well-style frame. */
    public static void bar(GuiGraphics g, int x, int y, int w, int h, double fraction, int from, int to) {
        g.fill(x - 1, y - 1, x + w + 1, y + h + 1, SLOT_SHADOW);
        g.fill(x, y, x + w, y + h, SCREEN_BG);
        int filled = (int) Math.round(w * Math.max(0, Math.min(1, fraction)));
        if (filled > 0) g.fillGradient(x, y, x + filled, y + h, from, to);
    }

    /** Small round status lamp (5x5) with a bezel; colour = GOOD/WARN/BAD/LED_OFF. */
    public static void led(GuiGraphics g, int x, int y, int color) {
        g.fill(x + 1, y, x + 4, y + 5, 0xFF7A7F85);
        g.fill(x, y + 1, x + 5, y + 4, 0xFF7A7F85);
        g.fill(x + 1, y + 1, x + 4, y + 4, color);
        g.fill(x + 1, y + 1, x + 2, y + 2, 0x80FFFFFF);
    }

    /** Engraved horizontal groove (shadow line + highlight line). */
    public static void groove(GuiGraphics g, int x, int y, int w) {
        g.fill(x, y, x + w, y + 1, 0xFFB0B4B9);
        g.fill(x, y + 1, x + w, y + 2, 0xFFF5F6F7);
    }
}
