// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.client.screen;

import com.miophas.singularity_iteration.common.menu.huentity.RTHeatGeneratorMenu;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/** 0.1.11.20: the RT heat generator's recorder: the heat offered over the last 12 seconds and the pellets. *
 * <p>0.1.11.181 (176 wide, merged): the six pellet slots in a lead container, three by two, the heat pipe out to the record.
 */
@OnlyIn(Dist.CLIENT)
@SuppressWarnings("null")
public class RtHeatScreen extends MimicScreen<RTHeatGeneratorMenu> {
    static final String K = "gui.mio_icif.rtg.";

    public RtHeatScreen(RTHeatGeneratorMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
    }

    private int v(int i) { return menu.value(i); }

    @Override
    protected void drawMachine(GuiGraphics g, int x, int y, double time) {
        int rate = v(2), pellets = v(3);
        // 0.1.11.181: the pellets in their lead container, the heat out of the front face, the record
        RtgScreen.container(g, x, y);
        MachineArt.pipe(g, x + 74, x + 86, y + 40, pellets > 0 ? MachineArt.HEAT : 0, MachineArt.phase(time, pellets > 0 ? 0.5 : 0));
        ChartView.draw(g, x + 88, y + 18, 78, 56, i -> v(RTHeatGeneratorMenu.CHART + i), 70, 0xFFD9531E, new int[0],
            new ChartView.Ref(64, 0xFF5B6867, Component.literal("64")));

        line(g, x, y, 0, Component.translatable(K + "heat_line", pellets, rate), WindRoseView.INK);
        line(g, x, y, 1, Component.translatable(K + "heat_front"), WindRoseView.DIM);
        ReadoutPanel.headline(Component.literal(rate + " HU/t"), Component.translatable(K + (pellets > 0 ? "state.running" : "state.no_pellets")),
            pellets > 0 ? 0x2F6E4F : 0xA0602A);   // 0.1.11.145: the output leads, the state beside it
    }

    @Override
    protected void drawStrip(GuiGraphics g, int x, int y) {
        bar(g, x, y, Component.translatable(K + "heat"), v(1) > 0 ? v(0) / (double) v(1) : 0, Component.literal(f("%,d / %,d HU", v(0), v(1))));
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        int rx = mouseX - leftPos, ry = mouseY - topPos;
        // EXPLAIN_TIP_HIDDEN (0.1.11.182): the drawing's how-it-works text, hidden for now
        // if (overMachine(rx, ry))
        //     g.renderComponentTooltip(this.font, List.of(Component.translatable(K + "heat_tip")), mouseX, mouseY);
    }

    /** 0.1.11.119: the window is a chart: kept as it was (not pixel art). */
    @Override
    protected boolean pixelArt() { return false; }
}
