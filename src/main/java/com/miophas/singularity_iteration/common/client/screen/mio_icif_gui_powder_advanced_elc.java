package com.miophas.singularity_iteration.common.client.screen;

import com.miophas.singularity_iteration.common.menu.MergedSlots;
import com.miophas.singularity_iteration.common.blockentity.producer.mio_icif_powder_advanced_elc;
import com.miophas.singularity_iteration.common.client.suit.FcsDraw;
import com.miophas.singularity_iteration.common.menu.producer.PowderAdvancedElcMenu;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/**
 * 进阶旋风打粉机界面. 0.1.11.110 (processing overhaul M3): the instrument screen (docs/gui_processing_machines.md). The
 * figure is the air-swept mill seen from above: the grinding ring with three rollers and, in the middle, the classifier
 * rotor with its blades, all turning at the synced rotor speed. The readouts give the speed (with a bar), the input, the
 * energy, the cut size d50 and the circulating load; a flat button switches hold speed.
 */
@OnlyIn(Dist.CLIENT)
@SuppressWarnings("null")
public class mio_icif_gui_powder_advanced_elc extends ProcessInstrumentScreen<PowderAdvancedElcMenu> {

    private static final float CLASSIFIER_TURNS_PER_SECOND = 1.4F, ROLLER_ORBITS_PER_SECOND = 0.35F;
    private float classifier, orbit;

    public mio_icif_gui_powder_advanced_elc(PowderAdvancedElcMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
    }

    private float speed() { return Math.max(0, Math.min(1, menu.getRotorSpeedPermille() / 1000F)); }

    @Override
    protected void drawFigure(GuiGraphics g, float cx, float cy, float dt) {
        float s = speed(), tau = 2 * (float) Math.PI;
        classifier = (classifier + dt * s * CLASSIFIER_TURNS_PER_SECOND * tau) % tau;
        orbit = (orbit + dt * s * ROLLER_ORBITS_PER_SECOND * tau) % tau;
        float r = 37;
        FcsDraw.disc(g, cx, cy, r + 4, SHELL);
        FcsDraw.disc(g, cx, cy, r, CHAMBER);
        FcsDraw.disc(g, cx, cy, r - 5, SHELL);        // the grinding ring
        FcsDraw.disc(g, cx, cy, r - 10, CHAMBER);
        for (int k = 0; k < 3; k++) {                    // the rollers, rolling round the ring
            float a = orbit + k * tau / 3;
            float x = cx + (float) Math.cos(a) * (r - 8), y = cy + (float) Math.sin(a) * (r - 8);
            FcsDraw.disc(g, x, y, 6, STEEL);
            blade(g, x, y, -classifier * 2, 0, 5, 0.6F, 0.6F, STEEL_LIGHT);
        }
        for (int k = 0; k < 16; k++)                     // the classifier blades
            blade(g, cx, cy, classifier + k * tau / 16, 5, 17, 1.2F, 2.4F, (k % 2 == 0 ? 0xFF000000 | BLUE : 0xFF6E9ACB));
        FcsDraw.disc(g, cx, cy, 5, STEEL);
    }

    // ---- 0.1.11.178: the merged drawing (panel coordinates; the slots are MergedSlots.Cyclone's); 0.1.11.180: 176 wide ----
    // The ring mill from the front with the input in its hopper, the motor on the left wired to the battery, the rollers
    // rolling round the ring and the classifier turning at the rotor speed; the progress arrow up to the cyclone, the dust
    // swirling down its barrel and cone, out of the spigot into the tray under it.
    private static final int MILL_X = 79, MILL_Y = 67, CYCLONE_X = 141;
    private float swirl;

    @Override protected int arrowX() { return 106; }
    @Override protected int arrowY() { return 40; }
    @Override protected int[] machineBody() { return new int[]{32, 40, 72, 54}; }

    @Override
    protected List<Component> mergedTooltip(int rx, int ry) {
        // EXPLAIN_TIP_HIDDEN (0.1.11.182): the cyclone's how-it-works text, hidden for now
        // return inside(rx, ry, CYCLONE_X - 11, 20, 22, 47) ? figureTooltip() : super.mergedTooltip(rx, ry);
        return super.mergedTooltip(rx, ry);
    }

