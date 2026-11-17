// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.client.screen;

import com.miophas.singularity_iteration.common.menu.producer.CondenserMenu;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/**
 * 0.1.11.16: the condenser's mimic: exhaust steam coming in, the condenser shell with its vent fans and the condensing
 * progress, the distilled water going out to its tank; a turbine beside it expands to the condenser's vacuum.
 */
@OnlyIn(Dist.CLIENT)
@SuppressWarnings("null")
public class CondenserScreen extends MimicScreen<CondenserMenu> {
    private static final String K = "gui.mio_icif.steam.";
    private static final int STEAM_CAPACITY = 10000, DISTILLED_CAPACITY = 10000, MAX_PROGRESS = 10000;

    public CondenserScreen(CondenserMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        wx = 54;
        ww = 187;
    }

    private int vents() {
        int n = 0;
        for (int i = 0; i < 4; i++) if (!menu.slots.get(i).getItem().isEmpty()) n++;
        return n;
    }

    @Override
    protected void drawWindow(GuiGraphics g, int x, int y, double time) {
        float u = WindRoseView.u();
        int vents = vents();
        double flow = menu.isWorking() ? 0.6 + 0.2 * vents : 0;
        PlantMimic.tank(g, x + 6, y + 8, 8, 44, menu.getSteamAmount() / (double) STEAM_CAPACITY, PlantMimic.STEAM);
        PlantMimic.pipe(g, new float[]{x + 14, y + 26, x + 50, y + 26}, PlantMimic.colour(100), 3, flow, time);
        PlantMimic.box(g, x + 50, y + 12, 70, 30, 0xFFDBE6EF);
        // cooling tubes and the vent fans on top
        for (int k = 0; k < 5; k++) g.fill(x + 56, y + 16 + k * 5, x + 114, y + 17 + k * 5, 0xFF9CB8CC);
        for (int k = 0; k < 4; k++) {
            float cx = x + 60 + k * 16, cy = y + 8;
            boolean on = k < vents;
            com.miophas.singularity_iteration.common.client.suit.FcsDraw.circle(g, cx, cy, 3.5F, 0.6F, on ? PlantMimic.OUTLINE : 0xFFB0B6B8);
            if (on && menu.isWorking()) {
                double a = time * 0.6 + k;
                com.miophas.singularity_iteration.common.client.suit.FcsDraw.line(g, cx - (float) Math.cos(a) * 3, cy - (float) Math.sin(a) * 3,
                    cx + (float) Math.cos(a) * 3, cy + (float) Math.sin(a) * 3, 0.6F, PlantMimic.OUTLINE);
            }
        }
        float progress = (float) Math.min(1, menu.getProgress() / (double) MAX_PROGRESS);
        g.fill(x + 50, y + 44, x + 120, y + 47, 0xFF33393A);
        WindRoseView.fill(g, x + 50, y + 44, 70 * progress, 3, PlantMimic.DISTILLED);
        PlantMimic.pipe(g, new float[]{x + 120, y + 30, x + 150, y + 30}, PlantMimic.DISTILLED, 3, flow * 0.3, time);
        PlantMimic.tank(g, x + 150, y + 8, 8, 44, menu.getDistilledAmount() / (double) DISTILLED_CAPACITY, PlantMimic.DISTILLED);

        line(g, x, y, 0, Component.translatable(K + "condenser_in", f("%,d", menu.getSteamAmount()), f("%,d", STEAM_CAPACITY),
            100 + 100 * vents), WindRoseView.INK);
        line(g, x, y, 1, Component.translatable(K + "condenser_out", f("%,d", menu.getDistilledAmount()), f("%,d", DISTILLED_CAPACITY), vents, 2 * vents),
            WindRoseView.DIM);
        line(g, x, y, 2, Component.translatable(K + (menu.isWorking() ? "state.condensing" : "state.idle")), menu.isWorking() ? 0x2F6E4F : 0xA0602A);
    }

    @Override
    protected void drawStrip(GuiGraphics g, int x, int y) {
        long e = Integer.toUnsignedLong(menu.getEnergy()), cap = Math.max(1, Integer.toUnsignedLong(menu.getMaxEnergy()));
        bar(g, x, y, Component.translatable(K + "stored"), e / (double) cap, Component.literal(f("%,d / %,d EU", e, cap)));
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        int rx = mouseX - leftPos, ry = mouseY - topPos;
        if (rx >= wx && rx < wx + ww && ry >= wy && ry < wy + wh)
            g.renderComponentTooltip(this.font, List.of(Component.translatable(K + "tip.condenser1"), Component.translatable(K + "tip.condenser2")), mouseX, mouseY);
    }
}
