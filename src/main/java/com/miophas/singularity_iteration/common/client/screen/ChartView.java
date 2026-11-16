// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.client.screen;

import com.miophas.singularity_iteration.common.client.suit.FcsDraw;
import com.miophas.singularity_iteration.common.energy.Recorder;
import java.util.function.IntUnaryOperator;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

/** 0.1.11.19: the strip chart of the recorder screens: {@link Recorder#SAMPLES} samples, reference lines and marks. */
final class ChartView {
    private ChartView() {}

    /** A dashed reference line at {@code value} with a label at the right. */
    record Ref(double value, int argb, Component label) {}

    static void draw(GuiGraphics g, float x, float y, float w, float h, IntUnaryOperator sample, double max, int pen,
                     int[] markColours, Ref... refs) {
        float u = WindRoseView.u();
        g.fill((int) x - 1, (int) y - 1, (int) (x + w) + 1, (int) (y + h) + 1, 0xFFB8C0C2);
        g.fill((int) x, (int) y, (int) (x + w), (int) (y + h), 0xFFFBFCFC);
        for (int k = 1; k < 4; k++) g.fill((int) (x + w * k / 4), (int) y, (int) (x + w * k / 4) + 1, (int) (y + h), 0xFFEDEFEF);
        for (Ref r : refs) {
            float ry = (float) (y + h - h * Math.min(1, r.value() / max));
            WindRoseView.dashed(g, x, ry, x + w, ry, r.argb());
            PumpCrossSection.text(g, r.label(), x + w - com.miophas.singularity_iteration.common.client.suit.AsciiHud.width(r.label(), u) - 1, ry - 8 * u, r.argb() & 0xFFFFFF, u);
        }
        float px = 0, py = 0;
        for (int i = 0; i < Recorder.SAMPLES; i++) {
            int s = sample.applyAsInt(i);
            float nx = x + w * i / (Recorder.SAMPLES - 1F), ny = (float) (y + h - h * Math.min(1, Recorder.value(s) / max));
            if (i > 0) FcsDraw.line(g, px, py, nx, ny, 1.1F, pen);
            int m = Recorder.markOf(s);
            if (m > 0 && m <= markColours.length) g.fill((int) nx - 1, (int) (y + h) - 4, (int) nx + 1, (int) (y + h) - 1, markColours[m - 1]);
            px = nx;
            py = ny;
        }
        if (px > 0) FcsDraw.disc(g, px, py, 1.6F, pen);
    }
}
