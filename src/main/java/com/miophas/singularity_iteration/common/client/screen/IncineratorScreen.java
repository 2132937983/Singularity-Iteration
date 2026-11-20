// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.client.screen;

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
 */
@OnlyIn(Dist.CLIENT)
@SuppressWarnings("null")
public class IncineratorScreen<M extends MimicGeneratorMenu> extends MimicScreen<M> {
    static final String K = "gui.mio_icif.incinerator.";

    public IncineratorScreen(M menu, Inventory inv, Component title) { super(menu, inv, title); }

    private int v(int i) { return menu.value(i); }

    @Override
    protected void drawWindow(GuiGraphics g, int x, int y, double time) {
        float u = WindRoseView.u();
        ChartView.draw(g, x + 4, y + 4, 140, 52, i -> v(IncineratorValues.CHART + i), 1100, 0xFFD9531E, new int[]{0xFFF0913A, 0xFF8D8F92},
            new ChartView.Ref(IncineratorPhysics.DESIGN, 0xFF2F6E4F, Component.literal("850")),
            new ChartView.Ref(IncineratorPhysics.IGNITION, 0xFFC0392B, Component.literal("300")));
        int chamber = v(4);
        PumpCrossSection.text(g, Component.literal(chamber + " °C"), x + 150, y + 6, PlantMimic.colour(chamber * 0.45) & 0xFFFFFF, u);
        PumpCrossSection.text(g, Component.translatable(K + "eff", Math.round(v(5) / 10.0) + "%"), x + 150, y + 18, 0x1C2524, u);
        PumpCrossSection.text(g, Component.literal(v(7) + " EU/t"), x + 150, y + 30, 0x2F6E4F, u);
        PumpCrossSection.text(g, Component.translatable(K + "ash_short", v(6)), x + 150, y + 42, v(6) >= IncineratorPhysics.ASH_CAPACITY - 1 ? 0xC0392B : 0x5B6867, u);

        line(g, x, y, 0, Component.translatable(K + "line_chamber", chamber, Math.round(v(5) / 10.0) + "%"), WindRoseView.INK);
        line(g, x, y, 1, Component.translatable(K + "line_fuel", f("%,d", v(2)), v(9), v(8)), WindRoseView.DIM);
        String state; int colour = 0xA0602A;
        if (v(6) >= IncineratorPhysics.ASH_CAPACITY - 1) { state = "state.ash_full"; colour = 0xC0392B; }
        else if (v(1) > 0 && v(0) >= v(1)) state = "state.full";
        else if (v(3) == 0) state = "state.no_fuel";
        else if (chamber < IncineratorPhysics.IGNITION) state = "state.igniting";
        else if (chamber < IncineratorPhysics.DESIGN) state = "state.warming";
        else { state = "state.running"; colour = 0x2F6E4F; }
        line(g, x, y, 2, Component.translatable(K + state), colour);
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

    /** 0.1.11.119: the window is a chart: kept as it was (not pixel art). */
    @Override
    protected boolean pixelArt() { return false; }
}
