// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.client.screen;

import com.miophas.singularity_iteration.common.blockentity.producer.PumpSurvey;
import com.miophas.singularity_iteration.common.blockentity.producer.mio_icif_pump_elc;
import com.miophas.singularity_iteration.common.client.suit.AsciiHud;
import com.miophas.singularity_iteration.common.client.suit.FcsDraw;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;

/**
 * 0.1.11.7: the pump screen's cross-section of the body of fluid it works on, seen from the side along the pump's
 * facing. The server sends {@link PumpSurvey#COLUMNS} columns (lowest and highest fluid block of each, relative to the
 * pump); the client draws the banks and the bed as earth, the fluid in its own texture and tint, and a surface that
 * moves as a 1D wave equation.
 *
 * <ul>
 *   <li>Level: the surface sits where the volume under it matches the share still there (pumping: buckets left / the
 *       most seen; discharging: cells filled / basin size). A pool that refills itself stays full.</li>
 *   <li>Waves: speed and damping follow the fluid's viscosity (water lively, lava slow). Pumping pulls the surface down
 *       at the intake and kicks a wave each bucket; discharging drops a jet at the outlet.</li>
 *   <li>Marks: the pump and its pipe to the intake or the outlet (the lift), the keep level or the target height as a
 *       dashed line, and a staff gauge in blocks relative to the pump on the right.</li>
 * </ul>
 */
public final class PumpCrossSection {
    private static final int N = 72;
    private final float[] h = new float[N], v = new float[N];
    private long lastNanos;
    private int lastPaid = -1;
    private final java.util.Random rnd = new java.util.Random();

    private static final int AIR = 0xFFEEF0F1, GRID = 0xFFE4E6E8, EARTH = 0xFF7B5B3E, EARTH_DARK = 0xFF5E4430, BED = 0xFF4A3727;
    private static final int INK = 0x2A2E33, DIM = 0x80868D, RED = 0xC8423C, AMBER = 0xD2902A;

    /** What the screen knows this frame. */
    public record Data(int mode, int count, int flags, int peak, int filled, byte[] low, byte[] high, int pumpColumn, int activeColumn,
                       int liftRel, Fluid fluid, int paidWork, boolean working, int keep, int target) { }

