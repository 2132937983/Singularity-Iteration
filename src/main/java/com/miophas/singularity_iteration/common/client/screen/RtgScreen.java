// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.client.screen;

import com.miophas.singularity_iteration.common.energy.RtgPhysics;
import com.miophas.singularity_iteration.common.menu.generator.RTGeneratorMenu;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/**
 * 0.1.11.20: the RTG's recorder: the hot junctions' and the cold side's thermometers with the difference between them,
 * the output of the last 12 seconds against the pellet table, and the cold side's factor. Values as in
 * {@link RTGeneratorMenu}.
 */
@OnlyIn(Dist.CLIENT)
@SuppressWarnings("null")
public class RtgScreen extends MimicScreen<RTGeneratorMenu> {
    static final String K = "gui.mio_icif.rtg.";

    public RtgScreen(RTGeneratorMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        wx = 54;
        ww = 187;
    }

    private int v(int i) { return menu.value(i); }

    static String coldKey(int kind) {
        return switch (kind) {
            case RtgPhysics.HOT_BIOME -> "cold.hot";
            case RtgPhysics.NETHER_AIR -> "cold.nether";
            case RtgPhysics.WATER_SIDE -> "cold.water";
            case RtgPhysics.ICE_SIDE -> "cold.ice";
            default -> "cold.air";
        };
    }

    @Override
    protected void drawWindow(GuiGraphics g, int x, int y, double time) {
        float u = WindRoseView.u();
        int pellets = v(3), base = v(2);
        double cold = v(5) / 10.0, hot = RtgPhysics.hot(cold, pellets), factor = v(4) / 1000.0, eu = v(8) / 100.0;
        PlantMimic.thermometer(g, x + 6, y + 6, 46, hot, 700, 0);
        PlantMimic.thermometer(g, x + 16, y + 6, 46, Math.max(0, cold), 700, 0);
        PlantMimic.label(g, Component.literal(Math.round(hot) + "°"), x + 8, y - 2 + 58, 0xA8461F);
        ChartView.draw(g, x + 30, y + 4, 100, 52, i -> v(RTGeneratorMenu.CHART + i), Math.max(10, base * 13.0), 0xFFD9531E, new int[0],
            new ChartView.Ref(base * 10.0, 0xFF5B6867, Component.literal(String.valueOf(base))));
        PumpCrossSection.text(g, Component.literal(f("%.1f EU/t", eu)), x + 136, y + 6, 0x2F6E4F, u);
        PumpCrossSection.text(g, Component.literal(f("×%.2f", factor)), x + 136, y + 18, factor >= 1 ? 0x2A5D8A : 0xC0602A, u);
        PumpCrossSection.text(g, Component.translatable(K + coldKey(v(6))), x + 136, y + 30, 0x5B6867, u);
        PumpCrossSection.text(g, Component.literal(f("ΔT %d K", Math.round(hot - cold))), x + 136, y + 42, 0x1C2524, u);

        line(g, x, y, 0, Component.translatable(K + "line_table", pellets, base, Component.translatable(K + coldKey(v(6))), f("%.2f", factor)), WindRoseView.INK);
        line(g, x, y, 1, Component.translatable(K + "line_temps", Math.round(hot), f("%.0f", cold), Math.round(hot - cold)), WindRoseView.DIM);
        String state; int colour = 0xA0602A;
        if (pellets == 0) state = "state.no_pellets";
        else if (v(1) > 0 && v(0) >= v(1)) state = "state.full";
        else if (v(7) != 0) state = "state.lava";
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
            g.renderComponentTooltip(this.font, List.of(Component.translatable(K + "tip1"), Component.translatable(K + "tip2")), mouseX, mouseY);
    }
}
