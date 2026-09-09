package com.singularity_iteration.mio_icif.Screen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/**
 * 模式扫描机方块的 GUI 类
 * 参考原版IC2布局
 */
@OnlyIn(Dist.CLIENT)
public class mio_icif_gui_scanner_elc extends mio_icif_screen<com.singularity_iteration.mio_icif.Menu.Producer.ScannerElcMenu> {

    private static final ResourceLocation GUI_TEXTURE =
        ResourceLocation.parse("mio_icif:textures/gui/gui_scanner_elc.png");

    // GUI 尺寸
    private static final int GUI_WIDTH = 176;
    private static final int GUI_HEIGHT = 166;

    // 进度条位置（在 GUI 中的相对位置）
    private static final int PROGRESS_X = 30;
    private static final int PROGRESS_Y = 20;
    /* private static final int PROGRESS_WIDTH = 66; */
    private static final int PROGRESS_HEIGHT = 43;

    // 标准闪电标志位置
    private static final int LIGHTNING_X = 9;
    private static final int LIGHTNING_Y = 24;

    // 按钮位置 (原版IC2: 删除102,49 保存143,49)
    private static final int DELETE_BTN_X = 102;
    private static final int DELETE_BTN_Y = 49;
    private static final int DELETE_BTN_WIDTH = 12;
    private static final int DELETE_BTN_HEIGHT = 12;

    private static final int SAVE_BTN_X = 143;
    private static final int SAVE_BTN_Y = 49;
    private static final int SAVE_BTN_WIDTH = 24;
    private static final int SAVE_BTN_HEIGHT = 12;

    public mio_icif_gui_scanner_elc(com.singularity_iteration.mio_icif.Menu.Producer.ScannerElcMenu menu, Inventory playerInventory, Component title) {
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

        com.singularity_iteration.mio_icif.Menu.Producer.ScannerElcMenu menu = this.menu;
        if (menu != null) {
            // 绘制进度条
        int progressPixels = menu.getProgressPixels(80);
            if (progressPixels > 0) {
guiGraphics.blit(ATLAS_TEXTURE, x + PROGRESS_X, y + PROGRESS_Y, 0, (float) 2, (float) 80, progressPixels, PROGRESS_HEIGHT, ATLAS_WIDTH, ATLAS_HEIGHT);
            }

            // 使用基类标准方法绘制闪电能量标志
            drawLightningEnergy(guiGraphics, x + LIGHTNING_X, y + LIGHTNING_Y, menu.getEnergy(), menu.getMaxEnergy());

            // 绘制按钮
            if (menu.isScanComplete() || menu.getState() == 5) {
                // 删除按钮
guiGraphics.blit(ATLAS_TEXTURE, x + DELETE_BTN_X, y + DELETE_BTN_Y, 0, (float) 130, (float) 243, DELETE_BTN_WIDTH, DELETE_BTN_HEIGHT, ATLAS_WIDTH, ATLAS_HEIGHT);
                // 删除按钮高光
                if (mouseX >= x + DELETE_BTN_X && mouseX <= x + DELETE_BTN_X + DELETE_BTN_WIDTH &&
                    mouseY >= y + DELETE_BTN_Y && mouseY <= y + DELETE_BTN_Y + DELETE_BTN_HEIGHT) {
                    guiGraphics.fill(x + DELETE_BTN_X, y + DELETE_BTN_Y,
                        x + DELETE_BTN_X + DELETE_BTN_WIDTH, y + DELETE_BTN_Y + DELETE_BTN_HEIGHT,
                        0x80FFFFFF);
                }
            }

            if (menu.isScanComplete()) {
                // 保存按钮
guiGraphics.blit(ATLAS_TEXTURE, x + SAVE_BTN_X, y + SAVE_BTN_Y, 0, (float) 50, (float) 243, SAVE_BTN_WIDTH, SAVE_BTN_HEIGHT, ATLAS_WIDTH, ATLAS_HEIGHT);
                // 保存按钮高光
                if (mouseX >= x + SAVE_BTN_X && mouseX <= x + SAVE_BTN_X + SAVE_BTN_WIDTH &&
                    mouseY >= y + SAVE_BTN_Y && mouseY <= y + SAVE_BTN_Y + SAVE_BTN_HEIGHT) {
                    guiGraphics.fill(x + SAVE_BTN_X, y + SAVE_BTN_Y,
                        x + SAVE_BTN_X + SAVE_BTN_WIDTH, y + SAVE_BTN_Y + SAVE_BTN_HEIGHT,
                        0x80FFFFFF);
                }
            }
        }
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        super.renderLabels(guiGraphics, mouseX, mouseY);

        // int x = (this.width - this.imageWidth) / 2;
        // int y = (this.height - this.imageHeight) / 2;

        com.singularity_iteration.mio_icif.Menu.Producer.ScannerElcMenu menu = this.menu;
        if (menu == null) return;

        // 绘制状态信息
        int state = menu.getState();
        String stateText;
        int color;
        switch (state) {
            case 0: // IDLE
                stateText = "Idle";
                color = 0xECA300;
                break;
            case 1: // SCANNING
                int progressPercent = menu.getMaxProgress() > 0 ?
                    (menu.getProgress() * 100 / menu.getMaxProgress()) : 0;
                stateText = "Scanning... " + progressPercent + "%";
                color = 0x20D4DE;
                break;
            case 2: // NO_ENERGY
                stateText = "No Energy";
                color = 0xD74242;
                break;
            case 3: // NO_STORAGE
                stateText = "No Pattern Storage";
                color = 0xD74242;
                break;
            case 4: // COMPLETED
                stateText = "Completed";
                color = 0x20D4DE;
                break;
            case 5: // FAILED
                stateText = "Failed";
                color = 0xD74242;
                break;
            default:
                stateText = "";
                color = 0xFFFFFF;
        }

        if (!stateText.isEmpty()) {
            guiGraphics.drawString(this.font, stateText, 10, 69, color, false);
        }

        // 如果扫描完成，显示UU物质和能量消耗
        if (state == 4 || state == 5) {
            double uuCostBuckets = menu.getUuCost();
            long euCost = menu.getEuCost();

            String uuText = toSiString(uuCostBuckets, 4) + "B UUM";

            String euText;
            if (euCost < 1000) {
                euText = euCost + " EU";
            } else if (euCost < 1000000) {
                euText = String.format("%.2fk EU", euCost / 1000.0);
            } else {
                euText = String.format("%.2fM EU", euCost / 1000000.0);
            }

            guiGraphics.drawString(this.font, uuText, 105, 25, 0xFFFFFF, false);
            guiGraphics.drawString(this.font, euText, 105, 36, 0xFFFFFF, false);
        }



    }

