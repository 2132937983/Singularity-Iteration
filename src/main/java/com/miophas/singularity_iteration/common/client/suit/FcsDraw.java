// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.client.suit;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import org.joml.Matrix4f;

/** 2D drawing helpers in the FCS style: hairlines, brackets, small caps text, glass panels. */
public final class FcsDraw {
    private FcsDraw() {}

    public static final int YELLOW = 0xFFD23F;
    public static final int RED = 0xFF3B30;
    public static final int WHITE = 0xEAF6FF;
    public static final int CYAN = 0x6FE6FF;

    public static int argb(int rgb, float alpha) {
        int a = Math.max(0, Math.min(255, (int) (alpha * 255)));
        return (a << 24) | (rgb & 0xFFFFFF);
    }

    /** Arbitrary line as a thin quad. */
    public static void line(GuiGraphics g, float x1, float y1, float x2, float y2, float width, int argb) {
        float dx = x2 - x1, dy = y2 - y1;
        float len = (float) Math.sqrt(dx * dx + dy * dy);
        if (len < 1.0E-3F) return;
        float nx = -dy / len * width * 0.5F, ny = dx / len * width * 0.5F;
        quad(g, x1 + nx, y1 + ny, x1 - nx, y1 - ny, x2 - nx, y2 - ny, x2 + nx, y2 + ny, argb);
    }

    /** Filled convex quad (corners in order). */
    public static void quad(GuiGraphics g, float x1, float y1, float x2, float y2, float x3, float y3, float x4, float y4, int argb) {
        VertexConsumer vc = g.bufferSource().getBuffer(RenderType.gui());
        Matrix4f m = g.pose().last().pose();
        // the GUI pass culls back faces: emit the quad with the front-facing winding
        float cross = (x2 - x1) * (y3 - y1) - (y2 - y1) * (x3 - x1);
        if (cross > 0) {
            float tx = x2, ty = y2;
            x2 = x4; y2 = y4; x4 = tx; y4 = ty;
        }
        vc.addVertex(m, x1, y1, 0).setColor(argb);
        vc.addVertex(m, x2, y2, 0).setColor(argb);
        vc.addVertex(m, x3, y3, 0).setColor(argb);
        vc.addVertex(m, x4, y4, 0).setColor(argb);
    }

    public static void triangle(GuiGraphics g, float x1, float y1, float x2, float y2, float x3, float y3, int argb) {
        quad(g, x1, y1, x2, y2, x3, y3, x3, y3, argb);
    }

    /** Ring segment from angle a0 to a1 (radians, 0 = up, clockwise). */
    public static void arc(GuiGraphics g, float cx, float cy, float r, float a0, float a1, float width, int argb) {
        int steps = Math.max(2, (int) Math.ceil(Math.abs(a1 - a0) / 0.12F));
        float px = 0, py = 0;
        for (int i = 0; i <= steps; i++) {
            float a = a0 + (a1 - a0) * i / steps;
            float x = cx + (float) Math.sin(a) * r, y = cy - (float) Math.cos(a) * r;
            if (i > 0) line(g, px, py, x, y, width, argb);
            px = x;
            py = y;
        }
    }

    public static void circle(GuiGraphics g, float cx, float cy, float r, float width, int argb) {
        arc(g, cx, cy, r, 0, (float) (Math.PI * 2), width, argb);
    }

    /** Four corner brackets around a box. */
    public static void brackets(GuiGraphics g, float x, float y, float w, float h, float len, float width, int argb) {
        float l = Math.min(len, Math.min(w, h) * 0.45F);
        line(g, x, y, x + l, y, width, argb);
        line(g, x, y, x, y + l, width, argb);
        line(g, x + w, y, x + w - l, y, width, argb);
        line(g, x + w, y, x + w, y + l, width, argb);
        line(g, x, y + h, x + l, y + h, width, argb);
        line(g, x, y + h, x, y + h - l, width, argb);
        line(g, x + w, y + h, x + w - l, y + h, width, argb);
        line(g, x + w, y + h, x + w, y + h - l, width, argb);
    }

