// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.client.screen;

import com.miophas.singularity_iteration.common.blockentity.generator.mio_icif_nuclear_reactor_generator;
import com.miophas.singularity_iteration.common.client.suit.AsciiHud;
import com.miophas.singularity_iteration.common.client.suit.FcsDraw;
import com.miophas.singularity_iteration.common.menu.generator.NuclearReactorGeneratorMenu;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/**
 * 0.1.11.21: the reactor's control room (generator mode), 300 x 284: the 9 x 6 grid with the three hottest parts
 * outlined; an annunciator panel (redstone, heat high, trip, rising, columns, output), the SCRAM button, the trip
 * setpoint and reset, the feed rate; the hull record of the last 60 cycles with the setpoint; and the heat balance of
 * the last cycle with the time to the setpoint.
 */
@OnlyIn(Dist.CLIENT)
@SuppressWarnings("null")
public class ReactorControlScreen extends mio_icif_screen<NuclearReactorGeneratorMenu> {
    private static final String K = "gui.mio_icif.reactor.control.";
    private static final int GX = NuclearReactorGeneratorMenu.REACTOR_SLOTS_X, GY = NuclearReactorGeneratorMenu.REACTOR_SLOTS_Y;
    private static final int PX = 184, RX = 14, RY = 134, RW = 162, RH = 26, LINE_Y = 166;
    // buttons: {x, y, w, h, event}
    // 0.1.11.147 (calm layout): the lamps in a column above, then SCRAM, the trip setpoint, reset and the feed, each row
    // wide enough for the vanilla font
    private static final int[][] BUTTONS = {
        {PX + 2, 94, 104, 18, 10},     // SCRAM
        {PX + 2, 116, 11, 11, 12}, {PX + 95, 116, 11, 11, 13},      // setpoint - +
        {PX + 2, 131, 104, 12, 11},    // reset
        {PX + 60, 147, 11, 11, 0}, {PX + 73, 147, 11, 11, 1}, {PX + 86, 147, 20, 11, 2},   // feed - + 1x
    };
    private static final int HEAD_Y = 162, LINE1_Y = 178, LINE2_Y = 188;   // the last line clear of the inventory (y 200)
    private static final String[] BUTTON_TEXT = {"SCRAM", "−", "+", null, "−", "+", "1x"};

    /** 0.1.11.76: the config card controls, left of the player inventory (the card slot is at the menu's CARD_SLOT_X/Y). */
    private static final int CARD_Y = NuclearReactorGeneratorMenu.CARD_SLOT_Y + 23;
    private final ReactorCardPanel card;

    public ReactorControlScreen(NuclearReactorGeneratorMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.imageWidth = 300;
        this.imageHeight = 284;
        this.card = new ReactorCardPanel(menu, new int[][] {{10, CARD_Y}, {28, CARD_Y}, {46, CARD_Y}}, 0xA0A8ACB1);
    }

    private int v(int i) { return menu.value(i); }

