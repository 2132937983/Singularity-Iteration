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
 * 装罐机界面. 0.1.11.116 (processing overhaul M9): the instrument screen (docs/gui_processing_machines.md). The figure has the
 * input tank on the left, the output tank on the right and the can or cell in the middle under its filler; the pipes
 * that work in the current mode carry moving dots: canning fills the can from the hopper, "cell to tank" empties the
 * cell into the right tank, "tank to cell" fills it from the left one, and mixing runs the left tank through the mixer
 * into the right one. The headline is the mode; the buttons switch the mode and swap the tanks.
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

    private void tank(GuiGraphics g, int x, int y, FluidStack fluid, int capacity) {
        g.fill(x, y, x + 14, y + 52, SHELL);
        g.fill(x + 2, y + 2, x + 12, y + 50, CHAMBER);
        mio_icif_GuiUtils.renderFluidBar(g, x + 2, y + 2, 10, 48, fluid, capacity);
    }

    /** A pipe from (x1, y1) to (x2, y2), with dots moving along it while it works. */
    private void pipe(GuiGraphics g, float x1, float y1, float x2, float y2, boolean active) {
        FcsDraw.line(g, x1, y1, x2, y2, 2.4F, active ? STEEL : STEEL_LIGHT);
        if (active && running()) for (int k = 0; k < 3; k++) {
            float f = (flow + k / 3F) % 1;
            FcsDraw.disc(g, x1 + (x2 - x1) * f, y1 + (y2 - y1) * f, 1.4F, 0xFF000000 | BLUE);
        }
    }

    @Override
    protected void drawFigure(GuiGraphics g, float cx, float cy, float dt) {
        if (running()) flow = (flow + dt * 1.2F) % 1;
        int m = mode();
        tank(g, (int) cx - 46, (int) cy - 30, menu.getInputFluid(), menu.getInputFluidCapacity());
        tank(g, (int) cx + 32, (int) cy - 30, menu.getOutputFluid(), menu.getOutputFluidCapacity());
        // the filler head over the can
        g.fill((int) cx - 8, (int) cy - 24, (int) cx + 8, (int) cy - 16, STEEL);
        g.fill((int) cx - 2, (int) cy - 16, (int) cx + 2, (int) cy - 8, STEEL);
        // the pipes of the mode
        pipe(g, cx - 32, cy - 26, cx - 8, cy - 20, m == 2 || m == 3);   // left tank -> filler
        pipe(g, cx + 8, cy - 20, cx + 32, cy - 26, m == 1 || m == 3);   // filler -> right tank
        if (m == 0) {   // the hopper of the food or fuel
            FcsDraw.quad(g, cx - 10, cy - 40, cx + 10, cy - 40, cx + 4, cy - 26, cx - 4, cy - 26, SHELL);
            if (menu.getSlot(CannerElcMenu.MATERIAL_SLOT).hasItem()) g.fill((int) cx - 6, (int) cy - 37, (int) cx + 6, (int) cy - 32, FormingFigure.WORK);
            if (running()) FcsDraw.disc(g, cx, cy - 26 + flow * 18, 1.6F, FormingFigure.WORK);
        }
        // the can (or cell), filled with the progress (emptied in "cell to tank")
        int x0 = (int) cx - 9, x1 = (int) cx + 9, y0 = (int) cy - 6, y1 = (int) cy + 26;
        g.fill(x0, y0, x1, y1, STEEL_LIGHT);
        g.fill(x0 + 2, y0 + 2, x1 - 2, y1 - 2, CHAMBER);
        int max = menu.getMaxProgress();
        float p = max > 0 && running() ? (float) menu.getProgress() / max : 0;
        float level = m == 1 ? 1 - p : p;
        int h = Math.round((y1 - y0 - 4) * level);
        if (h > 0) g.fill(x0 + 2, y1 - 2 - h, x1 - 2, y1 - 2, m == 0 ? FormingFigure.WORK : 0xFF000000 | BLUE);
        g.fill(x0 - 4, y1, x1 + 4, y1 + 3, STEEL);   // the stand
    }

    @Override
    protected Component headline() {
        return Component.translatable("gui.mio_icif.canner." + MODES[mode()]);
    }

    @Override
    protected int headlineColour() { return running() ? INK : DIM; }

    private static Component tankLine(String key, FluidStack fluid, int amount, int capacity) {
        Component name = fluid.isEmpty() ? Component.literal("") : fluid.getHoverName();
        return Component.translatable(K + key, String.format(Locale.ROOT, "%,d", amount), String.format(Locale.ROOT, "%,d", capacity), name);
    }

    @Override
    protected List<Component> readouts() {
        List<Component> lines = new ArrayList<>();
        lines.add(tankLine("tank_in", menu.getInputFluid(), menu.getInputFluidAmount(), menu.getInputFluidCapacity()));
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
        int by = Math.min(y - topPos, PROGRESS_Y - 14);
        menuButton(g, x - leftPos, by, 44, 11, Component.translatable(K + "switch_mode"), false, 0, mouseX, mouseY,
            List.of(Component.translatable(K + "tip.switch_mode")));
        menuButton(g, x - leftPos + 46, by, 44, 11, Component.translatable(K + "swap"), false, 1, mouseX, mouseY,
            List.of(Component.translatable("gui.mio_icif.canner.swap_fluids")));
    }

    @Override
    protected List<Component> figureTooltip() {
        return List.of(Component.translatable(K + "tip.canner.1"), Component.translatable(K + "tip.canner.2"));
    }

    // the material slot is not used for "cell to tank" (as before)
    @Override
    protected void renderSlot(GuiGraphics g, Slot slot) {
        if (slot.index == CannerElcMenu.MATERIAL_SLOT && menu.getMode() == 1) return;
        super.renderSlot(g, slot);
    }

    @Override
    protected void renderSlotHighlight(GuiGraphics g, Slot slot, int mouseX, int mouseY, float partialTick) {
        if (slot.index == CannerElcMenu.MATERIAL_SLOT && menu.getMode() == 1) return;
        super.renderSlotHighlight(g, slot, mouseX, mouseY, partialTick);
    }
}