    /**
     * Glass panel (0.1.7.34 style): chamfered corners, a header tab with the label, a thin rule
     * that runs on from the tab, an accent bar on the left edge and faint scanlines.
     */
    public static void panel(GuiGraphics g, int x, int y, int w, int h, String label, int rgb) {
        int c = 5;   // chamfer
        int body = 0x8C04080A, body2 = 0x5004080A;
        // body: octagon-ish (chamfer top-left and bottom-right)
        quad(g, x + c, y, x + w, y, x + w, y + h - c, x + c, y + h - c, body);
        quad(g, x, y + c, x + c, y + c, x + c, y + h, x, y + h, body);
        triangle(g, x, y + c, x + c, y, x + c, y + c, body);
        quad(g, x + c, y + h - c, x + w - c, y + h - c, x + w - c, y + h, x + c, y + h, body);
        triangle(g, x + w - c, y + h - c, x + w, y + h - c, x + w - c, y + h, body);
        // lower half slightly lighter: glass gradient
        g.fill(x + 1, y + h / 2, x + w - 1, y + h - c, 0x0AFFFFFF);
        // scanlines
        for (int sy = y + 10; sy < y + h - 2; sy += 2) g.fill(x + 2, sy, x + w - 2, sy + 1, 0x07FFFFFF);
        int edge = argb(rgb, 0.50F), bright = argb(rgb, 0.95F);
        // outline
        line(g, x + c, y, x + w, y, 1F, edge);
        line(g, x + w, y, x + w, y + h - c, 1F, edge);
        line(g, x + w, y + h - c, x + w - c, y + h, 1F, edge);
        line(g, x + w - c, y + h, x, y + h, 1F, edge);
        line(g, x, y + h, x, y + c, 1F, edge);
        line(g, x, y + c, x + c, y, 1F, bright);
        // bright corner accents
        line(g, x + w - 8, y, x + w, y, 1.4F, bright);
        line(g, x + w, y, x + w, y + 5, 1.4F, bright);
        line(g, x, y + h - 6, x, y + h, 1.4F, bright);
        line(g, x, y + h, x + 8, y + h, 1.4F, bright);
        // accent bar
        g.fill(x - 3, y + c + 2, x - 1, y + Math.max(c + 6, h - 6), argb(rgb, 0.75F));
        if (label != null && !label.isEmpty()) {
            float tw = width(label, 0.6F) + 8;
            // header tab
            quad(g, x + c, y, x + c + tw, y, x + c + tw - 3, y + 9, x + 2, y + 9, argb(rgb, 0.85F));
            triangle(g, x + 2, y + 9, x + c, y, x + c, y + 9, argb(rgb, 0.85F));
            line(g, x + c + tw, y + 9, x + w - 3, y + 9, 0.6F, argb(rgb, 0.35F));
            // index ticks on the rule
            for (int k = 0; k < 3; k++) g.fill(x + w - 6 - k * 3, y + 3, x + w - 5 - k * 3, y + 6, argb(rgb, 0.6F - k * 0.15F));
            g.flush();
            text(g, label, x + c + 3, y + 2, 0xF0050A0C, 0.6F);
        } else {
            g.flush();
        }
    }

    /** Segmented ring: {@code count} arcs with gaps, rotated by {@code rot} radians. */
    public static void segRing(GuiGraphics g, float cx, float cy, float r, int count, float fill, float rot, float width, int argb) {
        float step = (float) (Math.PI * 2 / count);
        for (int i = 0; i < count; i++) {
            float a0 = rot + i * step;
            arc(g, cx, cy, r, a0, a0 + step * fill, width, argb);
        }
    }

    /** Line with a soft halo (wide faint pass, then the bright core). */
    public static void glowLine(GuiGraphics g, float x1, float y1, float x2, float y2, float width, int rgb, float alpha) {
        line(g, x1, y1, x2, y2, width * 3.2F, argb(rgb, alpha * 0.18F));
        line(g, x1, y1, x2, y2, width, argb(rgb, alpha));
    }

    /** Filled disc (triangle fan). */
    public static void disc(GuiGraphics g, float cx, float cy, float r, int argb) {
        int steps = Math.max(10, (int) (r * 1.2F));
        for (int i = 0; i < steps; i++) {
            float a0 = (float) (i * Math.PI * 2 / steps), a1 = (float) ((i + 1) * Math.PI * 2 / steps);
            triangle(g, cx, cy, cx + (float) Math.sin(a0) * r, cy - (float) Math.cos(a0) * r,
                cx + (float) Math.sin(a1) * r, cy - (float) Math.cos(a1) * r, argb);
        }
    }

