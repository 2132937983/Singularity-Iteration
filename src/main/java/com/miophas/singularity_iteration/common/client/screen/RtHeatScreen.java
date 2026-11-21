// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.client.screen;

import com.miophas.singularity_iteration.common.menu.huentity.RTHeatGeneratorMenu;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/** 0.1.11.20: the RT heat generator's recorder: the heat offered over the last 12 seconds and the pellets. */
@OnlyIn(Dist.CLIENT)
@SuppressWarnings("null")
public class RtHeatScreen extends MimicScreen<RTHeatGeneratorMenu> {
    static final String K = "gui.mio_icif.rtg.";

    public RtHeatScreen(RTHeatGeneratorMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        wx = 54;
        ww = 187;
    }

    private int v(int i) { return menu.value(i); }

    @Override
    protected void drawWindow(GuiGraphics g, int x, int y, double time) {
        float u = WindRoseView.u();
        int rate = v(2), pellets = v(3);
        ChartView.draw(g, x + 4, y + 4, 126, 52, i -> v(RTHeatGeneratorMenu.CHART + i), 70, 0xFFD9531E, new int[0],
            new ChartView.Ref(64, 0xFF5B6867, Component.literal("64")));
        PumpCrossSection.text(g, Component.literal(rate + " HU/t"), x + 136, y + 6, 0xA8461F, u);
        PumpCrossSection.text(g, Component.translatable(K + "pellets", pellets), x + 136, y + 18, 0x1C2524, u);
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
        if (rx >= wx && rx < wx + ww && ry >= wy && ry < wy + wh)
            g.renderComponentTooltip(this.font, List.of(Component.translatable(K + "heat_tip")), mouseX, mouseY);
    }

    /** 0.1.11.119: the window is a chart: kept as it was (not pixel art). */
    @Override
    protected boolean pixelArt() { return false; }
}
