// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.client.screen;

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
 */
@OnlyIn(Dist.CLIENT)
@SuppressWarnings("null")
public class SteamTurbineScreen extends MimicScreen<SteamTurbineMenu> {
    private static final String K = "gui.mio_icif.steam.";

    public SteamTurbineScreen(SteamTurbineMenu menu, Inventory inv, Component title) { super(menu, inv, title); }

    private int v(int i) { return menu.value(i); }

    @Override
    protected void drawWindow(GuiGraphics g, int x, int y, double time) {
        float u = WindRoseView.u();
        boolean hasSteam = v(2) >= 0 && v(0) > 0;
        int inlet = hasSteam ? PlantMimic.colour(v(11)) : 0xFFB0B6B8;
        double flow = (v(6) + v(7)) / 64.0;
        PlantMimic.tank(g, x + 6, y + 8, 8, 44, v(1) > 0 ? v(0) / (double) v(1) : 0, inlet);
        PlantMimic.pipe(g, new float[]{x + 14, y + 24, x + 46, y + 24}, inlet, 3, flow, time);
        PlantMimic.turbine(g, x + 46, y + 8, 40, 34, flow > 0 ? time : 0);
        PlantMimic.shaft(g, x + 86, y + 25, x + 104, y + 25);
        PlantMimic.machine(g, x + 114, y + 25, 9, "G", 0xFFDDE3E6, u);
        // exhaust
        boolean condensing = v(5) != 0;
        int exhaust = PlantMimic.colour(condensing ? 46 : 100);
        PlantMimic.pipe(g, new float[]{x + 76, y + 42, x + 76, y + 50, x + 140, y + 50}, exhaust, 3, flow, time);
        if (condensing) {
            PlantMimic.box(g, x + 140, y + 40, 40, 16, 0xFFDBE6EF);
            PlantMimic.label(g, Component.translatable(K + "condenser"), x + 160, y + 44, 0x1C2524);
        } else {
            PlantMimic.vent(g, x + 140, y + 34, 18, v(7) > 0, time);
            PumpCrossSection.text(g, Component.translatable(K + "to_air"), x + 148, y + 44, WindRoseView.DIM, u);
        }
        PlantMimic.label(g, Component.literal(f("%.1f EU/t", v(3) / 100.0)), x + 146, y + 21, 0x2A5D8A);   // 0.1.11.145: room for the vanilla font

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
        bar(g, x, y, Component.translatable(K + "stored"), e / (double) cap, Component.literal(f("%,d / %,d EU", e, cap)));
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        int rx = mouseX - leftPos, ry = mouseY - topPos;
        if (rx >= wx && rx < wx + ww && ry >= wy && ry < wy + wh)
            g.renderComponentTooltip(this.font, List.of(Component.translatable(K + "tip.turbine1"), Component.translatable(K + "tip.turbine2"),
                Component.translatable(K + "tip.turbine3")), mouseX, mouseY);
    }
}
