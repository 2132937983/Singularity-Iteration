package com.miophas.singularity_iteration.common.client.screen;

import com.miophas.singularity_iteration.common.menu.MergedSlots;
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
        FcsDraw.fill(g, x0, y0, x1, y1, STEEL);                               // shell
        FcsDraw.fill(g, x0 + 6, y0 + 6, x1 - 6, y1 - 6, 0xFF2A2F2E);          // chamber
        int heat = glow(t);
        if (t > 300) FcsDraw.fill(g, x0 + 6, y0 + 6, x1 - 6, y1 - 6, (Math.min(0x90, (int) ((t - 300) / 800.0 * 0x90)) << 24) | (heat & 0xFFFFFF));
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
        FcsDraw.fill(g, x0 + 14, (int) cy + 12, x1 - 14, (int) cy + 15, STEEL_LIGHT);   // the hearth
        ItemStack input = menu.getSlot(mio_icif_furnace_elc.INPUT_SLOT).getItem();
        if (!input.isEmpty()) g.renderItem(input, (int) cx - 8, (int) cy - 5);
    }

    // ---- 0.1.11.178: the merged drawing (panel coordinates; the slots are MergedSlots.Oven's); 0.1.11.180: 176 wide ----
    // The furnace in brick with the chamber open: the input's slot on the hearth, the heating coils above and below
    // glowing with the temperature (the chamber too), the controller on the left wired to the battery, the arrow out to
    // the tray.
    private static final int BRICK = 0xFF985242, BRICK_LIGHT = 0xFFBC7A66, BRICK_DARK = 0xFF5A2C1E, MORTAR = 0xFF7A3E2E;

    @Override protected int arrowX() { return 122; }
    @Override protected int arrowY() { return MergedSlots.Oven.OUT_Y + 1; }
    @Override protected int[] machineBody() { return new int[]{46, 18, 74, 77}; }

    @Override
    protected void drawMachine(GuiGraphics g, float dt) {
        int t = menu.getTemperature();
        int code = menu.getStatus();
        boolean on = code == ProcessStatus.RUNNING || code == ProcessStatus.HEATING;
        MachineArt.cable(g, 28, 82, 32, 82);
        MachineArt.steel(g, 31, 64, 14, 26);                                          // the controller
        FcsDraw.fill(g, 34, 68, 42, 72, on ? 0xFF000000 | GREEN : MachineArt.WINDOW);
        FcsDraw.fill(g, 34, 76, 37, 78, on ? MachineArt.AMBER : MachineArt.STEEL_DARK);
        FcsDraw.fill(g, 39, 76, 42, 78, MachineArt.STEEL_DARK);
        MachineArt.raised(g, 46, 18, 74, 77, BRICK, BRICK_LIGHT, BRICK_DARK);         // the brick shell
        for (int y = 22; y < 92; y += 5) {
            FcsDraw.fill(g, 48, y, 118, y + 1, MORTAR);
            for (int x = 48 + ((y / 5) % 2 == 0 ? 0 : 6); x < 118; x += 12) FcsDraw.fill(g, x, y + 1, x + 1, y + 5, MORTAR);
        }
        MachineArt.sunk(g, 56, 26, 54, 62, MachineArt.WINDOW);                        // the chamber
        int heat = glow(t);
        if (t > 300) FcsDraw.fill(g, 57, 27, 109, 87, (Math.min(0x90, (int) ((t - 300) / 800.0 * 0x90)) << 24) | (heat & 0xFFFFFF));
        for (int y : new int[]{32, 81}) {                                             // the coils
            float px = 60, py = y;
            for (int k = 1; k <= 10; k++) {
                float nx = 60 + k * 46 / 10F, ny = y + (k % 2 == 0 ? -2.5F : 2.5F);
                FcsDraw.line(g, px, py, nx, ny, 1.4F, heat);
                px = nx;
                py = ny;
            }
        }
        MachineArt.steel(g, 65, 67, 36, 4);                                           // the hearth
        MachineArt.tray(g, MergedSlots.Oven.OUT_X, MergedSlots.Oven.OUT_Y);
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
        // 0.1.11.143: an icon in the figure window's corner; the stored experience is in its tooltip
        iconButton(g, 0, ICON_XP, false, FurnaceElcMenu.BUTTON_COLLECT_XP, mouseX, mouseY,
            List.of(Component.translatable(K + "collect_xp", f1(menu.getStoredXpTenths() / 10.0)),
                Component.translatable(K + "tip.collect_xp").withStyle(net.minecraft.ChatFormatting.GRAY)));
    }

    @Override
    protected int buttonCount() { return 1; }

    @Override
    protected List<Component> figureTooltip() {
        return List.of(Component.translatable(K + "tip.furnace.1"), Component.translatable(K + "tip.furnace.2"));
    }
}
