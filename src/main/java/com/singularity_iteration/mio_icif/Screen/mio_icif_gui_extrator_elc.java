package com.singularity_iteration.mio_icif.Screen;

import com.singularity_iteration.mio_icif.Menu.Producer.ExtractorElcMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/**
 * 提取机方块的 GUI 屏幕
 */
@OnlyIn(Dist.CLIENT)
@SuppressWarnings("null")
public class mio_icif_gui_extrator_elc extends mio_icif_screen<ExtractorElcMenu> {

    private static final ResourceLocation GUI_TEXTURE =
        ResourceLocation.parse("mio_icif:textures/gui/gui_extractor_elc.png");

    private static final int GUI_WIDTH = 176;
    private static final int GUI_HEIGHT = 166;

    private static final int PROGRESS_X = 79;
    private static final int PROGRESS_Y = 34;

    private static final int ENERGY_ICON_X = 56;
    private static final int ENERGY_ICON_Y = 36;

    public mio_icif_gui_extrator_elc(ExtractorElcMenu menu, Inventory playerInventory, Component title) {
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

        ExtractorElcMenu menu = this.getMenu();
        if (menu != null) {
            drawExtractorProgress(guiGraphics, x + PROGRESS_X, y + PROGRESS_Y, menu.getProgressPixels(EXTRACTOR_PROGRESS_WIDTH));

            drawLightningEnergy(guiGraphics, x + ENERGY_ICON_X, y + ENERGY_ICON_Y, menu.getEnergy(), menu.getMaxEnergy());
        }
    }

    /**
     * 渲染自定义悬浮提示（进度条、能量等）
     * 在绝对屏幕坐标系中渲染，避免与 renderLabels 的坐标变换冲突
     */
    @Override
    protected void renderCustomTooltips(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        ExtractorElcMenu menu = this.getMenu();
        if (menu == null) return;

        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;

        // 检测进度条区域悬停
        if (isHovering(mouseX, mouseY, x + PROGRESS_X, y + PROGRESS_Y, EXTRACTOR_PROGRESS_WIDTH, EXTRACTOR_PROGRESS_HEIGHT)) {
            guiGraphics.renderTooltip(this.font,
                Component.literal(menu.getProgress() + "/" + menu.getMaxProgress()),
                mouseX, mouseY);
        }

        // 检测能量图标区域悬停
        if (isHovering(mouseX, mouseY, x + ENERGY_ICON_X, y + ENERGY_ICON_Y, LIGHTNING_WIDTH, LIGHTNING_HEIGHT)) {
            guiGraphics.renderTooltip(this.font,
                Component.literal(menu.getEnergy() + "/" + menu.getMaxEnergy() + " EU"),
                mouseX, mouseY);
        }
    }

    @Override
    public void containerTick() {
        super.containerTick();

        // 直接从方块实体同步数据到客户端
        ExtractorElcMenu menu = this.getMenu();
        if (menu != null && menu.blockEntity != null) {
            menu.setSyncData(0, menu.blockEntity.getProgress());
            menu.setSyncData(1, menu.blockEntity.getMaxProgress());
            menu.setSyncData(2, menu.blockEntity.isWorking() ? 1 : 0);
            menu.setSyncData(3, menu.blockEntity.getEnergyStorage().getEnergyStored());
            menu.setSyncData(4, menu.blockEntity.getEnergyStorage().getMaxEnergyStored());
        }
    }
}