package com.singularity_iteration.mio_icif.Screen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import com.singularity_iteration.mio_icif.Menu.Generator.WaterGeneratorMenu;

/**
 * 水力发电机的 GUI类
 * GUI布局与风力发电机类似，但多了一个水桶槽
 */
@OnlyIn(Dist.CLIENT)
@SuppressWarnings("null")
public class mio_icif_gui_water_generator extends mio_icif_screen<com.singularity_iteration.mio_icif.Menu.Generator.WaterGeneratorMenu> {

    private static final ResourceLocation GUI_TEXTURE =
        ResourceLocation.parse("mio_icif:textures/gui/gui_water_generator.png");

    // GUI 尺寸
    private static final int GUI_WIDTH = 176;
    private static final int GUI_HEIGHT = 166;

    // 水桶槽位位置(80, 53)
    /* private static final int BUCKET_SLOT_X = 80; */
    /* private static final int BUCKET_SLOT_Y = 53; */

    // 电池槽位位置(80, 26) - 与风力发电机相同
    /* private static final int BATTERY_SLOT_X = 80; */
    /* private static final int BATTERY_SLOT_Y = 26; */

    // 水桶图标位置 (80, 36)
    private static final int WATER_ICON_X = 80;
    private static final int WATER_ICON_Y = 36;
    private static final int WATER_ICON_WIDTH = 14;
    private static final int WATER_ICON_HEIGHT = 14;

    // 蓝色水桶图标纹理位置 (176, 0) - 表示正在发电
    @SuppressWarnings("unused")
    private static final int WATER_BLUE_TEXTURE_X = 176;
    // private static final int WATER_BLUE_TEXTURE_Y = 0;

    // 红色水桶图标纹理位置 (176, 14) - 表示无法产生能量
    @SuppressWarnings("unused")
    private static final int WATER_RED_TEXTURE_X = 176;
    @SuppressWarnings("unused")
    private static final int WATER_RED_TEXTURE_Y = 14;

    // 最大发电量参考值（用于计算填充比例）
    private static final int MAX_ENERGY_OUTPUT = 5;

    public mio_icif_gui_water_generator(WaterGeneratorMenu menu, Inventory playerInventory, Component title) {
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

        WaterGeneratorMenu menu = this.getMenu();
        if (menu != null) {
            // 绘制水桶图标
            if (menu.isGenerating()) {
                // 正在发电 - 绘制蓝色水桶，根据输出能量从下往上填充
                int currentOutput = menu.getCurrentOutput();
                int fillHeight = (currentOutput * WATER_ICON_HEIGHT) / MAX_ENERGY_OUTPUT;
                fillHeight = Math.min(fillHeight, WATER_ICON_HEIGHT); // 确保不超过图标高度
                if (fillHeight > 0) {
                    // 从下往上绘制填充部分
                    // int srcY = WATER_BLUE_TEXTURE_Y + WATER_ICON_HEIGHT - fillHeight;
                    int destY = y + WATER_ICON_Y + WATER_ICON_HEIGHT - fillHeight;
                    guiGraphics.blit(ATLAS_TEXTURE, x + WATER_ICON_X, destY, 0, (float) 2, (float) 243 + WATER_ICON_HEIGHT - fillHeight, WATER_ICON_WIDTH, fillHeight, ATLAS_WIDTH, ATLAS_HEIGHT);
                }
            } else {
                // 无法发电 - 绘制红色水桶（完整显示）
                guiGraphics.blit(ATLAS_TEXTURE, x + WATER_ICON_X, y + WATER_ICON_Y, 0, (float) 18, (float) 243, WATER_ICON_WIDTH, WATER_ICON_HEIGHT, ATLAS_WIDTH, ATLAS_HEIGHT);
            }
        }
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        super.renderLabels(guiGraphics, mouseX, mouseY);

        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;

        WaterGeneratorMenu menu = this.getMenu();
        if (menu == null) return;

        // 检查鼠标是否在水桶图标区域
        if (mouseX >= x + WATER_ICON_X && mouseX <= x + WATER_ICON_X + WATER_ICON_WIDTH &&
            mouseY >= y + WATER_ICON_Y && mouseY <= y + WATER_ICON_Y + WATER_ICON_HEIGHT) {
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

    /**
     * 水力发电机的容器菜单类
     */
}