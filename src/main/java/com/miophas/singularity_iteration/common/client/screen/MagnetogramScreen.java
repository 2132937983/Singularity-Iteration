// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.client.screen;

import com.miophas.singularity_iteration.common.client.suit.FcsDraw;
import com.miophas.singularity_iteration.common.energy.GeomagPhysics;
import com.miophas.singularity_iteration.common.menu.generator.GeomagneticGeneratorMenu;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/**
 * 0.1.11.20: the geomagnetic generator's magnetogram: the field's disturbance over the last three days, Kp every three
 * hours under it (red in a storm), the time now at the right edge; the output, Kp and the site ratio beside it. Values
 * as in {@link GeomagneticGeneratorMenu}.
 *
 * <p>0.1.11.181 (176 wide, merged): the record on the left, the pickup coil on the right with the battery under it; the figures beside the record went (they are in the readouts).
 */
@OnlyIn(Dist.CLIENT)
@SuppressWarnings("null")
public class MagnetogramScreen extends MimicScreen<GeomagneticGeneratorMenu> {
    static final String K = "gui.mio_icif.geomag.";
    static final int PEN = 0xFF5A3FA8;

    public MagnetogramScreen(GeomagneticGeneratorMenu menu, Inventory inv, Component title) { super(menu, inv, title); }

    private int v(int i) { return menu.value(i); }

    @Override
    protected void drawMachine(GuiGraphics g, int x, int y, double time) {
        float cx = x + 10, cy = y + 18, cw = 124, ch = 36;
        FcsDraw.fill(g, (int) cx - 1, (int) cy - 1, (int) (cx + cw) + 1, (int) (cy + ch + 18) + 1, 0xFFB8C0C2);
        FcsDraw.fill(g, (int) cx, (int) cy, (int) (cx + cw), (int) (cy + ch + 18), 0xFFFBFCFC);
        // day boundaries: the trace ends now, three days long
        int now = v(7);
        for (int d = 0; d <= 3; d++) {
            float bx = cx + cw * (1 - (now + d * (float) GeomagPhysics.DAY) / GeomagneticGeneratorMenu.SPAN);
            if (bx > cx && bx < cx + cw) FcsDraw.fill(g, (int) bx, (int) cy, (int) bx + 1, (int) (cy + ch + 18), 0xFFE2E5EC);
        }
        float px = 0, py = 0;
        for (int i = 0; i < GeomagneticGeneratorMenu.TRACE; i++) {
            double dv = v(GeomagneticGeneratorMenu.TRACE_AT + i) / 1000.0;
            float nx = cx + cw * (i + 1) / GeomagneticGeneratorMenu.TRACE, ny = (float) (cy + ch - ch * Math.min(1, dv / GeomagPhysics.MAX));
            if (i > 0) FcsDraw.line(g, px, py, nx, ny, 1.1F, PEN);
            px = nx;
            py = ny;
        }
        for (int i = 0; i < GeomagneticGeneratorMenu.KP; i++) {
            double kp = v(GeomagneticGeneratorMenu.KP_AT + i) / 10.0;
            float bx = cx + cw * i / GeomagneticGeneratorMenu.KP, bh = (float) (16 * Math.min(9, kp) / 9);
            int c = kp >= 5 ? 0xFFC0392B : kp >= 4 ? 0xFFD9A32B : 0xFF7FB08A;
            WindRoseView.fill(g, bx + 1, cy + ch + 18 - bh, cw / GeomagneticGeneratorMenu.KP - 2, bh, c);
        }
        FcsDraw.fill(g, (int) (cx + cw) - 1, (int) cy, (int) (cx + cw), (int) (cy + ch + 18), 0xFFC0392B);
        double kp = v(5) / 10.0, dist = v(4) / 1000.0, site = v(6) / 1000.0;
        // 0.1.11.181: the pickup coil beside the record, the battery under it
        MachineArt.iron(g, x + 140, y + 18, 26, 30);
        MachineArt.sunk(g, x + 144, y + 22, 18, 22, MachineArt.WINDOW);
        for (int k = 23; k < 43; k += 2) FcsDraw.fill(g, x + 145, y + k, x + 161, y + k + 1, MachineArt.COPPER);
        MachineArt.euCable(g, v(3) > 0, time, x + 153, y + 48, x + 153, y + GeomagneticGeneratorMenu.BATTERY_Y - 2);

        boolean storm = kp >= 5;
        line(g, x, y, 0, Component.translatable(K + "line_field", f("%.2f", dist), f("%.1f", kp),
            Component.translatable(K + (storm ? "storm" : "quiet"))), WindRoseView.INK);
        line(g, x, y, 1, Component.translatable(K + "line_output", f("%,d", v(3)), f("%.2f", site), f("%.2f", dist)), WindRoseView.DIM);
        String state; int colour = 0xA0602A;
        if (v(2) == 0) state = "state.structure";
        else if (v(1) > 0 && v(0) >= v(1)) state = "state.full";
        else if (site * 6 < 1) state = "state.weak";
        else if (storm) { state = "state.storm"; colour = 0xC0392B; }
        else { state = "state.quiet"; colour = 0x2F6E4F; }
        ReadoutPanel.headline(Component.literal(f("%,d EU/t", v(3))), Component.translatable(K + state), colour);   // 0.1.11.145: the output leads, the state beside it
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
        //     g.renderComponentTooltip(this.font, List.of(Component.translatable(K + "tip1"), Component.translatable(K + "tip2"),
        //         Component.translatable(K + "tip3")), mouseX, mouseY);
    }

    /** 0.1.11.119: the window is a chart: kept as it was (not pixel art). */
    @Override
    protected boolean pixelArt() { return false; }
}
