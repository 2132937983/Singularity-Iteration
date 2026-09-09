package com.singularity_iteration.mio_icif.Screen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/**
 * å°ç­åçµæºç GUI ç±?
 */
@OnlyIn(Dist.CLIENT)
@SuppressWarnings("null")
public class mio_icif_gui_geo_generator extends mio_icif_screen<com.singularity_iteration.mio_icif.Menu.Generator.GeoGeneratorMenu> {

    private static final ResourceLocation GUI_TEXTURE =
        ResourceLocation.parse("mio_icif:textures/gui/gui_fluid_generator.png");

    // GUI å°ºå¯¸
    private static final int GUI_WIDTH = 176;
    private static final int GUI_HEIGHT = 166;

    // æ§½ä½ä½ç½®
    /* private static final int LAVA_BUCKET_SLOT_X = 27; */
    /* private static final int LAVA_BUCKET_SLOT_Y = 21; */
    /* private static final int EMPTY_BUCKET_SLOT_X = 27; */
    /* private static final int EMPTY_BUCKET_SLOT_Y = 54; */
    /* private static final int BATTERY_SLOT_X = 117; */
    /* private static final int BATTERY_SLOT_Y = 49; */

    // 能量条位置
    private static final int ENERGY_BAR_X = 112;
    private static final int ENERGY_BAR_Y = 25;

    // å²©æµæ§½èæ¯ä½ç½?
    private static final int LAVA_TANK_BG_X = 70;
    private static final int LAVA_TANK_BG_Y = 20;
    private static final int LAVA_TANK_BG_WIDTH = 20;
    private static final int LAVA_TANK_BG_HEIGHT = 55;

    // å²©æµæ§½çº¹çä½ç½®ï¼ç©ºæ§½ï¼?
// 岩浆槽纹理位置（空槽）
    @SuppressWarnings("unused")
    private static final int LAVA_TANK_TEXTURE_X = 176;
    @SuppressWarnings("unused")
    private static final int LAVA_TANK_TEXTURE_Y = 17;

    // å²©æµæè´¨ä½ç½®
    private static final int LAVA_TANK_X = 74;
    private static final int LAVA_TANK_Y = 24;
    private static final int LAVA_TANK_WIDTH = 12;
    private static final int LAVA_TANK_HEIGHT = 47;

    // å»åº¦çº¹çä½ç½®
// 刻度纹理位置
    @SuppressWarnings("unused")
    private static final int SCALE_TEXTURE_X = 176;
    @SuppressWarnings("unused")
    private static final int SCALE_TEXTURE_Y = 72;
    private static final int SCALE_WIDTH = 20;
    private static final int SCALE_HEIGHT = 46;
    private static final int SCALE_X = 74;
    private static final int SCALE_Y = 24;

    public mio_icif_gui_geo_generator(com.singularity_iteration.mio_icif.Menu.Generator.GeoGeneratorMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = GUI_WIDTH;
        this.imageHeight = GUI_HEIGHT;
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;

        // ç»å¶èæ¯
        guiGraphics.blit(GUI_TEXTURE, x, y, 0, 0, this.imageWidth, this.imageHeight);

        com.singularity_iteration.mio_icif.Menu.Generator.GeoGeneratorMenu menu = this.menu;
        if (menu != null) {
            // ç»å¶å²©æµæ§½èæ¯ï¼ç©ºæ§½ï¼?
guiGraphics.blit(ATLAS_TEXTURE, x + LAVA_TANK_BG_X, y + LAVA_TANK_BG_Y, 0, (float) 82, (float) 2, LAVA_TANK_BG_WIDTH, LAVA_TANK_BG_HEIGHT, ATLAS_WIDTH, ATLAS_HEIGHT);

            // ç»å¶å²©æµï¼ä»ä¸å¾ä¸å¡«åï¼
            int lavaAmount = menu.getLavaAmount();
            int lavaCapacity = menu.getLavaCapacity();
            if (lavaAmount > 0 && lavaCapacity > 0) {
                mio_icif_GuiUtils.renderFluidBar(guiGraphics, x + LAVA_TANK_X, y + LAVA_TANK_Y,
                    LAVA_TANK_WIDTH, LAVA_TANK_HEIGHT,
                    new FluidStack(Fluids.LAVA, lavaAmount), lavaCapacity);
            }

            // ç»å¶å»åº¦ï¼è´´å¨å²©æµæ§½ä¸ï¼
guiGraphics.blit(ATLAS_TEXTURE, x + SCALE_X, y + SCALE_Y, 0, (float) 216, (float) 2, SCALE_WIDTH, SCALE_HEIGHT, ATLAS_WIDTH, ATLAS_HEIGHT);

            // 绘制能量条
            int energyProgressPixels = menu.getEnergyProgressPixels();
            drawEnergyBar(guiGraphics, x + ENERGY_BAR_X, y + ENERGY_BAR_Y, energyProgressPixels);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        super.renderLabels(guiGraphics, mouseX, mouseY);

        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;

        com.singularity_iteration.mio_icif.Menu.Generator.GeoGeneratorMenu menu = this.menu;
        if (menu == null) return;

        // æ£æ¥é¼ æ æ¯å¦å¨è½éæ¡åºå?
        if (mouseX >= x + ENERGY_BAR_X && mouseX <= x + ENERGY_BAR_X + ENERGY_BAR_WIDTH &&
            mouseY >= y + ENERGY_BAR_Y && mouseY <= y + ENERGY_BAR_Y + ENERGY_BAR_HEIGHT) {
            guiGraphics.renderTooltip(this.font,
                Component.literal("Energy: " + menu.getEnergy() + "/" + menu.getMaxEnergy() + " EU"),
                mouseX - x, mouseY - y);
        }

        // æ£æ¥é¼ æ æ¯å¦å¨å²©æµæ§½åºå?
        if (mouseX >= x + LAVA_TANK_X && mouseX <= x + LAVA_TANK_X + LAVA_TANK_WIDTH &&
            mouseY >= y + LAVA_TANK_Y && mouseY <= y + LAVA_TANK_Y + LAVA_TANK_HEIGHT) {
            guiGraphics.renderTooltip(this.font,
                Component.literal(net.minecraft.world.level.material.Fluids.LAVA.getFluidType().getDescription().getString() + ": " + menu.getLavaAmount() + "/" + menu.getLavaCapacity() + " mB"),
                mouseX - x, mouseY - y);
        }
    }
}