// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.client.suit;

import com.miophas.singularity_iteration.common.client.screen.mio_icif_screen;
import com.miophas.singularity_iteration.common.suit.QuantumModStationBlockEntity;
import com.miophas.singularity_iteration.common.suit.QuantumModStationMenu;
import com.miophas.singularity_iteration.common.suit.SuitModuleType;
import com.miophas.singularity_iteration.common.suit.SuitModules;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

/**
 * Universal Suit Modification Station screen: installed-unit list with eject buttons, progress,
 * power. 0.1.8.5: a nano piece shows its lattice (LP bar, densifier level) instead of unit pips.
 */
public class QuantumModStationScreen extends mio_icif_screen<QuantumModStationMenu> {
    private static final ResourceLocation TEXTURE =
        ResourceLocation.fromNamespaceAndPath("mio_icif", "textures/gui/gui_quantum_modification_station.png");
    private static final int ENERGY_X = 8, ENERGY_Y = 18, ENERGY_W = 10, ENERGY_H = 70;
    private static final int PROGRESS_X = 36, PROGRESS_Y = 42, PROGRESS_W = 6, PROGRESS_H = 28;

    public QuantumModStationScreen(QuantumModStationMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = QuantumModStationMenu.GUI_WIDTH;
        this.imageHeight = QuantumModStationMenu.GUI_HEIGHT;
        this.inventoryLabelY = 113;
    }

    @Override
    protected void renderBg(GuiGraphics g, float partial, int mouseX, int mouseY) {
        int x = leftPos, y = topPos;
        g.blit(TEXTURE, x, y, 0, 0, imageWidth, imageHeight, 256, 256);
        // energy column
        float e = menu.energy() / (float) menu.capacity();
        int fill = (int) (Math.min(1, e) * (ENERGY_H - 2));
        g.fill(x + ENERGY_X + 1, y + ENERGY_Y + ENERGY_H - 1 - fill, x + ENERGY_X + ENERGY_W - 1, y + ENERGY_Y + ENERGY_H - 1, 0xFF3FB6FF);
        for (int i = 1; i < 7; i++) g.fill(x + ENERGY_X + 1, y + ENERGY_Y + i * 10, x + ENERGY_X + 4, y + ENERGY_Y + i * 10 + 1, 0x70000000);
        // install progress: rises from the unit slot to the piece slot
        int p = (int) (menu.installProgress() / (float) SuitModules.INSTALL_TICKS * PROGRESS_H);
        if (p > 0) {
            long t = System.currentTimeMillis() / 80;
            g.fill(x + PROGRESS_X, y + PROGRESS_Y + PROGRESS_H - p, x + PROGRESS_X + PROGRESS_W, y + PROGRESS_Y + PROGRESS_H, 0xFF4DE1C1);
            int band = (int) (t % PROGRESS_H);
            if (band < p) g.fill(x + PROGRESS_X, y + PROGRESS_Y + PROGRESS_H - band - 1, x + PROGRESS_X + PROGRESS_W, y + PROGRESS_Y + PROGRESS_H - band, 0xFFE8FFFA);
        }
        renderList(g, x, y, mouseX, mouseY);
        renderCore(g, x, y, mouseX, mouseY);
    }

    // ------------------------------------------------------------------ 0.1.9.0 core socket

    private static final int CORE_X = 27, CORE_Y = 91, CORE_EJECT_X = 46, CORE_EJECT_Y = 95;

    /** The piece takes a frame core (a suit helmet or chestplate). */
    private boolean coreSocket() {
        ItemStack suit = menu.suit();
        EquipmentSlot slot = SuitModules.slotOf(suit);
        return SuitModules.isSuitPiece(suit) && (slot == EquipmentSlot.HEAD || slot == EquipmentSlot.CHEST);
    }

