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
 *
 * <p>0.1.11.181 (176 wide, merged): the six pellet slots in a lead container, three by two, the hot and cold junctions' thermometers, the record; the figures beside it went (they are in the readouts).
 */
@OnlyIn(Dist.CLIENT)
@SuppressWarnings("null")
public class RtgScreen extends MimicScreen<RTGeneratorMenu> {
    static final String K = "gui.mio_icif.rtg.";

    public RtgScreen(RTGeneratorMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
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
    protected void drawMachine(GuiGraphics g, int x, int y, double time) {
        int pellets = v(3), base = v(2);
        double cold = v(5) / 10.0, hot = RtgPhysics.hot(cold, pellets), factor = v(4) / 1000.0, eu = v(8) / 100.0;
        // 0.1.11.181: the pellets in their lead container, the hot and cold junctions, the record
        container(g, x, y);
        MachineArt.thermometer(g, x + 80, y + 19, 52, hot, 700);
        MachineArt.thermometer(g, x + 89, y + 19, 52, Math.max(0, cold), 700);
        ChartView.draw(g, x + 98, y + 18, 68, 56, i -> v(RTGeneratorMenu.CHART + i), Math.max(10, base * 13.0), 0xFFD9531E, new int[0],
            new ChartView.Ref(base * 10.0, 0xFF5B6867, Component.literal(String.valueOf(base))));

        line(g, x, y, 0, Component.translatable(K + "line_table", pellets, base, Component.translatable(K + coldKey(v(6))), f("%.2f", factor)), WindRoseView.INK);
        line(g, x, y, 1, Component.translatable(K + "line_temps", Math.round(hot), f("%.0f", cold), Math.round(hot - cold)), WindRoseView.DIM);
        String state; int colour = 0xA0602A;
        if (pellets == 0) state = "state.no_pellets";
        else if (v(1) > 0 && v(0) >= v(1)) state = "state.full";
        else if (v(7) != 0) state = "state.lava";
        else { state = "state.running"; colour = 0x2F6E4F; }
        ReadoutPanel.headline(Component.literal(f("%.1f EU/t", eu)), Component.translatable(K + state), colour);   // 0.1.11.145: the output leads, the state beside it
    }

    /** 0.1.11.181: the lead container of the six pellet slots (also the RT heat generator's). */
    static void container(GuiGraphics g, int x, int y) {
        MachineArt.raised(g, x + 8, y + 16, 66, 48, MachineArt.LEAD, MachineArt.LEAD_LIGHT, MachineArt.LEAD_DARK);
        MachineArt.sunk(g, x + 11, y + 19, 60, 42, 0xFF3A3C48);
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
        //     g.renderComponentTooltip(this.font, List.of(Component.translatable(K + "tip1"), Component.translatable(K + "tip2")), mouseX, mouseY);
    }

    /** 0.1.11.119: the window is a chart: kept as it was (not pixel art). */
    @Override
    protected boolean pixelArt() { return false; }
}
