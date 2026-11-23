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
 *
 * <p>0.1.11.181 (176 wide, merged): the four container slots over and under their tanks' fillers, drawn on the panel; the thermometers went (the readouts give both temperatures).
 */
@OnlyIn(Dist.CLIENT)
@SuppressWarnings("null")
public class StirlingKineticScreen extends MimicScreen<StirlingKineticGeneratorMenu> {
    private static final String K = StirlingScreen.K;

    public StirlingKineticScreen(StirlingKineticGeneratorMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
    }

    private int v(int i) { return menu.value(i); }

    @Override
    protected void drawMachine(GuiGraphics g, int x, int y, double time) {
        double hot = v(11) / 10.0, cold = v(12) / 10.0, factor = v(13) / 1000.0, flow = v(14) / 10.0;
        int rpm = v(7), cap = Math.max(1, v(2));
        double angle = PlantMimic.angle(time, rpm);
        // 0.1.11.181: the water containers over the upper filler into the water tank, the hot water tank out through the lower
        // filler into the containers under it; both tanks piped to the engine's cooler; the shaft, the flywheel, the coupling
        int c1 = StirlingKineticGeneratorMenu.COL1;
        MachineArt.filler(g, x + c1 - 2, y + StirlingKineticGeneratorMenu.ROW_WATER + 18, 40);
        MachineArt.filler(g, x + c1 - 2, y + StirlingKineticGeneratorMenu.ROW_HOT - 7, 40);
        MachineArt.pipe(g, x + 48, x + 56, y + 38, MachineArt.WATER, -1);
        MachineArt.pipe(g, x + 48, x + 56, y + 53, MachineArt.HOT_WATER, -1);
        MachineArt.level(g, x + 58, y + 19, 6, 24, v(0) / (double) cap, MachineArt.WATER);
        MachineArt.level(g, x + 58, y + 49, 6, 24, v(1) / (double) cap, MachineArt.HOT_WATER);
        boolean cooled = flow >= 0.5 && v(0) > 0;
        MachineArt.pipe(g, x + 66, x + 71, y + 40, cooled ? MachineArt.WATER : 0, MachineArt.phase(time, cooled ? 0.5 : 0));
        MachineArt.pipeDown(g, x + 70, y + 38, y + 62, cooled ? MachineArt.WATER : 0, MachineArt.phase(time, cooled ? 0.5 : 0));
        MachineArt.pipe(g, x + 68, x + 74, y + 60, cooled ? MachineArt.WATER : 0, MachineArt.phase(time, cooled ? 0.5 : 0));
        MachineArt.pipe(g, x + 66, x + 74, y + 67, cooled ? MachineArt.HOT_WATER : 0, MachineArt.phase(time, cooled ? 0.5 : 0));
        StirlingScreen.engine(g, x + 74, y + 19, hot, cold, true, angle);
        MachineArt.shaft(g, x + 100, x + 140, y + 44);
        MachineArt.flywheel(g, x + 118, y + 44, 13, angle);
        centred(g, Component.literal(rpm + " rpm"), x + 118, y + 60, v(10) != 0 ? 0xC0392B : 0x2A5D8A);
        MachineArt.coupling(g, x + 140, y + 35, 24, 18);

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
        // EXPLAIN_TIP_HIDDEN (0.1.11.182): the drawing's how-it-works text, hidden for now
        // if (overMachine(rx, ry))
        //     g.renderComponentTooltip(this.font, List.of(Component.translatable(K + "tip1"), Component.translatable(K + "tip_flywheel"),
        //         Component.translatable(K + "tip_water")), mouseX, mouseY);
    }
}
