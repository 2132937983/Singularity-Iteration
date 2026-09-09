package com.singularity_iteration.mio_icif.Screen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/**
 * 风力发电机的 GUI类
 * GUI布局与太阳能发电机相似
 */
@OnlyIn(Dist.CLIENT)
@SuppressWarnings("null")
public class mio_icif_gui_wind_generator extends mio_icif_screen<com.singularity_iteration.mio_icif.Menu.Generator.WindGeneratorMenu> {

    private static final ResourceLocation GUI_TEXTURE =
        ResourceLocation.parse("mio_icif:textures/gui/gui_wind_generator.png");

    // GUI 尺寸
    private static final int GUI_WIDTH = 176;
    private static final int GUI_HEIGHT = 166;

    // 电池槽位位置
    /* private static final int BATTERY_SLOT_X = 80; */
    /* private static final int BATTERY_SLOT_Y = 26; */

    // 风扇图标位置 (80, 46) - 与太阳能的太阳图标位置相同
    private static final int FAN_ICON_X = 80;
    private static final int FAN_ICON_Y = 46;
    private static final int FAN_ICON_WIDTH = 14;
    private static final int FAN_ICON_HEIGHT = 14;

    // 蓝色风扇图标纹理位置 (176, 0) - 与太阳图标相同
    @SuppressWarnings("unused")
    private static final int FAN_BLUE_TEXTURE_X = 176;
    // private static final int FAN_BLUE_TEXTURE_Y = 0;

    // 红色风扇图标纹理位置 (176, 14) - 表示无法产生能量
    @SuppressWarnings("unused")
    private static final int FAN_RED_TEXTURE_X = 176;
    @SuppressWarnings("unused")
    private static final int FAN_RED_TEXTURE_Y = 14;

    // 最大发电量参考值（用于计算填充比例）理论最高约11.46，取12作为最大值
    private static final int MAX_ENERGY_OUTPUT = 12;

    public mio_icif_gui_wind_generator(com.singularity_iteration.mio_icif.Menu.Generator.WindGeneratorMenu menu, Inventory playerInventory, Component title) {
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

        com.singularity_iteration.mio_icif.Menu.Generator.WindGeneratorMenu menu = this.menu;
        if (menu != null) {
            // 绘制风扇图标
            if (menu.isGenerating()) {
                // 正在发电 - 绘制蓝色风扇，根据输出能量从下往上填充
                int currentOutput = menu.getCurrentOutput();
                int fillHeight = (currentOutput * FAN_ICON_HEIGHT) / MAX_ENERGY_OUTPUT;
                fillHeight = Math.min(fillHeight, FAN_ICON_HEIGHT); // 确保不超过图标高度
                if (fillHeight > 0) {
                    // 从下往上绘制填充部分
                    // int srcY = FAN_BLUE_TEXTURE_Y + FAN_ICON_HEIGHT - fillHeight;
                    int destY = y + FAN_ICON_Y + FAN_ICON_HEIGHT - fillHeight;
                    guiGraphics.blit(ATLAS_TEXTURE, x + FAN_ICON_X, destY, 0, (float) 216, (float) 227 + FAN_ICON_HEIGHT - fillHeight, FAN_ICON_WIDTH, fillHeight, ATLAS_WIDTH, ATLAS_HEIGHT);
                }
            } else {
                // 无法发电 - 绘制红色风扇（完整显示）
                guiGraphics.blit(ATLAS_TEXTURE, x + FAN_ICON_X, y + FAN_ICON_Y, 0, (float) 232, (float) 227, FAN_ICON_WIDTH, FAN_ICON_HEIGHT, ATLAS_WIDTH, ATLAS_HEIGHT);
            }
        }
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        super.renderLabels(guiGraphics, mouseX, mouseY);

        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;

        com.singularity_iteration.mio_icif.Menu.Generator.WindGeneratorMenu menu = this.menu;
        if (menu == null) return;

        // 检查鼠标是否在风扇图标区域
        if (mouseX >= x + FAN_ICON_X && mouseX <= x + FAN_ICON_X + FAN_ICON_WIDTH &&
            mouseY >= y + FAN_ICON_Y && mouseY <= y + FAN_ICON_Y + FAN_ICON_HEIGHT) {
            Component tooltip;
            if (menu.isGenerating()) {
                tooltip = Component.literal("Generating: " + menu.getCurrentOutput() + " EU/t");
            } else {
                tooltip = Component.literal("Not Generating");
            }
            guiGraphics.renderTooltip(this.font, tooltip, mouseX - x, mouseY - y);
        }
    }

    @Override
    public void containerTick() {
        super.containerTick();
        // 数据通过 broadcastChanges() 方法同步
    }
}