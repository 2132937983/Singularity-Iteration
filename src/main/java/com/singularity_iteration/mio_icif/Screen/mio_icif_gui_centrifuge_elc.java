package com.singularity_iteration.mio_icif.Screen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import com.singularity_iteration.mio_icif.Menu.Producer.CentrifugeElcMenu;

/**
 * 热能离心机方块的 GUI
 */
@OnlyIn(Dist.CLIENT)
@SuppressWarnings("null")
public class mio_icif_gui_centrifuge_elc extends mio_icif_screen<com.singularity_iteration.mio_icif.Menu.Producer.CentrifugeElcMenu> {

    private static final ResourceLocation GUI_TEXTURE =
        ResourceLocation.parse("mio_icif:textures/gui/gui_centrifuge_elc.png");

    // GUI 尺寸
    private static final int GUI_WIDTH = 176;
    private static final int GUI_HEIGHT = 166;

    // 闪电标志位置
    private static final int ENERGY_ICON_X = 12;
    private static final int ENERGY_ICON_Y = 41;

    // 热量条位置
    private static final int HEAT_BAR_X = 67;
    private static final int HEAT_BAR_Y = 65;

    // 绿灯标志位置 - 热量达到上限时显示
    private static final int GREEN_LIGHT_X = 92;
    private static final int GREEN_LIGHT_Y = 62;

    // 进度条位置
    private static final int PROGRESS_X = 83;
    private static final int PROGRESS_Y = 24;

    public mio_icif_gui_centrifuge_elc(CentrifugeElcMenu menu, Inventory playerInventory, Component title) {
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

        CentrifugeElcMenu menu = this.getMenu();
        if (menu != null) {
            // 绘制闪电标志（电能标志）- 先渲染背景，再从下往上填充
            int energy = menu.getEnergy();
            int maxEnergy = menu.getMaxEnergy();
            drawLightningEnergy(guiGraphics, x + ENERGY_ICON_X, y + ENERGY_ICON_Y, energy, maxEnergy);

            // 绘制热量条（从左往右填充，左边为底）
            int heat = menu.getHeat();
            int maxHeat = menu.getMaxHeat();
            drawHeatBar(guiGraphics, x + HEAT_BAR_X, y + HEAT_BAR_Y, heat, maxHeat);

            // 绘制绿灯标志（当热量达到上限时显示）
            if (heat >= maxHeat && maxHeat > 0) {
                drawHeatOkIcon(guiGraphics, x + GREEN_LIGHT_X, y + GREEN_LIGHT_Y);
            }

            // 绘制进度条（从下往上填充，下边为底）
            int progress = menu.getProgress();
            int maxProgress = menu.getMaxProgress();
            if (progress > 0 && maxProgress > 0) {
                drawCentrifugeProgress(guiGraphics, x + PROGRESS_X, y + PROGRESS_Y, progress, maxProgress);
            }
        }
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        super.renderLabels(guiGraphics, mouseX, mouseY);

        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;

        CentrifugeElcMenu menu = this.getMenu();
        if (menu == null) return;

        // 检查鼠标是否在进度条区域
        if (mouseX >= x + PROGRESS_X && mouseX <= x + PROGRESS_X + HEAT_ENERGY_CENTRIFUGE_WIDTH &&
            mouseY >= y + PROGRESS_Y && mouseY <= y + PROGRESS_Y + HEAT_ENERGY_CENTRIFUGE_HEIGHT) {
            int progress = menu.getProgress();
            int maxProgress = menu.getMaxProgress();
            guiGraphics.renderTooltip(this.font,
                Component.literal(progress + "/" + maxProgress),
                mouseX - x, mouseY - y);
        }

        // 检查鼠标是否在闪电标志区域
        if (mouseX >= x + ENERGY_ICON_X && mouseX <= x + ENERGY_ICON_X + LIGHTNING_WIDTH &&
            mouseY >= y + ENERGY_ICON_Y && mouseY <= y + ENERGY_ICON_Y + LIGHTNING_HEIGHT) {
            guiGraphics.renderTooltip(this.font,
                Component.literal("Energy: " + menu.getEnergy() + "/" + menu.getMaxEnergy() + " EU"),
                mouseX - x, mouseY - y);
        }

        // 检查鼠标是否在热量条区域
        if (mouseX >= x + HEAT_BAR_X && mouseX <= x + HEAT_BAR_X + HEAT_BAR_WIDTH &&
            mouseY >= y + HEAT_BAR_Y && mouseY <= y + HEAT_BAR_Y + HEAT_BAR_HEIGHT) {
            guiGraphics.renderTooltip(this.font,
                Component.literal("Heat: " + menu.getHeat() + "/" + menu.getMaxHeat() + " HU"),
                mouseX - x, mouseY - y);
        }
    }
}