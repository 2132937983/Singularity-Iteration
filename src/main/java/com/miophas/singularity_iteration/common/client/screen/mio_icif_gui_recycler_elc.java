package com.miophas.singularity_iteration.common.client.screen;

import com.miophas.singularity_iteration.common.menu.MergedSlots;
import com.miophas.singularity_iteration.common.client.suit.FcsDraw;
import com.miophas.singularity_iteration.common.menu.producer.RecyclerElcMenu;
import com.miophas.singularity_iteration.common.processing.ProcessStatus;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/**
 * 回收机界面. 0.1.11.116 (processing overhaul M9): the instrument screen (docs/gui_processing_machines.md). The figure is a
 * twin-shaft shredder from the side: the two cutter rotors turn against each other under the hopper, the shreds fall
 * into the bin below, and the bin fills with the carry towards the next product. The headline bar is that carry; the
 * readouts give the yield (one in eight for scrap), the items to the next product, and the energy, power and time.
 */
@OnlyIn(Dist.CLIENT)
@SuppressWarnings("null")
public class mio_icif_gui_recycler_elc extends ProcessInstrumentScreen<RecyclerElcMenu> {

    private static final int SCRAP = 0xFF8C7B62;
    private float angle, fall;

    public mio_icif_gui_recycler_elc(RecyclerElcMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
    }

    private boolean running() { return menu.getStatus() == ProcessStatus.RUNNING; }

    @Override
    protected void drawFigure(GuiGraphics g, float cx, float cy, float dt) {
        float tau = 2 * (float) Math.PI;
        if (running()) { angle = (angle + dt * 2.5F * tau) % tau; fall = (fall + dt * 1.6F) % 1; }
        // the hopper
        FcsDraw.quad(g, cx - 30, cy - 36, cx + 30, cy - 36, cx + 22, cy - 16, cx - 22, cy - 16, SHELL);
        if (menu.getSlot(RecyclerElcMenu.INPUT_SLOT).hasItem()) FcsDraw.fill(g, (int) cx - 16, (int) cy - 30, (int) cx + 16, (int) cy - 20, FormingFigure.WORK);
        // the housing and the two rotors turning against each other
        FcsDraw.fill(g, (int) cx - 30, (int) cy - 16, (int) cx + 30, (int) cy + 12, SHELL);
        FcsDraw.fill(g, (int) cx - 28, (int) cy - 14, (int) cx + 28, (int) cy + 10, CHAMBER);
        for (int side = -1; side <= 1; side += 2) {
            float rx = cx + side * 12, a = side * angle;
            FcsDraw.disc(g, rx, cy - 2, 9, STEEL_LIGHT);
            for (int k = 0; k < 6; k++) blade(g, rx, cy - 2, a + k * tau / 6, 8, 12, 2.2F, 0.6F, STEEL_LIGHT);
            FcsDraw.disc(g, rx, cy - 2, 3, STEEL);
        }
        // the shreds falling into the bin
        if (running()) for (int k = 0; k < 4; k++) {
            float f = (fall + k * 0.25F) % 1;
            FcsDraw.disc(g, cx - 6 + k * 4, cy + 12 + f * 12, 1.3F, FormingFigure.WORK);
        }
        // the bin, filled to the carry towards the next product
        int bx0 = (int) cx - 24, bx1 = (int) cx + 24, by0 = (int) cy + 24, by1 = (int) cy + 38;
        FcsDraw.fill(g, bx0, by0, bx1, by1, SHELL);
        int h = Math.round((by1 - by0 - 3) * Math.min(1, menu.getCarryPermille() / 1000F));
        if (h > 0) FcsDraw.fill(g, bx0 + 2, by1 - 2 - h, bx1 - 2, by1 - 2, SCRAP);
    }

