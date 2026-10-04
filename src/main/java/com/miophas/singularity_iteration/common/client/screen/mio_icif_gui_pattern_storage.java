package com.miophas.singularity_iteration.common.client.screen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/**
 * 模式存储机方块的 GUI类
 * 参考原版IC2布局
 */
@OnlyIn(Dist.CLIENT)
@SuppressWarnings("null")
public class mio_icif_gui_pattern_storage extends mio_icif_screen<com.miophas.singularity_iteration.common.menu.producer.PatternStorageMenu> {

    private static final ResourceLocation GUI_TEXTURE =
        ResourceLocation.parse("mio_icif:textures/gui/gui_pattern_storage.png");

    // GUI 尺寸
    private static final int GUI_WIDTH = 176;
    private static final int GUI_HEIGHT = 166;

    // 能量显示位置
    private static final int ENERGY_X = 9;
    private static final int ENERGY_Y = 24;
    private static final int ENERGY_WIDTH = 14;
    private static final int ENERGY_HEIGHT = 42;

    // 导航按钮位置 (原版IC2: 7, 19 和 36, 19)
    private static final int PREV_BTN_X = 7;
    private static final int PREV_BTN_Y = 19;
    private static final int NEXT_BTN_X = 36;
    private static final int NEXT_BTN_Y = 19;
    private static final int NAV_BTN_WIDTH = 9;
    private static final int NAV_BTN_HEIGHT = 18;

    // 导出/导入按钮位置 (原版IC2: 10, 37 和 26, 37)
    private static final int EXPORT_BTN_X = 10;
    private static final int EXPORT_BTN_Y = 37;
    private static final int IMPORT_BTN_X = 26;
    private static final int IMPORT_BTN_Y = 37;
    private static final int IO_BTN_WIDTH = 16;
    private static final int IO_BTN_HEIGHT = 8;

    // 信息显示位置
    private static final int INFO_X = 10;
    private static final int NAME_Y = 48;
    private static final int UUM_Y = 59;
    private static final int EU_Y = 70;
    private static final int VALUE_X = 44;
    /** Energy gauge inside the info panel (panel spans x 8..168, y 45..80). */
    private static final int GAUGE_X = 161, GAUGE_Y = 49, GAUGE_W = 4, GAUGE_H = 27;
    private static final int VALUE_W = GAUGE_X - 3 - VALUE_X;

    // 物品预览位置 (原版IC2: 152, 29)
    private static final int PREVIEW_X = 152;
    private static final int PREVIEW_Y = 29;

