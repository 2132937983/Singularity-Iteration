package com.miophas.singularity_iteration.common.client.screen;

import com.miophas.singularity_iteration.common.client.suit.FcsDraw;
import net.minecraft.client.gui.GuiGraphics;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.fluids.FluidStack;

/**
 * 0.1.11.175: pixel parts for the merged machine screens (docs/gui_processing_machines.md, "合并布局"), where the slots sit in
 * the machine's own drawing on the vanilla panel instead of beside a separate figure window. One art pixel is one GUI pixel;
 * raised parts are lit from the upper left like the vanilla buttons, sunk ones like the slots.
 *
 * <p>0.1.11.178: the parts draw through {@link FcsDraw#fill}, so they work both straight on the screen (the canner) and in the
 * merged screen's pixel canvas ({@code ProcessInstrumentScreen.drawMachine}), where they mix in order with the round and
 * turning parts. New parts for the machines' own drawings: dark iron, the window, hoppers round an input slot, trays and
 * racks round the outputs, the battery cable, motors and gauges.
 */
@OnlyIn(Dist.CLIENT)
public final class MachineArt {
    private MachineArt() {}

    public static final int STEEL = 0xFF788084, STEEL_LIGHT = 0xFFAAB2B6, STEEL_DARK = 0xFF464C50, GLASS = 0xFF6E6E6E,
        GLINT = 0xFF969696, TICK = 0xFF282828, WATER = 0xFF3F76E4, WATER_LIGHT = 0xFF8CB4F5;
    /** 0.1.11.178: the machines' frames and casings, their dark inside, the cable, the hot parts and the materials. */
    public static final int IRON = 0xFF5E6468, IRON_LIGHT = 0xFF8A9296, IRON_DARK = 0xFF34383C, WINDOW = 0xFF2A2E32,
        WINDOW_LIGHT = 0xFF3E444A, COPPER = 0xFFC47840, RUBBER = 0xFF262626, AMBER = 0xFFD49A12, GLOW = 0xFFF07A1C,
        WORK = 0xFFB4BCBA, MUD = 0xFF6E5A3C, RED = 0xFFC0392B;

    private static void fill(GuiGraphics g, int x0, int y0, int x1, int y1, int argb) {
        if (x1 > x0 && y1 > y0) FcsDraw.fill(g, x0, y0, x1, y1, argb);
    }

    /** A raised block: {@code light} on the upper and left edges, {@code shadow} on the lower and right ones. */
    public static void raised(GuiGraphics g, int x, int y, int w, int h, int fill, int light, int shadow) {
        fill(g, x, y, x + w, y + h, fill);
        fill(g, x, y, x + w - 1, y + 1, light);
        fill(g, x, y, x + 1, y + h - 1, light);
        fill(g, x + 1, y + h - 1, x + w, y + h, shadow);
        fill(g, x + w - 1, y + 1, x + w, y + h, shadow);
    }

    /** A raised steel part of the machine (hopper, filler head, stand). */
    public static void steel(GuiGraphics g, int x, int y, int w, int h) {
        raised(g, x, y, w, h, STEEL, STEEL_LIGHT, STEEL_DARK);
    }

    /** 0.1.11.178: a raised part in dark iron (frames, casings, beds). */
    public static void iron(GuiGraphics g, int x, int y, int w, int h) {
        raised(g, x, y, w, h, IRON, IRON_LIGHT, IRON_DARK);
    }

    /** 0.1.11.178: a part sunk into the panel like a slot (dark upper left, white lower right) filled with {@code fill}. */
    public static void sunk(GuiGraphics g, int x, int y, int w, int h, int fill) {
        fill(g, x, y, x + w - 1, y + 1, VanillaFrame.SLOT_DARK);
        fill(g, x, y, x + 1, y + h - 1, VanillaFrame.SLOT_DARK);
        fill(g, x + 1, y + h - 1, x + w, y + h, VanillaFrame.LIGHT);
        fill(g, x + w - 1, y + 1, x + w, y + h, VanillaFrame.LIGHT);
        fill(g, x + 1, y + 1, x + w - 1, y + h - 1, fill);
    }