    public void render(GuiGraphics g, int x, int y, int w, int h, Data d) {
        long t = System.nanoTime();
        float dt = lastNanos == 0 ? 0.016F : Math.min(0.05F, (t - lastNanos) / 1.0E9F);
        lastNanos = t;
        float u = (float) Math.min(1.0, 2.0 / Math.max(1.0, Minecraft.getInstance().getWindow().getGuiScale()));
        g.fill(x, y, x + w, y + h, AIR);
        for (int gx = x + 8; gx < x + w; gx += 8) g.fill(gx, y, gx + 1, y + h, GRID);
        for (int gy = y + 8; gy < y + h; gy += 8) g.fill(x, gy, x + w, gy + 1, GRID);
        boolean discharge = d.mode() == mio_icif_pump_elc.MODE_DISCHARGE;
        boolean known = (d.flags() & (mio_icif_pump_elc.FLAG_SCANNING | mio_icif_pump_elc.FLAG_NO_BODY)) == 0;
        int[] lo = new int[PumpSurvey.COLUMNS], hi = new int[PumpSurvey.COLUMNS];
        int minY = Integer.MAX_VALUE, maxY = Integer.MIN_VALUE;
        boolean any = false;
        for (int c = 0; c < PumpSurvey.COLUMNS; c++) {
            int l = d.low()[c] & 0xFF, hh = d.high()[c] & 0xFF;
            if (l == PumpSurvey.NONE) { lo[c] = Integer.MIN_VALUE; continue; }
            lo[c] = l - PumpSurvey.OFFSET;
            hi[c] = hh - PumpSurvey.OFFSET + 1;                       // the top of the highest block
            minY = Math.min(minY, lo[c]); maxY = Math.max(maxY, hi[c]);
            any = true;
        }
        if (!known || !any || d.fluid() == null) {
            Component msg = Component.translatable("gui.mio_icif.pump.view." + ((d.flags() & mio_icif_pump_elc.FLAG_SCANNING) != 0 ? "scanning"
                : discharge ? "no_basin" : "no_body"));
            text(g, msg, x + w / 2F - PumpCrossSection.width(msg, u) / 2, y + h / 2F - 4 * u, DIM, u);
            return;
        }
        // vertical range: the body, the pump (at 0) and the target, with a block of margin
        int top = Math.max(maxY, 1) + 1;
        if (discharge) top = Math.max(top, d.target() + 2);
        int bottom = minY - 1;
        if (top - bottom < 6) bottom = top - 6;
        float py = (h - 2F) / (top - bottom);
        float colW = (w - 10F) / PumpSurvey.COLUMNS;                // 10 px on the right for the staff gauge
        final int topRel = top;
        java.util.function.DoubleUnaryOperator yOf = rel -> y + 1 + (topRel - rel) * py;

        // the level: the volume under it matches the share still there (or already filled)
        double share;
        if (discharge) share = d.peak() <= 0 ? 1 : d.filled() / (double) d.peak();
        else share = (d.flags() & (mio_icif_pump_elc.FLAG_INFINITE | mio_icif_pump_elc.FLAG_CAPPED)) != 0 || d.peak() <= 0 ? 1 : d.count() / (double) d.peak();
        double level = levelFor(lo, hi, minY, maxY, Math.max(0, Math.min(1, share)));

        // the fluid texture over the whole view, covered where it is not
        Fluid fluid = d.fluid();
        var ext = IClientFluidTypeExtensions.of(fluid);
        TextureAtlasSprite sprite = Minecraft.getInstance().getTextureAtlas(TextureAtlas.LOCATION_BLOCKS).apply(ext.getStillTexture());
        int tint = ext.getTintColor();
        int visc = Math.max(1, fluid.getFluidType().getViscosity());
        stepWaves(dt, visc, d, discharge);
        g.enableScissor(x, y, x + w - 10, y + h);
        g.setColor(((tint >> 16) & 255) / 255F, ((tint >> 8) & 255) / 255F, (tint & 255) / 255F, 1F);
        int tile = 16;
        for (int ty = y; ty < y + h; ty += tile) for (int tx = x; tx < x + w - 10; tx += tile) g.blit(tx, ty, 0, tile, tile, sprite);
        g.setColor(1F, 1F, 1F, 1F);
        // deeper is darker
        for (int k = 0; k < 6; k++) {
            float y0 = (float) yOf.applyAsDouble(level) + k * (h / 6F);
            fillF(g, x, y0, w - 10, h / 6F + 1, FcsDraw.argb(0x000000, 0.04F * k));
        }
        float[] surface = new float[PumpSurvey.COLUMNS + 1];
        for (int c = 0; c < PumpSurvey.COLUMNS; c++) {
            float cx0 = x + c * colW, cx1 = cx0 + colW + 0.6F;
            if (lo[c] == Integer.MIN_VALUE) {
                // a bank: earth up to the top of the body
                float bank = (float) yOf.applyAsDouble(maxY);
                fillF(g, cx0, y, cx1 - cx0, bank - y, AIR);
                fillF(g, cx0, bank, cx1 - cx0, y + h - bank, EARTH);
                fillF(g, cx0, bank, cx1 - cx0, 0.8F, EARTH_DARK);
                surface[c] = Float.NaN;
                continue;
            }
            double colLevel = Math.min(level, hi[c]);
            float wave = waveAt((c + 0.5F) / PumpSurvey.COLUMNS);
            float surf = (float) yOf.applyAsDouble(colLevel) + (colLevel > lo[c] ? wave : 0);
            float floor = (float) yOf.applyAsDouble(lo[c]);
            if (colLevel <= lo[c]) surf = floor;
            surface[c] = surf;
            fillF(g, cx0, y, cx1 - cx0, surf - y, AIR);
            fillF(g, cx0, floor, cx1 - cx0, y + h - floor, EARTH);
            fillF(g, cx0, floor, cx1 - cx0, 0.8F, BED);
        }
        // surface line
        for (int c = 0; c < PumpSurvey.COLUMNS - 1; c++) {
            if (Float.isNaN(surface[c]) || Float.isNaN(surface[c + 1])) continue;
            float xa = x + (c + 0.5F) * colW, xb = x + (c + 1.5F) * colW;
            if (Math.abs(surface[c] - surface[c + 1]) < py * 1.5F) FcsDraw.line(g, xa, surface[c], xb, surface[c + 1], 0.6F, 0xB0FFFFFF);
        }
        // earth texture: a few darker speckles, fixed per column
        for (int c = 0; c < PumpSurvey.COLUMNS; c++) {
            float base = lo[c] == Integer.MIN_VALUE ? (float) yOf.applyAsDouble(maxY) : (float) yOf.applyAsDouble(lo[c]);
            for (int k = 0; k < 4; k++) {
                int hsh = c * 73 + k * 31;
                float sy = base + 3 + (hsh % 11) * (h / 14F);
                if (sy < y + h - 1) fillF(g, x + c * colW + (hsh % 5) * colW / 5, sy, 1.2F, 0.8F, 0x40000000);
            }
        }
        // keep level or target height (dashed)
        if (!discharge && d.keep() > 0 && d.peak() > 0 && (d.flags() & (mio_icif_pump_elc.FLAG_INFINITE | mio_icif_pump_elc.FLAG_CAPPED)) == 0) {
            double keepLevel = levelFor(lo, hi, minY, maxY, Math.min(1, d.keep() / (double) d.peak()));
            dashed(g, x, (float) yOf.applyAsDouble(keepLevel), w - 10, AMBER);
        }
        if (discharge) dashed(g, x, (float) yOf.applyAsDouble(d.target() + 1), w - 10, RED);
        // the pump and its pipe to the intake / outlet
        float pumpX = x + (d.pumpColumn() + 0.5F) * colW, pumpY = (float) yOf.applyAsDouble(1);
        if (d.liftRel() != Integer.MIN_VALUE && d.activeColumn() >= 0 && d.activeColumn() < PumpSurvey.COLUMNS) {
            float ax = x + (d.activeColumn() + 0.5F) * colW, ay = (float) yOf.applyAsDouble(d.liftRel() + 0.5);
            FcsDraw.line(g, pumpX, pumpY + 3, pumpX, ay, 1.2F, 0xFF6E747A);
            FcsDraw.line(g, pumpX, ay, ax, ay, 1.2F, 0xFF6E747A);
            if (d.working() && !discharge) vortex(g, ax, ay, colW);
        }
        fillF(g, pumpX - 3, pumpY - 3, 6, 6, 0xFF585D63);
        fillF(g, pumpX - 2, pumpY - 2, 4, 4, d.working() ? 0xFF6FCB8E : 0xFFA8ACB1);
        g.flush();
        g.disableScissor();

        // staff gauge: one red or white segment per block, labels every 5 blocks (relative to the pump)
        float gx = x + w - 9;
        for (int r = bottom; r < top; r++) {
            float a0 = (float) yOf.applyAsDouble(r + 1), a1 = (float) yOf.applyAsDouble(r);
            fillF(g, gx, a0, 3, a1 - a0, Math.floorMod(r, 2) == 0 ? 0xFFC8423C : 0xFFF4F4F4);
            if (Math.floorMod(r, 5) == 0) {
                String lbl = r == 0 ? "0" : (r > 0 ? "+" : "") + r;
                text(g, Component.literal(lbl), gx + 3.5F, a1 - 4 * u, INK, u);
            }
        }
        if ((d.flags() & mio_icif_pump_elc.FLAG_INFINITE) != 0 && !discharge) text(g, Component.literal("∞"), x + 3, y + 2, 0x3A6EA5, u);
    }

