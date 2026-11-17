// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.client.screen;

import com.miophas.singularity_iteration.common.menu.producer.SteamRepressurizerMenu;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/**
 * 0.1.11.16: the steam repressurizer's mimic: steam in, the vessel heated from the sides (HU), low-pressure steam out.
 * Superheated steam expands into twice its mass of ordinary steam; ordinary steam passes through unchanged.
 */
@OnlyIn(Dist.CLIENT)
@SuppressWarnings("null")
public class RepressurizerScreen extends MimicScreen<SteamRepressurizerMenu> {
    private static final String K = "gui.mio_icif.steam.";

    public RepressurizerScreen(SteamRepressurizerMenu menu, Inventory inv, Component title) { super(menu, inv, title); }

    @Override
    protected void drawWindow(GuiGraphics g, int x, int y, double time) {
        var in = menu.getInputFluid();
        boolean hot = !in.isEmpty() && net.minecraft.core.registries.BuiltInRegistries.FLUID.getKey(in.getFluid()).getPath().contains("superheated");
        double flow = menu.getInputAmount() >= 10 && menu.getHeat() > 0 ? 0.8 : 0;
        int inColour = in.isEmpty() ? 0xFFB0B6B8 : PlantMimic.colour(hot ? 401 : 180);
        PlantMimic.tank(g, x + 6, y + 8, 8, 44, menu.getInputCapacity() > 0 ? menu.getInputAmount() / (double) menu.getInputCapacity() : 0, inColour);
        PlantMimic.pipe(g, new float[]{x + 14, y + 26, x + 60, y + 26}, inColour, 3, flow, time);
        PlantMimic.box(g, x + 60, y + 12, 50, 28, 0xFFE6DDD5);
        PlantMimic.label(g, Component.translatable(K + "vessel"), x + 85, y + 22, 0x1C2524);
        // heat from below
        for (int k = 0; k < 3; k++) {
            float hx = x + 70 + k * 14;
            com.miophas.singularity_iteration.common.client.suit.FcsDraw.line(g, hx, y + 52, hx, y + 42, 1.2F, menu.getHeat() > 0 ? PlantMimic.HEAT : 0xFFB0B6B8);
            com.miophas.singularity_iteration.common.client.suit.FcsDraw.line(g, hx - 2, y + 45, hx, y + 42, 1.2F, menu.getHeat() > 0 ? PlantMimic.HEAT : 0xFFB0B6B8);
            com.miophas.singularity_iteration.common.client.suit.FcsDraw.line(g, hx + 2, y + 45, hx, y + 42, 1.2F, menu.getHeat() > 0 ? PlantMimic.HEAT : 0xFFB0B6B8);
        }
        PlantMimic.pipe(g, new float[]{x + 110, y + 26, x + 150, y + 26}, PlantMimic.colour(180), 3, flow * (hot ? 2 : 1), time);
        PlantMimic.tank(g, x + 150, y + 8, 8, 44, menu.getOutputCapacity() > 0 ? menu.getOutputAmount() / (double) menu.getOutputCapacity() : 0, PlantMimic.colour(180));

        Component inName = in.isEmpty() ? Component.translatable(K + "no_steam") : in.getFluid().getFluidType().getDescription();
        line(g, x, y, 0, Component.translatable(K + "repress_in", inName, f("%,d", menu.getInputAmount()), f("%,d", menu.getInputCapacity())), WindRoseView.INK);
        line(g, x, y, 1, Component.translatable(K + "repress_rule"), WindRoseView.DIM);
        line(g, x, y, 2, Component.translatable(K + "repress_out", f("%,d", menu.getOutputAmount()), f("%,d", menu.getOutputCapacity())), 0x2F6E4F);
    }

    @Override
    protected void drawStrip(GuiGraphics g, int x, int y) {
        bar(g, x, y, Component.translatable(K + "heat"), menu.getMaxHeat() > 0 ? menu.getHeat() / (double) menu.getMaxHeat() : 0,
            Component.literal(f("%,d / %,d HU", menu.getHeat(), menu.getMaxHeat())));
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        int rx = mouseX - leftPos, ry = mouseY - topPos;
        if (rx >= wx && rx < wx + ww && ry >= wy && ry < wy + wh)
            g.renderComponentTooltip(this.font, List.of(Component.translatable(K + "tip.repress1"), Component.translatable(K + "tip.repress2")), mouseX, mouseY);
    }
}
