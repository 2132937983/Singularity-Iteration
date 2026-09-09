package com.singularity_iteration.mio_icif.Screen;

import com.singularity_iteration.mio_icif.Menu.Producer.BlastFurnaceElcMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
@SuppressWarnings("null")
public class mio_icif_gui_blast_furnace_elc extends mio_icif_screen<BlastFurnaceElcMenu> {

    private static final ResourceLocation GUI_TEXTURE =
        ResourceLocation.parse("mio_icif:textures/gui/gui_blast_furnace_elc.png");

    private static final int GUI_WIDTH = 176;
    private static final int GUI_HEIGHT = 166;

    private static final int PROGRESS_X = 79;
    private static final int PROGRESS_Y = 35;

    private static final int ENERGY_ICON_X = 56;
    private static final int ENERGY_ICON_Y = 36;

    public mio_icif_gui_blast_furnace_elc(BlastFurnaceElcMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = GUI_WIDTH;
        this.imageHeight = GUI_HEIGHT;
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;

        guiGraphics.blit(GUI_TEXTURE, x, y, 0, 0, this.imageWidth, this.imageHeight);

        BlastFurnaceElcMenu menu = this.getMenu();
        if (menu != null) {
            drawProgressArrow(guiGraphics, x + PROGRESS_X, y + PROGRESS_Y, menu.getProgressPixels(ARROW_WIDTH));

            drawLightningEnergy(guiGraphics, x + ENERGY_ICON_X, y + ENERGY_ICON_Y, menu.getEnergy(), menu.getMaxEnergy());
        }
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        super.renderLabels(guiGraphics, mouseX, mouseY);

        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;

        BlastFurnaceElcMenu menu = this.getMenu();
        if (menu == null) return;

        if (isHovering(mouseX, mouseY, x + PROGRESS_X, y + PROGRESS_Y, ARROW_WIDTH, ARROW_HEIGHT)) {
            renderProgressTooltip(guiGraphics, mouseX - x, mouseY - y, menu.getProgress(), menu.getMaxProgress());
        }

        if (isHovering(mouseX, mouseY, x + ENERGY_ICON_X, y + ENERGY_ICON_Y, LIGHTNING_WIDTH, LIGHTNING_HEIGHT)) {
            renderEnergyTooltip(guiGraphics, mouseX - x, mouseY - y, menu.getEnergy(), menu.getMaxEnergy());
        }
    }

    @Override
    public void containerTick() {
        super.containerTick();

        BlastFurnaceElcMenu menu = this.getMenu();
        if (menu != null && menu.blockEntity != null) {
            menu.setSyncData(0, menu.blockEntity.getProgress());
            menu.setSyncData(1, menu.blockEntity.getMaxProgress());
            menu.setSyncData(2, menu.blockEntity.isWorking() ? 1 : 0);
            int energy = (int) menu.blockEntity.getEnergyStorage().getAmount();
            int maxEnergy = (int) menu.blockEntity.getEnergyStorage().getCapacity();
            menu.setSyncData(3, energy);
            menu.setSyncData(4, maxEnergy);
        }
    }
}