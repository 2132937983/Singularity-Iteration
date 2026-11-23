package com.miophas.singularity_iteration.common.client.screen;

import com.miophas.singularity_iteration.common.menu.MergedSlots;
import com.miophas.singularity_iteration.common.client.suit.FcsDraw;

import com.miophas.singularity_iteration.common.blockentity.producer.mio_icif_compressor_advanced_elc;
import com.miophas.singularity_iteration.common.menu.producer.CompressorAdvancedElcMenu;
import com.miophas.singularity_iteration.common.processing.Pressing;
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
 * 进阶高压压缩机界面. 0.1.11.112 (processing overhaul M5): the instrument screen (docs/gui_processing_machines.md). The
 * figure is the hydraulic press beside its accumulator: the ram strokes as the operation goes on, and the accumulator
 * bottle shows its oil level (the stored share) with a red line at the pressure the next stroke needs. The headline is
 * the oil pressure with a bar and the same red mark; the readouts give the input, the stroke's energy, power and time.
 */
@OnlyIn(Dist.CLIENT)
@SuppressWarnings("null")
public class mio_icif_gui_compressor_advanced_elc extends ProcessInstrumentScreen<CompressorAdvancedElcMenu> {

    public mio_icif_gui_compressor_advanced_elc(CompressorAdvancedElcMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
    }

    private boolean running() { return menu.getStatus() == ProcessStatus.RUNNING; }
    private float pressure() { return (float) Math.max(0, Math.min(1, menu.getPressureMpa() / Pressing.FULL_PRESSURE_MPA)); }
    private float required() { return (float) Math.max(0, Math.min(1, menu.getRequiredPressureMpa() / Pressing.FULL_PRESSURE_MPA)); }

    @Override
    protected void drawFigure(GuiGraphics g, float cx, float cy, float dt) {
        int max = menu.getMaxProgress();
        float p = max > 0 ? (float) menu.getProgress() / max : 0;
        boolean loaded = !menu.getSlot(mio_icif_compressor_advanced_elc.INPUT_SLOT).getItem().isEmpty();
        mio_icif_gui_compressor_elc.press(g, cx - 10, cy, running() ? mio_icif_gui_compressor_elc.stroke(p) : 0, running(), loaded);
        // the accumulator: a bottle right of the press, oil level = stored share, red line = the next stroke's need
        int bx0 = (int) cx + 32, bx1 = bx0 + 12, top = (int) cy - 30, bottom = (int) cy + 36;
        FcsDraw.fill(g, bx0, top, bx1, bottom, SHELL);
        int level = bottom - Math.round((bottom - top - 2) * pressure());
        FcsDraw.fill(g, bx0 + 2, level, bx1 - 2, bottom - 2, 0xFF000000 | AMBER);
        int need = bottom - Math.round((bottom - top - 2) * required());
        FcsDraw.fill(g, bx0 - 1, need, bx1 + 1, need + 1, 0xFF000000 | RED);
        FcsDraw.fill(g, (int) cx + 26, (int) cy + 30, bx0, (int) cy + 32, STEEL);   // the line to the press
    }

    // ---- 0.1.11.178: the merged drawing (panel coordinates; the slots are MergedSlots.Press's); 0.1.11.180: 176 wide ----
    // The press as the compressor's, fed from the accumulator bottle on the left: its oil stands at the stored pressure,
    // the red line at the pressure the next stroke needs.
    private static final int BOTTLE_X = 34, BOTTLE_Y = 24, BOTTLE_W = 10, BOTTLE_H = 62;

    @Override protected int arrowX() { return 122; }
    @Override protected int arrowY() { return MergedSlots.Press.OUT_Y + 1; }
    @Override protected int[] machineBody() { return new int[]{52, 17, 72, 78}; }

    @Override
    protected List<Component> mergedTooltip(int rx, int ry) {
        if (inside(rx, ry, BOTTLE_X - 2, BOTTLE_Y - 2, BOTTLE_W + 4, BOTTLE_H + 4))
            // EXPLAIN_TIP_HIDDEN (0.1.11.182): only the pressure; the accumulator's how-it-works lines are hidden for now
            // return List.of(Component.translatable(K + "accumulator", f1(menu.getPressureMpa())), Component.translatable(K + "tip.accumulator.1"),
            //     Component.translatable(K + "tip.accumulator.2"));
            return List.of(Component.translatable(K + "accumulator", f1(menu.getPressureMpa())));
        return super.mergedTooltip(rx, ry);
    }

    @Override
    protected void drawMachine(GuiGraphics g, float dt) {
        MachineArt.cable(g, 28, 82, 32, 82);
        MachineArt.gauge(g, BOTTLE_X, BOTTLE_Y, BOTTLE_W, BOTTLE_H, pressure(), MachineArt.AMBER);
        int need = BOTTLE_Y + BOTTLE_H - 1 - Math.round((BOTTLE_H - 2) * required());
        FcsDraw.fill(g, BOTTLE_X - 1, need, BOTTLE_X + BOTTLE_W + 1, need + 1, MachineArt.RED);
        MachineArt.pipeV(g, BOTTLE_X + BOTTLE_W / 2, 18, BOTTLE_Y - 2, false, -1);
        MachineArt.pipeH(g, BOTTLE_X + BOTTLE_W / 2 - 2, 78, 20, false, -1);
        mio_icif_gui_compressor_elc.pressFrame(g, running() ? mio_icif_gui_compressor_elc.stroke((float) progress()) : 0, running());
    }

    @Override
    protected Component headline() {
        return Component.translatable(K + "accumulator", f1(menu.getPressureMpa()));
    }

    @Override
    protected int headlineColour() { return INK; }

    @Override
    protected float headlineBar() { return pressure(); }

    @Override
    protected float headlineBarMarker() { return required(); }

    @Override
    protected List<Component> readouts() {
        List<Component> lines = new ArrayList<>();
        ItemStack input = menu.getSlot(mio_icif_compressor_advanced_elc.INPUT_SLOT).getItem();
        if (input.isEmpty()) lines.add(Component.translatable(K + "no_material"));
        else lines.add(Component.translatable(K + "press_material", input.getHoverName(), f1(menu.getRequiredPressureMpa())));
        lines.add(Component.translatable(K + "energy_line", menu.getEnergyPerOperation(), menu.getPower(), menu.getMaxProgress()));
        return lines;
    }

    @Override
    protected int status() { return menu.getStatus(); }

    @Override
    protected double energyFraction() { return pressure(); }

    @Override
    protected List<Component> figureTooltip() {
        return List.of(Component.translatable(K + "tip.press.1"), Component.translatable(K + "tip.accumulator.1"),
            Component.translatable(K + "tip.accumulator.2"));
    }
}
