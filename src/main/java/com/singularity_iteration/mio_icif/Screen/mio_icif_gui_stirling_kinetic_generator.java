package com.singularity_iteration.mio_icif.Screen;

import com.singularity_iteration.mio_icif.Blocks.Environment.fluid.mio_icif_fluids;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/**
 * 斯特林动能发生机的GUI类
 */
@OnlyIn(Dist.CLIENT)
public class mio_icif_gui_stirling_kinetic_generator extends mio_icif_screen<com.singularity_iteration.mio_icif.Menu.Generator.StirlingKineticGeneratorMenu> {

    private static final ResourceLocation GUI_TEXTURE =
            ResourceLocation.parse("mio_icif:textures/gui/gui_stirling_kinetic_generator.png");

    // GUI 尺寸
    private static final int GUI_WIDTH = 176;
    private static final int GUI_HEIGHT = 166;

    // 水槽图标位置 (19, 9)，UV(12, 44) - 往上提38像素
    private static final int WATER_TANK_X = 19;
    private static final int WATER_TANK_Y = 9;
    private static final int WATER_TANK_WIDTH = 12;
    private static final int WATER_TANK_HEIGHT = 44;

    // 热水槽图标位置 (145, 9)，UV(12, 44)，颜色比水浅且更热 - 往上提38像素
    private static final int HOT_WATER_TANK_X = 145;
    private static final int HOT_WATER_TANK_Y = 9;
    private static final int HOT_WATER_TANK_WIDTH = 12;
    private static final int HOT_WATER_TANK_HEIGHT = 44;

 // 热能存图标位置 - 往上提38像素
    private static final int HEAT_ICON_X = 80;
    private static final int HEAT_ICON_Y = 7;
    private static final int HEAT_ICON_WIDTH = 16;
    private static final int HEAT_ICON_HEIGHT = 16;

    // 工作进度条位置 - 往上提38像素
    private static final int PROGRESS_X = 80;
    private static final int PROGRESS_Y = 27;
    private static final int PROGRESS_WIDTH = 16;
    private static final int PROGRESS_HEIGHT = 2;

    public mio_icif_gui_stirling_kinetic_generator(com.singularity_iteration.mio_icif.Menu.Generator.StirlingKineticGeneratorMenu menu, Inventory playerInventory, Component title) {
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

        com.singularity_iteration.mio_icif.Menu.Generator.StirlingKineticGeneratorMenu menu = this.menu;
        if (menu != null) {
            // 绘制水槽填充
            int waterAmount = menu.getWaterAmount();
            int maxWater = menu.getMaxFluidAmount();
            if (waterAmount > 0 && maxWater > 0) {
                mio_icif_GuiUtils.renderFluidBar(guiGraphics, x + WATER_TANK_X, y + WATER_TANK_Y,
                    WATER_TANK_WIDTH, WATER_TANK_HEIGHT,
                    new FluidStack(Fluids.WATER, waterAmount), maxWater);
            }

            // 绘制热水槽填充
            int hotWaterAmount = menu.getHotWaterAmount();
            int maxHotWater = menu.getMaxFluidAmount();
            if (hotWaterAmount > 0 && maxHotWater > 0) {
                mio_icif_GuiUtils.renderFluidBar(guiGraphics, x + HOT_WATER_TANK_X, y + HOT_WATER_TANK_Y,
                    HOT_WATER_TANK_WIDTH, HOT_WATER_TANK_HEIGHT,
                    new FluidStack(mio_icif_fluids.HOTWATER.get(), hotWaterAmount), maxHotWater);
            }

 // 绘制热能存
            int bufferedHeat = menu.getBufferedHeat();
            int maxBufferedHeat = 40; // 最大缓存40 HU (10次操作)
            if (bufferedHeat > 0 && maxBufferedHeat > 0) {
                int heatHeight = Math.min((bufferedHeat * HEAT_ICON_HEIGHT) / maxBufferedHeat, HEAT_ICON_HEIGHT);
                if (heatHeight > 0) {
                    int drawY = y + HEAT_ICON_Y + HEAT_ICON_HEIGHT - heatHeight;
guiGraphics.blit(ATLAS_TEXTURE, x + HEAT_ICON_X, drawY, 0, (float) 172, (float) 192 + HEAT_ICON_HEIGHT - heatHeight, HEAT_ICON_WIDTH, heatHeight, ATLAS_WIDTH, ATLAS_HEIGHT);
                }
            }

            // 绘制工作进度条
            int progress = menu.getProgress();
            int maxProgress = menu.getMaxProgress();
            if (progress > 0 && maxProgress > 0) {
                int progressWidth = (progress * PROGRESS_WIDTH) / maxProgress;
                if (progressWidth > 0) {
guiGraphics.blit(ATLAS_TEXTURE, x + PROGRESS_X, y + PROGRESS_Y, 0, (float) 198, (float) 259, progressWidth, PROGRESS_HEIGHT, ATLAS_WIDTH, ATLAS_HEIGHT);
                }
            }
        }
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;

        com.singularity_iteration.mio_icif.Menu.Generator.StirlingKineticGeneratorMenu menu = this.menu;
        if (menu == null) return;

        // 检查鼠标是否在水槽区域
        if (mouseX >= x + WATER_TANK_X && mouseX <= x + WATER_TANK_X + WATER_TANK_WIDTH &&
                mouseY >= y + WATER_TANK_Y && mouseY <= y + WATER_TANK_Y + WATER_TANK_HEIGHT) {
            guiGraphics.renderTooltip(this.font,
                    Component.literal(net.minecraft.world.level.material.Fluids.WATER.getFluidType().getDescription().getString() + ": " + menu.getWaterAmount() + "/" + menu.getMaxFluidAmount() + " mB"),
                    mouseX - x, mouseY - y);
        }

        // 检查鼠标是否在热水槽区域
        if (mouseX >= x + HOT_WATER_TANK_X && mouseX <= x + HOT_WATER_TANK_X + HOT_WATER_TANK_WIDTH &&
                mouseY >= y + HOT_WATER_TANK_Y && mouseY <= y + HOT_WATER_TANK_Y + HOT_WATER_TANK_HEIGHT) {
            guiGraphics.renderTooltip(this.font,
                    Component.literal(com.singularity_iteration.mio_icif.Blocks.Environment.fluid.mio_icif_fluids.HOTWATER.get().getFluidType().getDescription().getString() + ": " + menu.getHotWaterAmount() + "/" + menu.getMaxFluidAmount() + " mB"),
                    mouseX - x, mouseY - y);
        }

 // 检查鼠标是否在热能存区域
        if (mouseX >= x + HEAT_ICON_X && mouseX <= x + HEAT_ICON_X + HEAT_ICON_WIDTH &&
                mouseY >= y + HEAT_ICON_Y && mouseY <= y + HEAT_ICON_Y + HEAT_ICON_HEIGHT) {
            guiGraphics.renderTooltip(this.font,
                    Component.literal("Buffered Heat: " + menu.getBufferedHeat() + " HU"),
                    mouseX - x, mouseY - y);
        }
    }

    @Override
    public void containerTick() {
        super.containerTick();
        // 数据通过 broadcastChanges() 方法自动同步，不需要直接访问方块实体
    }
}