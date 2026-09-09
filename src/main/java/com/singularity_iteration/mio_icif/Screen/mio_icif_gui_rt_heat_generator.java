package com.singularity_iteration.mio_icif.Screen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import com.singularity_iteration.mio_icif.Menu.HUEntity.RTHeatGeneratorMenu;

/**
 * 放射性同位素温差加热机(RTG Heat Generator) 的GUI类
 */
@OnlyIn(Dist.CLIENT)
@SuppressWarnings("null")
public class mio_icif_gui_rt_heat_generator extends mio_icif_screen<com.singularity_iteration.mio_icif.Menu.HUEntity.RTHeatGeneratorMenu> {

    private static final ResourceLocation GUI_TEXTURE =
        ResourceLocation.parse("mio_icif:textures/gui/gui_rt_heat_generator.png");

    // GUI 尺寸
    private static final int GUI_WIDTH = 176;
    private static final int GUI_HEIGHT = 166;

    // 靶丸槽位位置 (6个槽位，2排3列，第一个槽位在62,27)
    /* private static final int SLOT_START_X = 62; */
    /* private static final int SLOT_START_Y = 27; */
    /* private static final int SLOT_SPACING_X = 18; */
    /* private static final int SLOT_SPACING_Y = 18; */

    // 热能条位置(119, 40)
    private static final int HEAT_BAR_X = 119;
    private static final int HEAT_BAR_Y = 40;
    // 热能条尺寸31x8
    private static final int HEAT_BAR_WIDTH = 31;
    private static final int HEAT_BAR_HEIGHT = 8;

    // 热能条纹理位置(179, 3)
    @SuppressWarnings("unused")
    private static final int HEAT_TEXTURE_X = 179;
    @SuppressWarnings("unused")
    private static final int HEAT_TEXTURE_Y = 3;

    // 产热量显示位置(51, 68)
    private static final int OUTPUT_TEXT_X = 51;
    private static final int OUTPUT_TEXT_Y = 68;

    public mio_icif_gui_rt_heat_generator(RTHeatGeneratorMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = GUI_WIDTH;
        this.imageHeight = GUI_HEIGHT;
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;

        // 绘制背景
        guiGraphics.blit(GUI_TEXTURE, x, y, 0, 0, this.imageWidth, this.imageHeight);

        RTHeatGeneratorMenu menu = this.getMenu();
        if (menu != null) {
            // 绘制热能条（从左往右填充）
            int heatProgress = menu.getHeatProgress();
            if (heatProgress > 0) {
                int heatWidth = (heatProgress * HEAT_BAR_WIDTH) / 100;
                guiGraphics.blit(ATLAS_TEXTURE, x + HEAT_BAR_X, y + HEAT_BAR_Y, 0, (float) 2, (float) 259, heatWidth, HEAT_BAR_HEIGHT, ATLAS_WIDTH, ATLAS_HEIGHT);
            }
        }
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        super.renderLabels(guiGraphics, mouseX, mouseY);

        RTHeatGeneratorMenu menu = this.getMenu();
        if (menu == null) return;

        // 显示产热量(119, 50)
        int output = menu.getHeatRate();
        Component outputText = Component.literal(output + " HU/t");
        guiGraphics.drawString(this.font, outputText, OUTPUT_TEXT_X, OUTPUT_TEXT_Y, 0x57c4da, false);

        // 检查鼠标是否在热能条区域（显示tooltip）
        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;
        if (mouseX >= x + HEAT_BAR_X && mouseX <= x + HEAT_BAR_X + HEAT_BAR_WIDTH &&
            mouseY >= y + HEAT_BAR_Y && mouseY <= y + HEAT_BAR_Y + HEAT_BAR_HEIGHT) {
            Component tooltip = Component.literal(menu.getHeatStored() + " / " + menu.getMaxHeatStored() + " HU");
            guiGraphics.renderTooltip(this.font, tooltip, mouseX, mouseY);
        }
    }

    @Override
    public void containerTick() {
        super.containerTick();
        // 数据通过 ContainerData 自动同步
    }


}