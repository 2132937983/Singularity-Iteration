package com.miophas.singularity_iteration.common.client.screen;

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
