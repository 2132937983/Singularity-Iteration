package com.miophas.singularity_iteration.common.client.screen;

import com.miophas.singularity_iteration.common.client.suit.AsciiHud;
import com.miophas.singularity_iteration.common.client.suit.FcsDraw;
import com.miophas.singularity_iteration.common.menu.base.mio_icif_machine_menu;
import com.miophas.singularity_iteration.common.processing.ProcessStatus;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/**
 * 0.1.11.110 (processing overhaul M3): the instrument screen of a processing machine, as set out in
 * {@code docs/gui_processing_machines.md}. 248 x 204: a paper-white instrument area on top (machine slots on the left, the
 * machine's own moving figure in the middle, a column of readouts on the right, progress and stored energy under it) and a
 * gray inventory area below. No outlines: the slots and bars are flat fills one shade darker than their ground. The colours
 * are the solar overhaul's (the pyranometer screen). A machine draws its figure and gives its readout lines.
 */
@OnlyIn(Dist.CLIENT)
@SuppressWarnings("null")
public abstract class ProcessInstrumentScreen<T extends mio_icif_machine_menu> extends mio_icif_screen<T> {

    // ---- the palette (see the spec) ----
    public static final int PAPER = 0xFFF4F6F4, GROUND = 0xFFC9CDCF, SLOT_PAPER = 0xFFD5DAD8, SLOT_GROUND = 0xFFA8ACB1,
        SHELL = 0xFFD5DAD8, CHAMBER = 0xFFE6E9E7, STEEL = 0xFF5B6867, STEEL_LIGHT = 0xFFB9C1BF, TRACK = 0xFFE1E5E3;
    /** RGB (no alpha) for text. */
    public static final int INK = 0x1C2524, DIM = 0x5B6867, BLUE = 0x2A5D8A, AMBER = 0xD49A12, RED = 0xC0392B, GREEN = 0x2F6E4F;
    public static final int W = 248, H = 204, SPLIT = 116;
    /** The figure's centre and the readout column. */
    public static final int FIG_X = 98, FIG_Y = 58, READ_X = 150, READ_W = 90, PROGRESS_Y = 100, ENERGY_Y = 108;
    protected static final String K = "gui.mio_icif.process.";

    private long lastNanos;

    protected ProcessInstrumentScreen(T menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = W;
        this.imageHeight = H;
    }

    /** Seconds since the last frame (for the moving parts), at most a tenth of a second. */
    protected final float frameSeconds() {
        long now = System.nanoTime();
        float dt = lastNanos == 0 ? 0 : Math.min(0.1F, (now - lastNanos) / 1e9F);
        lastNanos = now;
        return dt;
    }

    /** Draws the machine's figure around (cx, cy) in GUI coordinates. */
    protected abstract void drawFigure(GuiGraphics g, float cx, float cy, float dt);

    /** The large first readout (state, speed or temperature) and its colour (RGB). */
    protected abstract Component headline();
    protected abstract int headlineColour();

    /** A bar under the headline (0..1, e.g. a rotor speed), or negative for none. */
    protected float headlineBar() { return -1; }

    /** The readout lines under the headline (material, energy, the machine's own figures). */
    protected abstract List<Component> readouts();

    /** The status code ({@link ProcessStatus}). */
    protected abstract int status();

    /** Tooltip lines when the mouse is over the figure. */
    protected abstract List<Component> figureTooltip();

    /** Optional extra drawing in the readout column (bars, buttons), below the readouts at {@code y}. */
    protected void drawExtras(GuiGraphics g, int x, int y, int mouseX, int mouseY) { }

    protected float u() { return WindRoseView.u(); }

    protected void text(GuiGraphics g, Component c, float x, float y, int rgb) {
        PumpCrossSection.text(g, c, x, y, rgb, u());
    }

    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        int x = leftPos, y = topPos;
        g.fill(x, y, x + W, y + SPLIT, PAPER);
        g.fill(x, y + SPLIT, x + W, y + H, GROUND);
        for (Slot s : menu.slots) {
            if (s.x < 0 || s.x > W) continue;   // docked upgrade slots are drawn by the dock
            g.fill(x + s.x - 1, y + s.y - 1, x + s.x + 16, y + s.y + 16, s.y < SPLIT ? SLOT_PAPER : SLOT_GROUND);   // 17 px: a 1 px gap between neighbours
        }
        float dt = frameSeconds();
        drawFigure(g, x + FIG_X, y + FIG_Y, dt);

