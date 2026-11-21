// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.client.screen;

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
 */
@OnlyIn(Dist.CLIENT)
@SuppressWarnings("null")
public class StirlingScreen<M extends MimicGeneratorMenu> extends MimicScreen<M> {
    static final String K = "gui.mio_icif.stirling.";

    public StirlingScreen(M menu, Inventory inv, Component title) { super(menu, inv, title); }

    private int v(int i) { return menu.value(i); }

    @Override
    protected void drawWindow(GuiGraphics g, int x, int y, double time) {
        float u = WindRoseView.u();
        double hot = v(6) / 10.0, cold = v(7) / 10.0, factor = v(8) / 1000.0, flow = v(9) / 10.0;
        boolean water = v(10) != 0;
        int eu = (int) Math.round(flow * v(13) / 1000.0);   // smoothed: the heat flow times the ratio
        double rpm = flow > 0.5 ? 1500 * Math.min(1.2, Math.sqrt(flow / Math.max(1, v(12)))) : 0;
        double angle = PlantMimic.angle(time, rpm);
        drawEngine(g, x, y, time, hot, cold, water, flow, angle, u);
        PlantMimic.shaft(g, x + 52, y + 30, x + 120, y + 30);
        PlantMimic.flywheel(g, x + 82, y + 30, 11, angle);
        PlantMimic.machine(g, x + 128, y + 30, 8, "G", 0xFFDDE3E6, u);
        PlantMimic.pipe(g, new float[]{x + 136, y + 30, x + 168, y + 30}, 0xFF2F6E4F, 3, eu > 0 ? Math.min(1.5, eu / 100.0) : 0, time);
        PlantMimic.label(g, Component.literal(eu + " EU/t"), x + 152, y + 12, 0x2F6E4F);   // 0.1.11.145: room for the vanilla font
        PlantMimic.label(g, Component.literal(Math.round(rpm) + " rpm"), x + 82, y + 45, 0x2A5D8A);
        PlantMimic.thermometer(g, x + 180, y + 6, 46, hot, 800, StirlingPhysics.DESIGN_HOT);
        PlantMimic.thermometer(g, x + 192, y + 6, 46, cold, 800, 0);

        lines(g, x, y, hot, cold, factor, water, v(11) / 10.0);
        line(g, x, y, 1, Component.translatable(K + "flow", Math.round(flow), v(12), eu, f("%.2f", v(13) / 1000.0)), WindRoseView.DIM);
        String state; int colour = 0xA0602A;
        if (v(1) > 0 && v(0) >= v(1)) state = "state.full";
        else if (flow < 0.5) state = "state.no_heat";
        else if (hot < 400) state = "state.warming";
        else { state = "state.running"; colour = 0x2F6E4F; }
        ReadoutPanel.headline(Component.literal(eu + " EU/t"), Component.translatable(K + state), colour);   // 0.1.11.145: the output leads, the state beside it
    }

    /** The heat pipe and the engine on the left of the window. */
    static void drawEngine(GuiGraphics g, int x, int y, double time, double hot, double cold, boolean water, double flow, double angle, float u) {
        PlantMimic.pipe(g, new float[]{x + 4, y + 10, x + 26, y + 10}, flow > 0.5 ? PlantMimic.HEAT : 0xFFB0B6B8, 3, flow > 0.5 ? Math.min(1.5, flow / 200) : 0, time);
        PlantMimic.label(g, Component.translatable(K + "heat_in"), x + 13, y + 14, WindRoseView.DIM);
        PlantMimic.stirling(g, x + 26, y + 5, hot, cold, water, angle, u);
        if (water) PlantMimic.pipe(g, new float[]{x + 4, y + 49, x + 26, y + 49}, PlantMimic.WATER, 3, flow > 0.5 ? 0.5 : 0, time);
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
        bar(g, x, y, Component.translatable(K + "stored"), e / (double) cap, Component.literal(f("%,d / %,d EU", e, cap)));
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        int rx = mouseX - leftPos, ry = mouseY - topPos;
        if (rx >= wx && rx < wx + ww && ry >= wy && ry < wy + wh)
            g.renderComponentTooltip(this.font, List.of(Component.translatable(K + "tip1"), Component.translatable(K + "tip2"),
                Component.translatable(K + "tip3")), mouseX, mouseY);
    }
}
