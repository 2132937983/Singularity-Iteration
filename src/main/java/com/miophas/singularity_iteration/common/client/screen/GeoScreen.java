// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.client.screen;

import com.miophas.singularity_iteration.common.client.suit.FcsDraw;
import com.miophas.singularity_iteration.common.energy.GeoPhysics;
import com.miophas.singularity_iteration.common.menu.generator.GeoGeneratorMenu;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/**
 * 0.1.11.19: the geothermal generator's recorder: a borehole log (the depth factor from Y 100 down to Y -64, the machine
 * marked at its height), the lava around it as a sum (sources, flowing, times the depth = EU/t), and the tank. Values
 * as in {@link GeoGeneratorMenu}.
 */
@OnlyIn(Dist.CLIENT)
@SuppressWarnings("null")
public class GeoScreen extends MimicScreen<GeoGeneratorMenu> {
    static final String K = "gui.mio_icif.geo.";
    static final int LAVA = 0xFFD9531E, FLOW = 0xFFF0913A;

    public GeoScreen(GeoGeneratorMenu menu, Inventory inv, Component title) { super(menu, inv, title); }

    private int v(int i) { return menu.value(i); }

    @Override
    protected void drawWindow(GuiGraphics g, int x, int y, double time) {
        float u = WindRoseView.u();
        boolean nether = v(10) != 0;
        int yLevel = v(9);
        // the borehole: Y 100 at the top, Y -64 at the bottom; the depth factor 1..2 across
        float bx = x + 18, by = y + 4, bw = 60, bh = 52;
        FcsDraw.fill(g, (int) bx - 1, (int) by - 1, (int) (bx + bw) + 1, (int) (by + bh) + 1, 0xFFB8C0C2);
        FcsDraw.fill(g, (int) bx, (int) by, (int) (bx + bw), (int) (by + bh), 0xFFFBFCFC);
        for (int yy : new int[]{64, 0, -64}) {
            float ly = by + bh * (100 - yy) / 164F;
            FcsDraw.fill(g, (int) bx, (int) ly, (int) (bx + bw), (int) ly + 1, 0xFFEDEFEF);
            Component c = Component.literal(String.valueOf(yy));
            PumpCrossSection.text(g, c, bx - 2 - com.miophas.singularity_iteration.common.client.suit.AsciiHud.width(c, u), ly - 4 * u, 0x5B6867, u);
        }
        float px = 0, py = 0;
        for (int k = 0; k <= 41; k++) {
            int yy = 100 - k * 4;
            double f = nether ? GeoPhysics.NETHER : GeoPhysics.depth(yy);
            float nx = bx + bw * (float) ((f - 1) / 1.0), ny = by + bh * (100 - yy) / 164F;
            if (k > 0) FcsDraw.line(g, px, py, nx, ny, 1.1F, LAVA);
            px = nx;
            py = ny;
        }
        float my = by + bh * (100 - Math.max(-64, Math.min(100, yLevel))) / 164F;
        double factor = v(7) / 1000.0;
        FcsDraw.fill(g, (int) (bx + bw * (factor - 1)) - 2, (int) my - 2, (int) (bx + bw * (factor - 1)) + 3, (int) my + 3, 0xFF33393A);
        PlantMimic.label(g, nether ? Component.translatable(K + "nether") : Component.translatable(K + "depth").append(f("%.2f", factor)), bx + bw / 2, by + bh + 2, WindRoseView.DIM);
        // the sum
        float sx = x + 92;
        int sources = v(5), flowing = v(6);
        for (int i = 0; i < Math.min(24, sources + flowing); i++) {
            int cx = (int) sx + (i % 12) * 7, cy = y + 6 + (i / 12) * 8;
            FcsDraw.fill(g, cx, cy, cx + 5, cy + 6, i < sources ? LAVA : FLOW);
        }
        if (sources + flowing > 24) PumpCrossSection.text(g, Component.literal("+" + (sources + flowing - 24)), sx + 86, y + 6, 0x5B6867, u);
        double world = v(4) / 100.0;
        PumpCrossSection.text(g, Component.literal(f("%.1f × %.2f = %.1f EU/t", v(8) / 100.0, factor, world)), sx, y + 26, 0x1C2524, u);
        PumpCrossSection.text(g, Component.translatable(K + "tank_part", v(11) != 0 ? 20 : 0), sx, y + 36, 0x1C2524, u);
        PumpCrossSection.text(g, Component.literal(f("= %.1f EU/t", world + (v(11) != 0 ? 20 : 0))), sx, y + 46, 0xA8461F, u);

        line(g, x, y, 0, Component.translatable(K + "line_world", sources, flowing, f("%.1f", world), f("%.0f", GeoPhysics.CAP)), WindRoseView.INK);
        line(g, x, y, 1, Component.translatable(K + "line_tank", f("%,d", v(2)), f("%,d", v(3)), f("%,d", v(0)), f("%,d", v(1))), WindRoseView.DIM);
        String state; int colour = 0xA0602A;
        if (v(1) > 0 && v(0) >= v(1)) state = "state.full";
        else if (v(11) != 0 || world > 0) { state = "state.running"; colour = 0x2F6E4F; }
        else state = "state.no_lava";
        line(g, x, y, 2, Component.translatable(K + state), colour);
    }

    @Override
    protected void drawStrip(GuiGraphics g, int x, int y) {
        bar(g, x, y, Component.translatable(K + "tank"), v(3) > 0 ? v(2) / (double) v(3) : 0, Component.literal(f("%,d / %,d mB", v(2), v(3))));
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        int rx = mouseX - leftPos, ry = mouseY - topPos;
        if (rx >= wx && rx < wx + ww && ry >= wy && ry < wy + wh)
            g.renderComponentTooltip(this.font, List.of(Component.translatable(K + "tip1"), Component.translatable(K + "tip2"),
                Component.translatable(K + "tip3")), mouseX, mouseY);
    }

    /** 0.1.11.119: the window is a chart: kept as it was (not pixel art). */
    @Override
    protected boolean pixelArt() { return false; }
}
