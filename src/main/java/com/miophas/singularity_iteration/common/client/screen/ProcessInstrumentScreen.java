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
    public static final int W = 248, H = 240, SPLIT = 116;
    /** The figure's centre and the readout column. */
    public static final int FIG_X = 162, FIG_Y = 56, READ_X = 12, READ_W = 226, PROGRESS_Y = 112, ENERGY_Y = 112;
    /**
     * 0.1.11.118: the vanilla layout. The figure window and the readout window (outer edges), the furnace block on the left:
     * the energy bolt between the input and the battery, the progress arrow from the inputs to the outputs.
     */
    public static final int FIG_WIN_X = 82, FIG_WIN_Y = 17, FIG_WIN_W = 160, FIG_WIN_H = 78,
        READ_WIN_X = 8, READ_WIN_Y = 97, READ_WIN_W = 234, READ_WIN_H = 56, BOLT_X = 13, BOLT_Y = 39, ARROW_X = 32, ARROW_Y = 37;   // 0.1.11.141: the arrow centred between input (ends 27) and output (60), the bolt's centre of mass on the slot column
    /**
     * 0.1.11.120: the readouts in the vanilla font (one font pixel a GUI pixel): the headline row (headline, its bar, the
     * status on the right), then up to four lines; the buttons stack in a column on the right of the lines.
     */
    public static final int HEAD_Y = 101, LINE_Y = 112, LINE_H = 10, BUTTON_W = 76, BUTTON_H = 12, TEXT = 0x404040;
    protected static final String K = "gui.mio_icif.process.";
    /**
     * 0.1.11.141 (calm layout, docs/gui_processing_machines.md): the flat figure window, the muted third level of text, the
     * rows of the headline (twice the vanilla font), its right-hand text and the readout lines.
     */
    public static final int MUTED = 0x80868A, CALM_HEAD_Y = 100, CALM_SIDE_Y = 104, CALM_LINE_Y = 122,
        CALM_LINE_H = 12, CALM_LINES = 3, ICON = 11;

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
    protected float figureScale() { return 0.85F; }

    /** 0.1.11.120: how many buttons the screen stacks on the right of the readout lines (they get the column). */
    protected int buttonCount() { return 0; }

    /** 0.1.11.118: whether the furnace block (energy bolt, progress arrow) is drawn on the left. */
    protected boolean furnaceBlock() { return true; }

    /**
     * 0.1.11.141: the calm layout (the premium-feel review): a flat figure window and flat figure parts, no readout window,
     * the headline twice the font size with the progress or the stop reason beside it, at most three lines under it (the
     * first in the body colour, the rest muted), small icon buttons in the figure window, the title on the left and no lamp
     * or upgrade hint. Machines move to it one by one.
     */
    protected boolean calm() { return false; }

    /** 0.1.11.142: whether a machine slot is drawn (a slot the current mode does not use can hide, frame and all). */
    protected boolean slotShown(Slot slot) { return true; }

    /**
     * 0.1.11.142: a ghost of {@code stack} in an empty slot, as the vanilla armour and smithing slots show what goes there:
     * the item under a wash of the slot's grey. (sx, sy) is the slot's item position (GUI-relative).
     */
    protected void ghost(GuiGraphics g, net.minecraft.world.item.ItemStack stack, int sx, int sy) {
        if (stack.isEmpty()) return;
        int gx = leftPos + sx, gy = topPos + sy;
        g.renderFakeItem(stack, gx, gy);
        g.pose().pushPose();
        g.pose().translate(0, 0, 200);   // above the item (near z 150)
        g.fill(gx, gy, gx + 16, gy + 16, 0xB08B8B8B);
        g.pose().popPose();
    }

    /** 0.1.11.141: drawn after the pixel canvas (textures such as fluids, which the canvas would cover); (cx, cy) as in drawFigure. */
    protected void drawOverFigure(GuiGraphics g, float cx, float cy) { }

    /** 0.1.11.141: a figure coordinate {@code c + d} on the screen (the canvas scales the figure about its centre). */
    protected final int figPx(float c, float d) { return Math.round(c + d * figureScale()); }

    /** 0.1.11.141: the text on the right of a calm headline: the progress while running, else the status. */
    protected Component calmSide() {
        int code = status();
        if (code == ProcessStatus.RUNNING) {
            int max = menu.getMaxProgress();
            return Component.literal((max > 0 ? Math.round(100F * menu.getProgress() / max) : 0) + "%");
        }
        return Component.translatable(K + "status." + ProcessStatus.KEYS[Math.max(0, Math.min(ProcessStatus.KEYS.length - 1, code))]);
    }

    @Override
    protected boolean showUpgradeHint() { return !calm() && super.showUpgradeHint(); }   // the empty upgrade slots say the same

    @Override
    protected int statusLampColor() { return calm() ? 0 : super.statusLampColor(); }

    @Override
    protected void drawTitle(GuiGraphics g) {
        if (!calm()) { super.drawTitle(g); return; }
        g.drawString(font, fit(title, W - 16), 8, titleLabelY, VanillaFrame.TITLE, false);   // left, as in the vanilla containers
    }

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
        if (!calm()) {
            VanillaFrame.inset(g, x + FIG_WIN_X, y + FIG_WIN_Y, FIG_WIN_W, FIG_WIN_H, VanillaFrame.SLOT);
            VanillaFrame.inset(g, x + READ_WIN_X, y + READ_WIN_Y, READ_WIN_W, READ_WIN_H, PAPER);
        }
        for (Slot s : menu.slots) {
            if (s.x < 0 || s.x > W) continue;   // docked upgrade slots are drawn by the dock
            if (!slotShown(s)) continue;
            VanillaFrame.slot(g, x + s.x - 1, y + s.y - 1);
        }
        if (furnaceBlock()) {
            VanillaFrame.bolt(g, x + BOLT_X, y + BOLT_Y, energyFraction());
            int max = menu.getMaxProgress();
            VanillaFrame.arrow(g, x + ARROW_X, y + ARROW_Y, max > 0 ? (double) menu.getProgress() / max : 0);
        }
        panelButtons.clear();
        float dt = frameSeconds();
        if (calm()) { renderCalm(g, x, y, dt, mouseX, mouseY); return; }
        // 0.1.11.118: the figure as pixel art, one cell a GUI pixel, inside its window
        FcsDraw.beginPixels(new com.miophas.singularity_iteration.common.client.suit.PixelCanvas(x + FIG_WIN_X + 1, y + FIG_WIN_Y + 1,
            FIG_WIN_W - 2, FIG_WIN_H - 2, x + FIG_X, y + FIG_Y, figureScale()));
        try {
            drawFigure(g, x + FIG_X, y + FIG_Y, dt);
        } finally {
            FcsDraw.endPixels(g);
        }

        // 0.1.11.120: the readout window in the vanilla look and font
        VanillaFrame.inset(g, x + READ_WIN_X, y + READ_WIN_Y, READ_WIN_W, READ_WIN_H, VanillaFrame.PANEL);
        int left = x + READ_X, right = x + READ_WIN_X + READ_WIN_W - 4;
        int code = status();
        int colour = code == ProcessStatus.RUNNING ? GREEN : ProcessStatus.isFault(code) ? RED : TEXT;
        Component st = Component.translatable(K + "status." + ProcessStatus.KEYS[Math.max(0, Math.min(ProcessStatus.KEYS.length - 1, code))]);
        Component head = fit(headline(), READ_W / 2);
        int headEnd = left + font.width(head);
        st = fit(st, right - headEnd - 8);
        int stX = right - font.width(st);
        g.drawString(font, head, left, y + HEAD_Y, headlineColour(), false);
        g.drawString(font, st, stX, y + HEAD_Y, colour, false);
        float hb = headlineBar();
        if (hb >= 0 && stX - headEnd > 24) {
            int bx = headEnd + 6, bw = stX - 6 - bx;
            furnaceBar(g, bx, y + HEAD_Y, bw, 7, hb);   // 0.1.11.141: on the capitals' rows (was a row low)
            float mark = headlineBarMarker();
            if (mark >= 0) {
                int mx = bx + 1 + Math.round((bw - 2) * Math.min(1, mark));
                g.fill(mx, y + HEAD_Y, mx + 1, y + HEAD_Y + 9, 0xFF000000 | RED);
            }
        }
        int lineW = READ_W - (buttonCount() > 0 ? BUTTON_W + 4 : 0);
        int ty = y + LINE_Y;
        for (Component c : readouts()) {
            if (ty > y + LINE_Y + 3 * LINE_H) break;
            g.drawString(font, fit(c, lineW), left, ty, TEXT, false);
            ty += LINE_H;
        }
        drawExtras(g, right - BUTTON_W, y + LINE_Y - 1, mouseX, mouseY);
    }

    /** 0.1.11.141: the calm layout (see {@link #calm}). */
    private void renderCalm(GuiGraphics g, int x, int y, float dt, int mouseX, int mouseY) {
        // 0.1.11.142: the figure window sunk into the panel like the slots (the vanilla look; the flat window read as a hole)
        VanillaFrame.inset(g, x + FIG_WIN_X, y + FIG_WIN_Y, FIG_WIN_W, FIG_WIN_H, VanillaFrame.SLOT);
        FcsDraw.beginPixels(new com.miophas.singularity_iteration.common.client.suit.PixelCanvas(x + FIG_WIN_X + 1, y + FIG_WIN_Y + 1,
            FIG_WIN_W - 2, FIG_WIN_H - 2, x + FIG_X, y + FIG_Y, figureScale(), VanillaFrame.SLOT).flat());
        try {
            drawFigure(g, x + FIG_X, y + FIG_Y, dt);
        } finally {
            FcsDraw.endPixels(g);
        }
        drawOverFigure(g, x + FIG_X, y + FIG_Y);

        int left = x + 8, right = x + W - 8;
        int code = status();
        Component side = calmSide();
        int sideColour = code == ProcessStatus.RUNNING ? INK : ProcessStatus.isFault(code) ? RED : DIM;
        side = fit(side, W / 2);
        int sideX = right - font.width(side);
        Component head = fit(headline(), (sideX - left - 8) / 2);
        g.pose().pushPose();
        g.pose().translate(left, y + CALM_HEAD_Y, 0);
        g.pose().scale(2F, 2F, 1F);
        g.drawString(font, head, 0, 0, headlineColour(), false);
        g.pose().popPose();
        g.drawString(font, side, sideX, y + CALM_SIDE_Y, sideColour, false);
        float hb = headlineBar();
        int headEnd = left + 2 * font.width(head);
        if (hb >= 0 && sideX - headEnd > 24) {
            int bx = headEnd + 6, bw = sideX - 6 - bx;
            VanillaFrame.flatBar(g, bx, y + CALM_SIDE_Y, bw, hb);
            float mark = headlineBarMarker();
            if (mark >= 0) {
                int mx = bx + Math.round((bw - 1) * Math.min(1, mark));
                g.fill(mx, y + CALM_SIDE_Y - 1, mx + 1, y + CALM_SIDE_Y + 8, 0xFF000000 | RED);
            }
        }
        int ty = y + CALM_LINE_Y, n = 0;
        for (Component c : readouts()) {
            if (n == CALM_LINES) break;
            g.drawString(font, fit(c, W - 16), left, ty, n == 0 ? TEXT : MUTED, false);
            ty += CALM_LINE_H;
            n++;
        }
        drawExtras(g, right, y + CALM_LINE_Y, mouseX, mouseY);
    }

    /**
     * 0.1.11.141: a small flat icon button in the top right corner of the figure window (index 0 rightmost) that sends menu
     * button {@code id}; lighter under the mouse, dark when on. {@code icon}: 7 rows of 7 ('#' drawn), so every icon has
     * the same size (font glyphs such as the arrows come out in different sizes). Call it from {@link #drawExtras} every frame.
     */
    public static final String[] ICON_CYCLE = {"..###.#", ".#...##", "#...###", "#......", "#.....#", ".#...#.", "..###.."},
        ICON_SWAP = {".....#.", "#######", ".....#.", ".......", ".#.....", "#######", ".#....."};

    protected void iconButton(GuiGraphics g, int index, String[] icon, boolean on, int id, int mouseX, int mouseY, List<Component> tooltip) {
        int bx = FIG_WIN_X + FIG_WIN_W - 2 - (index + 1) * (ICON + 1) + 1, by = FIG_WIN_Y + 2;
        boolean hover = inside(mouseX - leftPos, mouseY - topPos, bx, by, ICON, ICON);
        if (hover || on) g.fill(leftPos + bx, topPos + by, leftPos + bx + ICON, topPos + by + ICON, on ? 0xFF6B7176 : 0x30FFFFFF);
        int ix = leftPos + bx + 2, iy = topPos + by + 2, ink = on ? 0xFFFFFFFF : 0xFF3C4245;
        for (int j = 0; j < icon.length; j++)
            for (int i = 0; i < icon[j].length(); i++) if (icon[j].charAt(i) == '#') g.fill(ix + i, iy + j, ix + i + 1, iy + j + 1, ink);
        panelButtons.add(new PanelButton(bx, by, ICON, ICON, id, tooltip));
    }

    /** {@code c} cut to {@code maxW} pixels with an ellipsis. */
    protected Component fit(Component c, int maxW) {
        if (font.width(c) <= maxW) return c;
        String s = c.getString();
        String cut = font.plainSubstrByWidth(s, Math.max(0, maxW - font.width("…")));
        return Component.literal(cut + "…").withStyle(c.getStyle());
    }

    /** 0.1.11.120: a bar like the furnace's: sunk grey track, filled white from the left. */
    protected static void furnaceBar(GuiGraphics g, int x, int y, int w, int h, double fraction) {
        VanillaFrame.inset(g, x, y, w, h, VanillaFrame.SLOT);
        int fill = (int) Math.round((w - 2) * Math.max(0, Math.min(1, fraction)));
        if (fill > 0) g.fill(x + 1, y + 1, x + 1 + fill, y + h - 1, VanillaFrame.LIGHT);
    }

    protected static void bar(GuiGraphics g, int x, int y, int w, int h, double fraction, int argb) {
        g.fill(x, y, x + w, y + h, TRACK);
        int fill = (int) Math.round(w * Math.max(0, Math.min(1, fraction)));
        if (fill > 0) g.fill(x, y, x + fill, y + h, argb);
    }

    /**
     * 0.1.11.120: a vanilla-like button: raised (white upper left, dark lower right) with dark text, lighter on hover;
     * pressed in (sunk, darker, white text) when on.
     */
    protected void button(GuiGraphics g, int x, int y, int w, int h, Component label, boolean on, boolean hover) {
        if (on) {
            VanillaFrame.inset(g, x, y, w, h, hover ? 0xFF7A7A7A : 0xFF6B6B6B);
        } else {
            g.fill(x, y, x + w, y + h, VanillaFrame.SHADOW);
            g.fill(x, y, x + w - 1, y + h - 1, VanillaFrame.LIGHT);
            g.fill(x + 1, y + 1, x + w - 1, y + h - 1, hover ? 0xFFD8D8E8 : 0xFFB8B8B8);
        }
        Component c = fit(label, w - 4);
        g.drawString(font, c, x + (w - font.width(c)) / 2, y + (h - 8) / 2 + 1, on ? 0xFFFFFF : TEXT, false);
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
        } else if (inside(rx, ry, ARROW_X, ARROW_Y, 22, 15)) {   // 0.1.11.120: the arrow is the progress
            g.renderComponentTooltip(this.font, List.of(Component.translatable(K + "tip.progress", menu.getProgress(), menu.getMaxProgress())), mouseX, mouseY);
        } else if (inside(rx, ry, BOLT_X - 2, BOLT_Y - 1, 13, 15)) {   // the bolt is the stored energy
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
