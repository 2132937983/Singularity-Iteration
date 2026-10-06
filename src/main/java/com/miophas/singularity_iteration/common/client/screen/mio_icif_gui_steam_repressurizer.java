package com.miophas.singularity_iteration.common.client.screen;

import com.miophas.singularity_iteration.common.menu.producer.SteamRepressurizerMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.fluids.FluidStack;

/** Port of IC2's steam_repressurizer.xml layout (176 x 166). */
@OnlyIn(Dist.CLIENT)
@SuppressWarnings("null")
public class mio_icif_gui_steam_repressurizer extends AbstractContainerScreen<SteamRepressurizerMenu> {
    private static final ResourceLocation TEXTURE =
        ResourceLocation.fromNamespaceAndPath("mio_icif", "textures/gui/gui_steam_repressurizer.png");
    private static final int INPUT_X = 15, OUTPUT_X = 123, TANK_Y = 19;
    private static final int TANK_WIDTH = 38, TANK_HEIGHT = 47;

    public mio_icif_gui_steam_repressurizer(SteamRepressurizerMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 176;
        imageHeight = 166;
    }
    @Override protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        g.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight);
        renderTank(g, INPUT_X, menu.getInputFluid(), menu.getInputCapacity());
        renderTank(g, OUTPUT_X, menu.getOutputFluid(), menu.getOutputCapacity());
    }
    private void renderTank(GuiGraphics g, int x, FluidStack fluid, int capacity) {
        if (fluid.isEmpty()) return;
        // Legacy balances may exceed the new capacity: keep them, cap only the visual level.
        mio_icif_GuiUtils.renderFluidBar(g, leftPos + x, topPos + TANK_Y, TANK_WIDTH, TANK_HEIGHT,
            fluid.copyWithAmount(Math.min(fluid.getAmount(), capacity)), capacity);
    }
    @Override protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
        g.drawString(font, title, (imageWidth - font.width(title)) / 2, 6, 0x404040, false);
    }
    @Override public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        renderTooltip(g, mouseX, mouseY);
        tankTooltip(g, mouseX, mouseY, INPUT_X, menu.getInputFluid(), menu.getInputAmount(), menu.getInputCapacity());
        tankTooltip(g, mouseX, mouseY, OUTPUT_X, menu.getOutputFluid(), menu.getOutputAmount(), menu.getOutputCapacity());
    }
    private void tankTooltip(GuiGraphics g, int mouseX, int mouseY, int x, FluidStack fluid, int amount, int capacity) {
        if (!isHovering(x, TANK_Y, TANK_WIDTH, TANK_HEIGHT, mouseX, mouseY)) return;
        Component text = Component.translatable("gui.mio_icif.steam_repressurizer.tank",
            fluid.isEmpty() ? Component.translatable("gui.mio_icif.steam_repressurizer.empty") : fluid.getHoverName(),
            amount, capacity);
        g.renderTooltip(font, text, mouseX, mouseY);
    }
}
