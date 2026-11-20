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

    public static void begin() { open = new ArrayList<>(); seq = 0; }

    public static boolean active() { return open != null; }

    /** Adds a line; lines are written by {@code order}, then in the order they came. */
    public static void add(int order, Component text, int rgb) {
        if (open != null) open.add(new Entry(order, seq++, text, rgb));
    }

    /** Draws the window at (x, y) (outer edge, w x h) with the collected lines, and closes the panel. */
    public static void end(GuiGraphics g, int x, int y, int w, int h) {
        List<Entry> lines = open;
        open = null;
        VanillaFrame.inset(g, x, y, w, h, VanillaFrame.PANEL);
        if (lines == null) return;
        lines.sort((a, b) -> a.order != b.order ? Integer.compare(a.order, b.order) : Integer.compare(a.seq, b.seq));
        Font font = Minecraft.getInstance().font;
        int maxW = w - 8, rows = (h - 4) / ROW_H, ty = y + 4, used = 0;
        for (Entry e : lines) {
            int colour = vanilla(e.rgb);
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
