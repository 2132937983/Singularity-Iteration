package com.singularity_iteration.mio_icif.Screen;

import com.singularity_iteration.mio_icif.Menu.Producer.AdvancedMinerElcMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/**
 * 高级采矿机的GUI类
 * 参考原版IC2高级采矿机GUI布局
 * GUI 高度: 203 像素
 */
@OnlyIn(Dist.CLIENT)
public class mio_icif_gui_advanced_miner_elc extends mio_icif_screen<AdvancedMinerElcMenu> {

    // GUI 纹理路径
    public static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath("mio_icif", "textures/gui/gui_advminer_elc.png");

    // GUI 尺寸
    public static final int GUI_WIDTH = 176;
    public static final int GUI_HEIGHT = 203;

    // 标准闪电标志位置
    private static final int LIGHTNING_X = 9;
    private static final int LIGHTNING_Y = 54;

    private static final int ENERGY_X = 9;
    private static final int ENERGY_Y = 54;
    private static final int ENERGY_WIDTH = 14;
    private static final int ENERGY_HEIGHT = 15;

    // 重置按钮 - 原版IC2位置(133, 101)
    private static final int RESET_BUTTON_X = 133;
    private static final int RESET_BUTTON_Y = 101;
    private static final int RESET_BUTTON_WIDTH = 16;
    private static final int RESET_BUTTON_HEIGHT = 15;

    // 模式切换按钮（黑名单/白名单）- 原版IC2位置(123, 27)
    private static final int MODE_BUTTON_X = 123;
    private static final int MODE_BUTTON_Y = 27;
    private static final int MODE_BUTTON_WIDTH = 16;
    private static final int MODE_BUTTON_HEIGHT = 15;

    // 精准采集按钮 - 原版IC2位置(129, 45)
    private static final int SILK_TOUCH_BUTTON_X = 129;
    private static final int SILK_TOUCH_BUTTON_Y = 45;
    private static final int SILK_TOUCH_BUTTON_WIDTH = 16;
    private static final int SILK_TOUCH_BUTTON_HEIGHT = 15;

    // 升级槽位区域 - 4个(垂直排列), 最顶上位置(152,26)
    public mio_icif_gui_advanced_miner_elc(AdvancedMinerElcMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = GUI_WIDTH;
        this.imageHeight = GUI_HEIGHT;
        this.inventoryLabelY = 110;
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;

        // 绘制背景
        guiGraphics.blit(TEXTURE, x, y, 0, 0, this.imageWidth, this.imageHeight);

        AdvancedMinerElcMenu menu = this.menu;

        // 绘制标准闪电标志
        drawLightningEnergy(guiGraphics, x + LIGHTNING_X, y + LIGHTNING_Y, menu.getEnergy(), menu.getMaxEnergy());

        // 绘制按钮悬停高亮
        drawButtonHighlight(guiGraphics, x, y, mouseX, mouseY, RESET_BUTTON_X, RESET_BUTTON_Y, RESET_BUTTON_WIDTH, RESET_BUTTON_HEIGHT);
        drawButtonHighlight(guiGraphics, x, y, mouseX, mouseY, MODE_BUTTON_X, MODE_BUTTON_Y, MODE_BUTTON_WIDTH, MODE_BUTTON_HEIGHT);
        drawButtonHighlight(guiGraphics, x, y, mouseX, mouseY, SILK_TOUCH_BUTTON_X, SILK_TOUCH_BUTTON_Y, SILK_TOUCH_BUTTON_WIDTH, SILK_TOUCH_BUTTON_HEIGHT);
    }

    /**
     * 绘制按钮悬停高亮
     */
    private void drawButtonHighlight(GuiGraphics guiGraphics, int x, int y, int mouseX, int mouseY, int btnX, int btnY, int btnWidth, int btnHeight) {
        if (isHoveringButton(mouseX, mouseY, x + btnX, y + btnY, btnWidth, btnHeight)) {
            guiGraphics.fill(x + btnX, y + btnY,
                x + btnX + btnWidth, y + btnY + btnHeight,
                0x80FFFFFF);
        }
    }

