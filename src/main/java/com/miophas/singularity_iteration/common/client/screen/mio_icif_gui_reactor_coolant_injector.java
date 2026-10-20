package com.miophas.singularity_iteration.common.client.screen;

import com.miophas.singularity_iteration.common.menu.producer.ReactorCoolantInjectorMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/**
 * GUI of the coolant injectors, IC2 1.12 {@code rci_rsh.xml} / {@code rci_lzh.xml}: 176 x 166, the
 * 3 x 3 coolant grid at (61, 16), the energy gauge at (7, 55), the upgrades in the shared dock. The
 * reserve of an injector saved before 0.1.10.14 shows on the inventory label row while it lasts.
 */
@OnlyIn(Dist.CLIENT)
@SuppressWarnings("null")
public class mio_icif_gui_reactor_coolant_injector<M extends ReactorCoolantInjectorMenu> extends mio_icif_screen<M> {

    private static final ResourceLocation GUI_TEXTURE =
        ResourceLocation.fromNamespaceAndPath("mio_icif", "textures/gui/gui_reactor_coolant_injector.png");
    private static final int GUI_WIDTH = 176;
    private static final int GUI_HEIGHT = 166;
    private static final int ENERGY_X = 9;
    private static final int ENERGY_Y = 57;

    public mio_icif_gui_reactor_coolant_injector(M menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = GUI_WIDTH;
        this.imageHeight = GUI_HEIGHT;
        this.inventoryLabelY = this.imageHeight - 94;
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        guiGraphics.blit(GUI_TEXTURE, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight);
        drawLightningEnergy(guiGraphics, this.leftPos + ENERGY_X, this.topPos + ENERGY_Y, menu.getEnergy(), menu.getMaxEnergy());
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        super.renderLabels(guiGraphics, mouseX, mouseY);
        if (menu.getReserveCount() > 0) {
            drawFittedRight(guiGraphics, this.font, Component.translatable("gui.mio_icif.reactor_coolant_injector.reserve", menu.getReserveCount()),
                this.imageWidth - 8, this.inventoryLabelY, 90, 0x404040, false);
        }
        if (isHovering(mouseX, mouseY, this.leftPos + ENERGY_X, this.topPos + ENERGY_Y, LIGHTNING_WIDTH, LIGHTNING_HEIGHT)) {
            renderEnergyTooltip(guiGraphics, mouseX - this.leftPos, mouseY - this.topPos, menu.getEnergy(), menu.getMaxEnergy());
        }
    }
}
