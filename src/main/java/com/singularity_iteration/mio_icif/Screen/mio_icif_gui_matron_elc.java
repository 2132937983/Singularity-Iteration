package com.singularity_iteration.mio_icif.Screen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/**
 * 作物监管机方块的 GUI 类
 */
@OnlyIn(Dist.CLIENT)
@SuppressWarnings("null")
public class mio_icif_gui_matron_elc extends mio_icif_screen<com.singularity_iteration.mio_icif.Menu.Producer.MatronElcMenu> {

    private static final ResourceLocation GUI_TEXTURE =
        ResourceLocation.parse("mio_icif:textures/gui/gui_matron_elc.png");

    // GUI 尺寸
    private static final int GUI_WIDTH = 176;
    private static final int GUI_HEIGHT = 192;

    // 液体渲染位置
    private static final int FLUID_X = 64;
    private static final int FLUID_Y = 76;
    private static final int FLUID_WIDTH = 47;
    private static final int FLUID_HEIGHT = 24;

    // 闪电标志位置
    private static final int LIGHTNING_X = 153;
    private static final int LIGHTNING_Y = 81;

    public mio_icif_gui_matron_elc(com.singularity_iteration.mio_icif.Menu.Producer.MatronElcMenu menu, Inventory playerInventory, Component title) {
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
        guiGraphics.blit(GUI_TEXTURE, x, y, 0, 0, this.imageWidth, this.imageHeight);

        com.singularity_iteration.mio_icif.Menu.Producer.MatronElcMenu menu = this.menu;
        if (menu != null) {
            // 绘制液体（根据液体含量从左往右填充）
            int fluidAmount = menu.getFluidAmount();
            int maxFluid = menu.getMaxFluid();
            if (fluidAmount > 0 && maxFluid > 0) {
                int fluidWidth = (fluidAmount * FLUID_WIDTH) / maxFluid;
                if (fluidWidth > 0) {
                    // 蓝色表示水
                    guiGraphics.fill(x + FLUID_X, y + FLUID_Y,
                            x + FLUID_X + fluidWidth, y + FLUID_Y + FLUID_HEIGHT, 0xFF0080FF);
                }
            }

            // 使用基类标准闪电标志渲染
            drawLightningEnergy(guiGraphics, x + LIGHTNING_X, y + LIGHTNING_Y, menu.getEnergy(), menu.getMaxEnergy());
        }
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        super.renderLabels(guiGraphics, mouseX, mouseY);

        com.singularity_iteration.mio_icif.Menu.Producer.MatronElcMenu menu = this.menu;
        if (menu == null) return;

        int guiLeft = (this.width - this.imageWidth) / 2;
        int guiTop = (this.height - this.imageHeight) / 2;
        int relativeMouseX = mouseX - guiLeft;
        int relativeMouseY = mouseY - guiTop;

        // 检查鼠标是否在液体区域
        if (relativeMouseX >= FLUID_X && relativeMouseX < FLUID_X + FLUID_WIDTH &&
            relativeMouseY >= FLUID_Y && relativeMouseY < FLUID_Y + FLUID_HEIGHT) {
            Component tooltip = Component.translatable("gui.mio_icif.matron.fluid_tooltip", menu.getFluidAmount(), menu.getMaxFluid());
            guiGraphics.renderTooltip(this.font, tooltip, relativeMouseX, relativeMouseY);
        }

        // 检查鼠标是否在闪电标志区域
        if (isHovering(mouseX, mouseY, guiLeft + LIGHTNING_X, guiTop + LIGHTNING_Y, LIGHTNING_WIDTH, LIGHTNING_HEIGHT)) {
            renderEnergyTooltip(guiGraphics, relativeMouseX, relativeMouseY, menu.getEnergy(), menu.getMaxEnergy());
        }
    }

    @Override
    public void containerTick() {
        super.containerTick();
        // 数据通过 ContainerData 自动同步，无需手动同步
    }
}