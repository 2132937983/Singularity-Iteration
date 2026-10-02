package com.miophas.singularity_iteration.common.client.screen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/**
 * 水力动能发生机的 GUI 屏幕
 * 显示转子槽、生物群系状态和动能输出信息
 */
@OnlyIn(Dist.CLIENT)
@SuppressWarnings("null")
public class mio_icif_gui_water_kinetic_generator extends mio_icif_screen<com.miophas.singularity_iteration.common.menu.generator.WaterKineticGeneratorMenu> {

    private static final ResourceLocation GUI_TEXTURE =
        ResourceLocation.parse("mio_icif:textures/gui/gui_water_kinetic_generator.png");

    // GUI 尺寸
    private static final int GUI_WIDTH = 176;
    private static final int GUI_HEIGHT = 166;

    // 动能条位置
    private static final int KINETIC_BAR_X = 59;
    private static final int KINETIC_BAR_Y = 53;

    // 工作状态图标位置
    private static final int WORK_ICON_X = 80;
    private static final int WORK_ICON_Y = 70;

    // 文本位置
    private static final int INFO_TEXT_X = 42;
    private static final int OUTPUT_TEXT_Y = 50;
    private static final int BIOME_TEXT_Y = 66;
    private static final int ROTOR_DURABILITY_TEXT_Y = 75;
    // 文本颜色 20eb3e (绿色)
    private static final int INFO_TEXT_COLOR = 0x2A2E33;

    public mio_icif_gui_water_kinetic_generator(com.miophas.singularity_iteration.common.menu.generator.WaterKineticGeneratorMenu menu, Inventory playerInventory, Component title) {
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

        com.miophas.singularity_iteration.common.menu.generator.WaterKineticGeneratorMenu menu = this.menu;
        if (menu != null) {
            // 绘制动能条
            drawKineticBar(guiGraphics, x + KINETIC_BAR_X, y + KINETIC_BAR_Y, menu.getKinetic(), menu.getMaxKinetic());

            // 绘制工作状态图标
            drawWorkStatusIcon(guiGraphics, x + WORK_ICON_X, y + WORK_ICON_Y, menu.isGenerating());

            // 绘制输出信息文本
            String infoText = Component.translatable("gui.mio_icif.water_kinetic_generator.output", menu.getKineticOutput()).getString();
            guiGraphics.drawString(this.font, infoText, x + INFO_TEXT_X, y + OUTPUT_TEXT_Y, INFO_TEXT_COLOR);

            // 绘制生物群系信息
            String biomeText = Component.translatable("gui.mio_icif.water_kinetic_generator.biome", menu.getBiomeTypeName()).getString();
            guiGraphics.drawString(this.font, biomeText, x + INFO_TEXT_X, y + BIOME_TEXT_Y, INFO_TEXT_COLOR);

            // 绘制转子耐久度百分比（无转子时不显示）
            int rotorHealthPercent = menu.getRotorHealthPercent();
            if (rotorHealthPercent >= 0) {
                String rotorText = Component.translatable("gui.mio_icif.water_kinetic.rotor_health", rotorHealthPercent).getString();
                guiGraphics.drawString(this.font, rotorText, x + INFO_TEXT_X, y + ROTOR_DURABILITY_TEXT_Y, INFO_TEXT_COLOR);
            }
        }
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        super.renderLabels(guiGraphics, mouseX, mouseY);

        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;

        com.miophas.singularity_iteration.common.menu.generator.WaterKineticGeneratorMenu menu = this.menu;
        if (menu == null) return;

        // 检查鼠标是否在动能条区域
        if (isHovering(mouseX, mouseY, x + KINETIC_BAR_X, y + KINETIC_BAR_Y, KINETIC_BAR_WIDTH, KINETIC_BAR_HEIGHT)) {
            guiGraphics.renderTooltip(this.font,
                Component.translatable("gui.mio_icif.water_kinetic.kinetic", menu.getKinetic(), menu.getMaxKinetic()),
                mouseX - x, mouseY - y);
        }

        // 检查鼠标是否在工作状态图标区域
        if (isHovering(mouseX, mouseY, x + WORK_ICON_X, y + WORK_ICON_Y, WORK_ICON_WIDTH, WORK_ICON_HEIGHT)) {
            Component tooltip = menu.isGenerating() ?
                Component.translatable("gui.mio_icif.water_kinetic.generating", menu.getKineticOutput()) :
                Component.translatable("gui.mio_icif.water_kinetic.not_generating", Component.translatable(menu.getStatusReason()));
            guiGraphics.renderTooltip(this.font, tooltip, mouseX - x, mouseY - y);
        }
    }
}