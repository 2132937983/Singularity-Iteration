package com.singularity_iteration.mio_icif.Screen;

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
public class mio_icif_gui_pattern_storage extends mio_icif_screen<com.singularity_iteration.mio_icif.Menu.Producer.PatternStorageMenu> {

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
    private static final int VALUE_X = 80;

    // 物品预览位置 (原版IC2: 152, 29)
    private static final int PREVIEW_X = 152;
    private static final int PREVIEW_Y = 29;

    public mio_icif_gui_pattern_storage(com.singularity_iteration.mio_icif.Menu.Producer.PatternStorageMenu menu, Inventory playerInventory, Component title) {
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

        com.singularity_iteration.mio_icif.Menu.Producer.PatternStorageMenu menu = this.menu;
        if (menu == null) return;

        // 绘制能量条
        int energy = menu.getEnergy();
        int maxEnergy = menu.getMaxEnergy();
        if (energy > 0 && maxEnergy > 0) {
            int energyHeight = (energy * ENERGY_HEIGHT) / maxEnergy;
            if (energyHeight > 0) {
                int drawY = y + ENERGY_Y + ENERGY_HEIGHT - energyHeight;
                guiGraphics.blit(ATLAS_TEXTURE, x + ENERGY_X, drawY, 0, (float) 70, (float) ENERGY_HEIGHT - energyHeight, ENERGY_WIDTH, energyHeight, ATLAS_WIDTH, ATLAS_HEIGHT);
            }
        }

        // 绘制导航按钮
        guiGraphics.blit(ATLAS_TEXTURE, x + PREV_BTN_X, y + PREV_BTN_Y, 0, (float) 164, (float) 164, NAV_BTN_WIDTH, NAV_BTN_HEIGHT, ATLAS_WIDTH, ATLAS_HEIGHT);
        guiGraphics.blit(ATLAS_TEXTURE, x + NEXT_BTN_X, y + NEXT_BTN_Y, 0, (float) 175, (float) 164, NAV_BTN_WIDTH, NAV_BTN_HEIGHT, ATLAS_WIDTH, ATLAS_HEIGHT);

        // 绘制导出/导入按钮
        guiGraphics.blit(ATLAS_TEXTURE, x + EXPORT_BTN_X, y + EXPORT_BTN_Y, 0, (float) 60, (float) 259, IO_BTN_WIDTH, IO_BTN_HEIGHT, ATLAS_WIDTH, ATLAS_HEIGHT);
        guiGraphics.blit(ATLAS_TEXTURE, x + IMPORT_BTN_X, y + IMPORT_BTN_Y, 0, (float) 78, (float) 259, IO_BTN_WIDTH, IO_BTN_HEIGHT, ATLAS_WIDTH, ATLAS_HEIGHT);

        // 如果有模式数据，绘制物品预览
        ItemStack pattern = menu.getCurrentPattern();
        if (!pattern.isEmpty()) {
            guiGraphics.renderItem(pattern, x + PREVIEW_X, y + PREVIEW_Y);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        super.renderLabels(guiGraphics, mouseX, mouseY);

        com.singularity_iteration.mio_icif.Menu.Producer.PatternStorageMenu menu = this.menu;
        if (menu == null) return;

        // 绘制页码 (原版IC2: 中心对齐, y=30)
        int currentIndex = menu.getCurrentIndex();
        int maxIndex = menu.getMaxIndex();
        String pageText = (currentIndex + 1) + " / " + maxIndex;
        int pageWidth = this.font.width(pageText);
        guiGraphics.drawString(this.font, pageText, (this.imageWidth - pageWidth) / 2, 30, 0x404040, false);

        // 绘制标签
        guiGraphics.drawString(this.font, "Name:", INFO_X, NAME_Y, 0xFFFFFF, false);
        guiGraphics.drawString(this.font, "UU:", INFO_X, UUM_Y, 0xFFFFFF, false);
        guiGraphics.drawString(this.font, "EU:", INFO_X, EU_Y, 0xFFFFFF, false);

        // 绘制当前模式的信息
        ItemStack pattern = menu.getCurrentPattern();
        if (!pattern.isEmpty()) {
            // 物品名称
            String name = pattern.getHoverName().getString();
            if (name.length() > 20) {
                name = name.substring(0, 17) + "...";
            }
            guiGraphics.drawString(this.font, name, VALUE_X, NAME_Y, 0xFFFFFF, false);

            // UU物质消耗
            double uuCost = menu.getCurrentUuCost();
            guiGraphics.drawString(this.font,
                toSiString(uuCost, 4) + "B", VALUE_X, UUM_Y, 0xFFFFFF, false);

            // 能量消耗
            long euCost = menu.getCurrentEuCost();
            guiGraphics.drawString(this.font,
                String.format("%.2f", euCost / 1000000.0) + "M", VALUE_X, EU_Y, 0xFFFFFF, false);
        } else {
            guiGraphics.drawString(this.font, "Empty", VALUE_X, NAME_Y, 0x888888, false);
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