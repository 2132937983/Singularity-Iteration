package com.singularity_iteration.mio_icif.Screen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/**
 * 电力动能机的 GUI类
 */
@OnlyIn(Dist.CLIENT)
public class mio_icif_gui_kinetic_generator_elc extends mio_icif_screen<com.singularity_iteration.mio_icif.Menu.KUEntity.KineticGeneratorElcMenu> {

    private static final ResourceLocation GUI_TEXTURE =
            ResourceLocation.parse("mio_icif:textures/gui/gui_kinetic_generator_elc.png");

    // GUI 尺寸
    private static final int GUI_WIDTH = 176;
    private static final int GUI_HEIGHT = 166;

    // 闪电标志位置
    private static final int ENERGY_ICON_X = 9;
    private static final int ENERGY_ICON_Y = 43;
    private static final int ENERGY_ICON_WIDTH = 14;
    private static final int ENERGY_ICON_HEIGHT = 15;

    // 闪电标志纹理位置 (72, 227)，uv(14, 15)
    private static final int ENERGY_ICON_TEXTURE_X = 72;
    private static final int ENERGY_ICON_TEXTURE_Y = 227;

    // 动能标志位置
    private static final int KINETIC_ICON_X = 106;
    private static final int KINETIC_ICON_Y = 36;
    private static final int KINETIC_ICON_WIDTH = 14;
    private static final int KINETIC_ICON_HEIGHT = 14;

    // 动能标志纹理位置
    private static final int KINETIC_ICON_TEXTURE_X = 184;
    private static final int KINETIC_ICON_TEXTURE_Y = 227;

    // 动能文本显示位置
    private static final int KINETIC_TEXT_X = 35;
    private static final int KINETIC_TEXT_Y = 68;
    // 动能文本颜色 (57c4da)
    private static final int KINETIC_TEXT_COLOR = 0x57c4da;

    public mio_icif_gui_kinetic_generator_elc(com.singularity_iteration.mio_icif.Menu.KUEntity.KineticGeneratorElcMenu menu, Inventory playerInventory, Component title) {
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

        com.singularity_iteration.mio_icif.Menu.KUEntity.KineticGeneratorElcMenu menu = this.menu;
        if (menu != null) {
            // 绘制电能标志（根据能量多少从上往下填充）
            int energy = menu.getEnergy();
            int maxEnergy = menu.getMaxEnergy();
            if (energy > 0 && maxEnergy > 0) {
                int energyHeight = (energy * ENERGY_ICON_HEIGHT) / maxEnergy;
                if (energyHeight > 0) {
                    int drawY = y + ENERGY_ICON_Y + ENERGY_ICON_HEIGHT - energyHeight;
                    int textureY = ENERGY_ICON_TEXTURE_Y + ENERGY_ICON_HEIGHT - energyHeight;
                    guiGraphics.blit(ATLAS_TEXTURE, x + ENERGY_ICON_X, drawY, 0, (float) ENERGY_ICON_TEXTURE_X, (float) textureY, ENERGY_ICON_WIDTH, energyHeight, ATLAS_WIDTH, ATLAS_HEIGHT);
                }
            }

            // 绘制动能标志（根据动能产生速率填充）
            int kineticGeneration = menu.isWorking() ? menu.getKineticGeneration() : 0;
            int maxKineticGeneration = menu.getMaxKineticGeneration();
            if (kineticGeneration > 0 && maxKineticGeneration > 0) {
                int kineticHeight = (kineticGeneration * KINETIC_ICON_HEIGHT) / maxKineticGeneration;
                if (kineticHeight > 0) {
                    int drawY = y + KINETIC_ICON_Y + KINETIC_ICON_HEIGHT - kineticHeight;
                    int textureY = KINETIC_ICON_TEXTURE_Y + KINETIC_ICON_HEIGHT - kineticHeight;
                    guiGraphics.blit(ATLAS_TEXTURE, x + KINETIC_ICON_X, drawY, 0, (float) KINETIC_ICON_TEXTURE_X, (float) textureY, KINETIC_ICON_WIDTH, kineticHeight, ATLAS_WIDTH, ATLAS_HEIGHT);
                }
            }

            // 绘制动能产生速率文本
            int kineticGen = menu.isWorking() ? menu.getKineticGeneration() : 0;
            int maxKineticGen = menu.getMaxKineticGeneration();
            String kineticText = kineticGen + " KU/" + Component.translatable("gui.mio_icif.max").getString() + " " + maxKineticGen + " KU";
            guiGraphics.drawString(this.font, kineticText, x + KINETIC_TEXT_X, y + KINETIC_TEXT_Y, KINETIC_TEXT_COLOR);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        // 不调用super.renderLabels() 以避免渲染Inventory 文本标签

        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;

        com.singularity_iteration.mio_icif.Menu.KUEntity.KineticGeneratorElcMenu menu = this.menu;
        if (menu == null) return;

        // 检查鼠标是否在电能标志区域
        if (mouseX >= x + ENERGY_ICON_X && mouseX <= x + ENERGY_ICON_X + ENERGY_ICON_WIDTH &&
                mouseY >= y + ENERGY_ICON_Y && mouseY <= y + ENERGY_ICON_Y + ENERGY_ICON_HEIGHT) {
            guiGraphics.renderTooltip(this.font,
                    Component.literal("Energy: " + menu.getEnergy() + "/" + menu.getMaxEnergy() + " EU"),
                    mouseX - x, mouseY - y);
        }

        // 检查鼠标是否在动能标志区域
        if (mouseX >= x + KINETIC_ICON_X && mouseX <= x + KINETIC_ICON_X + KINETIC_ICON_WIDTH &&
                mouseY >= y + KINETIC_ICON_Y && mouseY <= y + KINETIC_ICON_Y + KINETIC_ICON_HEIGHT) {
            guiGraphics.renderTooltip(this.font,
                    Component.literal("Kinetic: " + (menu.isWorking() ? menu.getKineticGeneration() : 0) + "/" + menu.getMaxKineticGeneration() + " KU"),
                    mouseX - x, mouseY - y);
        }
    }

    @Override
    public void containerTick() {
        super.containerTick();
        // 数据通过 ContainerData 自动同步，无需手动同步
    }
}