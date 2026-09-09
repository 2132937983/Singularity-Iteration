package com.singularity_iteration.mio_icif.Screen;

import com.singularity_iteration.mio_icif.Menu.Generator.ExperienceGeneratorMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
@SuppressWarnings("null")
public class mio_icif_gui_experience_generator extends mio_icif_screen<ExperienceGeneratorMenu> {

    private static final ResourceLocation GUI_TEXTURE =
        ResourceLocation.parse("mio_icif:textures/gui/gui_generator.png");

    private static final int GUI_WIDTH = 176;
    private static final int GUI_HEIGHT = 166;

    private static final int ENERGY_BAR_X = 73;
    private static final int ENERGY_BAR_Y = 26;

    private static final int INFO_TEXT_X = 42;
    private static final int INFO_TEXT_Y = 50;
    private static final int INFO_TEXT_COLOR = 0x57c4da;

    public mio_icif_gui_experience_generator(ExperienceGeneratorMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = GUI_WIDTH;
        this.imageHeight = GUI_HEIGHT;
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;

        guiGraphics.blit(GUI_TEXTURE, x, y, 0, 0, this.imageWidth, this.imageHeight);

        ExperienceGeneratorMenu menu = this.menu;
        if (menu != null) {
            int energyProgressPixels = menu.getEnergyProgressPixels();
            drawEnergyBar(guiGraphics, x + ENERGY_BAR_X, y + ENERGY_BAR_Y, energyProgressPixels);

            String infoText = "XP:" + menu.getFuel() + "/EU:" + menu.getEnergy();
            guiGraphics.drawString(this.font, infoText, x + INFO_TEXT_X, y + INFO_TEXT_Y, INFO_TEXT_COLOR);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        super.renderLabels(guiGraphics, mouseX, mouseY);

        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;

        ExperienceGeneratorMenu menu = this.menu;
        if (menu == null) return;

        if (mouseX >= x + ENERGY_BAR_X && mouseX <= x + ENERGY_BAR_X + ENERGY_BAR_WIDTH &&
            mouseY >= y + ENERGY_BAR_Y && mouseY <= y + ENERGY_BAR_Y + ENERGY_BAR_HEIGHT) {
            guiGraphics.renderTooltip(this.font,
                Component.literal("Energy: " + menu.getEnergy() + "/" + menu.getMaxEnergy() + " EU"),
                mouseX - x, mouseY - y);
        }
    }
}