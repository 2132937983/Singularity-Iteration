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
 *
 * <p>0.1.11.181 (176 wide, merged): the lava bucket over the tank's filler and the empty one under it, the tank, the heat exchanger with its generator under the lava around it (a square a block), the borehole log, the output cable to the battery. The sum went (the readouts say it).
 */
@OnlyIn(Dist.CLIENT)
@SuppressWarnings("null")
public class GeoScreen extends MimicScreen<GeoGeneratorMenu> {
    static final String K = "gui.mio_icif.geo.";
    static final int LAVA = 0xFFD9531E, FLOW = 0xFFF0913A;

    public GeoScreen(GeoGeneratorMenu menu, Inventory inv, Component title) { super(menu, inv, title); }

    private int v(int i) { return menu.value(i); }

    @Override
    protected void drawMachine(GuiGraphics g, int x, int y, double time) {
        boolean nether = v(10) != 0, running = v(11) != 0 || v(4) > 0;
        int yLevel = v(9), sources = v(5), flowing = v(6);
        double factor = v(7) / 1000.0;
        // 0.1.11.181: the lava bucket over the filler, the empty one under it, the lava tank, the heat exchanger and generator
        MachineArt.filler(g, x + GeoGeneratorMenu.CONTAINER_X - 2, y + GeoGeneratorMenu.CONTAINER_IN_Y + 19, 20);
        MachineArt.pipe(g, x + 28, x + 34, y + 39, MachineArt.LAVA, -1);
        MachineArt.level(g, x + 36, y + 19, 8, 52, v(3) > 0 ? v(2) / (double) v(3) : 0, MachineArt.LAVA);
        MachineArt.pipe(g, x + 46, x + 52, y + 56, v(11) != 0 ? MachineArt.LAVA : 0, MachineArt.phase(time, v(11) != 0 ? 0.5 : 0));
        MachineArt.iron(g, x + 52, y + 42, 36, 32);
        MachineArt.sunk(g, x + 55, y + 45, 30, 10, MachineArt.WINDOW);
        if (running) FcsDraw.fill(g, x + 56, y + 50, x + 84, y + 54, MachineArt.LAVA);
        MachineArt.sunk(g, x + 55, y + 58, 30, 13, MachineArt.WINDOW);
        for (int k = 56; k < 84; k += 2) FcsDraw.fill(g, x + k, y + 59, x + k + 1, y + 70, MachineArt.COPPER);
        // the lava around it over the exchanger: a square a source block (dark) or flowing block (light), up to 18
        for (int i = 0; i < Math.min(18, sources + flowing); i++)
            FcsDraw.fill(g, x + 52 + (i % 6) * 6, y + 18 + (i / 6) * 7, x + 57 + (i % 6) * 6, y + 23 + (i / 6) * 7, i < sources ? LAVA : FLOW);
        // the borehole log: the depth factor (1 to 2 across) from Y 100 at the top down to Y -64, the machine at its height
        float bx = x + 96, by = y + 18, bw = 42, bh = 36;
        FcsDraw.fill(g, (int) bx - 1, (int) by - 1, (int) (bx + bw) + 1, (int) (by + bh) + 1, 0xFFB8C0C2);
        FcsDraw.fill(g, (int) bx, (int) by, (int) (bx + bw), (int) (by + bh), 0xFFFBFCFC);
        for (int yy : new int[]{64, 0, -64}) {
            float ly = by + bh * (100 - yy) / 164F;
            FcsDraw.fill(g, (int) bx, (int) ly, (int) (bx + bw), (int) ly + 1, 0xFFEDEFEF);
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
        FcsDraw.fill(g, (int) (bx + bw * (factor - 1)) - 2, (int) my - 2, (int) (bx + bw * (factor - 1)) + 3, (int) my + 3, 0xFF33393A);
        centred(g, nether ? Component.translatable(K + "nether") : Component.translatable(K + "depth").append(f("%.2f", factor)), bx + bw / 2, by + bh + 2,
            VanillaFrame.TITLE);
        // the output cable to the battery
        MachineArt.euCable(g, running, time, x + 88, y + 70, x + GeoGeneratorMenu.BATTERY_X - 2, y + 70);
        double world = v(4) / 100.0;

        line(g, x, y, 0, Component.translatable(K + "line_world", sources, flowing, f("%.1f", world), f("%.0f", GeoPhysics.CAP)), WindRoseView.INK);
        line(g, x, y, 1, Component.translatable(K + "line_tank", f("%,d", v(2)), f("%,d", v(3)), f("%,d", v(0)), f("%,d", v(1))), WindRoseView.DIM);
        String state; int colour = 0xA0602A;
        if (v(1) > 0 && v(0) >= v(1)) state = "state.full";
        else if (v(11) != 0 || world > 0) { state = "state.running"; colour = 0x2F6E4F; }
        else state = "state.no_lava";
        ReadoutPanel.headline(Component.literal(f("%.1f EU/t", world + (v(11) != 0 ? 20 : 0))), Component.translatable(K + state), colour);   // 0.1.11.145: the output leads, the state beside it
    }

    @Override
    protected void drawStrip(GuiGraphics g, int x, int y) {
        bar(g, x, y, Component.translatable(K + "tank"), v(3) > 0 ? v(2) / (double) v(3) : 0, Component.literal(f("%,d / %,d mB", v(2), v(3))));
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        int rx = mouseX - leftPos, ry = mouseY - topPos;
        // EXPLAIN_TIP_HIDDEN (0.1.11.182): the drawing's how-it-works text, hidden for now
        // if (overMachine(rx, ry))
        //     g.renderComponentTooltip(this.font, List.of(Component.translatable(K + "tip1"), Component.translatable(K + "tip2"),
        //         Component.translatable(K + "tip3")), mouseX, mouseY);
    }

    /** 0.1.11.119: the window is a chart: kept as it was (not pixel art). */
    @Override
    protected boolean pixelArt() { return false; }
}
