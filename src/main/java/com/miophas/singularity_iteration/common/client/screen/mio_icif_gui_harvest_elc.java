package com.miophas.singularity_iteration.common.client.screen;

import com.miophas.singularity_iteration.common.menu.producer.HarvestElcMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/**
 * 作物收割机方块的 GUI类
 */
@OnlyIn(Dist.CLIENT)
@SuppressWarnings("null")
public class mio_icif_gui_harvest_elc extends mio_icif_screen<HarvestElcMenu> {

    private static final ResourceLocation GUI_TEXTURE =
        ResourceLocation.parse("mio_icif:textures/gui/gui_harvest_elc.png");

    // GUI 尺寸
    private static final int GUI_WIDTH = 176;
    private static final int GUI_HEIGHT = 192;

    // 闪电标志位置
    private static final int LIGHTNING_X = 153;
    private static final int LIGHTNING_Y = 42;

    public mio_icif_gui_harvest_elc(HarvestElcMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = GUI_WIDTH;
        this.imageHeight = GUI_HEIGHT;
        this.inventoryLabelY = this.imageHeight - 94;
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;

        // 绘制背景
        guiGraphics.blit(GUI_TEXTURE, x, y, 0, 0, 176, this.imageHeight);

        HarvestElcMenu menu = this.menu;
        if (menu != null) {
            drawLightningEnergy(guiGraphics, x + LIGHTNING_X, y + LIGHTNING_Y, menu.getEnergy(), menu.getMaxEnergy());
        }
    }

    /**
     * <p><b>EN:</b> 0.1.8.1: a red status at the right end of the inventory label row while drops
     * wait for free storage. The row has free space there; the slots above do not.
     * <p><b>ZH:</b> 0.1.8.1：有物品等待存储空位时，在物品栏标签行右端显示红色状态。该处有空位，上方的槽位区没有。
     * <p><b>JA:</b> 0.1.8.1：ドロップが格納の空きを待つ間、インベントリ見出し行の右端に赤い状態表示を出す。
     * そこには空きがあり、上のスロット部分にはない。
     */
    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        super.renderLabels(guiGraphics, mouseX, mouseY);
        if (menu != null && menu.getPendingDrops() > 0) {
            drawFittedRight(guiGraphics, font, Component.translatable("gui.mio_icif.harvest.storage_full"),
                imageWidth - 8, inventoryLabelY, 100, 0xFFD04545, false);
        }
    }
}