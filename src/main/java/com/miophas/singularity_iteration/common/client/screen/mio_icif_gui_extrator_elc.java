package com.miophas.singularity_iteration.common.client.screen;

import com.miophas.singularity_iteration.common.client.suit.FcsDraw;
import com.miophas.singularity_iteration.common.menu.producer.ExtractorElcMenu;
import com.miophas.singularity_iteration.common.processing.ProcessStatus;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/**
 * 提取机界面. 0.1.11.116 (processing overhaul M9): the instrument screen (docs/gui_processing_machines.md). The figure is a
 * screw press from the side: the hopper feeds the barrel, the screw's flights turn and carry the charge to the outlet,
 * the charge is pressed thinner on the way, the juice drips through the screen into the tray and the cake leaves at
 * the end. The readouts give the recipe's energy, power and time and the operations a minute.
 */
@OnlyIn(Dist.CLIENT)
@SuppressWarnings("null")
public class mio_icif_gui_extrator_elc extends ProcessInstrumentScreen<ExtractorElcMenu> {

    private static final int JUICE = 0xFFC9A54A;
    private float phase;

    public mio_icif_gui_extrator_elc(ExtractorElcMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
    }

    private boolean running() { return menu.getStatus() == ProcessStatus.RUNNING; }

    @Override
    protected void drawFigure(GuiGraphics g, float cx, float cy, float dt) {
        if (running()) phase = (phase + dt * 12) % 10;
        int x0 = (int) cx - 38, x1 = (int) cx + 30, y0 = (int) cy - 12, y1 = (int) cy + 8;
        // the hopper over the inlet
        FcsDraw.quad(g, cx - 40, cy - 30, cx - 18, cy - 30, cx - 26, cy - 12, cx - 32, cy - 12, SHELL);
        if (menu.getSlot(ExtractorElcMenu.INPUT_SLOT).hasItem()) g.fill((int) cx - 36, (int) cy - 26, (int) cx - 22, (int) cy - 18, FormingFigure.WORK);
        // the barrel, the charge pressed thinner towards the outlet
        g.fill(x0, y0, x1, y1, SHELL);
        g.fill(x0 + 2, y0 + 2, x1 - 2, y1 - 2, CHAMBER);
        if (running()) for (int x = x0 + 2; x < x1 - 2; x++) {
            float t = (float) (x - x0) / (x1 - x0);
            int h = Math.round(7 - 4 * t);
            g.fill(x, (int) cy - 2 - h, x + 1, (int) cy - 2 + h, FormingFigure.WORK);
        }
        // the shaft and the flights (shallower towards the outlet), moving with the turn
        g.fill(x0 - 6, (int) cy - 3, x1 + 4, (int) cy - 1, STEEL);
        for (int k = 0; k < 8; k++) {
            float x = x0 + 4 + ((k * 8 + phase * 0.8F) % 62);
            float t = (x - x0) / (x1 - x0);
            float r = 7 - 3 * t;
            FcsDraw.line(g, x - 2, cy - 2 - r, x + 2, cy - 2 + r, 1.4F, STEEL_LIGHT);
        }
        // the drive at the inlet end
        g.fill(x0 - 12, (int) cy - 8, x0 - 4, (int) cy + 4, STEEL);
        // the screen under the barrel, the juice dripping into the tray
        for (int x = x0 + 8; x < x1 - 4; x += 4) g.fill(x, y1, x + 2, y1 + 1, STEEL_LIGHT);
        g.fill(x0 + 4, (int) cy + 26, x1 - 2, (int) cy + 34, SHELL);
        g.fill(x0 + 6, (int) cy + 29, x1 - 4, (int) cy + 32, JUICE);
        if (running()) for (int k = 0; k < 5; k++) {
            float dx = x0 + 12 + k * 12, f = ((phase / 10) + k * 0.37F) % 1;
            FcsDraw.disc(g, dx, y1 + 2 + f * 16, 1.2F, JUICE);
        }
        // the cake leaving the outlet
        if (running()) {
            float f = phase / 10;
            g.fill(x1, (int) (cy - 4), x1 + 3 + Math.round(5 * f), (int) cy, FormingFigure.WORK);
        }
    }

    @Override
    protected Component headline() {
        return Component.translatable(K + (running() ? "extracting" : "stopped"));
    }

    @Override
    protected int headlineColour() { return running() ? INK : DIM; }

    @Override
    protected List<Component> readouts() {
        List<Component> lines = new ArrayList<>();
        int max = menu.getMaxProgress();
        lines.add(Component.translatable(K + "energy_line", (long) menu.getPower() * max, menu.getPower(), max));
        if (max > 0) lines.add(Component.translatable(K + "rate_line", f1(1200.0 / max)));
        return lines;
    }

    @Override
    protected int status() { return menu.getStatus(); }

    @Override
    protected double energyFraction() { return menu.getEnergyPermille() / 1000.0; }

    @Override
    protected List<Component> figureTooltip() {
        return List.of(Component.translatable(K + "tip.extractor.1"), Component.translatable(K + "tip.extractor.2"));
    }
}
