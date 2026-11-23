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

    /** 0.1.11.178: the tool's part of the bench (panel x, y, w, h), where the formers' screens show their hover text. */
    static final int[] BENCH_TOOL = {56, 18, 48, 77};   // 0.1.11.180: 176 wide

    /**
     * 0.1.11.178: the formers' merged drawing, in panel coordinates (the slots are MergedSlots.Former's): the bench with the
     * work's slot on the infeed table on the left, the tool of the mode in the middle, the output's tray on the outfeed, the
     * drive under the bench wired to the battery. Rolling: the bar runs between the two rolls (the lower one in the bed's
     * opening) and leaves as the thin plate. Cutting: the slanted shear blade comes down through the plate. Extruding: the
     * ram pushes the billet through the die and the wire grows out of it. {@code p} is the operation's progress (0..1).
     */
    static void bench(GuiGraphics g, String mode, float p, float spin, boolean running) {
        int active = running ? MachineArt.AMBER : MachineArt.STEEL_LIGHT;
        MachineArt.cable(g, 28, 82, 110, 82);
        MachineArt.iron(g, 30, 70, 34, 8);                                            // the bed, open under the tool
        MachineArt.iron(g, 96, 70, 74, 8);
        MachineArt.iron(g, 34, 78, 8, 17);                                            // the legs
        MachineArt.iron(g, 158, 78, 8, 17);
        MachineArt.motor(g, 110, 78, 30, 14);                                         // the drive
        int tx = 80;
        switch (mode) {
            case "CUTTING" -> {
                FcsDraw.fill(g, 53, 62, 118, 66, WORK);                               // the plate
                MachineArt.iron(g, 64, 66, 32, 4);                                    // the lower blade on its block
                MachineArt.iron(g, 64, 70, 32, 8);
                MachineArt.iron(g, 56, 18, 48, 7);                                    // the crosshead and the guides
                MachineArt.iron(g, 58, 25, 6, 41);
                MachineArt.iron(g, 96, 25, 6, 41);
                float travel = running ? (p < 0.8F ? p / 0.8F : 1 - (p - 0.8F) / 0.2F) : 0;
                int edge = Math.round(34 + travel * 26);
                FcsDraw.fill(g, 78, 25, 82, edge - 6, MachineArt.STEEL_LIGHT);
                FcsDraw.quad(g, 66, edge - 6, 94, edge - 6, 94, edge - 2, 66, edge + 2, active);   // the slanted blade
            }
            case "EXTRUDING" -> {
                MachineArt.iron(g, 54, 50, 30, 20);                                   // the container
                FcsDraw.fill(g, 58, 54, 80, 66, MachineArt.WINDOW);
                MachineArt.iron(g, 64, 70, 32, 8);
                float push = running ? p : 0;
                int ram = Math.round(58 + push * 12);
                FcsDraw.fill(g, 50, 58, ram, 62, MachineArt.STEEL_LIGHT);             // the ram
                FcsDraw.fill(g, ram, 55, ram + 4, 65, active);
                if (ram + 4 < 80) FcsDraw.fill(g, ram + 4, 55, 80, 65, WORK_HOT);    // the billet
                MachineArt.iron(g, 84, 44, 8, 26);                                    // the die
                int wire = 92 + Math.round(push * 26);
                if (wire > 92) FcsDraw.fill(g, 92, 59, wire, 61, WORK);               // the wire
            }
            default -> {   // ROLLING
                FcsDraw.fill(g, 53, 57, 64, 63, WORK);                                // the bar in
                FcsDraw.fill(g, 96, 59, 116, 61, WORK);                               // the plate out
                MachineArt.iron(g, 64, 25, 6, 70);                                    // the housing, down to the floor
                MachineArt.iron(g, 90, 25, 6, 70);
                MachineArt.iron(g, 62, 18, 36, 7);
                for (int k = -1; k <= 1; k += 2) {
                    float ry = 60 + k * 10, a = k * spin;
                    FcsDraw.disc(g, tx, ry, 10, MachineArt.STEEL);
                    FcsDraw.disc(g, tx, ry, 8, MachineArt.STEEL_LIGHT);
                    ProcessInstrumentScreen.blade(g, tx, ry, a, 0, 8, 0.8F, 0.8F, running ? MachineArt.AMBER : MachineArt.STEEL);
                    ProcessInstrumentScreen.blade(g, tx, ry, a + (float) Math.PI, 0, 8, 0.8F, 0.8F, running ? MachineArt.AMBER : MachineArt.STEEL);
                    FcsDraw.disc(g, tx, ry, 2.5F, MachineArt.STEEL_DARK);
                }
            }
        }
        MachineArt.tray(g, com.miophas.singularity_iteration.common.menu.MergedSlots.Former.OUT_X,
            com.miophas.singularity_iteration.common.menu.MergedSlots.Former.OUT_Y);
    }
}