    @Override
    protected void renderTooltip(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        super.renderTooltip(guiGraphics, mouseX, mouseY);

        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;

        com.singularity_iteration.mio_icif.Menu.Producer.ScannerElcMenu menu = this.menu;
        if (menu == null) return;

        // 鼠标悬停提示 - 闪电标志区域
        if (mouseX >= x + LIGHTNING_X && mouseX <= x + LIGHTNING_X + LIGHTNING_WIDTH &&
            mouseY >= y + LIGHTNING_Y && mouseY <= y + LIGHTNING_Y + LIGHTNING_HEIGHT) {
            guiGraphics.renderTooltip(this.font,
                Component.literal("Energy: " + menu.getEnergy() + "/" + menu.getMaxEnergy() + " EU"),
                mouseX, mouseY);
        }

        // 鼠标悬停提示 - 删除按钮
        if ((menu.isScanComplete() || menu.getState() == 5) &&
            mouseX >= x + DELETE_BTN_X && mouseX <= x + DELETE_BTN_X + DELETE_BTN_WIDTH &&
            mouseY >= y + DELETE_BTN_Y && mouseY <= y + DELETE_BTN_Y + DELETE_BTN_HEIGHT) {
            guiGraphics.renderTooltip(this.font, Component.literal("Cancel"), mouseX, mouseY);
        }

        // 鼠标悬停提示 - 保存按钮
        if (menu.isScanComplete() &&
            mouseX >= x + SAVE_BTN_X && mouseX <= x + SAVE_BTN_X + SAVE_BTN_WIDTH &&
            mouseY >= y + SAVE_BTN_Y && mouseY <= y + SAVE_BTN_Y + SAVE_BTN_HEIGHT) {
            guiGraphics.renderTooltip(this.font, Component.literal("Save to Crystal Memory"), mouseX, mouseY);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && this.menu != null && this.minecraft != null) {
            int x = (this.width - this.imageWidth) / 2;
            int y = (this.height - this.imageHeight) / 2;

            com.singularity_iteration.mio_icif.Menu.Producer.ScannerElcMenu menu = this.menu;
            if (menu == null) return super.mouseClicked(mouseX, mouseY, button);

            // 检查删除按钮点击 (按钮ID: 0)
            if (menu.isScanComplete() || menu.getState() == 5) {
                if (mouseX >= x + DELETE_BTN_X && mouseX <= x + DELETE_BTN_X + DELETE_BTN_WIDTH &&
                    mouseY >= y + DELETE_BTN_Y && mouseY <= y + DELETE_BTN_Y + DELETE_BTN_HEIGHT) {
                    // 发送按钮点击到服务器
                    if (this.minecraft.getConnection() != null) {
                        this.minecraft.getConnection().send(new net.minecraft.network.protocol.game.ServerboundContainerButtonClickPacket(this.menu.containerId, 0));
                    }
                    // 客户端立即响应
                    this.menu.clickMenuButton(this.minecraft.player, 0);
                    return true;
                }
            }

            // 检查保存按钮点击 (按钮ID: 1)
            if (menu.isScanComplete()) {
                if (mouseX >= x + SAVE_BTN_X && mouseX <= x + SAVE_BTN_X + SAVE_BTN_WIDTH &&
                    mouseY >= y + SAVE_BTN_Y && mouseY <= y + SAVE_BTN_Y + SAVE_BTN_HEIGHT) {
                    // 发送按钮点击到服务器
                    if (this.minecraft.getConnection() != null) {
                        this.minecraft.getConnection().send(new net.minecraft.network.protocol.game.ServerboundContainerButtonClickPacket(this.menu.containerId, 1));
                    }
                    // 客户端立即响应
                    this.menu.clickMenuButton(this.minecraft.player, 1);
                    return true;
                }
            }
        }

        return super.mouseClicked(mouseX, mouseY, button);
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