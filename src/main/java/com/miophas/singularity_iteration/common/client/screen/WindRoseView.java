// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.client.screen;

import com.miophas.singularity_iteration.common.client.suit.AsciiHud;
import com.miophas.singularity_iteration.common.client.suit.FcsDraw;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

/**
 * 0.1.11.9: drawing shared by the two wind machine screens: the wind rose (16 bearings; a sector's length is how often
 * the wind came from there over the last minute or so, its shade how strong it was; the rotor's facing as a black
 * needle and the wind's bearing as a red one) and small chart helpers. Text is in the HUD font at whole pixels.
 */
final class WindRoseView {
    private WindRoseView() {}

    static final int PAPER = 0xFFF4F6F4, RING = 0xFFD5DAD8, INK = 0x1C2524, DIM = 0x5B6867, SECTOR = 0x23454A, RED = 0xC0392B;

    static float u() {
        return (float) Math.min(1.0, 2.0 / Math.max(1.0, Minecraft.getInstance().getWindow().getGuiScale()));
    }

    /** Unpacks 16 bytes from four menu ints. */
    static int[] bytes(java.util.function.IntUnaryOperator value, int first) {
        int[] out = new int[16];
        for (int i = 0; i < 4; i++) {
            int v = value.applyAsInt(first + i);
            for (int k = 0; k < 4; k++) out[i * 4 + k] = (v >>> (8 * k)) & 0xFF;
        }
        return out;
    }

    /**
     * The rose at (cx, cy) with radius r. {@code share}: 0..255 per bearing; {@code wind}: mean wind / 2 per bearing.
     * {@code facing}: the rotor's bearing (-1 none); {@code bearing}: the wind's bearing now.
     */
    static void rose(GuiGraphics g, float cx, float cy, float r, int[] share, int[] wind, int facing, int bearing) {
        for (float rr : new float[]{r / 3, r * 2 / 3, r}) FcsDraw.circle(g, cx, cy, rr, 0.4F, RING);
        int max = 1;
        for (int s : share) max = Math.max(max, s);
        for (int b = 0; b < 16; b++) {
            if (share[b] == 0) continue;
            float a0 = (float) Math.toRadians(b * 22.5 - 9), a1 = (float) Math.toRadians(b * 22.5 + 9);
            float shade = Math.max(0.25F, Math.min(0.95F, 0.25F + wind[b] * 2 / 120F));
            FcsDraw.sector(g, cx, cy, 0, r * share[b] / max, a0, a1, FcsDraw.argb(SECTOR, shade));
        }
        String[] points = {"n", "e", "s", "w"};
        float u = u();
        for (int k = 0; k < 4; k++) {
            double a = Math.toRadians(k * 90);
            Component c = Component.translatable("gui.mio_icif.wind.point." + points[k]);
            float tx = cx + (float) Math.sin(a) * (r + 5), ty = cy - (float) Math.cos(a) * (r + 5);
            PumpCrossSection.text(g, c, tx - AsciiHud.width(c, u) / 2, ty - 4 * u, DIM, u);
        }
        if (facing >= 0) needle(g, cx, cy, r, facing, 1.0F, 0xFF1C2524);
        needle(g, cx, cy, r * 0.95F, bearing, 0.8F, 0xFFC0392B);
    }

    private static void needle(GuiGraphics g, float cx, float cy, float r, int bearing, float width, int argb) {
        double a = Math.toRadians(bearing);
        FcsDraw.line(g, cx, cy, cx + (float) Math.sin(a) * r, cy - (float) Math.cos(a) * r, width, argb);
    }

    static void fill(GuiGraphics g, float x, float y, float w, float h, int argb) {
        if (w <= 0 || h <= 0) return;
        FcsDraw.quad(g, x, y, x, y + h, x + w, y + h, x + w, y, argb);
    }

    static void dashed(GuiGraphics g, float x1, float y1, float x2, float y2, int argb) {
        FcsDraw.dashed(g, x1, y1, x2, y2, 1.5F, 0.8F, argb);
    }

    /** One line of text cut with an ellipsis to {@code maxW}. */
    static void line(GuiGraphics g, Component c, float x, float y, int rgb, float maxW) {
        if (ReadoutPanel.active()) { ReadoutPanel.add(0, c, rgb); return; }   // 0.1.11.121: into the readout window
        float u = u();
        if (AsciiHud.width(c, u) > maxW) {
            // 0.1.11.24: the longest prefix that fits, by halving (it measured one character shorter at a time)
            String s = c.getString();
            int lo = 1, hi = s.length();
            while (lo < hi) {
                int mid = (lo + hi + 1) >>> 1;
                if (AsciiHud.width(s.substring(0, mid) + "…", u) > maxW) hi = mid - 1; else lo = mid;
            }
            c = Component.literal(s.substring(0, lo) + "…");
        }
        PumpCrossSection.text(g, c, x, y, rgb, u);
    }

    static String factor(int thousandths) {
        return String.format(java.util.Locale.ROOT, "×%.2f", thousandths / 1000.0);
    }

    /** "49° NE" */
    static Component bearing(int degrees) {
        return Component.literal(degrees + "° ").append(Component.translatable("gui.mio_icif.wind.point."
            + com.miophas.singularity_iteration.common.blockentity.kuentity.kugenerator.WindPhysics.point(degrees)));
    }
}
