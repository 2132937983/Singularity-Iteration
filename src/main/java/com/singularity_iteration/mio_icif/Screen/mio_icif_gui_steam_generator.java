package com.singularity_iteration.mio_icif.Screen;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.fluids.FluidStack;

/**
 * è¸æ±½æ?GUI ç±»ï¼æ?IC2 åç GuiSteamGenerator é£æ ¼å¸å±ï¼?
 *
 * çº¹ç 176x220ï¼çº¯æ¾ç¤ºçé¢ï¼æ ç©åæ§½ä½ï¼æ ç©å®¶èåæ ã?
 */
@OnlyIn(Dist.CLIENT)
@SuppressWarnings("null")
public class mio_icif_gui_steam_generator extends mio_icif_screen<com.singularity_iteration.mio_icif.Menu.Producer.SteamGeneratorMenu> {

    private static final ResourceLocation GUI_TEXTURE =
        ResourceLocation.parse("mio_icif:textures/gui/gui_steam_generator_elc.png");

    private static final int GUI_WIDTH = 176;
    private static final int GUI_HEIGHT = 220;

    // IC2 åçææ¬é¢è²
    private static final int TEXT_COLOR = 0x20EC1E;

    // æ°´æ§½åºåï¼ç¸å¯¹äº GUI å·¦ä¸è§ï¼
    private static final int WATER_TANK_X = 10;
    private static final int WATER_TANK_Y = 155;  // 202 - 47ï¼åºé¨åæ ?- é«åº¦ï¼?
    private static final int WATER_TANK_WIDTH = 75;
    private static final int WATER_TANK_HEIGHT = 47;

    public mio_icif_gui_steam_generator(com.singularity_iteration.mio_icif.Menu.Producer.SteamGeneratorMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = GUI_WIDTH;
        this.imageHeight = GUI_HEIGHT;
    }

