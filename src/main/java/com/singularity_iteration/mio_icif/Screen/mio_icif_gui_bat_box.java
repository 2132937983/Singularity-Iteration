package com.singularity_iteration.mio_icif.Screen;

import com.singularity_iteration.mio_icif.Menu.Storage.BatBoxMenu;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.client.gui.GuiGraphics;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
@SuppressWarnings("null")
public class mio_icif_gui_bat_box extends mio_icif_screen<BatBoxMenu> {

    private static final ResourceLocation GUI_TEXTURE =
        ResourceLocation.parse("mio_icif:textures/gui/gui_bat_box.png");

    private static final int ENERGY_BAR_X = 79;
    private static final int ENERGY_BAR_Y = 34;
    
    // 红石模式按钮位置
    private static final int REDSTONE_BUTTON_X = 152; // GUI上的X坐标
    private static final int REDSTONE_BUTTON_Y = 4;   // GUI上的Y坐标
    
    // 按钮状态
    private boolean isRedstoneButtonPressed = false;

    public mio_icif_gui_bat_box(BatBoxMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 196;
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;

        guiGraphics.blit(GUI_TEXTURE, x, y, 0, 0, this.imageWidth, this.imageHeight);

        BatBoxMenu menu = this.getMenu();
        if (menu != null) {
            int energyProgressPixels = menu.getEnergyProgressPixels();
            drawEnergyBar(guiGraphics, x + ENERGY_BAR_X, y + ENERGY_BAR_Y, energyProgressPixels);
            
            // 绘制红石模式按钮
            drawRedstoneButton(guiGraphics, x + REDSTONE_BUTTON_X, y + REDSTONE_BUTTON_Y, isRedstoneButtonPressed);
        }
    }
    
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        // 检查是否点击了红石按钮
        if (isHoveringRedstoneButton((int) mouseX, (int) mouseY, REDSTONE_BUTTON_X, REDSTONE_BUTTON_Y)) {
            isRedstoneButtonPressed = true;
            // 发送数据包到服务器切换红石模式
            if (this.minecraft != null && this.minecraft.gameMode != null) {
                this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, 0);
            }
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }
    
    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        isRedstoneButtonPressed = false;
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        super.renderLabels(guiGraphics, mouseX, mouseY);

        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;

        if (mouseX >= x + ENERGY_BAR_X && mouseX <= x + ENERGY_BAR_X + ENERGY_BAR_WIDTH &&
            mouseY >= y + ENERGY_BAR_Y && mouseY <= y + ENERGY_BAR_Y + ENERGY_BAR_HEIGHT) {
            BatBoxMenu menu = this.getMenu();
            if (menu != null) {
                guiGraphics.renderTooltip(this.font,
                    Component.literal(menu.getEnergy() + "/" + menu.getMaxEnergy() + " EU"),
                    mouseX - x, mouseY - y);
            }
        }
        
        // 红石按钮悬停提示
        if (isHoveringRedstoneButton(mouseX, mouseY, REDSTONE_BUTTON_X, REDSTONE_BUTTON_Y)) {
            BatBoxMenu menu = this.getMenu();
            if (menu != null) {
                renderRedstoneModeTooltip(guiGraphics, mouseX - x, mouseY - y, menu.getRedstoneMode());
            }
        }
    }
}