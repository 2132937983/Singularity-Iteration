package com.miophas.singularity_iteration.common.client.screen;

import com.miophas.singularity_iteration.common.client.suit.FcsDraw;
import com.miophas.singularity_iteration.common.menu.producer.CannerElcMenu;
import com.miophas.singularity_iteration.common.processing.ProcessStatus;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.fluids.FluidStack;

/**
 * 装罐机界面. 0.1.11.116 (processing overhaul M9): the instrument screen (docs/gui_processing_machines.md). The headline is
 * the mode; the buttons switch the mode and swap the tanks.
 *
 * <p>0.1.11.175: the first merged screen. The figure window took the panel's right two thirds for a bare drawing while the
 * slots that matter stood small in the corner; now the machine is drawn on the panel with its slots in it, in pixel art:
 * the input tank on the left and the output tank on the right as full-height gauges, the hopper with the material slot on
 * top, the filler head over the can slot, the progress arrow to the output slot, the energy bolt over the battery slot at
 * the far left. The pipes the mode works carry water (with running light while it runs): "tank to cell" and mixing from
 * the left tank into the filler, "cell to tank" and mixing from the filler into the right tank.
 */
@OnlyIn(Dist.CLIENT)
@SuppressWarnings("null")
public class mio_icif_gui_canner_elc extends ProcessInstrumentScreen<CannerElcMenu> {

    private static final String[] MODES = {"mode_canning", "mode_empty_to_tank", "mode_fill_from_tank", "mode_mix"};
    private float flow;

    public mio_icif_gui_canner_elc(CannerElcMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
    }

    private boolean running() { return menu.getStatus() == ProcessStatus.RUNNING; }
    private int mode() { return Math.max(0, Math.min(3, menu.getMode())); }

    // ---- 0.1.11.175: the merged layout (panel coordinates; the slots are CannerElcMenu's) ----
    // 0.1.11.180: 176 wide (the tanks narrower, the machine between them closer)
    private static final int TANK_IN_X = 34, TANK_OUT_X = 152, TANK_Y = 20, TANK_W = 12, TANK_H = 70;
    private static final int PIPE_Y = 26, FILL_Y = 44, ELBOW_X = 62;
    // 0.1.11.176: the bolt stands right on the battery slot (no gap), as in every merged screen
    private static final int MERGED_BOLT_X = com.miophas.singularity_iteration.common.menu.MergedSlots.BOLT_X,
        MERGED_BOLT_Y = com.miophas.singularity_iteration.common.menu.MergedSlots.BOLT_Y, MERGED_ARROW_X = 100, MERGED_ARROW_Y = 56;

    @Override protected boolean merged() { return true; }
    @Override protected int boltX() { return MERGED_BOLT_X; }
    @Override protected int boltY() { return MERGED_BOLT_Y; }
    @Override protected int arrowX() { return MERGED_ARROW_X; }
    @Override protected int arrowY() { return MERGED_ARROW_Y; }
    /** The mode and swap buttons; 0.1.11.180: side by side right of the hopper (the output tank stands at the right edge). */
    @Override protected int iconButtonX(int index) { return 106 + (1 - index) * 13; }
    @Override protected int iconButtonY(int index) { return 19; }

