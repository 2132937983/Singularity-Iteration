package com.singularity_iteration.mio_icif.Screen;

import com.singularity_iteration.mio_icif.Menu.Producer.BlastFurnaceMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/**
 * 高炉的 GUI
 * 基础纹理：gui_blast_furnace.png
 * 完全按指定坐标和纹理区域布局
 */
@OnlyIn(Dist.CLIENT)
@SuppressWarnings("null")
public class mio_icif_gui_blast_furnace extends mio_icif_screen<BlastFurnaceMenu> {

    private static final ResourceLocation GUI_TEXTURE =
        ResourceLocation.parse("mio_icif:textures/gui/gui_blast_furnace.png");

    // GUI 尺寸
    private static final int GUI_WIDTH = 176;
    private static final int GUI_HEIGHT = 166;

    // 热量进度条位置
    private static final int HEAT_BAR_X = 70;
    private static final int HEAT_BAR_Y = 69;

    // 热量允许标志位置
    private static final int HEAT_OK_X = 95;
    private static final int HEAT_OK_Y = 66;

    // 压缩空气允许标志位置
    private static final int AIR_OK_X = 75;
    private static final int AIR_OK_Y = 34;

 // 高冶炼进度条位置
    private static final int PROGRESS_X = 75;
    private static final int PROGRESS_Y = 34;

    public mio_icif_gui_blast_furnace(BlastFurnaceMenu menu, Inventory playerInventory, Component title) {
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

        BlastFurnaceMenu menu = this.menu;

        // 1. 热量进度条：根据热量从左往右填充
        int heat = menu.getHeat();
        int maxHeat = menu.getMaxHeat();
        drawHeatBar(guiGraphics, x + HEAT_BAR_X, y + HEAT_BAR_Y, heat, maxHeat);

        // 2. 热量允许标志：热量满时显示
        if (menu.isHot()) {
            drawHeatOkIcon(guiGraphics, x + HEAT_OK_X, y + HEAT_OK_Y);
        }

        // 3. 压缩空气允许标志：存在空气且热量满时显示
        if (menu.isHot() && menu.getAirAmount() > 0) {
            drawAirOkIcon(guiGraphics, x + AIR_OK_X, y + AIR_OK_Y);
        }

 // 4. 高冶炼进度条：原材料符合且热量空气满足时，从下往上根据进度显示
        int progress = menu.getProgress();
        int maxProgress = menu.getMaxProgress();
        if (progress > 0 && maxProgress > 0 && menu.isHot() && menu.getAirAmount() > 0) {
            drawBlastProgress(guiGraphics, x + PROGRESS_X, y + PROGRESS_Y, progress, maxProgress);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        super.renderLabels(guiGraphics, mouseX, mouseY);

        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;

        BlastFurnaceMenu menu = this.menu;

        // 热量条提示
        if (mouseX >= x + HEAT_BAR_X && mouseX <= x + HEAT_BAR_X + HEAT_BAR_WIDTH &&
            mouseY >= y + HEAT_BAR_Y && mouseY <= y + HEAT_BAR_Y + HEAT_BAR_HEIGHT) {
            guiGraphics.renderTooltip(this.font,
                Component.literal("Heat: " + menu.getHeat() + "/" + menu.getMaxHeat() + " HU"),
                mouseX - x, mouseY - y);
        }

        // 热量允许标志提示
        if (mouseX >= x + HEAT_OK_X && mouseX <= x + HEAT_OK_X + HEAT_OK_WIDTH &&
            mouseY >= y + HEAT_OK_Y && mouseY <= y + HEAT_OK_Y + HEAT_OK_HEIGHT) {
            guiGraphics.renderTooltip(this.font,
                Component.literal(menu.isHot() ? "Heat Ready" : "Not Enough Heat"),
                mouseX - x, mouseY - y);
        }

        // 空气/进度区域提示
        if (mouseX >= x + PROGRESS_X && mouseX <= x + PROGRESS_X + BLAST_PROGRESS_WIDTH &&
            mouseY >= y + PROGRESS_Y && mouseY <= y + PROGRESS_Y + BLAST_PROGRESS_HEIGHT) {
            guiGraphics.renderTooltip(this.font,
                Component.literal("Progress: " + menu.getProgress() + "/" + menu.getMaxProgress()
                    + " | Air: " + menu.getAirAmount() + "/" + menu.getAirCapacity() + " mB"),
                mouseX - x, mouseY - y);
        }
    }
}