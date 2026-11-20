// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.client.suit;

import net.minecraft.client.gui.GuiGraphics;

/**
 * 0.1.11.118: a low-resolution canvas that turns a machine figure into pixel art. While a canvas is open
 * ({@link FcsDraw#beginPixels}), every FcsDraw primitive and {@link FcsDraw#fill} is rasterised into it on whole GUI pixels
 * (one cell = one GUI pixel, as fine as the item icons in the slots) instead of being drawn as smooth geometry, scaled by
 * {@code scale} around the figure's centre. Each primitive is one "part"; when the canvas is drawn, a part with an interior
 * gets a light edge on its upper left and a dark edge on its lower right where it borders what lies behind it (an earlier
 * part or the ground), the way pixel art shades its shapes. Translucent fills (glows) are blended and do not make a part.
 */
public final class PixelCanvas {
    private final int x0, y0, w, h;
    private final float cx, cy, scale;
    private final int[] rgb;      // 0 = empty
    private final short[] part;   // 0 = none
    private short parts;
    private int depth;
    private short current;
    private final int ground;

    public PixelCanvas(int x0, int y0, int w, int h, float cx, float cy, float scale) {
        this(x0, y0, w, h, cx, cy, scale, 0xFF8B8B8B);
    }

    /** {@code ground}: the colour under the canvas (translucent fills blend over it where nothing is drawn yet). */
    public PixelCanvas(int x0, int y0, int w, int h, float cx, float cy, float scale, int ground) {
        this.ground = ground;
        this.x0 = x0; this.y0 = y0; this.w = w; this.h = h; this.cx = cx; this.cy = cy; this.scale = scale;
        this.rgb = new int[w * h];
        this.part = new short[w * h];
    }

    /** Pixel-space x / y of a figure coordinate. */
    private float px(float x) { return cx + (x - cx) * scale - x0; }
    private float py(float y) { return cy + (y - cy) * scale - y0; }

    /** Opens a part (nested calls join the outermost one). */
    void begin() {
        if (depth++ == 0) current = (short) Math.min(Short.MAX_VALUE, ++parts);
    }

    void end() { depth = Math.max(0, depth - 1); }

    private short partFor() { return depth > 0 ? current : (short) Math.min(Short.MAX_VALUE, ++parts); }

    private void plot(int i, int j, int argb, short p) {
        if (i < 0 || j < 0 || i >= w || j >= h) return;
        int k = j * w + i, a = argb >>> 24;
        if (a == 0) return;
        if (a >= 0xF0) {
            rgb[k] = argb | 0xFF000000;
            part[k] = p;
            return;
        }
        int under = rgb[k] == 0 ? ground : rgb[k];   // over the empty window: its fill
        float t = a / 255F;
        int r = Math.round(((argb >> 16) & 255) * t + ((under >> 16) & 255) * (1 - t));
        int g = Math.round(((argb >> 8) & 255) * t + ((under >> 8) & 255) * (1 - t));
        int b = Math.round((argb & 255) * t + (under & 255) * (1 - t));
        rgb[k] = 0xFF000000 | r << 16 | g << 8 | b;
        if (a >= 0x99) part[k] = p;
    }

    /** A filled convex polygon (figure coordinates). */
    void convex(float[] xs, float[] ys, int argb) {
        int n = xs.length;
        float[] X = new float[n], Y = new float[n];
        float minX = Float.MAX_VALUE, minY = Float.MAX_VALUE, maxX = -Float.MAX_VALUE, maxY = -Float.MAX_VALUE;
        for (int i = 0; i < n; i++) {
            X[i] = px(xs[i]); Y[i] = py(ys[i]);
            minX = Math.min(minX, X[i]); maxX = Math.max(maxX, X[i]); minY = Math.min(minY, Y[i]); maxY = Math.max(maxY, Y[i]);
        }
        short p = partFor();
        float area = 0;
        for (int i = 0; i < n; i++) { int k = (i + 1) % n; area += X[i] * Y[k] - X[k] * Y[i]; }
        if (Math.abs(area) < 1e-4F) return;
        float sgn = Math.signum(area);
        int i0 = (int) Math.floor(minX), i1 = (int) Math.ceil(maxX), j0 = (int) Math.floor(minY), j1 = (int) Math.ceil(maxY);
        boolean any = false;
        for (int j = j0; j <= j1; j++) for (int i = i0; i <= i1; i++) {
            float sx = i + 0.5F, sy = j + 0.5F;
            boolean in = true;
            for (int e = 0; e < n && in; e++) {
                int f = (e + 1) % n;
                float c = ((X[f] - X[e]) * (sy - Y[e]) - (Y[f] - Y[e]) * (sx - X[e])) * sgn;
                if (c < -1e-4F) in = false;
            }
            if (in) { plot(i, j, argb, p); any = true; }
        }
        // a sliver thinner than a pixel still shows its centre
        if (!any && maxX - minX < 1.5F && maxY - minY < 1.5F && depth == 0)
            plot((int) Math.floor((minX + maxX) / 2), (int) Math.floor((minY + maxY) / 2), argb, p);
    }

