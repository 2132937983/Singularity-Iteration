package com.singularity_iteration.mio_icif.Screen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/**
 * 放射性同位素温差发电机(RTG) 的GUI类
 */
@OnlyIn(Dist.CLIENT)
@SuppressWarnings("null")
public class mio_icif_gui_rt_generator extends mio_icif_screen<com.singularity_iteration.mio_icif.Menu.Generator.RTGeneratorMenu> {

    private static final ResourceLocation GUI_TEXTURE =
        ResourceLocation.parse("mio_icif:textures/gui/gui_rt_generator.png");

    // GUI 尺寸
    private static final int GUI_WIDTH = 176;
    private static final int GUI_HEIGHT = 166;

    // 靶丸槽位位置 (6个槽位，2排3列，第一个槽位在36,36)
    /* private static final int SLOT_START_X = 36; */
    /* private static final int SLOT_START_Y = 36; */
    /* private static final int SLOT_SPACING_X = 18; */
    /* private static final int SLOT_SPACING_Y = 18; */

    // 标准能量条位置 (119, 40)
    private static final int ENERGY_BAR_X = 119;
    private static final int ENERGY_BAR_Y = 40;

    // 发电量显示位置 (119, 56) - 往下调6个像素
    private static final int OUTPUT_TEXT_X = 119;
    private static final int OUTPUT_TEXT_Y = 56;

    public mio_icif_gui_rt_generator(com.singularity_iteration.mio_icif.Menu.Generator.RTGeneratorMenu menu, Inventory playerInventory, Component title) {
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

        com.singularity_iteration.mio_icif.Menu.Generator.RTGeneratorMenu menu = this.menu;
        if (menu != null) {
            // 使用基类标准方法绘制能量条
            int progressPixels = menu.getMaxEnergy() > 0 ? (menu.getEnergy() * ENERGY_BAR_WIDTH / menu.getMaxEnergy()) : 0;
            drawEnergyBar(guiGraphics, x + ENERGY_BAR_X, y + ENERGY_BAR_Y, progressPixels);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        super.renderLabels(guiGraphics, mouseX, mouseY);

        com.singularity_iteration.mio_icif.Menu.Generator.RTGeneratorMenu menu = this.menu;
        if (menu == null) return;

        // 显示发电量(119, 50)
        long output = menu.getGenerationRate();
        Component outputText = Component.literal(output + " EU/t");
        guiGraphics.drawString(this.font, outputText, OUTPUT_TEXT_X, OUTPUT_TEXT_Y, 0x00FF00, false);

        // 检查鼠标是否在能量条区域（显示tooltip）
        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;
        if (mouseX >= x + ENERGY_BAR_X && mouseX <= x + ENERGY_BAR_X + ENERGY_BAR_WIDTH &&
            mouseY >= y + ENERGY_BAR_Y && mouseY <= y + ENERGY_BAR_Y + ENERGY_BAR_HEIGHT) {
            Component tooltip = Component.literal(menu.getEnergy() + " / " + menu.getMaxEnergy() + " EU");
            guiGraphics.renderTooltip(this.font, tooltip, mouseX, mouseY);
        }
    }

    @Override
    public void containerTick() {
        super.containerTick();
        // 数据通过 ContainerData 自动同步
    }
}