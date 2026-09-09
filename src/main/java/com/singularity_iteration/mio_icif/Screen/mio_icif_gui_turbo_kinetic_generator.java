package com.singularity_iteration.mio_icif.Screen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
@SuppressWarnings("null")
public class mio_icif_gui_turbo_kinetic_generator extends mio_icif_screen<com.singularity_iteration.mio_icif.Menu.Generator.TurboKineticGeneratorMenu> {

    private static final ResourceLocation GUI_TEXTURE =
        ResourceLocation.parse("mio_icif:textures/gui/gui_kinetic_generator.png");

    private static final int GUI_WIDTH = 176;
    private static final int GUI_HEIGHT = 166;

    private static final int ENERGY_BAR_X = 40;
    private static final int ENERGY_BAR_Y = 28;

    private static final int INFO_TEXT_X = 42;
    private static final int INFO_TEXT_Y = 50;
    private static final int INFO_TEXT_COLOR = 0x20eb3e;

    public mio_icif_gui_turbo_kinetic_generator(com.singularity_iteration.mio_icif.Menu.Generator.TurboKineticGeneratorMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = GUI_WIDTH;
        this.imageHeight = GUI_HEIGHT;
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;

        guiGraphics.blit(GUI_TEXTURE, x, y, 0, 0, this.imageWidth, this.imageHeight);

        com.singularity_iteration.mio_icif.Menu.Generator.TurboKineticGeneratorMenu menu = this.menu;
        if (menu != null) {
            int energyProgressPixels = menu.getEnergyProgressPixels();
            drawKineticEnergyBar(guiGraphics, x + ENERGY_BAR_X, y + ENERGY_BAR_Y, energyProgressPixels);

            String infoText = Component.translatable("gui.mio_icif.turbo_kinetic_generator.efficiency", 85).getString();
            guiGraphics.drawString(this.font, infoText, x + INFO_TEXT_X, y + INFO_TEXT_Y, INFO_TEXT_COLOR);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        super.renderLabels(guiGraphics, mouseX, mouseY);

        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;

        com.singularity_iteration.mio_icif.Menu.Generator.TurboKineticGeneratorMenu menu = this.menu;
        if (menu == null) return;

        if (isHovering(mouseX, mouseY, x + ENERGY_BAR_X, y + ENERGY_BAR_Y, KINETIC_ENERGY_BAR_WIDTH, KINETIC_ENERGY_BAR_HEIGHT)) {
            guiGraphics.renderTooltip(this.font,
                Component.literal("Energy: " + menu.getEnergy() + "/" + menu.getMaxEnergy() + " EU"),
                mouseX - x, mouseY - y);
        }
    }

    @Override
    public void containerTick() {
        super.containerTick();
    }
}