    private void renderCore(GuiGraphics g, int x, int y, int mouseX, int mouseY) {
        if (!coreSocket()) return;
        var core = com.miophas.singularity_iteration.common.suit.core.FrameCores.core(menu.suit());
        int bx = x + CORE_X, by = y + CORE_Y;
        int edge = core == null ? 0xFF3A4A52 : 0xFF000000 | core.color();
        g.fill(bx - 1, by - 1, bx + 18, by + 18, edge);
        g.fill(bx, by, bx + 17, by + 17, 0xFF0B1A20);
        // socket diamond when empty
        if (core == null) {
            int cx = bx + 8, cy = by + 8;
            for (int k = 0; k < 5; k++) g.fill(cx - k, cy - 4 + k, cx + k + 1, cy - 3 + k, 0x403A6EA5);
            for (int k = 0; k < 4; k++) g.fill(cx - 3 + k, cy + 1 + k, cx + 4 - k, cy + 2 + k, 0x403A6EA5);
            return;
        }
        g.renderItem(new ItemStack(com.miophas.singularity_iteration.common.suit.SuitRegistry.coreItem(core)), bx, by);
        int ex = x + CORE_EJECT_X, ey = y + CORE_EJECT_Y;
        boolean over = mouseX >= ex && mouseX < ex + 9 && mouseY >= ey && mouseY < ey + 9;
        g.fill(ex, ey, ex + 9, ey + 9, over ? 0xFFFF5A4C : 0xFF8A3A34);
        g.drawString(font, "\u00D7", ex + 2, ey + 1, 0xFFFFFFFF, false);
    }

    /** Row height: compact rows when a nano piece carries many small units. */
    private int rowH() {
        return menu.installed().size() > 4 ? 11 : QuantumModStationMenu.ROW_H;
    }

    private int rowY(int ly, int i) {
        return ly + QuantumModStationMenu.LIST_TOP + (SuitModules.isNanoPiece(menu.suit()) ? 6 : 0) + i * rowH();
    }

