package com.singularity_iteration.mio_icif.Screen;

import com.singularity_iteration.mio_icif.Blocks.Environment.fluid.mio_icif_fluids;
import com.singularity_iteration.mio_icif.Blocks.entity.producer.mio_icif_solar_distiller;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.fluids.FluidStack;

/**
 * 太阳能蒸馏机 GUI 类（槽位坐标与IC2 原版一致）
 */
@OnlyIn(Dist.CLIENT)
@SuppressWarnings("null")
public class mio_icif_gui_solar_distiller extends mio_icif_screen<com.singularity_iteration.mio_icif.Menu.Producer.SolarDistillerMenu> {

    private static final ResourceLocation GUI_TEXTURE =
        ResourceLocation.parse("mio_icif:textures/gui/gui_solar_destiller.png");

    private static final int GUI_WIDTH = 176;
    private static final int GUI_HEIGHT = 184;

    public mio_icif_gui_solar_distiller(com.singularity_iteration.mio_icif.Menu.Producer.SolarDistillerMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = GUI_WIDTH;
        this.imageHeight = GUI_HEIGHT;
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;
        guiGraphics.blit(GUI_TEXTURE, x, y, 0, 0, this.imageWidth, this.imageHeight);

        com.singularity_iteration.mio_icif.Menu.Producer.SolarDistillerMenu menu = this.menu;
        if (menu == null) return;

        if (menu.isWorking()) {
            guiGraphics.blit(ATLAS_TEXTURE, x + 36, y + 26, 0, (float) 73, (float) 125, 97, 29, ATLAS_WIDTH, ATLAS_HEIGHT);
        }

        // 渲染水槽液位 (37,43) 宽53 高18，从下往上填充
        mio_icif_GuiUtils.renderFluidBar(guiGraphics, x + 37, y + 43, 53, 18,
            new FluidStack(net.minecraft.world.level.material.Fluids.WATER, menu.getWaterAmount()),
            mio_icif_solar_distiller.WATER_TANK_CAPACITY);

        // 渲染蒸馏水槽液位 (115,55) 宽17 高43
        mio_icif_GuiUtils.renderFluidBar(guiGraphics, x + 115, y + 55, 17, 43,
            new FluidStack(mio_icif_fluids.DISTILLEDWATER.get(), menu.getDistilledAmount()),
            mio_icif_solar_distiller.DISTILLED_TANK_CAPACITY);
    }

}