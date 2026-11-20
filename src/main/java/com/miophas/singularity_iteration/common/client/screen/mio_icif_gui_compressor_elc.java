package com.miophas.singularity_iteration.common.client.screen;

import com.miophas.singularity_iteration.common.client.suit.FcsDraw;

import com.miophas.singularity_iteration.common.menu.producer.CompressorElcMenu;
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
 * 压缩机界面. 0.1.11.112 (processing overhaul M5): the instrument screen (docs/gui_processing_machines.md). The figure is a
 * hydraulic press from the side: the frame, the cylinder on the crown, the ram coming down onto the charge in the die as
 * the operation goes on (it presses for the first 70 % and returns) and the charge squashed under it. The readouts give
 * the input with the oil pressure its work stands for, and the recipe's energy, power and time.
 */
@OnlyIn(Dist.CLIENT)
@SuppressWarnings("null")
public class mio_icif_gui_compressor_elc extends ProcessInstrumentScreen<CompressorElcMenu> {

    public mio_icif_gui_compressor_elc(CompressorElcMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
    }

    private boolean running() { return menu.getStatus() == ProcessStatus.RUNNING; }

    /** Ram travel 0 (up) .. 1 (on the charge) for an operation share: down for 70 %, back up for 30 %. */
    static float stroke(float p) {
        return p < 0.7F ? p / 0.7F : 1 - (p - 0.7F) / 0.3F;
    }

    /** Draws the press around (cx, cy); {@code travel} 0..1, {@code active} colours the ram face. */
    static void press(GuiGraphics g, float cx, float cy, float travel, boolean active, boolean loaded) {
        int x0 = (int) cx - 36, x1 = (int) cx + 36, top = (int) cy - 36, bed = (int) cy + 28;
        FcsDraw.fill(g, x0, top + 6, x1, top + 14, STEEL);                              // crown
        FcsDraw.fill(g, (int) cx - 10, top, (int) cx + 10, top + 6, STEEL);              // cylinder
        FcsDraw.fill(g, x0 + 4, top + 14, x0 + 10, bed, STEEL);                          // columns
        FcsDraw.fill(g, x1 - 10, top + 14, x1 - 4, bed, STEEL);
        FcsDraw.fill(g, x0, bed, x1, bed + 8, STEEL);                                    // bed
        FcsDraw.fill(g, (int) cx - 16, bed - 4, (int) cx + 16, bed, SHELL);              // die
        int travelPx = 33;   // at full travel the platen squashes the charge to half its height
        int ramFace = top + 22 + Math.round(travel * travelPx);
        FcsDraw.fill(g, (int) cx - 3, top + 14, (int) cx + 3, ramFace - 4, STEEL_LIGHT);  // rod
        FcsDraw.fill(g, (int) cx - 18, ramFace - 4, (int) cx + 18, ramFace, active ? 0xFF000000 | AMBER : STEEL_LIGHT);   // platen
        if (loaded) {   // the charge: full height until the platen reaches it, then squashed to half
            int full = 10, chargeTop = bed - 4 - full;
            int squashed = Math.max(chargeTop, Math.min(bed - 4 - full / 2, ramFace));
            FcsDraw.fill(g, (int) cx - 12, Math.max(chargeTop, squashed), (int) cx + 12, bed - 4, 0xFFA7B0AE);
        }
    }

    @Override
    protected void drawFigure(GuiGraphics g, float cx, float cy, float dt) {
        int max = menu.getMaxProgress();
        float p = max > 0 ? (float) menu.getProgress() / max : 0;
        boolean loaded = !menu.getSlot(CompressorElcMenu.INPUT_SLOT).getItem().isEmpty();
        press(g, cx, cy, running() ? stroke(p) : 0, running(), loaded);
    }

    @Override
    protected Component headline() {
        return Component.translatable(K + (running() ? "pressing" : "stopped"));
    }

    @Override
    protected int headlineColour() { return running() ? INK : DIM; }

    @Override
    protected List<Component> readouts() {
        List<Component> lines = new ArrayList<>();
        ItemStack input = menu.getSlot(CompressorElcMenu.INPUT_SLOT).getItem();
        if (input.isEmpty()) lines.add(Component.translatable(K + "no_material"));
        else lines.add(Component.translatable(K + "press_material", input.getHoverName(), f1(Pressing.pressureMpa(menu.getEnergyPerOperation()))));
        lines.add(Component.translatable(K + "energy_line", menu.getEnergyPerOperation(), menu.getPower(), menu.getMaxProgress()));
        return lines;
    }

    @Override
    protected int status() { return menu.getStatus(); }

    @Override
    protected double energyFraction() { return menu.getEnergyPermille() / 1000.0; }

    @Override
    protected List<Component> figureTooltip() {
        return List.of(Component.translatable(K + "tip.press.1"), Component.translatable(K + "tip.press.2"));
    }
}