    @Override
    protected void drawMerged(GuiGraphics g, int x, int y, float dt) {
        boolean run = running();
        if (run) flow = (flow + dt * 1.2F) % 1;
        float moving = run ? flow : -1;
        int m = mode();
        boolean fromLeft = m == 2 || m == 3, toRight = m == 1 || m == 3;
        // the tanks at the two ends
        MachineArt.tank(g, x + TANK_IN_X, y + TANK_Y, TANK_W, TANK_H, menu.getInputFluid(), menu.getInputFluidCapacity());
        MachineArt.tank(g, x + TANK_OUT_X, y + TANK_Y, TANK_W, TANK_H, menu.getOutputFluid(), menu.getOutputFluidCapacity());
        // the pipes: input tank -> filler head (over and down), filler head -> output tank (over the output slot)
        int inEnd = TANK_IN_X + TANK_W + 2, outStart = TANK_OUT_X - 2, mx = CannerElcMenu.MATERIAL_X, cy = CannerElcMenu.CAN_Y;
        MachineArt.pipeH(g, x + inEnd, x + ELBOW_X + 2, y + PIPE_Y, fromLeft, moving);
        MachineArt.pipeV(g, x + ELBOW_X, y + PIPE_Y - 2, y + FILL_Y + 2, fromLeft, moving);
        MachineArt.pipeH(g, x + ELBOW_X - 2, x + mx - 8, y + FILL_Y, fromLeft, moving);
        MachineArt.pipeH(g, x + mx + 24, x + outStart, y + FILL_Y, toRight, moving);
        // the hopper round the material slot (shut in the cell modes, which use no material)
        MachineArt.steel(g, x + mx - 6, y + CannerElcMenu.MATERIAL_Y - 3, 28, 22);
        if (m == 1 || m == 2) VanillaFrame.inset(g, x + mx - 1, y + CannerElcMenu.MATERIAL_Y - 1, 18, 18, MachineArt.STEEL_DARK);
        // the filler head, its nozzle over the can, the stand under it
        MachineArt.steel(g, x + mx - 8, y + FILL_Y - 5, 32, 10);
        g.fill(x + mx + 5, y + FILL_Y + 5, x + mx + 11, y + cy - 1, MachineArt.STEEL_DARK);
        g.fill(x + mx + 6, y + FILL_Y + 5, x + mx + 10, y + cy - 1, run ? MachineArt.WATER : MachineArt.STEEL);
        MachineArt.steel(g, x + mx - 6, y + cy + 18, 28, 5);
        // the energy bolt over the battery slot, the progress arrow to the output slot
        VanillaFrame.bolt(g, x + MERGED_BOLT_X, y + MERGED_BOLT_Y, energyFraction());
        int max = menu.getMaxProgress();
        VanillaFrame.arrow(g, x + MERGED_ARROW_X, y + MERGED_ARROW_Y, max > 0 ? (double) menu.getProgress() / max : 0);
    }

    /** The tanks tell their fluid and amount. */
    @Override
    protected List<Component> mergedTooltip(int rx, int ry) {
        if (inside(rx, ry, TANK_IN_X - 2, TANK_Y - 2, TANK_W + 4, TANK_H + 4))
            return List.of(tankLine("tank_in", menu.getInputFluid(), menu.getInputFluidAmount(), menu.getInputFluidCapacity()));
        if (inside(rx, ry, TANK_OUT_X - 2, TANK_Y - 2, TANK_W + 4, TANK_H + 4))
            return List.of(tankLine("tank_out", menu.getOutputFluid(), menu.getOutputFluidAmount(), menu.getOutputFluidCapacity()));
        return null;
    }

    /** The merged screen draws no figure window (see {@link #drawMerged}). */
    @Override
    protected void drawFigure(GuiGraphics g, float cx, float cy, float dt) { }

    @Override
    protected Component headline() {
        return Component.translatable("gui.mio_icif.canner." + MODES[mode()]);
    }

    @Override
    protected int headlineColour() { return INK; }   // 0.1.11.141: the mode always leads; the state is beside it

    private static Component tankLine(String key, FluidStack fluid, int amount, int capacity) {
        Component name = fluid.isEmpty() ? Component.literal("") : fluid.getHoverName();
        return Component.translatable(K + key, String.format(Locale.ROOT, "%,d", amount), String.format(Locale.ROOT, "%,d", capacity), name);
    }

    @Override
    protected List<Component> readouts() {
        // 0.1.11.141: a tank's line only when it holds something or the mode works it (an empty idle tank says nothing)
        List<Component> lines = new ArrayList<>();
        int m = mode();
        if (!menu.getInputFluid().isEmpty() || m == 2 || m == 3)
            lines.add(tankLine("tank_in", menu.getInputFluid(), menu.getInputFluidAmount(), menu.getInputFluidCapacity()));
        if (!menu.getOutputFluid().isEmpty() || m == 1 || m == 3)
            lines.add(tankLine("tank_out", menu.getOutputFluid(), menu.getOutputFluidAmount(), menu.getOutputFluidCapacity()));
        int max = menu.getMaxProgress();
        lines.add(Component.translatable(K + "energy_line", (long) menu.getPower() * max, menu.getPower(), max));
        return lines;
    }

