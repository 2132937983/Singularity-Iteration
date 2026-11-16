// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.client.screen;

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
    protected void drawWindow(GuiGraphics g, int x, int y, double time) {
        float u = WindRoseView.u();
        int kuIn = v(6), eu = v(5), kind = v(8), rpm = v(9), max = Math.max(1, v(10));
        double shown = kind == 0 ? 0 : rpm > 0 ? rpm : 1500 * Math.sqrt(Math.min(1, kuIn / (double) max));
        double angle = PlantMimic.angle(time, shown);
        String source = kind == 0 ? "source.none" : kind == 1 ? "source.natural" : "source.other";
        PlantMimic.box(g, x + 6, y + 14, 40, 24, kind == 0 ? 0xFFE6E9EA : 0xFFDDE3E6);
        PlantMimic.label(g, Component.translatable(K + source), x + 26, y + 22, 0x1C2524);
        PlantMimic.shaft(g, x + 46, y + 26, x + 130, y + 26);
        PlantMimic.flywheel(g, x + 76, y + 26, 10, angle);
        PlantMimic.label(g, Component.literal(Math.round(shown) + " rpm"), x + 76, y + 40, 0x2A5D8A);
        PlantMimic.label(g, Component.literal(kuIn + " KU/t"), x + 108, y + 16, 0x2A5D8A);
        PlantMimic.machine(g, x + 140, y + 26, 10, "G", 0xFFDDE3E6, u);
        PlantMimic.pipe(g, new float[]{x + 150, y + 26, x + 200, y + 26}, 0xFF2F6E4F, 3, eu > 0 ? Math.min(1.5, eu / 200.0) : 0, time);
        PlantMimic.label(g, Component.literal(eu + " EU/t"), x + 176, y + 16, 0x2F6E4F);
        // input load: how much of the generator's intake the shaft fills
        float load = (float) Math.min(1, kuIn / (double) max);
        g.fill(x + 120, y + 42, x + 160, y + 45, 0xFF33393A);
        WindRoseView.fill(g, x + 120, y + 42, 40 * load, 3, 0xFF2A5D8A);

        line(g, x, y, 0, Component.translatable(K + "in", kuIn, f("%,d", max), Math.round(load * 100) + "%"), WindRoseView.INK);
        Component ratio = Component.translatable(K + (turbo ? (kind == 2 ? "ratio_other" : "ratio_natural") : "ratio"), f("%.2f", v(7) / 1000.0));
        line(g, x, y, 1, ratio.copy().append(" → ").append(Component.literal(eu + " EU/t")), WindRoseView.DIM);
        String state; int colour = 0xA0602A;
        if (v(1) > 0 && v(0) >= v(1)) state = "state.full";
        else if (kind == 0) state = "state.no_source";
        else if (kuIn <= 0) state = "state.idle";
        else { state = "state.running"; colour = 0x2F6E4F; }
        line(g, x, y, 2, Component.translatable(K + state), colour);
    }

    @Override
    protected void drawStrip(GuiGraphics g, int x, int y) {
        long e = Integer.toUnsignedLong(v(0)), cap = Math.max(1, Integer.toUnsignedLong(v(1)));
        bar(g, x, y, Component.translatable(K + "stored"), e / (double) cap, Component.literal(f("%,d / %,d EU", e, cap)));
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        int rx = mouseX - leftPos, ry = mouseY - topPos;
        if (rx >= wx && rx < wx + ww && ry >= wy && ry < wy + wh)
            g.renderComponentTooltip(this.font, turbo
                ? List.of(Component.translatable(K + "tip1"), Component.translatable(K + "tip_turbo"))
                : List.of(Component.translatable(K + "tip1")), mouseX, mouseY);
    }
}
