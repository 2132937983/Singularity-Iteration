package com.singularity_iteration.mio_icif.Screen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import com.singularity_iteration.mio_icif.Menu.HUEntity.SolidHeatGeneratorMenu;

/**
 * 固体加热机的 GUI类
 */
@OnlyIn(Dist.CLIENT)
@SuppressWarnings("null")
public class mio_icif_gui_solid_heat_generator extends mio_icif_screen<com.singularity_iteration.mio_icif.Menu.HUEntity.SolidHeatGeneratorMenu> {

    private static final ResourceLocation GUI_TEXTURE =
            ResourceLocation.parse("mio_icif:textures/gui/gui_solid_heat_generator.png");

    // GUI 尺寸
    private static final int GUI_WIDTH = 176;
    private static final int GUI_HEIGHT = 166;

    // 标准火焰标志位置
    private static final int FLAME_X = 81;
    private static final int FLAME_Y = 29;
    private static final int FLAME_WIDTH = 14;
    private static final int FLAME_HEIGHT = 14;

    // 燃料输入槽位位置
    @SuppressWarnings("unused")
    private static final int FUEL_SLOT_X = 80;
    @SuppressWarnings("unused")
    private static final int FUEL_SLOT_Y = 45;

    // 灰烬输出槽位位置
    @SuppressWarnings("unused")
    private static final int ASH_SLOT_X = 113;
    @SuppressWarnings("unused")
    private static final int ASH_SLOT_Y = 45;

    // 热能文本显示位置 (49, 68)
    private static final int HEAT_TEXT_X = 49;
    private static final int HEAT_TEXT_Y = 68;
    // 热能文本颜色 (57c4da)
    private static final int HEAT_TEXT_COLOR = 0x57c4da;

    public mio_icif_gui_solid_heat_generator(SolidHeatGeneratorMenu menu, Inventory playerInventory, Component title) {
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

        SolidHeatGeneratorMenu menu = this.getMenu();
        if (menu != null) {
            // 使用基类标准方法绘制火焰能量标志
            // burnProgress 0-100 表示燃烧进度，需要映射为能量值用于火焰高度
            int burnProgress = menu.getBurnProgress();
            if (burnProgress > 0) {
                // 将 burnProgress (0-100) 映射为能量值 (0-10000)
                // burnProgress 越小（刚开始燃烧），火焰越满
                int flameEnergy = (100 - burnProgress) * 100;
                drawFlameEnergy(guiGraphics, x + FLAME_X, y + FLAME_Y, flameEnergy, 10000);
            }

            // 绘制热能产生速率文本
            int heatGeneration = menu.isWorking() ? 20 : 0;
            String heatText = heatGeneration + " HU/t";
            guiGraphics.drawString(this.font, heatText, x + HEAT_TEXT_X, y + HEAT_TEXT_Y, HEAT_TEXT_COLOR);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        // 不调用 super.renderLabels() 以避免渲染 Inventory 文本标签
    }

    @Override
    protected void renderTooltip(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        super.renderTooltip(guiGraphics, mouseX, mouseY);

        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;

        SolidHeatGeneratorMenu menu = this.getMenu();
        if (menu == null) return;

        // 检查鼠标是否在火焰标志区域
        if (mouseX >= x + FLAME_X && mouseX <= x + FLAME_X + FLAME_WIDTH &&
                mouseY >= y + FLAME_Y && mouseY <= y + FLAME_Y + FLAME_HEIGHT) {
            guiGraphics.renderTooltip(this.font,
                    Component.literal("Burning: " + menu.getBurnProgress() + "%"),
                    mouseX, mouseY);
        }
    }




}