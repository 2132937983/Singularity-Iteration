// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.client.screen;

import com.miophas.singularity_iteration.common.client.suit.AsciiHud;
import com.miophas.singularity_iteration.common.client.suit.FcsDraw;
import com.miophas.singularity_iteration.common.menu.producer.SteamGeneratorMenu;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/**
 * 0.1.11.16: the boiler's mimic: feed water from its tank into the drum, the heat under it, the steam leaving at the top
 * (ordinary steam below the critical point, 374 °C; superheated above it), a temperature gauge with the critical point
 * and the 500 °C limit (it explodes above), and the scale (calcification). On the left two columns of buttons set the pressure and the
 * feed rate (the same button events as before).
 */
@OnlyIn(Dist.CLIENT)
@SuppressWarnings("null")
public class BoilerScreen extends MimicScreen<SteamGeneratorMenu> {
    private static final String K = "gui.mio_icif.steam.";
    private static final int[] PRESSURE = {100, 10, 1}, FLOW = {1000, 100, 10, 1};
    private static final int COL_P = 4, COL_F = 32, BTN_W = 26, BTN_H = 8;

    public BoilerScreen(SteamGeneratorMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        wx = 64;
        ww = 177;
    }

    // ---- the controls -------------------------------------------------------------------------------------------------

    /** Rows of a column: the increases, the value, the decreases; returns the event of row {@code r}, 0 for the value. */
    private static int pressureEvent(int r) {
        if (r < 3) return 2000 + PRESSURE[r];
        if (r == 3) return 0;
        return -2000 - PRESSURE[6 - r];
    }

    private static int flowEvent(int r) {
        if (r < 4) return FLOW[r];
        if (r == 4) return 0;
        return -FLOW[8 - r];
    }

    private static int rowY(int r, boolean flow) { return 26 + r * (BTN_H + (flow ? 1 : 3)); }

    private void controls(GuiGraphics g, int x, int y) {
        float u = WindRoseView.u();
        PlantMimic.label(g, Component.translatable(K + "pressure"), x + COL_P + BTN_W / 2F, y + 16, WindRoseView.DIM);
        PlantMimic.label(g, Component.translatable(K + "feed"), x + COL_F + BTN_W / 2F, y + 16, WindRoseView.DIM);
        for (int r = 0; r < 7; r++) button(g, x + COL_P, y + rowY(r, false), pressureEvent(r), r == 3 ? f("%d", menu.getPressure()) : null, u);
        for (int r = 0; r < 9; r++) button(g, x + COL_F, y + rowY(r, true), flowEvent(r), r == 4 ? f("%d", menu.getInputMB()) : null, u);
    }

