package com.singularity_iteration.mio_icif.Screen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
@SuppressWarnings("null")
public class mio_icif_gui_solar_generator extends mio_icif_screen<com.singularity_iteration.mio_icif.Menu.Generator.SolarGeneratorMenu> {

    private static final ResourceLocation GUI_TEXTURE =
        ResourceLocation.parse("mio_icif:textures/gui/gui_solar_generator.png");

    private static final int GUI_WIDTH = 176;
    private static final int GUI_HEIGHT = 166;

    // 太阳标志位置 - 与高级太阳能发电机一致
    private static final int SUN_ICON_X = 80;
    private static final int SUN_ICON_Y = 46;
    private static final int SUN_ICON_WIDTH = 14;
    private static final int SUN_ICON_HEIGHT = 14;

    // Atlas 纹理中太阳标志的 UV 坐标
    private static final int SUN_TEXTURE_U = 200;
    private static final int SUN_TEXTURE_V = 227;

    public mio_icif_gui_solar_generator(com.singularity_iteration.mio_icif.Menu.Generator.SolarGeneratorMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = GUI_WIDTH;
        this.imageHeight = GUI_HEIGHT;
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;

        guiGraphics.blit(GUI_TEXTURE, x, y, 0, 0, this.imageWidth, this.imageHeight);

        com.singularity_iteration.mio_icif.Menu.Generator.SolarGeneratorMenu menu = this.getMenu();
        if (menu != null && menu.isGenerating()) {
            guiGraphics.blit(ATLAS_TEXTURE, x + SUN_ICON_X, y + SUN_ICON_Y, 0, 
                (float) SUN_TEXTURE_U, (float) SUN_TEXTURE_V, 
                SUN_ICON_WIDTH, SUN_ICON_HEIGHT, ATLAS_WIDTH, ATLAS_HEIGHT);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        super.renderLabels(guiGraphics, mouseX, mouseY);

        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;

        com.singularity_iteration.mio_icif.Menu.Generator.SolarGeneratorMenu menu = this.getMenu();
        if (menu == null) return;

        if (mouseX >= x + SUN_ICON_X && mouseX <= x + SUN_ICON_X + SUN_ICON_WIDTH &&
            mouseY >= y + SUN_ICON_Y && mouseY <= y + SUN_ICON_Y + SUN_ICON_HEIGHT) {
            Component tooltip = menu.isGenerating() ?
                Component.literal("Generating: 1 EU/t") :
                Component.literal("Not Generating");
            guiGraphics.renderTooltip(this.font, tooltip, mouseX - x, mouseY - y);
        }
    }

    @Override
    public void containerTick() {
        super.containerTick();
    }
}