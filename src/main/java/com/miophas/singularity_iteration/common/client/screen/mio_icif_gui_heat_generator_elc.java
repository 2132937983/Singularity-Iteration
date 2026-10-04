package com.miophas.singularity_iteration.common.client.screen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import com.miophas.singularity_iteration.common.menu.huentity.HeatGeneratorElcMenu;

/**
 * 电力发热机的 GUI类
 */
@OnlyIn(Dist.CLIENT)
@SuppressWarnings("null")
public class mio_icif_gui_heat_generator_elc extends mio_icif_screen<com.miophas.singularity_iteration.common.menu.huentity.HeatGeneratorElcMenu> {

    private static final ResourceLocation GUI_TEXTURE =
            ResourceLocation.parse("mio_icif:textures/gui/gui_heat_generator_elc.png");

    // GUI 尺寸
    private static final int GUI_WIDTH = 176;
    private static final int GUI_HEIGHT = 166;

    // 闪电标志位置
    private static final int ENERGY_ICON_X = 9;
    private static final int ENERGY_ICON_Y = 43;

    // 热能标志位置
    private static final int HEAT_ICON_X = 106;
    private static final int HEAT_ICON_Y = 36;
    private static final int HEAT_ICON_WIDTH = 14;
    private static final int HEAT_ICON_HEIGHT = 14;

    // 热能标志纹理位置
    private static final int HEAT_ICON_TEXTURE_X = 168;
    private static final int HEAT_ICON_TEXTURE_Y = 227;

    // 电池槽位位置
    @SuppressWarnings("unused")
    private static final int BATTERY_X = 8;
    @SuppressWarnings("unused")
    private static final int BATTERY_Y = 62;

    // 线圈槽位置（2排5列，紧凑排列18x18）
    @SuppressWarnings("unused")
    private static final int COIL_START_X = 44;
    @SuppressWarnings("unused")
    private static final int COIL_START_Y = 27;
    @SuppressWarnings("unused")
    private static final int COIL_COLS = 5;
    @SuppressWarnings("unused")
    private static final int COIL_ROWS = 2;
    @SuppressWarnings("unused")
    private static final int COIL_SPACING = 18; // 紧凑排列，无间隔

    // 热能文本显示位置
    private static final int HEAT_TEXT_X = 35;
    private static final int HEAT_TEXT_Y = 68;
    // 热能文本颜色 (57c4da)
    private static final int HEAT_TEXT_COLOR = 0x2A2E33;

    public mio_icif_gui_heat_generator_elc(HeatGeneratorElcMenu menu, Inventory playerInventory, Component title) {
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

        HeatGeneratorElcMenu menu = this.getMenu();
        if (menu != null) {
            // 绘制电能标志（根据能量从下往上填充）
            drawLightningEnergy(guiGraphics, x + ENERGY_ICON_X, y + ENERGY_ICON_Y, menu.getEnergy(), menu.getMaxEnergy());

            // 绘制热能标志（根据热能多少从上往下填充）
            int heat = menu.getHeat();
            int maxHeat = menu.getMaxHeat();
            if (heat > 0 && maxHeat > 0) {
                int heatHeight = (heat * HEAT_ICON_HEIGHT) / maxHeat;
                if (heatHeight > 0) {
                    int drawY = y + HEAT_ICON_Y + HEAT_ICON_HEIGHT - heatHeight;
                    int textureY = HEAT_ICON_TEXTURE_Y + HEAT_ICON_HEIGHT - heatHeight;
                    guiGraphics.blit(ATLAS_TEXTURE, x + HEAT_ICON_X, drawY, 0, (float) HEAT_ICON_TEXTURE_X, (float) textureY, HEAT_ICON_WIDTH, heatHeight, ATLAS_WIDTH, ATLAS_HEIGHT);
                }
            }

            // 空线圈槽给出直接可见的操作提示；有线圈时显示实际发热能力。
            if (menu.getCoilCount() == 0) {
                Component insertCoil = Component.translatable("gui.mio_icif.heat_generator_elc.insert_coil");
                guiGraphics.drawString(this.font, insertCoil, x + HEAT_TEXT_X, y + HEAT_TEXT_Y, 0xffe6a23c);
            } else {
                // 对齐 IC2 GuiElectricHeatGenerator：显示“最近一次实际传输 / 上限（线圈数 × 10）”
                int transmitHeat = menu.getTransmitHeat();
                int maxHeatEmit = menu.getMaxHeatEmittedPerTick();
                String heatText = transmitHeat + " HU/" + Component.translatable("gui.mio_icif.max").getString() + " " + maxHeatEmit + " HU";
                guiGraphics.drawString(this.font, heatText, x + HEAT_TEXT_X, y + HEAT_TEXT_Y, HEAT_TEXT_COLOR);
            }
        }
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        // 不调用super.renderLabels() 以避免渲染Inventory 文本标签; the title was missing entirely
        drawTitle(guiGraphics);

        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;

        HeatGeneratorElcMenu menu = this.getMenu();
        if (menu == null) return;

        // 线圈槽为空时，悬停任意一个线圈槽都会提示应放入的物品。
        if (menu.getCoilCount() == 0 && mouseX >= x + COIL_START_X && mouseX < x + COIL_START_X + COIL_COLS * COIL_SPACING
                && mouseY >= y + COIL_START_Y && mouseY < y + COIL_START_Y + COIL_ROWS * COIL_SPACING) {
            guiGraphics.renderTooltip(this.font,
                    Component.translatable("gui.mio_icif.heat_generator_elc.insert_coil"), mouseX - x, mouseY - y);
        }

        // 检查鼠标是否在电能标志区域
        if (mouseX >= x + ENERGY_ICON_X && mouseX <= x + ENERGY_ICON_X + LIGHTNING_WIDTH &&
                mouseY >= y + ENERGY_ICON_Y && mouseY <= y + ENERGY_ICON_Y + LIGHTNING_HEIGHT) {
            guiGraphics.renderTooltip(this.font,
                    Component.literal("Energy: " + menu.getEnergy() + "/" + menu.getMaxEnergy() + " EU"),
                    mouseX - x, mouseY - y);
        }

        // 检查鼠标是否在热能标志区域
        if (mouseX >= x + HEAT_ICON_X && mouseX <= x + HEAT_ICON_X + HEAT_ICON_WIDTH &&
                mouseY >= y + HEAT_ICON_Y && mouseY <= y + HEAT_ICON_Y + HEAT_ICON_HEIGHT) {
            guiGraphics.renderTooltip(this.font,
                    Component.literal("Heat: " + menu.getHeat() + "/" + menu.getMaxHeat() + " HU"),
                    mouseX - x, mouseY - y);
        }
    }

    @Override
    public void containerTick() {
        super.containerTick();

        // 直接从方块实体同步数据到客户端
        HeatGeneratorElcMenu menu = this.getMenu();
        if (menu != null && menu.getBlockEntity() != null) {
            var be = menu.getBlockEntity();
            menu.data.set(0, be.isWorking() ? 1 : 0);
            menu.data.set(1, be.getEnergyStorage().getEnergyStored());
            menu.data.set(2, be.getEnergyStorage().getMaxEnergyStored());
            menu.data.set(3, (int) be.getHeatStored());
            menu.data.set(4, (int) be.getHeatCapacity());
            menu.data.set(5, be.getTemperature());
            menu.data.set(6, be.getCoilCount());
            menu.data.set(7, be.getTransmitHeat());
            menu.data.set(8, be.getMaxHeatEmittedPerTick());
        }
    }


}
