package com.singularity_iteration.mio_icif.Screen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import com.singularity_iteration.mio_icif.Menu.HUEntity.FluidHeatGeneratorMenu;
import net.neoforged.neoforge.fluids.FluidStack;
import com.singularity_iteration.mio_icif.Blocks.Environment.fluid.mio_icif_fluids;

/**
 * 流体加热机的 GUI类
 */
@OnlyIn(Dist.CLIENT)
@SuppressWarnings("null")
public class mio_icif_gui_fluid_heat_generator extends mio_icif_screen<com.singularity_iteration.mio_icif.Menu.HUEntity.FluidHeatGeneratorMenu> {

    private static final ResourceLocation GUI_TEXTURE =
        ResourceLocation.parse("mio_icif:textures/gui/gui_fluid_heat_generator.png");

    // GUI 尺寸
    private static final int GUI_WIDTH = 176;
    private static final int GUI_HEIGHT = 166;

    // 槽位位置
    /* private static final int FUEL_BUCKET_SLOT_X = 27; */
    /* private static final int FUEL_BUCKET_SLOT_Y = 21; */
    /* private static final int EMPTY_BUCKET_SLOT_X = 27; */
    /* private static final int EMPTY_BUCKET_SLOT_Y = 54; */

    // 燃料槽位置（相对背景）
    private static final int FUEL_TANK_X = 70;
    private static final int FUEL_TANK_Y = 20;

    // 火焰标志位置
    private static final int FLAME_ICON_X = 81;
    private static final int FLAME_ICON_Y = 29;
    private static final int FLAME_ICON_WIDTH = 14;
    private static final int FLAME_ICON_HEIGHT = 14;

    // 火焰标志纹理位置 (176, 0)，uv(14, 14)
    @SuppressWarnings("unused")
    private static final int FLAME_ICON_TEXTURE_X = 176;
    @SuppressWarnings("unused")
    private static final int FLAME_ICON_TEXTURE_Y = 0;

    // 发热效率文本位置
    private static final int HEAT_RATE_TEXT_X = 95;
    private static final int HEAT_RATE_TEXT_Y = 32;
    // 文本颜色 (57c4da)
    private static final int TEXT_COLOR = 0x57c4da;

    public mio_icif_gui_fluid_heat_generator(FluidHeatGeneratorMenu menu, Inventory playerInventory, Component title) {
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

        FluidHeatGeneratorMenu menu = this.getMenu();
        if (menu != null) {
            // 绘制标准流体槽（背景+流体+刻度）
            int fuelAmount = menu.getFuelAmount();
            int fuelCapacity = menu.getFuelCapacity();
            drawFluidTank(guiGraphics, x + FUEL_TANK_X, y + FUEL_TANK_Y,
                new FluidStack(mio_icif_fluids.BIOGAS.get(), fuelAmount), fuelAmount, fuelCapacity);

            // 绘制火焰标志（根据燃烧进度从上往下消失）
            int burnProgress = menu.getBurnProgress();
            // burnProgress: 0-100, 0表示刚开始燃烧(完整显示), 100表示燃烧结束(完全消失)
            // 从上往下消失，进度越小，从顶部显示的部分越多
            if (burnProgress < 100) {
                int visibleHeight = ((100 - burnProgress) * FLAME_ICON_HEIGHT) / 100;
                if (visibleHeight > 0) {
                    // 从顶部开始消失，所以绘制的起始Y坐标要向下偏移
                    int drawY = y + FLAME_ICON_Y + (FLAME_ICON_HEIGHT - visibleHeight);
                    // int textureY = FLAME_ICON_TEXTURE_Y + (FLAME_ICON_HEIGHT - visibleHeight);
                    // 绘制剩余可见的部分（从下往上显示）
                    guiGraphics.blit(ATLAS_TEXTURE, x + FLAME_ICON_X, drawY, 0, (float) 152, (float) 227 + FLAME_ICON_HEIGHT - visibleHeight, FLAME_ICON_WIDTH, visibleHeight, ATLAS_WIDTH, ATLAS_HEIGHT);
                }
            }

            // 绘制发热效率文本
            int heatRate = menu.isWorking() ? 32 : 0;
            String heatRateText = heatRate + " HU/t";
            guiGraphics.drawString(this.font, heatRateText, x + HEAT_RATE_TEXT_X, y + HEAT_RATE_TEXT_Y, TEXT_COLOR);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        super.renderLabels(guiGraphics, mouseX, mouseY);

        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;

        FluidHeatGeneratorMenu menu = this.getMenu();
        if (menu == null) return;

        // 检查鼠标是否在燃料槽区域
        if (mouseX >= x + FUEL_TANK_X && mouseX <= x + FUEL_TANK_X + FLUID_TANK_BG_WIDTH &&
            mouseY >= y + FUEL_TANK_Y && mouseY <= y + FUEL_TANK_Y + FLUID_TANK_BG_HEIGHT) {
            guiGraphics.renderTooltip(this.font,
                Component.literal((menu.getFuelAmount() > 0 ? com.singularity_iteration.mio_icif.Blocks.Environment.fluid.mio_icif_fluids.BIOGAS.get().getFluidType().getDescription().getString() + ": " : "") + menu.getFuelAmount() + "/" + menu.getFuelCapacity() + " mB"),
                mouseX - x, mouseY - y);
        }
    }

    @Override
    public void containerTick() {
        super.containerTick();
        // 数据通过 broadcastChanges() 方法同步
    }


}