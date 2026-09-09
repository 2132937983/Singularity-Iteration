package com.singularity_iteration.mio_icif.Screen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/**
 * 风力动能发生机的 GUI类
 * 显示转子槽、风力强度和动能输出信息
 */
@OnlyIn(Dist.CLIENT)
@SuppressWarnings("null")
public class mio_icif_gui_wind_kinetic_generator extends mio_icif_screen<com.singularity_iteration.mio_icif.Menu.Generator.WindKineticGeneratorMenu> {

    private static final ResourceLocation GUI_TEXTURE =
        ResourceLocation.parse("mio_icif:textures/gui/gui_wind_kinetic_generator.png");

    // GUI 尺寸
    private static final int GUI_WIDTH = 176;
    private static final int GUI_HEIGHT = 166;

    // IC2 原版 GUI 的两行状态文本位置
    private static final int STATUS_TEXT_X = 17;
    private static final int PRIMARY_STATUS_TEXT_Y = 48;
    private static final int SECONDARY_STATUS_TEXT_Y = 66;
    private static final int INFO_TEXT_COLOR = 0x20eb3e;

    public mio_icif_gui_wind_kinetic_generator(com.singularity_iteration.mio_icif.Menu.Generator.WindKineticGeneratorMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = GUI_WIDTH;
        this.imageHeight = GUI_HEIGHT;
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int x = this.leftPos;
        int y = this.topPos;

        // 绘制背景
        guiGraphics.blit(GUI_TEXTURE, x, y, 0, 0, this.imageWidth, this.imageHeight);

        com.singularity_iteration.mio_icif.Menu.Generator.WindKineticGeneratorMenu menu = this.menu;
        if (menu != null) {
            Component primaryText = getPrimaryStatusText(menu);
            if (primaryText != null) {
                guiGraphics.drawString(this.font, primaryText, x + STATUS_TEXT_X, y + PRIMARY_STATUS_TEXT_Y, INFO_TEXT_COLOR);
            }

            Component secondaryText = getSecondaryStatusText(menu);
            if (secondaryText != null) {
                guiGraphics.drawString(this.font, secondaryText, x + STATUS_TEXT_X, y + SECONDARY_STATUS_TEXT_Y, INFO_TEXT_COLOR);
            }
        }
    }

    private Component getPrimaryStatusText(com.singularity_iteration.mio_icif.Menu.Generator.WindKineticGeneratorMenu menu) {
        return switch (menu.getStatus()) {
            case com.singularity_iteration.mio_icif.Blocks.entity.KUEntity.KUGenerator.mio_icif_Wind_Kinetic_Generator.STATUS_MISSING_ROTOR ->
                Component.translatable("gui.mio_icif.wind_kinetic.status.missing_rotor");
            case com.singularity_iteration.mio_icif.Blocks.entity.KUEntity.KUGenerator.mio_icif_Wind_Kinetic_Generator.STATUS_BLOCKED_ROTOR ->
                Component.translatable("gui.mio_icif.wind_kinetic.status.blocked_rotor");
            case com.singularity_iteration.mio_icif.Blocks.entity.KUEntity.KUGenerator.mio_icif_Wind_Kinetic_Generator.STATUS_WEAK_WIND ->
                Component.translatable("gui.mio_icif.wind_kinetic.status.weak_wind");
            case com.singularity_iteration.mio_icif.Blocks.entity.KUEntity.KUGenerator.mio_icif_Wind_Kinetic_Generator.STATUS_GENERATING ->
                Component.translatable("gui.mio_icif.wind_kinetic.status.output", menu.getKineticOutput());
            default -> null;
        };
    }

    private Component getSecondaryStatusText(com.singularity_iteration.mio_icif.Menu.Generator.WindKineticGeneratorMenu menu) {
        return switch (menu.getStatus()) {
            case com.singularity_iteration.mio_icif.Blocks.entity.KUEntity.KUGenerator.mio_icif_Wind_Kinetic_Generator.STATUS_WEAK_WIND ->
                Component.translatable("gui.mio_icif.wind_kinetic.status.weak_wind_detail");
            case com.singularity_iteration.mio_icif.Blocks.entity.KUEntity.KUGenerator.mio_icif_Wind_Kinetic_Generator.STATUS_GENERATING ->
                Component.translatable("gui.mio_icif.wind_kinetic.status.rotor_health", menu.getRotorHealthPercent());
            default -> null;
        };
    }
}