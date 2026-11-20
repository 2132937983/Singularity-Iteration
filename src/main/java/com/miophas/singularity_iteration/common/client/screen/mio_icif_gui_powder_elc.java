package com.miophas.singularity_iteration.common.client.screen;

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
