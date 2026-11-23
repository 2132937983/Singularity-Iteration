// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.client.screen;

import com.miophas.singularity_iteration.common.menu.generator.IncineratorValues;
import com.miophas.singularity_iteration.common.menu.generator.MimicGeneratorMenu;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/**
 * 0.1.11.19: the experience generators' recorder: the output of the last 12 seconds with a mark where orbs were taken,
 * the experience stocked and the range. Values as in {@link IncineratorValues}.
 *
 * <p>0.1.11.181 (176 wide): the record fills the drawing; the figures beside it went (they are in the readouts).
 */
@OnlyIn(Dist.CLIENT)
@SuppressWarnings("null")
public class ExperienceScreen<M extends MimicGeneratorMenu> extends MimicScreen<M> {
    static final String K = "gui.mio_icif.experience.";

    public ExperienceScreen(M menu, Inventory inv, Component title) { super(menu, inv, title); }

    private int v(int i) { return menu.value(i); }

    @Override
    protected void drawMachine(GuiGraphics g, int x, int y, double time) {
        int production = Math.max(1, v(6));
        // 0.1.11.181: the record across the drawing (the output, stock and range are in the readouts)
        ChartView.draw(g, x + 10, y + 18, 156, 56, i -> v(IncineratorValues.XP_CHART + i), production * 1.25, 0xFF4FA35A, new int[]{0xFF7FD15C},
            new ChartView.Ref(production, 0xFF2F6E4F, Component.literal(String.valueOf(production))));

        line(g, x, y, 0, Component.translatable(K + "line_rate", v(4), production), WindRoseView.INK);
        line(g, x, y, 1, Component.translatable(K + "line_stock", f("%,d", v(2)), f("%,d", (long) v(2) * production), v(5)), WindRoseView.DIM);
        String state; int colour = 0xA0602A;
        if (v(1) > 0 && v(0) >= v(1)) state = "state.full";
        else if (v(3) != 0) { state = "state.running"; colour = 0x2F6E4F; }
        else state = "state.no_xp";
        ReadoutPanel.headline(Component.literal(v(4) + " EU/t"), Component.translatable(K + state), colour);   // 0.1.11.145: the output leads, the state beside it
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
        //     g.renderComponentTooltip(this.font, List.of(Component.translatable(K + "tip1")), mouseX, mouseY);
    }

    /** 0.1.11.119: the window is a chart: kept as it was (not pixel art). */
    @Override
    protected boolean pixelArt() { return false; }
}
