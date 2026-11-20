package com.miophas.singularity_iteration.common.client.screen;

import com.miophas.singularity_iteration.common.blockentity.mio_icif_Thermal_Generator;
import com.miophas.singularity_iteration.common.client.suit.AsciiHud;
import com.miophas.singularity_iteration.common.client.suit.FcsDraw;
import com.miophas.singularity_iteration.common.menu.generator.ThermalGeneratorMenu;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/**
 * Generator (thermal) screen. 0.1.11.10: 248 x 204 with a circular chart recorder, the kind on a boiler-house wall: a
 * paper disc turns once a game day under a fixed pen arm; the red pen writes the furnace temperature, the blue pen the
 * output. The time lines are arcs (the pens swing on arms), as on a real chart. Beside it the fuel queue, the damper and
 * the readings; under the window the stored energy.
 */
@OnlyIn(Dist.CLIENT)
@SuppressWarnings("null")
public class mio_icif_gui_Thermal_Generator extends mio_icif_screen<ThermalGeneratorMenu> {

    private static final int WX = 34, WY = 15, WW = 207, WH = 95, STRIP_Y = 113;
    private static final int CX = WX + 47, CY = WY + 48, R = 42;
    private static final int COL = WX + 98, COL_W = 105;
    private static final int CHART_PAPER = 0xFFFBFBF6, GRID = 0xFFD9DED9, GRID_MAJOR = 0xFFB9C0BD;
    private static final int PEN_RED = 0xFFC0392B, PEN_BLUE = 0xFF2A5D8A;
    private static final int BLUE = 0x2A5D8A, GREEN = 0x2F6E4F, AMBER = 0xA0602A;
    private static final String K = "gui.mio_icif.thermal.";

    public mio_icif_gui_Thermal_Generator(ThermalGeneratorMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = ThermalGeneratorMenu.GUI_WIDTH;
        this.imageHeight = ThermalGeneratorMenu.GUI_HEIGHT;
    }

    private int v(int i) { return menu.value(i); }

    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        int x = leftPos, y = topPos;
        VanillaFrame.panel(g, x, y, imageWidth, imageHeight);
        VanillaFrame.slot(g, x + ThermalGeneratorMenu.SLOT_X - 1, y + ThermalGeneratorMenu.FUEL_Y - 1);
        VanillaFrame.slot(g, x + ThermalGeneratorMenu.SLOT_X - 1, y + ThermalGeneratorMenu.BATTERY_Y - 1);
        for (int r = 0; r < 3; r++) for (int c = 0; c < 9; c++)
            VanillaFrame.slot(g, x + ThermalGeneratorMenu.INVENTORY_X - 1 + c * 18, y + ThermalGeneratorMenu.INVENTORY_Y - 1 + r * 18);
        for (int c = 0; c < 9; c++) VanillaFrame.slot(g, x + ThermalGeneratorMenu.INVENTORY_X - 1 + c * 18, y + ThermalGeneratorMenu.HOTBAR_Y - 1);

