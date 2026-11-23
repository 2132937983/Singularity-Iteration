package com.miophas.singularity_iteration.common.client.screen;

import com.miophas.singularity_iteration.common.menu.MergedSlots;
import com.miophas.singularity_iteration.common.blockentity.producer.mio_icif_induction_elc;
import com.miophas.singularity_iteration.common.client.suit.FcsDraw;
import com.miophas.singularity_iteration.common.menu.producer.InductionElcMenu;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/**
 * 感应炉界面. 0.1.11.113 (processing overhaul M6): the instrument screen (docs/gui_processing_machines.md). The figure is
 * the induction furnace from above: the crucible glowing with its temperature, the two charges in it, and the copper coil
 * around it, its turns pulsing while the heater draws power. The headline is the melt temperature with a bar and a red
 * mark at 600 °C, where smelting starts; flat buttons switch heat hold and collect the banked experience.
 */
@OnlyIn(Dist.CLIENT)
@SuppressWarnings("null")
public class mio_icif_gui_induction_elc extends ProcessInstrumentScreen<InductionElcMenu> {

    private static final int COPPER = 0xFFB87333, COPPER_HOT = 0xFFE0A060;
    private float phase;

    public mio_icif_gui_induction_elc(InductionElcMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
    }

    @Override
    protected void drawFigure(GuiGraphics g, float cx, float cy, float dt) {
        int t = menu.getTemperature();
        boolean heating = menu.getPowerNow() > 0 && (menu.isWorking() || menu.isHeatHold() || t < mio_icif_induction_elc.MAX_TEMPERATURE - 1);
        phase = (phase + dt * 3) % 1F;
        FcsDraw.disc(g, cx, cy, 38, SHELL);
        for (int k = 0; k < 4; k++) {                                  // the coil turns, a pulse running outward while heating
            float r = 36 - k * 3.5F;
            boolean lit = heating && Math.abs(((phase * 4) % 4) - k) < 0.8F;
            FcsDraw.circle(g, cx, cy, r, 2.2F, lit ? COPPER_HOT : COPPER);
        }
        FcsDraw.disc(g, cx, cy, 22, STEEL);                             // the crucible wall
        FcsDraw.disc(g, cx, cy, 19, glow(t));                           // the melt
        ItemStack a = menu.getSlot(InductionElcMenu.INPUT_SLOT_1).getItem(), b = menu.getSlot(InductionElcMenu.INPUT_SLOT_2).getItem();
        if (!a.isEmpty()) g.renderItem(a, (int) cx - 17, (int) cy - 8);
        if (!b.isEmpty()) g.renderItem(b, (int) cx + 1, (int) cy - 8);
    }

    // ---- 0.1.11.178: the merged drawing (panel coordinates; the slots are MergedSlots.Induction's); 0.1.11.180: 176 wide ----
    // The crucible from above in its coil (a pulse running outward while it heats; the melt glowing with the temperature),
    // two lanes: the inputs in two hoppers side by side on top, each output in a tray on the right after its tap and arrow;
    // the cable from the battery into the body.
    private static final int CRUCIBLE_X = 85, CRUCIBLE_Y = 67, ARROW_LEFT = 118;

    @Override protected int arrowX() { return ARROW_LEFT; }
    @Override protected int arrowY() { return MergedSlots.Induction.OUT_A_Y + 1; }
    @Override protected boolean overArrow(int rx, int ry) {
        return super.overArrow(rx, ry) || inside(rx, ry, ARROW_LEFT, MergedSlots.Induction.OUT_B_Y + 1, 22, 15);
    }
    @Override protected int[] machineBody() { return new int[]{56, 40, 58, 55}; }

    @Override
    protected void drawMerged(GuiGraphics g, int x, int y, float dt) {
        super.drawMerged(g, x, y, dt);
        VanillaFrame.arrow(g, x + ARROW_LEFT, y + MergedSlots.Induction.OUT_B_Y + 1, progress());   // the second lane
    }

