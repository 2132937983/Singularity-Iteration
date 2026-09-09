package com.singularity_iteration.mio_icif.Screen;

import com.singularity_iteration.mio_icif.Menu.Storage.TransformerMenu;
import com.singularity_iteration.mio_icif.Blocks.entity.transformer.mio_icif_transformer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/**
 * 变压器的 GUI 类
 * 显示当前模式、红石信号状态、能量冲等信息
 */
@OnlyIn(Dist.CLIENT)
@SuppressWarnings("null")
public class mio_icif_gui_transformer extends mio_icif_screen<TransformerMenu> {

    private static final ResourceLocation GUI_TEXTURE =
        ResourceLocation.parse("mio_icif:textures/gui/gui_transformer.png");

    // Vanilla button sprites in GUI atlas.
    private static final ResourceLocation BUTTON_SPRITE = ResourceLocation.withDefaultNamespace("widget/button");
    private static final ResourceLocation BUTTON_HIGHLIGHTED_SPRITE = ResourceLocation.withDefaultNamespace("widget/button_highlighted");
    private static final int MODE_BUTTON_X = 7;
    private static final int MODE_BUTTON_WIDTH = 143;
    private static final int MODE_BUTTON_HEIGHT = 19;
    private static final int MODE_BUTTON_REDSTONE_Y = 65;
    private static final int MODE_BUTTON_STEP_DOWN_Y = 84;
    private static final int MODE_BUTTON_STEP_UP_Y = 103;

    public mio_icif_gui_transformer(TransformerMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 219;
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;

        // 绘制背景
        guiGraphics.blit(GUI_TEXTURE, x, y, 0, 0, this.imageWidth, this.imageHeight);

        // 绘制模式按钮
        TransformerMenu menu = this.getMenu();
        if (menu != null) {
            // 绘制三个原版样式按钮 (x=7, y=65开始, 尺寸143x19)
            renderMinecraftButton(
                guiGraphics,
                x + MODE_BUTTON_X, y + MODE_BUTTON_REDSTONE_Y, MODE_BUTTON_WIDTH, MODE_BUTTON_HEIGHT,
                isHoveringButton(mouseX, mouseY, x + MODE_BUTTON_X, y + MODE_BUTTON_REDSTONE_Y, MODE_BUTTON_WIDTH, MODE_BUTTON_HEIGHT)
            );
            renderMinecraftButton(
                guiGraphics,
                x + MODE_BUTTON_X, y + MODE_BUTTON_STEP_DOWN_Y, MODE_BUTTON_WIDTH, MODE_BUTTON_HEIGHT,
                isHoveringButton(mouseX, mouseY, x + MODE_BUTTON_X, y + MODE_BUTTON_STEP_DOWN_Y, MODE_BUTTON_WIDTH, MODE_BUTTON_HEIGHT)
            );
            renderMinecraftButton(
                guiGraphics,
                x + MODE_BUTTON_X, y + MODE_BUTTON_STEP_UP_Y, MODE_BUTTON_WIDTH, MODE_BUTTON_HEIGHT,
                isHoveringButton(mouseX, mouseY, x + MODE_BUTTON_X, y + MODE_BUTTON_STEP_UP_Y, MODE_BUTTON_WIDTH, MODE_BUTTON_HEIGHT)
            );
        }
    }

    /**
     * 渲染原版Minecraft样式按钮
     * @param guiGraphics 图形上下文
     * @param x 按钮X坐标
     * @param y 按钮Y坐标
     * @param width 按钮宽度
     * @param height 按钮高度
     * @param hovered 是否悬停
     */
    private void renderMinecraftButton(GuiGraphics guiGraphics, int x, int y, int width, int height, boolean hovered) {
        guiGraphics.blitSprite(hovered ? BUTTON_HIGHLIGHTED_SPRITE : BUTTON_SPRITE, x, y, width, height);
    }

    private boolean isHoveringButton(int mouseX, int mouseY, int x, int y, int width, int height) {
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        super.renderLabels(guiGraphics, mouseX, mouseY);

        // int x = (this.width - this.imageWidth) / 2;
        // int y = (this.height - this.imageHeight) / 2;

        TransformerMenu menu = this.getMenu();
        if (menu != null) {
            // 绘制输入文本 (49, 28) - 从实体获取
            long inputValue = menu.getEffectiveMode() == mio_icif_transformer.TransformerMode.STEP_UP
                ? menu.getLowSideLimit()
                : menu.getHighSideLimit();
            Component inputText = Component.translatable("gui.mio_icif.transformer.input", inputValue);
            guiGraphics.drawString(this.font, inputText, 49, 28, 0x404040, false);

            // 绘制输出文本 (48, 44) - 从实体获取
            long outputValue = menu.getEffectiveMode() == mio_icif_transformer.TransformerMode.STEP_UP
                ? menu.getHighSideLimit()
                : menu.getLowSideLimit();
            Component outputText = Component.translatable("gui.mio_icif.transformer.output", outputValue);
            guiGraphics.drawString(this.font, outputText, 48, 44, 0x404040, false);

            // 绘制三个模式按钮文本
            Component redstoneModeText = Component.translatable("gui.mio_icif.transformer.button.redstone_control");
            Component stepDownModeText = Component.translatable("gui.mio_icif.transformer.button.step_down");
            Component stepUpModeText = Component.translatable("gui.mio_icif.transformer.button.step_up");
            int buttonTextX = MODE_BUTTON_X + MODE_BUTTON_WIDTH / 2;
            guiGraphics.drawCenteredString(this.font, redstoneModeText, buttonTextX, MODE_BUTTON_REDSTONE_Y + 6, 0xE0E0E0);
            guiGraphics.drawCenteredString(this.font, stepDownModeText, buttonTextX, MODE_BUTTON_STEP_DOWN_Y + 6, 0xE0E0E0);
            guiGraphics.drawCenteredString(this.font, stepUpModeText, buttonTextX, MODE_BUTTON_STEP_UP_Y + 6, 0xE0E0E0);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && this.menu != null && this.minecraft != null && this.minecraft.gameMode != null) {
            int x = (this.width - this.imageWidth) / 2;
            int y = (this.height - this.imageHeight) / 2;
            int modeButtonId = getModeButtonId(mouseX, mouseY, x, y);
            if (modeButtonId >= 0) {
                this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, modeButtonId);
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private int getModeButtonId(double mouseX, double mouseY, int guiLeft, int guiTop) {
        if (isHoveringButton((int) mouseX, (int) mouseY, guiLeft + MODE_BUTTON_X, guiTop + MODE_BUTTON_REDSTONE_Y, MODE_BUTTON_WIDTH, MODE_BUTTON_HEIGHT)) {
            return 0;
        }
        if (isHoveringButton((int) mouseX, (int) mouseY, guiLeft + MODE_BUTTON_X, guiTop + MODE_BUTTON_STEP_DOWN_Y, MODE_BUTTON_WIDTH, MODE_BUTTON_HEIGHT)) {
            return 1;
        }
        if (isHoveringButton((int) mouseX, (int) mouseY, guiLeft + MODE_BUTTON_X, guiTop + MODE_BUTTON_STEP_UP_Y, MODE_BUTTON_WIDTH, MODE_BUTTON_HEIGHT)) {
            return 2;
        }
        return -1;
    }
}