    /** A line at least one pixel wide (figure coordinates). */
    void line(float x1, float y1, float x2, float y2, float width, int argb) {
        float X1 = px(x1), Y1 = py(y1), X2 = px(x2), Y2 = py(y2);
        float half = Math.max(0.5F, width * scale / 2);
        short p = partFor();
        int i0 = (int) Math.floor(Math.min(X1, X2) - half), i1 = (int) Math.ceil(Math.max(X1, X2) + half);
        int j0 = (int) Math.floor(Math.min(Y1, Y2) - half), j1 = (int) Math.ceil(Math.max(Y1, Y2) + half);
        float dx = X2 - X1, dy = Y2 - Y1, len2 = dx * dx + dy * dy;
        for (int j = j0; j <= j1; j++) for (int i = i0; i <= i1; i++) {
            float sx = i + 0.5F, sy = j + 0.5F;
            float t = len2 < 1e-6F ? 0 : Math.max(0, Math.min(1, ((sx - X1) * dx + (sy - Y1) * dy) / len2));
            float ex = X1 + t * dx - sx, ey = Y1 + t * dy - sy;
            if (ex * ex + ey * ey <= half * half + 0.05F) plot(i, j, argb, p);
        }
    }

    /** A filled disc (figure coordinates); a tiny one is one pixel. */
    void disc(float x, float y, float r, int argb) {
        float X = px(x), Y = py(y), R = r * scale;
        short p = partFor();
        if (R < 0.75F) { plot((int) Math.floor(X), (int) Math.floor(Y), argb, p); return; }
        for (int j = (int) Math.floor(Y - R); j <= Math.ceil(Y + R); j++) for (int i = (int) Math.floor(X - R); i <= Math.ceil(X + R); i++) {
            float ex = i + 0.5F - X, ey = j + 0.5F - Y;
            if (ex * ex + ey * ey <= R * R) plot(i, j, argb, p);
        }
    }

    /** An axis-aligned fill in GUI coordinates (scaled like everything else). */
    void rect(float xa, float ya, float xb, float yb, int argb) {
        convex(new float[]{xa, xb, xb, xa}, new float[]{ya, ya, yb, yb}, argb);
    }

    /** Draws the canvas: shaded edges, then runs of equal colour as fills. */
    public void draw(GuiGraphics g) {
        int n = w * h;
        boolean[] interior = new boolean[parts + 1];
        for (int k = 0; k < n; k++) {
            short p = part[k];
            if (p == 0 || interior[p]) continue;
            int i = k % w, j = k / w;
            if (i > 0 && i < w - 1 && j > 0 && j < h - 1 && part[k - 1] == p && part[k + 1] == p && part[k - w] == p && part[k + w] == p)
                interior[p] = true;
        }
        int[] out = rgb.clone();
        for (int k = 0; k < n; k++) {
            short p = part[k];
            if (p == 0 || !interior[p]) continue;
            int i = k % w, j = k / w;
            boolean darkEdge = behind(p, i + 1, j) || behind(p, i, j + 1);
            boolean lightEdge = behind(p, i - 1, j) || behind(p, i, j - 1);
            if (darkEdge) out[k] = shade(rgb[k], 0.62F);
            else if (lightEdge) out[k] = tint(rgb[k], 0.35F);
        }
        for (int j = 0; j < h; j++) {
            int i = 0;
            while (i < w) {
                int c = out[j * w + i];
                int e = i + 1;
                while (e < w && out[j * w + e] == c) e++;
                if (c != 0) g.fill(x0 + i, y0 + j, x0 + e, y0 + j + 1, c);
                i = e;
            }
        }
    }

    /** Whether the pixel at (i, j) lies behind part p (empty, outside, or an earlier part). */
    private boolean behind(short p, int i, int j) {
        if (i < 0 || j < 0 || i >= w || j >= h) return true;
        short q = part[j * w + i];
        return q != p && q < p;
    }

    private static int shade(int c, float f) {
        return 0xFF000000 | Math.round(((c >> 16) & 255) * f) << 16 | Math.round(((c >> 8) & 255) * f) << 8 | Math.round((c & 255) * f);
    }

    private static int tint(int c, float f) {
        int r = (c >> 16) & 255, g = (c >> 8) & 255, b = c & 255;
        return 0xFF000000 | (r + Math.round((255 - r) * f)) << 16 | (g + Math.round((255 - g) * f)) << 8 | (b + Math.round((255 - b) * f));
    }
}
