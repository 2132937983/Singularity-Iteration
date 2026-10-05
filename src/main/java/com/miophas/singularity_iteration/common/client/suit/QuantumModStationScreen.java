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

/** Quantum modification station screen: installed-unit list with eject buttons, progress, power. */
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
    }

    private void renderList(GuiGraphics g, int x, int y, int mouseX, int mouseY) {
        int lx = x + QuantumModStationMenu.LIST_X, ly = y + QuantumModStationMenu.LIST_Y;
        int lw = QuantumModStationMenu.LIST_W, lh = QuantumModStationMenu.LIST_H;
        ItemStack suit = menu.suit();
        EquipmentSlot slot = SuitModules.slotOf(suit);
        int cap = slot == null || !SuitModules.isSuitPiece(suit) ? 0 : SuitModules.capacity(slot);
        List<SuitModuleType> units = menu.installed();
        int head = 0xFF7CFFB2;
        g.drawString(font, Component.translatable("gui.mio_icif.station.units", units.size(), cap), lx + 4, ly + 3, head, false);
        // capacity pips
        for (int i = 0; i < cap; i++) {
            int px = lx + lw - 6 - (cap - 1 - i) * 7;
            g.fill(px, ly + 4, px + 5, ly + 9, i < units.size() ? 0xFF7CFFB2 : 0x407CFFB2);
        }
        for (int i = 0; i < units.size(); i++) {
            SuitModuleType type = units.get(i);
            int ry = ly + QuantumModStationMenu.LIST_TOP + i * QuantumModStationMenu.ROW_H;
            boolean hover = mouseX >= lx + 2 && mouseX < lx + lw - 2 && mouseY >= ry && mouseY < ry + QuantumModStationMenu.ROW_H - 1;
            g.fill(lx + 2, ry, lx + lw - 2, ry + QuantumModStationMenu.ROW_H - 1, hover ? 0x40FFFFFF : 0x20FFFFFF);
            g.fill(lx + 2, ry, lx + 4, ry + QuantumModStationMenu.ROW_H - 1, 0xFF000000 | type.color());
            g.renderItem(new ItemStack(com.miophas.singularity_iteration.common.suit.SuitRegistry.unitItem(type)), lx + 5, ry - 2);
            drawFitted(g, font, Component.translatable(type.nameKey()), lx + 22, ry + 3, lw - 40, 0xFFE8F4F8, false);
            boolean on = com.miophas.singularity_iteration.core.prefab.item.ArmorFeatures.isEnabled(suit, type.featureKey());
            g.fill(lx + lw - 25, ry + 4, lx + lw - 21, ry + 8, on ? 0xFF7CFFB2 : 0xFF5A5F66);
            // eject button
            int bx = lx + lw - 14, by = ry + 1;
            boolean overEject = mouseX >= bx && mouseX < bx + 11 && mouseY >= by && mouseY < by + 11;
            g.fill(bx, by, bx + 11, by + 11, overEject ? 0xFFFF5A4C : 0xFF8A3A34);
            g.drawString(font, "×", bx + 3, by + 2, 0xFFFFFFFF, false);
        }
        if (units.isEmpty()) {
            Component hint = suit.isEmpty() ? Component.translatable("gui.mio_icif.station.insert_piece")
                : cap == 0 ? Component.translatable("gui.mio_icif.station.not_quantum")
                : Component.translatable("gui.mio_icif.station.no_units");
            List<net.minecraft.util.FormattedCharSequence> lines = font.split(hint, lw - 8);
            for (int i = 0; i < lines.size() && i < 4; i++) g.drawString(font, lines.get(i), lx + 4, ly + 22 + i * 10, 0xFF8FA4AE, false);
        }
        // status line
        QuantumModStationBlockEntity.Status status = menu.status();
        int color = switch (status) {
            case WORKING -> 0xFF4DE1C1;
            case IDLE -> 0xFF8FA4AE;
            default -> 0xFFFF7A5C;
        };
        Component text = Component.translatable("gui.mio_icif.station.status." + status.name().toLowerCase(java.util.Locale.ROOT));
        drawFitted(g, font, text, lx + 4, ly + lh - 10, lw - 8, color, false);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int lx = leftPos + QuantumModStationMenu.LIST_X, ly = topPos + QuantumModStationMenu.LIST_Y, lw = QuantumModStationMenu.LIST_W;
        List<SuitModuleType> units = menu.installed();
        for (int i = 0; i < units.size(); i++) {
            int ry = ly + QuantumModStationMenu.LIST_TOP + i * QuantumModStationMenu.ROW_H;
            int bx = lx + lw - 14, by = ry + 1;
            if (mouseX >= bx && mouseX < bx + 11 && mouseY >= by && mouseY < by + 11) {
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
        int lx = x + QuantumModStationMenu.LIST_X, ly = y + QuantumModStationMenu.LIST_Y, lw = QuantumModStationMenu.LIST_W;
        List<SuitModuleType> units = menu.installed();
        for (int i = 0; i < units.size(); i++) {
            int ry = ly + QuantumModStationMenu.LIST_TOP + i * QuantumModStationMenu.ROW_H;
            if (mouseX >= lx + 2 && mouseX < lx + lw - 2 && mouseY >= ry && mouseY < ry + QuantumModStationMenu.ROW_H - 1) {
                SuitModuleType type = units.get(i);
                List<Component> tip = new ArrayList<>();
                tip.add(Component.translatable(type.nameKey()).withStyle(s -> s.withColor(type.color())));
                tip.add(Component.translatable(type.descriptionKey()).withStyle(ChatFormatting.GRAY));
                tip.add(type == SuitModuleType.DEFLECTOR
                    ? Component.translatable("module.mio_icif.tooltip.deflector_cost", SuitModules.DEFLECTOR_EU_PER_DAMAGE, (int) SuitModules.DEFLECTOR_MAX_PER_HIT)
                    : Component.translatable("module.mio_icif.tooltip.drain", type.drainPerTick()));
                tip.add(Component.translatable("gui.mio_icif.station.eject_hint").withStyle(ChatFormatting.DARK_GRAY));
                g.renderComponentTooltip(font, tip, mouseX, mouseY);
            }
        }
    }
}
