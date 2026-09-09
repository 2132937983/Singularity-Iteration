package com.singularity_iteration.mio_icif.Screen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import com.singularity_iteration.mio_icif.Menu.Producer.FermenterElcMenu;
import net.neoforged.neoforge.fluids.FluidStack;
import com.singularity_iteration.mio_icif.Blocks.Environment.fluid.mio_icif_fluids;

/**
 * 发酵机方块的 GUI类
 * 复刻 IC2 原版发酵机的GUI 布局
 */
@OnlyIn(Dist.CLIENT)
@SuppressWarnings("null")
public class mio_icif_gui_fermenter_elc extends mio_icif_screen<com.singularity_iteration.mio_icif.Menu.Producer.FermenterElcMenu> {

    private static final ResourceLocation GUI_TEXTURE =
        ResourceLocation.parse("mio_icif:textures/gui/gui_fermenter_elc.png");
    // GUI 尺寸：IC2 原版发酵机高度为 184
    private static final int GUI_WIDTH = 176;
    private static final int GUI_HEIGHT = 184;

    // 进度条：方向向右
    private static final int PROGRESS_X = 38;
    private static final int PROGRESS_Y = 88;

    // 热量条：方向向右
    private static final int HEAT_X = 42;
    private static final int HEAT_Y = 41;

    // 生物质流体槽：IC2 createPlain(38, 49, 48, 30)
    private static final int BIOMASS_TANK_X = 38;
    private static final int BIOMASS_TANK_Y = 49;
    private static final int BIOMASS_TANK_WIDTH = 48;
    private static final int BIOMASS_TANK_HEIGHT = 30;

    // 沼气流体槽：IC2 createNormal(127, 24)，尺寸 16x51
    private static final int BIOGAS_TANK_X = 127;
    private static final int BIOGAS_TANK_Y = 24;
    private static final int BIOGAS_TANK_WIDTH = 16;
    private static final int BIOGAS_TANK_HEIGHT = 51;

    // 流体颜色（临时占位，最终应读取流体纹理）
    @SuppressWarnings("unused")
    private static final int BIOMASS_COLOR = 0xFF4A7C31;
    @SuppressWarnings("unused")
    private static final int BIOGAS_COLOR = 0xFFD4C44A;

    public mio_icif_gui_fermenter_elc(FermenterElcMenu menu, Inventory playerInventory, Component title) {
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

        FermenterElcMenu menu = this.getMenu();
        if (menu == null) return;

        // 绘制生物质流体
        int biomassAmount = menu.getBiomassAmount();
        int biomassCapacity = menu.getBiomassCapacity();
        if (biomassAmount > 0 && biomassCapacity > 0) {
            mio_icif_GuiUtils.renderFluidBar(guiGraphics, x + BIOMASS_TANK_X, y + BIOMASS_TANK_Y,
                BIOMASS_TANK_WIDTH, BIOMASS_TANK_HEIGHT,
                new FluidStack(mio_icif_fluids.BIOMASS.get(), biomassAmount), biomassCapacity);
        }

        // 绘制沼气流体
        int biogasAmount = menu.getBiogasAmount();
        int biogasCapacity = menu.getBiogasCapacity();
        if (biogasAmount > 0 && biogasCapacity > 0) {
            mio_icif_GuiUtils.renderFluidBar(guiGraphics, x + BIOGAS_TANK_X, y + BIOGAS_TANK_Y,
                BIOGAS_TANK_WIDTH, BIOGAS_TANK_HEIGHT,
                new FluidStack(mio_icif_fluids.BIOGAS.get(), biogasAmount), biogasCapacity);
        }

        // 绘制进度条
        int progressPixels = menu.getProgressPixels(FERMENTER_PROGRESS_WIDTH);
        drawFermenterProgress(guiGraphics, x + PROGRESS_X, y + PROGRESS_Y, progressPixels);

        // 绘制热量条
        int heat = menu.getHeat();
        int maxHeat = menu.getMaxHeat();
        if (heat > 0 && maxHeat > 0) {
            int heatPixels = (heat * FERMENTER_HEAT_WIDTH) / maxHeat;
            drawFermenterHeat(guiGraphics, x + HEAT_X, y + HEAT_Y, heatPixels);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        super.renderLabels(guiGraphics, mouseX, mouseY);

        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;

        FermenterElcMenu menu = this.getMenu();
        if (menu == null) return;

        // 进度条提示
        if (mouseX >= x + PROGRESS_X && mouseX <= x + PROGRESS_X + FERMENTER_PROGRESS_WIDTH &&
                mouseY >= y + PROGRESS_Y && mouseY <= y + PROGRESS_Y + FERMENTER_PROGRESS_HEIGHT) {
            guiGraphics.renderTooltip(this.font,
                Component.literal(menu.getProgress() + "/" + menu.getMaxProgress()),
                mouseX - x, mouseY - y);
        }

        // 热量提示
        if (mouseX >= x + HEAT_X && mouseX <= x + HEAT_X + FERMENTER_HEAT_WIDTH &&
            mouseY >= y + HEAT_Y && mouseY <= y + HEAT_Y + FERMENTER_HEAT_HEIGHT) {
            guiGraphics.renderTooltip(this.font,
                Component.literal("Heat: " + menu.getHeat() + "/" + menu.getMaxHeat() + " HU"),
                mouseX - x, mouseY - y);
        }

        // 生物质槽提示
        if (mouseX >= x + BIOMASS_TANK_X && mouseX <= x + BIOMASS_TANK_X + BIOMASS_TANK_WIDTH &&
            mouseY >= y + BIOMASS_TANK_Y && mouseY <= y + BIOMASS_TANK_Y + BIOMASS_TANK_HEIGHT) {
            guiGraphics.renderTooltip(this.font,
                Component.literal((menu.getBiomassAmount() > 0 ? com.singularity_iteration.mio_icif.Blocks.Environment.fluid.mio_icif_fluids.BIOMASS.get().getFluidType().getDescription().getString() + ": " : "") + menu.getBiomassAmount() + "/" + menu.getBiomassCapacity() + " mB"),
                mouseX - x, mouseY - y);
        }

        // 沼气槽提示
        if (mouseX >= x + BIOGAS_TANK_X && mouseX <= x + BIOGAS_TANK_X + BIOGAS_TANK_WIDTH &&
            mouseY >= y + BIOGAS_TANK_Y && mouseY <= y + BIOGAS_TANK_Y + BIOGAS_TANK_HEIGHT) {
            guiGraphics.renderTooltip(this.font,
                Component.literal((menu.getBiogasAmount() > 0 ? com.singularity_iteration.mio_icif.Blocks.Environment.fluid.mio_icif_fluids.BIOGAS.get().getFluidType().getDescription().getString() + ": " : "") + menu.getBiogasAmount() + "/" + menu.getBiogasCapacity() + " mB"),
                mouseX - x, mouseY - y);
        }
    }


}