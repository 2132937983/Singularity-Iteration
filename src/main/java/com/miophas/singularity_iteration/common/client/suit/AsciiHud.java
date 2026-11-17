// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.client.suit;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;

/**
 * 0.1.11.5: drawing helpers of the text-mode ("ASCII") suit HUD. Everything is text in Minecraft's {@code uniform} font
 * (Unifont, monospace for Latin, digits and box-drawing characters), drawn without a shadow on a dark glass backing.
 * Columns are placed by measured width, never by padding with spaces, so Latin and CJK text line up in every language.
 *
 * <p>Conventions: frames are {@code ┌─ title ─┐ │ │ └─┘}; bars are {@code ▕████░░░░▏}; message levels are {@code !!}
 * (critical, red), {@code !} (warning, amber) and {@code ·} (note).
 */
public final class AsciiHud {
    private AsciiHud() {}

    /** The monospace font of the HUD. */
    public static final ResourceLocation FONT = ResourceLocation.withDefaultNamespace("uniform");
    private static final Style STYLE = Style.EMPTY.withFont(FONT);

    /** HUD text colours: body, dim, accent, warning, critical. */
    public static final int TEXT = 0xE9FFF6, DIM = 0x9FC9BB, OK = 0x7FE0C4, WARN = 0xFFD166, CRIT = 0xFF6B5E;
    /** Backing glass behind text blocks. */
    public static final int GLASS = 0x9E060C0E;
    /** Line height in GUI pixels at scale 1. */
    public static final float LINE = 10F;

    /** {@code s} in the HUD font. */
    public static MutableComponent t(String s) {
        return Component.literal(s).withStyle(STYLE);
    }

    /** {@code c} in the HUD font (a translated component keeps its arguments). */
    public static MutableComponent t(Component c) {
        return c.copy().withStyle(STYLE);
    }

    public static float width(String s, float scale) { return FcsDraw.width(t(s), scale); }

    public static float width(Component c, float scale) { return FcsDraw.width(t(c), scale); }

    public static void text(GuiGraphics g, String s, float x, float y, int rgb, float scale) {
        FcsDraw.text(g, t(s), x, y, FcsDraw.argb(rgb, 1F), scale);
    }

    public static void text(GuiGraphics g, Component c, float x, float y, int rgb, float scale) {
        FcsDraw.text(g, t(c), x, y, FcsDraw.argb(rgb, 1F), scale);
    }

    public static void textRight(GuiGraphics g, String s, float right, float y, int rgb, float scale) {
        text(g, s, right - width(s, scale), y, rgb, scale);
    }

    public static void textCentered(GuiGraphics g, Component c, float cx, float y, int rgb, float scale) {
        text(g, c, cx - width(c, scale) / 2, y, rgb, scale);
    }

    /** A translated line in the HUD font, centred on {@code cx}, with an ARGB colour (for fading read-outs). */
    public static void centred(GuiGraphics g, String key, float cx, float y, int argb, float scale, Object... args) {
        MutableComponent c = t(Component.translatable(key, args));
        FcsDraw.text(g, c, cx - FcsDraw.width(c, scale) / 2, y, argb, scale);
    }

    /** A translated line in the HUD font, right-aligned on {@code right}, with an ARGB colour. */
    public static void right(GuiGraphics g, String key, float right, float y, int argb, float scale, Object... args) {
        MutableComponent c = t(Component.translatable(key, args));
        FcsDraw.text(g, c, right - FcsDraw.width(c, scale), y, argb, scale);
    }

    /** A translated line in the HUD font at (x, y), with an ARGB colour. */
    public static void left(GuiGraphics g, String key, float x, float y, int argb, float scale, Object... args) {
        FcsDraw.text(g, t(Component.translatable(key, args)), x, y, argb, scale);
    }

    /** A bar of {@code cells} cells, {@code ▕████░░░░▏}. */
    public static String bar(float fraction, int cells) {
        int full = Math.round(Math.max(0, Math.min(1, fraction)) * cells);
        return "▕" + "█".repeat(full) + "░".repeat(cells - full) + "▏";
    }

    /** {@code ch} repeated as often as fits in {@code px} at {@code scale}. */
    public static String fill(char ch, float px, float scale) {
        float one = Math.max(0.5F, width(String.valueOf(ch), scale));
        return String.valueOf(ch).repeat(Math.max(0, (int) Math.floor(px / one)));
    }

    /**
     * A frame around the box (x, y, w, h) with the dark backing and the title cut into the top edge; the title may be null.
     * The rows inside start at {@code y + LINE * scale}. The edges are 1-pixel lines (Unifont's box-drawing glyphs are
     * narrower than their advance and leave gaps); the contents stay text.
     */
    public static void box(GuiGraphics g, float x, float y, float w, float h, Component title, int rgb, float scale) {
        g.fill((int) x, (int) y, (int) (x + w), (int) (y + h), GLASS);
        int col = FcsDraw.argb(rgb, 0.85F);
        float mid = y + LINE * scale * 0.4F;
        float tx = x + 4 * scale;
        float te = tx;
        if (title != null) {
            text(g, title, tx + 2 * scale, y, TEXT, scale);
            te = tx + 4 * scale + width(title, scale);
        }
        FcsDraw.line(g, x, mid, tx, mid, 1F, col);
        FcsDraw.line(g, te, mid, x + w, mid, 1F, col);
        FcsDraw.line(g, x, mid, x, y + h, 1F, col);
        FcsDraw.line(g, x + w, mid, x + w, y + h, 1F, col);
        FcsDraw.line(g, x, y + h, x + w, y + h, 1F, col);
    }
}