        VanillaFrame.inset(g, x + WX - 1, y + WY - 1, WW + 2, WH + 2, WindRoseView.PAPER);   // 0.1.11.119: the vanilla look
        chart(g, x + CX, y + CY, partialTick);
        column(g, x + COL, y + WY + 5);
        strip(g, x, y + STRIP_Y);
    }

    // ---- the chart --------------------------------------------------------------------------------------------------

    /** Where a pen at radius r (0..R) and paper angle a draws: the arm swings, so the line bends by 2 asin(r / 2R). */
    private static float[] at(float cx, float cy, double a, double r) {
        double bend = 2 * Math.asin(Math.min(1, r / (2.0 * R)));
        double t = a + bend;
        return new float[]{cx + (float) (Math.sin(t) * r), cy - (float) (Math.cos(t) * r)};
    }

    private static double radius(double v) { return R * (0.06 + 0.94 * Math.max(0, Math.min(1, v))); }

    /** Furnace heat (0..1) as a share of the chart's 1500 K rim. */
    private static double kelvinScale(double heat) { return mio_icif_Thermal_Generator.kelvin(heat) / 1500.0; }

    private void chart(GuiGraphics g, float cx, float cy, float partialTick) {
        int rev = mio_icif_Thermal_Generator.REVOLUTION, sample = mio_icif_Thermal_Generator.SAMPLE, n = mio_icif_Thermal_Generator.SAMPLES;
        double phase = v(10) + partialTick;
        double rot = -phase / rev * Math.PI * 2;
        FcsDraw.disc(g, cx, cy, R + 2, CHART_PAPER);
        FcsDraw.circle(g, cx, cy, R + 2, 0.6F, 0xFF9AA3A1);
        for (int k = 1; k <= 5; k++) FcsDraw.circle(g, cx, cy, R * k / 5F, 0.35F, GRID);
        // time lines: 24 arcs (one per game hour), bolder every 6
        for (int h = 0; h < 24; h++) {
            double a = h / 24.0 * Math.PI * 2 + rot;
            float[] p = at(cx, cy, a, 0);
            for (int s = 1; s <= 8; s++) {
                float[] q = at(cx, cy, a, R * s / 8.0);
                FcsDraw.line(g, p[0], p[1], q[0], q[1], h % 6 == 0 ? 0.55F : 0.3F, h % 6 == 0 ? GRID_MAJOR : GRID);
                p = q;
            }
        }
        // the record, oldest first, up to the pen
        int[] heat = new int[n], out = new int[n];
        for (int i = 0; i < n / 2; i++) {
            int w = v(12 + i);
            heat[2 * i] = w & 0xFF;
            out[2 * i] = (w >>> 8) & 0xFF;
            heat[2 * i + 1] = (w >>> 16) & 0xFF;
            out[2 * i + 1] = (w >>> 24) & 0xFF;
        }
        int now = (int) (phase / sample) % n;
        double liveHeat = v(1) / 1000.0, liveOut = v(11) > 0 ? v(2) / (double) v(11) : 0;
        // blue first, red on top; the red pen reads kelvin on a 0..1500 K scale (room temperature is not the centre)
        trace(g, cx, cy, rot, out, now, liveOut, PEN_BLUE, false);
        trace(g, cx, cy, rot, heat, now, liveHeat, PEN_RED, true);
        // the pen arm (fixed at twelve o'clock) and the pen tips
        float[] p = at(cx, cy, 0, 0);
        for (int s = 1; s <= 8; s++) {
            float[] q = at(cx, cy, 0, R * s / 8.0);
            FcsDraw.line(g, p[0], p[1], q[0], q[1], 0.5F, 0xFF6B7170);
            p = q;
        }
        float[] tipH = at(cx, cy, 0, radius(kelvinScale(liveHeat))), tipO = at(cx, cy, 0, radius(liveOut));
        FcsDraw.disc(g, tipH[0], tipH[1], 1.4F, PEN_RED);
        FcsDraw.disc(g, tipO[0], tipO[1], 1.4F, PEN_BLUE);
        FcsDraw.disc(g, cx, cy, 2.6F, 0xFF33393A);
    }

    /** One pen's line: the stored points (0 = blank paper breaks it) from the oldest to the newest, then the pen. */
    private void trace(GuiGraphics g, float cx, float cy, double rot, int[] ink, int now, double live, int argb, boolean kelvin) {
        int n = ink.length;
        double step = Math.PI * 2 / n;
        float[] prev = null;
        for (int k = 1; k < n; k++) {
            int i = (now + k) % n;
            if (ink[i] == 0) { prev = null; continue; }
            double a = (i + 0.5) * step + rot;
            double v = (ink[i] - 1) / 254.0;
            float[] q = at(cx, cy, a, radius(kelvin ? kelvinScale(v) : v));
            if (prev != null) FcsDraw.line(g, prev[0], prev[1], q[0], q[1], 0.7F, argb);
            prev = q;
        }
        float[] pen = at(cx, cy, 0, radius(kelvin ? kelvinScale(live) : live));
        if (prev != null) FcsDraw.line(g, prev[0], prev[1], pen[0], pen[1], 0.7F, argb);
    }

    // ---- readings ---------------------------------------------------------------------------------------------------

    private void column(GuiGraphics g, float x, float y) {
        float u = WindRoseView.u(), lh = 9 * u + 1;
        Component lt = Component.literal("— ").append(Component.translatable(K + "legend_temp"));
        PumpCrossSection.text(g, lt, x, y, PEN_RED & 0xFFFFFF, u);
        PumpCrossSection.text(g, Component.literal("— ").append(Component.translatable(K + "legend_out")),
            x + AsciiHud.width(lt, u) + 6, y, PEN_BLUE & 0xFFFFFF, u);

        // fuel queue: the lump burning, then the fuel slot (up to six shown, then +n)
        float qy = y + lh + 1;
        int left = v(6), dur = Math.max(1, v(7)), count = v(8);
        boolean banked = v(5) != 0;
        for (int c = 0; c < 7; c++) {
            float cxl = x + c * 13;
            g.fill((int) cxl, (int) qy, (int) cxl + 11, (int) qy + 10, 0xFF9AA3A1);
            g.fill((int) cxl + 1, (int) qy + 1, (int) cxl + 10, (int) qy + 9, 0xFFF4F6F4);
            double fill = c == 0 ? Math.min(1, left / (double) dur) : (c <= count ? 1 : 0);
            if (fill > 0) {
                int top = (int) Math.round(qy + 1 + 8 * (1 - fill));
                int col = c == 0 ? (banked ? 0xFF8A3B22 : 0xFFD9792B) : 0xFF3B3F42;
                g.fill((int) cxl + 1, top, (int) cxl + 10, (int) qy + 9, col);
            }
        }
        if (count > 6) PumpCrossSection.text(g, Component.literal("+" + (count - 6)), x + 7 * 13, qy + 1, WindRoseView.DIM, u);
        int per = v(9);
        double full = v(11) / 100.0;
        if (per > 0) WindRoseView.line(g, Component.translatable(K + "per_item", Math.round(per * full), per), x, qy + 12, WindRoseView.DIM, COL_W);

        // damper and state
        float dy = qy + 12 + lh + 2;
        boolean firing = v(0) != 0;
        double heat = v(1) / 1000.0;
        float dx = x + 6;
        double ang = banked ? 0 : Math.PI / 2.4;
        FcsDraw.line(g, dx - (float) Math.cos(ang) * 5, dy + 3 + (float) Math.sin(ang) * 5,
            dx + (float) Math.cos(ang) * 5, dy + 3 - (float) Math.sin(ang) * 5, 1.4F, banked ? 0xFF2A5D8A : 0xFF7A7F85);
        Component state;
        int stateColour;
        if (banked) { state = Component.translatable(K + "state.banked"); stateColour = BLUE; }
        else if (firing && heat < 0.995) { state = Component.translatable(K + "state.warming", Math.round(heat * 100) + "%"); stateColour = AMBER; }
        else if (firing) { state = Component.translatable(K + "state.full"); stateColour = GREEN; }
        else if (heat > 0.005) { state = Component.translatable(K + "state.cooling"); stateColour = WindRoseView.DIM; }
        else { state = Component.translatable(K + "state.cold"); stateColour = WindRoseView.DIM; }
        Component damper = Component.translatable(K + (banked ? "damper_closed" : "damper_open")).append(" · ").append(state);
        WindRoseView.line(g, damper, x + 14, dy, stateColour, COL_W - 14);

        // readings
        float ry = dy + lh + 3;
        WindRoseView.line(g, Component.translatable(K + "temp_line", mio_icif_Thermal_Generator.kelvin(heat),
            String.format(Locale.ROOT, "%.1f", v(2) / 100.0)), x, ry, WindRoseView.INK, COL_W);
        double ticks = Math.max(0, left) + (double) per * count;
        if (ticks > 0) {
            WindRoseView.line(g, Component.translatable(K + "fuel_line", Math.round(ticks * full)), x, ry + lh, WindRoseView.DIM, COL_W);
            long s = Math.round(ticks / 20);
            WindRoseView.line(g, Component.translatable(K + "time_line", s / 60, String.format(Locale.ROOT, "%02d", s % 60)), x, ry + 2 * lh, WindRoseView.DIM, COL_W);
        } else {
            WindRoseView.line(g, Component.translatable(K + "no_fuel"), x, ry + lh, WindRoseView.DIM, COL_W);
        }
    }

    private void strip(GuiGraphics g, int x, int sy) {
        float u = WindRoseView.u();
        Component ec = Component.translatable(K + "stored");
        PumpCrossSection.text(g, ec, x + WX, sy + 6 - 4.5F * u, 0x2A2E33, u);
        float barX = x + WX + AsciiHud.width(ec, u) + 3, barW = x + 241 - 70 - barX;
        long e = Integer.toUnsignedLong(v(3)), cap = Math.max(1, Integer.toUnsignedLong(v(4)));
        g.fill((int) barX - 1, sy + 2, (int) (barX + barW) + 1, sy + 10, SiGuiTheme.SLOT_SHADOW);
        g.fill((int) barX, sy + 3, (int) (barX + barW), sy + 9, SiGuiTheme.SCREEN_BG);
        if (e > 0) g.fillGradient((int) barX, sy + 3, (int) (barX + barW * Math.min(1.0, e / (double) cap)), sy + 9, SiGuiTheme.BAR_FROM, SiGuiTheme.BAR_TO);
        // the damper reopens at 85 %
        int mark = (int) (barX + barW * mio_icif_Thermal_Generator.REOPEN);
        g.fill(mark, sy + 1, mark + 1, sy + 11, 0xFF2A5D8A);
        Component pc = Component.literal(e + " / " + cap + " EU");
        PumpCrossSection.text(g, pc, x + 241 - AsciiHud.width(pc, u), sy + 6 - 4.5F * u, 0x2A2E33, u);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        int rx = mouseX - leftPos, ry = mouseY - topPos;
        if (rx >= WX && rx < WX + 94 && ry >= WY && ry < WY + WH) {
            g.renderComponentTooltip(this.font, List.of(Component.translatable(K + "tip.chart1"), Component.translatable(K + "tip.chart2"),
                Component.translatable(K + "tip.chart3")), mouseX, mouseY);
        } else if (rx >= COL && rx < COL + COL_W && ry >= WY + 14 && ry < WY + 38) {
            g.renderComponentTooltip(this.font, List.of(Component.translatable(K + "tip.fuel1"), Component.translatable(K + "tip.fuel2")), mouseX, mouseY);
        } else if (rx >= COL && rx < COL + COL_W && ry >= WY + 38 && ry < WY + 50) {
            g.renderComponentTooltip(this.font, List.of(Component.translatable(K + "tip.damper1"), Component.translatable(K + "tip.damper2")), mouseX, mouseY);
        } else if (rx >= WX && rx < 241 && ry >= STRIP_Y && ry < STRIP_Y + 12) {
            g.renderComponentTooltip(this.font, List.of(Component.translatable(K + "tip.damper2")), mouseX, mouseY);
        }
    }
}