    /**
     * 0.1.11.178: a hopper round the input slot at item position (ix, iy): a steel collar three pixels round the slot's frame,
     * then its throat narrowing from 24 to 12 pixels down to {@code bottom} (the machine's top).
     */
    public static void hopper(GuiGraphics g, int ix, int iy, int bottom) {
        steel(g, ix - 6, iy - 3, 28, 22);
        int top = iy + 19;
        for (int y = top; y < bottom; y++) {
            int half = Math.round(12 - 6F * (y - top + 1) / Math.max(1, bottom - top));
            fill(g, ix + 8 - half, y, ix + 8 + half, y + 1, STEEL);
            fill(g, ix + 8 - half, y, ix + 9 - half, y + 1, STEEL_LIGHT);
            fill(g, ix + 7 + half, y, ix + 8 + half, y + 1, STEEL_DARK);
        }
    }

    /** 0.1.11.178: an open tray round the output slot at item position (ix, iy): a wall on each side and a floor. */
    public static void tray(GuiGraphics g, int ix, int iy) {
        steel(g, ix - 5, iy - 2, 4, 23);
        steel(g, ix + 17, iy - 2, 4, 23);
        steel(g, ix - 5, iy + 17, 26, 4);
    }

    /** 0.1.11.178: a rack round {@code n} output slots stacked from item position (ix, iy0), {@code step} apart. */
    public static void rack(GuiGraphics g, int ix, int iy0, int n, int step) {
        int h = (n - 1) * step + 22;
        steel(g, ix - 5, iy0 - 3, 4, h + 1);
        steel(g, ix + 17, iy0 - 3, 4, h + 1);
        steel(g, ix - 5, iy0 + h - 6, 26, 4);
    }

    /**
     * 0.1.11.178: the power cable along the points (x0, y0, x1, y1, ...), each run straight across or down: a copper core one
     * pixel wide in black rubber.
     */
    public static void cable(GuiGraphics g, int... xy) {
        for (int pass = 0; pass < 2; pass++)
            for (int i = 0; i + 3 < xy.length; i += 2) {
                int x0 = Math.min(xy[i], xy[i + 2]), x1 = Math.max(xy[i], xy[i + 2]), y0 = Math.min(xy[i + 1], xy[i + 3]), y1 = Math.max(xy[i + 1], xy[i + 3]);
                if (pass == 0) fill(g, x0 - 1, y0 - 1, x1 + 2, y1 + 2, RUBBER);
                else fill(g, x0, y0, x1 + 1, y1 + 1, COPPER);
            }
    }

    /** 0.1.11.178: an electric motor (or pump unit): a steel box with cooling fins. */
    public static void motor(GuiGraphics g, int x, int y, int w, int h) {
        steel(g, x, y, w, h);
        for (int fx = x + 3; fx < x + w - 3; fx += 3) fill(g, fx, y + 3, fx + 1, y + h - 3, STEEL_DARK);
    }

    /**
     * 0.1.11.178: a gauge column like {@link #tank} without a fluid texture (it can go in the pixel canvas): the steel housing,
     * the sunk glass, {@code colour} up to {@code level} (0..1) and five ticks on the right.
     */
    public static void gauge(GuiGraphics g, int x, int y, int w, int h, float level, int colour) {
        steel(g, x - 2, y - 2, w + 4, h + 4);
        sunk(g, x, y, w, h, GLASS);
        int room = h - 2, filled = level > 0 ? Math.max(1, Math.round(room * Math.min(1, level))) : 0;
        if (filled > 0) fill(g, x + 1, y + h - 1 - filled, x + w - 1, y + h - 1, colour);
        for (int k = 1; k <= 5; k++) {
            int ty = y + 1 + k * room / 6;
            fill(g, x + w - 4, ty, x + w - 1, ty + 1, TICK);
        }
    }

