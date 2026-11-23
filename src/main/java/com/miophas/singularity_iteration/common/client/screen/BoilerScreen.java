// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.client.screen;

import com.miophas.singularity_iteration.common.client.suit.FcsDraw;
import com.miophas.singularity_iteration.common.menu.producer.SteamGeneratorMenu;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/**
 * 0.1.11.16: the boiler's mimic: feed water from its tank into the drum, the heat under it, the steam leaving at the top
 * (ordinary steam below the critical point, 374 °C; superheated above it), a temperature gauge with the critical point
 * and the 500 °C limit (it explodes above), and the scale (calcification).
 *
 * <p>0.1.11.181 (176 wide, merged): the boiler drawn on the panel: the feed water tank on the left, its pipe into the drum,
 * the fire under the drum, the steam pipe out to the thermometer and the scale gauge. The two columns of step buttons became
 * two setters in the drawing, the pressure valve on the drum and the feed under the tank: [−] value [+], a click a step of
 * 1, with Shift 10, with Ctrl 100, with both 1,000 (the feed), or the mouse wheel over it (the same button events as before).
 */
@OnlyIn(Dist.CLIENT)
@SuppressWarnings("null")
public class BoilerScreen extends MimicScreen<SteamGeneratorMenu> {
    private static final String K = "gui.mio_icif.steam.";
    /** The setters: [−] at (x, y), the display, [+]; each 10 high. */
    static final int VALVE_X = 55, VALVE_Y = 21, FEED_X = 8, FEED_Y = 66, BUTTON = 10, DISPLAY = 27, SETTER_W = 2 * BUTTON + DISPLAY + 2;
    private static final int TANK_X = 12, TANK_Y = 19, TANK_H = 40, DRUM_X = 50, DRUM_Y = 18, DRUM_W = 56, DRUM_H = 38,
        GAUGE_X = 132, SCALE_X = 161, GAUGE_Y = 19, GAUGE_H = 42;

    public BoilerScreen(SteamGeneratorMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        wx = TANK_X - 2;
        ww = SCALE_X + 6 - wx;
    }

    // ---- the setters --------------------------------------------------------------------------------------------------

    /** The step of a click by the modifier keys: 1, 10 (Shift), 100 (Ctrl), 1,000 (both; the valve stops at 100). */
    private static int step(boolean valve) {
        boolean shift = Screen.hasShiftDown(), ctrl = Screen.hasControlDown();
        int step = shift && ctrl ? 1000 : ctrl ? 100 : shift ? 10 : 1;
        return valve ? Math.min(100, step) : step;
    }

    /** The button event of a step (the events of the old buttons: the valve 2000 ± step, the feed ± step). */
    private static int event(boolean valve, int step, boolean up) {
        return valve ? (up ? 2000 + step : -2000 - step) : (up ? step : -step);
    }

    /** Which part of a setter at (sx, sy) the mouse is on: -1 the [−], 1 the [+], 0 the display, 2 none. */
    private static int part(double rx, double ry, int sx, int sy) {
        if (ry < sy || ry >= sy + BUTTON || rx < sx || rx >= sx + SETTER_W) return 2;
        if (rx < sx + BUTTON) return -1;
        if (rx >= sx + BUTTON + DISPLAY + 2) return 1;
        return 0;
    }

