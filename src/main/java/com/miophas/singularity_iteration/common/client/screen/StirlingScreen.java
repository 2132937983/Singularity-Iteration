// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.client.screen;

import com.miophas.singularity_iteration.common.client.suit.FcsDraw;
import com.miophas.singularity_iteration.common.energy.StirlingPhysics;
import com.miophas.singularity_iteration.common.menu.generator.MimicGeneratorMenu;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/**
 * 0.1.11.17: the mimic of the Stirling generator and the advanced Stirling generator: heat in, the engine (heater head,
 * pistons, cooler), the flywheel, the generator and the EU out, with thermometers of the heater and the cooler. Values
 * as in {@link com.miophas.singularity_iteration.common.menu.generator.StirlingGeneratorMenu}.
 *
 * <p>0.1.11.181 (176 wide, merged): drawn on the panel; the engine is a pixel part ({@link #engine}).
 */
@OnlyIn(Dist.CLIENT)
@SuppressWarnings("null")
public class StirlingScreen<M extends MimicGeneratorMenu> extends MimicScreen<M> {
    static final String K = "gui.mio_icif.stirling.";

    public StirlingScreen(M menu, Inventory inv, Component title) { super(menu, inv, title); }

    private int v(int i) { return menu.value(i); }

    @Override
    protected void drawMachine(GuiGraphics g, int x, int y, double time) {
        double hot = v(6) / 10.0, cold = v(7) / 10.0, factor = v(8) / 1000.0, flow = v(9) / 10.0;
        boolean water = v(10) != 0;
        int eu = (int) Math.round(flow * v(13) / 1000.0);   // smoothed: the heat flow times the ratio
        double rpm = flow > 0.5 ? 1500 * Math.min(1.2, Math.sqrt(flow / Math.max(1, v(12)))) : 0;
        double angle = PlantMimic.angle(time, rpm);
        // 0.1.11.181: the heat pipe into the engine, the shaft with its flywheel, the generator and the cable out; the heater's and
        // the cooler's thermometers at the upper right (the design temperature marked)
        MachineArt.pipe(g, x + 8, x + 30, y + 24, flow > 0.5 ? MachineArt.HEAT : 0, MachineArt.phase(time, flow > 0.5 ? Math.min(1.5, flow / 200) : 0));
        if (water) MachineArt.pipe(g, x + 8, x + 30, y + 63, MachineArt.WATER, MachineArt.phase(time, flow > 0.5 ? 0.5 : 0));
        engine(g, x + 30, y + 19, hot, cold, water, angle);
        MachineArt.shaft(g, x + 56, x + 108, y + 44);
        MachineArt.flywheel(g, x + 80, y + 44, 13, angle);
        centred(g, Component.literal(Math.round(rpm) + " rpm"), x + 80, y + 60, 0x2A5D8A);
        MachineArt.generator(g, x + 108, y + 31, 30, 26);
        MachineArt.euCable(g, eu > 0, time, x + 138, y + 44, x + 168, y + 44);
        MachineArt.thermometer(g, x + 150, y + 19, 18, hot, 800);
        MachineArt.mark(g, x + 150, y + 19, 4, 18, StirlingPhysics.DESIGN_HOT, 800, 0xFF2A5D8A);
        MachineArt.thermometer(g, x + 160, y + 19, 18, cold, 800);

        lines(g, x, y, hot, cold, factor, water, v(11) / 10.0);
        line(g, x, y, 1, Component.translatable(K + "flow", Math.round(flow), v(12), eu, f("%.2f", v(13) / 1000.0)), WindRoseView.DIM);
        String state; int colour = 0xA0602A;
        if (v(1) > 0 && v(0) >= v(1)) state = "state.full";
        else if (flow < 0.5) state = "state.no_heat";
        else if (hot < 400) state = "state.warming";
        else { state = "state.running"; colour = 0x2F6E4F; }
        ReadoutPanel.headline(Component.literal(eu + " EU/t"), Component.translatable(K + state), colour);   // 0.1.11.145: the output leads, the state beside it
    }

    /**
     * 0.1.11.181: a Stirling engine standing at (x, y), 26 wide and 49 high: the heater head on top in its temperature's colour,
     * the cylinder behind a window with the displacer and the power piston a quarter turn apart, the cooler at the bottom (fins
     * in air, pale blue with water).
     */
    static void engine(GuiGraphics g, int x, int y, double hot, double cold, boolean water, double angle) {
        MachineArt.raised(g, x, y, 26, 11, MachineArt.temperature(hot), MachineArt.STEEL_LIGHT, MachineArt.STEEL_DARK);
        MachineArt.steel(g, x + 3, y + 13, 20, 24);
        MachineArt.sunk(g, x + 5, y + 15, 16, 20, MachineArt.WINDOW);
        int d = y + 16 + (int) Math.round(6 * (1 + Math.sin(angle)) / 2), p = y + 25 + (int) Math.round(6 * (1 + Math.sin(angle + Math.PI / 2)) / 2);
        FcsDraw.fill(g, x + 6, d, x + 20, d + 3, MachineArt.STEEL_LIGHT);
        FcsDraw.fill(g, x + 6, p, x + 20, p + 4, MachineArt.IRON);
        MachineArt.raised(g, x, y + 39, 26, 10, water ? 0xFFCFE0EF : MachineArt.STEEL, MachineArt.STEEL_LIGHT, MachineArt.STEEL_DARK);
        if (!water) for (int k = 0; k < 5; k++) FcsDraw.fill(g, x + 3 + k * 5, y + 40, x + 4 + k * 5, y + 48, MachineArt.STEEL_DARK);
    }

    /** Line 0: the heater, the cooler and the efficiency against the design point. */
    void lines(GuiGraphics g, int x, int y, double hot, double cold, double factor, boolean water, double ambient) {
        Component cooling = water ? Component.translatable(K + "water") : Component.translatable(K + "air", Math.round(ambient));
        line(g, x, y, 0, Component.translatable(K + "temps", Math.round(hot), Math.round(cold), cooling,
            Math.round(StirlingPhysics.carnot(hot, cold) * 100) + "%", Math.round(factor * 100) + "%"), WindRoseView.INK);
    }

    @Override
    protected void drawStrip(GuiGraphics g, int x, int y) {
        long e = Integer.toUnsignedLong(v(0)), cap = Math.max(1, Integer.toUnsignedLong(v(1)));
        bar(g, x, y, Component.translatable(K + "stored"), e / (double) cap, stored(e, cap));
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        int rx = mouseX - leftPos, ry = mouseY - topPos;
        // EXPLAIN_TIP_HIDDEN (0.1.11.182): the drawing's how-it-works text, hidden for now
        // if (overMachine(rx, ry))
        //     g.renderComponentTooltip(this.font, List.of(Component.translatable(K + "tip1"), Component.translatable(K + "tip2"),
        //         Component.translatable(K + "tip3")), mouseX, mouseY);
    }
}
