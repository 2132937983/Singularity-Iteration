package com.singularity_iteration.mio_icif.Screen;

import com.singularity_iteration.mio_icif.Menu.Generator.QuantumGeneratorMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
@SuppressWarnings("null")
public class mio_icif_gui_quantum_generator extends mio_icif_screen<QuantumGeneratorMenu> {

    private static final ResourceLocation GUI_TEXTURE =
        ResourceLocation.parse("mio_icif:textures/gui/gui_solar_generator.png");

    private static final int GUI_WIDTH = 176;
    private static final int GUI_HEIGHT = 166;

    private static final int SUN_ICON_X = 80;
    private static final int SUN_ICON_Y = 46;
    private static final int SUN_ICON_WIDTH = 14;
    private static final int SUN_ICON_HEIGHT = 14;

    private static final int SUN_TEXTURE_X = 176;
    private static final int SUN_TEXTURE_Y = 0;

    public mio_icif_gui_quantum_generator(QuantumGeneratorMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = GUI_WIDTH;
        this.imageHeight = GUI_HEIGHT;
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;

        guiGraphics.blit(GUI_TEXTURE, x, y, 0, 0, this.imageWidth, this.imageHeight);

        QuantumGeneratorMenu menu = this.getMenu();
        if (menu != null && menu.isGenerating()) {
            guiGraphics.blit(GUI_TEXTURE, x + SUN_ICON_X, y + SUN_ICON_Y, SUN_TEXTURE_X, SUN_TEXTURE_Y, SUN_ICON_WIDTH, SUN_ICON_HEIGHT);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        super.renderLabels(guiGraphics, mouseX, mouseY);

        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;

        QuantumGeneratorMenu menu = this.getMenu();
        if (menu == null) return;

        if (mouseX >= x + SUN_ICON_X && mouseX <= x + SUN_ICON_X + SUN_ICON_WIDTH &&
            mouseY >= y + SUN_ICON_Y && mouseY <= y + SUN_ICON_Y + SUN_ICON_HEIGHT) {
            Component tooltip;
            if (menu.isGenerating()) {
                tooltip = Component.literal("Generating: " + menu.getProduction() + " EU/t (Tier " + menu.getTier() + ")");
            } else {
                tooltip = Component.literal("Not Generating");
            }
            guiGraphics.renderTooltip(this.font, tooltip, mouseX - x, mouseY - y);
        }
    }
}