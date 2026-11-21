package com.miophas.singularity_iteration.common.client.screen;

import com.miophas.singularity_iteration.common.client.suit.AsciiHud;
import com.miophas.singularity_iteration.common.client.suit.FcsDraw;
import com.miophas.singularity_iteration.common.energy.EngineGovernor;
import com.miophas.singularity_iteration.common.menu.generator.EngineMenu;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/**
 * 0.1.11.15: the engine screen (semifluid, advanced semifluid and diesel generators), 248 x 204, an engine room monitor:
 * an exhaust gas temperature bar per cylinder, the tachometer, the coolant and the fuel tank, the readings beside them; a
 * record strip of the load (blue) and exhaust temperature (rust) over the last ~48 s; the output, fuel use, efficiency and
 * state below; the stored energy under the window with the restart (60 %) and stop (98 %) marks.
 */
@OnlyIn(Dist.CLIENT)
@SuppressWarnings("null")
public class EngineScreen<T extends EngineMenu> extends mio_icif_screen<T> {

    private static final int WX = 34, WY = 15, WW = 207, WH = 66, STRIP_Y = 145;   // 0.1.11.121: the readouts under the window
    private static final int RUST = 0xFFB4471F, HOT = 0xFFC0392B, BLUE = 0xFF2A5D8A, GREEN = 0xFF2F6E4F, WELL = 0xFFE3E6E4, GRID = 0xFFD9DED9;
    private static final String K = "gui.mio_icif.engine.";

    public EngineScreen(T menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = EngineMenu.GUI_WIDTH;
        this.imageHeight = EngineMenu.GUI_HEIGHT;
    }

