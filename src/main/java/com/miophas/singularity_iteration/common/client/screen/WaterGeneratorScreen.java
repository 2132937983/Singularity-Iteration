// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.client.screen;

import com.miophas.singularity_iteration.common.client.suit.FcsDraw;

import com.miophas.singularity_iteration.common.menu.generator.WaterGeneratorMenu;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/**
 * 0.1.11.18: the water generator: the block of water in front (moving, still or none) with its speed, the tank and the
 * small turbine, and the two outputs. Values as in {@link WaterGeneratorMenu}.
 */
@OnlyIn(Dist.CLIENT)
@SuppressWarnings("null")
public class WaterGeneratorScreen extends MimicScreen<WaterGeneratorMenu> {
    private static final String K = WaterRotorScreen.K;

    public WaterGeneratorScreen(WaterGeneratorMenu menu, Inventory inv, Component title) { super(menu, inv, title); }

    private int v(int i) { return menu.value(i); }

    @Override
    protected void drawWindow(GuiGraphics g, int x, int y, double time) {
        float u = WindRoseView.u();
        boolean front = v(6) != 0;
        double speed = v(8) / 1000.0;
        int face = v(7), eu = v(3), source = v(9);
        // the tank and its pipe into the turbine
        PlantMimic.tank(g, x + 6, y + 6, 8, 46, v(5) > 0 ? v(4) / (double) v(5) : 0, PlantMimic.WATER);
        PlantMimic.pipe(g, new float[]{x + 14, y + 40, x + 60, y + 40}, PlantMimic.WATER, 3, v(4) > 0 && eu > face ? 0.4 : 0, time);
        // the machine with its turbine
        PlantMimic.box(g, x + 60, y + 16, 40, 34, 0xFFE3E6E4);
        PlantMimic.turbine(g, x + 64, y + 22, 32, 22, eu > 0 ? time : 0);
        PlantMimic.pipe(g, new float[]{x + 100, y + 12, x + 100, y + 6, x + 196, y + 6}, 0xFF2F6E4F, 2, eu > 0 ? Math.min(1.5, eu / 4.0) : 0, time);
        PlantMimic.label(g, Component.literal(eu + " EU/t"), x + 170, y + 10, 0x2F6E4F);
        // the water in front
        int fx = x + 118, fy = y + 18;
        int c = !front ? 0xFFF4F6F4 : speed > 0.05 ? WaterRotorScreen.MOVING : WaterRotorScreen.STILL;
        PlantMimic.box(g, fx, fy, 32, 32, c);
        if (front && speed > 0.05)
            for (int r = 0; r < 4; r++) {
                int dx = (int) ((time * speed * 1.2 + r * 7) % 26);
                FcsDraw.fill(g, fx + 30 - dx - 4, fy + 5 + r * 7, fx + 30 - dx, fy + 6 + r * 7, 0xD0FFFFFF);
            }
        PlantMimic.label(g, Component.translatable(K + "in_front"), fx + 16, fy + 36, WindRoseView.DIM);
        PlantMimic.label(g, Component.literal(f("%.2f m/s", speed)), fx + 56, fy + 12, 0x2A5D8A);
        PlantMimic.label(g, Component.literal(face + " EU/t"), fx + 56, fy + 22, 0x2F6E4F);

        Component src = Component.translatable(K + WaterRotorScreen.sourceKey(front ? source : 0));
        line(g, x, y, 0, Component.translatable(K + "gen_front", src, f("%.2f", speed), face), WindRoseView.INK);
        line(g, x, y, 1, Component.translatable(K + "gen_tank", f("%,d", v(4)), f("%,d", v(5))), WindRoseView.DIM);
        String state; int colour = 0xA0602A;
        if (v(1) > 0 && v(0) >= v(1)) state = "state.full";
        else if (eu > 0) { state = "state.running"; colour = 0x2F6E4F; }
        else if (!front) state = "state.not_in_water";
        else state = "state.still";
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
            g.renderComponentTooltip(this.font, List.of(Component.translatable(K + "gen_tip1"), Component.translatable(K + "gen_tip2")), mouseX, mouseY);
    }
}
