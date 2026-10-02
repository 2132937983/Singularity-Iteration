package com.miophas.singularity_iteration.common.client.screen;

import com.miophas.singularity_iteration.common.menu.producer.InductionElcMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/**
 * 鎰熷簲鐐夋柟鍧楃殑 GUI 绫�
 */
@OnlyIn(Dist.CLIENT)
public class mio_icif_gui_induction_elc extends mio_icif_screen<InductionElcMenu> {

    private static final ResourceLocation GUI_TEXTURE =
        ResourceLocation.parse("mio_icif:textures/gui/gui_adv_induction.png");

    private static final int GUI_WIDTH = 176;
    private static final int GUI_HEIGHT = 166;

    // 标准工作进度条位置
    private static final int PROGRESS_X = 79;
    private static final int PROGRESS_Y = 35;

    private static final int ENERGY_BAR_X = 44;
    private static final int ENERGY_BAR_Y = 58;

    public mio_icif_gui_induction_elc(InductionElcMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = GUI_WIDTH;
        this.imageHeight = GUI_HEIGHT;
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;

        guiGraphics.blit(GUI_TEXTURE, x, y, 0, 0, this.imageWidth, this.imageHeight);

        InductionElcMenu menu = this.menu;

        // 使用基类标准工作进度条方法
        int progress = menu.getProgress1();
        int maxProgress = menu.getMaxProgress1();
        int progressPixels = maxProgress > 0 ? (progress * ARROW_WIDTH) / maxProgress : 0;
        drawProgressArrow(guiGraphics, x + PROGRESS_X, y + PROGRESS_Y, progressPixels);

        drawModernEnergyBar(guiGraphics, x + ENERGY_BAR_X, y + ENERGY_BAR_Y, menu.getEnergy(), menu.getMaxEnergy());
        drawXpButton(guiGraphics, x + XP_X, y + XP_Y, menu.getStoredXpTenths(), isHovering(mouseX, mouseY, x + XP_X, y + XP_Y, XP_BUTTON_W, XP_BUTTON_H));
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        super.renderLabels(guiGraphics, mouseX, mouseY);

        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;

        InductionElcMenu menu = this.menu;

        if (isHovering(mouseX, mouseY, x + PROGRESS_X, y + PROGRESS_Y, ARROW_WIDTH, ARROW_HEIGHT)) {
            int progress = menu.getProgress1();
            int maxProgress = menu.getMaxProgress1();
            guiGraphics.renderTooltip(this.font,
                Component.literal("Progress: " + progress + "/" + maxProgress),
                mouseX - x, mouseY - y);
        }

        if (isHovering(mouseX, mouseY, x + ENERGY_BAR_X, y + ENERGY_BAR_Y, KINETIC_ENERGY_BAR_WIDTH, KINETIC_ENERGY_BAR_HEIGHT)) {
            renderEnergyTooltip(guiGraphics, mouseX - x, mouseY - y, menu.getEnergy(), menu.getMaxEnergy());
        }
        if (isHovering(mouseX, mouseY, x + XP_X, y + XP_Y, XP_BUTTON_W, XP_BUTTON_H)) {
            renderXpTooltip(guiGraphics, mouseX - x, mouseY - y, menu.getStoredXpTenths());
        }
    }

    private static final int XP_X = 70, XP_Y = 16;

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int x = (this.width - this.imageWidth) / 2, y = (this.height - this.imageHeight) / 2;
        if (button == 0 && isHovering((int) mouseX, (int) mouseY, x + XP_X, y + XP_Y, XP_BUTTON_W, XP_BUTTON_H)) {
            clickXpButton();
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }
}