    /**
     * A gauge tank: a steel housing two pixels round a sunk glass chamber (x, y, w, h), the fluid's own texture up to its
     * level (a drop still shows), a glint on the empty glass and five ticks on the right. (The fluid texture cannot go in the
     * pixel canvas: draw it straight on the screen.)
     */
    public static void tank(GuiGraphics g, int x, int y, int w, int h, FluidStack fluid, int capacity) {
        steel(g, x - 2, y - 2, w + 4, h + 4);
        VanillaFrame.inset(g, x, y, w, h, GLASS);
        int room = h - 2, level = 0;
        if (!fluid.isEmpty() && capacity > 0) level = Math.max(1, Math.round(room * Math.min(1F, fluid.getAmount() / (float) capacity)));
        if (level > 0) VanillaFrame.fluid(g, x + 1, y + h - 1 - level, w - 2, level, fluid);
        if (room - level > 3) g.fill(x + 2, y + 2, x + 3, y + h - 2 - level, GLINT);
        for (int k = 1; k <= 5; k++) {
            int ty = y + 1 + k * room / 6;
            g.fill(x + w - 4, ty, x + w - 1, ty + 1, TICK);
        }
    }

    /**
     * A horizontal pipe from x0 to x1, four pixels high round {@code y}: steel when idle, water inside when {@code active},
     * with light dashes running along it while {@code flow} (0..1, the animation phase) is not negative.
     */
    public static void pipeH(GuiGraphics g, int x0, int x1, int y, boolean active, float flow) {
        fill(g, x0, y - 2, x1, y + 2, STEEL_DARK);
        fill(g, x0, y - 2, x1, y - 1, STEEL_LIGHT);
        fill(g, x0, y - 1, x1, y + 1, active ? WATER : STEEL);
        if (active && flow >= 0) for (int d = x0 + Math.round(flow * 8); d < x1; d += 8) fill(g, d, y - 1, Math.min(x1, d + 2), y + 1, WATER_LIGHT);
    }

    /** A vertical pipe from y0 to y1 round {@code x}, as {@link #pipeH}; the dashes run downwards. */
    public static void pipeV(GuiGraphics g, int x, int y0, int y1, boolean active, float flow) {
        fill(g, x - 2, y0, x + 2, y1, STEEL_DARK);
        fill(g, x - 2, y0, x - 1, y1, STEEL_LIGHT);
        fill(g, x - 1, y0, x + 1, y1, active ? WATER : STEEL);
        if (active && flow >= 0) for (int d = y0 + Math.round(flow * 8); d < y1; d += 8) fill(g, x - 1, d, x + 1, Math.min(y1, d + 2), WATER_LIGHT);
    }

    // ---- 0.1.11.181: the generators' drawings (MimicScreen and the generator screens, 176 wide) ------------------------------

    /** The fluids and media of the generators' drawings. */
    public static final int EU = 0xFFF2C14E, LCD_TEXT = 0xFFE6E9EC, KU = 0xFF2A5D8A, HEAT = 0xFFC0392B, STEAM = 0xFFB9C3C8,
        HOT_STEAM = 0xFFD9792B, DISTILLED = 0xFF7FB3D6, LAVA = 0xFFD9531E, LAVA_LIGHT = 0xFFF0913A, HOT_WATER = 0xFFD9792B,
        LEAD = 0xFF4E5060, LEAD_LIGHT = 0xFF6E7088, LEAD_DARK = 0xFF2E3040, SCALE = 0xFFC9B48A;

    /** The animation phase (0..1) of the dashes in a pipe at {@code speed} (0 stands still: -1, no dashes). */
    public static float phase(double time, double speed) {
        if (speed <= 0) return -1;
        return (float) ((time * Math.min(1.5, 0.2 + speed) / 8) % 1);
    }

    /** A pipe from x0 to x1 round {@code y} carrying {@code colour} (0: empty, steel inside), light dashes running while {@code flow} >= 0. */
    public static void pipe(GuiGraphics g, int x0, int x1, int y, int colour, float flow) {
        fill(g, x0, y - 2, x1, y + 2, STEEL_DARK);
        fill(g, x0, y - 2, x1, y - 1, STEEL_LIGHT);
        fill(g, x0, y - 1, x1, y + 1, colour != 0 ? colour : STEEL);
        if (colour != 0 && flow >= 0) for (int d = x0 + Math.round(flow * 8); d < x1; d += 8) fill(g, d, y - 1, Math.min(x1, d + 2), y + 1, 0xC0FFFFFF);
    }

