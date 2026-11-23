// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.client.screen;

import com.miophas.singularity_iteration.common.client.suit.FcsDraw;
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
 *
 * <p>0.1.11.181 (176 wide, merged): drawn on the panel, the vessel heated from below.
 */
@OnlyIn(Dist.CLIENT)
@SuppressWarnings("null")
public class RepressurizerScreen extends MimicScreen<SteamRepressurizerMenu> {
    private static final String K = "gui.mio_icif.steam.";

    public RepressurizerScreen(SteamRepressurizerMenu menu, Inventory inv, Component title) { super(menu, inv, title); }

    @Override
    protected void drawMachine(GuiGraphics g, int x, int y, double time) {
        var in = menu.getInputFluid();
        boolean hot = !in.isEmpty() && net.minecraft.core.registries.BuiltInRegistries.FLUID.getKey(in.getFluid()).getPath().contains("superheated");
        double flow = menu.getInputAmount() >= 10 && menu.getHeat() > 0 ? 0.8 : 0;
        int inColour = hot ? MachineArt.HOT_STEAM : MachineArt.STEAM;
        // 0.1.11.181: the steam in, the vessel with the heat under it, the steam out
        MachineArt.level(g, x + 12, y + 19, 8, 44, menu.getInputCapacity() > 0 ? menu.getInputAmount() / (double) menu.getInputCapacity() : 0, inColour);
        MachineArt.pipe(g, x + 22, x + 56, y + 30, flow > 0 ? inColour : 0, MachineArt.phase(time, flow));
        MachineArt.iron(g, x + 56, y + 20, 56, 34);
        MachineArt.sunk(g, x + 59, y + 23, 50, 28, MachineArt.WINDOW);
        if (menu.getInputAmount() > 0) FcsDraw.fill(g, x + 60, y + 38, x + 108, y + 50, hot ? 0xC0D9792B : 0xC0B9C3C8);
        MachineArt.iron(g, x + 60, y + 56, 48, 8);
        MachineArt.sunk(g, x + 62, y + 58, 44, 5, MachineArt.WINDOW);
        if (menu.getHeat() > 0) MachineArt.flames(g, x + 63, x + 105, y + 62, time, 5, 2);
        MachineArt.pipe(g, x + 112, x + 146, y + 30, flow > 0 ? MachineArt.STEAM : 0, MachineArt.phase(time, flow * (hot ? 2 : 1)));
        MachineArt.level(g, x + 148, y + 19, 8, 44, menu.getOutputCapacity() > 0 ? menu.getOutputAmount() / (double) menu.getOutputCapacity() : 0, MachineArt.STEAM);

        Component inName = in.isEmpty() ? Component.translatable(K + "no_steam") : in.getFluid().getFluidType().getDescription();

        line(g, x, y, 0, Component.translatable(K + "repress_in", inName, f("%,d", menu.getInputAmount()), f("%,d", menu.getInputCapacity())), WindRoseView.INK);
        line(g, x, y, 1, Component.translatable(K + "repress_rule"), WindRoseView.DIM);
        // 0.1.11.145: the steam made leads (the line said the same)
        ReadoutPanel.headline(Component.literal(f("%,d mB", menu.getOutputAmount())), Component.literal(f("/ %,d mB", menu.getOutputCapacity())), 0x5B6867);
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
        // EXPLAIN_TIP_HIDDEN (0.1.11.182): the drawing's how-it-works text, hidden for now
        // if (overMachine(rx, ry))
        //     g.renderComponentTooltip(this.font, List.of(Component.translatable(K + "tip.repress1"), Component.translatable(K + "tip.repress2")), mouseX, mouseY);
    }
}
