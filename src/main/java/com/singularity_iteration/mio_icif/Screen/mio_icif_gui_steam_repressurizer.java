package com.singularity_iteration.mio_icif.Screen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/**
 * 閽傚憡鑻�閸愭繂濮為崢瀣�婧� GUI 缁�? */
@OnlyIn(Dist.CLIENT)
@SuppressWarnings("null")
public class mio_icif_gui_steam_repressurizer extends mio_icif_screen<com.singularity_iteration.mio_icif.Menu.Producer.SteamRepressurizerMenu> {

    private static final ResourceLocation GUI_TEXTURE =
        ResourceLocation.parse("mio_icif:textures/gui/gui_steam_generator_elc.png");

    private static final int GUI_WIDTH = 176;
    private static final int GUI_HEIGHT = 166;

    // 杈撳叆妲芥樉绀哄尯鍩?(49, 24) 16x58
    private static final int INPUT_TANK_X = 49;
    private static final int INPUT_TANK_Y = 24;
    private static final int INPUT_TANK_WIDTH = 16;
    private static final int INPUT_TANK_HEIGHT = 58;
    @SuppressWarnings("unused")
    private static final int INPUT_COLOR = 0xFFD0D0D0;

    // 杈撳嚭妲芥樉绀哄尯鍩?(111, 24) 16x58
    private static final int OUTPUT_TANK_X = 111;
    private static final int OUTPUT_TANK_Y = 24;
    private static final int OUTPUT_TANK_WIDTH = 16;
    private static final int OUTPUT_TANK_HEIGHT = 58;
    @SuppressWarnings("unused")
    private static final int OUTPUT_COLOR = 0xFFE0E0E0;

    public mio_icif_gui_steam_repressurizer(com.singularity_iteration.mio_icif.Menu.Producer.SteamRepressurizerMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = GUI_WIDTH;
        this.imageHeight = GUI_HEIGHT;
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;

        guiGraphics.blit(GUI_TEXTURE, x, y, 0, 0, this.imageWidth, this.imageHeight);

        com.singularity_iteration.mio_icif.Menu.Producer.SteamRepressurizerMenu menu = this.menu;
        if (menu == null) return;

        // 杈撳叆娴濅綋
        int inputAmount = menu.getInputAmount();
        int inputCapacity = menu.getInputCapacity();
        net.neoforged.neoforge.fluids.FluidStack inputFluid = menu.getInputFluid();
        if (inputAmount > 0 && inputCapacity > 0 && !inputFluid.isEmpty()) {
            mio_icif_GuiUtils.renderFluidBar(guiGraphics, x + INPUT_TANK_X, y + INPUT_TANK_Y,
                INPUT_TANK_WIDTH, INPUT_TANK_HEIGHT, inputFluid, inputCapacity);
        }

        // 杈撳嚭娴濅綋
        int outputAmount = menu.getOutputAmount();
        int outputCapacity = menu.getOutputCapacity();
        net.neoforged.neoforge.fluids.FluidStack outputFluid = menu.getOutputFluid();
        if (outputAmount > 0 && outputCapacity > 0 && !outputFluid.isEmpty()) {
            mio_icif_GuiUtils.renderFluidBar(guiGraphics, x + OUTPUT_TANK_X, y + OUTPUT_TANK_Y,
                OUTPUT_TANK_WIDTH, OUTPUT_TANK_HEIGHT, outputFluid, outputCapacity);
        }

        // 閻戭參鍤岄弶?(80, 35) 16x4
        int heat = menu.getHeat();
        int maxHeat = menu.getMaxHeat();
        if (heat > 0 && maxHeat > 0) {
            int heatPixels = (heat * 16) / maxHeat;
            if (heatPixels > 0) {
guiGraphics.blit(ATLAS_TEXTURE, x + 80, y + 35, 0, (float) 138, (float) 259, heatPixels, 4, ATLAS_WIDTH, ATLAS_HEIGHT);
            }
        }
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        super.renderLabels(guiGraphics, mouseX, mouseY);

        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;

        com.singularity_iteration.mio_icif.Menu.Producer.SteamRepressurizerMenu menu = this.menu;
        if (menu == null) return;

        if (mouseX >= x + INPUT_TANK_X && mouseX <= x + INPUT_TANK_X + INPUT_TANK_WIDTH &&
            mouseY >= y + INPUT_TANK_Y && mouseY <= y + INPUT_TANK_Y + INPUT_TANK_HEIGHT) {
            guiGraphics.renderTooltip(this.font,
                Component.literal((!menu.getInputFluid().isEmpty() ? menu.getInputFluid().getFluid().getFluidType().getDescription().getString() + ": " : "") + menu.getInputAmount() + "/" + menu.getInputCapacity() + " mB"),
                mouseX, mouseY);
        }

        if (mouseX >= x + OUTPUT_TANK_X && mouseX <= x + OUTPUT_TANK_X + OUTPUT_TANK_WIDTH &&
            mouseY >= y + OUTPUT_TANK_Y && mouseY <= y + OUTPUT_TANK_Y + OUTPUT_TANK_HEIGHT) {
            guiGraphics.renderTooltip(this.font,
                Component.literal((!menu.getOutputFluid().isEmpty() ? menu.getOutputFluid().getFluid().getFluidType().getDescription().getString() + ": " : "") + menu.getOutputAmount() + "/" + menu.getOutputCapacity() + " mB"),
                mouseX, mouseY);
        }
    }
}