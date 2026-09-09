package com.singularity_iteration.mio_icif.Screen;

import com.singularity_iteration.mio_icif.Blocks.Environment.fluid.mio_icif_fluids;
import com.singularity_iteration.mio_icif.Menu.Generator.FluidReactorMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.neoforged.neoforge.fluids.FluidStack;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/**
 * 流体核反应堆的GUI类
 * 
 * 特点：
 * - 54个槽位（6排 x 9列）用于放置燃料棒和散热器
 * - 2个液体槽：输入槽（冷却液）和输出槽（热冷却液）
 * - 不显示能量条（流体反应堆不发电）
 * - 显示热量
 */
@OnlyIn(Dist.CLIENT)
@SuppressWarnings("null")
public class mio_icif_gui_nuclear_reactor_fluid extends mio_icif_screen<FluidReactorMenu> {

    private static final ResourceLocation GUI_TEXTURE =
        ResourceLocation.parse("mio_icif:textures/gui/gui_ncl_rct_fluid.png");
    // GUI 尺寸
    private static final int GUI_WIDTH = 212;
    private static final int GUI_HEIGHT = 243;

    // 反应堆槽位区域
    @SuppressWarnings("unused")
    private static final int REACTOR_SLOTS_X = 26;
    @SuppressWarnings("unused")
    private static final int REACTOR_SLOTS_Y = 25;
    @SuppressWarnings("unused")
    private static final int SLOT_SIZE = 18;
    
    // 液体槽背景位置(纹理坐标 215, 21, uv14,49)
    private static final int INPUT_TANK_BG_X = 9;
    private static final int INPUT_TANK_BG_Y = 53;
    private static final int OUTPUT_TANK_BG_X = 189;
    private static final int OUTPUT_TANK_BG_Y = 53;
    private static final int TANK_BG_WIDTH = 14;
    private static final int TANK_BG_HEIGHT = 49;
    @SuppressWarnings("unused")
    private static final int TANK_BG_UV_X = 215;
    @SuppressWarnings("unused")
    private static final int TANK_BG_UV_Y = 21;

    // 液体槽刻度位置
    // 输入槽刻度 纹理坐标 218, 123, uv9, 37, 位置 11, 59
    private static final int INPUT_TANK_SCALE_X = 11;
    private static final int INPUT_TANK_SCALE_Y = 59;
    private static final int TANK_SCALE_WIDTH = 9;
    private static final int TANK_SCALE_HEIGHT = 37;
    @SuppressWarnings("unused")
    private static final int INPUT_TANK_SCALE_UV_X = 218;
    @SuppressWarnings("unused")
    private static final int INPUT_TANK_SCALE_UV_Y = 123;

    // 输出槽刻度 纹理坐标 218, 81, uv9, 37, 位置 192, 59
    private static final int OUTPUT_TANK_SCALE_X = 192;
    private static final int OUTPUT_TANK_SCALE_Y = 59;
    @SuppressWarnings("unused")
    private static final int OUTPUT_TANK_SCALE_UV_X = 218;
    @SuppressWarnings("unused")
    private static final int OUTPUT_TANK_SCALE_UV_Y = 81;

    // 液体槽物品槽位置（往右下移动一格）
    // 输入液体: 输出槽在 8, 115, 输入槽在 8, 25
    /* private static final int INPUT_TANK_OUTPUT_SLOT_X = 8; */
    /* private static final int INPUT_TANK_OUTPUT_SLOT_Y = 115; */
    /* private static final int INPUT_TANK_INPUT_SLOT_X = 8; */
    /* private static final int INPUT_TANK_INPUT_SLOT_Y = 25; */

    // 输出液体: 输出槽在 188, 115, 输入槽在 188, 25
    /* private static final int OUTPUT_TANK_OUTPUT_SLOT_X = 188; */
    /* private static final int OUTPUT_TANK_OUTPUT_SLOT_Y = 115; */
    /* private static final int OUTPUT_TANK_INPUT_SLOT_X = 188; */
    /* private static final int OUTPUT_TANK_INPUT_SLOT_Y = 25; */

    // 液体渲染位置 (在刻度下面)
    // 输入液体: 位置 10, 54, uv12, 47
    private static final int INPUT_FLUID_X = 10;
    private static final int INPUT_FLUID_Y = 54;
    @SuppressWarnings("unused")
    private static final int INPUT_FLUID_UV_X = 12;
    @SuppressWarnings("unused")
    private static final int INPUT_FLUID_UV_Y = 47;
    private static final int FLUID_WIDTH = 12;
    private static final int FLUID_HEIGHT = 47;

    // 输出液体: 位置 192, 54, uv12, 47
    private static final int OUTPUT_FLUID_X = 192;
    private static final int OUTPUT_FLUID_Y = 54;
    @SuppressWarnings("unused")
    private static final int OUTPUT_FLUID_UV_X = 12;
    @SuppressWarnings("unused")
    private static final int OUTPUT_FLUID_UV_Y = 47;

    // 热量条位置
    private static final int HEAT_BAR_X = 188;
    private static final int HEAT_BAR_Y = 145;
    private static final int HEAT_BAR_WIDTH = 16;
    private static final int HEAT_BAR_HEIGHT = 54;

