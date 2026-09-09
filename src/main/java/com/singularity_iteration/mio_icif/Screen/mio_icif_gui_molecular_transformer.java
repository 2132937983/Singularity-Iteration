package com.singularity_iteration.mio_icif.Screen;

import com.singularity_iteration.mio_icif.Menu.Producer.MolecularTransformerMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class mio_icif_gui_molecular_transformer extends mio_icif_screen<MolecularTransformerMenu> {

    private static final ResourceLocation GUI_TEXTURE =
        ResourceLocation.parse("mio_icif:textures/gui/gui_molecular_transformer.png");

    private static final int GUI_WIDTH = 176;
    private static final int GUI_HEIGHT = 166;

    // 标准工作进度条位置
    private static final int PROGRESS_X = 79;
    private static final int PROGRESS_Y = 35;

    // 动能发电机能量条位置
    private static final int ENERGY_BAR_X = 44;
    private static final int ENERGY_BAR_Y = 58;

    public mio_icif_gui_molecular_transformer(MolecularTransformerMenu menu, Inventory playerInventory, Component title) {
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

        MolecularTransformerMenu menu = this.menu;
        if (menu != null) {
            // 使用标准工作进度条渲染（箭头样式）
            drawProgressArrow(guiGraphics, x + PROGRESS_X, y + PROGRESS_Y, menu.getProgressPixels(ARROW_WIDTH));

            // 使用动能发电机专属EU能量条表示能量（带背景）
            int energyPixels = (int) ((long) menu.getEnergy() * KINETIC_ENERGY_BAR2_WIDTH / Math.max(1, menu.getMaxEnergy()));
            drawKineticEnergyBar(guiGraphics, x + ENERGY_BAR_X, y + ENERGY_BAR_Y, energyPixels);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        super.renderLabels(guiGraphics, mouseX, mouseY);

        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;

        MolecularTransformerMenu menu = this.menu;
        if (menu == null) return;

        // 进度条工具提示
        if (isHovering(mouseX, mouseY, x + PROGRESS_X, y + PROGRESS_Y, ARROW_WIDTH, ARROW_HEIGHT)) {
            renderProgressTooltip(guiGraphics, mouseX - x, mouseY - y, menu.getProgress(), menu.getMaxProgress());
        }

        // 能量条工具提示（使用动能发电机能量条尺寸）
        if (isHovering(mouseX, mouseY, x + ENERGY_BAR_X, y + ENERGY_BAR_Y, KINETIC_ENERGY_BAR_WIDTH, KINETIC_ENERGY_BAR_HEIGHT)) {
            renderEnergyTooltip(guiGraphics, mouseX - x, mouseY - y, menu.getEnergy(), menu.getMaxEnergy());
        }
    }


}