        float u = u(), lh = 9 * u + 1;
        int rx = x + READ_X;
        float ty = y + 22;
        PumpCrossSection.text(g, headline(), rx, ty, headlineColour(), u * 1.25F);
        ty += 12 * u + 2;
        float hb = headlineBar();
        if (hb >= 0) {
            bar(g, rx, (int) ty - 1, READ_W, 3, hb, 0xFF000000 | AMBER);
            ty += 4;
        }
        for (Component c : readouts()) {
            WindRoseView.line(g, c, rx, ty, DIM, READ_W);
            ty += lh;
        }
        int code = status();
        int colour = code == ProcessStatus.RUNNING ? GREEN : ProcessStatus.isFault(code) ? RED : DIM;
        WindRoseView.line(g, Component.translatable(K + "status." + ProcessStatus.KEYS[Math.max(0, Math.min(ProcessStatus.KEYS.length - 1, code))]),
            rx, ty, colour, READ_W);
        drawExtras(g, rx, (int) (ty + lh + 1), mouseX, mouseY);

        // progress and stored energy
        int max = menu.getMaxProgress(), p = menu.getProgress();
        bar(g, rx, y + PROGRESS_Y, READ_W, 5, max > 0 ? (double) p / max : 0, 0xFF000000 | BLUE);
        long e = Integer.toUnsignedLong(menu.getEnergy()), cap = Math.max(1, Integer.toUnsignedLong(menu.getMaxEnergy()));
        bar(g, rx, y + ENERGY_Y, READ_W, 3, (double) e / cap, 0xFF000000 | AMBER);
    }

    protected static void bar(GuiGraphics g, int x, int y, int w, int h, double fraction, int argb) {
        g.fill(x, y, x + w, y + h, TRACK);
        int fill = (int) Math.round(w * Math.max(0, Math.min(1, fraction)));
        if (fill > 0) g.fill(x, y, x + fill, y + h, argb);
    }

    /** A flat button (no outline): blue with white text when on, a light fill otherwise, a shade darker on hover. */
    protected void button(GuiGraphics g, int x, int y, int w, int h, Component label, boolean on, boolean hover) {
        int fill = on ? (hover ? 0xFF234E74 : 0xFF000000 | BLUE) : (hover ? 0xFFD3D8D6 : TRACK);
        g.fill(x, y, x + w, y + h, fill);
        float u = u();
        PumpCrossSection.text(g, label, x + (w - AsciiHud.width(label, u)) / 2F, y + (h - 8 * u) / 2F, on ? 0xFFFFFF : DIM, u);
    }

    protected static boolean inside(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        int rx = mouseX - leftPos, ry = mouseY - topPos;
        if (inside(rx, ry, FIG_X - 40, FIG_Y - 40, 80, 80)) {
            g.renderComponentTooltip(this.font, figureTooltip(), mouseX, mouseY);
        } else if (inside(rx, ry, READ_X, PROGRESS_Y - 1, READ_W, 7)) {
            g.renderComponentTooltip(this.font, List.of(Component.translatable(K + "tip.progress", menu.getProgress(), menu.getMaxProgress())), mouseX, mouseY);
        } else if (inside(rx, ry, READ_X, ENERGY_Y - 1, READ_W, 5)) {
            g.renderComponentTooltip(this.font, List.of(Component.translatable(K + "tip.energy",
                String.format(Locale.ROOT, "%,d", Integer.toUnsignedLong(menu.getEnergy())),
                String.format(Locale.ROOT, "%,d", Integer.toUnsignedLong(menu.getMaxEnergy())))), mouseX, mouseY);
        }
    }

    // ---- shared figure parts ----

    /** A blade or hammer: a quad from radius r0 to r1, half-widths w0 and w1, at angle a (radians, 0 = right). */
    protected static void blade(GuiGraphics g, float cx, float cy, float a, float r0, float r1, float w0, float w1, int argb) {
        float c = (float) Math.cos(a), s = (float) Math.sin(a);
        FcsDraw.quad(g, cx + c * r0 - s * w0, cy + s * r0 + c * w0, cx + c * r1 - s * w1, cy + s * r1 + c * w1,
            cx + c * r1 + s * w1, cy + s * r1 - c * w1, cx + c * r0 + s * w0, cy + s * r0 - c * w0, argb);
    }

    protected static String f1(double v) { return String.format(Locale.ROOT, "%.1f", v); }
}
