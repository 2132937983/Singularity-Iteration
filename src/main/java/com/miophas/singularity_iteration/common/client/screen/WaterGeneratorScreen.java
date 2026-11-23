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
 *
 * <p>0.1.11.181 (176 wide, merged): the bucket slot over the tank's filler, the battery at the end of the output cable, drawn on the panel.
 */
@OnlyIn(Dist.CLIENT)
@SuppressWarnings("null")
public class WaterGeneratorScreen extends MimicScreen<WaterGeneratorMenu> {
    private static final String K = WaterRotorScreen.K;

    public WaterGeneratorScreen(WaterGeneratorMenu menu, Inventory inv, Component title) { super(menu, inv, title); }

    private int v(int i) { return menu.value(i); }

    @Override
    protected void drawMachine(GuiGraphics g, int x, int y, double time) {
        boolean front = v(6) != 0;
        double speed = v(8) / 1000.0;
        int face = v(7), eu = v(3), source = v(9);
        // 0.1.11.181: the bucket over the tank's filler, the tank, its pipe into the turbine; the water in front through the
        // intake; the output cable to the battery
        MachineArt.filler(g, x + WaterGeneratorMenu.BUCKET_X - 2, y + WaterGeneratorMenu.BUCKET_Y + 19, 20);
        MachineArt.level(g, x + 12, y + 46, 12, 26, v(5) > 0 ? v(4) / (double) v(5) : 0, MachineArt.WATER);
        boolean fromTank = v(4) > 0 && eu > face;
        MachineArt.pipe(g, x + 26, x + 46, y + 60, fromTank ? MachineArt.WATER : 0, MachineArt.phase(time, fromTank ? 0.4 : 0));
        MachineArt.iron(g, x + 46, y + 22, 44, 46);
        MachineArt.sunk(g, x + 50, y + 26, 36, 38, MachineArt.WINDOW);
        MachineArt.turbine(g, x + 52, y + 28, 32, 34, eu > 0 ? time : -1);
        int c = !front ? 0xFFF4F6F4 : speed > 0.05 ? WaterRotorScreen.MOVING : WaterRotorScreen.STILL;
        MachineArt.sunk(g, x + 96, y + 28, 34, 40, c);
        if (front && speed > 0.05)
            for (int r = 0; r < 4; r++) {
                int dx = (int) ((time * speed * 1.2 + r * 7) % 26);
                FcsDraw.fill(g, x + 126 - dx - 4, y + 33 + r * 9, x + 126 - dx, y + 34 + r * 9, 0xD0FFFFFF);
            }
        centred(g, Component.literal(f("%.2f m/s", speed)), x + 113, y + 70, 0x2A5D8A);
        MachineArt.euCable(g, eu > 0, time, x + 68, y + 22, x + 68, y + 18, x + 154, y + 18, x + 154, y + WaterGeneratorMenu.BATTERY_Y - 2);

        Component src = Component.translatable(K + WaterRotorScreen.sourceKey(front ? source : 0));
        line(g, x, y, 0, Component.translatable(K + "gen_front", src, f("%.2f", speed), face), WindRoseView.INK);
        line(g, x, y, 1, Component.translatable(K + "gen_tank", f("%,d", v(4)), f("%,d", v(5))), WindRoseView.DIM);
        String state; int colour = 0xA0602A;
        if (v(1) > 0 && v(0) >= v(1)) state = "state.full";
        else if (eu > 0) { state = "state.running"; colour = 0x2F6E4F; }
        else if (!front) state = "state.not_in_water";
        else state = "state.still";
        ReadoutPanel.headline(Component.literal(eu + " EU/t"), Component.translatable(K + state), colour);   // 0.1.11.145: the output leads, the state beside it
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
        //     g.renderComponentTooltip(this.font, List.of(Component.translatable(K + "gen_tip1"), Component.translatable(K + "gen_tip2")), mouseX, mouseY);
    }
}
