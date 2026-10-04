package com.miophas.singularity_iteration.common.client.screen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/**
 * 动能发电机的 GUI类
 * 显示能量和动能信息
 */
@OnlyIn(Dist.CLIENT)
@SuppressWarnings("null")
public class mio_icif_gui_kinetic_generator extends mio_icif_screen<com.miophas.singularity_iteration.common.menu.generator.KineticGeneratorMenu> {

    private static final ResourceLocation GUI_TEXTURE =
        ResourceLocation.parse("mio_icif:textures/gui/gui_kinetic_generator.png");

    // GUI 尺寸
    private static final int GUI_WIDTH = 176;
    private static final int GUI_HEIGHT = 166;

    // 能量条位置（使用动能发电机能量条）
    private static final int ENERGY_BAR_X = 40;
    private static final int ENERGY_BAR_Y = 28;

    // 文本位置
    private static final int INFO_TEXT_X = 42;
    private static final int INFO_TEXT_Y = 50;
    // 文本颜色 20eb3e (绿色)
    private static final int INFO_TEXT_COLOR = 0x2A2E33;

    public mio_icif_gui_kinetic_generator(com.miophas.singularity_iteration.common.menu.generator.KineticGeneratorMenu menu, Inventory playerInventory, Component title) {
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

        com.miophas.singularity_iteration.common.menu.generator.KineticGeneratorMenu menu = this.menu;
        if (menu != null) {
            // 绘制 EU 能量条（动能发电机专属）
            int energyProgressPixels = menu.getEnergyProgressPixels();
            drawKineticEnergyBar(guiGraphics, x + ENERGY_BAR_X, y + ENERGY_BAR_Y, energyProgressPixels);

            // 绘制转换效率文本 (4 KU = 1 FE, 25%效率)
            String infoText = Component.translatable("gui.mio_icif.kinetic_generator.efficiency", 25).getString();
            // the read-out box spans x 42..136 on gui_kinetic_generator.png
            drawFitted(guiGraphics, this.font, Component.literal(infoText), x + INFO_TEXT_X, y + INFO_TEXT_Y, 94, INFO_TEXT_COLOR, true);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        super.renderLabels(guiGraphics, mouseX, mouseY);

        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;

        com.miophas.singularity_iteration.common.menu.generator.KineticGeneratorMenu menu = this.menu;
        if (menu == null) return;

        // 检查鼠标是否在能量条区域
        if (isHovering(mouseX, mouseY, x + ENERGY_BAR_X, y + ENERGY_BAR_Y, KINETIC_ENERGY_BAR_WIDTH, KINETIC_ENERGY_BAR_HEIGHT)) {
            guiGraphics.renderTooltip(this.font,
                Component.literal("Energy: " + menu.getEnergy() + "/" + menu.getMaxEnergy() + " EU"),
                mouseX - x, mouseY - y);
        }
    }

    @Override
    public void containerTick() {
        super.containerTick();
        // 数据通过 ContainerData 自动同步，无需手动同步
    }
}