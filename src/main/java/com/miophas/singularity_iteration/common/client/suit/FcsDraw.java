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

    /** Glass panel: dark translucent body, hairline border, header strip with label. */
    public static void panel(GuiGraphics g, int x, int y, int w, int h, String label, int rgb) {
        g.fill(x, y, x + w, y + h, 0x70060A0C);
        int border = argb(rgb, 0.55F);
        g.fill(x, y, x + w, y + 1, border);
        g.fill(x, y + h - 1, x + w, y + h, border);
        g.fill(x, y, x + 1, y + h, border);
        g.fill(x + w - 1, y, x + w, y + h, border);
        brackets(g, x - 1.5F, y - 1.5F, w + 3, h + 3, 5, 1.2F, argb(rgb, 0.95F));
        if (label != null && !label.isEmpty()) {
            g.fill(x + 1, y + 1, x + w - 1, y + 9, argb(rgb, 0.16F));
            g.flush();
            text(g, label, x + 3, y + 2, argb(rgb, 1F), 0.62F);
        } else {
            g.flush();
        }
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
