package com.miophas.singularity_iteration.common.client.screen;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/**
 * 0.1.11.121: the readout window of the generator screens in the vanilla look and font. While it is open
 * ({@link #begin}), the reading lines ({@link WindRoseView#line}, {@code MimicScreen.line}) are collected instead of drawn;
 * {@link #end} draws a sunk grey window and writes them in the vanilla font (one font pixel a GUI pixel), each line
 * broken at its " · " separators onto as many rows as it needs.
 */
@OnlyIn(Dist.CLIENT)
public final class ReadoutPanel {
    private ReadoutPanel() {}

    public static final int ROW_H = 10, TEXT = 0x404040;
    private record Entry(int order, int seq, Component text, int rgb) {}
    private static List<Entry> open;
    private static int seq;

    public static void begin() { open = new ArrayList<>(); seq = 0; head = null; side = null; }

    private static Component head, side;
    private static int sideRgb;

    /**
     * 0.1.11.145: the main readout (the screen's most important figure, usually its output), written twice the vanilla
     * font size above the lines, with {@code sideText} (a share of the rating, or why it stands) right-aligned on its row
     * in {@code sideColour}. When both do not fit on the row, the side text goes down as the first line.
     */
    public static void headline(Component headline, Component sideText, int sideColour) {
        head = headline; side = sideText; sideRgb = sideColour;
    }

    public static boolean active() { return open != null; }

    /** Adds a line; lines are written by {@code order}, then in the order they came. */
    public static void add(int order, Component text, int rgb) {
        if (open != null) open.add(new Entry(order, seq++, text, rgb));
    }

    /** 0.1.11.144: the third level of text (the calm layout's muted lines). */
    public static final int MUTED = 0x80868A;

    /**
     * Writes the collected lines in the area at (x, y) (outer edge, w x h; the window's left edge is x + 1) and closes the
     * panel. 0.1.11.144 (calm layout): no window, the text sits on the panel; the first line in the body colour, the
     * later lines muted, a line in a state colour (running, fault) keeps it.
     */
    public static void end(GuiGraphics g, int x, int y, int w, int h) {
        List<Entry> lines = open;
        open = null;
        if (lines == null) return;
        lines.sort((a, b) -> a.order != b.order ? Integer.compare(a.order, b.order) : Integer.compare(a.seq, b.seq));
        Font font = Minecraft.getInstance().font;
        int maxW = w - 2, rows = (h - 4) / ROW_H, ty = y + 4, used = 0;
        x -= 3;   // the text starts at the window's left edge (x + 1); it was 3 pixels inside the window
        boolean firstLine = true;
        Component headline = head, sideText = side;
        head = null; side = null;
        if (headline != null) {
            int left = x + 4, right = x + 3 + w - 1;
            boolean below = sideText != null && 2 * font.width(headline) + 8 + font.width(sideText) > right - left;
            g.pose().pushPose();
            g.pose().translate(left, y + 2, 0);
            g.pose().scale(2F, 2F, 1F);
            g.drawString(font, headline, 0, 0, 0x1C2524, false);
            g.pose().popPose();
            if (sideText != null && !below) g.drawString(font, sideText, right - font.width(sideText), y + 6, sideRgb & 0xFFFFFF, false);
            if (below) lines.add(0, new Entry(Integer.MIN_VALUE, -1, sideText, sideRgb));
            ty = y + 23;
            rows = (h - 23) / ROW_H + ((h - 23) % ROW_H >= 8 ? 1 : 0);
            firstLine = true;
        }
        for (Entry e : lines) {
            int colour = vanilla(e.rgb);
            if (colour == TEXT && !firstLine) colour = MUTED;
            if (colour == TEXT || colour == MUTED) firstLine = false;
            // the pieces: the " · " parts, a part too wide for a row broken into its words
            List<String> pieces = new ArrayList<>(), joins = new ArrayList<>();
            for (String part : e.text.getString().split(" · ")) {
                if (font.width(part) <= maxW) { pieces.add(part); joins.add(" · "); continue; }
                boolean first = true;
                for (String word : part.split(" ")) { pieces.add(word); joins.add(first ? " · " : " "); first = false; }
            }
            String row = "";
            for (int k = 0; k < pieces.size(); k++) {
                String next = row.isEmpty() ? pieces.get(k) : row + joins.get(k) + pieces.get(k);
                if (!row.isEmpty() && font.width(next) > maxW) {
                    if (used >= rows) return;
                    g.drawString(font, cut(font, row, maxW), x + 4, ty, colour, false);
                    ty += ROW_H; used++;
                    row = pieces.get(k);
                } else {
                    row = next;
                }
            }
            if (!row.isEmpty()) {
                if (used >= rows) return;
                g.drawString(font, cut(font, row, maxW), x + 4, ty, colour, false);
                ty += ROW_H; used++;
            }
        }
    }

    /** The screens' ink colours become the vanilla text colour; the state colours stay. */
    private static int vanilla(int rgb) {
        rgb &= 0xFFFFFF;
        return rgb == 0x1C2524 || rgb == 0x2A2E33 || rgb == 0x5B6867 ? TEXT : rgb;
    }

    private static String cut(Font font, String s, int maxW) {
        return font.width(s) <= maxW ? s : font.plainSubstrByWidth(s, Math.max(0, maxW - font.width("…"))) + "…";
    }
}
