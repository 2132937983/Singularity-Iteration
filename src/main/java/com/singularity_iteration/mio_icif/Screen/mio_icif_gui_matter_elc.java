package com.singularity_iteration.mio_icif.Screen;

import com.singularity_iteration.mio_icif.Blocks.Environment.fluid.mio_icif_fluids;
import com.singularity_iteration.mio_icif.Menu.Producer.MatterElcMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/**
 * UU物质生成机的 GUI 类
 */
@OnlyIn(Dist.CLIENT)
@SuppressWarnings("null")
public class mio_icif_gui_matter_elc extends mio_icif_screen<MatterElcMenu> {

    private static final ResourceLocation GUI_TEXTURE =
        ResourceLocation.parse("mio_icif:textures/gui/gui_matter_elc.png");

    // GUI尺寸
    private static final int GUI_WIDTH = 176;
    private static final int GUI_HEIGHT = 166;

    // UU物质槽位置
    private static final int UU_TANK_X = 96;
    private static final int UU_TANK_Y = 22;

    // 进度文本位置（对齐原版IC2）
    private static final int PROGRESS_LABEL_X = 8;
    private static final int PROGRESS_LABEL_Y = 22;
    private static final int PROGRESS_VALUE_X = 18;
    private static final int PROGRESS_VALUE_Y = 31;

    // 增幅器文本位置
    private static final int AMPLIFIER_LABEL_X = 8;
    private static final int AMPLIFIER_LABEL_Y = 46;
    private static final int AMPLIFIER_VALUE_X = 8;
    private static final int AMPLIFIER_VALUE_Y = 58;

    // 翻译键
    private final Component progressLabel;
    private final Component amplifierLabel;

    public mio_icif_gui_matter_elc(MatterElcMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = GUI_WIDTH;
        this.imageHeight = GUI_HEIGHT;
        this.inventoryLabelY = this.imageHeight - 94;

        // 初始化翻译键
        this.progressLabel = Component.translatable("gui.mio_icif.matter_elc.progress");
        this.amplifierLabel = Component.translatable("gui.mio_icif.matter_elc.amplifier");
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;

        // 绘制背景
        guiGraphics.blit(GUI_TEXTURE, x, y, 0, 0, this.imageWidth, this.imageHeight);

        MatterElcMenu menu = this.menu;
        if (menu != null) {
            // 使用基类标准流体槽渲染UU物质
            drawFluidTank(guiGraphics, x + UU_TANK_X, y + UU_TANK_Y, 
                new net.neoforged.neoforge.fluids.FluidStack(
                    com.singularity_iteration.mio_icif.Blocks.Environment.fluid.mio_icif_fluids.UUMATTER.get(), 
                    menu.getFluidAmount()), 
                menu.getFluidAmount(),
                menu.getFluidCapacity());
        }
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        // 渲染标题和玩家背包标题
        guiGraphics.drawString(this.font, this.title, this.titleLabelX, this.titleLabelY, 4210752, false);
        guiGraphics.drawString(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, 4210752, false);

        // 渲染进度标签和数值（对齐原版IC2）
        guiGraphics.drawString(this.font, this.progressLabel, PROGRESS_LABEL_X, PROGRESS_LABEL_Y, 4210752, false);

        // 计算并显示进度百分比
        MatterElcMenu menu = this.menu;
        if (menu != null) {
            int energy = menu.getEnergy();
            int maxEnergy = menu.getMaxEnergy();
            int progressPercent = maxEnergy > 0 ? (int) Math.min(100.0 * energy / maxEnergy, 100.0) : 0;
            Component progressValue = Component.literal(progressPercent + "%");
            guiGraphics.drawString(this.font, progressValue, PROGRESS_VALUE_X, PROGRESS_VALUE_Y, 4210752, false);

            // 如果有增幅器数值，显示增幅器信息
            int scrap = menu.getScrap();
            if (scrap > 0) {
                guiGraphics.drawString(this.font, this.amplifierLabel, AMPLIFIER_LABEL_X, AMPLIFIER_LABEL_Y, 4210752, false);
                Component scrapValue = Component.literal(String.valueOf(scrap));
                guiGraphics.drawString(this.font, scrapValue, AMPLIFIER_VALUE_X, AMPLIFIER_VALUE_Y, 4210752, false);
            }
        }
    }

    @Override
    protected void renderTooltip(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        super.renderTooltip(guiGraphics, mouseX, mouseY);

        MatterElcMenu menu = this.menu;
        if (menu == null) return;

        // 检查鼠标是否在UU物质槽区域，显示tooltip
        // 注意：renderTooltip中的mouseX, mouseY已经是相对于屏幕的绝对坐标
        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;
        int tankScreenX = x + UU_TANK_X;
        int tankScreenY = y + UU_TANK_Y;
        
        if (mouseX >= tankScreenX && mouseX < tankScreenX + 20 &&
            mouseY >= tankScreenY && mouseY < tankScreenY + 55) {
            Component tooltip = Component.literal(mio_icif_fluids.UUMATTER.get().getFluidType().getDescription().getString() + ": " + menu.getFluidAmount() + "/" + menu.getFluidCapacity() + " mB");
            guiGraphics.renderTooltip(this.font, tooltip, mouseX, mouseY);
        }
    }

    @Override
    public void containerTick() {
        super.containerTick();
    }
}