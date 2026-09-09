package com.singularity_iteration.mio_icif.Screen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import com.singularity_iteration.mio_icif.Menu.Producer.CondenserMenu;
import net.neoforged.neoforge.fluids.FluidStack;
import com.singularity_iteration.mio_icif.Blocks.Environment.fluid.mio_icif_fluids;
import com.singularity_iteration.mio_icif.Blocks.entity.producer.mio_icif_condenser;

/**
 * 冷凝机的GUI类（槽位坐标与IC2 原版一致）
 */
@OnlyIn(Dist.CLIENT)
@SuppressWarnings("null")
public class mio_icif_gui_condenser extends mio_icif_screen<com.singularity_iteration.mio_icif.Menu.Producer.CondenserMenu> {

    private static final ResourceLocation GUI_TEXTURE =
        ResourceLocation.parse("mio_icif:textures/gui/gui_condenser_elc.png");
    private static final int GUI_WIDTH = 176;
    private static final int GUI_HEIGHT = 184;

    // 闪电/能量图标位置
    private static final int ENERGY_X = 9;
    private static final int ENERGY_Y = 25;

    // 蒸汽 -> 蒸馏水进度条
    private static final int PROGRESS_X = 46;
    private static final int PROGRESS_Y = 62;
    @SuppressWarnings("unused")
    private static final int PROGRESS_WIDTH = 84;
    @SuppressWarnings("unused")
    private static final int PROGRESS_HEIGHT = 9;
    @SuppressWarnings("unused")
    private static final int PROGRESS_TEXTURE_X = 0;
    @SuppressWarnings("unused")
    private static final int PROGRESS_TEXTURE_Y = 184;

    public mio_icif_gui_condenser(CondenserMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = GUI_WIDTH;
        this.imageHeight = GUI_HEIGHT;
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;
        guiGraphics.blit(GUI_TEXTURE, x, y, 0, 0, this.imageWidth, this.imageHeight);

        CondenserMenu menu = this.getMenu();
        if (menu == null) return;

        // 能量图标（使用基类标准闪电标志渲染方法）
        drawLightningEnergy(guiGraphics, x + ENERGY_X, y + ENERGY_Y, menu.getEnergy(), menu.getMaxEnergy());

        // 蒸汽转换进度条：从左往右填充
        int progress = menu.getProgress();
        int maxProgress = mio_icif_condenser.MAX_PROGRESS;
        if (progress > 0 && maxProgress > 0) {
            int fillWidth = (progress * CONDENSER_PROGRESS_WIDTH) / maxProgress;
            drawCondenserProgress(guiGraphics, x + PROGRESS_X, y + PROGRESS_Y, fillWidth);
        }

        // 蒸汽储量条：从上往下填充(46,27) 宽84 高33
        mio_icif_GuiUtils.renderFluidBar(guiGraphics, x + 46, y + 27, 84, 33,
            new FluidStack(mio_icif_fluids.STEAM.get(), menu.getSteamAmount()),
            mio_icif_condenser.STEAM_TANK_CAPACITY, true);

        // 蒸馏水储量条：从下往上填充(46,74) 宽84 高15
        mio_icif_GuiUtils.renderFluidBar(guiGraphics, x + 46, y + 74, 84, 15,
            new FluidStack(mio_icif_fluids.DISTILLEDWATER.get(), menu.getDistilledAmount()),
            mio_icif_condenser.DISTILLED_TANK_CAPACITY, false);
    }

}