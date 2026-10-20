package com.miophas.singularity_iteration.common.client.screen;

import com.miophas.singularity_iteration.common.client.lathe.LatheProfileRenderer;
import com.miophas.singularity_iteration.common.menu.producer.LatheMenu;
import com.miophas.singularity_iteration.core.api.item.ILatheItem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/**
 * Lathe GUI, aligned with IC2 GuiLathe:
 * - 176 x 166 panel, slots: workpiece (10, 12), tool (10, 30), output (10, 57)
 * - 5 buttons (24 x 20) from (40, 15); button i cuts position i on the server
 * - the tool in the tool slot is drawn on every button
 * - the thickness of each position ("n/max") at (40 + 24 i, 37)
 * - the workpiece profile (5 segments of 24 x up to 32 px) at (40, 45)
 */
@OnlyIn(Dist.CLIENT)
@SuppressWarnings("null")
public class mio_icif_gui_lathe extends mio_icif_screen<LatheMenu> {

    private static final ResourceLocation GUI_TEXTURE =
        ResourceLocation.fromNamespaceAndPath("mio_icif", "textures/gui/gui_lathe.png");
    private static final int GUI_WIDTH = 176;
    private static final int GUI_HEIGHT = 166;

    private static final int SEGMENT_LENGTH = LatheProfileRenderer.SEGMENT_LENGTH;
    private static final int POSITIONS = LatheProfileRenderer.POSITIONS;

    private static final int BUTTON_X = 40;
    private static final int BUTTON_Y = 15;
    private static final int BUTTON_WIDTH = 24;
    private static final int BUTTON_HEIGHT = 20;
    private static final int TOOL_ICON_X = 44;
    private static final int TOOL_ICON_Y = 17;
    private static final int INFO_X = 40;
    private static final int INFO_Y = 37;
    private static final int PROFILE_X = 40;
    private static final int PROFILE_Y = 45;

    public mio_icif_gui_lathe(LatheMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = GUI_WIDTH;
        this.imageHeight = GUI_HEIGHT;
        this.inventoryLabelY = this.imageHeight - 94;
    }

    @Override
    protected void init() {
        super.init();
        for (int i = 0; i < POSITIONS; i++) {
            final int position = i;
            this.addRenderableWidget(new com.miophas.singularity_iteration.common.client.screen.widget.SiButton(
                this.leftPos + BUTTON_X + i * SEGMENT_LENGTH, this.topPos + BUTTON_Y, BUTTON_WIDTH, BUTTON_HEIGHT,
                Component.empty(), btn -> cut(position)));
        }
    }

    /** IC2 actionPerformed: the button id goes to the server (LatheMenu.clickMenuButton). */
    private void cut(int position) {
        if (minecraft != null && minecraft.gameMode != null) {
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId, position);
        }
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        guiGraphics.blit(GUI_TEXTURE, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        super.renderLabels(guiGraphics, mouseX, mouseY);

        ItemStack workpiece = menu.getSlot(LatheMenu.LATHE_SLOT).getItem();
        if (workpiece.getItem() instanceof ILatheItem lathe) {
            LatheProfileRenderer.renderGui(guiGraphics, workpiece, PROFILE_X, PROFILE_Y);
            int[] state = lathe.getCurrentState(workpiece);
            int max = lathe.getWidth(workpiece);
            for (int j = 0; j < POSITIONS && j < state.length; j++) {
                guiGraphics.drawString(this.font, Component.translatable("ic2.Lathe.gui.info", state[j], max),
                    INFO_X + SEGMENT_LENGTH * j, INFO_Y, 0x404040, false);
            }
        }

        ItemStack tool = menu.getSlot(LatheMenu.TOOL_SLOT).getItem();
        if (tool.getItem() instanceof ILatheItem.ILatheTool) {
            for (int i = 0; i < POSITIONS; i++) {
                guiGraphics.renderItem(tool, TOOL_ICON_X + i * SEGMENT_LENGTH, TOOL_ICON_Y);
            }
        }
    }
}