    /**
     * åç IC2 é£æ ¼çé¼ æ ç¹å»å¤çï¼ç¡¬ç¼ç åæ æ£æµæé®ï¼
     */
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && this.minecraft != null && this.minecraft.gameMode != null) {
            int xMin = (this.width - this.imageWidth) / 2;
            int yMin = (this.height - this.imageHeight) / 2;
            int x = (int) mouseX - xMin;
            int y = (int) mouseY - yMin;

            // å³ä¾§æµéè°èæé®ï¼åï¼? 4ç»ï¼x = 92,102,112,122ï¼y = 186
            if      (x >= 92 && y >= 186 && x < 101 && y < 195) sendButtonEvent(-1000);
            else if (x >= 102 && y >= 186 && x < 111 && y < 195) sendButtonEvent(-100);
            else if (x >= 112 && y >= 186 && x < 121 && y < 195) sendButtonEvent(-10);
            else if (x >= 122 && y >= 186 && x < 131 && y < 195) sendButtonEvent(-1);
            // å³ä¾§æµéè°èæé®ï¼å ï¼? 4ç»ï¼x = 122,112,102,92ï¼y = 162
            else if (x >= 122 && y >= 162 && x < 131 && y < 171) sendButtonEvent(1);
            else if (x >= 112 && y >= 162 && x < 121 && y < 171) sendButtonEvent(10);
            else if (x >= 102 && y >= 162 && x < 111 && y < 171) sendButtonEvent(100);
            else if (x >= 92 && y >= 162 && x < 101 && y < 171) sendButtonEvent(1000);
            // å·¦ä¾§ååè°èæé®ï¼åï¼? 3ç»ï¼x = 23,33,43ï¼y = 49
            else if (x >= 23 && y >= 49 && x < 32 && y < 58) sendButtonEvent(-2100);
            else if (x >= 33 && y >= 49 && x < 42 && y < 58) sendButtonEvent(-2010);
            else if (x >= 43 && y >= 49 && x < 52 && y < 58) sendButtonEvent(-2001);
            // å·¦ä¾§ååè°èæé®ï¼å ï¼? 3ç»ï¼x = 43,33,23ï¼y = 25
            else if (x >= 43 && y >= 25 && x < 52 && y < 34) sendButtonEvent(2001);
            else if (x >= 33 && y >= 25 && x < 42 && y < 34) sendButtonEvent(2010);
            else if (x >= 23 && y >= 25 && x < 32 && y < 34) sendButtonEvent(2100);
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private void sendButtonEvent(int eventId) {
        if (this.minecraft == null || this.minecraft.gameMode == null) return;
        // åéå°æå¡å¨å¹¶å¨å®¢æ·ç«¯ç«å³ååº
        this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, eventId);
        this.menu.clickMenuButton(this.minecraft.player, eventId);
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;

        guiGraphics.blit(GUI_TEXTURE, x, y, 0, 0, this.imageWidth, this.imageHeight);

        com.singularity_iteration.mio_icif.Menu.Producer.SteamGeneratorMenu menu = this.menu;
        if (menu == null) return;

        // ç»å¶ç³»ç»ç­éç«æ¡ï¼åçä½ç½?x=13, y=70, å®?, é«?6ï¼?
        int heatPixels = menu.getGaugeHeatScaled76();
        if (heatPixels > 0) {
guiGraphics.blit(ATLAS_TEXTURE, x + 13, y + 70 + 76 - heatPixels, 0, (float) 2, (float) 2 + 76 - heatPixels, 7, heatPixels, ATLAS_WIDTH, ATLAS_HEIGHT);
        }

        // ç»å¶éåæ¡ï¼åçä½ç½® x=155, y=61, å®?, é«?8ï¼?
        int calcPixels = menu.getGaugeCalcificationScaled58();
        if (calcPixels > 0) {
guiGraphics.blit(ATLAS_TEXTURE, x + 155, y + 61 + 58 - calcPixels, 0, (float) 29, (float) 2 + 58 - calcPixels, 7, calcPixels, ATLAS_WIDTH, ATLAS_HEIGHT);
        }

        // ç»å¶æ°´æ§½ï¼æè´¨å¹³éºæ¸²æï¼
        renderFluidBar(guiGraphics, x + WATER_TANK_X, y + WATER_TANK_Y,
            WATER_TANK_WIDTH, WATER_TANK_HEIGHT,
            new FluidStack(Fluids.WATER, menu.getWaterAmount()),
            menu.getWaterCapacity());

        // ç»å¶æé®é«äº®ææ
        renderButtonHighlights(guiGraphics, x, y, mouseX, mouseY);
    }

    /**
     * ç»å¶æé®é«äº®ææ
     */
    private void renderButtonHighlights(GuiGraphics guiGraphics, int guiX, int guiY, int mouseX, int mouseY) {
        int relX = mouseX - guiX;
        int relY = mouseY - guiY;

        // æ£æµæææé®å¹¶ç»å¶é«äº®
        int[][] allButtons = {
            // å³ä¾§æµéè°èæé®ï¼åï¼
            {92, 186}, {102, 186}, {112, 186}, {122, 186},
            // å³ä¾§æµéè°èæé®ï¼å ï¼
            {122, 162}, {112, 162}, {102, 162}, {92, 162},
            // å·¦ä¾§ååè°èæé®ï¼åï¼
            {23, 49}, {33, 49}, {43, 49},
            // å·¦ä¾§ååè°èæé®ï¼å ï¼
            {43, 25}, {33, 25}, {23, 25}
        };

        for (int[] btn : allButtons) {
            if (relX >= btn[0] && relX < btn[0] + 9 &&
                relY >= btn[1] && relY < btn[1] + 9) {
                guiGraphics.fill(guiX + btn[0], guiY + btn[1],
                    guiX + btn[0] + 9, guiY + btn[1] + 9,
                    0x80FFFFFF); // åéæç½è²é«äº®
            }
        }
    }

    /**
     * å?GUI ä¸­å¹³éºæ¸²ææµä½æ¶²ä½æ¡ï¼æ¥è?docs/gui_fluid_bar_rendering.mdï¼?
     */
    private void renderFluidBar(GuiGraphics guiGraphics, int x, int y, int width, int height,
                                FluidStack fluid, int capacity) {
        if (fluid.isEmpty() || capacity <= 0) return;

        // æ ¹æ®å½åæµä½å æ¯è®¡ç®å®éå¡«åé«åº¦ï¼ä»ä¸å¾ä¸ï¼
        int fillHeight = (int) ((float) fluid.getAmount() / capacity * height);
        if (fillHeight <= 0) return;

        // è·åå®¢æ·ç«¯æµä½æ©å±ï¼ç¨äºè¯»å still çº¹çä¸çè?
        IClientFluidTypeExtensions extensions = IClientFluidTypeExtensions.of(fluid.getFluidType());
        @SuppressWarnings("deprecation")
        TextureAtlasSprite sprite = Minecraft.getInstance()
            .getTextureAtlas(TextureAtlas.LOCATION_BLOCKS)
            .apply(extensions.getStillTexture());

        // è¯»åæµä½çè²ï¼ARGBï¼å¹¶æåä¸?RGBA åé
        int color = extensions.getTintColor(fluid);
        float a = ((color >> 24) & 0xFF) / 255f;
        float r = ((color >> 16) & 0xFF) / 255f;
        float g = ((color >> 8) & 0xFF) / 255f;
        float b = (color & 0xFF) / 255f;

        // æ¯ä¸ªå¹³éºååçå¤§å°ï¼éå¸¸ä¸?16x16
        int tileSize = sprite.contents().width();

        // å¡«ååºåé¡¶é¨ y åæ ï¼æ»é«åº?- å¡«åé«åº¦ï¼?
        int fillTop = y + height - fillHeight;

        // åºç¨æµä½é¢è²
        guiGraphics.setColor(r, g, b, a);

        // ä½¿ç¨è£åªéå®åªç»å¶å¨å¡«ååºåå?
        guiGraphics.enableScissor(x, fillTop, x + width, y + height);

        // å¹³éºç»å¶æµä½ç²¾çµ
        for (int tileY = fillTop; tileY < y + height; tileY += tileSize) {
            for (int tileX = x; tileX < x + width; tileX += tileSize) {
                guiGraphics.blit(tileX, tileY, 0, tileSize, tileSize, sprite);
            }
        }

        // æ¢å¤è£åªä¸é¢è?
        guiGraphics.disableScissor();
        guiGraphics.setColor(1f, 1f, 1f, 1f);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        super.renderLabels(guiGraphics, mouseX, mouseY);

        @SuppressWarnings("unused")
        int x = (this.width - this.imageWidth) / 2;
        @SuppressWarnings("unused")
        int y = (this.height - this.imageHeight) / 2;

        com.singularity_iteration.mio_icif.Menu.Producer.SteamGeneratorMenu menu = this.menu;
        if (menu == null) return;

        // ======== æå­æ¸²æï¼åæ ç¸å¯¹äº GUI å·¦ä¸è§ï¼ ========
        guiGraphics.drawString(this.font,
            Component.translatable("gui.mio_icif.steam_generator.heat_input", menu.getLastHeatInput()),
            40, 136, TEXT_COLOR);
        guiGraphics.drawString(this.font,
            Component.literal("Pressure: " + menu.getPressure()),
            25, 38, TEXT_COLOR);
        guiGraphics.drawString(this.font,
            Component.literal(menu.getInputMB() + "mB/t"),
            92, 175, TEXT_COLOR);
        guiGraphics.drawString(this.font,
            Component.literal(menu.getOutputMB() + "mB/t"),
            70, 29, TEXT_COLOR);
        guiGraphics.drawString(this.font,
            Component.translatable(menu.getOutputFluidTranslationKey()),
            70, 49, TEXT_COLOR);

        // ======== Tooltip æ£æµï¼ä½¿ç¨ç¸å¯¹ GUI åæ ï¼å ä¸?GuiGraphics å·²å¹³ç§»è³ GUI å·¦ä¸è§ï¼ ========
        int relX = mouseX - this.leftPos;
        int relY = mouseY - this.topPos;

        // ç³»ç»ç­éæ?(x+13,y+70) 7x76
        if (isMouseOver(relX, relY, 13, 70, 7, 76)) {
            guiGraphics.renderTooltip(this.font,
                Component.translatable("gui.mio_icif.steam_generator.system_heat", menu.getSystemHeatFloat()),
                relX, relY);
        }
        // éåæ?(x+155,y+61) 7x58
        else if (isMouseOver(relX, relY, 155, 61, 7, 58)) {
            guiGraphics.renderTooltip(this.font,
                Component.literal("Calcification: " + menu.getCalcificationPercent() + "%"),
                relX, relY);
        }
        // æ°´æ§½ (x+10, y+155) 75x47
        else if (isMouseOver(relX, relY, WATER_TANK_X, WATER_TANK_Y, WATER_TANK_WIDTH, WATER_TANK_HEIGHT)) {
            guiGraphics.renderTooltip(this.font,
                Component.literal(net.minecraft.world.level.material.Fluids.WATER.getFluidType().getDescription().getString() + ": " + menu.getWaterAmount() + "/" + menu.getWaterCapacity() + " mB"),
                relX, relY);
        }
        // "Heat Input: X" ææ¬åºå (40,136)
        else if (isMouseOver(relX, relY, 40, 136, 90, 10)) {
            guiGraphics.renderTooltip(this.font,
                Component.translatable("gui.mio_icif.steam_generator.heat_input", menu.getLastHeatInput()),
                relX, relY);
        }
        // "Pressure: X" ææ¬åºå (25,38)
        else if (isMouseOver(relX, relY, 25, 38, 75, 10)) {
            guiGraphics.renderTooltip(this.font,
                Component.literal("Pressure Valve: " + menu.getPressure()),
                relX, relY);
        }
        // è¾åºæµéææ¬åºå?(70,29)
        else if (isMouseOver(relX, relY, 70, 29, 60, 10)) {
            guiGraphics.renderTooltip(this.font,
                Component.literal("Fluid Output: " + menu.getOutputMB() + "mB/t"),
                relX, relY);
        }
        // è¾åºæµä½åç§°ææ¬åºå (70,49)
        else if (isMouseOver(relX, relY, 70, 49, 80, 10)) {
            guiGraphics.renderTooltip(this.font,
                Component.translatable(menu.getOutputFluidTranslationKey()),
                relX, relY);
        }
        // è¾å¥æµéææ¬åºå?(92,175)
        else if (isMouseOver(relX, relY, 92, 175, 60, 10)) {
            guiGraphics.renderTooltip(this.font,
                Component.translatable("gui.mio_icif.steam_generator.water_input", menu.getInputMB()),
                relX, relY);
        }
        // å·¦ä¾§ååæé® Â± åºå
        else if (isMouseOver(relX, relY, 23, 25, 30, 32)) {
            guiGraphics.renderTooltip(this.font,
                Component.literal("Pressure: " + menu.getPressure() + " (click +/- to adjust)"),
                relX, relY);
        }
        // å³ä¾§æµéæé® Â± åºå
        else if (isMouseOver(relX, relY, 92, 162, 40, 32)) {
            guiGraphics.renderTooltip(this.font,
                Component.literal("Flow: " + menu.getInputMB() + " mB/t (click +/- to adjust)"),
                relX, relY);
        }
    }

    private boolean isMouseOver(int relX, int relY, int rx, int ry, int w, int h) {
        return relX >= rx && relX <= rx + w &&
               relY >= ry && relY <= ry + h;
    }
}