package com.singularity_iteration.mio_icif.Screen;

import com.singularity_iteration.mio_icif.Blocks.Environment.fluid.mio_icif_fluids;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.fluids.FluidStack;

@OnlyIn(Dist.CLIENT)
@SuppressWarnings("null")
public class mio_icif_gui_diesel_generator extends mio_icif_screen<com.singularity_iteration.mio_icif.Menu.Generator.DieselGeneratorMenu> {

    private static final ResourceLocation GUI_TEXTURE =
        ResourceLocation.parse("mio_icif:textures/gui/gui_fluid_generator.png");

    private static final int GUI_WIDTH = 176;
    private static final int GUI_HEIGHT = 166;

    private static final int ENERGY_BAR_X = 108;
    private static final int ENERGY_BAR_Y = 25;

    private static final int FUEL_TANK_X = 70;
    private static final int FUEL_TANK_Y = 20;

    public mio_icif_gui_diesel_generator(com.singularity_iteration.mio_icif.Menu.Generator.DieselGeneratorMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = GUI_WIDTH;
        this.imageHeight = GUI_HEIGHT;
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;

        guiGraphics.blit(GUI_TEXTURE, x, y, 0, 0, this.imageWidth, this.imageHeight);

        com.singularity_iteration.mio_icif.Menu.Generator.DieselGeneratorMenu menu = this.menu;
        if (menu != null) {
            int fuelAmount = menu.getFuelAmount();
            int fuelCapacity = menu.getFuelCapacity();
            drawFluidTank(guiGraphics, x + FUEL_TANK_X, y + FUEL_TANK_Y,
                new FluidStack(mio_icif_fluids.DIESELOIL.get(), fuelAmount), fuelAmount, fuelCapacity);

            int energyProgressPixels = menu.getEnergyProgressPixels();
            drawEnergyBar(guiGraphics, x + ENERGY_BAR_X, y + ENERGY_BAR_Y, energyProgressPixels);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        super.renderLabels(guiGraphics, mouseX, mouseY);

        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;

        com.singularity_iteration.mio_icif.Menu.Generator.DieselGeneratorMenu menu = this.menu;
        if (menu == null) return;

        if (mouseX >= x + ENERGY_BAR_X && mouseX <= x + ENERGY_BAR_X + ENERGY_BAR_WIDTH &&
            mouseY >= y + ENERGY_BAR_Y && mouseY <= y + ENERGY_BAR_Y + ENERGY_BAR_HEIGHT) {
            guiGraphics.renderTooltip(this.font,
                Component.literal("Energy: " + menu.getEnergy() + "/" + menu.getMaxEnergy() + " EU"),
                mouseX - x, mouseY - y);
        }

        if (mouseX >= x + FUEL_TANK_X && mouseX <= x + FUEL_TANK_X + FLUID_TANK_BG_WIDTH &&
            mouseY >= y + FUEL_TANK_Y && mouseY <= y + FUEL_TANK_Y + FLUID_TANK_BG_HEIGHT) {
            guiGraphics.renderTooltip(this.font,
                Component.literal((menu.getFuelAmount() > 0 ? com.singularity_iteration.mio_icif.Blocks.Environment.fluid.mio_icif_fluids.DIESELOIL.get().getFluidType().getDescription().getString() + ": " : "") + menu.getFuelAmount() + "/" + menu.getFuelCapacity() + " mB"),
                mouseX - x, mouseY - y);
        }
    }
}