    /** A pipe down from y0 to y1 round {@code x}, as {@link #pipe}. */
    public static void pipeDown(GuiGraphics g, int x, int y0, int y1, int colour, float flow) {
        fill(g, x - 2, y0, x + 2, y1, STEEL_DARK);
        fill(g, x - 2, y0, x - 1, y1, STEEL_LIGHT);
        fill(g, x - 1, y0, x + 1, y1, colour != 0 ? colour : STEEL);
        if (colour != 0 && flow >= 0) for (int d = y0 + Math.round(flow * 8); d < y1; d += 8) fill(g, x - 1, d, x + 1, Math.min(y1, d + 2), 0xC0FFFFFF);
    }

    /** A gauge column of a fluid as {@link #gauge}, with the colour of a pipe's medium. */
    public static void level(GuiGraphics g, int x, int y, int w, int h, double share, int colour) {
        gauge(g, x, y, w, h, (float) Math.max(0, Math.min(1, share)), colour);
    }

    /** A shaft from x0 to x1 round {@code y}, three pixels thick. */
    public static void shaft(GuiGraphics g, int x0, int x1, int y) {
        fill(g, x0, y - 1, x1, y + 2, STEEL);
        fill(g, x0, y - 1, x1, y, STEEL_LIGHT);
        fill(g, x0, y + 1, x1, y + 2, STEEL_DARK);
    }

    /** A flywheel at (cx, cy), radius {@code r}, turned to {@code angle}: an iron rim, four spokes and the hub. */
    public static void flywheel(GuiGraphics g, float cx, float cy, float r, double angle) {
        FcsDraw.disc(g, cx, cy, r, IRON_DARK);
        FcsDraw.disc(g, cx, cy, r - 1, IRON);
        FcsDraw.circle(g, cx, cy, r - 2.5F, 1F, IRON_LIGHT);
        for (int k = 0; k < 4; k++) {
            double a = angle + k * Math.PI / 2;
            FcsDraw.line(g, cx, cy, cx + (float) Math.cos(a) * (r - 2), cy + (float) Math.sin(a) * (r - 2), 1.2F, IRON_DARK);
        }
        FcsDraw.disc(g, cx, cy, 2F, STEEL_LIGHT);
    }

    /** A generator: an iron housing with cooling fins at the top and bottom and its copper winding behind a window. */
    public static void generator(GuiGraphics g, int x, int y, int w, int h) {
        iron(g, x, y, w, h);
        for (int fx = x + 3; fx < x + w - 3; fx += 3) {
            fill(g, fx, y + 2, fx + 1, y + 5, IRON_DARK);
            fill(g, fx, y + h - 5, fx + 1, y + h - 2, IRON_DARK);
        }
        sunk(g, x + 3, y + 6, w - 6, h - 12, WINDOW);
        for (int k = x + 4; k < x + w - 4; k += 2) fill(g, k, y + 7, k + 1, y + h - 7, COPPER);
    }

    /** A kinetic coupling (the KU output): a steel flange with the blue drive face. */
    public static void coupling(GuiGraphics g, int x, int y, int w, int h) {
        steel(g, x, y, w, h);
        fill(g, x + 2, y + h / 2 - 3, x + w - 2, y + h / 2 + 3, STEEL_DARK);
        fill(g, x + 3, y + h / 2 - 2, x + w - 3, y + h / 2 + 2, KU);
    }

    /** The output cable along the points, as {@link #cable}, with yellow packets running along it while {@code on}. */
    public static void euCable(GuiGraphics g, boolean on, double time, int... xy) {
        cable(g, xy);
        if (!on) return;
        int total = 0;
        for (int i = 0; i + 3 < xy.length; i += 2) total += Math.abs(xy[i + 2] - xy[i]) + Math.abs(xy[i + 3] - xy[i + 1]);
        if (total <= 0) return;
        for (int k = 0; k < total; k += 6) {
            int d = (int) Math.floorMod((long) Math.floor(k + time * 0.75), total);
            for (int i = 0; i + 3 < xy.length; i += 2) {
                int len = Math.abs(xy[i + 2] - xy[i]) + Math.abs(xy[i + 3] - xy[i + 1]);
                if (d <= len) {
                    int px = xy[i] + Integer.signum(xy[i + 2] - xy[i]) * Math.min(d, Math.abs(xy[i + 2] - xy[i]));
                    int py = xy[i + 1] + Integer.signum(xy[i + 3] - xy[i + 1]) * Math.min(d, Math.abs(xy[i + 3] - xy[i + 1]));
                    fill(g, px, py, px + 1, py + 1, EU);
                    break;
                }
                d -= len;
            }
        }
    }

