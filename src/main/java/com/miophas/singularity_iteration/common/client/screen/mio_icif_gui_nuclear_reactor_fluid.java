package com.miophas.singularity_iteration.common.client.screen;

import com.miophas.singularity_iteration.common.block.environment.fluid.mio_icif_fluids;
import com.miophas.singularity_iteration.common.menu.generator.FluidReactorMenu;
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
    
    // 液体槽背景位置(纹理坐标 215, 21, uv14,49)
    private static final int INPUT_TANK_BG_X = 9;
    private static final int INPUT_TANK_BG_Y = 53;
    private static final int OUTPUT_TANK_BG_X = 189;
    private static final int OUTPUT_TANK_BG_Y = 53;
    private static final int TANK_BG_WIDTH = 14;
    private static final int TANK_BG_HEIGHT = 49;

    // 液体槽刻度位置
    // 输入槽刻度 纹理坐标 218, 123, uv9, 37, 位置 11, 59
    private static final int INPUT_TANK_SCALE_X = 11;
    private static final int INPUT_TANK_SCALE_Y = 59;
    private static final int TANK_SCALE_WIDTH = 9;
    private static final int TANK_SCALE_HEIGHT = 37;

    // 输出槽刻度 纹理坐标 218, 81, uv9, 37, 位置 192, 59
    private static final int OUTPUT_TANK_SCALE_X = 192;
    private static final int OUTPUT_TANK_SCALE_Y = 59;

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
    private static final int FLUID_WIDTH = 12;
    private static final int FLUID_HEIGHT = 47;

    // 输出液体: 位置 190, 54, uv12, 47（0.1.10.6：原为 192，越出右侧槽框 2 像素；
    // 槽框内腔为 190..201，与输入槽 10..21 对称。刻度 192 本就右对齐，不变）
    private static final int OUTPUT_FLUID_X = 190;
    private static final int OUTPUT_FLUID_Y = 54;

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
        com.miophas.singularity_iteration.common.menu.generator.FluidReactorMenu menu = this.menu;
        if (menu != null) {
            // 绘制输入液体 - 冷媒（冷却液 / 水）
            int inputFluidAmount = menu.getInputFluidAmount();
            int inputCapacity = menu.getInputFluidCapacity();
            if (inputFluidAmount > 0 && inputCapacity > 0) {
                mio_icif_GuiUtils.renderFluidBar(guiGraphics, x + INPUT_FLUID_X, y + INPUT_FLUID_Y,
                    FLUID_WIDTH, FLUID_HEIGHT,
                    new FluidStack(coolantFluid(menu.getInputCoolantKind(), true), inputFluidAmount), inputCapacity);
            }

            // 绘制输出液体 - 热媒（热冷却液 / 热水）
            int outputFluidAmount = menu.getOutputFluidAmount();
            int outputCapacity = menu.getOutputFluidCapacity();
            if (outputFluidAmount > 0 && outputCapacity > 0) {
                mio_icif_GuiUtils.renderFluidBar(guiGraphics, x + OUTPUT_FLUID_X, y + OUTPUT_FLUID_Y,
                    FLUID_WIDTH, FLUID_HEIGHT,
                    new FluidStack(coolantFluid(menu.getOutputCoolantKind(), false), outputFluidAmount), outputCapacity);
            }
            // 0.1.10.6：移除旧热量条（188,145）。该位置没有槽框，图集区域几乎透明，只会在
            // 物品栏右侧画出一条灰线；热量改由下方信息面板的文字颜色表示。
        }
    }

    // 信息文本显示位置 - 与核反应堆发电模式相同
    private static final int INFO_TEXT_X = 9;
    private static final int INFO_TEXT_Y = 138;
    // 纹理中的绿色网格信息面板：外框 6..205 × 136..147，内腔 7..204 × 137..146
    private static final int PANEL_X0 = 7, PANEL_Y0 = 137, PANEL_X1 = 205, PANEL_Y1 = 147;
    private static final int PANEL_BACKING = 0xD00B140D;
    private static final int OUTPUT_INK = 0xFF8FE9FF;

    /** 按冷媒下标取冷/热流体（0 = 冷却液，1 = 水；其余按冷却液处理）。 */
    private static net.minecraft.world.level.material.Fluid coolantFluid(int kind, boolean cold) {
        return switch (kind) {
            case 1 -> cold ? net.minecraft.world.level.material.Fluids.WATER : mio_icif_fluids.HOTWATER.get();
            default -> cold ? mio_icif_fluids.COOLANT.get() : mio_icif_fluids.HOTCOOLANT.get();
        };
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        super.renderLabels(guiGraphics, mouseX, mouseY);

        com.miophas.singularity_iteration.common.menu.generator.FluidReactorMenu menu = this.menu;
        if (menu == null) return;

        // 绘制标题
        drawTitle(guiGraphics);

        // 绘制玩家物品栏标签
        guiGraphics.drawString(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, 0x404040, false);

        // 绘制热量和发热效率信息(与核反应堆发电模式相同位置)
        int heat = menu.getCurrentHeat();
        int maxHeat = menu.getMaxHeat();
        int heatOutput = menu.getHeatOutput();

        // 0.1.10.6：面板底色是深绿色网格，旧的深灰文字（0x2A2E33）几乎看不清。
        // 先在面板内腔铺一层深色半透明底，再用带阴影的亮色文字：热量在左（按热量比例由绿变黄变红），
        // 发热功率在右（青色）。文字过长时自动缩小，不会越出面板。
        float heatFraction = maxHeat > 0 ? Math.max(0F, Math.min(1F, heat / (float) maxHeat)) : 0F;
        guiGraphics.fill(PANEL_X0, PANEL_Y0, PANEL_X1, PANEL_Y1, PANEL_BACKING);
        Component outputLine = Component.translatable("gui.mio_icif.reactor.heat_output", heatOutput);
        int outputMax = (PANEL_X1 - PANEL_X0) / 2 - 4;
        int outputWidth = Math.min(this.font.width(outputLine), outputMax);
        drawFittedRight(guiGraphics, this.font, outputLine, PANEL_X1 - 2, INFO_TEXT_Y, outputMax, OUTPUT_INK, true);
        drawFitted(guiGraphics, this.font, Component.translatable("gui.mio_icif.nuclear_reactor.heat", heat, maxHeat),
            INFO_TEXT_X, INFO_TEXT_Y, PANEL_X1 - 2 - outputWidth - 8 - INFO_TEXT_X, heatInk(heatFraction), true);
    }

    /** 热量文字颜色：低于 50 % 为浅绿，低于 85 % 为琥珀色，其余为红色。 */
    static int heatInk(float fraction) {
        return fraction < 0.5F ? 0xFFB8FFB0 : fraction < 0.85F ? 0xFFFFD36A : 0xFFFF7A6A;
    }
}