    /** Filled ring sector from a0 to a1 (radians, 0 = up, clockwise). */
    public static void sector(GuiGraphics g, float cx, float cy, float r0, float r1, float a0, float a1, int argb) {
        int steps = Math.max(2, (int) Math.ceil(Math.abs(a1 - a0) / 0.15F));
        for (int i = 0; i < steps; i++) {
            float b0 = a0 + (a1 - a0) * i / steps, b1 = a0 + (a1 - a0) * (i + 1) / steps;
            float s0 = (float) Math.sin(b0), c0 = (float) Math.cos(b0), s1 = (float) Math.sin(b1), c1 = (float) Math.cos(b1);
            quad(g, cx + s0 * r0, cy - c0 * r0, cx + s0 * r1, cy - c0 * r1, cx + s1 * r1, cy - c1 * r1, cx + s1 * r0, cy - c1 * r0, argb);
        }
    }

    /** Dashed line. */
    public static void dashed(GuiGraphics g, float x1, float y1, float x2, float y2, float dash, float width, int argb) {
        float dx = x2 - x1, dy = y2 - y1;
        float len = (float) Math.sqrt(dx * dx + dy * dy);
        if (len < 1) return;
        int n = (int) (len / (dash * 2));
        for (int i = 0; i <= n; i++) {
            float t0 = i * dash * 2 / len, t1 = Math.min(1, t0 + dash / len);
            if (t0 >= 1) break;
            line(g, x1 + dx * t0, y1 + dy * t0, x1 + dx * t1, y1 + dy * t1, width, argb);
        }
    }

    /** Small "chip" label: filled tag with dark text. */
    public static void chip(GuiGraphics g, String s, float x, float y, int rgb, float scale) {
        float tw = width(s, scale);
        float hgt = 9 * scale + 2;
        quad(g, x, y, x + tw + 5, y, x + tw + 3, y + hgt, x - 2, y + hgt, argb(rgb, 0.88F));
        g.flush();
        text(g, s, x + 1.5F, y + 1.5F, 0xF0050A0C, scale);
    }

    public static Font font() { return Minecraft.getInstance().font; }

    public static void text(GuiGraphics g, String s, float x, float y, int argb, float scale) {
        g.pose().pushPose();
        g.pose().translate(x, y, 0);
        g.pose().scale(scale, scale, 1);
        g.drawString(font(), s, 0, 0, argb, false);
        g.pose().popPose();
    }

    public static void text(GuiGraphics g, Component s, float x, float y, int argb, float scale) {
        g.pose().pushPose();
        g.pose().translate(x, y, 0);
        g.pose().scale(scale, scale, 1);
        g.drawString(font(), s, 0, 0, argb, false);
        g.pose().popPose();
    }

    public static void textCentered(GuiGraphics g, String s, float cx, float y, int argb, float scale) {
        text(g, s, cx - font().width(s) * scale / 2, y, argb, scale);
    }

    public static void textRight(GuiGraphics g, String s, float right, float y, int argb, float scale) {
        text(g, s, right - font().width(s) * scale, y, argb, scale);
    }

    public static float width(String s, float scale) { return font().width(s) * scale; }

    public static float width(Component s, float scale) {
        FormattedCharSequence seq = s.getVisualOrderText();
        return font().width(seq) * scale;
    }

    /** Meter bar: frame + fill. */
    public static void bar(GuiGraphics g, int x, int y, int w, int h, float fraction, int rgb) {
        g.fill(x, y, x + w, y + h, 0x60000000);
        int fill = (int) (Math.max(0, Math.min(1, fraction)) * (w - 2));
        g.fill(x + 1, y + 1, x + 1 + fill, y + h - 1, argb(rgb, 0.9F));
        for (int i = 1; i < 10; i++) g.fill(x + i * w / 10, y + h - 2, x + i * w / 10 + 1, y + h, 0x80000000);
    }

    /** "12.3k" style EU numbers. */
    public static String si(double v) {
        double a = Math.abs(v);
        if (a >= 1.0E9) return String.format(java.util.Locale.ROOT, "%.2fG", v / 1.0E9);
        if (a >= 1.0E6) return String.format(java.util.Locale.ROOT, "%.2fM", v / 1.0E6);
        if (a >= 1.0E4) return String.format(java.util.Locale.ROOT, "%.1fk", v / 1.0E3);
        if (a >= 100) return String.format(java.util.Locale.ROOT, "%.0f", v);
        return String.format(java.util.Locale.ROOT, "%.1f", v);
    }
}