    private void button(GuiGraphics g, int bx, int by, int event, String value, float u) {
        Component c;
        if (value != null) {
            FcsDraw.fill(g, bx, by, bx + BTN_W, by + BTN_H, 0xFF2B3036);
            c = Component.literal(value);
            PumpCrossSection.text(g, c, bx + (BTN_W - AsciiHud.width(c, u)) / 2, by + (BTN_H - 8 * u) / 2, 0xE6E9EC, u);
            return;
        }
        int delta = Math.abs(event) >= 2000 ? Math.abs(event) - 2000 : Math.abs(event);
        boolean up = event > 0;
        FcsDraw.fill(g, bx, by, bx + BTN_W, by + BTN_H, up ? 0xFF4F7A5E : 0xFF8A5A4A);
        FcsDraw.fill(g, bx, by, bx + BTN_W, by + 1, up ? 0xFF6E9B7E : 0xFFA9786A);
        c = Component.literal((up ? "+" : "−") + delta);
        PumpCrossSection.text(g, c, bx + (BTN_W - AsciiHud.width(c, u)) / 2, by + (BTN_H - 8 * u) / 2, 0xFFFFFF, u);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && minecraft != null && minecraft.gameMode != null) {
            double rx = mouseX - leftPos, ry = mouseY - topPos;
            int event = 0;
            for (int r = 0; r < 7 && event == 0; r++)
                if (inside(rx, ry, COL_P, rowY(r, false))) event = pressureEvent(r);
            for (int r = 0; r < 9 && event == 0; r++)
                if (inside(rx, ry, COL_F, rowY(r, true))) event = flowEvent(r);
            if (event != 0) {
                minecraft.gameMode.handleInventoryButtonClick(menu.containerId, event);
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private static boolean inside(double rx, double ry, int bx, int by) {
        return rx >= bx && rx < bx + BTN_W && ry >= by && ry < by + BTN_H;
    }

    // ---- the mimic ------------------------------------------------------------------------------------------------------

    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        super.renderBg(g, partialTick, mouseX, mouseY);
        controls(g, leftPos, topPos);
    }

    @Override
    protected void drawWindow(GuiGraphics g, int x, int y, double time) {
        float u = WindRoseView.u();
        float heat = menu.getSystemHeatFloat();
        String outKey = menu.getOutputFluidTranslationKey();
        boolean superheated = outKey.endsWith("hotsteam"), water = outKey.endsWith("water");
        Component outName = Component.translatable(outKey.isEmpty() ? K + "no_output" : outKey);
        double feed = menu.getInputMB() > 0 && menu.getOutputMB() > 0 ? Math.min(1.5, menu.getInputMB() / 400.0) : 0;
        // feed water
        PlantMimic.tank(g, x + 5, y + 8, 8, 44, menu.getWaterCapacity() > 0 ? menu.getWaterAmount() / (double) menu.getWaterCapacity() : 0, PlantMimic.WATER);
        PlantMimic.pipe(g, new float[]{x + 13, y + 44, x + 30, y + 44}, PlantMimic.WATER, 3, feed, time);
        // drum, water inside, fire under it
        PlantMimic.box(g, x + 30, y + 10, 50, 40, 0xFFE6E9EA);
        WindRoseView.fill(g, x + 30, y + 32, 50, 18, 0xC04F7FB0);
        int heatIn = menu.getLastHeatInput();
        for (int k = 0; k < 5 && heatIn > 0; k++) {
            float fx = x + 34 + k * 10, fh = 3 + (float) (2 * Math.sin(time / 3 + k)) + Math.min(4, heatIn / 300F);
            FcsDraw.quad(g, fx, y + 56, fx + 3, y + 56 - fh, fx + 6, y + 56, fx + 3, y + 56, 0xFFD9792B);
        }
        PlantMimic.label(g, Component.literal(f("%d bar", menu.getPressure())), x + 55, y + 14, 0x1C2524);
        PlantMimic.label(g, Component.literal(f("%.1f °C", heat)), x + 55, y + 23, PlantMimic.colour(heat) & 0xFFFFFF);
        // steam out
        int steamColour = water ? PlantMimic.WATER : PlantMimic.colour(superheated ? 401 : Math.max(100, heat));
        double out = menu.getOutputMB() > 0 ? Math.min(1.5, menu.getOutputMB() / 40000.0) : 0;
        PlantMimic.pipe(g, new float[]{x + 80, y + 16, x + 130, y + 16}, steamColour, 3, out, time);
        PlantMimic.label(g, outName, x + 105, y + 20, WindRoseView.DIM);
        // temperature gauge: 0..500 °C, the critical point and the limit
        float gx = x + 140, gy = y + 8, gh = 44;
        FcsDraw.fill(g, (int) gx - 1, (int) gy - 1, (int) gx + 7, (int) (gy + gh) + 1, PlantMimic.OUTLINE);
        FcsDraw.fill(g, (int) gx, (int) gy, (int) gx + 6, (int) (gy + gh), 0xFFF4F6F4);
        float tf = Math.max(0, Math.min(1, heat / 500F));
        WindRoseView.fill(g, gx, gy + gh * (1 - tf), 6, gh * tf, PlantMimic.colour(heat));
        float crit = gy + gh * (1 - 374 / 500F);
        FcsDraw.fill(g, (int) gx - 2, (int) crit, (int) gx + 8, (int) crit + 1, 0xFF2A5D8A);
        PumpCrossSection.text(g, Component.literal("374"), gx + 9, crit - 4 * u, 0x2A5D8A, u);
        PumpCrossSection.text(g, Component.literal("500"), gx + 9, gy - 4 * u, 0xC0392B, u);
        // scale
        float calc = menu.getMaxCalcification() > 0 ? (float) menu.getCalcification() / menu.getMaxCalcification() : 0;
        PlantMimic.tank(g, x + 160, y + 8, 5, 44, calc, 0xFFC9B48A);

        line(g, x, y, 0, Component.translatable(K + "boiler_line", menu.getPressure(), f("%.1f", heat), menu.getLastHeatInput()), WindRoseView.INK);
        line(g, x, y, 1, Component.translatable(K + "boiler_flow", menu.getInputMB(), f("%,d", menu.getOutputMB()))
            .append(" · ").append(outName), WindRoseView.DIM);
        String state; int colour;
        if (calc >= 1) { state = "state.calcified"; colour = 0xC0392B; }
        else if (heat > 450) { state = "state.too_hot"; colour = 0xC0392B; }
        else if (water && menu.getOutputMB() > 0) { state = "state.passing"; colour = 0xA0602A; }
        else if (menu.getOutputMB() <= 0) { state = heat < 100 ? (menu.getLastHeatInput() > 0 ? "state.heating" : "state.cold") : "state.hot_idle"; colour = 0xA0602A; }
        else if (superheated) { state = "state.supercritical"; colour = 0x2F6E4F; }
        else { state = "state.boiling"; colour = 0x2F6E4F; }
        line(g, x, y, 2, Component.translatable(K + state, Math.round(calc * 100) + "%"), colour);
    }

    @Override
    protected void drawStrip(GuiGraphics g, int x, int y) {
        bar(g, x, y, Component.translatable(K + "water"), menu.getWaterCapacity() > 0 ? menu.getWaterAmount() / (double) menu.getWaterCapacity() : 0,
            Component.literal(f("%,d / %,d mB", menu.getWaterAmount(), menu.getWaterCapacity())));
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        int rx = mouseX - leftPos, ry = mouseY - topPos;
        if (rx >= wx && rx < wx + ww && ry >= wy && ry < wy + wh)
            g.renderComponentTooltip(this.font, List.of(Component.translatable(K + "tip.boiler1"), Component.translatable(K + "tip.boiler2"),
                Component.translatable(K + "tip.boiler3")), mouseX, mouseY);
        else if (rx >= COL_P && rx < COL_F + BTN_W && ry >= 16 && ry < 110)
            g.renderComponentTooltip(this.font, List.of(Component.translatable(K + "tip.controls")), mouseX, mouseY);
    }
}
