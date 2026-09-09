package com.singularity_iteration.mio_icif.Screen;

import com.singularity_iteration.mio_icif.Menu.Producer.PumpElcMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.fluids.FluidStack;

/**
 * 电力泵方块的 GUI 类 - 使用标准组件
 */
@OnlyIn(Dist.CLIENT)
@SuppressWarnings("null")
public class mio_icif_gui_pump_elc extends mio_icif_screen<PumpElcMenu> {

    private static final ResourceLocation GUI_TEXTURE =
        ResourceLocation.parse("mio_icif:textures/gui/gui_pump_elc.png");

    // GUI 尺寸
    private static final int GUI_WIDTH = 176;
    private static final int GUI_HEIGHT = 166;

    // 进度条位置（使用标准箭头进度条）
    private static final int PROGRESS_X = 35;
    private static final int PROGRESS_Y = 35;

    // 电能标志位置（使用标准闪电标志）
    private static final int ENERGY_ICON_X = 8;
    private static final int ENERGY_ICON_Y = 27;

    // 流体槽位置（使用标准流体槽）
    private static final int FLUID_TANK_X = 74;
    private static final int FLUID_TANK_Y = 20;

    public mio_icif_gui_pump_elc(PumpElcMenu menu, Inventory playerInventory, Component title) {
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

        PumpElcMenu menu = this.menu;
        if (menu != null) {
            // 使用基类标准流体槽渲染
            drawFluidTank(guiGraphics, x + FLUID_TANK_X, y + FLUID_TANK_Y, 
                menu.getFluidStack(), menu.getFluidAmount(), menu.getFluidCapacity());

            // 使用基类标准工作进度条（箭头）
            int progressPixels = menu.getProgress() * ARROW_WIDTH / Math.max(menu.getMaxProgress(), 1);
            drawProgressArrow(guiGraphics, x + PROGRESS_X, y + PROGRESS_Y, progressPixels);

            // 使用基类标准闪电标志
            drawLightningEnergy(guiGraphics, x + ENERGY_ICON_X, y + ENERGY_ICON_Y, menu.getEnergy(), menu.getMaxEnergy());
        }
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        super.renderLabels(guiGraphics, mouseX, mouseY);

        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;

        PumpElcMenu menu = this.menu;
        if (menu == null) return;

        // 进度条 tooltip
        if (isHovering(mouseX, mouseY, x + PROGRESS_X, y + PROGRESS_Y, ARROW_WIDTH, ARROW_HEIGHT)) {
            renderProgressTooltip(guiGraphics, mouseX - x, mouseY - y, menu.getProgress(), menu.getMaxProgress());
        }

        // 电能标志 tooltip
        if (isHovering(mouseX, mouseY, x + ENERGY_ICON_X, y + ENERGY_ICON_Y, LIGHTNING_WIDTH, LIGHTNING_HEIGHT)) {
            renderEnergyTooltip(guiGraphics, mouseX - x, mouseY - y, menu.getEnergy(), menu.getMaxEnergy());
        }

        // 流体槽 tooltip
        if (isHovering(mouseX, mouseY, x + FLUID_TANK_X, y + FLUID_TANK_Y, FLUID_TANK_BG_WIDTH, FLUID_TANK_BG_HEIGHT)) {
            FluidStack fluid = menu.getFluidStack();
            if (!fluid.isEmpty()) {
                guiGraphics.renderTooltip(this.font, 
                    Component.literal(fluid.getFluid().getFluidType().getDescription().getString() + ": " + menu.getFluidAmount() + " / " + menu.getFluidCapacity() + " mB"),
                    mouseX - x, mouseY - y);
            } else {
                guiGraphics.renderTooltip(this.font, 
                    Component.literal("空"),
                    mouseX - x, mouseY - y);
            }
        }
    }

    @Override
    public void containerTick() {
        super.containerTick();
        // 数据通过 ContainerData 自动同步，无需手动同步
    }
}