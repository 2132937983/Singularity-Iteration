package com.miophas.singularity_iteration.common.client.screen;

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
        int bx = x - leftPos, by = Math.min(y - topPos, PROGRESS_Y - 14);
        menuButton(g, bx, by, 42, 11, Component.translatable(K + "heat_hold"), menu.isHeatHold(), InductionElcMenu.BUTTON_HEAT_HOLD,
            mouseX, mouseY, List.of(Component.translatable(K + "tip.heat_hold.1"), Component.translatable(K + "tip.heat_hold.2")));
        menuButton(g, bx + 46, by, 44, 11, Component.translatable(K + "collect_xp", f1(menu.getStoredXpTenths() / 10.0)), false,
            InductionElcMenu.BUTTON_COLLECT_XP, mouseX, mouseY, List.of(Component.translatable(K + "tip.collect_xp")));
    }

    @Override
    protected List<Component> figureTooltip() {
        return List.of(Component.translatable(K + "tip.induction.1"), Component.translatable(K + "tip.induction.2"),
            Component.translatable(K + "tip.induction.3"));
    }
}