    /** The level whose volume (column by column) is {@code share} of the full body. */
    private static double levelFor(int[] lo, int[] hi, int minY, int maxY, double share) {
        double total = volume(lo, hi, maxY);
        if (total <= 0) return minY;
        double want = total * share, a = minY, b = maxY;
        for (int i = 0; i < 30; i++) {
            double m = (a + b) / 2;
            if (volume(lo, hi, m) < want) a = m; else b = m;
        }
        return (a + b) / 2;
    }

    private static double volume(int[] lo, int[] hi, double level) {
        double v = 0;
        for (int c = 0; c < lo.length; c++) if (lo[c] != Integer.MIN_VALUE) v += Math.max(0, Math.min(level, hi[c]) - lo[c]);
        return v;
    }

    // ------------------------------------------------------------------ waves

    /** One wave step: speed and damping from the viscosity; the intake pulls, each bucket (or pour) kicks. */
    private void stepWaves(float dt, int viscosity, Data d, boolean discharge) {
        float k = 0.22F / (1 + (viscosity - 1000) / 1500F);
        float damp = 0.988F - Math.min(0.05F, Math.max(0, viscosity - 1000) / 120000F);
        int at = Math.max(0, Math.min(N - 1, (int) ((d.activeColumn() + 0.5F) / PumpSurvey.COLUMNS * N)));
        boolean kicked = lastPaid >= 0 && d.paidWork() < lastPaid;
        lastPaid = d.paidWork();
        if (d.working() && d.activeColumn() >= 0 && d.activeColumn() < PumpSurvey.COLUMNS) {
            if (!discharge) h[at] += 0.04F;                          // the intake pulls the surface down a little
            if (kicked) {
                h[at] += discharge ? -2.5F : 2.2F;
            }
        }
        int steps = Math.max(1, Math.round(dt * 60));
        for (int s = 0; s < steps; s++) {
            for (int i = 0; i < N; i++) {
                float l = h[Math.max(0, i - 1)], r = h[Math.min(N - 1, i + 1)];
                v[i] = (v[i] + k * (l + r - 2 * h[i]) - 0.01F * h[i]) * damp;
            }
            for (int i = 0; i < N; i++) h[i] += v[i];
        }
        // a little ambient ripple
        if (rnd.nextFloat() < 0.05F) h[rnd.nextInt(N)] += (rnd.nextFloat() - 0.5F) * 0.4F;
    }