    public mio_icif_gui_nuclear_reactor_fluid(FluidReactorMenu menu, Inventory playerInventory, Component title) {
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

        // 绘制输入液体槽背景(纹理 215, 21, 14x49)
        guiGraphics.blit(ATLAS_TEXTURE, x + INPUT_TANK_BG_X, y + INPUT_TANK_BG_Y, 0, (float) 162, (float) 2, TANK_BG_WIDTH, TANK_BG_HEIGHT, ATLAS_WIDTH, ATLAS_HEIGHT);

        // 绘制输出液体槽背景
        guiGraphics.blit(ATLAS_TEXTURE, x + OUTPUT_TANK_BG_X, y + OUTPUT_TANK_BG_Y, 0, (float) 178, (float) 2, TANK_BG_WIDTH, TANK_BG_HEIGHT, ATLAS_WIDTH, ATLAS_HEIGHT);

        // 绘制输入液体槽刻度(纹理 218, 123, 9x37)
        guiGraphics.blit(ATLAS_TEXTURE, x + INPUT_TANK_SCALE_X, y + INPUT_TANK_SCALE_Y, 0, (float) 44, (float) 125, TANK_SCALE_WIDTH, TANK_SCALE_HEIGHT, ATLAS_WIDTH, ATLAS_HEIGHT);

        // 绘制输出液体槽刻度(纹理 218, 81, 9x37)
        guiGraphics.blit(ATLAS_TEXTURE, x + OUTPUT_TANK_SCALE_X, y + OUTPUT_TANK_SCALE_Y, 0, (float) 55, (float) 125, TANK_SCALE_WIDTH, TANK_SCALE_HEIGHT, ATLAS_WIDTH, ATLAS_HEIGHT);

        // 绘制液体 (在刻度下面) 和热量条
        com.singularity_iteration.mio_icif.Menu.Generator.FluidReactorMenu menu = this.menu;
        if (menu != null) {
            // 绘制输入液体 - 冷却液
            int inputFluidAmount = menu.getInputFluidAmount();
            int inputCapacity = menu.getInputFluidCapacity();
            if (inputFluidAmount > 0 && inputCapacity > 0) {
                mio_icif_GuiUtils.renderFluidBar(guiGraphics, x + INPUT_FLUID_X, y + INPUT_FLUID_Y,
                    FLUID_WIDTH, FLUID_HEIGHT,
                    new FluidStack(mio_icif_fluids.COOLANT.get(), inputFluidAmount), inputCapacity);
            }

            // 绘制输出液体 - 热冷却液
            int outputFluidAmount = menu.getOutputFluidAmount();
            int outputCapacity = menu.getOutputFluidCapacity();
            if (outputFluidAmount > 0 && outputCapacity > 0) {
                mio_icif_GuiUtils.renderFluidBar(guiGraphics, x + OUTPUT_FLUID_X, y + OUTPUT_FLUID_Y,
                    FLUID_WIDTH, FLUID_HEIGHT,
                    new FluidStack(mio_icif_fluids.HOTCOOLANT.get(), outputFluidAmount), outputCapacity);
            }

            // 绘制热量条
            int heatProgress = menu.getHeatProgressPixels();
            if (heatProgress > 0) {
                // 热量条从下往上填充
                int heatBarFillHeight = (int) ((float) heatProgress / 100 * HEAT_BAR_HEIGHT);
                guiGraphics.blit(ATLAS_TEXTURE, x + HEAT_BAR_X, y + HEAT_BAR_Y + (HEAT_BAR_HEIGHT - heatBarFillHeight), 0, (float) 126, (float) 2 + (HEAT_BAR_HEIGHT - heatBarFillHeight), HEAT_BAR_WIDTH, heatBarFillHeight, ATLAS_WIDTH, ATLAS_HEIGHT);
            }
        }
    }

    // 信息文本显示位置 - 与核反应堆发电模式相同
    private static final int INFO_TEXT_X = 7;
    private static final int INFO_TEXT_Y = 138;

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        super.renderLabels(guiGraphics, mouseX, mouseY);

        com.singularity_iteration.mio_icif.Menu.Generator.FluidReactorMenu menu = this.menu;
        if (menu == null) return;

        // 绘制标题
        guiGraphics.drawString(this.font, this.title, this.titleLabelX, this.titleLabelY, 0x404040, false);

        // 绘制玩家物品栏标签
        guiGraphics.drawString(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, 0x404040, false);

        // 绘制热量和发热效率信息(与核反应堆发电模式相同位置)
        int heat = menu.getCurrentHeat();
        int maxHeat = menu.getMaxHeat();
        int heatOutput = menu.getHeatOutput();

        // 使用翻译键
        String heatText = Component.translatable("gui.mio_icif.nuclear_reactor.heat", heat, maxHeat).getString();
        String outputText = Component.translatable("gui.mio_icif.reactor.heat_output", heatOutput).getString();
        String infoText = heatText + "    " + outputText;

        // 绘制绿色文本 (0x00FF00 是绿色)
        guiGraphics.drawString(this.font, infoText, INFO_TEXT_X, INFO_TEXT_Y, 0x00FF00, false);
    }
}