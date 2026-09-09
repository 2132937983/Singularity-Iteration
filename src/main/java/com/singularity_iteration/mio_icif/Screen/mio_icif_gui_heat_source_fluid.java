package com.singularity_iteration.mio_icif.Screen;

import com.singularity_iteration.mio_icif.Menu.HUEntity.HeatSourceFluidMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.fluids.FluidStack;

/**
 * 热交换机的GUI类
 */
@OnlyIn(Dist.CLIENT)
@SuppressWarnings("null")
public class mio_icif_gui_heat_source_fluid extends mio_icif_screen<HeatSourceFluidMenu> {

    private static final ResourceLocation GUI_TEXTURE =
        ResourceLocation.parse("mio_icif:textures/gui/gui_heat_source_fluid.png");

    // GUI 尺寸 - 纹理高度为204
    private static final int GUI_WIDTH = 176;
    private static final int GUI_HEIGHT = 204;

    // 槽位位置 - 输入液体桶(输入桶8,103，输出槽紧挨右边26,103)
    /* private static final int INPUT_BUCKET_SLOT_X = 8; */
    /* private static final int INPUT_BUCKET_SLOT_Y = 103; */
    /* private static final int INPUT_EMPTY_SLOT_X = 26; */
    /* private static final int INPUT_EMPTY_SLOT_Y = 103; */

    // 槽位位置 - 输出液体桶(输入桶134,103，输出槽紧挨右边152,103)
    /* private static final int OUTPUT_BUCKET_SLOT_X = 134; */
    /* private static final int OUTPUT_BUCKET_SLOT_Y = 103; */
    /* private static final int OUTPUT_FULL_SLOT_X = 152; */
    /* private static final int OUTPUT_FULL_SLOT_Y = 103; */

    // 热交换器槽位区域(2排5列) - 第一排第一个46,50，第二排第一个46,72
    /* private static final int HEAT_CONDUCTOR_START_X = 46; */
    /* private static final int HEAT_CONDUCTOR_START_Y = 50; */
    /* private static final int HEAT_CONDUCTOR_COLS = 5; */
    /* private static final int HEAT_CONDUCTOR_ROWS = 2; */
    /* private static final int SLOT_SPACING_X = 17; */
    /* private static final int SLOT_SPACING_Y = 22; */

    // 额外物品槽位区域(3个槽紧挨在一起，第一个在62,103)
    /* private static final int EXTRA_SLOT_START_X = 62; */
    /* private static final int EXTRA_SLOT_Y = 103; */
    /* private static final int EXTRA_SLOT_COUNT = 3; */
    /* private static final int EXTRA_SLOT_SPACING = 18; */

    // 输入流体槽位置
    private static final int INPUT_TANK_X = 15;
    private static final int INPUT_TANK_Y = 41;

    // 输出流体槽位置
    private static final int OUTPUT_TANK_X = 141;
    private static final int OUTPUT_TANK_Y = 41;

    // 热能条位置(50, 28)，UV (79, 13)，宽79
    private static final int HEAT_BAR_X = 50;
    private static final int HEAT_BAR_Y = 28;
    private static final int HEAT_BAR_WIDTH = 79;
    private static final int HEAT_BAR_HEIGHT = 13;
    /* private static final int HEAT_BAR_UV_X = 79; */
    /* private static final int HEAT_BAR_UV_Y = 13; */

    public mio_icif_gui_heat_source_fluid(HeatSourceFluidMenu menu, Inventory playerInventory, Component title) {
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

        HeatSourceFluidMenu menu = this.menu;

        // 绘制输入流体槽
        int inputAmount = menu.getInputFluidAmount();
        int inputCapacity = menu.getInputFluidCapacity();
        FluidStack inputFluid = menu.getInputFluid();
        drawFluidTank(guiGraphics, x + INPUT_TANK_X, y + INPUT_TANK_Y, inputFluid, inputAmount, inputCapacity);

        // 绘制输出流体槽
        int outputAmount = menu.getOutputFluidAmount();
        int outputCapacity = menu.getOutputFluidCapacity();
        FluidStack outputFluid = menu.getOutputFluid();
        drawFluidTank(guiGraphics, x + OUTPUT_TANK_X, y + OUTPUT_TANK_Y, outputFluid, outputAmount, outputCapacity);

        // 绘制热能条
        renderHeatBar(guiGraphics, x + HEAT_BAR_X, y + HEAT_BAR_Y,
            HEAT_BAR_WIDTH, HEAT_BAR_HEIGHT, menu.getHeatStored(), menu.getHeatCapacity());
    }