    private float waveAt(float f) {
        float p = f * (N - 1);
        int i = (int) p;
        float fr = p - i;
        return h[Math.min(N - 1, i)] * (1 - fr) + h[Math.min(N - 1, i + 1)] * fr;
    }

    private void vortex(GuiGraphics g, float cx, float cy, float colW) {
        float t = (System.nanoTime() % 6_283_000_000L) / 1.0E9F;
        float px = 0, py = 0;
        for (int i = 0; i <= 24; i++) {
            float a = t * 6 + i * 0.5F, r = 0.4F + i * 0.12F;
            float qx = cx + (float) Math.cos(a) * r * 1.3F, qy = cy - (float) Math.sin(a) * r * 0.5F - i * 0.08F;
            if (i > 0) FcsDraw.line(g, px, py, qx, qy, 0.4F, 0x90FFFFFF);
            px = qx; py = qy;
        }
    }

    private static void dashed(GuiGraphics g, float x, float yy, float w, int rgb) {
        for (float dx = 0; dx < w; dx += 4) fillF(g, x + dx, yy - 0.3F, Math.min(2.4F, w - dx), 0.6F, FcsDraw.argb(rgb, 0.9F));
    }

    private static void fillF(GuiGraphics g, float x, float y, float w, float h, int argb) {
        if (w <= 0 || h <= 0) return;
        FcsDraw.quad(g, x, y, x, y + h, x + w, y + h, x + w, y, argb);
    }

    /**
     * Window text. 0.1.11.145: in the vanilla font at a whole scale on whole GUI pixels (it was small Unifont on screen
     * pixels), so the charts and diagrams read like the rest of the screen.
     */
    static void text(GuiGraphics g, Component c, float x, float y, int rgb, float scale) {
        if (!WindRoseView.vanillaText) {   // the screens not yet on the calm layout: small Unifont on screen pixels
            float gs = (float) Minecraft.getInstance().getWindow().getGuiScale();
            FcsDraw.text(g, AsciiHud.t(c), Math.round(x * gs) / gs, Math.round(y * gs) / gs, FcsDraw.argb(rgb, 1F), scale);
            return;
        }
        FcsDraw.text(g, c, Math.round(x), Math.round(y), FcsDraw.argb(rgb, 1F), whole(scale));
    }

    static void text(GuiGraphics g, String s, float x, float y, int rgb, float scale) {
        text(g, Component.literal(s), x, y, rgb, scale);
    }

    /** 0.1.11.145: the width of window text (the vanilla font at the whole scale {@link #text} uses). */
    static float width(Component c, float scale) {
        return WindRoseView.vanillaText ? Minecraft.getInstance().font.width(c) * whole(scale) : AsciiHud.width(c, scale);
    }

    static float width(String s, float scale) {
        return WindRoseView.vanillaText ? Minecraft.getInstance().font.width(s) * whole(scale) : AsciiHud.width(s, scale);
    }

    private static float whole(float scale) { return Math.max(1, Math.round(scale)); }
}