    private void send(int event) {
        if (minecraft != null && minecraft.gameMode != null) minecraft.gameMode.handleInventoryButtonClick(menu.containerId, event);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            double rx = mouseX - leftPos, ry = mouseY - topPos;
            for (boolean valve : new boolean[]{true, false}) {
                int p = part(rx, ry, valve ? VALVE_X : FEED_X, valve ? VALVE_Y : FEED_Y);
                if (p == -1 || p == 1) {
                    send(event(valve, step(valve), p == 1));
                    return true;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        double rx = mouseX - leftPos, ry = mouseY - topPos;
        for (boolean valve : new boolean[]{true, false})
            if (scrollY != 0 && part(rx, ry, valve ? VALVE_X : FEED_X, valve ? VALVE_Y : FEED_Y) != 2) {
                send(event(valve, step(valve), scrollY > 0));
                return true;
            }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    /** A setter: [−], the value on a dark display, [+] (vanilla buttons, lit while the mouse is on them). */
    private void setter(GuiGraphics g, int x, int y, int sx, int sy, int value) {
        int hover = lastMouse == null ? 2 : part(lastMouse[0] - x, lastMouse[1] - y, sx, sy);
        button(g, x + sx, y + sy, false, hover == -1);
        MachineArt.sunk(g, x + sx + BUTTON + 1, y + sy, DISPLAY, BUTTON, MachineArt.WINDOW);
        Component c = Component.literal(String.valueOf(value));
        label(g, c, x + sx + BUTTON + 1 + (DISPLAY - font.width(c)) / 2F, y + sy + 1, MachineArt.LCD_TEXT & 0xFFFFFF);
        button(g, x + sx + BUTTON + DISPLAY + 2, y + sy, true, hover == 1);
    }

    private static void button(GuiGraphics g, int bx, int by, boolean plus, boolean lit) {
        MachineArt.raised(g, bx, by, BUTTON, BUTTON, lit ? 0xFFA8A8A8 : VanillaFrame.SLOT, VanillaFrame.LIGHT, VanillaFrame.SHADOW);
        FcsDraw.fill(g, bx + 3, by + 4, bx + 7, by + 6, 0xFF202020);
        if (plus) FcsDraw.fill(g, bx + 4, by + 3, bx + 6, by + 7, 0xFF202020);
    }

    private int[] lastMouse;

    // ---- the boiler -----------------------------------------------------------------------------------------------------

    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        lastMouse = new int[]{mouseX, mouseY};
        super.renderBg(g, partialTick, mouseX, mouseY);
    }

    @Override
    protected void drawMachine(GuiGraphics g, int x, int y, double time) {
        float heat = menu.getSystemHeatFloat();
        String outKey = menu.getOutputFluidTranslationKey();
        boolean superheated = outKey.endsWith("hotsteam"), water = outKey.endsWith("water");
        Component outName = Component.translatable(outKey.isEmpty() ? K + "no_output" : outKey);
        double feed = menu.getInputMB() > 0 && menu.getOutputMB() > 0 ? Math.min(1.5, menu.getInputMB() / 400.0) : 0;
        double share = menu.getWaterCapacity() > 0 ? menu.getWaterAmount() / (double) menu.getWaterCapacity() : 0;
        // the feed water tank and its pipe into the drum
        MachineArt.level(g, x + TANK_X, y + TANK_Y, 8, TANK_H, share, MachineArt.WATER);
        MachineArt.pipe(g, x + TANK_X + 10, x + DRUM_X, y + 50, feed > 0 ? MachineArt.WATER : 0, MachineArt.phase(time, feed));
        // the drum: the valve on top, the water behind the window, bubbling while it boils
        MachineArt.iron(g, x + DRUM_X, y + DRUM_Y, DRUM_W, DRUM_H);
        MachineArt.sunk(g, x + DRUM_X + 3, y + 33, DRUM_W - 6, 20, MachineArt.WINDOW);
        FcsDraw.fill(g, x + DRUM_X + 4, y + 42, x + DRUM_X + DRUM_W - 4, y + 52, MachineArt.WATER);
        if (menu.getOutputMB() > 0)
            for (int k = 0; k < 6; k++) {
                int by = 50 - (int) Math.floorMod((long) Math.floor(time / 2 + k * 3), 8L);
                FcsDraw.fill(g, x + DRUM_X + 8 + k * 8, y + by, x + DRUM_X + 9 + k * 8, y + by + 1, MachineArt.WATER_LIGHT);
            }
        setter(g, x, y, VALVE_X, VALVE_Y, menu.getPressure());
        // the fire under it while heat comes in
        MachineArt.iron(g, x + DRUM_X + 4, y + 57, DRUM_W - 8, 8);
        MachineArt.sunk(g, x + DRUM_X + 6, y + 59, DRUM_W - 12, 5, MachineArt.WINDOW);
        if (menu.getLastHeatInput() > 0) MachineArt.flames(g, x + DRUM_X + 7, x + DRUM_X + DRUM_W - 7, y + 63, time, 6, 3);
        // the steam out, the temperature gauge (the critical point and the limit) and the scale
        int steam = water ? MachineArt.WATER : MachineArt.temperature(superheated ? 401 : Math.max(100, heat));
        double out = menu.getOutputMB() > 0 ? Math.min(1.5, menu.getOutputMB() / 40000.0) : 0;
        MachineArt.pipe(g, x + DRUM_X + DRUM_W, x + GAUGE_X - 4, y + 24, menu.getOutputMB() > 0 ? steam : 0, MachineArt.phase(time, out));
        MachineArt.thermometer(g, x + GAUGE_X, y + GAUGE_Y, GAUGE_H, heat, 500);
        MachineArt.mark(g, x + GAUGE_X, y + GAUGE_Y, 4, GAUGE_H, 374, 500, 0xFF2A5D8A);
        MachineArt.mark(g, x + GAUGE_X, y + GAUGE_Y, 4, GAUGE_H, 500, 500, MachineArt.RED);
        label(g, Component.literal("500"), x + GAUGE_X + 8, y + GAUGE_Y - 2, 0xC0392B);
        label(g, Component.literal("374"), x + GAUGE_X + 8, y + GAUGE_Y + 8, 0x2A5D8A);
        float calc = menu.getMaxCalcification() > 0 ? (float) menu.getCalcification() / menu.getMaxCalcification() : 0;
        MachineArt.level(g, x + SCALE_X, y + GAUGE_Y, 4, GAUGE_H, calc, MachineArt.SCALE);
        setter(g, x, y, FEED_X, FEED_Y, menu.getInputMB());
        label(g, Component.literal("mB/t"), x + FEED_X + SETTER_W + 3, y + FEED_Y + 1, VanillaFrame.TITLE);

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
        // 0.1.11.145: the steam made is the main readout; the state beside it
        ReadoutPanel.headline(Component.literal(f("%,d mB/t", menu.getOutputMB())), Component.translatable(K + state, Math.round(calc * 100) + "%"), colour);
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
        if (part(rx, ry, VALVE_X, VALVE_Y) != 2 || part(rx, ry, FEED_X, FEED_Y) != 2)
            g.renderComponentTooltip(this.font, List.of(Component.translatable(K + "tip.controls"), Component.translatable(K + "tip.steps")), mouseX, mouseY);
        // EXPLAIN_TIP_HIDDEN (0.1.11.182): the boiler's how-it-works text, hidden for now (the -/+ controls keep theirs)
        // else if (overMachine(rx, ry))
        //     g.renderComponentTooltip(this.font, List.of(Component.translatable(K + "tip.boiler1"), Component.translatable(K + "tip.boiler2"),
        //         Component.translatable(K + "tip.boiler3")), mouseX, mouseY);
    }
}
