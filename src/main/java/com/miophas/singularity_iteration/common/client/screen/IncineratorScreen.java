// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.client.screen;

import com.miophas.singularity_iteration.common.client.suit.FcsDraw;
import com.miophas.singularity_iteration.common.energy.IncineratorPhysics;
import com.miophas.singularity_iteration.common.menu.generator.IncineratorValues;
import com.miophas.singularity_iteration.common.menu.generator.MimicGeneratorMenu;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/**
 * 0.1.11.19: the item generators' recorder: the chamber temperature of the last 12 seconds with the design (850 °C) and
 * ignition (300 °C) lines, a mark for every item that went in (orange burning, grey not burning); the temperature, the
 * efficiency and the output beside it; the ash pit under it. Values as in {@link IncineratorValues}.
 *
 * <p>0.1.11.181 (176 wide, merged): the furnace on the left with the ash slot as the drawer under its grate, the record beside it; the figures beside the record went (they are in the readouts).
 */
@OnlyIn(Dist.CLIENT)
@SuppressWarnings("null")
public class IncineratorScreen<M extends MimicGeneratorMenu> extends MimicScreen<M> {
    static final String K = "gui.mio_icif.incinerator.";

    public IncineratorScreen(M menu, Inventory inv, Component title) { super(menu, inv, title); }

    private int v(int i) { return menu.value(i); }

    @Override
    protected void drawMachine(GuiGraphics g, int x, int y, double time) {
        int chamber = v(4);
        // 0.1.11.181: the furnace: the fire in its chamber, the grate, the ash drawer (the ash slot) under it; the record beside it
        MachineArt.iron(g, x + 10, y + 18, 52, 56);
        MachineArt.sunk(g, x + 14, y + 22, 44, 26, MachineArt.WINDOW);
        if (v(3) != 0) MachineArt.flames(g, x + 16, x + 56, y + 46, time, 5, Math.max(3, Math.min(12, chamber / 80)));
        FcsDraw.fill(g, x + 14, y + 48, x + 58, y + 51, MachineArt.IRON_DARK);
        for (int k = 16; k < 56; k += 4) FcsDraw.fill(g, x + k, y + 49, x + k + 2, y + 50, 0xFF8D8F92);
        ChartView.draw(g, x + 68, y + 18, 98, 56, i -> v(IncineratorValues.CHART + i), 1100, 0xFFD9531E, new int[]{0xFFF0913A, 0xFF8D8F92},
            new ChartView.Ref(IncineratorPhysics.DESIGN, 0xFF2F6E4F, Component.literal("850")),
            new ChartView.Ref(IncineratorPhysics.IGNITION, 0xFFC0392B, Component.literal("300")));

        line(g, x, y, 0, Component.translatable(K + "line_chamber", chamber, Math.round(v(5) / 10.0) + "%"), WindRoseView.INK);
        line(g, x, y, 1, Component.translatable(K + "line_fuel", f("%,d", v(2)), v(9), v(8)), WindRoseView.DIM);
        String state; int colour = 0xA0602A;
        if (v(6) >= IncineratorPhysics.ASH_CAPACITY - 1) { state = "state.ash_full"; colour = 0xC0392B; }
        else if (v(1) > 0 && v(0) >= v(1)) state = "state.full";
        else if (v(3) == 0) state = "state.no_fuel";
        else if (chamber < IncineratorPhysics.IGNITION) state = "state.igniting";
        else if (chamber < IncineratorPhysics.DESIGN) state = "state.warming";
        else { state = "state.running"; colour = 0x2F6E4F; }
        ReadoutPanel.headline(Component.literal(v(7) + " EU/t"), Component.translatable(K + state), colour);   // 0.1.11.145: the output leads, the state beside it
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

    /** 0.1.11.119: the window is a chart: kept as it was (not pixel art). */
    @Override
    protected boolean pixelArt() { return false; }
}
