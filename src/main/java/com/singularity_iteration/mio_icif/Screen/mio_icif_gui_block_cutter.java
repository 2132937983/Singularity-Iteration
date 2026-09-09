package com.singularity_iteration.mio_icif.Screen;

import com.singularity_iteration.mio_icif.Menu.Producer.BlockCutterMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/**
 * æ¹ååå²æ?GUI ç±»ï¼æ§½ä½åæ ä¸?IC2 block_cutter.xml ä¸è´ï¼
 */
@OnlyIn(Dist.CLIENT)
@SuppressWarnings("null")
public class mio_icif_gui_block_cutter extends mio_icif_screen<BlockCutterMenu> {

private static final ResourceLocation GUI_TEXTURE =
        ResourceLocation.parse("mio_icif:textures/gui/gui_cutter_elc.png");

    private static final int GUI_WIDTH = 176;
    private static final int GUI_HEIGHT = 166;

    private static final int PROGRESS_X = 55;
    private static final int PROGRESS_Y = 37;

    private static final int ENERGY_X = 27;
    private static final int ENERGY_Y = 36;

    public mio_icif_gui_block_cutter(BlockCutterMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = GUI_WIDTH;
        this.imageHeight = GUI_HEIGHT;
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;
        guiGraphics.blit(GUI_TEXTURE, x, y, 0, 0, this.imageWidth, this.imageHeight);

        BlockCutterMenu menu = this.menu;

        // 固定渲染进度条背景
        guiGraphics.blit(ATLAS_TEXTURE, x + PROGRESS_X, y + PROGRESS_Y, 0,
            (float) CUTTER_PROGRESS_BG_U, (float) CUTTER_PROGRESS_BG_V,
            CUTTER_PROGRESS_BG_WIDTH, CUTTER_PROGRESS_BG_HEIGHT, ATLAS_WIDTH, ATLAS_HEIGHT);

        // 有进度时渲染进度条填充
        int progressPixels = menu.getProgressPixels(CUTTER_PROGRESS_WIDTH);
        if (progressPixels > 0) {
            guiGraphics.blit(ATLAS_TEXTURE, x + PROGRESS_X, y + PROGRESS_Y, 0,
                (float) CUTTER_PROGRESS_U, (float) CUTTER_PROGRESS_V,
                progressPixels, CUTTER_PROGRESS_HEIGHT, ATLAS_WIDTH, ATLAS_HEIGHT);
        }

        drawLightningEnergy(guiGraphics, x + ENERGY_X, y + ENERGY_Y, menu.getEnergy(), menu.getMaxEnergy());
    }
}