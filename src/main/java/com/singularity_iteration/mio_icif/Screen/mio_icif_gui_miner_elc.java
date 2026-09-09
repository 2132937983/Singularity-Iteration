package com.singularity_iteration.mio_icif.Screen;

import com.singularity_iteration.mio_icif.Menu.Producer.MinerElcMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/**
 * 电力采矿机方块的 GUI 类
 */
@OnlyIn(Dist.CLIENT)
@SuppressWarnings("null")
public class mio_icif_gui_miner_elc extends mio_icif_screen<MinerElcMenu> {

    private static final ResourceLocation GUI_TEXTURE =
        ResourceLocation.parse("mio_icif:textures/gui/gui_miner_elc.png");

    private static final int GUI_WIDTH = 176;
    private static final int GUI_HEIGHT = 166;

    private static final int ENERGY_ICON_X = 152;
    private static final int ENERGY_ICON_Y = 40;

    public mio_icif_gui_miner_elc(MinerElcMenu menu, Inventory playerInventory, Component title) {
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

        MinerElcMenu menu = this.getMenu();
        if (menu != null) {
            // 只渲染闪电能量标志，不渲染工作进度条
            drawLightningEnergy(guiGraphics, x + ENERGY_ICON_X, y + ENERGY_ICON_Y, menu.getEnergy(), menu.getMaxEnergy());
        }
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        super.renderLabels(guiGraphics, mouseX, mouseY);

        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;

        MinerElcMenu menu = this.getMenu();
        if (menu == null) return;

        // 只渲染能量tooltip
        if (isHovering(mouseX, mouseY, x + ENERGY_ICON_X, y + ENERGY_ICON_Y, LIGHTNING_WIDTH, LIGHTNING_HEIGHT)) {
            renderEnergyTooltip(guiGraphics, mouseX - x, mouseY - y, menu.getEnergy(), menu.getMaxEnergy());
        }
    }

    @Override
    public void containerTick() {
        super.containerTick();

        // 直接从方块实体同步数据到客户端
        MinerElcMenu menu = this.getMenu();
        if (menu != null && menu.blockEntity != null) {
            menu.setSyncData(0, menu.blockEntity.getProgress());
            menu.setSyncData(1, menu.blockEntity.getMaxProgress());
            menu.setSyncData(2, menu.blockEntity.isWorking() ? 1 : 0);
            menu.setSyncData(3, (int) menu.blockEntity.getEnergyStorage().getAmount());
            menu.setSyncData(4, (int) menu.blockEntity.getEnergyStorage().getCapacity());
        }
    }
}