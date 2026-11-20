package com.miophas.singularity_iteration.common.client.screen;

import com.miophas.singularity_iteration.common.blockentity.producer.mio_icif_furnace_elc;
import com.miophas.singularity_iteration.common.client.suit.FcsDraw;
import com.miophas.singularity_iteration.common.menu.producer.FurnaceElcMenu;
import com.miophas.singularity_iteration.common.processing.ProcessStatus;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/**
 * 电炉界面. 0.1.11.113 (processing overhaul M6): the instrument screen (docs/gui_processing_machines.md). The figure is the
 * resistance furnace from the front: the chamber with the heating coils above and below glowing with the chamber
 * temperature and the item on the hearth. The headline is the chamber temperature with a bar and a red mark at the
 * temperature the item needs (food 250 °C, the rest 1,000 °C). A flat button collects the banked experience.
 */
@OnlyIn(Dist.CLIENT)
@SuppressWarnings("null")
public class mio_icif_gui_furnace_elc extends ProcessInstrumentScreen<FurnaceElcMenu> {

    public mio_icif_gui_furnace_elc(FurnaceElcMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
    }

    @Override
    protected void drawFigure(GuiGraphics g, float cx, float cy, float dt) {
        int t = menu.getTemperature();
        int x0 = (int) cx - 38, x1 = (int) cx + 38, y0 = (int) cy - 32, y1 = (int) cy + 34;
        g.fill(x0, y0, x1, y1, STEEL);                               // shell
        g.fill(x0 + 6, y0 + 6, x1 - 6, y1 - 6, 0xFF2A2F2E);          // chamber
        int heat = glow(t);
        if (t > 300) g.fill(x0 + 6, y0 + 6, x1 - 6, y1 - 6, (Math.min(0x90, (int) ((t - 300) / 800.0 * 0x90)) << 24) | (heat & 0xFFFFFF));
        for (int row = 0; row < 2; row++) {                          // the heating coils, top and bottom
            float y = row == 0 ? y0 + 10 : y1 - 10;
            float px = x0 + 10, py = y;
            for (int k = 1; k <= 12; k++) {
                float nx = x0 + 10 + k * (x1 - x0 - 20) / 12F, ny = y + (k % 2 == 0 ? -2.5F : 2.5F);
                FcsDraw.line(g, px, py, nx, ny, 1.4F, heat);
                px = nx;
                py = ny;
            }
        }
        g.fill(x0 + 14, (int) cy + 12, x1 - 14, (int) cy + 15, STEEL_LIGHT);   // the hearth
        ItemStack input = menu.getSlot(mio_icif_furnace_elc.INPUT_SLOT).getItem();
        if (!input.isEmpty()) g.renderItem(input, (int) cx - 8, (int) cy - 5);
    }

    @Override
    protected Component headline() {
        return Component.translatable(K + "chamber", menu.getTemperature());
    }

    @Override
    protected int headlineColour() { return INK; }

    @Override
    protected float headlineBar() { return (float) (menu.getTemperature() / mio_icif_furnace_elc.MAX_TEMPERATURE); }

    @Override
    protected float headlineBarMarker() { return (float) (menu.getRequiredTemperature() / mio_icif_furnace_elc.MAX_TEMPERATURE); }

    @Override
    protected List<Component> readouts() {
        List<Component> lines = new ArrayList<>();
        ItemStack input = menu.getSlot(mio_icif_furnace_elc.INPUT_SLOT).getItem();
        if (input.isEmpty()) lines.add(Component.translatable(K + "no_material"));
        else lines.add(Component.translatable(K + "needs_temperature", input.getHoverName(), menu.getRequiredTemperature()));
        lines.add(Component.translatable(K + "energy_line", mio_icif_furnace_elc.DEFAULT_ENERGY_PER_TICK * mio_icif_furnace_elc.DEFAULT_COOK_TIME,
            mio_icif_furnace_elc.DEFAULT_ENERGY_PER_TICK, menu.getMaxProgress()));
        return lines;
    }

    @Override
    protected int status() { return menu.getStatus(); }

    @Override
    protected void drawExtras(GuiGraphics g, int x, int y, int mouseX, int mouseY) {
        int bx = x - leftPos, by = Math.min(y - topPos, PROGRESS_Y - 14);
        menuButton(g, bx, by, 64, 11, Component.translatable(K + "collect_xp", f1(menu.getStoredXpTenths() / 10.0)), false,
            FurnaceElcMenu.BUTTON_COLLECT_XP, mouseX, mouseY, List.of(Component.translatable(K + "tip.collect_xp")));
    }

    @Override
    protected List<Component> figureTooltip() {
        return List.of(Component.translatable(K + "tip.furnace.1"), Component.translatable(K + "tip.furnace.2"));
    }
}
