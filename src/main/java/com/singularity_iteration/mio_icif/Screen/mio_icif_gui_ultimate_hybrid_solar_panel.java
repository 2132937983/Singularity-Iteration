package com.singularity_iteration.mio_icif.Screen;

import com.singularity_iteration.mio_icif.Menu.Generator.UltimateHybridSolarPanelMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
@SuppressWarnings("null")
public class mio_icif_gui_ultimate_hybrid_solar_panel extends mio_icif_screen<UltimateHybridSolarPanelMenu> {

    private static final ResourceLocation GUI_TEXTURE =
        ResourceLocation.parse("mio_icif:textures/gui/gui_advanced_solar_generator.png");

    private static final int GUI_WIDTH = 176;
    private static final int GUI_HEIGHT = 166;

    // EU能量条位置
    private static final int ENERGY_BAR_X = 39;
    private static final int ENERGY_BAR_Y = 59;

    // 太阳标志位置 - 与太阳能发电机一致
    private static final int SUN_ICON_X = 80;
    private static final int SUN_ICON_Y = 46;
    private static final int SUN_ICON_WIDTH = 14;
    private static final int SUN_ICON_HEIGHT = 14;

    // Atlas 纹理中太阳标志的 UV 坐标
    private static final int SUN_TEXTURE_U = 200;
    private static final int SUN_TEXTURE_V = 227;

    public mio_icif_gui_ultimate_hybrid_solar_panel(UltimateHybridSolarPanelMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = GUI_WIDTH;
        this.imageHeight = GUI_HEIGHT;
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;

        guiGraphics.blit(GUI_TEXTURE, x, y, 0, 0, this.imageWidth, this.imageHeight);

        UltimateHybridSolarPanelMenu menu = this.getMenu();
        if (menu != null) {
            // 绘制 EU 能量条
            int energyProgressPixels = menu.getEnergyProgressPixels();
            drawKineticEnergyBar(guiGraphics, x + ENERGY_BAR_X, y + ENERGY_BAR_Y, energyProgressPixels);

            if (menu.isGenerating()) {
                guiGraphics.blit(ATLAS_TEXTURE, x + SUN_ICON_X, y + SUN_ICON_Y, 0, 
                    (float) SUN_TEXTURE_U, (float) SUN_TEXTURE_V, 
                    SUN_ICON_WIDTH, SUN_ICON_HEIGHT, ATLAS_WIDTH, ATLAS_HEIGHT);
            }
        }
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        super.renderLabels(guiGraphics, mouseX, mouseY);

        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;

        UltimateHybridSolarPanelMenu menu = this.getMenu();
        if (menu == null) return;

        // 检查鼠标是否在能量条区域
        if (isHovering(mouseX, mouseY, x + ENERGY_BAR_X, y + ENERGY_BAR_Y, KINETIC_ENERGY_BAR_WIDTH, KINETIC_ENERGY_BAR_HEIGHT)) {
            guiGraphics.renderTooltip(this.font,
                Component.literal("Energy: " + menu.getEnergy() + "/" + menu.getMaxEnergy() + " EU"),
                mouseX - x, mouseY - y);
        }

        if (mouseX >= x + SUN_ICON_X && mouseX <= x + SUN_ICON_X + SUN_ICON_WIDTH &&
            mouseY >= y + SUN_ICON_Y && mouseY <= y + SUN_ICON_Y + SUN_ICON_HEIGHT) {
            Component tooltip;
            if (menu.isGenerating()) {
                if (menu.isDaytimeGeneration()) {
                    tooltip = Component.literal("Generating: 512 EU/t (Day)");
                } else {
                    tooltip = Component.literal("Generating: 64 EU/t (Night)");
                }
            } else {
                tooltip = Component.literal("Not Generating");
            }
            guiGraphics.renderTooltip(this.font, tooltip, mouseX - x, mouseY - y);
        }
    }
}