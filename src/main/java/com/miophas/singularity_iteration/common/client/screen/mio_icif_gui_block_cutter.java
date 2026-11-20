package com.miophas.singularity_iteration.common.client.screen;

import com.miophas.singularity_iteration.common.blockentity.producer.mio_icif_block_cutter;
import com.miophas.singularity_iteration.common.client.suit.FcsDraw;
import com.miophas.singularity_iteration.common.item.resource.mio_icif_resources;
import com.miophas.singularity_iteration.common.menu.producer.BlockCutterMenu;
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
 * 方块切割机界面. 0.1.11.115 (processing overhaul M8): the instrument screen (docs/gui_processing_machines.md). The figure is
 * the circular saw from the side, its blade turning while it cuts (steel grey for the iron blade, pale cyan for the
 * diamond one) and the kerf going down into the block as the cut goes on. The readouts give the blade with its hardness
 * and durability, the material's hardness and the wear of a cut, the recipe's energy, power and time.
 */
@OnlyIn(Dist.CLIENT)
@SuppressWarnings("null")
public class mio_icif_gui_block_cutter extends ProcessInstrumentScreen<BlockCutterMenu> {

    private static final int DIAMOND = 0xFF7FD3D8;
    private float angle;

    public mio_icif_gui_block_cutter(BlockCutterMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
    }

    private boolean running() { return menu.getStatus() == ProcessStatus.RUNNING; }
    private ItemStack blade() { return menu.getSlot(mio_icif_block_cutter.BLADE_SLOT).getItem(); }
    private int bladeHardness() {
        ItemStack b = blade();
        return b.is(mio_icif_resources.DIAMOND_CUT_BLADE.get()) ? 8 : b.is(mio_icif_resources.IRON_CUT_BLADE.get()) ? 2 : 0;
    }

    @Override
    protected void drawFigure(GuiGraphics g, float cx, float cy, float dt) {
        float tau = 2 * (float) Math.PI;
        if (running()) angle = (angle + dt * 3.5F * tau) % tau;
        int max = menu.getMaxProgress();
        float p = running() && max > 0 ? (float) menu.getProgress() / max : 0;
        int bx0 = (int) cx - 26, bx1 = (int) cx + 26, by0 = (int) cy + 10, by1 = (int) cy + 34;
        // the blade, coming down with the cut (behind the block: its lower part is in the cut)
        float bcy = cy - 14 + p * 20;
        int bh = bladeHardness();
        int colour = bh == 8 ? DIAMOND : bh == 2 ? STEEL_LIGHT : SHELL;
        FcsDraw.disc(g, cx, bcy, 22, colour);
        if (bh > 0) for (int k = 0; k < 18; k++) blade(g, cx, bcy, angle + k * tau / 18, 21, 25, 1.6F, 0.2F, colour);
        FcsDraw.disc(g, cx, bcy, 6, STEEL);
        blade(g, cx, bcy, angle, 0, 6, 0.8F, 0.8F, STEEL_LIGHT);
        FcsDraw.fill(g, (int) cx - 36, (int) bcy - 2, (int) cx - 6, (int) bcy + 2, STEEL);   // the arbor arm
        // the block over it, with the kerf going down as the cut goes on
        FcsDraw.fill(g, bx0, by0, bx1, by1, FormingFigure.WORK);
        if (p > 0) FcsDraw.fill(g, (int) cx - 1, by0, (int) cx + 1, by0 + Math.round((by1 - by0) * p), colour);
    }

    @Override
    protected Component headline() {
        return Component.translatable(K + (running() ? "cutting" : "stopped"));
    }

    @Override
    protected int headlineColour() { return running() ? INK : DIM; }

    @Override
    protected List<Component> readouts() {
        List<Component> lines = new ArrayList<>();
        ItemStack b = blade();
        int bh = bladeHardness();
        if (bh == 0) lines.add(Component.translatable(K + "no_blade"));
        else lines.add(Component.translatable(K + "blade", bh, b.getMaxDamage() - b.getDamageValue(), b.getMaxDamage()));
        int mh = menu.getMaterialHardness();
        if (mh > 0) {
            double r = bh > 0 ? (double) mh / bh : 0;
            lines.add(Component.translatable(K + "cut_wear", mh, String.format(java.util.Locale.ROOT, "%.2f", r * r)));
        }
        lines.add(Component.translatable(K + "energy_line", (long) menu.getPower() * menu.getMaxProgress(), menu.getPower(), menu.getMaxProgress()));
        return lines;
    }

    @Override
    protected int status() { return menu.getStatus(); }

    @Override
    protected double energyFraction() { return menu.getEnergyPermille() / 1000.0; }

    @Override
    protected List<Component> figureTooltip() {
        return List.of(Component.translatable(K + "tip.cutter.1"), Component.translatable(K + "tip.cutter.2"));
    }
}
