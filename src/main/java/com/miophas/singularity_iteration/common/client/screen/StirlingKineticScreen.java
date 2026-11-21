// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.client.screen;

import com.miophas.singularity_iteration.common.energy.StirlingPhysics;
import com.miophas.singularity_iteration.common.menu.generator.StirlingKineticGeneratorMenu;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/**
 * 0.1.11.17: the Stirling kinetic generator's mimic: heat in, the engine cooled by its water (the water tank in, the hot
 * water tank out), the flywheel with its speed, and the KU leaving on the front. Values as in
 * {@link StirlingKineticGeneratorMenu}.
 */
@OnlyIn(Dist.CLIENT)
@SuppressWarnings("null")
public class StirlingKineticScreen extends MimicScreen<StirlingKineticGeneratorMenu> {
    private static final String K = StirlingScreen.K;

    public StirlingKineticScreen(StirlingKineticGeneratorMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        wx = 54;
        ww = 187;
    }

    private int v(int i) { return menu.value(i); }

    @Override
    protected void drawWindow(GuiGraphics g, int x, int y, double time) {
        float u = WindRoseView.u();
        double hot = v(11) / 10.0, cold = v(12) / 10.0, factor = v(13) / 1000.0, flow = v(14) / 10.0;
        int rpm = v(7), cap = Math.max(1, v(2));
        double angle = PlantMimic.angle(time, rpm);
        StirlingScreen.drawEngine(g, x, y, time, hot, cold, true, flow, angle, u);
        // water in, hot water out
        PlantMimic.tank(g, x + 60, y + 36, 5, 18, v(0) / (double) cap, PlantMimic.WATER);
        PlantMimic.tank(g, x + 70, y + 36, 5, 18, v(1) / (double) cap, 0xFFD9792B);
        PlantMimic.shaft(g, x + 52, y + 22, x + 150, y + 22);
        PlantMimic.flywheel(g, x + 98, y + 22, 13, angle);
        PlantMimic.label(g, Component.literal(rpm + " rpm"), x + 98, y + 39, v(10) != 0 ? 0xC0392B : 0x2A5D8A);
        PlantMimic.machine(g, x + 156, y + 22, 7, "KU", 0xFFDDE3E6, u);
        PlantMimic.label(g, Component.literal(v(6) + " KU/t"), x + 150, y + 33, 0x2A5D8A);   // 0.1.11.145: room for the vanilla font
        PlantMimic.thermometer(g, x + 172, y + 6, 46, hot, 800, StirlingPhysics.DESIGN_HOT);
        PlantMimic.thermometer(g, x + 180, y + 6, 46, cold, 800, 0);

        Component cooling = Component.translatable(K + "water");
        line(g, x, y, 0, Component.translatable(K + "temps", Math.round(hot), Math.round(cold), cooling,
            Math.round(StirlingPhysics.carnot(hot, cold) * 100) + "%", Math.round(factor * 100) + "%"), WindRoseView.INK);
        line(g, x, y, 1, Component.translatable(K + "kinetic_flow", Math.round(flow), f("%.2f", 3 * factor), v(6), f("%,d", v(0))), WindRoseView.DIM);
        String state; int colour = 0xA0602A;
        if (v(10) != 0) state = "state.overspeed";
        else if (v(0) <= 0) state = "state.no_water";
        else if (v(1) >= cap) state = "state.hot_full";
        else if (flow < 0.5) state = v(8) > 0 ? "state.coasting" : "state.no_heat";
        else if (hot < 400) state = "state.warming";
        else { state = "state.running"; colour = 0x2F6E4F; }
        ReadoutPanel.headline(Component.literal(v(6) + " KU/t"), Component.translatable(K + state), colour);   // 0.1.11.145: the output leads, the state beside it
    }

    @Override
    protected void drawStrip(GuiGraphics g, int x, int y) {
        bar(g, x, y, Component.translatable(K + "flywheel"), v(9) > 0 ? v(8) / (double) v(9) : 0,
            Component.literal(f("%,d / %,d KU", v(8), v(9))));
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        int rx = mouseX - leftPos, ry = mouseY - topPos;
        if (rx >= wx && rx < wx + ww && ry >= wy && ry < wy + wh)
            g.renderComponentTooltip(this.font, List.of(Component.translatable(K + "tip1"), Component.translatable(K + "tip_flywheel"),
                Component.translatable(K + "tip_water")), mouseX, mouseY);
    }
}
