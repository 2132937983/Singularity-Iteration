package com.singularity_iteration.mio_icif.Screen;

import com.singularity_iteration.mio_icif.Menu.Producer.ElectrolyzerElcMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/**
 * 电解机方块的 GUI类
 */
@OnlyIn(Dist.CLIENT)
@SuppressWarnings("null")
public class mio_icif_gui_electrolyzer_elc extends mio_icif_screen<ElectrolyzerElcMenu> {

    private static final ResourceLocation GUI_TEXTURE =
        ResourceLocation.parse("mio_icif:textures/gui/gui_electrolyzer_elc.png");

    private static final int GUI_WIDTH = 176;
    private static final int GUI_HEIGHT = 166;

    private static final int ENERGY_X = 74;
    private static final int ENERGY_Y = 34;

    private static final int STATUS_X = 152;
    private static final int STATUS_Y = 8;
    private static final int STATUS_SIZE = 16;

    public mio_icif_gui_electrolyzer_elc(ElectrolyzerElcMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = GUI_WIDTH;
        this.imageHeight = GUI_HEIGHT;
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;

        // 绘制背景
        guiGraphics.blit(GUI_TEXTURE, x, y, 0, 0, this.imageWidth, this.imageHeight);

        ElectrolyzerElcMenu menu = this.getMenu();
        if (menu != null) {
            // 绘制标准能量条（显示电能存储量）
            drawEnergyBar(guiGraphics, x + ENERGY_X, y + ENERGY_Y, menu.getEnergyProgressPixels());

            // 绘制状态图标
            int status = menu.getStatus();
            if (status == 1) {
                // 充电模式
                guiGraphics.blit(ATLAS_TEXTURE, x + STATUS_X, y + STATUS_Y, 0, (float) 28, (float) 192, STATUS_SIZE, STATUS_SIZE, ATLAS_WIDTH, ATLAS_HEIGHT);
            } else if (status == 2) {
                // 放电模式
                guiGraphics.blit(ATLAS_TEXTURE, x + STATUS_X, y + STATUS_Y, 0, (float) 46, (float) 192, STATUS_SIZE, STATUS_SIZE, ATLAS_WIDTH, ATLAS_HEIGHT);
            }
        }
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        super.renderLabels(guiGraphics, mouseX, mouseY);

        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;

        ElectrolyzerElcMenu menu = this.getMenu();
        if (menu == null) return;

        // 检查鼠标是否在能量条区域
        if (isHovering(mouseX, mouseY, x + ENERGY_X, y + ENERGY_Y, ENERGY_BAR_WIDTH, ENERGY_BAR_HEIGHT)) {
            renderEnergyTooltip(guiGraphics, mouseX - x, mouseY - y, menu.getEnergy(), menu.getMaxEnergy());
        }

        // 检查鼠标是否在状态图标区域
        if (mouseX >= x + STATUS_X && mouseX <= x + STATUS_X + STATUS_SIZE &&
            mouseY >= y + STATUS_Y && mouseY <= y + STATUS_Y + STATUS_SIZE) {
            int status = menu.getStatus();
            Component tooltip;
            if (status == 1) {
                tooltip = Component.literal("Charging Mode");
            } else if (status == 2) {
                tooltip = Component.literal("Discharging Mode");
            } else {
                tooltip = Component.literal("Idle");
            }
            guiGraphics.renderTooltip(this.font, tooltip, mouseX - x, mouseY - y);
        }
    }

    @Override
    public void containerTick() {
        super.containerTick();
        
        // 直接从方块实体同步数据到客户端
        ElectrolyzerElcMenu menu = this.getMenu();
        if (menu != null && menu.getBlockEntity() != null) {
            var be = menu.getBlockEntity();
            menu.setSyncData(0, be.getProgress());
            menu.setSyncData(1, be.getMaxProgress());
            menu.setSyncData(2, be.isWorking() ? 1 : 0);
            menu.setSyncData(3, (int) be.getEnergyStorage().getAmount());
            menu.setSyncData(4, (int) be.getEnergyStorage().getCapacity());
            int status = 0;
            if (be.isCharging()) status = 1;
            else if (be.isDischarging()) status = 2;
            menu.setSyncData(5, status);
        }
    }
}