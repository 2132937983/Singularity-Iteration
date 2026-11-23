// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.client.screen;

import com.miophas.singularity_iteration.common.client.suit.FcsDraw;
import com.miophas.singularity_iteration.common.menu.generator.EngineMenu;
import com.miophas.singularity_iteration.common.menu.generator.SteamTurbineMenu;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/**
 * 0.1.11.16: the steam turbine's mimic: the steam tank, the inlet pipe (coloured by the steam's temperature), the
 * turbine, the generator, and the exhaust going down to a condenser or out of a vent stack; under it the inlet state,
 * the expansion (back pressure, exhaust quality, enthalpy drop, EU per mB) and the output.
 *
 * <p>0.1.11.181 (176 wide, merged): the steam container slots at the tank's filler, the battery at the end of the output cable, drawn on the panel.
 */
@OnlyIn(Dist.CLIENT)
@SuppressWarnings("null")
public class SteamTurbineScreen extends MimicScreen<SteamTurbineMenu> {
    private static final String K = "gui.mio_icif.steam.";

    public SteamTurbineScreen(SteamTurbineMenu menu, Inventory inv, Component title) { super(menu, inv, title); }

    private int v(int i) { return menu.value(i); }

    @Override
    protected void drawMachine(GuiGraphics g, int x, int y, double time) {
        boolean hasSteam = v(2) >= 0 && v(0) > 0;
        int inlet = hasSteam ? MachineArt.temperature(v(11)) : 0;
        double flow = (v(6) + v(7)) / 64.0;
        boolean condensing = v(5) != 0;
        // 0.1.11.181: the steam container over the filler and the empty one under it, the tank, the turbine, the generator; the
        // exhaust down to the condenser (or the vent stack); the output cable to the battery
        MachineArt.filler(g, x + EngineMenu.SLOT_X - 2, y + EngineMenu.FUEL_Y + 19, 20);
        MachineArt.pipe(g, x + 28, x + 34, y + 39, hasSteam ? inlet : 0, -1);
        MachineArt.level(g, x + 36, y + 19, 8, 52, v(1) > 0 ? v(0) / (double) v(1) : 0, hasSteam ? inlet : MachineArt.STEAM);
        MachineArt.pipe(g, x + 46, x + 58, y + 32, flow > 0 ? inlet : 0, MachineArt.phase(time, flow));
        MachineArt.turbine(g, x + 58, y + 17, 46, 38, flow > 0 ? time : -1);
        MachineArt.shaft(g, x + 104, x + 114, y + 36);
        MachineArt.generator(g, x + 114, y + 24, 28, 24);
        int exhaust = flow > 0 ? (condensing ? MachineArt.DISTILLED : MachineArt.STEAM) : 0;
        MachineArt.pipeDown(g, x + 86, y + 53, y + 72, exhaust, MachineArt.phase(time, flow));
        MachineArt.pipe(g, x + 84, x + 120, y + 70, exhaust, MachineArt.phase(time, flow));
        if (condensing) {
            MachineArt.steel(g, x + 120, y + 62, 18, 14);
            MachineArt.sunk(g, x + 123, y + 65, 12, 8, 0xFFDBE6EF);
        } else {
            MachineArt.steel(g, x + 122, y + 52, 6, 24);
            if (v(7) > 0) for (int k = 0; k < 3; k++) {
                double t = (time / 10 + k / 3.0) % 1;
                FcsDraw.disc(g, x + 125 + (float) Math.sin(t * 6 + k) * 2, y + 50 - (float) (t * 10), 1.2F + (float) t * 2, 0x80FFFFFF);
            }
        }
        MachineArt.euCable(g, v(3) > 0, time, x + 142, y + 36, x + 158, y + 36, x + 158, y + EngineMenu.BATTERY_Y - 2);

        Component fluid = hasSteam ? BuiltInRegistries.FLUID.byId(v(2)).getFluidType().getDescription() : Component.translatable(K + "no_steam");

        line(g, x, y, 0, Component.translatable(K + "inlet", fluid, f("%.0f", v(12) / 10.0), v(11), f("%,d", v(0)), f("%,d", v(1))), WindRoseView.INK);
        double quality = v(8) / 1000.0;
        line(g, x, y, 1, Component.translatable(K + "expansion", f("%.1f", v(14) / 100.0), Math.round(quality * 100) + "%", v(13), f("%.2f", v(4) / 1000.0)),
            quality < 0.88 ? 0xC0392B : WindRoseView.DIM);
        Component exhaustLine = condensing ? Component.translatable(K + "to_condenser", v(6)) : Component.translatable(K + "vented", v(7));
        // 0.1.11.145: the output leads; the exhaust goes on its own line
        ReadoutPanel.headline(Component.literal(f("%.1f EU/t", v(3) / 100.0)), null, 0);
        line(g, x, y, 2, exhaustLine, WindRoseView.DIM);
    }

    @Override
    protected void drawStrip(GuiGraphics g, int x, int y) {
        long e = Integer.toUnsignedLong(v(9)), cap = Math.max(1, Integer.toUnsignedLong(v(10)));
        bar(g, x, y, Component.translatable(K + "stored"), e / (double) cap, stored(e, cap));
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        int rx = mouseX - leftPos, ry = mouseY - topPos;
        // EXPLAIN_TIP_HIDDEN (0.1.11.182): the drawing's how-it-works text, hidden for now
        // if (overMachine(rx, ry))
        //     g.renderComponentTooltip(this.font, List.of(Component.translatable(K + "tip.turbine1"), Component.translatable(K + "tip.turbine2"),
        //         Component.translatable(K + "tip.turbine3")), mouseX, mouseY);
    }
}
