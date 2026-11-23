package com.miophas.singularity_iteration.common.client.screen;

import com.miophas.singularity_iteration.common.menu.MergedSlots;
import com.miophas.singularity_iteration.common.blockentity.producer.mio_icif_centrifuge_elc;
import com.miophas.singularity_iteration.common.client.suit.FcsDraw;
import com.miophas.singularity_iteration.common.menu.producer.CentrifugeElcMenu;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/**
 * 热能离心机界面. 0.1.11.114 (processing overhaul M7): the instrument screen (docs/gui_processing_machines.md). The figure is
 * the thermal centrifuge from above: the heater ring glowing with the heat, and the rotor with six buckets turning at the
 * synced rotor speed. The headline is the speed in rpm with a bar; the readouts give the heat against what the recipe
 * needs, the centrifugal force, the energy and the time.
 */
@OnlyIn(Dist.CLIENT)
@SuppressWarnings("null")
public class mio_icif_gui_centrifuge_elc extends ProcessInstrumentScreen<CentrifugeElcMenu> {

    private static final float TURNS_PER_SECOND = 2.2F;
    private float angle;

    public mio_icif_gui_centrifuge_elc(CentrifugeElcMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
    }

    private float speed() { return Math.max(0, Math.min(1, menu.getRotorSpeedPermille() / 1000F)); }

    @Override
    protected void drawFigure(GuiGraphics g, float cx, float cy, float dt) {
        float tau = 2 * (float) Math.PI;
        angle = (angle + dt * speed() * TURNS_PER_SECOND * tau) % tau;
        FcsDraw.disc(g, cx, cy, 38, SHELL);
        // the heater ring: dark when cold, red from a little heat, pale yellow at 5,000 HU
        double celsius = menu.getHeat() <= 0 ? 20 : 450 + 1100.0 * menu.getHeat() / Math.max(1, menu.getMaxHeat());
        FcsDraw.circle(g, cx, cy, 33, 4.5F, glow(celsius));
        FcsDraw.disc(g, cx, cy, 28, CHAMBER);
        for (int k = 0; k < 6; k++) {                            // the arms and the buckets
            float a = angle + k * tau / 6;
            blade(g, cx, cy, a, 5, 18, 1.2F, 1.2F, STEEL);
            blade(g, cx, cy, a, 17, 26, 4F, 4F, STEEL_LIGHT);
        }
        FcsDraw.disc(g, cx, cy, 6, STEEL);
    }

    // ---- 0.1.11.178: the merged drawing (panel coordinates; the slots are MergedSlots.Centrifuge's); 0.1.11.180: 176 wide ----
    // The centrifuge from the front: the input in the hopper on top, the rotor with its six buckets turning at the rotor
    // speed inside the heater ring (dark when cold, red to pale yellow with the heat), the cable from the battery into the
    // casing, the arrow to the rack of the three outputs.
    private static final int ROTOR_X = 80, ROTOR_Y = 66;

    @Override protected int arrowX() { return 118; }
    @Override protected int arrowY() { return MergedSlots.Centrifuge.OUT_TOP_Y + MergedSlots.Centrifuge.OUT_STEP + 1; }
    @Override protected int[] machineBody() { return new int[]{50, 38, 60, 57}; }

    @Override
    protected void drawMachine(GuiGraphics g, float dt) {
        float tau = 2 * (float) Math.PI;
        angle = (angle + dt * speed() * TURNS_PER_SECOND * tau) % tau;
        MachineArt.cable(g, 28, 82, 50, 82);
        MachineArt.iron(g, 50, 38, 60, 57);                                           // the casing
        double celsius = menu.getHeat() <= 0 ? 20 : 450 + 1100.0 * menu.getHeat() / Math.max(1, menu.getMaxHeat());
        FcsDraw.circle(g, ROTOR_X, ROTOR_Y, 23, 3.5F, glow(celsius));                  // the heater ring
        FcsDraw.disc(g, ROTOR_X, ROTOR_Y, 20, MachineArt.WINDOW);
        for (int k = 0; k < 6; k++) {                                                 // the arms and the buckets
            float a = angle + k * tau / 6;
            blade(g, ROTOR_X, ROTOR_Y, a, 4, 12, 1, 1, MachineArt.STEEL);
            blade(g, ROTOR_X, ROTOR_Y, a, 11, 17, 3, 3, MachineArt.STEEL_LIGHT);
        }
        FcsDraw.disc(g, ROTOR_X, ROTOR_Y, 4, MachineArt.STEEL);
        MachineArt.hopper(g, MergedSlots.Centrifuge.IN_X, MergedSlots.Centrifuge.IN_Y, 38);
        MachineArt.rack(g, MergedSlots.Centrifuge.OUT_X, MergedSlots.Centrifuge.OUT_TOP_Y, 3, MergedSlots.Centrifuge.OUT_STEP);
    }

    @Override
    protected Component headline() {
        return Component.translatable(K + "rpm", String.format(Locale.ROOT, "%,d", Math.round(mio_icif_centrifuge_elc.FULL_RPM * speed())));
    }

    @Override
    protected int headlineColour() { return INK; }

    @Override
    protected float headlineBar() { return speed(); }

    @Override
    protected List<Component> readouts() {
        List<Component> lines = new ArrayList<>();
        lines.add(Component.translatable(K + "heat_line", String.format(Locale.ROOT, "%,d", menu.getHeat()),
            String.format(Locale.ROOT, "%,d", menu.getRequiredHeat())));
        double omega = mio_icif_centrifuge_elc.FULL_RPM * speed() * 2 * Math.PI / 60;
        lines.add(Component.translatable(K + "rcf", String.format(Locale.ROOT, "%,d", Math.round(omega * omega * mio_icif_centrifuge_elc.RADIUS_M / 9.81))));
        lines.add(Component.translatable(K + "energy_line", mio_icif_centrifuge_elc.DEFAULT_ENERGY_PER_TICK * menu.getMaxProgress(),
            mio_icif_centrifuge_elc.DEFAULT_ENERGY_PER_TICK, menu.getMaxProgress()));
        return lines;
    }

    @Override
    protected int status() { return menu.getStatus(); }

    @Override
    protected double energyFraction() { return menu.getEnergyPermille() / 1000.0; }

    @Override
    protected List<Component> figureTooltip() {
        return List.of(Component.translatable(K + "tip.centrifuge.1"), Component.translatable(K + "tip.centrifuge.2"),
            Component.translatable(K + "tip.centrifuge.3"));
    }
}
