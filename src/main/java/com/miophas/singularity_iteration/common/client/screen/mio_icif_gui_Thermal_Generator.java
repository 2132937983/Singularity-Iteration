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
 *
 * <p>0.1.11.181: the vanilla 176 wide, merged (docs/gui_processing_machines.md, "合并布局"): the furnace drawn on the left
 * with the fuel slot as its feed door, the fire behind its window, the damper flap in the flue on top, the furnace's heat
 * under the grate, the cable down to the battery and the lumps burning and waiting beside it; the chart (radius 40, was 42)
 * on the right. The legend went (the tooltip names the pens).
 */
@OnlyIn(Dist.CLIENT)
@SuppressWarnings("null")
public class mio_icif_gui_Thermal_Generator extends mio_icif_screen<ThermalGeneratorMenu> {

    private static final int STRIP_Y = 145, READ_Y = 113, READ_H = 32;   // 0.1.11.121: the readouts under the drawing
    private static final int CX = 124, CY = 61, R = 40;
    /** 0.1.11.181: the furnace (its feed door is the fuel slot), the flue, the queue of lumps beside the battery. */
    private static final int BODY_X = 10, BODY_Y = 28, BODY_W = 50, BODY_H = 50, FLUE_X = 29, QUEUE_X = 38, QUEUE_Y = 90;
    private List<Component> cutLines = List.of();
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
        furnace(g, x, y, PlantMimic.time(partialTick));
        chart(g, x + CX, y + CY, partialTick);
        for (var slot : menu.slots) {
            if (slot.x < 0 || slot.x > imageWidth) continue;
            VanillaFrame.slot(g, x + slot.x - 1, y + slot.y - 1);
        }
        ReadoutPanel.begin();   // 0.1.11.121: the reading lines in the vanilla font
        readings();
        cutLines = ReadoutPanel.end(g, x + 7, y + READ_Y, 162, READ_H);
        strip(g, x, y + STRIP_Y);
    }

    // ---- the furnace (0.1.11.181) -------------------------------------------------------------------------------------

    private void furnace(GuiGraphics g, int x, int y, double time) {
        boolean banked = v(5) != 0, firing = v(0) != 0;
        double heat = v(1) / 1000.0;
        // the flue with the damper flap in it: across it closed (banked), along it open
        MachineArt.steel(g, x + FLUE_X, y + 16, 8, BODY_Y - 16);
        g.fill(x + FLUE_X + 2, y + 17, x + FLUE_X + 6, y + BODY_Y, MachineArt.WINDOW);
        if (banked) g.fill(x + FLUE_X + 1, y + 21, x + FLUE_X + 7, y + 23, 0xFF2A5D8A);
        else g.fill(x + FLUE_X + 3, y + 18, x + FLUE_X + 5, y + 26, 0xFF7A7F85);
        // the body: the feed door (the fuel slot), the fire behind its window, the grate, the heat under it
        MachineArt.iron(g, x + BODY_X, y + BODY_Y, BODY_W, BODY_H);
        MachineArt.sunk(g, x + 36, y + 33, 20, 17, MachineArt.WINDOW);
        if (heat > 0.01) g.fill(x + 37, y + 34, x + 55, y + 49, FcsDraw.argb(0xD9792B, (float) Math.min(0.6, heat * 0.6)));
        if (firing && !banked) MachineArt.flames(g, x + 38, x + 54, y + 48, time, 3, 8);
        else if (heat > 0.01) g.fill(x + 38, y + 46, x + 54, y + 48, 0xFF8A3B22);
        g.fill(x + BODY_X + 4, y + 54, x + BODY_X + BODY_W - 4, y + 56, MachineArt.IRON_DARK);
        MachineArt.sunk(g, x + BODY_X + 4, y + 62, BODY_W - 8, 6, MachineArt.GLASS);
        int hw = (int) Math.round((BODY_W - 10) * Math.max(0, Math.min(1, heat)));
        if (hw > 0) g.fill(x + BODY_X + 5, y + 63, x + BODY_X + 5 + hw, y + 67, MachineArt.temperature(mio_icif_Thermal_Generator.kelvin(heat) - 273));
        // the cable down to the battery
        MachineArt.euCable(g, v(2) > 0, time, x + 23, y + BODY_Y + BODY_H, x + 23, y + ThermalGeneratorMenu.BATTERY_Y - 2);
        // the lump burning, then the lumps waiting in the slot (+n past two)
        int left = v(6), dur = Math.max(1, v(7)), count = v(8);
        for (int c = 0; c < 3; c++) {
            int bx = x + QUEUE_X + c * 12, by = y + QUEUE_Y;
            MachineArt.sunk(g, bx, by, 11, 10, 0xFFF4F6F4);
            double fill = c == 0 ? Math.min(1, left / (double) dur) : (c <= count ? 1 : 0);
            if (fill > 0) {
                int top = (int) Math.round(by + 1 + 8 * (1 - fill));
                g.fill(bx + 1, top, bx + 10, by + 9, c == 0 ? (banked ? 0xFF8A3B22 : 0xFFD9792B) : 0xFF3B3F42);
            }
        }
        if (count > 2) PumpCrossSection.text(g, Component.literal("+" + (count - 2)), x + QUEUE_X + 37, y + QUEUE_Y + 1, WindRoseView.DIM, 1F);
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

    private void readings() {
        int left = v(6), count = v(8);
        boolean banked = v(5) != 0, firing = v(0) != 0;
        double heat = v(1) / 1000.0;
        Component state;
        int stateColour;
        if (banked) { state = Component.translatable(K + "state.banked"); stateColour = BLUE; }
        else if (firing && heat < 0.995) { state = Component.translatable(K + "state.warming", Math.round(heat * 100) + "%"); stateColour = AMBER; }
        else if (firing) { state = Component.translatable(K + "state.full"); stateColour = GREEN; }
        else if (heat > 0.005) { state = Component.translatable(K + "state.cooling"); stateColour = WindRoseView.DIM; }
        else { state = Component.translatable(K + "state.cold"); stateColour = WindRoseView.DIM; }
        Component damper = Component.translatable(K + (banked ? "damper_closed" : "damper_open")).append(" · ").append(state);
        // 0.1.11.145: the output leads, the damper and state beside it
        ReadoutPanel.headline(Component.literal(String.format(Locale.ROOT, "%.1f EU/t", v(2) / 100.0)), damper, stateColour);
        ReadoutPanel.add(0, Component.translatable(K + "temp_line", mio_icif_Thermal_Generator.kelvin(heat),
            String.format(Locale.ROOT, "%.1f", v(2) / 100.0)), WindRoseView.INK);
        int per = v(9);
        double full = v(11) / 100.0;
        if (per > 0) ReadoutPanel.add(1, Component.translatable(K + "per_item", Math.round(per * full), per), WindRoseView.DIM);
        double ticks = Math.max(0, left) + (double) per * count;
        if (ticks > 0) {
            long s = Math.round(ticks / 20);
            ReadoutPanel.add(2, Component.translatable(K + "fuel_line", Math.round(ticks * full)).append(" · ")
                .append(Component.translatable(K + "time_line", s / 60, String.format(Locale.ROOT, "%02d", s % 60))), WindRoseView.DIM);
        } else {
            ReadoutPanel.add(2, Component.translatable(K + "no_fuel"), WindRoseView.DIM);
        }
    }

    private void strip(GuiGraphics g, int x, int sy) {
        // 0.1.11.121: vanilla font, furnace-style bar; the damper reopens at 85 % (blue mark)
        long e = Integer.toUnsignedLong(v(3)), cap = Math.max(1, Integer.toUnsignedLong(v(4)));
        int[] bar = VanillaFrame.labelledBar(g, x + 8, sy, 160, Component.translatable(K + "stored"), Math.min(1.0, e / (double) cap),
            Component.literal(e + " / " + cap + " EU"));
        int mark = (int) (bar[0] + bar[1] * mio_icif_Thermal_Generator.REOPEN);
        if (bar[1] > 0) g.fill(mark, sy + 1, mark + 1, sy + 10, 0xFF2A5D8A);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        int rx = mouseX - leftPos, ry = mouseY - topPos;
        if (hoveredSlot != null) return;   // 0.1.11.181: a slot shows its own tooltip only
        // EXPLAIN_TIP_HIDDEN (0.1.11.182): the chart's, the damper's, the fuel queue's and the bar mark's how-it-works texts are hidden for now
        if (Math.hypot(rx - CX, ry - CY) <= R + 2) {
            // g.renderComponentTooltip(this.font, List.of(Component.translatable(K + "tip.chart1"), Component.translatable(K + "tip.chart2"),
            //     Component.translatable(K + "tip.chart3")), mouseX, mouseY);
        } else if (rx >= FLUE_X && rx < FLUE_X + 8 && ry >= 16 && ry < BODY_Y) {
            // g.renderComponentTooltip(this.font, List.of(Component.translatable(K + "tip.damper1"), Component.translatable(K + "tip.damper2")), mouseX, mouseY);
        } else if (rx >= BODY_X && rx < QUEUE_X + 50 && ry >= BODY_Y && ry < QUEUE_Y + 10) {
            // g.renderComponentTooltip(this.font, List.of(Component.translatable(K + "tip.fuel1"), Component.translatable(K + "tip.fuel2")), mouseX, mouseY);
        } else if (!cutLines.isEmpty() && rx >= 8 && rx < 168 && ry >= READ_Y && ry < READ_Y + READ_H) {
            g.renderComponentTooltip(this.font, cutLines, mouseX, mouseY);
        } else if (rx >= 8 && rx < 168 && ry >= STRIP_Y && ry < STRIP_Y + 12) {
            // g.renderComponentTooltip(this.font, List.of(Component.translatable(K + "tip.damper2")), mouseX, mouseY);
        }
    }

    /** 0.1.11.144: the calm chrome (title on the left, no lamp or upgrade hint). */
    @Override
    protected boolean calmChrome() { return true; }
}
