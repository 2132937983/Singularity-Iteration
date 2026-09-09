package com.singularity_iteration.mio_icif.Screen;

import com.singularity_iteration.mio_icif.Menu.Producer.MagnetizerMenu;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.client.gui.GuiGraphics;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/**
 * 磁化机的 GUI 类
 * 包含电池槽和玩家四个盔甲槽显示（头盔、胸甲、护腿、靴子）
 */
@OnlyIn(Dist.CLIENT)
@SuppressWarnings("null")
public class mio_icif_gui_magnetizer extends mio_icif_screen<MagnetizerMenu> {

    private static final ResourceLocation GUI_TEXTURE =
        ResourceLocation.parse("mio_icif:textures/gui/gui_magnetizer.png");

    // 闪电标志位置（在电池槽上方）
    private static final int LIGHTNING_X = 8;
    private static final int LIGHTNING_Y = 27;

    // 能量条位置
    private static final int ENERGY_BAR_X = 79;
    private static final int ENERGY_BAR_Y = 34;

    // 工作指示器位置
    private static final int PROGRESS_X = 80;
    private static final int PROGRESS_Y = 20;

    public mio_icif_gui_magnetizer(MagnetizerMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 166;
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;

        // 绘制背景
        guiGraphics.blit(GUI_TEXTURE, x, y, 0, 0, this.imageWidth, this.imageHeight);

        MagnetizerMenu menu = this.getMenu();
        if (menu != null) {
            // 使用基类标准闪电标志渲染
            drawLightningEnergy(guiGraphics, x + LIGHTNING_X, y + LIGHTNING_Y, menu.getEnergy(), menu.getMaxEnergy());

            // 绘制水平能量条
            drawHorizontalEnergyBar(guiGraphics, x + ENERGY_BAR_X, y + ENERGY_BAR_Y, menu.getEnergy(), menu.getMaxEnergy());

            // 如果正在工作，绘制工作指示器
            if (menu.isWorking()) {
                guiGraphics.blit(ATLAS_TEXTURE, x + PROGRESS_X, y + PROGRESS_Y, 0, (float) 54, (float) 227, 16, 14, ATLAS_WIDTH, ATLAS_HEIGHT);
            }
        }
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        super.renderLabels(guiGraphics, mouseX, mouseY);

        // 绘制能量信息提示（当鼠标悬停在能量条上时）
        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;

        // 检查鼠标是否在能量条区域
        if (isHovering(mouseX, mouseY, x + ENERGY_BAR_X, y + ENERGY_BAR_Y, ENERGY_BAR2_WIDTH, ENERGY_BAR2_HEIGHT)) {
            MagnetizerMenu menu = this.getMenu();
            if (menu != null) {
                renderEnergyTooltip(guiGraphics, mouseX - x, mouseY - y, menu.getEnergy(), menu.getMaxEnergy());
            }
        }
    }
}