    private int v(int i) { return menu.value(i); }

    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        int x = leftPos, y = topPos;
        VanillaFrame.panel(g, x, y, imageWidth, imageHeight);
        VanillaFrame.slot(g, x + EngineMenu.SLOT_X - 1, y + EngineMenu.FUEL_Y - 1);
        VanillaFrame.slot(g, x + EngineMenu.SLOT_X - 1, y + EngineMenu.EMPTY_Y - 1);
        VanillaFrame.slot(g, x + EngineMenu.SLOT_X - 1, y + EngineMenu.BATTERY_Y - 1);
        for (int r = 0; r < 3; r++) for (int c = 0; c < 9; c++)
            VanillaFrame.slot(g, x + EngineMenu.INVENTORY_X - 1 + c * 18, y + EngineMenu.INVENTORY_Y - 1 + r * 18);
        for (int c = 0; c < 9; c++) VanillaFrame.slot(g, x + EngineMenu.INVENTORY_X - 1 + c * 18, y + EngineMenu.HOTBAR_Y - 1);
        VanillaFrame.inset(g, x + WX - 1, y + WY - 1, WW + 2, WH + 2, WindRoseView.PAPER);   // 0.1.11.119: the vanilla look
        gauges(g, x + WX, y + WY, partialTick);
        record(g, x + WX + 4, y + WY + 47, WW - 8, 16);
        ReadoutPanel.begin();   // 0.1.11.121: the reading lines in the readout window, vanilla font
        readings(g, x + WX + 4, y + WY + 66);
        ReadoutPanel.end(g, x + WX - 1, y + WY + WH + 3, WW + 2, 58);
        strip(g, x, y + STRIP_Y);
    }

    private boolean firing() { return v(0) >= EngineGovernor.WARMING; }

    // ---- gauges ------------------------------------------------------------------------------------------------------

    private void gauges(GuiGraphics g, int wx, int wy, float partialTick) {
        float u = WindRoseView.u();
        double load = v(1) / 1000.0, coolant = v(3) / 10.0;
        int cylinders = Math.max(1, v(14));
        // exhaust gas temperature per cylinder
        PumpCrossSection.text(g, Component.translatable(K + "egt"), wx + 4, wy + 2, WindRoseView.DIM, u);
        long time = minecraft != null && minecraft.level != null ? minecraft.level.getGameTime() : 0;
        for (int c = 0; c < cylinders; c++) {
            double t = EngineGovernor.exhaust(load, coolant, firing()) + (firing() ? 12 * Math.sin(c * 1.7 + (time + partialTick) / 8.0) : 0);
            float bx = wx + 5 + c * 9, top = wy + 12, h = 30;
            g.fill((int) bx, (int) top, (int) bx + 6, (int) (top + h), WELL);
            float f = (float) Math.max(0, Math.min(1, t / 600.0));
            WindRoseView.fill(g, bx, top + h * (1 - f), 6, h * f, t > 520 ? HOT : RUST);
        }
        // tachometer: 0..2000 rpm over 270°
        float cx = wx + 86, cy = wy + 27, r = 15;
        for (int k = 0; k <= 10; k++) {
            double a = Math.toRadians(135 + 27 * k);
            float c1 = (float) Math.cos(a), s1 = (float) Math.sin(a);
            FcsDraw.line(g, cx + c1 * (r - 3), cy + s1 * (r - 3), cx + c1 * r, cy + s1 * r, 0.6F, k >= 9 ? HOT : 0xFF5B6867);
        }
        FcsDraw.circle(g, cx, cy, r, 0.5F, 0xFF9AA3A1);
        double a = Math.toRadians(135 + 270 * Math.min(1, v(2) / 2000.0));
        FcsDraw.line(g, cx, cy, cx + (float) Math.cos(a) * (r - 2), cy + (float) Math.sin(a) * (r - 2), 1.0F, HOT);
        FcsDraw.disc(g, cx, cy, 1.5F, 0xFF33393A);
        // coolant
        float kx = wx + 108, ky = wy + 12, kh = 30;
        g.fill((int) kx, (int) ky, (int) kx + 5, (int) (ky + kh), WELL);
        float cf = (float) Math.max(0, Math.min(1, coolant / 110.0));
        WindRoseView.fill(g, kx, ky + kh * (1 - cf), 5, kh * cf, coolant < 70 ? BLUE : GREEN);
        // fuel tank
        float fx = wx + 118, fh = 30;
        g.fill((int) fx, (int) ky, (int) fx + 8, (int) (ky + fh), WELL);
        float ff = v(10) > 0 ? (float) Math.min(1, v(9) / (double) v(10)) : 0;
        WindRoseView.fill(g, fx, ky + fh * (1 - ff), 8, fh * ff, fuelColour());
        PumpCrossSection.text(g, Component.translatable(K + "tank_labels"), wx + 104, wy + 2, WindRoseView.DIM, u);
        // readings beside the gauges
        float rx = wx + 132, lh = 9 * u + 1;
        WindRoseView.line(g, Component.literal(String.format(Locale.ROOT, "%d rpm", v(2))), rx, wy + 4, WindRoseView.INK, 72);
        WindRoseView.line(g, Component.translatable(K + "load", Math.round(load * 100) + "%"), rx, wy + 4 + lh, WindRoseView.INK, 72);
        WindRoseView.line(g, Component.translatable(K + "egt_value", Math.round(EngineGovernor.exhaust(load, coolant, firing()))), rx, wy + 4 + 2 * lh, RUST & 0xFFFFFF, 72);
        WindRoseView.line(g, Component.translatable(K + "coolant", String.format(Locale.ROOT, "%.0f", coolant)), rx, wy + 4 + 3 * lh,
            (coolant < 70 ? BLUE : GREEN) & 0xFFFFFF, 72);
    }

    private int fuelColour() {
        if (v(11) < 0) return 0xFF9AA3A1;
        var fluid = BuiltInRegistries.FLUID.byId(v(11));
        String path = BuiltInRegistries.FLUID.getKey(fluid).getPath();
        if (path.contains("diesel")) return 0xFFC9A227;
        if (path.contains("biogas")) return 0xFF7A9A3A;
        if (path.contains("crude")) return 0xFF3B2E25;
        if (path.contains("biomass")) return 0xFF5E7A2E;
        return 0xFF8A8F94;
    }

    // ---- the record strip --------------------------------------------------------------------------------------------

    private void record(GuiGraphics g, float x, float y, float w, float h) {
        g.fill((int) x, (int) y, (int) (x + w), (int) (y + h), 0xFFFBFBF6);
        for (int k = 1; k < 4; k++) FcsDraw.line(g, x, y + h * k / 4, x + w, y + h * k / 4, 0.3F, GRID);
        int n = EngineGovernor.SAMPLES, head = v(15);
        int[] load = new int[n], egt = new int[n];
        for (int i = 0; i < n / 2; i++) {
            int word = v(16 + i);
            load[2 * i] = word & 0xFF; egt[2 * i] = (word >>> 8) & 0xFF; load[2 * i + 1] = (word >>> 16) & 0xFF; egt[2 * i + 1] = (word >>> 24) & 0xFF;
        }
        float[] pl = null, pe = null;
        for (int k = 0; k < n; k++) {
            int i = (head + k) % n;
            if (load[i] == 0) { pl = pe = null; continue; }
            float px = x + w * k / (n - 1);
            float[] ql = {px, y + h - h * (load[i] - 1) / 254F}, qe = {px, y + h - h * (egt[i] - 1) / 254F};
            if (pl != null) {
                FcsDraw.line(g, pe[0], pe[1], qe[0], qe[1], 0.7F, RUST);
                FcsDraw.line(g, pl[0], pl[1], ql[0], ql[1], 0.7F, BLUE);
            }
            pl = ql; pe = qe;
        }
    }

    // ---- readings ----------------------------------------------------------------------------------------------------

    private static String f1(double v) { return String.format(Locale.ROOT, "%.1f", v); }

    private void readings(GuiGraphics g, float tx, float ty) {
        float u = WindRoseView.u(), lh = 9 * u, w = WW - 8;
        WindRoseView.line(g, Component.translatable(K + "out_line", f1(v(4) / 100.0), f1(v(5) / 100.0), String.format(Locale.ROOT, "%.2f", v(6) / 1000.0)),
            tx, ty, WindRoseView.INK, w);
        Component fuel = v(11) < 0 ? Component.translatable(K + "no_fuel")
            : BuiltInRegistries.FLUID.byId(v(11)).getFluidType().getDescription();
        boolean stopped = v(0) <= EngineGovernor.STOPPED_FUEL;   // no efficiency to show while stopped
        WindRoseView.line(g, Component.translatable(K + "fuel_line", stopped ? "—" : Math.round(v(7) / 10.0) + "%", stopped ? "—" : f1(v(8) / 10.0))
            .append(" · ").append(fuel).append(String.format(Locale.ROOT, " %,d / %,d mB", v(9), v(10))), tx, ty + lh, WindRoseView.DIM, w);
        String state; int colour;
        switch (v(0)) {
            case EngineGovernor.STOPPED_FULL -> { state = "state.stopped_full"; colour = BLUE; }
            case EngineGovernor.STOPPED_FUEL -> { state = "state.stopped_fuel"; colour = WindRoseView.DIM; }
            case EngineGovernor.WARMING -> { state = "state.warming"; colour = 0xA0602A; }
            case EngineGovernor.LOW_LOAD -> { state = "state.low_load"; colour = 0xA0602A; }
            default -> { state = "state.running"; colour = GREEN; }
        }
        WindRoseView.line(g, Component.translatable(K + state), tx, ty + 2 * lh, colour & 0xFFFFFF, w);
    }

    private void strip(GuiGraphics g, int x, int sy) {
        // 0.1.11.121: vanilla font, furnace-style bar; the governor's start and stop marks in blue
        long e = Integer.toUnsignedLong(v(12)), cap = Math.max(1, Integer.toUnsignedLong(v(13)));
        int[] bar = VanillaFrame.labelledBar(g, x + WX, sy, 241 - WX, Component.translatable(K + "stored"), Math.min(1.0, e / (double) cap),
            Component.literal(String.format(Locale.ROOT, "%,d / %,d EU", e, cap)));
        for (double mark : new double[]{EngineGovernor.START_BELOW, EngineGovernor.STOP_AT}) {
            int m = (int) (bar[0] + bar[1] * mark);
            if (bar[1] > 0) g.fill(m, sy + 1, m + 1, sy + 10, BLUE);
        }
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        int rx = mouseX - leftPos, ry = mouseY - topPos;
        if (rx >= WX && rx < WX + WW && ry >= WY && ry < WY + 46) {
            g.renderComponentTooltip(this.font, List.of(Component.translatable(K + "tip.1"), Component.translatable(K + "tip.2")), mouseX, mouseY);
        } else if (rx >= WX && rx < WX + WW && ry >= WY + 46 && ry < WY + 64) {
            g.renderComponentTooltip(this.font, List.of(Component.translatable(K + "tip.3")), mouseX, mouseY);
        } else if (rx >= WX && rx < WX + WW && ry >= WY + 64 && ry < WY + WH) {
            g.renderComponentTooltip(this.font, List.of(Component.translatable(K + "tip.4"), Component.translatable(K + "tip.5")), mouseX, mouseY);
        } else if (rx >= WX && rx < 241 && ry >= STRIP_Y && ry < STRIP_Y + 12) {
            g.renderComponentTooltip(this.font, List.of(Component.translatable(K + "tip.6")), mouseX, mouseY);
        }
    }
}
