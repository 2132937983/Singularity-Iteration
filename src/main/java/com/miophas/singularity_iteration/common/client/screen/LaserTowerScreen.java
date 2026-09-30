package com.miophas.singularity_iteration.common.client.screen;

import com.miophas.singularity_iteration.common.menu.producer.LaserTowerMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/**
 * Laser tower GUI. Drawn with flat fills (no texture): an energy bar, the scan box
 * (&plusmn;X/Z and &plusmn;Y) with -8/-1/+1/+8 buttons, and volley statistics.
 */
@OnlyIn(Dist.CLIENT)
@SuppressWarnings("null")
public class LaserTowerScreen extends mio_icif_screen<LaserTowerMenu> {
    private static final int PANEL = 0xFFC6C6C6, PANEL_DARK = 0xFF555555, PANEL_LIGHT = 0xFFFFFFFF;
    private static final int SLOT_BG = 0xFF8B8B8B;
    private static final int TEXT = 0x404040;
    private static final int BAR_X = 30, BAR_Y = 18, BAR_W = 138, BAR_H = 6;
    private static final int ROW_H_Y = 29, ROW_V_Y = 45;
    private static final String[] BUTTON_LABELS = {"-1", "+1", "-8", "+8"};

    public LaserTowerScreen(LaserTowerMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = LaserTowerMenu.GUI_HEIGHT;
    }

    @Override
    protected void init() {
        super.init();
        this.inventoryLabelY = this.imageHeight - 94;
        addRow(ROW_H_Y, 0);
        addRow(ROW_V_Y, 2);
    }

    /** Buttons in display order -8, -1, +1, +8 mapped onto menu button ids. */
    private void addRow(int y, int base) {
        int[] ids = {base + 4, base, base + 1, base + 5};
        String[] labels = {BUTTON_LABELS[2], BUTTON_LABELS[0], BUTTON_LABELS[1], BUTTON_LABELS[3]};
        for (int i = 0; i < 4; i++) {
            int id = ids[i];
            addRenderableWidget(Button.builder(Component.literal(labels[i]), b -> press(id))
                .bounds(leftPos + 96 + i * 18, topPos + y - 2, 17, 12).build());
        }
    }

    private void press(int id) {
        if (minecraft != null && minecraft.gameMode != null) {
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId, id);
        }
    }

    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        int x = leftPos, y = topPos;
        // Raised panel
        g.fill(x, y, x + imageWidth, y + imageHeight, PANEL_DARK);
        g.fill(x, y, x + imageWidth - 1, y + imageHeight - 1, PANEL_LIGHT);
        g.fill(x + 1, y + 1, x + imageWidth - 1, y + imageHeight - 1, PANEL);
        for (Slot slot : menu.slots) {
            int sx = x + slot.x - 1, sy = y + slot.y - 1;
            g.fill(sx, sy, sx + 18, sy + 18, PANEL_LIGHT);
            g.fill(sx, sy, sx + 17, sy + 17, PANEL_DARK);
            g.fill(sx + 1, sy + 1, sx + 17, sy + 17, SLOT_BG);
        }

        long energy = menu.energy(), capacity = Math.max(1, menu.capacity());
        drawLightningEnergy(g, x + 9, y + 44, (int) (energy * 1000 / capacity), 1000);
        // Horizontal charge bar
        g.fill(x + BAR_X - 1, y + BAR_Y - 1, x + BAR_X + BAR_W + 1, y + BAR_Y + BAR_H + 1, PANEL_DARK);
        g.fill(x + BAR_X, y + BAR_Y, x + BAR_X + BAR_W, y + BAR_Y + BAR_H, 0xFF1A1A2A);
        int filled = (int) (BAR_W * Math.min(energy, capacity) / capacity);
        if (filled > 0) {
            g.fillGradient(x + BAR_X, y + BAR_Y, x + BAR_X + filled, y + BAR_Y + BAR_H, 0xFF7FE8FF, 0xFF2A7FFF);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
        super.renderLabels(g, mouseX, mouseY);
        g.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, TEXT, false);
        g.drawString(font, Component.translatable("gui.mio_icif.laser_tower.horizontal", menu.horizontalRange()),
            BAR_X, ROW_H_Y, TEXT, false);
        g.drawString(font, Component.translatable("gui.mio_icif.laser_tower.vertical", menu.verticalRange()),
            BAR_X, ROW_V_Y, TEXT, false);
        g.drawString(font, Component.translatable("gui.mio_icif.laser_tower.targets", menu.lastVolley(), menu.maxTargets()),
            BAR_X, 62, TEXT, false);
        g.drawString(font, Component.translatable("gui.mio_icif.laser_tower.cost", menu.costPerTarget()),
            BAR_X, 74, TEXT, false);
    }

    @Override
    protected void renderCustomTooltips(GuiGraphics g, int mouseX, int mouseY) {
        if (isHovering(mouseX, mouseY, leftPos + BAR_X, topPos + BAR_Y, BAR_W, BAR_H)
                || isHovering(mouseX, mouseY, leftPos + 9, topPos + 44, LIGHTNING_WIDTH, LIGHTNING_HEIGHT)) {
            g.renderTooltip(font, Component.translatable("gui.mio_icif.laser_tower.energy",
                String.format("%,d", menu.energy()), String.format("%,d", menu.capacity())), mouseX, mouseY);
        } else if (isHovering(mouseX, mouseY, leftPos + BAR_X, topPos + ROW_H_Y, 64, 9)) {
            g.renderTooltip(font, Component.translatable("gui.mio_icif.laser_tower.range_limit", menu.maxHorizontal()), mouseX, mouseY);
        } else if (isHovering(mouseX, mouseY, leftPos + BAR_X, topPos + ROW_V_Y, 64, 9)) {
            g.renderTooltip(font, Component.translatable("gui.mio_icif.laser_tower.range_limit", menu.maxVertical()), mouseX, mouseY);
        }
    }
}
