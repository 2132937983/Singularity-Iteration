package com.miophas.singularity_iteration.common.client.screen;

import com.miophas.singularity_iteration.common.menu.tool.mio_icif_toolbox_menu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/**
 * 工具箱 GUI。
 * 对齐 IC2 1.12.2 的 GuiToolbox：176x166，一排工具槽（y=41）+ 玩家背包。
 */
@OnlyIn(Dist.CLIENT)
@SuppressWarnings("null")
public class mio_icif_gui_toolbox extends mio_icif_screen<mio_icif_toolbox_menu> {

    private static final ResourceLocation GUI_TEXTURE =
        ResourceLocation.parse("mio_icif:textures/gui/gui_toolbox.png");

    private static final int GUI_WIDTH = 176;
    private static final int GUI_HEIGHT = 166;
    /** 0.1.11.170: the bottom of the slot row in the texture (its slots start at y 41, their frames at 40). */
    private static final int ROW_BOTTOM = 58;

    // 对齐 IC2: GuiToolbox Text.create(this, 65, 11, ...)
    private static final int TITLE_X = 65;
    private static final int TITLE_Y = 11;

    public mio_icif_gui_toolbox(mio_icif_toolbox_menu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = GUI_WIDTH;
        // 0.1.11.170: taller by the extra toolbox rows (the advanced toolbox has three)
        this.imageHeight = GUI_HEIGHT + mio_icif_toolbox_menu.extraRowsHeight(menu.getInventorySize());
    }

    @Override
    protected void init() {
        super.init();
        this.titleLabelX = TITLE_X;
        this.titleLabelY = TITLE_Y;
        this.inventoryLabelY = this.imageHeight - 94;
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;
        int extra = this.imageHeight - GUI_HEIGHT;
        if (extra == 0) {
            guiGraphics.blit(GUI_TEXTURE, x, y, 0, 0, this.imageWidth, this.imageHeight);
            return;
        }
        // 0.1.11.170: the panel down to the first slot row, that row again for each extra row, then the rest
        guiGraphics.blit(GUI_TEXTURE, x, y, 0, 0, GUI_WIDTH, ROW_BOTTOM);
        for (int dy = 0; dy < extra; dy += 18)
            guiGraphics.blit(GUI_TEXTURE, x, y + ROW_BOTTOM + dy, 0, ROW_BOTTOM - 18, GUI_WIDTH, 18);
        guiGraphics.blit(GUI_TEXTURE, x, y + ROW_BOTTOM + extra, 0, ROW_BOTTOM, GUI_WIDTH, GUI_HEIGHT - ROW_BOTTOM);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        this.renderTooltip(guiGraphics, mouseX, mouseY);
    }
}