    @Override
    protected void drawMachine(GuiGraphics g, float dt) {
        int t = menu.getTemperature();
        boolean heating = menu.getPowerNow() > 0 && (menu.isWorking() || menu.isHeatHold() || t < mio_icif_induction_elc.MAX_TEMPERATURE - 1);
        phase = (phase + dt * 3) % 1F;
        MachineArt.cable(g, 28, 82, 56, 82);
        MachineArt.iron(g, 56, 40, 58, 55);                                           // the body
        for (int k = 0; k < 4; k++) {                                                 // the coil turns
            boolean lit = heating && Math.abs(((phase * 4) % 4) - k) < 0.8F;
            FcsDraw.circle(g, CRUCIBLE_X, CRUCIBLE_Y, 25 - k * 3.2F, 2.2F, lit ? COPPER_HOT : COPPER);
        }
        FcsDraw.disc(g, CRUCIBLE_X, CRUCIBLE_Y, 13, MachineArt.STEEL_DARK);           // the crucible wall
        FcsDraw.disc(g, CRUCIBLE_X, CRUCIBLE_Y, 10.5F, glow(t));                       // the melt
        MachineArt.hopper(g, MergedSlots.Induction.IN_A_X, MergedSlots.Induction.IN_Y, 40);
        MachineArt.hopper(g, MergedSlots.Induction.IN_B_X, MergedSlots.Induction.IN_Y, 40);
        for (int iy : new int[]{MergedSlots.Induction.OUT_A_Y, MergedSlots.Induction.OUT_B_Y}) {
            FcsDraw.fill(g, 114, iy + 4, 118, iy + 12, MachineArt.STEEL_DARK);        // the tap
            MachineArt.tray(g, MergedSlots.Induction.OUT_X, iy);
        }
    }

    @Override
    protected Component headline() {
        return Component.translatable(K + "melt", menu.getTemperature());
    }

    @Override
    protected int headlineColour() { return INK; }

    @Override
    protected float headlineBar() { return (float) (menu.getTemperature() / mio_icif_induction_elc.MAX_TEMPERATURE); }

    @Override
    protected float headlineBarMarker() { return (float) (mio_icif_induction_elc.SMELT_START / mio_icif_induction_elc.MAX_TEMPERATURE); }

    @Override
    protected List<Component> readouts() {
        List<Component> lines = new ArrayList<>();
        lines.add(Component.translatable(K + "induction_power", menu.getPowerNow(), Math.round(menu.getSmeltRatePermille() / 10.0)));
        int s = menu.secondsToFullHeat();
        lines.add(s > 0 ? Component.translatable(K + "to_full_heat", s) : Component.translatable(K + "at_full_heat",
            menu.isInsulated() ? 2 : 8));
        return lines;
    }

    @Override
    protected int status() { return menu.getStatus(); }

    @Override
    protected void drawExtras(GuiGraphics g, int x, int y, int mouseX, int mouseY) {
        // 0.1.11.143: icons in the figure window's corner (heat hold shows pressed while on)
        iconButton(g, 1, ICON_FLAME, menu.isHeatHold(), InductionElcMenu.BUTTON_HEAT_HOLD, mouseX, mouseY,
            List.of(Component.translatable(K + "heat_hold"), Component.translatable(K + "tip.heat_hold.1").withStyle(net.minecraft.ChatFormatting.GRAY),
                Component.translatable(K + "tip.heat_hold.2").withStyle(net.minecraft.ChatFormatting.GRAY)));
        iconButton(g, 0, ICON_XP, false, InductionElcMenu.BUTTON_COLLECT_XP, mouseX, mouseY,
            List.of(Component.translatable(K + "collect_xp", f1(menu.getStoredXpTenths() / 10.0)),
                Component.translatable(K + "tip.collect_xp").withStyle(net.minecraft.ChatFormatting.GRAY)));
    }

    @Override
    protected int buttonCount() { return 2; }

    @Override
    protected List<Component> figureTooltip() {
        return List.of(Component.translatable(K + "tip.induction.1"), Component.translatable(K + "tip.induction.2"),
            Component.translatable(K + "tip.induction.3"));
    }
}
