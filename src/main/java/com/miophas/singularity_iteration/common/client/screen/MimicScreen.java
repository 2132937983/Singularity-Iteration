// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.client.screen;

import com.miophas.singularity_iteration.common.client.suit.AsciiHud;
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
 */
@OnlyIn(Dist.CLIENT)
@SuppressWarnings("null")
abstract class MimicScreen<T extends AbstractContainerMenu> extends mio_icif_screen<T> {

    /** 0.1.11.121: the window holds the diagram only (62 high); the reading lines go to the readout window under it. */
    protected int wx = 34, wy = 15, ww = 207, wh = 62;
    protected static final int STRIP_Y = 145, LINES_Y = 64, READ_Y = 80, READ_H = 62, H = 244;

    MimicScreen(T menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 248;
        this.imageHeight = H;   // 0.1.11.121: 204 before the readout window
    }

    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        // 0.1.11.24: one batch (each fill and text otherwise flushed on its own: ~115 draw calls a frame here, ~450 in
        // the control room). Only fills, quads and text are drawn here; a blit (immediate) must sit inside a scissor,
        // which flushes the batch first (PumpCrossSection).
        g.drawManaged(() -> {
            int x = leftPos, y = topPos;
            // 0.1.11.119: the vanilla container look (docs/gui_processing_machines.md)
            VanillaFrame.panel(g, x, y, imageWidth, imageHeight);
            for (Slot slot : menu.slots) {
                if (slot.x < 0 || slot.x > imageWidth) continue;   // docked upgrade slots draw their own frames
                VanillaFrame.slot(g, x + slot.x - 1, y + slot.y - 1);
            }
            VanillaFrame.inset(g, x + wx - 1, y + wy - 1, ww + 2, wh + 2, WindRoseView.PAPER);
            // 0.1.11.119: the diagram as pixel art (one cell a GUI pixel); the text is drawn over it afterwards
            if (pixelArt()) com.miophas.singularity_iteration.common.client.suit.FcsDraw.beginPixels(
                new com.miophas.singularity_iteration.common.client.suit.PixelCanvas(x + wx, y + wy, ww, wh, x + wx, y + wy, 1F, WindRoseView.PAPER));
            ReadoutPanel.begin();
            try {
                drawWindow(g, x + wx, y + wy, PlantMimic.time(partialTick));
            } finally {
                com.miophas.singularity_iteration.common.client.suit.FcsDraw.endPixels(g);
                ReadoutPanel.end(g, x + wx - 1, y + READ_Y, ww + 2, READ_H);
            }
            drawStrip(g, x, y + STRIP_Y);
        });
    }

    /** 0.1.11.119: whether the window is drawn as pixel art; a screen whose window is a chart keeps it smooth. */
    protected boolean pixelArt() { return true; }

    /** The diagram (the top ~58 px of the window) and the reading lines (from {@link #LINES_Y}). */
    protected abstract void drawWindow(GuiGraphics g, int x, int y, double time);

    protected abstract void drawStrip(GuiGraphics g, int x, int y);

    /** Reading line {@code n} (0..2) under the diagram. */
    protected void line(GuiGraphics g, int x, int y, int n, Component c, int rgb) {
        ReadoutPanel.add(n, c, rgb);   // 0.1.11.121: written in the readout window, in the vanilla font
    }

    /** A labelled bar under the window: {@code value} of {@code max} with a right-hand text (0.1.11.121: vanilla font, furnace bar). */
    protected void bar(GuiGraphics g, int x, int sy, Component label, double share, Component right) {
        VanillaFrame.labelledBar(g, x + wx, sy, x + 241 - (x + wx), label, share, right);
    }

    protected static String f(String pattern, Object... args) { return String.format(java.util.Locale.ROOT, pattern, args); }
}
