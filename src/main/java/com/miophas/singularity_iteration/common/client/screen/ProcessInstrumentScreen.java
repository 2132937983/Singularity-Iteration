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
    public static final int FIG_X = 113, FIG_Y = 64, READ_X = 150, READ_W = 90, PROGRESS_Y = 100, ENERGY_Y = 108;
    /**
     * 0.1.11.118: the vanilla layout. The figure window and the readout window (outer edges), the furnace block on the left:
     * the energy bolt between the input and the battery, the progress arrow from the inputs to the outputs.
     */
    public static final int FIG_WIN_X = 82, FIG_WIN_Y = 17, FIG_WIN_W = 62, FIG_WIN_H = 95,
        READ_WIN_X = 146, READ_WIN_Y = 17, READ_WIN_W = 96, READ_WIN_H = 95, BOLT_X = 14, BOLT_Y = 38, ARROW_X = 33, ARROW_Y = 37;
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

    /** 0.1.11.112: a red mark on the headline bar (0..1, e.g. the pressure the next stroke needs), or negative for none. */
    protected float headlineBarMarker() { return -1; }

    /** 0.1.11.112: the stored share of the buffer (0..1) for the energy bar; by default from the synced energy. */
    protected double energyFraction() {
        long e = Integer.toUnsignedLong(menu.getEnergy()), cap = Math.max(1, Integer.toUnsignedLong(menu.getMaxEnergy()));
        return (double) e / cap;
    }

    /** The readout lines under the headline (material, energy, the machine's own figures). */
    protected abstract List<Component> readouts();

    /** The status code ({@link ProcessStatus}). */
    protected abstract int status();

    /** Tooltip lines when the mouse is over the figure. */
    protected abstract List<Component> figureTooltip();

    /** 0.1.11.118: the figure's scale in its window (the figures were drawn for a radius of about 40). */
    protected float figureScale() { return 0.75F; }

    /** 0.1.11.118: whether the furnace block (energy bolt, progress arrow) is drawn on the left. */
    protected boolean furnaceBlock() { return true; }

    /** Optional extra drawing in the readout column (bars, buttons), below the readouts at {@code y}. */
    protected void drawExtras(GuiGraphics g, int x, int y, int mouseX, int mouseY) { }

    protected float u() { return WindRoseView.u(); }

    protected void text(GuiGraphics g, Component c, float x, float y, int rgb) {
        PumpCrossSection.text(g, c, x, y, rgb, u());
    }

    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        int x = leftPos, y = topPos;
        // 0.1.11.118: the vanilla container look (docs/gui_processing_machines.md)
        VanillaFrame.panel(g, x, y, W, H);
        VanillaFrame.inset(g, x + FIG_WIN_X, y + FIG_WIN_Y, FIG_WIN_W, FIG_WIN_H, VanillaFrame.SLOT);
        VanillaFrame.inset(g, x + READ_WIN_X, y + READ_WIN_Y, READ_WIN_W, READ_WIN_H, PAPER);
        for (Slot s : menu.slots) {
            if (s.x < 0 || s.x > W) continue;   // docked upgrade slots are drawn by the dock
            VanillaFrame.slot(g, x + s.x - 1, y + s.y - 1);
        }
        if (furnaceBlock()) {
            VanillaFrame.bolt(g, x + BOLT_X, y + BOLT_Y, energyFraction());
            int max = menu.getMaxProgress();
            VanillaFrame.arrow(g, x + ARROW_X, y + ARROW_Y, max > 0 ? (double) menu.getProgress() / max : 0);
        }
        panelButtons.clear();
        float dt = frameSeconds();
        // 0.1.11.118: the figure as pixel art, one cell a GUI pixel, inside its window
        FcsDraw.beginPixels(new com.miophas.singularity_iteration.common.client.suit.PixelCanvas(x + FIG_WIN_X + 1, y + FIG_WIN_Y + 1,
            FIG_WIN_W - 2, FIG_WIN_H - 2, x + FIG_X, y + FIG_Y, figureScale()));
        try {
            drawFigure(g, x + FIG_X, y + FIG_Y, dt);
        } finally {
            FcsDraw.endPixels(g);
        }

        float u = u(), lh = 9 * u + 1;
        int rx = x + READ_X;
        float ty = y + 22;
        PumpCrossSection.text(g, headline(), rx, ty, headlineColour(), u * 1.25F);
        ty += 12 * u + 2;
        float hb = headlineBar();
        if (hb >= 0) {
            bar(g, rx, (int) ty - 1, READ_W, 3, hb, 0xFF000000 | AMBER);
            float mark = headlineBarMarker();
            if (mark >= 0) {
                int mx = rx + Math.round(READ_W * Math.min(1, mark));
                g.fill(mx, (int) ty - 2, mx + 1, (int) ty + 3, 0xFF000000 | RED);
            }
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
        bar(g, rx, y + ENERGY_Y, READ_W, 3, energyFraction(), 0xFF000000 | AMBER);
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

    // ---- 0.1.11.113: menu buttons drawn in the panel (flat, no outline) ----

    private record PanelButton(int x, int y, int w, int h, int id, List<Component> tooltip) {}
    private final java.util.ArrayList<PanelButton> panelButtons = new java.util.ArrayList<>();

    /**
     * Draws a flat button at panel coordinates (x, y) that sends menu button {@code id} when clicked; call it from
     * {@link #drawExtras} every frame.
     */
    protected void menuButton(GuiGraphics g, int x, int y, int w, int h, Component label, boolean on, int id, int mouseX, int mouseY,
                              List<Component> tooltip) {
        boolean hover = inside(mouseX - leftPos, mouseY - topPos, x, y, w, h);
        button(g, leftPos + x, topPos + y, w, h, label, on, hover);
        panelButtons.add(new PanelButton(x, y, w, h, id, tooltip));
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && minecraft != null && minecraft.gameMode != null) {
            for (PanelButton b : panelButtons) {
                if (inside(mouseX - leftPos, mouseY - topPos, b.x, b.y, b.w, b.h)) {
                    minecraft.gameMode.handleInventoryButtonClick(menu.containerId, b.id);
                    minecraft.getSoundManager().play(net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK, 1.0F));
                    return true;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    /** Colour of a body at {@code celsius} (RGB with alpha): dark below 500 °C, then red, orange, yellow, pale yellow-white. */
    protected static int glow(double celsius) {
        double[][] stops = {{500, 0x5B, 0x68, 0x67}, {650, 0x8B, 0x1A, 0x10}, {850, 0xC8, 0x3A, 0x10}, {1050, 0xF0, 0x7A, 0x1C},
            {1300, 0xF8, 0xC0, 0x40}, {1600, 0xFF, 0xF0, 0xB8}};
        if (celsius <= stops[0][0]) return 0xFF5B6867;
        for (int i = 1; i < stops.length; i++) {
            if (celsius <= stops[i][0]) {
                double t = (celsius - stops[i - 1][0]) / (stops[i][0] - stops[i - 1][0]);
                int r = (int) Math.round(stops[i - 1][1] + (stops[i][1] - stops[i - 1][1]) * t);
                int gr = (int) Math.round(stops[i - 1][2] + (stops[i][2] - stops[i - 1][2]) * t);
                int b = (int) Math.round(stops[i - 1][3] + (stops[i][3] - stops[i - 1][3]) * t);
                return 0xFF000000 | r << 16 | gr << 8 | b;
            }
        }
        return 0xFFFFF0B8;
    }

    protected static boolean inside(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        int rx = mouseX - leftPos, ry = mouseY - topPos;
        for (PanelButton b : panelButtons) {
            if (!b.tooltip.isEmpty() && inside(rx, ry, b.x, b.y, b.w, b.h)) {
                g.renderComponentTooltip(this.font, b.tooltip, mouseX, mouseY);
                return;
            }
        }
        if (inside(rx, ry, FIG_WIN_X, FIG_WIN_Y, FIG_WIN_W, FIG_WIN_H)) {
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