    // ---- 0.1.11.178: the merged drawing (panel coordinates; the slots are MergedSlots.Shredder's); 0.1.11.180: 176 wide ----
    // The shredder: the input in the hopper on top, the two cutter shafts turning against each other behind the window,
    // the motor on the left wired to the battery, the shreds falling into the bin (filled to the carry towards the next
    // scrap), the arrow to the tray.
    @Override protected int arrowX() { return 118; }
    @Override protected int arrowY() { return MergedSlots.Shredder.OUT_Y + 1; }
    @Override protected int[] machineBody() { return new int[]{30, 40, 84, 54}; }

    @Override
    protected void drawMachine(GuiGraphics g, float dt) {
        float tau = 2 * (float) Math.PI;
        boolean run = running();
        if (run) { angle = (angle + dt * 2.5F * tau) % tau; fall = (fall + dt * 1.6F) % 1; }
        MachineArt.cable(g, 28, 82, 39, 82, 39, 68);
        MachineArt.motor(g, 30, 46, 20, 24);
        MachineArt.iron(g, 50, 40, 64, 32);                                           // the cutter housing
        FcsDraw.fill(g, 54, 44, 110, 68, MachineArt.WINDOW);
        for (int side = -1; side <= 1; side += 2) {
            float rx = 82 + side * 13, a = side * angle;
            FcsDraw.disc(g, rx, 56, 8, MachineArt.STEEL_LIGHT);
            for (int k = 0; k < 6; k++) blade(g, rx, 56, a + k * tau / 6, 7, 11, 2, 0.6F, MachineArt.STEEL_LIGHT);
            FcsDraw.disc(g, rx, 56, 3, MachineArt.STEEL);
        }
        MachineArt.hopper(g, MergedSlots.Shredder.IN_X, MergedSlots.Shredder.IN_Y, 40);
        MachineArt.sunk(g, 54, 74, 56, 20, MachineArt.GLASS);                         // the bin
        int h = Math.round(17 * Math.min(1, menu.getCarryPermille() / 1000F));
        if (h > 0) FcsDraw.fill(g, 55, 93 - h, 109, 93, SCRAP);
        if (run) for (int k = 0; k < 4; k++) {
            float f = (fall + k * 0.25F) % 1;
            FcsDraw.disc(g, 76 + k * 4, 70 + f * 12, 1.3F, MachineArt.WORK);
        }
        MachineArt.tray(g, MergedSlots.Shredder.OUT_X, MergedSlots.Shredder.OUT_Y);
    }

    @Override
    protected Component headline() {
        return Component.translatable(K + (running() ? "recycling" : "stopped"));
    }

    @Override
    protected int headlineColour() { return running() ? INK : DIM; }

    @Override
    protected float headlineBar() { return menu.getCarryPermille() / 1000F; }

    @Override
    protected List<Component> readouts() {
        List<Component> lines = new ArrayList<>();
        int share = menu.getYieldPermille();
        if (share > 0) {
            lines.add(Component.translatable(K + "yield_line", num(share / 10.0), num(1000.0 / share)));
            int left = (int) Math.ceil((1000.0 - menu.getCarryPermille()) / share - 1e-6);
            lines.add(Component.translatable(K + "carry_line", String.format(java.util.Locale.ROOT, "%.3f", menu.getCarryPermille() / 1000.0), Math.max(1, left)));
        }
        int max = menu.getMaxProgress();
        lines.add(Component.translatable(K + "energy_line", (long) menu.getPower() * max, menu.getPower(), max));
        return lines;
    }

    /** A whole number without the decimal (12.5, 8). */
    private static String num(double v) { return Math.abs(v - Math.round(v)) < 0.05 ? Long.toString(Math.round(v)) : f1(v); }

    @Override
    protected int status() { return menu.getStatus(); }

    @Override
    protected double energyFraction() { return menu.getEnergyPermille() / 1000.0; }

    @Override
    protected List<Component> figureTooltip() {
        return List.of(Component.translatable(K + "tip.recycler.1"), Component.translatable(K + "tip.recycler.2"));
    }
}
