package com.miophas.singularity_iteration.common.client.screen;

import com.miophas.singularity_iteration.common.menu.storage.GESUCoreMenu;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.client.gui.GuiGraphics;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import java.text.DecimalFormat;

@OnlyIn(Dist.CLIENT)
@SuppressWarnings("null")
public class mio_icif_gui_gesu_core extends mio_icif_screen<GESUCoreMenu> {

    private static final ResourceLocation GUI_TEXTURE =
        ResourceLocation.parse("mio_icif:textures/gui/gui_bat_box.png");

    private static final int ENERGY_BAR_X = 79;
    private static final int ENERGY_BAR_Y = 34;
    private static final int MODULE_TEXT_X = 80;
    private static final int MODULE_TEXT_W = 176 - 6 - 80;

    private static final DecimalFormat ENERGY_FORMAT = new DecimalFormat("#,###");

    public mio_icif_gui_gesu_core(GESUCoreMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 196;
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;

        guiGraphics.blit(GUI_TEXTURE, x, y, 0, 0, this.imageWidth, this.imageHeight);

        GESUCoreMenu menu = this.getMenu();
        if (menu != null) {
            int energyProgressPixels = menu.getEnergyProgressPixels();
            drawEnergyBar(guiGraphics, x + ENERGY_BAR_X, y + ENERGY_BAR_Y, energyProgressPixels);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        super.renderLabels(guiGraphics, mouseX, mouseY);

        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;

        GESUCoreMenu menu = this.getMenu();
        if (menu == null) return;

        if (mouseX >= x + ENERGY_BAR_X && mouseX <= x + ENERGY_BAR_X + ENERGY_BAR_WIDTH &&
            mouseY >= y + ENERGY_BAR_Y && mouseY <= y + ENERGY_BAR_Y + ENERGY_BAR_HEIGHT) {
            long energy = menu.getEnergy();
            long maxEnergy = menu.getMaxEnergy();
            guiGraphics.renderTooltip(this.font,
                Component.literal(formatEnergy(energy) + "/" + formatEnergy(maxEnergy) + " EU"),
                mouseX - x, mouseY - y);
        }

        int inputCount = menu.getInputModuleCount();
        int outputCount = menu.getOutputModuleCount();
        if (inputCount > 0 || outputCount > 0) {
            // right of the slot column (the discharge slot frame sits at 56..74 on this texture)
            drawFitted(guiGraphics, this.font,
                Component.translatable("gui.mio_icif.gesu_core.input_modules", inputCount),
                MODULE_TEXT_X, 56, MODULE_TEXT_W, 0x404040, false);
            drawFitted(guiGraphics, this.font,
                Component.translatable("gui.mio_icif.gesu_core.output_modules", outputCount),
                MODULE_TEXT_X, 68, MODULE_TEXT_W, 0x404040, false);
        } else {
            drawFitted(guiGraphics, this.font,
                Component.translatable("gui.mio_icif.gesu_core.no_structure"),
                MODULE_TEXT_X, 56, MODULE_TEXT_W, 0xFF5555, false);
        }
    }

    private String formatEnergy(long energy) {
        if (energy >= 1_000_000_000L) {
            return String.format("%.2fB", energy / 1_000_000_000.0);
        } else if (energy >= 1_000_000L) {
            return String.format("%.2fM", energy / 1_000_000.0);
        } else if (energy >= 1_000L) {
            return String.format("%.2fK", energy / 1_000.0);
        }
        return ENERGY_FORMAT.format(energy);
    }
}