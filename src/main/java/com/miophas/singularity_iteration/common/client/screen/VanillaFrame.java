package com.miophas.singularity_iteration.common.client.screen;

import net.minecraft.client.gui.GuiGraphics;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/**
 * 0.1.11.118: the vanilla container look drawn in code: the light grey panel with its black rounded outline, white upper
 * left and dark lower right bevel; slots and windows sunk into it (dark upper left, white lower right); the furnace's
 * progress arrow (grey, filling white) and an energy bolt (grey, filling yellow from the bottom).
 */
@OnlyIn(Dist.CLIENT)
public final class VanillaFrame {
    private VanillaFrame() {}

    public static final int PANEL = 0xFFC6C6C6, LIGHT = 0xFFFFFFFF, SHADOW = 0xFF555555, OUTLINE = 0xFF000000,
        SLOT = 0xFF8B8B8B, SLOT_DARK = 0xFF373737, BOLT = 0xFFF2C14E;
    /** RGB of the vanilla container title. */
    public static final int TITLE = 0x404040;

    public static void panel(GuiGraphics g, int x, int y, int w, int h) {
        g.fill(x + 2, y, x + w - 2, y + 1, OUTLINE);
        g.fill(x + 2, y + h - 1, x + w - 2, y + h, OUTLINE);
        g.fill(x, y + 2, x + 1, y + h - 2, OUTLINE);
        g.fill(x + w - 1, y + 2, x + w, y + h - 2, OUTLINE);
        g.fill(x + 1, y + 1, x + 2, y + 2, OUTLINE);
        g.fill(x + w - 2, y + 1, x + w - 1, y + 2, OUTLINE);
        g.fill(x + 1, y + h - 2, x + 2, y + h - 1, OUTLINE);
        g.fill(x + w - 2, y + h - 2, x + w - 1, y + h - 1, OUTLINE);
        g.fill(x + 1, y + 1, x + w - 1, y + h - 1, PANEL);
        g.fill(x + 2, y + 1, x + w - 3, y + 3, LIGHT);
        g.fill(x + 1, y + 2, x + 3, y + h - 3, LIGHT);
        g.fill(x + 3, y + h - 3, x + w - 2, y + h - 1, SHADOW);
        g.fill(x + w - 3, y + 3, x + w - 1, y + h - 2, SHADOW);
        g.fill(x + 3, y + 3, x + w - 3, y + h - 3, PANEL);
    }

    /** A window sunk into the panel, filled with {@code fill}; (x, y, w, h) is its outer edge. */
    public static void inset(GuiGraphics g, int x, int y, int w, int h, int fill) {
        g.fill(x, y, x + w - 1, y + 1, SLOT_DARK);
        g.fill(x, y, x + 1, y + h - 1, SLOT_DARK);
        g.fill(x + 1, y + h - 1, x + w, y + h, LIGHT);
        g.fill(x + w - 1, y + 1, x + w, y + h, LIGHT);
        g.fill(x + 1, y + 1, x + w - 1, y + h - 1, fill);
    }

    /** An 18 x 18 slot whose item sits at (x + 1, y + 1). */
    public static void slot(GuiGraphics g, int x, int y) {
        inset(g, x, y, 18, 18, SLOT);
    }

    /** The furnace arrow (22 x 15) at (x, y), filled white from the left by {@code fraction}. */
    public static void arrow(GuiGraphics g, int x, int y, double fraction) {
        int fill = (int) Math.round(22 * Math.max(0, Math.min(1, fraction)));
        for (int j = 0; j < 15; j++) {
            int d = Math.abs(j - 7);
            int x1 = 15 + 8 - d;                       // the head narrows to the tip
            int x0 = j >= 4 && j <= 10 ? 0 : 15;       // the shaft
            if (x1 <= x0) continue;
            int split = Math.max(x0, Math.min(x1, fill));
            if (split > x0) g.fill(x + x0, y + j, x + split, y + j + 1, LIGHT);
            if (x1 > split) g.fill(x + split, y + j, x + x1, y + j + 1, SLOT);
        }
    }

    private static final String[] BOLT_ROWS = {
        ".....####", "....####.", "...####..", "..####...", ".#######.", "#######..", "...####..",
        "..####...", "..###....", ".###.....", ".##......", "##.......", "#........"};

    /** The energy bolt (9 x 13) at (x, y), filled yellow from the bottom by {@code fraction}. */
    public static void bolt(GuiGraphics g, int x, int y, double fraction) {
        int h = BOLT_ROWS.length, filledFrom = h - (int) Math.round(h * Math.max(0, Math.min(1, fraction)));
        for (int j = 0; j < h; j++) {
            String row = BOLT_ROWS[j];
            int c = j >= filledFrom ? BOLT : SLOT;
            for (int i = 0; i < row.length(); i++) if (row.charAt(i) == '#') {
                int e = i;
                while (e < row.length() && row.charAt(e) == '#') e++;
                g.fill(x + i, y + j, x + e, y + j + 1, c);
                i = e - 1;
            }
        }
    }

    /**
     * 0.1.11.121: a strip line in the vanilla font: {@code label}, a furnace-style bar (sunk grey, filled white) and
     * {@code right} at the right end, across {@code w} pixels from x.
     */
    public static int[] labelledBar(GuiGraphics g, int x, int y, int w, net.minecraft.network.chat.Component label, double share,
                                   net.minecraft.network.chat.Component right) {
        net.minecraft.client.gui.Font font = net.minecraft.client.Minecraft.getInstance().font;
        g.drawString(font, label, x, y + 2, TITLE, false);
        int rw = font.width(right);
        g.drawString(font, right, x + w - rw, y + 2, TITLE, false);
        int bx = x + font.width(label) + 4, bw = x + w - rw - 4 - bx;
        if (bw < 6) return new int[]{bx, 0};
        inset(g, bx, y + 2, bw, 8, SLOT);
        int fill = (int) Math.round((bw - 2) * Math.max(0, Math.min(1, share)));
        if (fill > 0) g.fill(bx + 1, y + 3, bx + 1 + fill, y + 9, LIGHT);
        return new int[]{bx + 1, bw - 2};   // the fill's left edge and width (for marks)
    }
}