    private void renderHeatBar(GuiGraphics guiGraphics, int x, int y, int width, int height, int stored, int capacity) {
        if (capacity <= 0) {
            return;
        }

        // 从左往右填充，只有满时宽度才为79
        int heatWidth = (stored * width) / capacity;
        if (heatWidth > 0) {
            // 根据热能水平改变颜色
            int color = stored > capacity * 0.8 ? 0xFFFF0000 : // 红色（危险）
                       stored > capacity * 0.5 ? 0xFFFF8800 : // 橙色
                       0xFFFFAA00; // 黄色
            guiGraphics.fill(x, y, x + heatWidth, y + height, color);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        // 不调用super.renderLabels() 以避免渲染物品栏名称文本
        // 只渲染标题
        guiGraphics.drawString(this.font, this.title, this.titleLabelX, this.titleLabelY, 0x404040, false);

        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;

        HeatSourceFluidMenu menu = this.menu;

        // 检查鼠标是否在输入流体槽区域
        if (mouseX >= x + INPUT_TANK_X && mouseX <= x + INPUT_TANK_X + FLUID_TANK_BG_WIDTH &&
                mouseY >= y + INPUT_TANK_Y && mouseY <= y + INPUT_TANK_Y + FLUID_TANK_BG_HEIGHT) {
            guiGraphics.renderTooltip(this.font,
                Component.literal((!menu.getInputFluid().isEmpty() ? menu.getInputFluid().getFluid().getFluidType().getDescription().getString() + ": " : "") + menu.getInputFluidAmount() + "/" + menu.getInputFluidCapacity() + " mB"),
                mouseX - x, mouseY - y);
        }

        // 检查鼠标是否在输出流体槽区域
        if (mouseX >= x + OUTPUT_TANK_X && mouseX <= x + OUTPUT_TANK_X + FLUID_TANK_BG_WIDTH &&
                mouseY >= y + OUTPUT_TANK_Y && mouseY <= y + OUTPUT_TANK_Y + FLUID_TANK_BG_HEIGHT) {
            guiGraphics.renderTooltip(this.font,
                Component.literal((!menu.getOutputFluid().isEmpty() ? menu.getOutputFluid().getFluid().getFluidType().getDescription().getString() + ": " : "") + menu.getOutputFluidAmount() + "/" + menu.getOutputFluidCapacity() + " mB"),
                mouseX - x, mouseY - y);
        }

        // 检查鼠标是否在热能条区域
        if (mouseX >= x + HEAT_BAR_X && mouseX <= x + HEAT_BAR_X + HEAT_BAR_WIDTH &&
            mouseY >= y + HEAT_BAR_Y && mouseY <= y + HEAT_BAR_Y + HEAT_BAR_HEIGHT) {
            guiGraphics.renderTooltip(this.font,
                Component.literal("Heat: " + menu.getHeatStored() + "/" + menu.getHeatCapacity() + " HU"),
                mouseX - x, mouseY - y);
        }

        // 显示当前输出功率 - 放在热度条中间
        String heatText = menu.getCurrentHeatOutput() + " HU/t";
        int textWidth = this.font.width(heatText);
        int textX = HEAT_BAR_X + (HEAT_BAR_WIDTH - textWidth) / 2;
        int textY = HEAT_BAR_Y + (HEAT_BAR_HEIGHT - 8) / 2; // 8是字体高度
        guiGraphics.drawString(this.font, Component.literal(heatText), textX, textY, 0xFFFFFF, false);
    }
}