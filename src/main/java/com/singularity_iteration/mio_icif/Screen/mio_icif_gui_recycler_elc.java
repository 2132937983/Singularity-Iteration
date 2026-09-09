package com.singularity_iteration.mio_icif.Screen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
@SuppressWarnings("null")
public class mio_icif_gui_recycler_elc extends mio_icif_screen<com.singularity_iteration.mio_icif.Menu.Producer.RecyclerElcMenu> {

    private static final ResourceLocation GUI_TEXTURE =
        ResourceLocation.parse("mio_icif:textures/gui/gui_recycler_elc.png");

    private static final int GUI_WIDTH = 176;
    private static final int GUI_HEIGHT = 166;

    private static final int PROGRESS_X = 79;
    private static final int PROGRESS_Y = 34;

    private static final int ENERGY_ICON_X = 56;
    private static final int ENERGY_ICON_Y = 36;

    public mio_icif_gui_recycler_elc(com.singularity_iteration.mio_icif.Menu.Producer.RecyclerElcMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = GUI_WIDTH;
        this.imageHeight = GUI_HEIGHT;
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;

        guiGraphics.blit(GUI_TEXTURE, x, y, 0, 0, this.imageWidth, this.imageHeight);

        com.singularity_iteration.mio_icif.Menu.Producer.RecyclerElcMenu menu = this.menu;
        if (menu != null) {
            int progress = menu.getProgress();
            int maxProgress = menu.getMaxProgress();
            int progressPixels = maxProgress > 0 ? (progress * ARROW_WIDTH) / maxProgress : 0;
            drawProgressArrow(guiGraphics, x + PROGRESS_X, y + PROGRESS_Y, progressPixels);

            drawLightningEnergy(guiGraphics, x + ENERGY_ICON_X, y + ENERGY_ICON_Y, menu.getEnergy(), menu.getMaxEnergy());
        }
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        super.renderLabels(guiGraphics, mouseX, mouseY);

        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;

        com.singularity_iteration.mio_icif.Menu.Producer.RecyclerElcMenu menu = this.menu;
        if (menu == null) return;

        if (isHovering(mouseX, mouseY, x + PROGRESS_X, y + PROGRESS_Y, ARROW_WIDTH, ARROW_HEIGHT)) {
            guiGraphics.renderTooltip(this.font,
                Component.literal(menu.getProgress() + "/" + menu.getMaxProgress()),
                mouseX - x, mouseY - y);
        }

        if (isHovering(mouseX, mouseY, x + ENERGY_ICON_X, y + ENERGY_ICON_Y, LIGHTNING_WIDTH, LIGHTNING_HEIGHT)) {
            renderEnergyTooltip(guiGraphics, mouseX - x, mouseY - y, menu.getEnergy(), menu.getMaxEnergy());
        }
    }

    @Override
    public void containerTick() {
        super.containerTick();
        // 数据通过 ContainerData 自动同步，无需手动同步
    }
}