    public mio_icif_gui_pattern_storage(com.miophas.singularity_iteration.common.menu.producer.PatternStorageMenu menu, Inventory playerInventory, Component title) {
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

        com.miophas.singularity_iteration.common.menu.producer.PatternStorageMenu menu = this.menu;
        if (menu == null) return;

        // energy: a slim gauge at the right end of the info panel. The old 14x42 atlas sprite at
        // (9,24) and the extra arrow sprites were drawn over the texture's own arrows and the
        // "Name / UU" labels.
        int energy = menu.getEnergy();
        int maxEnergy = menu.getMaxEnergy();
        if (maxEnergy > 0) {
            int gx = x + GAUGE_X, gy = y + GAUGE_Y;
            guiGraphics.fill(gx - 1, gy - 1, gx + GAUGE_W + 1, gy + GAUGE_H + 1, 0xFF5A5F66);
            guiGraphics.fill(gx, gy, gx + GAUGE_W, gy + GAUGE_H, 0xFF2A2E33);
            int filled = (int) ((long) Math.max(0, energy) * GAUGE_H / maxEnergy);
            if (filled > 0) guiGraphics.fillGradient(gx, gy + GAUGE_H - filled, gx + GAUGE_W, gy + GAUGE_H, 0xFFF0C040, 0xFFD06020);
        }

        // 如果有模式数据，绘制物品预览
        ItemStack pattern = menu.getCurrentPattern();
        if (!pattern.isEmpty()) {
            guiGraphics.renderItem(pattern, x + PREVIEW_X, y + PREVIEW_Y);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        super.renderLabels(guiGraphics, mouseX, mouseY);

        com.miophas.singularity_iteration.common.menu.producer.PatternStorageMenu menu = this.menu;
        if (menu == null) return;

        // 绘制页码 (原版IC2: 中心对齐, y=30); an empty storage reads 0 / 0
        int currentIndex = menu.getCurrentIndex();
        int maxIndex = menu.getMaxIndex();
        String pageText = (maxIndex <= 0 ? 0 : currentIndex + 1) + " / " + maxIndex;
        int pageWidth = this.font.width(pageText);
        guiGraphics.drawString(this.font, pageText, (this.imageWidth - pageWidth) / 2, 30, 0x404040, false);

        drawFitted(guiGraphics, this.font, Component.translatable("gui.mio_icif.pattern_storage.name"), INFO_X, NAME_Y, VALUE_X - INFO_X - 2, 0x2A2E33, false);
        drawFitted(guiGraphics, this.font, Component.translatable("gui.mio_icif.pattern_storage.uu"), INFO_X, UUM_Y, VALUE_X - INFO_X - 2, 0x2A2E33, false);
        drawFitted(guiGraphics, this.font, Component.translatable("gui.mio_icif.pattern_storage.eu"), INFO_X, EU_Y, VALUE_X - INFO_X - 2, 0x2A2E33, false);

        ItemStack pattern = menu.getCurrentPattern();
        if (!pattern.isEmpty()) {
            drawFitted(guiGraphics, this.font, pattern.getHoverName(), VALUE_X, NAME_Y, VALUE_W, 0x2A2E33, false);
            guiGraphics.drawString(this.font, toSiString(menu.getCurrentUuCost(), 4) + "B", VALUE_X, UUM_Y, 0x2A2E33, false);
            guiGraphics.drawString(this.font, String.format("%.2f", menu.getCurrentEuCost() / 1000000.0) + "M", VALUE_X, EU_Y, 0x2A2E33, false);
        } else {
            drawFitted(guiGraphics, this.font, Component.translatable("gui.mio_icif.pattern_storage.empty"), VALUE_X, NAME_Y, VALUE_W, 0x888888, false);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && this.menu != null && this.minecraft != null && this.minecraft.gameMode != null) {
            int x = (this.width - this.imageWidth) / 2;
            int y = (this.height - this.imageHeight) / 2;

            // 检查是否点击了导航按钮
            if (isHoveringButton(mouseX, mouseY, x + PREV_BTN_X, y + PREV_BTN_Y, NAV_BTN_WIDTH, NAV_BTN_HEIGHT)) {
                this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, 0);
                return true;
            }
            if (isHoveringButton(mouseX, mouseY, x + NEXT_BTN_X, y + NEXT_BTN_Y, NAV_BTN_WIDTH, NAV_BTN_HEIGHT)) {
                this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, 1);
                return true;
            }
            if (isHoveringButton(mouseX, mouseY, x + EXPORT_BTN_X, y + EXPORT_BTN_Y, IO_BTN_WIDTH, IO_BTN_HEIGHT)) {
                this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, 2);
                return true;
            }
            if (isHoveringButton(mouseX, mouseY, x + IMPORT_BTN_X, y + IMPORT_BTN_Y, IO_BTN_WIDTH, IO_BTN_HEIGHT)) {
                this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, 3);
                return true;
            }
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    private boolean isHoveringButton(double mouseX, double mouseY, int x, int y, int width, int height) {
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
    }

    @Override
    public void containerTick() {
        super.containerTick();
    }

    private static String toSiString(double value, int digits) {
        if (value == 0.0D) return "0 ";
        if (Double.isNaN(value)) return "NaN ";
        String ret = "";
        String si;
        if (value < 0.0D) {
            ret = "-";
            value = -value;
        }
        if (Double.isInfinite(value)) return ret + "\u221E ";
        double log = Math.log10(value);
        double mul;
        if (log >= 0.0D) {
            int reduce = (int) Math.floor(log / 3.0D);
            mul = 1.0D / Math.pow(10.0D, reduce * 3);
            si = switch (reduce) {
                case 0 -> "";
                case 1 -> "k";
                case 2 -> "M";
                case 3 -> "G";
                case 4 -> "T";
                case 5 -> "P";
                case 6 -> "E";
                case 7 -> "Z";
                case 8 -> "Y";
                default -> "E" + (reduce * 3);
            };
        } else {
            int expand = (int) Math.ceil(-log / 3.0D);
            mul = Math.pow(10.0D, expand * 3);
            si = switch (expand) {
                case 0 -> "";
                case 1 -> "m";
                case 2 -> "\u00B5";
                case 3 -> "n";
                case 4 -> "p";
                case 5 -> "f";
                case 6 -> "a";
                case 7 -> "z";
                case 8 -> "y";
                default -> "E-" + (expand * 3);
            };
        }
        value *= mul;
        int iVal = (int) Math.floor(value);
        value -= iVal;
        int iDigits = 1;
        if (iVal > 0) iDigits = (int) (iDigits + Math.floor(Math.log10(iVal)));
        double mul2 = Math.pow(10.0D, digits - iDigits);
        int dVal = (int) Math.round(value * mul2);
        if (dVal >= mul2) {
            iVal++;
            dVal = (int) (dVal - mul2);
            iDigits = 1;
            if (iVal > 0) iDigits = (int) (iDigits + Math.floor(Math.log10(iVal)));
        }
        ret = ret + Integer.toString(iVal);
        if (digits > iDigits && dVal != 0) {
            ret = ret + String.format(".%0" + (digits - iDigits) + "d", dVal);
        }
        ret = ret.replaceFirst("(\\.\\d*?)0+$", "$1");
        return ret + " " + si;
    }
}