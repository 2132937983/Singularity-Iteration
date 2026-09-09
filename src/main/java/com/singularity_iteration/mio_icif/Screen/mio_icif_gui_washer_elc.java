package com.singularity_iteration.mio_icif.Screen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.fluids.FluidStack;

/**
 * 洗矿机 GUI 类
 * 使用标准闪电标志和流体渲染
 */
@OnlyIn(Dist.CLIENT)
@SuppressWarnings("null")
public class mio_icif_gui_washer_elc extends mio_icif_screen<com.singularity_iteration.mio_icif.Menu.Producer.WasherElcMenu> {

    private static final ResourceLocation GUI_TEXTURE =
        ResourceLocation.parse("mio_icif:textures/gui/gui_washer_elc.png");

    // GUI 尺寸
    private static final int GUI_WIDTH = 176;
    private static final int GUI_HEIGHT = 166;

    // 进度条位置
    private static final int PROGRESS_X = 102;
    private static final int PROGRESS_Y = 38;
    private static final int PROGRESS_WIDTH = 20;
    private static final int PROGRESS_HEIGHT = 19;

    // 进度条纹理位置
    private static final int PROGRESS_TEXTURE_X = 34;
    private static final int PROGRESS_TEXTURE_Y = 164;

    // 标准闪电标志位置
    private static final int LIGHTNING_X = 9;
    private static final int LIGHTNING_Y = 43;

    // 流体槽位置
    private static final int FLUID_TANK_X = 64;
    private static final int FLUID_TANK_Y = 24;
    private static final int FLUID_TANK_WIDTH = 12;
    private static final int FLUID_TANK_HEIGHT = 47;

    public mio_icif_gui_washer_elc(com.singularity_iteration.mio_icif.Menu.Producer.WasherElcMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = GUI_WIDTH;
        this.imageHeight = GUI_HEIGHT;
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;

        // 缁樺?惰?屾櫙
        guiGraphics.blit(GUI_TEXTURE, x, y, 0, 0, this.imageWidth, this.imageHeight);

        com.singularity_iteration.mio_icif.Menu.Producer.WasherElcMenu menu = this.getMenu();
        if (menu != null) {
            // 使用基类方法绘制标准闪电标志
            drawLightningEnergy(guiGraphics, x + LIGHTNING_X, y + LIGHTNING_Y, menu.getEnergy(), menu.getMaxEnergy());

            // 绘制流体（水）
            int fluidAmount = menu.getFluidAmount();
            int fluidCapacity = menu.getFluidCapacity();
            if (fluidAmount > 0 && fluidCapacity > 0) {
                mio_icif_GuiUtils.renderFluidBar(guiGraphics, x + FLUID_TANK_X, y + FLUID_TANK_Y,
                    FLUID_TANK_WIDTH, FLUID_TANK_HEIGHT,
                    new FluidStack(Fluids.WATER, fluidAmount), fluidCapacity);
            }

            // 绘制进度条
            int progressPixels = menu.getProgressPixels(PROGRESS_WIDTH);
            if (progressPixels > 0) {
                guiGraphics.blit(ATLAS_TEXTURE, x + PROGRESS_X, y + PROGRESS_Y, 0, 
                    (float) PROGRESS_TEXTURE_X, (float) PROGRESS_TEXTURE_Y, 
                    progressPixels, PROGRESS_HEIGHT, ATLAS_WIDTH, ATLAS_HEIGHT);
            }
        }
    }

    @Override
    protected void renderTooltip(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        super.renderTooltip(guiGraphics, mouseX, mouseY);

        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;

        com.singularity_iteration.mio_icif.Menu.Producer.WasherElcMenu menu = this.getMenu();
        if (menu == null) return;

        // 检测鼠标是否在进度条区域
        if (mouseX >= x + PROGRESS_X && mouseX <= x + PROGRESS_X + PROGRESS_WIDTH &&
            mouseY >= y + PROGRESS_Y && mouseY <= y + PROGRESS_Y + PROGRESS_HEIGHT) {
            int progress = menu.getProgress();
            int maxProgress = menu.getMaxProgress();
            guiGraphics.renderTooltip(this.font,
                Component.literal(progress + "/" + maxProgress),
                mouseX, mouseY);
        }

        // 检测鼠标是否在闪电标志区域
        if (mouseX >= x + LIGHTNING_X && mouseX <= x + LIGHTNING_X + LIGHTNING_WIDTH &&
            mouseY >= y + LIGHTNING_Y && mouseY <= y + LIGHTNING_Y + LIGHTNING_HEIGHT) {
            guiGraphics.renderTooltip(this.font,
                Component.literal("Energy: " + menu.getEnergy() + "/" + menu.getMaxEnergy() + " EU"),
                mouseX, mouseY);
        }

        // 检测鼠标是否在流体槽区域
        if (mouseX >= x + FLUID_TANK_X && mouseX <= x + FLUID_TANK_X + FLUID_TANK_WIDTH &&
            mouseY >= y + FLUID_TANK_Y && mouseY <= y + FLUID_TANK_Y + FLUID_TANK_HEIGHT) {
            guiGraphics.renderTooltip(this.font,
                Component.literal(net.minecraft.world.level.material.Fluids.WATER.getFluidType().getDescription().getString() + ": " + menu.getFluidAmount() + "/" + menu.getFluidCapacity() + " mB"),
                mouseX, mouseY);
        }
    }

    @Override
    public void containerTick() {
        super.containerTick();
        // 数据同步通过 broadcastChanges() 方法，不需要直接访问方块实体
    }
}