    @Override
    protected int status() { return menu.getStatus(); }

    @Override
    protected double energyFraction() { return menu.getEnergyPermille() / 1000.0; }

    @Override
    protected void drawExtras(GuiGraphics g, int x, int y, int mouseX, int mouseY) {
        // 0.1.11.141: two icons in the figure window's corner (the names move to the tooltips)
        // 0.1.11.150: the canner's own modes (it showed the metal former's "rolling, cutting, extruding")
        Component[] modes = new Component[MODES.length + 1];
        for (int i = 0; i < MODES.length; i++) modes[i] = Component.translatable("gui.mio_icif.canner." + MODES[i]);
        modes[MODES.length] = Component.translatable("gui.mio_icif.canner." + MODES[mode()]);
        iconButton(g, 1, ICON_CYCLE, false, 0, mouseX, mouseY,
            List.of(Component.translatable(K + "switch_mode"),
                Component.translatable("gui.mio_icif.canner.tip.switch_mode", (Object[]) modes).withStyle(net.minecraft.ChatFormatting.GRAY)));
        iconButton(g, 0, ICON_SWAP, false, 1, mouseX, mouseY,
            List.of(Component.translatable(K + "swap"), Component.translatable("gui.mio_icif.canner.swap_fluids").withStyle(net.minecraft.ChatFormatting.GRAY)));
    }

    @Override
    protected List<Component> figureTooltip() {
        return List.of(Component.translatable(K + "tip.canner.1"), Component.translatable(K + "tip.canner.2"));
    }

    // the material slot is used only for canning and mixing (0.1.11.142: hidden, frame and all, in both cell modes)
    private boolean materialUsed() { int m = mode(); return m == 0 || m == 3; }

    private boolean hiddenSlot(Slot slot) { return slot.index == CannerElcMenu.MATERIAL_SLOT && !materialUsed() && !slot.hasItem(); }

    @Override
    protected boolean slotShown(Slot slot) { return !hiddenSlot(slot); }

    @Override
    protected void renderSlot(GuiGraphics g, Slot slot) {
        if (hiddenSlot(slot)) return;
        super.renderSlot(g, slot);
    }

    @Override
    protected void renderSlotHighlight(GuiGraphics g, Slot slot, int mouseX, int mouseY, float partialTick) {
        if (hiddenSlot(slot)) return;
        super.renderSlotHighlight(g, slot, mouseX, mouseY, partialTick);
    }

    // ---- 0.1.11.142: what goes in each slot (the empty slots were all alike); the battery is the base class's ----

    @Override
    protected net.minecraft.world.item.ItemStack slotGhost(Slot slot) {
        int m = mode();
        return switch (slot.index) {
            case CannerElcMenu.INPUT_SLOT -> itemOf(switch (m) {
                case 0 -> "mio_icif:normal/item_tin_empty_can";
                case 1 -> "mio_icif:cell/item_cell_water";
                default -> "mio_icif:cell/item_cell_empty";
            });
            case CannerElcMenu.MATERIAL_SLOT -> m == 0 ? itemOf("minecraft:bread") : net.minecraft.world.item.ItemStack.EMPTY;
            // 0.1.11.175: the battery moved off the furnace block's place, where the base screen looks for it
            case CannerElcMenu.BATTERY_SLOT -> itemOf("mio_icif:normal/item_chargebat_lev0");
            default -> super.slotGhost(slot);
        };
    }

    @Override
    protected Component slotHint(Slot slot) {
        String k = "gui.mio_icif.canner.slot.";
        int m = mode();
        return switch (slot.index) {
            case CannerElcMenu.INPUT_SLOT -> Component.translatable(k + switch (m) {
                case 0 -> "can";
                case 1 -> "full_cell";
                case 2 -> "empty_cell";
                default -> "mix_container";
            });
            case CannerElcMenu.MATERIAL_SLOT -> materialUsed() ? Component.translatable(k + (m == 0 ? "food" : "additive")) : null;
            case CannerElcMenu.BATTERY_SLOT -> Component.translatable(K + "slot.battery");   // 0.1.11.175: see slotGhost
            default -> super.slotHint(slot);
        };
    }
}