    @Override
    protected void drawMachine(GuiGraphics g, float dt) {
        float s = speed(), tau = 2 * (float) Math.PI;
        classifier = (classifier + dt * s * CLASSIFIER_TURNS_PER_SECOND * tau) % tau;
        orbit = (orbit + dt * s * ROLLER_ORBITS_PER_SECOND * tau) % tau;
        swirl = (swirl + dt * s * 1.5F) % 1;
        MachineArt.cable(g, 28, 82, 34, 82);
        MachineArt.motor(g, 32, 66, 20, 24);
        FcsDraw.fill(g, 52, 74, 56, 80, MachineArt.STEEL_DARK);
        MachineArt.iron(g, 54, 40, 50, 54);
        FcsDraw.disc(g, MILL_X, MILL_Y, 21, MachineArt.WINDOW);
        FcsDraw.circle(g, MILL_X, MILL_Y, 17.5F, 3, MachineArt.STEEL_DARK);           // the grinding ring
        for (int k = 0; k < 3; k++) {                                                 // the rollers
            float a = orbit + k * tau / 3;
            float x = MILL_X + (float) Math.cos(a) * 12, y = MILL_Y + (float) Math.sin(a) * 12;
            FcsDraw.disc(g, x, y, 4.5F, MachineArt.STEEL_LIGHT);
            blade(g, x, y, -classifier * 2, 0, 3.5F, 0.6F, 0.6F, MachineArt.STEEL_DARK);
        }
        for (int k = 0; k < 8; k++)                                                   // the classifier
            blade(g, MILL_X, MILL_Y, classifier + k * tau / 8, 3, 7, 0.8F, 1.3F, k % 2 == 0 ? 0xFF000000 | BLUE : 0xFF6E9ACB);
        FcsDraw.disc(g, MILL_X, MILL_Y, 3, MachineArt.STEEL);
        MachineArt.hopper(g, MergedSlots.Cyclone.IN_X, MergedSlots.Cyclone.IN_Y, 40);
        MachineArt.steel(g, CYCLONE_X - 11, 20, 22, 28);                              // the barrel
        FcsDraw.fill(g, CYCLONE_X - 8, 23, CYCLONE_X + 8, 45, MachineArt.WINDOW);
        if (s > 0) for (int k = 0; k < 5; k++) {                                      // the dust swirling down
            float ph = (swirl + k / 5F) % 1, y = 24 + ph * 19, x = CYCLONE_X + (float) Math.sin(ph * tau * 2) * 6;
            FcsDraw.fill(g, x - 1, y, x + 1, y + 1, 0xFF6E9ACB);
        }
        for (int y = 48; y < 62; y++) {                                               // the cone
            int half = Math.round(11 - 8 * (y - 48) / 14F);
            FcsDraw.fill(g, CYCLONE_X - half, y, CYCLONE_X + half, y + 1, MachineArt.STEEL);
            FcsDraw.fill(g, CYCLONE_X - half, y, CYCLONE_X + 1 - half, y + 1, MachineArt.STEEL_LIGHT);
            FcsDraw.fill(g, CYCLONE_X - 1 + half, y, CYCLONE_X + half, y + 1, MachineArt.STEEL_DARK);
        }
        MachineArt.steel(g, CYCLONE_X - 3, 62, 6, 5);                                 // the spigot
        MachineArt.tray(g, MergedSlots.Cyclone.OUT_X, MergedSlots.Cyclone.OUT_Y);
    }

    @Override
    protected Component headline() {
        return Component.translatable(K + "speed", Math.round(speed() * 100));
    }

    @Override
    protected int headlineColour() { return INK; }

    @Override
    protected List<Component> readouts() {
        List<Component> lines = new ArrayList<>();
        ItemStack input = menu.getSlot(mio_icif_powder_advanced_elc.INPUT_SLOT).getItem();
        if (input.isEmpty()) lines.add(Component.translatable(K + "no_material"));
        else lines.add(Component.translatable(K + "material", input.getHoverName(), f1(menu.getWorkIndexTenths() / 10.0)));
        float s = speed();
        lines.add(Component.translatable(K + "energy_line", menu.getEnergyPerOperation(), Math.round(mio_icif_powder_advanced_elc.DEFAULT_ENERGY_PER_TICK * s),
            menu.getMaxProgress()));
        lines.add(s > 0
            ? Component.translatable(K + "cyclone", Math.round(mio_icif_powder_advanced_elc.FULL_SPEED_D50 / Math.sqrt(s)),
                Math.round(mio_icif_powder_advanced_elc.FULL_SPEED_CIRCULATING_LOAD * s))
            : Component.translatable(K + "cyclone_idle"));
        return lines;
    }

    @Override
    protected int status() { return menu.getStatus(); }

    @Override
    protected void drawExtras(GuiGraphics g, int x, int y, int mouseX, int mouseY) {
        // 0.1.11.143: an icon in the figure window's corner, pressed while the speed is held
        iconButton(g, 0, ICON_LOCK, menu.isHoldSpeedOn(), PowderAdvancedElcMenu.BUTTON_HOLD_SPEED, mouseX, mouseY,
            List.of(Component.translatable(K + "hold"), Component.translatable(K + "tip.hold.1").withStyle(net.minecraft.ChatFormatting.GRAY),
                Component.translatable(K + "tip.hold.2").withStyle(net.minecraft.ChatFormatting.GRAY)));
    }

    @Override
    protected float headlineBar() { return speed(); }

    @Override
    protected int buttonCount() { return 1; }

    @Override
    protected List<Component> figureTooltip() {
        return List.of(Component.translatable(K + "tip.cyclone.1"), Component.translatable(K + "tip.cyclone.2"),
            Component.translatable(K + "tip.cyclone.3"), Component.translatable(K + "tip.bond"));
    }
}
