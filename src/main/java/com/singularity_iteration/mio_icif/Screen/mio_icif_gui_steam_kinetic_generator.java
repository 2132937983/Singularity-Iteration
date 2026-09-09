package com.singularity_iteration.mio_icif.Screen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/**
 * 蒸汽动能发生器的GUI类（参考IC2 原版 GuSteamKineticGenerator 风格布局）
 *
 * 槽位：涡轮叶片(80,26)、升级槽 (152,26)，加上玩家背包栏
 * 纹理 176x166
 */
@OnlyIn(Dist.CLIENT)
@SuppressWarnings("null")
public class mio_icif_gui_steam_kinetic_generator extends mio_icif_screen<com.singularity_iteration.mio_icif.Menu.Generator.SteamKineticGeneratorMenu> {

    private static final ResourceLocation GUI_TEXTURE =
        ResourceLocation.parse("mio_icif:textures/gui/gui_steam_kinetic_generator.png");

    private static final int GUI_WIDTH = 176;
    private static final int GUI_HEIGHT = 166;

    private static final int COLOR_ERROR = 0xE3A064;
    private static final int COLOR_ACTIVE = 0x20EC1E;

    public mio_icif_gui_steam_kinetic_generator(com.singularity_iteration.mio_icif.Menu.Generator.SteamKineticGeneratorMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = GUI_WIDTH;
        this.imageHeight = GUI_HEIGHT;
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;

        guiGraphics.blit(GUI_TEXTURE, x, y, 0, 0, this.imageWidth, this.imageHeight);

        com.singularity_iteration.mio_icif.Menu.Generator.SteamKineticGeneratorMenu menu = this.menu;
        if (menu == null) return;

        // 冷凝警告图标：原版位置(110, 20)，纹理(176, 0)，30x26
        if (!menu.isHotSteam() && menu.hasTurbine()) {
            guiGraphics.blit(ATLAS_TEXTURE, x + 110, y + 20, 0, (float) 2, (float) 164, 30, 26, ATLAS_WIDTH, ATLAS_HEIGHT);
        }

        // 绘制蒸馏水槽：原版位置(75, 47)，宽 26，高根据 gaugeLiquidScaled(26, 0)
        int liquidHeight = menu.getGaugeLiquidScaled26();
        if (liquidHeight > 0) {
            guiGraphics.fill(x + 75, y + 47 - liquidHeight, x + 75 + 26, y + 47, 0xFF3C44DB);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        super.renderLabels(guiGraphics, mouseX, mouseY);

        com.singularity_iteration.mio_icif.Menu.Generator.SteamKineticGeneratorMenu menu = this.menu;
        if (menu == null) return;

        int relX = mouseX - this.leftPos;
        int relY = mouseY - this.topPos;

        // IC2 原版风格状态文本
        if (!menu.hasTurbine()) {
            guiGraphics.drawString(this.font,
                Component.translatable("gui.mio_icif.steam_kinetic_generator.error.noturbine"),
                10, 55, COLOR_ERROR);
        } else if (menu.isTurbineFilledWithWater()) {
            guiGraphics.drawString(this.font,
                Component.translatable("gui.mio_icif.steam_kinetic_generator.error.filledupwithwater"),
                10, 55, COLOR_ERROR);
        } else {
            if (menu.isWorking()) {
                guiGraphics.drawString(this.font,
                    Component.translatable("gui.mio_icif.steam_kinetic_generator.active"),
                    10, 55, COLOR_ACTIVE);
            } else {
                guiGraphics.drawString(this.font,
                    Component.translatable("gui.mio_icif.steam_kinetic_generator.waiting"),
                    10, 55, COLOR_ACTIVE);
            }
            guiGraphics.drawString(this.font,
                Component.translatable("gui.mio_icif.steam_kinetic_generator.turbine.output", menu.getLastKineticOutput()),
                10, 71, COLOR_ACTIVE);
        }

        // Tooltip（相对坐标）
        // 冷凝警告 (110,20) 30x26
        if (isInside(relX, relY, 110, 20, 30, 26) && !menu.isHotSteam() && menu.hasTurbine()) {
            guiGraphics.renderTooltip(this.font,
                Component.translatable("gui.mio_icif.steam_kinetic_generator.condensationwarrning"),
                relX, relY);
        }
        // 涡轮槽(79,25) 18x18（物品槽标准大小）
        else if (isInside(relX, relY, 79, 25, 18, 18) && !menu.hasTurbine()) {
            guiGraphics.renderTooltip(this.font,
                Component.translatable("gui.mio_icif.steam_kinetic_generator.turbineslot"),
                relX, relY);
        }
        // 蒸馏水槽 (75, 21) 26x26
        else if (isInside(relX, relY, 75, 21, 26, 26) && menu.hasTurbine()) {
            guiGraphics.renderTooltip(this.font,
                Component.literal(com.singularity_iteration.mio_icif.Blocks.Environment.fluid.mio_icif_fluids.DISTILLEDWATER.get().getFluidType().getDescription().getString() + ": " + menu.getWaterAmount() + " mB"),
                relX, relY);
        }
    }

    private boolean isInside(int rx, int ry, int x, int y, int w, int h) {
        return rx >= x && rx <= x + w && ry >= y && ry <= y + h;
    }
}