    private static String f(String p, Object... a) { return String.format(java.util.Locale.ROOT, p, a); }

    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        // 0.1.11.24: one batch (each fill and text otherwise flushed on its own: ~115 draw calls a frame here, ~450 in
        // the control room). Only fills, quads and text are drawn here; a blit (immediate) must sit inside a scissor,
        // which flushes the batch first (PumpCrossSection).
        g.drawManaged(() -> {
            int x = leftPos, y = topPos;
            float u = WindRoseView.u();
            VanillaFrame.panel(g, x, y, imageWidth, imageHeight);
            for (Slot slot : menu.slots) VanillaFrame.slot(g, x + slot.x - 1, y + slot.y - 1);
            int columns = menu.getAvailableColumns();
            for (int row = 0; row < 6; row++)
                for (int col = columns; col < 9; col++) {
                    int sx = x + GX + col * 18, sy = y + GY + row * 18;
                    g.fill(sx, sy, sx + 16, sy + 16, 0x90585D63);
                    FcsDraw.line(g, sx + 3, sy + 3, sx + 13, sy + 13, 1, 0xFFEEF0F1);
                    FcsDraw.line(g, sx + 13, sy + 3, sx + 3, sy + 13, 1, 0xFFEEF0F1);
                }
            // the hottest parts
            for (int k = 0; k < 3; k++) {
                int slot = v(17 + k), share = v(20 + k);
                if (slot < 0 || share <= 0) continue;
                int sx = x + GX + slot % 9 * 18 - 1, sy = y + GY + slot / 9 * 18 - 1;
                int c = share >= 800 ? 0xFFC0392B : share >= 500 ? 0xFFD9A32B : 0xFF4F9A6A;
                g.fill(sx, sy, sx + 18, sy + 1, c); g.fill(sx, sy + 17, sx + 18, sy + 18, c);
                g.fill(sx, sy, sx + 1, sy + 18, c); g.fill(sx + 17, sy, sx + 18, sy + 18, c);
            }
            drawPanel(g, x, y, u, mouseX, mouseY);
            drawRecord(g, x, y, u);
            drawLines(g, x, y, u);
            card.drawButtons(g, x, y, mouseX, mouseY);
        });
        card.drawGhosts(g, leftPos, topPos);   // items: outside the managed batch
    }

    private double hullShare() { return v(3) > 0 ? v(2) / (double) v(3) : 0; }

    private void drawPanel(GuiGraphics g, int x, int y, float u, int mouseX, int mouseY) {
        int status = v(11);
        boolean tripped = v(13) != 0, blink = (System.currentTimeMillis() / 400) % 2 == 0;
        Object[][] lamps = {
            {"lamp.redstone", status != mio_icif_nuclear_reactor_generator.STATUS_OFF && !tripped, 0xFF2F6E4F},
            {"lamp.heat", hullShare() >= 0.5, 0xFFD9A32B},
            {"lamp.trip", tripped, blink ? 0xFFC0392B : 0xFF7A2A22},
            {"lamp.rising", v(16) > 0, 0xFFD9A32B},
            {"lamp.columns", menu.getAvailableColumns() >= 9, 0xFF2F6E4F},
            {"lamp.output", v(4) > 0, 0xFF2F6E4F}};
        for (int i = 0; i < lamps.length; i++) {   // 0.1.11.147: a lamp and its name, one a row
            int lx = x + PX + 2, ly = y + 20 + i * 12;
            boolean on = (boolean) lamps[i][1];
            VanillaFrame.inset(g, lx, ly, 9, 9, on ? (int) lamps[i][2] : VanillaFrame.PANEL);
            g.drawString(font, Component.translatable(K + lamps[i][0]), lx + 13, ly + 1, on ? 0x1C2524 : 0x5B6867, false);
        }
        for (int i = 0; i < BUTTONS.length; i++) {
            int[] b = BUTTONS[i];
            int bx = x + b[0], by = y + b[1];
            boolean hover = mouseX >= bx && mouseX < bx + b[2] && mouseY >= by && mouseY < by + b[3];
            boolean scram = b[4] == 10;
            g.fill(bx, by, bx + b[2], by + b[3], scram ? 0xFF7A2A22 : VanillaFrame.SHADOW);
            g.fill(bx, by, bx + b[2] - 1, by + b[3] - 1, scram ? 0xFFE07A6E : VanillaFrame.LIGHT);
            g.fill(bx + 1, by + 1, bx + b[2] - 1, by + b[3] - 1, scram ? (hover ? 0xFFD9463A : 0xFFC0392B) : hover ? 0xFFD8D8E8 : 0xFFB8B8B8);
            Component c = BUTTON_TEXT[i] != null ? Component.literal(BUTTON_TEXT[i]) : Component.translatable(K + "reset");
            g.drawString(font, c, bx + (b[2] - font.width(c) + 1) / 2, by + (b[3] - 8) / 2 + 1, scram ? 0xFFFFFF : 0x404040, false);
        }
        int sp = v(12);
        Component spText = sp >= mio_icif_nuclear_reactor_generator.MAX_SETPOINT ? Component.translatable(K + "setpoint_off")
            : Component.translatable(K + "setpoint", sp + "%");
        g.drawString(font, spText, x + PX + 54 - font.width(spText) / 2, y + 118, 0x1C2524, false);
        g.drawString(font, Component.translatable(K + "feed", menu.getFeedRate()), x + PX + 2, y + 149, 0x1C2524, false);
    }

    private void drawRecord(GuiGraphics g, int x, int y, float u) {
        float rx = x + RX, ry = y + RY;
        g.fill((int) rx - 1, (int) ry - 1, (int) rx + RW + 1, (int) ry + RH + 1, 0xFFB8C0C2);
        g.fill((int) rx, (int) ry, (int) rx + RW, (int) ry + RH, 0xFFFBFCFC);
        int sp = v(12);
        if (sp < mio_icif_nuclear_reactor_generator.MAX_SETPOINT) {
            float sy = ry + RH - RH * sp / 100F;
            WindRoseView.dashed(g, rx, sy, rx + RW, sy, 0xFFC0392B);
        }
        int n = mio_icif_nuclear_reactor_generator.RECORD;
        int maxEu = 1;
        for (int i = 0; i < n; i++) maxEu = Math.max(maxEu, v(mio_icif_nuclear_reactor_generator.CONTROL_EU + i));
        float px = 0, py = 0, qx = 0, qy = 0;
        for (int i = 0; i < n; i++) {
            float nx = rx + RW * i / (n - 1F);
            float hy = ry + RH - RH * Math.min(1000, v(mio_icif_nuclear_reactor_generator.CONTROL_HULL + i)) / 1000F;
            float ey = ry + RH - RH * v(mio_icif_nuclear_reactor_generator.CONTROL_EU + i) / (float) maxEu;
            if (i > 0) {
                FcsDraw.line(g, qx, qy, nx, ey, 0.8F, 0x804F9A6A);
                FcsDraw.line(g, px, py, nx, hy, 1.2F, 0xFFD9531E);
            }
            px = nx; py = hy; qx = nx; qy = ey;
        }
        // 0.1.11.147: the record's name is in its tooltip (the feed row took its place)
    }

    /**
     * 0.1.11.147: the output twice the font size under the record, the state beside it (a config card's result for a few
     * seconds after its button); then the hull and the last cycle's balance. A state too long for the row goes down
     * as the first line.
     */
    private void drawLines(GuiGraphics g, int x, int y, float u) {
        int heat = v(2), max = v(3), sp = v(12);
        double share = hullShare();
        Component side;
        int sideColour;
        Component cardStatus = card.status();
        String key = mio_icif_nuclear_reactor_generator.statusKey(v(11));
        if (cardStatus != null) { side = cardStatus; sideColour = card.statusBad() ? 0xC0392B : 0x2F6E4F; }
        else if (key != null) { side = Component.translatable(key); sideColour = 0xC0392B; }
        else { side = Component.translatable(K + (v(4) > 0 ? "running" : "no_output")); sideColour = v(4) > 0 ? 0x2F6E4F : 0x5B6867; }
        int left = x + RX - 1, right = x + 292;
        Component head = Component.literal(f("%,d EU/t", v(4)));
        VanillaFrame.bigText(g, head, left, y + HEAD_Y, 0x1C2524);
        boolean below = 2 * font.width(head) + 8 + font.width(side) > right - left;
        if (!below) g.drawString(font, side, right - font.width(side), y + HEAD_Y + 4, sideColour, false);
        Component hull = Component.translatable(K + "line_hull", f("%,d", heat), f("%,d", max), Math.round(share * 100) + "%", f("%,d", v(4)), menu.getFeedRate());
        long delta = v(16);
        long target = sp < mio_icif_nuclear_reactor_generator.MAX_SETPOINT ? (long) max * sp / 100 : max;
        Component eta = delta > 0 ? Component.translatable(K + "eta", Math.max(0, (target - heat) / delta)) : Component.translatable(K + "eta_none");
        net.minecraft.network.chat.MutableComponent balance = Component.translatable(K + "line_balance", f("%,d", v(14)), f("%,d", v(15)),
            (delta >= 0 ? "+" : "") + f("%,d", delta)).append(" \u00B7 ").append(eta);
        if (v(17) >= 0 && v(20) > 0)
            balance.append(" \u00B7 ").append(Component.translatable(K + "hottest", v(17) / 9 + 1, v(17) % 9 + 1, Math.round(v(20) / 10.0) + "%"));
        if (below) {
            g.drawString(font, fit(side, right - left), left, y + LINE1_Y, sideColour, false);
            g.drawString(font, fit(hull, right - left), left, y + LINE2_Y, ReadoutPanel.TEXT, false);
        } else {
            g.drawString(font, fit(hull, right - left), left, y + LINE1_Y, ReadoutPanel.TEXT, false);
            g.drawString(font, fit(balance, right - left), left, y + LINE2_Y, ReadoutPanel.MUTED, false);
        }
    }

    private Component fit(Component c, int maxW) {
        if (font.width(c) <= maxW) return c;
        return Component.literal(font.plainSubstrByWidth(c.getString(), Math.max(0, maxW - font.width("\u2026"))) + "\u2026");
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
        g.drawString(this.font, this.title, 8, 6, VanillaFrame.TITLE, false);
        // 0.1.11.76: the card section's label, centred over the card slot
        Component label = Component.translatable("gui.mio_icif.reactor_card.label");
        int cx = NuclearReactorGeneratorMenu.CARD_SLOT_X + 8, w = Math.min(56, this.font.width(label));
        drawFitted(g, this.font, label, cx - w / 2, NuclearReactorGeneratorMenu.CARD_SLOT_Y - 12, 56, 0xFF5B6867, false);
        card.drawMismatches(g);
    }

    @Override
    protected void renderCustomTooltips(GuiGraphics g, int mouseX, int mouseY) {
        List<Component> lines = card.tooltip(leftPos, topPos, mouseX, mouseY, hoveredSlot);
        if (lines != null) g.renderComponentTooltip(this.font, lines, mouseX, mouseY);
    }

    @Override
    protected List<Component> getTooltipFromContainerItem(net.minecraft.world.item.ItemStack stack) {
        return card.withMismatch(super.getTooltipFromContainerItem(stack), hoveredSlot);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        int rx = mouseX - leftPos, ry = mouseY - topPos;
        if (rx >= PX && rx < PX + 108 && ry >= 20 && ry < 160 && hoveredSlot == null)
            g.renderComponentTooltip(this.font, List.of(Component.translatable(K + "tip_scram1"), Component.translatable(K + "tip_scram2"),
                Component.translatable(K + "tip_scram3")), mouseX, mouseY);
        // EXPLAIN_TIP_HIDDEN (0.1.11.182): the record chart's legend, hidden for now (the SCRAM panel keeps its control help)
        // else if (rx >= RX && rx < RX + RW && ry >= RY && ry < RY + RH)
        //     g.renderComponentTooltip(this.font, List.of(Component.translatable(K + "tip_record1"), Component.translatable(K + "tip_record2")), mouseX, mouseY);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (card.mouseClicked(leftPos, topPos, mouseX, mouseY, button)) return true;   // 0.1.11.76
        if (button == 0 && this.minecraft != null && this.minecraft.gameMode != null) {
            for (int[] b : BUTTONS) {
                if (mouseX >= leftPos + b[0] && mouseX < leftPos + b[0] + b[2] && mouseY >= topPos + b[1] && mouseY < topPos + b[1] + b[3]) {
                    int id = b[4];
                    if ((id == 0 || id == 1) && hasShiftDown()) id += 3;
                    this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, id);
                    return true;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    /** 0.1.11.147: the calm layout (no lamp or upgrade hint, window text in the vanilla font). */
    @Override
    protected boolean calmChrome() { return true; }
}
