package com.singularity_iteration.mio_icif.Screen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/**
 * 斯特林发电机的GUI类
 * 没有电池槽，只显示能量和热能信息
 */
@OnlyIn(Dist.CLIENT)
@SuppressWarnings("null")
public class mio_icif_gui_stirling_generator extends mio_icif_screen<com.singularity_iteration.mio_icif.Menu.Generator.StirlingGeneratorMenu> {

    private static final ResourceLocation GUI_TEXTURE =
        ResourceLocation.parse("mio_icif:textures/gui/gui_stirling_generator.png");

    // GUI 尺寸
    private static final int GUI_WIDTH = 176;
    private static final int GUI_HEIGHT = 166;

    // 能量条位置
    private static final int ENERGY_BAR_X = 73;
    private static final int ENERGY_BAR_Y = 26;

    // 热能条位置
    private static final int HEAT_BAR_X = 40;
    private static final int HEAT_BAR_Y = 25;
    private static final int HEAT_BAR_WIDTH = 25;
    private static final int HEAT_BAR_HEIGHT = 17;

    // 热能条纹理位置
    @SuppressWarnings("unused")
    private static final int HEAT_BAR_TEXTURE_X = 176;
    @SuppressWarnings("unused")
    private static final int HEAT_BAR_TEXTURE_Y = 17;

    // 工作状态图标位置
    private static final int WORK_ICON_X = 80;
    private static final int WORK_ICON_Y = 46;
    private static final int WORK_ICON_WIDTH = 14;
    private static final int WORK_ICON_HEIGHT = 14;

    // 工作状态图标纹理位置
    @SuppressWarnings("unused")
    private static final int WORK_ACTIVE_TEXTURE_X = 176;
    @SuppressWarnings("unused")
    private static final int WORK_ACTIVE_TEXTURE_Y = 34;
    @SuppressWarnings("unused")
    private static final int WORK_INACTIVE_TEXTURE_X = 176;
    @SuppressWarnings("unused")
    private static final int WORK_INACTIVE_TEXTURE_Y = 48;

    // 文本位置
    private static final int INFO_TEXT_X = 42;
    private static final int INFO_TEXT_Y = 50;
    // 文本颜色 (与电力发热机相同)
    private static final int INFO_TEXT_COLOR = 0x57c4da;

    public mio_icif_gui_stirling_generator(com.singularity_iteration.mio_icif.Menu.Generator.StirlingGeneratorMenu menu, Inventory playerInventory, Component title) {
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

        com.singularity_iteration.mio_icif.Menu.Generator.StirlingGeneratorMenu menu = this.menu;
        if (menu != null) {
            // 绘制能量条
            int energyProgressPixels = menu.getEnergyProgressPixels();
            drawEnergyBar(guiGraphics, x + ENERGY_BAR_X, y + ENERGY_BAR_Y, energyProgressPixels);

            // 绘制热能条
            int heat = menu.getHeat();
            int maxHeat = menu.getMaxHeat();
            if (heat > 0 && maxHeat > 0) {
                int heatWidth = (heat * HEAT_BAR_WIDTH) / maxHeat;
                if (heatWidth > 0) {
                    guiGraphics.blit(ATLAS_TEXTURE, x + HEAT_BAR_X, y + HEAT_BAR_Y, 0, (float) 186, (float) 164, heatWidth, HEAT_BAR_HEIGHT, ATLAS_WIDTH, ATLAS_HEIGHT);
                }
            }

            // 绘制工作状态图标
            if (menu.isWorking()) {
                guiGraphics.blit(ATLAS_TEXTURE, x + WORK_ICON_X, y + WORK_ICON_Y, 0, (float) 88, (float) 227, WORK_ICON_WIDTH, WORK_ICON_HEIGHT, ATLAS_WIDTH, ATLAS_HEIGHT);
            } else {
                guiGraphics.blit(ATLAS_TEXTURE, x + WORK_ICON_X, y + WORK_ICON_Y, 0, (float) 104, (float) 227, WORK_ICON_WIDTH, WORK_ICON_HEIGHT, ATLAS_WIDTH, ATLAS_HEIGHT);
            }

            // 绘制 HU/EU 信息文本
            String infoText = "HU:" + menu.getHeat() + "/EU:" + menu.getEnergy();
            guiGraphics.drawString(this.font, infoText, x + INFO_TEXT_X, y + INFO_TEXT_Y, INFO_TEXT_COLOR);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        super.renderLabels(guiGraphics, mouseX, mouseY);

        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;

        com.singularity_iteration.mio_icif.Menu.Generator.StirlingGeneratorMenu menu = this.menu;
        if (menu == null) return;

        // 检查鼠标是否在能量条区域
        if (mouseX >= x + ENERGY_BAR_X && mouseX <= x + ENERGY_BAR_X + ENERGY_BAR_WIDTH &&
            mouseY >= y + ENERGY_BAR_Y && mouseY <= y + ENERGY_BAR_Y + ENERGY_BAR_HEIGHT) {
            guiGraphics.renderTooltip(this.font,
                Component.literal("Energy: " + menu.getEnergy() + "/" + menu.getMaxEnergy() + " EU"),
                mouseX - x, mouseY - y);
        }

        // 检查鼠标是否在热能条区域
        if (mouseX >= x + HEAT_BAR_X && mouseX <= x + HEAT_BAR_X + HEAT_BAR_WIDTH &&
            mouseY >= y + HEAT_BAR_Y && mouseY <= y + HEAT_BAR_Y + HEAT_BAR_HEIGHT) {
            guiGraphics.renderTooltip(this.font,
                Component.literal("Heat: " + menu.getHeat() + "/" + menu.getMaxHeat() + " HU"),
                mouseX - x, mouseY - y);
        }

        // 检查鼠标是否在工作状态图标区域
        if (mouseX >= x + WORK_ICON_X && mouseX <= x + WORK_ICON_X + WORK_ICON_WIDTH &&
            mouseY >= y + WORK_ICON_Y && mouseY <= y + WORK_ICON_Y + WORK_ICON_HEIGHT) {
            Component tooltip = menu.isWorking() ?
                Component.literal("Generating: " + menu.getEnergyOutput() + " EU/t") :
                Component.literal("Not Generating");
            guiGraphics.renderTooltip(this.font, tooltip, mouseX - x, mouseY - y);
        }
    }
}