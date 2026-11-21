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

    // 0.1.11.141: the calm layout. Flat parts in two greys on the flat window; the tanks are drawn after the pixel canvas
    // (drawOverFigure) so they show the fluid's own texture again (0.1.11.118 had to use a flat colour under the canvas).
    private static final int FRAME = 0xFF8E959A, INNER = 0xFFA9AFB3, IDLE_PIPE = 0xFF9DA3A7;

    @Override
    protected boolean calm() { return true; }

    /** A tank at figure offset (dx, -30), 14 x 52 figure units: a frame, its chamber and the fluid's texture to its level. */
    private void tank(GuiGraphics g, float cx, float cy, float dx, FluidStack fluid, int capacity) {
        int x0 = figPx(cx, dx), y0 = figPx(cy, -30), x1 = figPx(cx, dx + 14), y1 = figPx(cy, 22);
        g.fill(x0, y0, x1, y1, FRAME);
        g.fill(x0 + 1, y0 + 1, x1 - 1, y1 - 1, INNER);
        if (!fluid.isEmpty() && capacity > 0) {
            int room = y1 - y0 - 2, lv = Math.round(room * Math.min(1, fluid.getAmount() / (float) capacity));
            if (lv == 0) lv = 1;   // a drop still shows
            VanillaFrame.fluid(g, x0 + 1, y1 - 1 - lv, x1 - x0 - 2, lv, fluid);
        }
    }

    @Override
    protected void drawOverFigure(GuiGraphics g, float cx, float cy) {
        tank(g, cx, cy, -46, menu.getInputFluid(), menu.getInputFluidCapacity());
        tank(g, cx, cy, 32, menu.getOutputFluid(), menu.getOutputFluidCapacity());
    }

    /** A pipe from (x1, y1) to (x2, y2), with dots moving along it while it works. */
    private void pipe(GuiGraphics g, float x1, float y1, float x2, float y2, boolean active) {
        FcsDraw.line(g, x1, y1, x2, y2, 2.4F, active ? STEEL : IDLE_PIPE);
        if (active && running()) for (int k = 0; k < 3; k++) {
            float f = (flow + k / 3F) % 1;
            FcsDraw.disc(g, x1 + (x2 - x1) * f, y1 + (y2 - y1) * f, 1.4F, 0xFF000000 | BLUE);
        }
    }

    @Override
    protected void drawFigure(GuiGraphics g, float cx, float cy, float dt) {
        if (running()) flow = (flow + dt * 1.2F) % 1;
        int m = mode();
        // the tanks: drawOverFigure
        // the filler head over the can
        FcsDraw.fill(g, (int) cx - 8, (int) cy - 24, (int) cx + 8, (int) cy - 16, STEEL);
        FcsDraw.fill(g, (int) cx - 2, (int) cy - 16, (int) cx + 2, (int) cy - 8, STEEL);
        // the pipes of the mode
        pipe(g, cx - 32, cy - 26, cx - 8, cy - 20, m == 2 || m == 3);   // left tank -> filler
        pipe(g, cx + 8, cy - 20, cx + 32, cy - 26, m == 1 || m == 3);   // filler -> right tank
        if (m == 0) {   // the hopper of the food or fuel
            FcsDraw.quad(g, cx - 10, cy - 40, cx + 10, cy - 40, cx + 4, cy - 26, cx - 4, cy - 26, FRAME);
            if (menu.getSlot(CannerElcMenu.MATERIAL_SLOT).hasItem()) FcsDraw.fill(g, (int) cx - 6, (int) cy - 37, (int) cx + 6, (int) cy - 32, FormingFigure.WORK);
            if (running()) FcsDraw.disc(g, cx, cy - 26 + flow * 18, 1.6F, FormingFigure.WORK);
        }
        // the can (or cell), filled with the progress (emptied in "cell to tank")
        int x0 = (int) cx - 9, x1 = (int) cx + 9, y0 = (int) cy - 6, y1 = (int) cy + 26;
        FcsDraw.fill(g, x0, y0, x1, y1, FRAME);
        FcsDraw.fill(g, x0 + 2, y0 + 2, x1 - 2, y1 - 2, INNER);
        int max = menu.getMaxProgress();
        float p = max > 0 && running() ? (float) menu.getProgress() / max : 0;
        float level = m == 1 ? 1 - p : p;
        int h = Math.round((y1 - y0 - 4) * level);
        if (h > 0) FcsDraw.fill(g, x0 + 2, y1 - 2 - h, x1 - 2, y1 - 2, m == 0 ? FormingFigure.WORK : 0xFF000000 | BLUE);
        FcsDraw.fill(g, x0 - 4, y1, x1 + 4, y1 + 3, STEEL);   // the stand
    }

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
        iconButton(g, 1, ICON_CYCLE, false, 0, mouseX, mouseY,
            List.of(Component.translatable(K + "switch_mode"), Component.translatable(K + "tip.switch_mode").withStyle(net.minecraft.ChatFormatting.GRAY)));
        iconButton(g, 0, ICON_SWAP, false, 1, mouseX, mouseY,
            List.of(Component.translatable(K + "swap"), Component.translatable("gui.mio_icif.canner.swap_fluids").withStyle(net.minecraft.ChatFormatting.GRAY)));
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