    /**
     * A turbine casing (x, y, w, h): narrow at the inlet on the left, wide at the exhaust on the right, the blade rows in its
     * dark inside shimmering while it turns ({@code spin} the time, negative standing).
     */
    public static void turbine(GuiGraphics g, int x, int y, int w, int h, double spin) {
        float mid = y + h / 2F;
        for (int i = 0; i < w; i++) {
            float half = h * (0.22F + 0.28F * i / Math.max(1, w - 1));
            int top = Math.round(mid - half), bottom = Math.round(mid + half);
            fill(g, x + i, top, x + i + 1, bottom, STEEL);
            fill(g, x + i, top, x + i + 1, top + 1, STEEL_LIGHT);
            fill(g, x + i, bottom - 1, x + i + 1, bottom, STEEL_DARK);
            if (i >= 2 && i < w - 2) fill(g, x + i, top + 2, x + i + 1, bottom - 2, WINDOW);
        }
        for (int k = 1; k <= 4; k++) {
            int bx = x + Math.round(w * k / 5F);
            float half = h * (0.22F + 0.28F * k / 5F) - 3;
            int c = spin >= 0 && ((int) Math.floor(spin / 2 + k)) % 2 == 0 ? STEEL_LIGHT : STEEL;
            fill(g, bx, Math.round(mid - half), bx + 1, Math.round(mid + half), c);
        }
    }

    /** A thermometer column, four pixels wide: the temperature's colour up to {@code celsius} of {@code max}. */
    public static void thermometer(GuiGraphics g, int x, int y, int h, double celsius, double max) {
        gauge(g, x, y, 4, h, (float) Math.max(0, Math.min(1, celsius / max)), temperature(celsius));
    }

    /** A mark across a thermometer (or gauge) at {@code value} of {@code max}, three pixels out on each side. */
    public static void mark(GuiGraphics g, int x, int y, int w, int h, double value, double max, int colour) {
        int my = y + 1 + (int) Math.round((h - 2) * (1 - Math.max(0, Math.min(1, value / max))));
        fill(g, x - 3, my, x + w + 3, my + 1, colour);
    }

    /** The colour of a temperature: blue cold, through grey, to orange-red hot (as the pipes of the mimic diagrams). */
    public static int temperature(double celsius) {
        double f = Math.max(0, Math.min(1, (celsius - 20) / 460));
        int r = (int) (70 + 175 * f), gr = (int) (120 - 40 * f), b = (int) (180 - 150 * f);
        return 0xFF000000 | r << 16 | gr << 8 | b;
    }

    /** {@code n} flames standing on {@code base} between x0 and x1, flickering, at most {@code high} tall. */
    public static void flames(GuiGraphics g, int x0, int x1, int base, double time, int n, int high) {
        for (int k = 0; k < n; k++) {
            float fx = x0 + (x1 - x0) * (k + 0.5F) / n;
            float fh = 2 + high * (float) (0.5 + 0.5 * Math.sin(time / 3 + k * 1.3));
            FcsDraw.quad(g, fx - 3, base, fx, base - fh, fx + 3, base, fx, base, GLOW);
            FcsDraw.quad(g, fx - 1.5F, base, fx, base - fh * 0.55F, fx + 1.5F, base, fx, base, AMBER);
        }
    }

    /** The filler head between a container's input slot and its output slot: a steel bar {@code w} wide, six high. */
    public static void filler(GuiGraphics g, int x, int y, int w) {
        steel(g, x, y, w, 6);
        fill(g, x + w / 2 - 2, y + 2, x + w / 2 + 2, y + 4, STEEL_DARK);
    }
}
