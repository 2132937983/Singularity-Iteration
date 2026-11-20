package com.miophas.singularity_iteration.common.client.screen;

import com.miophas.singularity_iteration.common.client.suit.FcsDraw;
import net.minecraft.client.gui.GuiGraphics;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/**
 * 0.1.11.115 (processing overhaul M8): the metal formers' figure, by mode, from the side. Rolling: two rolls turning the
 * thick bar into a thin plate. Cutting: a shear blade coming down through the plate. Extruding: the ram pushing the
 * billet through the die, the wire growing out of it. {@code p} is the operation's progress (0..1), {@code spin} the roll
 * angle.
 */
@OnlyIn(Dist.CLIENT)
final class FormingFigure {
    private FormingFigure() {}

    static final int WORK = 0xFFB4BCBA, WORK_HOT = 0xFFC9A27A;

    static void draw(GuiGraphics g, float cx, float cy, String mode, float p, float spin, boolean running) {
        int active = running ? 0xFF000000 | ProcessInstrumentScreen.AMBER : ProcessInstrumentScreen.STEEL_LIGHT;
        switch (mode) {
            case "CUTTING" -> {
                FcsDraw.fill(g, (int) cx - 38, (int) cy + 8, (int) cx + 38, (int) cy + 30, ProcessInstrumentScreen.STEEL);    // the bed and lower blade
                FcsDraw.fill(g, (int) cx - 36, (int) cy + 2, (int) cx + 36, (int) cy + 8, WORK);                               // the plate
                float travel = running ? (p < 0.8F ? p / 0.8F : 1 - (p - 0.8F) / 0.2F) : 0;
                int edge = (int) (cy - 26 + travel * 30);
                FcsDraw.fill(g, (int) cx - 30, (int) cy - 36, (int) cx + 30, (int) cy - 30, ProcessInstrumentScreen.STEEL);   // the crosshead
                FcsDraw.fill(g, (int) cx - 2, (int) cy - 30, (int) cx + 2, edge - 6, ProcessInstrumentScreen.STEEL_LIGHT);
                FcsDraw.quad(g, cx - 14, edge - 6, cx + 14, edge - 6, cx + 14, edge - 2, cx - 14, edge + 2, active);   // the slanted blade
                if (travel > 0.95F) FcsDraw.fill(g, (int) cx - 1, (int) cy + 2, (int) cx + 1, (int) cy + 8, ProcessInstrumentScreen.PAPER);   // the cut
            }
            case "EXTRUDING" -> {
                FcsDraw.fill(g, (int) cx - 38, (int) cy - 14, (int) cx + 6, (int) cy + 14, ProcessInstrumentScreen.STEEL);    // the container
                FcsDraw.fill(g, (int) cx - 34, (int) cy - 9, (int) cx + 2, (int) cy + 9, ProcessInstrumentScreen.CHAMBER);
                float push = running ? p : 0;
                int ram = (int) (cx - 34 + push * 22);
                FcsDraw.fill(g, (int) cx - 46, (int) cy - 3, ram, (int) cy + 3, ProcessInstrumentScreen.STEEL_LIGHT);           // the ram
                FcsDraw.fill(g, ram, (int) cy - 8, ram + 4, (int) cy + 8, active);
                FcsDraw.fill(g, ram + 4, (int) cy - 8, (int) cx + 2, (int) cy + 8, WORK_HOT);                                  // the billet
                FcsDraw.fill(g, (int) cx + 2, (int) cy - 16, (int) cx + 10, (int) cy + 16, ProcessInstrumentScreen.STEEL);     // the die
                int wire = (int) (cx + 10 + push * 30);
                if (wire > cx + 10) FcsDraw.fill(g, (int) cx + 10, (int) cy - 2, wire, (int) cy + 2, WORK);                   // the wire
            }
            default -> {   // ROLLING
                FcsDraw.fill(g, (int) cx - 40, (int) cy - 4, (int) cx - 13, (int) cy + 4, WORK);                              // the thick bar
                FcsDraw.fill(g, (int) cx + 13, (int) cy - 1, (int) cx + 40, (int) cy + 1, WORK);                              // the thin plate
                for (int k = -1; k <= 1; k += 2) {
                    float ry = cy + k * 14.5F;
                    FcsDraw.disc(g, cx, ry, 13, ProcessInstrumentScreen.STEEL);
                    FcsDraw.disc(g, cx, ry, 10, ProcessInstrumentScreen.STEEL_LIGHT);
                    float a = k * spin;
                    ProcessInstrumentScreen.blade(g, cx, ry, a, 0, 10, 0.8F, 0.8F, active);
                    ProcessInstrumentScreen.blade(g, cx, ry, a + (float) Math.PI, 0, 10, 0.8F, 0.8F, active);
                    FcsDraw.disc(g, cx, ry, 2.5F, ProcessInstrumentScreen.STEEL);
                }
            }
        }
    }
}