    private void renderList(GuiGraphics g, int x, int y, int mouseX, int mouseY) {
        int lx = x + QuantumModStationMenu.LIST_X, ly = y + QuantumModStationMenu.LIST_Y;
        int lw = QuantumModStationMenu.LIST_W, lh = QuantumModStationMenu.LIST_H;
        ItemStack suit = menu.suit();
        EquipmentSlot slot = SuitModules.slotOf(suit);
        boolean nano = SuitModules.isNanoPiece(suit);
        int cap = slot == null || !SuitModules.isSuitPiece(suit) ? 0 : nano ? SuitModules.latticeCapacity(suit) : SuitModules.capacity(suit);
        List<com.miophas.singularity_iteration.common.suit.UnitRef> units = menu.installed();
        int head = nano ? 0xFF6FE6FF : 0xFF7CFFB2;
        if (nano) {
            // 0.1.8.5 nano lattice: LP bar, one cell per point, coloured by the unit that uses it
            int used = SuitModules.latticeUsed(suit);
            g.drawString(font, Component.translatable("gui.mio_icif.station.lattice", used, cap), lx + 4, ly + 3, head, false);
            for (int k = 0; k < SuitModules.LATTICE_MAX_LEVEL; k++) {      // densifier level diamonds
                int dx = lx + lw - 6 - k * 6, dy = ly + 6;
                boolean on = k < SuitModules.latticeLevel(suit);
                int c = on ? 0xFFFFD45A : 0x50FFD45A;
                g.fill(dx - 1, dy - 2, dx + 1, dy + 2, c);
                g.fill(dx - 2, dy - 1, dx + 2, dy + 1, c);
            }
            int bx = lx + 4, by = ly + 13, bw = lw - 8;
            float cell = bw / (float) Math.max(1, cap);
            int at = 0;
            g.fill(bx - 1, by - 1, bx + bw + 1, by + 5, 0xFF0B1A20);
            for (var u : units) {
                for (int k = 0; k < u.latticePoints() && at < cap; k++, at++) {
                    g.fill(bx + Math.round(at * cell), by, bx + Math.round((at + 1) * cell) - 1, by + 4, 0xFF000000 | u.color());
                }
            }
            for (; at < cap; at++) g.fill(bx + Math.round(at * cell), by, bx + Math.round((at + 1) * cell) - 1, by + 4, 0x306FE6FF);
            // the unit in the slot: preview of the points it would take
            var incoming = com.miophas.singularity_iteration.common.suit.UnitRef.ofItem(menu.unit());
            if (incoming != null && incoming.allows(com.miophas.singularity_iteration.core.api.suit.SuitClass.NANO)) {
                int from = used, to = Math.min(cap, used + incoming.latticePoints());
                boolean fits = used + incoming.latticePoints() <= cap;
                float blink = 0.5F + 0.5F * (float) Math.sin(System.currentTimeMillis() / 160.0);
                int a = (int) (0x50 + 0x80 * blink) << 24;
                for (int k = from; k < to; k++) g.fill(bx + Math.round(k * cell), by, bx + Math.round((k + 1) * cell) - 1, by + 4, a | (fits ? 0xFFFFFF : 0xFF5A4C));
                if (!fits) g.fill(bx + bw - 2, by - 1, bx + bw + 1, by + 5, 0xFFFF5A4C);
            }
        } else {
            g.drawString(font, Component.translatable("gui.mio_icif.station.units", units.size(), cap), lx + 4, ly + 3, head, false);
            // capacity pips; 0.1.10.0: the expansion bay slot is violet, and a bay expander in the unit slot previews it
            int base = cap - SuitModules.bayLevel(suit);
            boolean preview = QuantumModStationBlockEntity.isBayExpander(menu.unit()) && SuitModules.canExpand(suit);
            float blink = 0.5F + 0.5F * (float) Math.sin(System.currentTimeMillis() / 160.0);
            int shown = cap + (preview ? 1 : 0);
            for (int i = 0; i < shown; i++) {
                int px = lx + lw - 6 - (shown - 1 - i) * 7;
                boolean bay = i >= base;
                int c = i >= cap ? ((int) (0x50 + 0x80 * blink) << 24) | 0xB98CFF
                    : bay ? (i < units.size() ? 0xFFB98CFF : 0x50B98CFF) : (i < units.size() ? 0xFF7CFFB2 : 0x407CFFB2);
                g.fill(px, ly + 4, px + 5, ly + 9, c);
            }
        }
        int rh = rowH();
        boolean compact = rh < QuantumModStationMenu.ROW_H;
        for (int i = 0; i < units.size(); i++) {
            var type = units.get(i);
            int ry = rowY(ly, i);
            boolean hover = mouseX >= lx + 2 && mouseX < lx + lw - 2 && mouseY >= ry && mouseY < ry + rh - 1;
            g.fill(lx + 2, ry, lx + lw - 2, ry + rh - 1, hover ? 0x40FFFFFF : 0x20FFFFFF);
            g.fill(lx + 2, ry, lx + 4, ry + rh - 1, 0xFF000000 | type.color());
            int tx = lx + 22;
            if (compact) {
                tx = lx + 7;
            } else {
                g.renderItem(type.unitStack(), lx + 5, ry - 2);
            }
            String lp = nano ? type.latticePoints() + "LP" : null;
            int nameW = lw - (tx - lx) - 18 - (lp != null ? 16 : 0) - (type.switchable() ? 6 : 0);
            boolean mk2 = SuitModules.tier(suit, type.key()) >= 2;
            if (mk2) {                                   // 0.1.8.6: Mk.II badge
                g.fill(tx, ry + (compact ? 1 : 3), tx + 9, ry + (compact ? 9 : 11), 0xFF7A3CC8);
                g.drawString(font, "II", tx + 1, ry + (compact ? 1 : 3), 0xFFF3E8FF, false);
                tx += 11;
                nameW -= 11;
            }
            drawFitted(g, font, Component.translatable(type.nameKey()), tx, ry + (compact ? 1 : 3), nameW, 0xFFE8F4F8, false);
            if (lp != null) g.drawString(font, lp, lx + lw - 30 - font.width(lp) + (type.switchable() ? 0 : 6), ry + (compact ? 1 : 3), 0xFF6FE6FF, false);
            if (type.switchable()) {
                boolean on = com.miophas.singularity_iteration.core.prefab.item.ArmorFeatures.isEnabled(suit, type.featureKey());
                g.fill(lx + lw - 25, ry + (compact ? 3 : 4), lx + lw - 21, ry + (compact ? 7 : 8), on ? 0xFF7CFFB2 : 0xFF5A5F66);
            }
            // eject button
            int bs = compact ? 9 : 11;
            int bx = lx + lw - 14, by = ry + (compact ? 0 : 1);
            boolean overEject = mouseX >= bx && mouseX < bx + bs && mouseY >= by && mouseY < by + bs;
            g.fill(bx, by, bx + bs, by + bs, overEject ? 0xFFFF5A4C : 0xFF8A3A34);
            g.drawString(font, "×", bx + (compact ? 2 : 3), by + (compact ? 1 : 2), 0xFFFFFFFF, false);
        }
        if (units.isEmpty()) {
            Component hint = suit.isEmpty() ? Component.translatable("gui.mio_icif.station.insert_piece")
                : cap == 0 ? Component.translatable("gui.mio_icif.station.not_quantum")
                : Component.translatable(nano ? "gui.mio_icif.station.no_units_nano" : "gui.mio_icif.station.no_units");
            List<net.minecraft.util.FormattedCharSequence> lines = font.split(hint, lw - 8);
            for (int i = 0; i < lines.size() && i < 4; i++) g.drawString(font, lines.get(i), lx + 4, ly + (nano ? 26 : 22) + i * 10, 0xFF8FA4AE, false);
        }
        // status line
        QuantumModStationBlockEntity.Status status = menu.status();
        int color = switch (status) {
            case WORKING, UPGRADING, CORE_INSTALL, CORE_SWAP -> 0xFF4DE1C1;
            case IDLE -> 0xFF8FA4AE;
            case CORE_RETURNED -> 0xFFFFD27A;
            default -> 0xFFFF7A5C;
        };
        Component text = Component.translatable("gui.mio_icif.station.status." + status.name().toLowerCase(java.util.Locale.ROOT));
        drawFitted(g, font, text, lx + 4, ly + lh - 10, lw - 8, color, false);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (coreSocket() && com.miophas.singularity_iteration.common.suit.core.FrameCores.core(menu.suit()) != null) {
            int ex = leftPos + CORE_EJECT_X, ey = topPos + CORE_EJECT_Y;
            if (mouseX >= ex && mouseX < ex + 9 && mouseY >= ey && mouseY < ey + 9) {
                if (minecraft != null && minecraft.gameMode != null) {
                    minecraft.gameMode.handleInventoryButtonClick(menu.containerId, QuantumModStationMenu.CORE_BUTTON);
                    playClick();
                }
                return true;
            }
        }
        int lx = leftPos + QuantumModStationMenu.LIST_X, ly = topPos + QuantumModStationMenu.LIST_Y, lw = QuantumModStationMenu.LIST_W;
        List<com.miophas.singularity_iteration.common.suit.UnitRef> units = menu.installed();
        boolean compact = rowH() < QuantumModStationMenu.ROW_H;
        int bs = compact ? 9 : 11;
        for (int i = 0; i < units.size(); i++) {
            int ry = rowY(ly, i);
            int bx = lx + lw - 14, by = ry + (compact ? 0 : 1);
            if (mouseX >= bx && mouseX < bx + bs && mouseY >= by && mouseY < by + bs) {
                if (minecraft != null && minecraft.gameMode != null) {
                    minecraft.gameMode.handleInventoryButtonClick(menu.containerId, i);
                    playClick();
                }
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
        super.renderLabels(g, mouseX, mouseY);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partial) {
        super.render(g, mouseX, mouseY, partial);
        int x = leftPos, y = topPos;
        if (mouseX >= x + ENERGY_X && mouseX < x + ENERGY_X + ENERGY_W && mouseY >= y + ENERGY_Y && mouseY < y + ENERGY_Y + ENERGY_H) {
            g.renderComponentTooltip(font, List.of(Component.literal(menu.energy() + " / " + menu.capacity() + " EU"),
                Component.translatable("gui.mio_icif.station.cost", SuitModules.INSTALL_EU_PER_TICK, SuitModules.INSTALL_TICKS)
                    .withStyle(ChatFormatting.GRAY)), mouseX, mouseY);
        }
        // 0.1.9.0: core socket tooltip
        if (coreSocket() && mouseX >= x + CORE_X - 1 && mouseX < x + CORE_X + 18 && mouseY >= y + CORE_Y - 1 && mouseY < y + CORE_Y + 18) {
            var core = com.miophas.singularity_iteration.common.suit.core.FrameCores.core(menu.suit());
            List<Component> tip = new ArrayList<>();
            tip.add(Component.translatable("gui.mio_icif.station.core_socket").withStyle(ChatFormatting.AQUA));
            if (core == null) {
                tip.add(Component.translatable("gui.mio_icif.station.core_empty").withStyle(ChatFormatting.GRAY));
            } else {
                tip.add(Component.translatable(core.nameKey()).withStyle(s -> s.withColor(core.color())));
                tip.add(Component.translatable(core.descriptionKey()).withStyle(ChatFormatting.GRAY));
                tip.add(Component.translatable("gui.mio_icif.station.core_eject").withStyle(ChatFormatting.DARK_GRAY));
            }
            g.renderComponentTooltip(font, tip, mouseX, mouseY);
        }
        int lx = x + QuantumModStationMenu.LIST_X, ly = y + QuantumModStationMenu.LIST_Y, lw = QuantumModStationMenu.LIST_W;
        List<com.miophas.singularity_iteration.common.suit.UnitRef> units = menu.installed();
        for (int i = 0; i < units.size(); i++) {
            int ry = rowY(ly, i);
            if (mouseX >= lx + 2 && mouseX < lx + lw - 2 && mouseY >= ry && mouseY < ry + rowH() - 1) {
                var type = units.get(i);
                List<Component> tip = new ArrayList<>();
                tip.add(Component.translatable(type.nameKey()).withStyle(s -> s.withColor(type.color())));
                if (SuitModules.tier(menu.suit(), type.key()) >= 2) tip.add(Component.translatable("module.mio_icif.tooltip.mk2").withStyle(ChatFormatting.LIGHT_PURPLE));
                tip.add(Component.translatable(type.descriptionKey()).withStyle(ChatFormatting.GRAY));
                tip.add(type.builtin() == SuitModuleType.DEFLECTOR
                    ? Component.translatable("module.mio_icif.tooltip.deflector_cost", SuitModules.DEFLECTOR_EU_PER_DAMAGE, (int) SuitModules.DEFLECTOR_MAX_PER_HIT)
                    : Component.translatable("module.mio_icif.tooltip.drain", type.drainPerTick()));
                if (SuitModules.isNanoPiece(menu.suit()))
                    tip.add(Component.translatable("gui.mio_icif.station.lp_cost", type.latticePoints()).withStyle(ChatFormatting.AQUA));
                tip.add(Component.translatable("gui.mio_icif.station.eject_hint").withStyle(ChatFormatting.DARK_GRAY));
                g.renderComponentTooltip(font, tip, mouseX, mouseY);
            }
        }
    }
}
