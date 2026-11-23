// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.client.screen;

import com.miophas.singularity_iteration.common.client.suit.FcsDraw;
import com.miophas.singularity_iteration.common.client.suit.PixelCanvas;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/**
 * 0.1.11.16: base of the plant mimic screens of the steam chain, 248 x 204: the menu's slots on the left (frames drawn
 * where the menu put them), the mimic window, up to three reading lines under the diagram, a bar under the window, and
 * the player inventory.
 *
 * <p>0.1.11.181: the vanilla 176 wide, and merged as the processing machines (docs/gui_processing_machines.md, "合并布局"):
 * no window, the machine is drawn straight on the panel (x 8..168, y 15..77, panel coordinates) with its slots in it where
 * they belong (a bucket at the tank's filler, the pellets in their container, the battery at the end of the output cable).
 * Under it the readouts (their full text in a tooltip when a line did not fit), the bar and the player inventory at (8, 162).
 */
@OnlyIn(Dist.CLIENT)
@SuppressWarnings("null")
abstract class MimicScreen<T extends AbstractContainerMenu> extends mio_icif_screen<T> {

    /** 0.1.11.181: the drawing area (panel coordinates) and the screen. */
    protected static final int W = 176, ART_X = 8, ART_Y = 15, ART_W = 160, ART_H = 62, ART_RIGHT = ART_X + ART_W, ART_BOTTOM = ART_Y + ART_H;
    protected static final int STRIP_Y = 145, LINES_Y = 64, READ_Y = 80, READ_H = 62, H = 244;
    /** The machine's drawing for the tooltip ({@link #overMachine}); a screen narrows it to its machine. */
    protected int wx = ART_X, wy = ART_Y, ww = ART_W, wh = ART_H;
    private List<Component> cutLines = List.of();

    MimicScreen(T menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = W;
        this.imageHeight = H;   // 0.1.11.121: 204 before the readout window
    }

    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        // 0.1.11.24: one batch (each fill and text otherwise flushed on its own). Only fills, quads and text are drawn here.
        g.drawManaged(() -> {
            int x = leftPos, y = topPos;
            VanillaFrame.panel(g, x, y, imageWidth, imageHeight);
            // 0.1.11.181: the machine as flat pixel art on the panel (one cell a GUI pixel); the text over it afterwards
            if (pixelArt()) FcsDraw.beginPixels(new PixelCanvas(x + 4, y + ART_Y - 1, W - 8, ART_H + 2, x + 4, y + ART_Y - 1, 1F,
                VanillaFrame.PANEL).flat());
            ReadoutPanel.begin();
            try {
                drawMachine(g, x, y, PlantMimic.time(partialTick));
            } finally {
                FcsDraw.endPixels(g);
            }
            for (Slot slot : menu.slots) {   // the slots on the machine
                if (slot.x < 0 || slot.x > imageWidth) continue;   // docked upgrade slots draw their own frames
                VanillaFrame.slot(g, x + slot.x - 1, y + slot.y - 1);
            }
            cutLines = ReadoutPanel.end(g, x + ART_X - 1, y + READ_Y, ART_W + 2, READ_H);
            drawStrip(g, x, y + STRIP_Y);
        });
    }

    /** 0.1.11.119: whether the machine is drawn as pixel art; a screen whose drawing is mostly a chart keeps it smooth. */
    protected boolean pixelArt() { return true; }

    /**
     * 0.1.11.181: the machine and its readouts. {@code x}, {@code y} are the panel's corner on the screen: the drawing uses
     * panel coordinates (x 8..168, y 15..77) and the menu's slot positions; the reading lines go to {@link #line}.
     */
    protected abstract void drawMachine(GuiGraphics g, int x, int y, double time);

    protected abstract void drawStrip(GuiGraphics g, int x, int y);

    /** Reading line {@code n} (0..3) under the machine. */
    protected void line(GuiGraphics g, int x, int y, int n, Component c, int rgb) {
        ReadoutPanel.add(n, c, rgb);   // 0.1.11.121: written in the readout area, in the vanilla font
    }

    /** A labelled bar under the readouts: {@code share} with a right-hand text (0.1.11.121: vanilla font, furnace bar). */
    protected void bar(GuiGraphics g, int x, int sy, Component label, double share, Component right) {
        VanillaFrame.labelledBar(g, x + ART_X, sy, ART_W, label, share, right);
    }

    /** 0.1.11.181: a label in the drawing (vanilla font, over the pixels). */
    protected static void label(GuiGraphics g, Component c, float x, float y, int rgb) {
        PumpCrossSection.text(g, c, x, y, rgb, 1F);
    }

    /** 0.1.11.181: a label centred on {@code cx}. */
    protected static void centred(GuiGraphics g, Component c, float cx, float y, int rgb) {
        PumpCrossSection.text(g, c, cx - PumpCrossSection.width(c, 1F) / 2, y, rgb, 1F);
    }

    /**
     * 0.1.11.181: whether the mouse (panel coordinates) is over the machine's drawing ({@link #wx}...) and not over one of its
     * slots: a slot shows its own tooltip only (as the processing machines since 0.1.11.179).
     */
    protected boolean overMachine(int rx, int ry) {
        return hoveredSlot == null && rx >= wx && rx < wx + ww && ry >= wy && ry < wy + wh;
    }

    /** 0.1.11.181: the readout lines in full when one did not fit (the mouse over the readouts). */
    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        int rx = mouseX - leftPos, ry = mouseY - topPos;
        if (!cutLines.isEmpty() && hoveredSlot == null && rx >= ART_X && rx < ART_RIGHT && ry >= READ_Y && ry < READ_Y + READ_H)
            g.renderComponentTooltip(this.font, cutLines, mouseX, mouseY);
    }

    protected static String f(String pattern, Object... args) { return String.format(java.util.Locale.ROOT, pattern, args); }

    /** 0.1.11.183: "stored / capacity EU" for the bar: in full below ten million, else three figures in k, M, G... */
    protected static Component stored(long e, long cap) {
        return Component.literal(compact(e) + " / " + compact(cap) + " EU");
    }

    protected static String compact(long v) {
        if (v < 10_000_000L) return f("%,d", v);
        String[] units = {"k", "M", "G", "T", "P"};
        double d = v;
        int u = -1;
        while (d >= 1000 && u < units.length - 1) { d /= 1000; u++; }
        return f(d >= 100 ? "%.0f%s" : d >= 10 ? "%.1f%s" : "%.2f%s", d, units[u]);
    }

    /** 0.1.11.144: the calm chrome (title on the left, no lamp or upgrade hint). */
    @Override
    protected boolean calmChrome() { return true; }
}
