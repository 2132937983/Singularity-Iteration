package com.miophas.singularity_iteration.common.client.screen;

import net.minecraft.client.gui.GuiGraphics;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/**
 * SI "clean modern IC2" GUI theme for code-drawn widgets (matte light grey, thin flat
 * outlines, light inset displays with dark text, one steel-blue accent). Colours match the GUI
 * textures (tools/assetgen si_gui.py) so drawn panels, wells and slots blend in.
 *
 * <p>0.1.11.183: the panel, outline, bevels and slots are the vanilla container's (as {@link VanillaFrame}, which the
 * processing machines and generators use); the textures were recoloured to match (tools/gui_vanilla_111183.py). The wells
 * are the light paper of the readout windows; the accent and status colours stay.
 */
@OnlyIn(Dist.CLIENT)
public final class SiGuiTheme {
    private SiGuiTheme() {}

    public static final int OUTLINE = VanillaFrame.OUTLINE;
    public static final int BEVEL_HI = VanillaFrame.LIGHT;
    public static final int BEVEL_LO = VanillaFrame.SHADOW;
    public static final int PANEL_TOP = VanillaFrame.PANEL;
    public static final int PANEL_BOTTOM = VanillaFrame.PANEL;
    public static final int SLOT_FILL = VanillaFrame.SLOT;
    public static final int SLOT_SHADOW = VanillaFrame.SLOT_DARK;
    public static final int SLOT_HI = VanillaFrame.LIGHT;
    public static final int SCREEN_BG = 0xFFF4F6F4;
    public static final int SCREEN_GRID = 0xFFE2E6E2;
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

    /** The window panel (0.1.11.183: the vanilla container's, {@link VanillaFrame#panel}). */
    public static void panel(GuiGraphics g, int x, int y, int w, int h) {
        VanillaFrame.panel(g, x, y, w, h);
    }

    /** 18x18 inventory slot frame (item drawn at x + 1, y + 1; 0.1.11.183: the vanilla slot). */
    public static void slot(GuiGraphics g, int x, int y) {
        VanillaFrame.slot(g, x, y);
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
        g.fill(x + 1, y, x + 4, y + 5, SLOT_SHADOW);
        g.fill(x, y + 1, x + 5, y + 4, SLOT_SHADOW);
        g.fill(x + 1, y + 1, x + 4, y + 4, color);
        g.fill(x + 1, y + 1, x + 2, y + 2, 0x80FFFFFF);
    }

    /** Engraved horizontal groove (shadow line + highlight line). */
    public static void groove(GuiGraphics g, int x, int y, int w) {
        g.fill(x, y, x + w, y + 1, VanillaFrame.SLOT);
        g.fill(x, y + 1, x + w, y + 2, VanillaFrame.LIGHT);
    }
}
