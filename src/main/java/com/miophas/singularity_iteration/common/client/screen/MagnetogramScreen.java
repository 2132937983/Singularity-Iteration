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
 */
@OnlyIn(Dist.CLIENT)
@SuppressWarnings("null")
public class MagnetogramScreen extends MimicScreen<GeomagneticGeneratorMenu> {
    static final String K = "gui.mio_icif.geomag.";
    static final int PEN = 0xFF5A3FA8;

    public MagnetogramScreen(GeomagneticGeneratorMenu menu, Inventory inv, Component title) { super(menu, inv, title); }

    private int v(int i) { return menu.value(i); }

    @Override
    protected void drawWindow(GuiGraphics g, int x, int y, double time) {
        float u = WindRoseView.u();
        float cx = x + 4, cy = y + 4, cw = 146, ch = 34;
        g.fill((int) cx - 1, (int) cy - 1, (int) (cx + cw) + 1, (int) (cy + ch + 18) + 1, 0xFFB8C0C2);
        g.fill((int) cx, (int) cy, (int) (cx + cw), (int) (cy + ch + 18), 0xFFFBFCFC);
        // day boundaries: the trace ends now, three days long
        int now = v(7);
        for (int d = 0; d <= 3; d++) {
            float bx = cx + cw * (1 - (now + d * (float) GeomagPhysics.DAY) / GeomagneticGeneratorMenu.SPAN);
            if (bx > cx && bx < cx + cw) g.fill((int) bx, (int) cy, (int) bx + 1, (int) (cy + ch + 18), 0xFFE2E5EC);
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
        g.fill((int) (cx + cw) - 1, (int) cy, (int) (cx + cw), (int) (cy + ch + 18), 0xFFC0392B);
        double kp = v(5) / 10.0, dist = v(4) / 1000.0, site = v(6) / 1000.0;
        PumpCrossSection.text(g, Component.literal(f("%,d EU/t", v(3))), x + 156, y + 6, 0x2F6E4F, u);
        PumpCrossSection.text(g, Component.literal(f("Kp %.1f", kp)), x + 156, y + 18, kp >= 5 ? 0xC0392B : 0x1C2524, u);
        PumpCrossSection.text(g, Component.literal(f("×%.2f", dist)), x + 156, y + 30, PEN & 0xFFFFFF, u);
        PumpCrossSection.text(g, Component.translatable(K + "site", f("%.2f", site)), x + 156, y + 42, 0x5B6867, u);

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
            g.renderComponentTooltip(this.font, List.of(Component.translatable(K + "tip1"), Component.translatable(K + "tip2"),
                Component.translatable(K + "tip3")), mouseX, mouseY);
    }
}
