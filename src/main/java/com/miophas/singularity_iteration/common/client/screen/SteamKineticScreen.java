// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.client.screen;

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
 */
@OnlyIn(Dist.CLIENT)
@SuppressWarnings("null")
public class SteamKineticScreen extends MimicScreen<SteamKineticGeneratorMenu> {
    private static final String K = "gui.mio_icif.steam.";

    public SteamKineticScreen(SteamKineticGeneratorMenu menu, Inventory inv, Component title) { super(menu, inv, title); }

    private int v(int i) { return menu.value(i); }

    @Override
    protected void drawWindow(GuiGraphics g, int x, int y, double time) {
        float u = WindRoseView.u();
        boolean hot = v(0) != 0, hasSteam = v(15) >= 0 && v(9) > 0;
        int inlet = hasSteam ? PlantMimic.colour(hot ? 401 : 180) : 0xFFB0B6B8;
        double flow = v(4) > 0 ? Math.min(1.5, v(4) / 1000.0) : 0;
        PlantMimic.tank(g, x + 6, y + 8, 8, 44, v(10) > 0 ? v(9) / (double) v(10) : 0, inlet);
        PlantMimic.pipe(g, new float[]{x + 14, y + 24, x + 46, y + 24}, inlet, 3, flow, time);
        PlantMimic.turbine(g, x + 46, y + 8, 40, 34, flow > 0 ? time : 0);
        PlantMimic.shaft(g, x + 86, y + 25, x + 110, y + 25);
        PlantMimic.machine(g, x + 118, y + 25, 8, "KU", 0xFFDDE3E6, u);
        // turbine wear under the casing
        float life = v(14) / 1000F;
        g.fill(x + 46, y + 45, x + 86, y + 48, 0xFF33393A);
        WindRoseView.fill(g, x + 46, y + 45, 40 * life, 3, life > 0.25F ? 0xFF2F6E4F : 0xFFC0392B);
        // condensate tank
        PlantMimic.tank(g, x + 96, y + 36, 6, 18, v(16) > 0 ? v(6) / (double) v(16) : 0, PlantMimic.DISTILLED);
        // exhaust
        boolean condensed = v(12) != 0;
        PlantMimic.pipe(g, new float[]{x + 76, y + 42, x + 76, y + 52, x + 140, y + 52}, PlantMimic.colour(condensed ? 46 : 100), 3, flow, time);
        if (condensed) {
            PlantMimic.box(g, x + 140, y + 42, 40, 16, 0xFFDBE6EF);
            PlantMimic.label(g, Component.translatable(K + "condenser"), x + 160, y + 46, 0x1C2524);
        } else {
            PlantMimic.vent(g, x + 140, y + 36, 18, v(8) != 0, time);
            PumpCrossSection.text(g, Component.translatable(K + "to_air"), x + 148, y + 46, WindRoseView.DIM, u);
        }
        PlantMimic.label(g, Component.literal(f("%,d KU/t", v(4))), x + 118, y + 36, 0x2A5D8A);

        Component fluid = hasSteam ? BuiltInRegistries.FLUID.byId(v(15)).getFluidType().getDescription() : Component.translatable(K + "no_steam");
        line(g, x, y, 0, Component.translatable(K + "kinetic_inlet", fluid, f("%,d", v(9)), f("%,d", v(10)), f("%.2f", v(11) / 100.0)), WindRoseView.INK);
        double quality = v(13) / 1000.0;
        line(g, x, y, 1, Component.translatable(K + "kinetic_expansion", condensed ? "0.1" : "1.0", Math.round(quality * 100) + "%",
            Math.round(life * 100) + "%", v(6), v(16)), quality < 0.88 ? 0xC0392B : WindRoseView.DIM);
        String state = v(1) == 0 ? "state.no_turbine" : v(2) != 0 ? "state.waterlogged" : v(8) != 0 ? "state.venting"
            : v(7) != 0 ? "state.throttled" : v(3) != 0 ? "state.running" : "state.idle";
        int colour = v(3) != 0 && v(2) == 0 && v(8) == 0 ? 0x2F6E4F : 0xA0602A;
        line(g, x, y, 2, Component.translatable(K + state), colour);
    }

    @Override
    protected void drawStrip(GuiGraphics g, int x, int y) {
        bar(g, x, y, Component.translatable(K + "steam"), v(10) > 0 ? v(9) / (double) v(10) : 0, Component.literal(f("%,d / %,d mB", v(9), v(10))));
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        int rx = mouseX - leftPos, ry = mouseY - topPos;
        if (rx >= wx && rx < wx + ww && ry >= wy && ry < wy + wh)
            g.renderComponentTooltip(this.font, List.of(Component.translatable(K + "tip.kinetic1"), Component.translatable(K + "tip.kinetic2"),
                Component.translatable(K + "tip.turbine3")), mouseX, mouseY);
    }
}
