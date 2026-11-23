// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.client.screen;

import com.miophas.singularity_iteration.common.client.suit.FcsDraw;

import com.miophas.singularity_iteration.common.menu.generator.MimicGeneratorMenu;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/**
 * 0.1.11.17: the mimic of the kinetic generators (kinetic, turbo, twin turbo): the KU source on the front, the shaft and
 * its coupling turning at the source's speed, the generator and the EU out. Values as in
 * {@link com.miophas.singularity_iteration.common.menu.generator.KineticGeneratorMenu}.
 *
 * <p>0.1.11.181 (176 wide, merged): drawn on the panel; the load is a bar under the generator.
 */
@OnlyIn(Dist.CLIENT)
@SuppressWarnings("null")
public class ShaftScreen<M extends MimicGeneratorMenu> extends MimicScreen<M> {
    private static final String K = "gui.mio_icif.shaft.";
    private final boolean turbo;

    public ShaftScreen(M menu, Inventory inv, Component title, boolean turbo) {
        super(menu, inv, title);
        this.turbo = turbo;
    }

    private int v(int i) { return menu.value(i); }

    @Override
    protected void drawMachine(GuiGraphics g, int x, int y, double time) {
        int kuIn = v(6), eu = v(5), kind = v(8), rpm = v(9), max = Math.max(1, v(10));
        double shown = kind == 0 ? 0 : rpm > 0 ? rpm : 1500 * Math.sqrt(Math.min(1, kuIn / (double) max));
        double angle = PlantMimic.angle(time, shown);
        String source = kind == 0 ? "source.none" : kind == 1 ? "source.natural" : "source.other";
        // 0.1.11.181: the drive's flange on the left (turning while something drives it), the shaft with its flywheel, the
        // generator and the cable out; the load (how much of the generator's intake the shaft fills) under the generator
        label(g, Component.translatable(K + source), x + 10, y + 17, VanillaFrame.TITLE);
        MachineArt.steel(g, x + 10, y + 26, 26, 26);
        MachineArt.sunk(g, x + 13, y + 29, 20, 20, MachineArt.WINDOW);
        for (int k = 0; k < 2; k++) {
            double a = angle + k * Math.PI;
            FcsDraw.line(g, x + 23, y + 39, x + 23 + (float) Math.cos(a) * 8, y + 39 + (float) Math.sin(a) * 8, 2F, kind == 0 ? MachineArt.STEEL : MachineArt.STEEL_LIGHT);
        }
        MachineArt.shaft(g, x + 36, x + 100, y + 39);
        MachineArt.flywheel(g, x + 62, y + 39, 14, angle);
        centred(g, Component.literal(Math.round(shown) + " rpm"), x + 62, y + 57, 0x2A5D8A);
        MachineArt.generator(g, x + 100, y + 26, 34, 26);
        float load = (float) Math.min(1, kuIn / (double) max);
        MachineArt.sunk(g, x + 100, y + 56, 34, 6, MachineArt.GLASS);
        if (load > 0) FcsDraw.fill(g, x + 101, y + 57, x + 101 + Math.max(1, Math.round(32 * load)), y + 61, MachineArt.KU);
        MachineArt.euCable(g, eu > 0, time, x + 134, y + 39, x + 168, y + 39);

        line(g, x, y, 0, Component.translatable(K + "in", kuIn, f("%,d", max), Math.round(load * 100) + "%"), WindRoseView.INK);
        Component ratio = Component.translatable(K + (turbo ? (kind == 2 ? "ratio_other" : "ratio_natural") : "ratio"), f("%.2f", v(7) / 1000.0));
        line(g, x, y, 1, ratio.copy().append(" → ").append(Component.literal(eu + " EU/t")), WindRoseView.DIM);
        String state; int colour = 0xA0602A;
        if (v(1) > 0 && v(0) >= v(1)) state = "state.full";
        else if (kind == 0) state = "state.no_source";
        else if (kuIn <= 0) state = "state.idle";
        else { state = "state.running"; colour = 0x2F6E4F; }
        ReadoutPanel.headline(Component.literal(eu + " EU/t"), Component.translatable(K + state), colour);   // 0.1.11.145: the output leads, the state beside it
    }

    @Override
    protected void drawStrip(GuiGraphics g, int x, int y) {
        long e = Integer.toUnsignedLong(v(0)), cap = Math.max(1, Integer.toUnsignedLong(v(1)));
        bar(g, x, y, Component.translatable(K + "stored"), e / (double) cap, stored(e, cap));
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        int rx = mouseX - leftPos, ry = mouseY - topPos;
        // EXPLAIN_TIP_HIDDEN (0.1.11.182): the drawing's how-it-works text, hidden for now
        // if (overMachine(rx, ry))
        //     g.renderComponentTooltip(this.font, turbo
        //         ? List.of(Component.translatable(K + "tip1"), Component.translatable(K + "tip_turbo"))
        //         : List.of(Component.translatable(K + "tip1")), mouseX, mouseY);
    }
}
