// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.client.screen;

import com.miophas.singularity_iteration.common.client.suit.FcsDraw;

import com.miophas.singularity_iteration.common.menu.generator.SteamKineticGeneratorMenu;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/**
 * 0.1.11.16: the steam kinetic generator's mimic: the steam tank and inlet, the turbine with its wear, the shaft to the
 * kinetic output on the front, the condensate (distilled water) tank, and the exhaust to a condenser or a vent.
 *
 * <p>0.1.11.181 (176 wide, merged): the turbine slot in the turbine casing (its wear shows on the item), drawn on the panel.
 */
@OnlyIn(Dist.CLIENT)
@SuppressWarnings("null")
public class SteamKineticScreen extends MimicScreen<SteamKineticGeneratorMenu> {
    private static final String K = "gui.mio_icif.steam.";

    public SteamKineticScreen(SteamKineticGeneratorMenu menu, Inventory inv, Component title) { super(menu, inv, title); }

    private int v(int i) { return menu.value(i); }

    @Override
    protected void drawMachine(GuiGraphics g, int x, int y, double time) {
        boolean hot = v(0) != 0, hasSteam = v(15) >= 0 && v(9) > 0;
        int inlet = hasSteam ? (hot ? MachineArt.HOT_STEAM : MachineArt.STEAM) : 0;
        double flow = v(4) > 0 ? Math.min(1.5, v(4) / 1000.0) : 0;
        int tx = SteamKineticGeneratorMenu.TURBINE_X - 19;
        // 0.1.11.181: the steam tank, the turbine casing with the turbine slot in it (its wear is the item's own bar), the shaft
        // to the kinetic coupling; the exhaust down to the condenser (or the vent stack) and the condensate tank
        MachineArt.level(g, x + 12, y + 19, 8, 40, v(10) > 0 ? v(9) / (double) v(10) : 0, hasSteam ? inlet : MachineArt.STEAM);
        MachineArt.pipe(g, x + 22, x + tx, y + 32, flow > 0 ? inlet : 0, MachineArt.phase(time, flow));
        MachineArt.turbine(g, x + tx, y + 17, 54, 40, flow > 0 ? time : -1);
        MachineArt.shaft(g, x + tx + 54, x + 116, y + 37);
        MachineArt.coupling(g, x + 116, y + 28, 22, 18);
        MachineArt.level(g, x + 100, y + 52, 6, 14, v(16) > 0 ? v(6) / (double) v(16) : 0, MachineArt.DISTILLED);
        boolean condensed = v(12) != 0;
        int exhaust = flow > 0 ? (condensed ? MachineArt.DISTILLED : MachineArt.STEAM) : 0;
        MachineArt.pipeDown(g, x + 74, y + 57, y + 72, exhaust, MachineArt.phase(time, flow));
        MachineArt.pipe(g, x + 72, x + 140, y + 70, exhaust, MachineArt.phase(time, flow));
        if (condensed) {
            MachineArt.steel(g, x + 140, y + 60, 24, 16);
            MachineArt.sunk(g, x + 143, y + 63, 18, 10, 0xFFDBE6EF);
        } else {
            MachineArt.steel(g, x + 146, y + 50, 6, 26);
            if (v(8) != 0) for (int k = 0; k < 3; k++) {
                double t = (time / 10 + k / 3.0) % 1;
                FcsDraw.disc(g, x + 149 + (float) Math.sin(t * 6 + k) * 2, y + 48 - (float) (t * 10), 1.2F + (float) t * 2, 0x80FFFFFF);
            }
        }
        float life = v(14) / 1000F;

        Component fluid = hasSteam ? BuiltInRegistries.FLUID.byId(v(15)).getFluidType().getDescription() : Component.translatable(K + "no_steam");

        line(g, x, y, 0, Component.translatable(K + "kinetic_inlet", fluid, f("%,d", v(9)), f("%,d", v(10)), f("%.2f", v(11) / 100.0)), WindRoseView.INK);
        double quality = v(13) / 1000.0;
        line(g, x, y, 1, Component.translatable(K + "kinetic_expansion", condensed ? "0.1" : "1.0", Math.round(quality * 100) + "%",
            Math.round(life * 100) + "%", v(6), v(16)), quality < 0.88 ? 0xC0392B : WindRoseView.DIM);
        String state = v(1) == 0 ? "state.no_turbine" : v(2) != 0 ? "state.waterlogged" : v(8) != 0 ? "state.venting"
            : v(7) != 0 ? "state.throttled" : v(3) != 0 ? "state.running" : "state.idle";
        int colour = v(3) != 0 && v(2) == 0 && v(8) == 0 ? 0x2F6E4F : 0xA0602A;
        ReadoutPanel.headline(Component.literal(f("%,d KU/t", v(4))), Component.translatable(K + state), colour);   // 0.1.11.145: the output leads, the state beside it
    }

    @Override
    protected void drawStrip(GuiGraphics g, int x, int y) {
        bar(g, x, y, Component.translatable(K + "steam"), v(10) > 0 ? v(9) / (double) v(10) : 0, Component.literal(f("%,d / %,d mB", v(9), v(10))));
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        int rx = mouseX - leftPos, ry = mouseY - topPos;
        // EXPLAIN_TIP_HIDDEN (0.1.11.182): the drawing's how-it-works text, hidden for now
        // if (overMachine(rx, ry))
        //     g.renderComponentTooltip(this.font, List.of(Component.translatable(K + "tip.kinetic1"), Component.translatable(K + "tip.kinetic2"),
        //         Component.translatable(K + "tip.turbine3")), mouseX, mouseY);
    }
}
