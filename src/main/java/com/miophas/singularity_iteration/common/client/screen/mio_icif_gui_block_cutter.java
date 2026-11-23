package com.miophas.singularity_iteration.common.client.screen;

import com.miophas.singularity_iteration.common.menu.MergedSlots;
import com.miophas.singularity_iteration.common.blockentity.producer.mio_icif_block_cutter;
import com.miophas.singularity_iteration.common.client.suit.FcsDraw;
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
        return mio_icif_block_cutter.bladeHardness(b);   // 0.1.11.179: the block entity's (it knows the refined iron blade)
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
        int colour = bh == 8 ? DIAMOND : bh > 0 ? STEEL_LIGHT : SHELL;
        FcsDraw.disc(g, cx, bcy, 22, colour);
        if (bh > 0) for (int k = 0; k < 18; k++) blade(g, cx, bcy, angle + k * tau / 18, 21, 25, 1.6F, 0.2F, colour);
        FcsDraw.disc(g, cx, bcy, 6, STEEL);
        blade(g, cx, bcy, angle, 0, 6, 0.8F, 0.8F, STEEL_LIGHT);
        FcsDraw.fill(g, (int) cx - 36, (int) bcy - 2, (int) cx - 6, (int) bcy + 2, STEEL);   // the arbor arm
        // the block over it, with the kerf going down as the cut goes on
        FcsDraw.fill(g, bx0, by0, bx1, by1, FormingFigure.WORK);
        if (p > 0) FcsDraw.fill(g, (int) cx - 1, by0, (int) cx + 1, by0 + Math.round((by1 - by0) * p), colour);
    }

    // ---- 0.1.11.178: the merged drawing (panel coordinates; the slots are MergedSlots.Cutter's); 0.1.11.180: 176 wide ----
    // The saw bench: the block's slot on the table under the blade, the blade coming down into it with the cut; the arm on
    // the column with the motor at its head (wired to the battery, belted to the arbor); the blade's slot hanging on the
    // rack beside the column; the arrow to the tray. No blade mounted: the bare arbor.
    private static final int SAW_X = 96;

    @Override protected int arrowX() { return 120; }
    @Override protected int arrowY() { return MergedSlots.Cutter.OUT_Y + 1; }
    @Override protected int[] machineBody() { return new int[]{54, 20, 62, 56}; }

    @Override
    protected void drawMachine(GuiGraphics g, float dt) {
        float tau = 2 * (float) Math.PI;
        if (running()) angle = (angle + dt * 3.5F * tau) % tau;
        float p = running() ? (float) progress() : 0;
        MachineArt.cable(g, 28, 82, 32, 82, 32, 68, 54, 68);
        MachineArt.iron(g, 30, 76, 112, 8);                                           // the table
        MachineArt.iron(g, 34, 84, 8, 11);
        MachineArt.iron(g, 130, 84, 8, 11);
        MachineArt.iron(g, 54, 20, 8, 56);                                            // the column
        int bcy = 38 + Math.round(p * 8);
        MachineArt.iron(g, 62, bcy - 3, 34, 6);                                       // the arm
        MachineArt.motor(g, 60, 20, 26, 12);
        FcsDraw.line(g, 73, 32, 73, bcy - 3, 2, MachineArt.RUBBER);                   // the belt
        int bh = bladeHardness();
        if (bh > 0) {
            // 0.1.11.179: iron in steel grey, refined iron brighter, diamond cyan
            int colour = bh == 8 ? DIAMOND : bh == 5 ? 0xFFD2D8DC : MachineArt.STEEL_LIGHT;
            FcsDraw.disc(g, SAW_X, bcy, 18, colour);
            for (int k = 0; k < 18; k++) blade(g, SAW_X, bcy, angle + k * tau / 18, 17, 20, 1.4F, 0.2F, colour);
            FcsDraw.disc(g, SAW_X, bcy, 16, bh == 8 ? 0xFF5FB3B8 : bh == 5 ? 0xFFB4BCC0 : 0xFF9AA2A6);
        }
        FcsDraw.disc(g, SAW_X, bcy, 5, MachineArt.STEEL);                              // the arbor
        blade(g, SAW_X, bcy, angle, 0, 5, 0.8F, 0.8F, MachineArt.STEEL_LIGHT);
        MachineArt.steel(g, MergedSlots.Cutter.BLADE_X - 4, MergedSlots.Cutter.BLADE_Y - 4, 24, 4);   // the rack
        FcsDraw.fill(g, MergedSlots.Cutter.BLADE_X + 7, MergedSlots.Cutter.BLADE_Y - 1, MergedSlots.Cutter.BLADE_X + 9, MergedSlots.Cutter.BLADE_Y,
            MachineArt.IRON_DARK);
        MachineArt.tray(g, MergedSlots.Cutter.OUT_X, MergedSlots.Cutter.OUT_Y);
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

    // 0.1.11.143: the blade slot shows a ghost blade and says what goes there
    @Override
    protected net.minecraft.world.item.ItemStack slotGhost(net.minecraft.world.inventory.Slot slot) {
        return slot.index == com.miophas.singularity_iteration.common.menu.producer.BlockCutterMenu.BLADE_SLOT
            ? itemOf("mio_icif:resource/item_iron_cut_blade") : super.slotGhost(slot);
    }

    @Override
    protected Component slotHint(net.minecraft.world.inventory.Slot slot) {
        return slot.index == com.miophas.singularity_iteration.common.menu.producer.BlockCutterMenu.BLADE_SLOT
            ? Component.translatable(K + "slot.blade") : super.slotHint(slot);
    }

    @Override
    protected List<Component> figureTooltip() {
        return List.of(Component.translatable(K + "tip.cutter.1"), Component.translatable(K + "tip.cutter.2"));
    }
}
