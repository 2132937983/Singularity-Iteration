package com.miophas.singularity_iteration.common.client.screen;

import com.miophas.singularity_iteration.common.blockentity.producer.mio_icif_washer_elc;
import com.miophas.singularity_iteration.common.client.suit.FcsDraw;
import com.miophas.singularity_iteration.common.menu.producer.WasherElcMenu;
import com.miophas.singularity_iteration.common.processing.ProcessStatus;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/**
 * 洗矿机界面. 0.1.11.114 (processing overhaul M7): the instrument screen (docs/gui_processing_machines.md). The figure is
 * the washer from the side: water falls into the turning washing drum, the slurry runs down into the settling pond (mud
 * on the bottom, its level the tank's), and the return pipe takes 80 % of the water back up. The headline is the tank
 * with a bar and a red mark at one washing's net water; the readouts give the flow, the recovery and the net use.
 */
@OnlyIn(Dist.CLIENT)
@SuppressWarnings("null")
public class mio_icif_gui_washer_elc extends ProcessInstrumentScreen<WasherElcMenu> {

    private static final int WATER = 0xFF4A8CC4, WATER_LIGHT = 0xFF8DBDE4, MUD = 0xFF8A7A62;
    private float drum, fall;

    public mio_icif_gui_washer_elc(WasherElcMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
    }

    private boolean running() { return menu.getStatus() == ProcessStatus.RUNNING; }
    private float tank() { return menu.getFluidCapacity() > 0 ? (float) menu.getFluidAmount() / menu.getFluidCapacity() : 0; }

    @Override
    protected void drawFigure(GuiGraphics g, float cx, float cy, float dt) {
        float tau = 2 * (float) Math.PI;
        if (running()) { drum = (drum + dt * 0.8F * tau) % tau; fall = (fall + dt * 2) % 1F; }
        // the settling pond: mud at the bottom, water up to the tank's level
        int px0 = (int) cx - 38, px1 = (int) cx + 30, pTop = (int) cy + 10, pBottom = (int) cy + 36;
        FcsDraw.fill(g, px0, pTop, px1, pBottom, SHELL);
        int level = pBottom - 2 - Math.round((pBottom - pTop - 6) * tank());
        FcsDraw.fill(g, px0 + 2, level, px1 - 2, pBottom - 2, WATER);
        FcsDraw.fill(g, px0 + 2, pBottom - 5, px1 - 2, pBottom - 2, MUD);
        // the return pipe on the right, back up to the feed
        int rx = (int) cx + 34;
        FcsDraw.fill(g, px1, pBottom - 8, rx + 2, pBottom - 6, STEEL);
        FcsDraw.fill(g, rx, (int) cy - 34, rx + 2, pBottom - 6, STEEL);
        FcsDraw.fill(g, (int) cx - 2, (int) cy - 34, rx + 2, (int) cy - 32, STEEL);
        if (running()) for (int k = 0; k < 3; k++) {   // water drops falling into the drum, and moving up the return
            float t = (fall + k / 3F) % 1F;
            FcsDraw.disc(g, cx, cy - 32 + t * 10, 1.2F, WATER);
            FcsDraw.disc(g, rx + 1, pBottom - 6 - t * (pBottom - 6 - (cy - 34)), 1.0F, WATER_LIGHT);
        }
        // the washing drum with its paddles
        float dy = cy - 10;
        FcsDraw.disc(g, cx, dy, 15, SHELL);
        FcsDraw.disc(g, cx, dy, 12, running() ? WATER_LIGHT : CHAMBER);
        for (int k = 0; k < 6; k++) blade(g, cx, dy, drum + k * tau / 6, 3, 12, 1, 1, STEEL);
        FcsDraw.disc(g, cx, dy, 3, STEEL);
        if (running()) FcsDraw.fill(g, (int) cx - 1, (int) dy + 14, (int) cx + 1, pTop, WATER);   // the slurry to the pond
    }

    @Override
    protected Component headline() {
        return Component.translatable(K + "tank", String.format(java.util.Locale.ROOT, "%,d", menu.getFluidAmount()));
    }

    @Override
    protected int headlineColour() { return INK; }

    @Override
    protected float headlineBar() { return tank(); }

    @Override
    protected float headlineBarMarker() {
        return menu.getFluidCapacity() > 0 ? (float) menu.getNetWater() / menu.getFluidCapacity() : -1;
    }

    @Override
    protected List<Component> readouts() {
        List<Component> lines = new ArrayList<>();
        lines.add(Component.translatable(K + "wash_flow", menu.getGrossWater(), Math.round(mio_icif_washer_elc.RECOVERED_SHARE * 100)));
        lines.add(Component.translatable(K + "wash_net", menu.getNetWater()));
        lines.add(Component.translatable(K + "energy_line", mio_icif_washer_elc.DEFAULT_ENERGY_PER_TICK * menu.getMaxProgress(),
            mio_icif_washer_elc.DEFAULT_ENERGY_PER_TICK, menu.getMaxProgress()));
        return lines;
    }

    @Override
    protected int status() { return menu.getStatus(); }

    @Override
    protected List<Component> figureTooltip() {
        return List.of(Component.translatable(K + "tip.washer.1"), Component.translatable(K + "tip.washer.2"));
    }
}
