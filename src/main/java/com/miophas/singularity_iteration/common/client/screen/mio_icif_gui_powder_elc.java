package com.miophas.singularity_iteration.common.client.screen;

import com.miophas.singularity_iteration.common.menu.MergedSlots;
import com.miophas.singularity_iteration.common.client.suit.FcsDraw;
import com.miophas.singularity_iteration.common.menu.producer.PowderElcMenu;
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
 * 打粉机界面. 0.1.11.110 (processing overhaul M3): the instrument screen (docs/gui_processing_machines.md). The figure is
 * the hammer mill seen from above: a round chamber with the screen holes around it and the rotor with four swing hammers,
 * turning while the machine runs (a fixed speed: the macerator has no rotor inertia). The readouts give the input and its
 * work index, the energy of the operation, the power and the time.
 */
@OnlyIn(Dist.CLIENT)
@SuppressWarnings("null")
public class mio_icif_gui_powder_elc extends ProcessInstrumentScreen<PowderElcMenu> {

    /** Rotor turns per second while running (the figure; about 1,450 rpm would be a blur). */
    private static final float TURNS_PER_SECOND = 1.1F;
    private float angle;

    public mio_icif_gui_powder_elc(PowderElcMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
    }

    private boolean running() { return menu.getStatus() == ProcessStatus.RUNNING; }

    @Override
    protected void drawFigure(GuiGraphics g, float cx, float cy, float dt) {
        if (running()) angle = (angle + dt * TURNS_PER_SECOND * 2 * (float) Math.PI) % (2 * (float) Math.PI);
        float r = 34;
        FcsDraw.disc(g, cx, cy, r + 5, SHELL);
        FcsDraw.disc(g, cx, cy, r, CHAMBER);
        for (int k = 0; k < 24; k++) {   // the screen (grate) holes
            double a = k / 24.0 * Math.PI * 2;
            FcsDraw.disc(g, cx + (float) Math.cos(a) * (r - 2.5F), cy + (float) Math.sin(a) * (r - 2.5F), 1.2F, SHELL);
        }
        int hammer = running() ? 0xFF000000 | AMBER : STEEL_LIGHT;
        for (int k = 0; k < 4; k++) {
            float a = angle + k * (float) Math.PI / 2;
            blade(g, cx, cy, a, 8, 18, 2, 2, STEEL);        // the arm
            blade(g, cx, cy, a, 17, 26, 4, 4, hammer);      // the hammer
        }
        FcsDraw.disc(g, cx, cy, 9, STEEL);
        FcsDraw.disc(g, cx, cy, 3, STEEL_LIGHT);
    }

    // ---- 0.1.11.178: the merged drawing (panel coordinates; the slots are MergedSlots.Mill's); 0.1.11.180: 176 wide ----
    // The hammer mill from the front: the input in the hopper on top, the motor on the left wired to the battery, the
    // rotor and its hammers turning in the dark chamber behind the screen, the discharge on the right, the arrow to the tray.
    private static final int MILL_X = 81, MILL_Y = 67;
    private static final int[][] CASING_BOLTS = {{57, 43}, {103, 43}, {57, 89}, {103, 89}};

    @Override protected int arrowX() { return 120; }
    @Override protected int arrowY() { return MergedSlots.Mill.OUT_Y + 1; }
    @Override protected int[] machineBody() { return new int[]{32, 40, 86, 54}; }

    @Override
    protected void drawMachine(GuiGraphics g, float dt) {
        boolean run = running();
        if (run) angle = (angle + dt * TURNS_PER_SECOND * 2 * (float) Math.PI) % (2 * (float) Math.PI);
        MachineArt.cable(g, 28, 82, 34, 82);
        MachineArt.motor(g, 32, 66, 20, 24);
        FcsDraw.fill(g, 52, 74, 56, 80, MachineArt.STEEL_DARK);                     // the coupling
        MachineArt.iron(g, 54, 40, 54, 54);                                          // the casing
        FcsDraw.disc(g, MILL_X, MILL_Y, 22, MachineArt.WINDOW);                      // the chamber
        for (int k = 0; k < 18; k++) {                                               // the screen
            double a = k * 2 * Math.PI / 18;
            int px = (int) (MILL_X + Math.cos(a) * 19.5), py = (int) (MILL_Y + Math.sin(a) * 19.5);
            FcsDraw.fill(g, px, py, px + 1, py + 1, VanillaFrame.SLOT);
        }
        for (int k = 0; k < 4; k++) {                                                // the arms and the hammers
            float a = angle + k * (float) Math.PI / 2;
            blade(g, MILL_X, MILL_Y, a, 4, 12, 1.5F, 1.5F, MachineArt.STEEL);
            blade(g, MILL_X, MILL_Y, a, 11, 17, 3, 3, run ? MachineArt.AMBER : MachineArt.STEEL_LIGHT);
        }
        FcsDraw.disc(g, MILL_X, MILL_Y, 5, MachineArt.STEEL);
        FcsDraw.disc(g, MILL_X, MILL_Y, 2, MachineArt.STEEL_LIGHT);
        for (int[] b : CASING_BOLTS) FcsDraw.fill(g, b[0], b[1], b[0] + 2, b[1] + 2, MachineArt.IRON_DARK);
        MachineArt.hopper(g, MergedSlots.Mill.IN_X, MergedSlots.Mill.IN_Y, 40);
        MachineArt.steel(g, 108, 70, 10, 10);                                        // the discharge
        FcsDraw.fill(g, 114, 72, 118, 78, MachineArt.WINDOW);
        MachineArt.tray(g, MergedSlots.Mill.OUT_X, MergedSlots.Mill.OUT_Y);
    }

    @Override
    protected Component headline() {
        return Component.translatable(K + (running() ? "turning" : "stopped"));
    }

    @Override
    protected int headlineColour() { return running() ? INK : DIM; }

    @Override
    protected List<Component> readouts() {
        List<Component> lines = new ArrayList<>();
        ItemStack input = menu.getSlot(PowderElcMenu.INPUT_SLOT).getItem();
        if (input.isEmpty()) lines.add(Component.translatable(K + "no_material"));
        else lines.add(Component.translatable(K + "material", input.getHoverName(), f1(menu.getWorkIndexTenths() / 10.0)));
        lines.add(Component.translatable(K + "energy_line", menu.getEnergyPerOperation(), 2, menu.getMaxProgress()));
        return lines;
    }

    @Override
    protected int status() { return menu.getStatus(); }

    @Override
    protected List<Component> figureTooltip() {
        return List.of(Component.translatable(K + "tip.hammer.1"), Component.translatable(K + "tip.hammer.2"), Component.translatable(K + "tip.bond"));
    }
}
