package com.singularity_iteration.mio_icif.Screen;

import com.singularity_iteration.mio_icif.Menu.Producer.OilRefineryElcMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.fluids.FluidStack;

@OnlyIn(Dist.CLIENT)
@SuppressWarnings("null")
public class mio_icif_gui_oil_refinery_elc extends mio_icif_screen<OilRefineryElcMenu> {

    private static final ResourceLocation GUI_TEXTURE =
        ResourceLocation.parse("mio_icif:textures/gui/gui_oil_refinery_elc.png");

    private static final int GUI_WIDTH = 176;
    private static final int GUI_HEIGHT = 166;

    private static final int LIGHTNING_X = 77;
    private static final int LIGHTNING_Y = 45;

    private static final int PROGRESS_X = 73;
    private static final int PROGRESS_Y = 21;

    private static final int INPUT_TANK_X = 45;
    private static final int INPUT_TANK_Y = 13;

    private static final int OUTPUT_TANK_X = 103;
    private static final int OUTPUT_TANK_Y = 13;

    public mio_icif_gui_oil_refinery_elc(OilRefineryElcMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = GUI_WIDTH;
        this.imageHeight = GUI_HEIGHT;
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;

        guiGraphics.blit(GUI_TEXTURE, x, y, 0, 0, this.imageWidth, this.imageHeight);

        OilRefineryElcMenu menu = this.getMenu();
        if (menu == null) return;

        drawLightningEnergy(guiGraphics, x + LIGHTNING_X, y + LIGHTNING_Y, menu.getEnergy(), menu.getMaxEnergy());

        drawProgressArrow(guiGraphics, x + PROGRESS_X, y + PROGRESS_Y, menu.getProgressPixels(ARROW_WIDTH));

        FluidStack inputFluid = menu.getInputFluid();
        drawFluidTank(guiGraphics, x + INPUT_TANK_X, y + INPUT_TANK_Y, inputFluid, menu.getInputFluidAmount(), menu.getInputFluidCapacity());

        FluidStack outputFluid = menu.getOutputFluid();
        drawFluidTank(guiGraphics, x + OUTPUT_TANK_X, y + OUTPUT_TANK_Y, outputFluid, menu.getOutputFluidAmount(), menu.getOutputFluidCapacity());
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        super.renderLabels(guiGraphics, mouseX, mouseY);

        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;

        OilRefineryElcMenu menu = this.getMenu();
        if (menu == null) return;

        if (isHovering(mouseX, mouseY, x + LIGHTNING_X, y + LIGHTNING_Y, LIGHTNING_WIDTH, LIGHTNING_HEIGHT)) {
            renderEnergyTooltip(guiGraphics, mouseX - x, mouseY - y, menu.getEnergy(), menu.getMaxEnergy());
        }

        if (isHovering(mouseX, mouseY, x + PROGRESS_X, y + PROGRESS_Y, ARROW_WIDTH, ARROW_HEIGHT)) {
            renderProgressTooltip(guiGraphics, mouseX - x, mouseY - y, menu.getProgress(), menu.getMaxProgress());
        }

        if (isHovering(mouseX, mouseY, x + INPUT_TANK_X, y + INPUT_TANK_Y, FLUID_TANK_BG_WIDTH, FLUID_TANK_BG_HEIGHT)) {
            FluidStack inputFluid = menu.getInputFluid();
            if (!inputFluid.isEmpty()) {
                guiGraphics.renderTooltip(this.font,
                    Component.literal(inputFluid.getFluid().getFluidType().getDescription().getString() + ": " + menu.getInputFluidAmount() + "/" + menu.getInputFluidCapacity() + " mB"),
                    mouseX - x, mouseY - y);
            } else {
                guiGraphics.renderTooltip(this.font,
                    Component.literal("0/" + menu.getInputFluidCapacity() + " mB"),
                    mouseX - x, mouseY - y);
            }
        }

        if (isHovering(mouseX, mouseY, x + OUTPUT_TANK_X, y + OUTPUT_TANK_Y, FLUID_TANK_BG_WIDTH, FLUID_TANK_BG_HEIGHT)) {
            FluidStack outputFluid = menu.getOutputFluid();
            if (!outputFluid.isEmpty()) {
                guiGraphics.renderTooltip(this.font,
                    Component.literal(outputFluid.getFluid().getFluidType().getDescription().getString() + ": " + menu.getOutputFluidAmount() + "/" + menu.getOutputFluidCapacity() + " mB"),
                    mouseX - x, mouseY - y);
            } else {
                guiGraphics.renderTooltip(this.font,
                    Component.literal("0/" + menu.getOutputFluidCapacity() + " mB"),
                    mouseX - x, mouseY - y);
            }
        }
    }
}