    /**
     * 检查鼠标是否悬停在按钮上
     */
    private boolean isHoveringButton(int mouseX, int mouseY, int x, int y, int width, int height) {
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        this.renderTooltip(guiGraphics, mouseX, mouseY);

        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;

        AdvancedMinerElcMenu menu = this.menu;

        // 绘制能量提示
        if (mouseX >= x + ENERGY_X && mouseX <= x + ENERGY_X + ENERGY_WIDTH &&
            mouseY >= y + ENERGY_Y && mouseY <= y + ENERGY_Y + ENERGY_HEIGHT) {
            Component tooltip = Component.literal(String.format("EU: %,d / %,d", menu.getEnergy(), menu.getMaxEnergy()));
            guiGraphics.renderTooltip(this.font, tooltip, mouseX, mouseY);
        }

        // 绘制重置按钮提示
        if (isHoveringButton(mouseX, mouseY, x + RESET_BUTTON_X, y + RESET_BUTTON_Y, RESET_BUTTON_WIDTH, RESET_BUTTON_HEIGHT)) {
            Component tooltip = Component.translatable("gui.mio_icif.advanced_miner.reset");
            guiGraphics.renderTooltip(this.font, tooltip, mouseX, mouseY);
        }

        // 绘制模式按钮提示（黑名单/白名单）
        if (isHoveringButton(mouseX, mouseY, x + MODE_BUTTON_X, y + MODE_BUTTON_Y, MODE_BUTTON_WIDTH, MODE_BUTTON_HEIGHT)) {
            Component tooltip = Component.translatable(menu.isWhitelistMode() ?
                "gui.mio_icif.advanced_miner.mode.whitelist" :
                "gui.mio_icif.advanced_miner.mode.blacklist");
            guiGraphics.renderTooltip(this.font, tooltip, mouseX, mouseY);
        }

        // 绘制精准采集按钮提示
        if (isHoveringButton(mouseX, mouseY, x + SILK_TOUCH_BUTTON_X, y + SILK_TOUCH_BUTTON_Y, SILK_TOUCH_BUTTON_WIDTH, SILK_TOUCH_BUTTON_HEIGHT)) {
            Component tooltip = Component.translatable(menu.isSilkTouchMode() ?
                "gui.mio_icif.advanced_miner.silk_touch.on" :
                "gui.mio_icif.advanced_miner.silk_touch.off");
            guiGraphics.renderTooltip(this.font, tooltip, mouseX, mouseY);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        super.renderLabels(guiGraphics, mouseX, mouseY);

        // 绘制标题
        guiGraphics.drawString(this.font, this.title, this.titleLabelX, this.titleLabelY, 0x404040, false);

        // 绘制模式文本（黑名单/白名单）- 原版IC2位置(40, 31)
        AdvancedMinerElcMenu menu = this.menu;
        Component modeText = Component.translatable(menu.isWhitelistMode() ?
            "gui.mio_icif.advanced_miner.mode.whitelist" :
            "gui.mio_icif.advanced_miner.mode.blacklist");
        guiGraphics.drawString(this.font, modeText, 40, 31, 0x404040, false);

        // 绘制深度信息
        if (menu.getBlockEntity() != null) {
            int depth = menu.getBlockEntity().getCurrentDepth();
            Component depthText = Component.translatable("gui.mio_icif.advanced_miner.depth", depth);
            guiGraphics.drawString(this.font, depthText, 28, 105, 0x404040, false);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && this.menu != null && this.minecraft != null && this.minecraft.gameMode != null) {
            int x = (this.width - this.imageWidth) / 2;
            int y = (this.height - this.imageHeight) / 2;

            // 检查重置按钮(id=0)
            if (isHoveringButton((int) mouseX, (int) mouseY, x + RESET_BUTTON_X, y + RESET_BUTTON_Y, RESET_BUTTON_WIDTH, RESET_BUTTON_HEIGHT)) {
                this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, 0);
                return true;
            }

            // 检查模式按钮(id=1) - 黑名单/白名单切换
            if (isHoveringButton((int) mouseX, (int) mouseY, x + MODE_BUTTON_X, y + MODE_BUTTON_Y, MODE_BUTTON_WIDTH, MODE_BUTTON_HEIGHT)) {
                this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, 1);
                return true;
            }

            // 检查精准采集按钮(id=2)
            if (isHoveringButton((int) mouseX, (int) mouseY, x + SILK_TOUCH_BUTTON_X, y + SILK_TOUCH_BUTTON_Y, SILK_TOUCH_BUTTON_WIDTH, SILK_TOUCH_BUTTON_HEIGHT)) {
                this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, 2);
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }
}