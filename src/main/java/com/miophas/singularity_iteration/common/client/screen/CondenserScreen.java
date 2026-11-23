// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.client.screen;

import com.miophas.singularity_iteration.common.client.suit.FcsDraw;
import com.miophas.singularity_iteration.common.menu.producer.CondenserMenu;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/**
 * 0.1.11.16: the condenser's mimic: steam from its tank through the cooled tubes, the vents' fans on top, the distilled water
 * into its tank.
 *
 * <p>0.1.11.181 (176 wide, merged): the condenser drawn on the panel: the steam tank on the left, the condenser body with its
 * tubes behind a window and the four vent slots on its deck, the distilled water tank on the right with the empty container
 * over its filler and the filled one under it, the battery at the lower left cabled to the fans.
 */
@OnlyIn(Dist.CLIENT)
@SuppressWarnings("null")
public class CondenserScreen extends MimicScreen<CondenserMenu> {
    private static final String K = "gui.mio_icif.steam.";
    private static final int STEAM_CAPACITY = 10000, DISTILLED_CAPACITY = 10000, MAX_PROGRESS = 10000;
    private static final int BODY_X = 30, BODY_Y = 34, BODY_W = 84, BODY_H = 28, TANK_X = 124;

    public CondenserScreen(CondenserMenu menu, Inventory inv, Component title) { super(menu, inv, title); }

    private int vents() {
        int n = 0;
        for (int i = 0; i < 4; i++) if (!menu.slots.get(i).getItem().isEmpty()) n++;
        return n;
    }

    /** 0.1.11.151: the four vent slots show a vent's silhouette; the others their type's (the battery). */
    @Override
    protected net.minecraft.world.item.ItemStack slotGhost(net.minecraft.world.inventory.Slot slot) {
        return slot.index < 4 ? new net.minecraft.world.item.ItemStack(net.minecraft.core.registries.BuiltInRegistries.ITEM.get(
            net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("mio_icif", "reactor/item_reactor_vent"))) : super.slotGhost(slot);
    }

    @Override
    protected void drawMachine(GuiGraphics g, int x, int y, double time) {
        int vents = vents();
        boolean on = menu.isWorking();
        double flow = on ? 0.6 + 0.2 * vents : 0;
        // the steam in
        MachineArt.level(g, x + 12, y + 19, 8, 32, menu.getSteamAmount() / (double) STEAM_CAPACITY, MachineArt.STEAM);
        MachineArt.pipe(g, x + 22, x + BODY_X, y + 44, on ? MachineArt.STEAM : 0, MachineArt.phase(time, flow));
        // the body: the cooled tubes behind its window, the condensate collecting under them (the progress), the vents on its deck
        MachineArt.iron(g, x + BODY_X, y + BODY_Y, BODY_W, BODY_H);
        MachineArt.sunk(g, x + BODY_X + 3, y + BODY_Y + 3, BODY_W - 6, BODY_H - 6, MachineArt.WINDOW);
        for (int k = 0; k < 4; k++) FcsDraw.fill(g, x + BODY_X + 5, y + BODY_Y + 6 + k * 5, x + BODY_X + BODY_W - 5, y + BODY_Y + 7 + k * 5, 0xFF9CB8CC);
        float progress = (float) Math.min(1, menu.getProgress() / (double) MAX_PROGRESS);
        int pw = Math.round((BODY_W - 8) * progress);
        if (pw > 0) FcsDraw.fill(g, x + BODY_X + 4, y + BODY_Y + BODY_H - 7, x + BODY_X + 4 + pw, y + BODY_Y + BODY_H - 4, MachineArt.DISTILLED);
        for (int k = 0; k < 4; k++) {
            int vx = CondenserMenu.VENT_X + k * CondenserMenu.VENT_STEP;
            MachineArt.steel(g, x + vx - 3, y + CondenserMenu.VENT_Y - 3, 22, BODY_Y - CondenserMenu.VENT_Y + 3);
            if (on && k < vents)   // the air the fan blows up out of the vent
                for (int p = 0; p < 2; p++) {
                    int py = (int) Math.floorMod((long) Math.floor(time / 2 + p * 4 + k), 8L);
                    FcsDraw.fill(g, x + vx + 4 + p * 7, y + CondenserMenu.VENT_Y - 4 - py / 3, x + vx + 6 + p * 7, y + CondenserMenu.VENT_Y - 3 - py / 3, 0x80FFFFFF);
                }
        }
        // the distilled water out, its tank and the filler between the two container slots
        MachineArt.pipe(g, x + BODY_X + BODY_W, x + TANK_X - 2, y + 50, on ? MachineArt.DISTILLED : 0, MachineArt.phase(time, flow * 0.3));
        MachineArt.level(g, x + TANK_X, y + 19, 6, 44, menu.getDistilledAmount() / (double) DISTILLED_CAPACITY, MachineArt.DISTILLED);
        MachineArt.pipe(g, x + TANK_X + 8, x + CondenserMenu.CONTAINER_X - 2, y + 40, MachineArt.DISTILLED, -1);
        MachineArt.filler(g, x + CondenserMenu.CONTAINER_X - 2, y + CondenserMenu.CONTAINER_IN_Y + 19, 20);
        // the battery feeds the fans
        MachineArt.cable(g, x + CondenserMenu.BATTERY_X + 18, y + 66, x + BODY_X + 10, y + 66, x + BODY_X + 10, y + BODY_Y + BODY_H);

        line(g, x, y, 0, Component.translatable(K + "condenser_in", f("%,d", menu.getSteamAmount()), f("%,d", STEAM_CAPACITY),
            100 + 100 * vents), WindRoseView.INK);
        line(g, x, y, 1, Component.translatable(K + "condenser_out", f("%,d", menu.getDistilledAmount()), f("%,d", DISTILLED_CAPACITY), vents, 2 * vents),
            WindRoseView.DIM);
        // 0.1.11.145: the condensing rate is the main readout; the state beside it
        ReadoutPanel.headline(Component.literal(f("%,d mB/t", on ? 100 + 100 * vents : 0)),
            Component.translatable(K + (on ? "state.condensing" : "state.idle")), on ? 0x2F6E4F : 0xA0602A);
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
        // EXPLAIN_TIP_HIDDEN (0.1.11.182): the drawing's how-it-works text, hidden for now
        // if (overMachine(rx, ry))
        //     g.renderComponentTooltip(this.font, List.of(Component.translatable(K + "tip.condenser1"), Component.translatable(K + "tip.condenser2")), mouseX, mouseY);
    }
}
