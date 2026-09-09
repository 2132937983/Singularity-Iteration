package com.singularity_iteration.mio_icif.Screen;

import com.singularity_iteration.mio_icif.Menu.Generator.AdvancedStirlingGeneratorMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
@SuppressWarnings("null")
public class mio_icif_gui_advanced_stirling_generator extends mio_icif_screen<AdvancedStirlingGeneratorMenu> {

    private static final ResourceLocation GUI_TEXTURE =
        ResourceLocation.parse("mio_icif:textures/gui/gui_stirling_generator.png");

    private static final int GUI_WIDTH = 176;
    private static final int GUI_HEIGHT = 166;

    private static final int ENERGY_BAR_X = 73;
    private static final int ENERGY_BAR_Y = 26;

    private static final int HEAT_BAR_X = 40;
    private static final int HEAT_BAR_Y = 25;
    private static final int HEAT_BAR_WIDTH = 25;
    private static final int HEAT_BAR_HEIGHT = 17;

    private static final int WORK_ICON_X = 80;
    private static final int WORK_ICON_Y = 46;
    private static final int WORK_ICON_WIDTH = 14;
    private static final int WORK_ICON_HEIGHT = 14;

    private static final int INFO_TEXT_X = 42;
    private static final int INFO_TEXT_Y = 50;
    private static final int INFO_TEXT_COLOR = 0x57c4da;

    public mio_icif_gui_advanced_stirling_generator(AdvancedStirlingGeneratorMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = GUI_WIDTH;
        this.imageHeight = GUI_HEIGHT;
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;

        guiGraphics.blit(GUI_TEXTURE, x, y, 0, 0, this.imageWidth, this.imageHeight);

        AdvancedStirlingGeneratorMenu menu = this.menu;
        if (menu != null) {
            int energyProgressPixels = menu.getEnergyProgressPixels();
            drawEnergyBar(guiGraphics, x + ENERGY_BAR_X, y + ENERGY_BAR_Y, energyProgressPixels);

            int heat = menu.getHeat();
            int maxHeat = menu.getMaxHeat();
            if (heat > 0 && maxHeat > 0) {
                int heatWidth = (heat * HEAT_BAR_WIDTH) / maxHeat;
                if (heatWidth > 0) {
                    guiGraphics.blit(ATLAS_TEXTURE, x + HEAT_BAR_X, y + HEAT_BAR_Y, 0, (float) 186, (float) 164, heatWidth, HEAT_BAR_HEIGHT, ATLAS_WIDTH, ATLAS_HEIGHT);
                }
            }

            if (menu.isWorking()) {
                guiGraphics.blit(ATLAS_TEXTURE, x + WORK_ICON_X, y + WORK_ICON_Y, 0, (float) 88, (float) 227, WORK_ICON_WIDTH, WORK_ICON_HEIGHT, ATLAS_WIDTH, ATLAS_HEIGHT);
            } else {
                guiGraphics.blit(ATLAS_TEXTURE, x + WORK_ICON_X, y + WORK_ICON_Y, 0, (float) 104, (float) 227, WORK_ICON_WIDTH, WORK_ICON_HEIGHT, ATLAS_WIDTH, ATLAS_HEIGHT);
            }

            String infoText = "HU:" + menu.getHeat() + "/EU:" + menu.getEnergy();
            guiGraphics.drawString(this.font, infoText, x + INFO_TEXT_X, y + INFO_TEXT_Y, INFO_TEXT_COLOR);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        super.renderLabels(guiGraphics, mouseX, mouseY);

        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;

        AdvancedStirlingGeneratorMenu menu = this.menu;
        if (menu == null) return;

        if (mouseX >= x + ENERGY_BAR_X && mouseX <= x + ENERGY_BAR_X + ENERGY_BAR_WIDTH &&
            mouseY >= y + ENERGY_BAR_Y && mouseY <= y + ENERGY_BAR_Y + ENERGY_BAR_HEIGHT) {
            guiGraphics.renderTooltip(this.font,
                Component.literal("Energy: " + menu.getEnergy() + "/" + menu.getMaxEnergy() + " EU"),
                mouseX - x, mouseY - y);
        }

        if (mouseX >= x + HEAT_BAR_X && mouseX <= x + HEAT_BAR_X + HEAT_BAR_WIDTH &&
            mouseY >= y + HEAT_BAR_Y && mouseY <= y + HEAT_BAR_Y + HEAT_BAR_HEIGHT) {
            guiGraphics.renderTooltip(this.font,
                Component.literal("Heat: " + menu.getHeat() + "/" + menu.getMaxHeat() + " HU"),
                mouseX - x, mouseY - y);
        }

        if (mouseX >= x + WORK_ICON_X && mouseX <= x + WORK_ICON_X + WORK_ICON_WIDTH &&
            mouseY >= y + WORK_ICON_Y && mouseY <= y + WORK_ICON_Y + WORK_ICON_HEIGHT) {
            Component tooltip = menu.isWorking() ?
                Component.literal("Generating: " + menu.getEnergyOutput() + " EU/t") :
                Component.literal("Not Generating");
            guiGraphics.renderTooltip(this.font, tooltip, mouseX - x, mouseY - y);
        }
    }
}