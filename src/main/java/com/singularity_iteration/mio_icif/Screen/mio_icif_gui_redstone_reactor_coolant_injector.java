package com.singularity_iteration.mio_icif.Screen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/**
 * 反应堆冷却液注入器的 GUI 类
 *
 * 特性：
 * - 54 个槽位（大箱子大小）用于存储红石块
 * - 显示能量状态
 * - 显示红石块数量
 */
@OnlyIn(Dist.CLIENT)
public class mio_icif_gui_redstone_reactor_coolant_injector extends mio_icif_screen<com.singularity_iteration.mio_icif.Menu.Producer.RedstoneReactorCoolantInjectorMenu> {

    private static final ResourceLocation GUI_TEXTURE =
        ResourceLocation.parse("mio_icif:textures/gui/gui_double_chest.png");

    // GUI 尺寸（大箱子大小）
    private static final int GUI_WIDTH = 176;
    private static final int GUI_HEIGHT = 222;

    // 能量图标位置
    private static final int ENERGY_ICON_X = 8;
    private static final int ENERGY_ICON_Y = 8;
    private static final int ENERGY_ICON_WIDTH = 16;
    private static final int ENERGY_ICON_HEIGHT = 52;

    public mio_icif_gui_redstone_reactor_coolant_injector(com.singularity_iteration.mio_icif.Menu.Producer.RedstoneReactorCoolantInjectorMenu menu, Inventory playerInventory, Component title) {
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

        com.singularity_iteration.mio_icif.Menu.Producer.RedstoneReactorCoolantInjectorMenu menu = this.menu;
        if (menu != null) {
            // 绘制能量条（从下往上填充）
            int energy = menu.getEnergy();
            int maxEnergy = menu.getMaxEnergy();
            if (energy > 0 && maxEnergy > 0) {
                int energyHeight = (energy * ENERGY_ICON_HEIGHT) / maxEnergy;
                    if (energyHeight > 0) {
                        int drawY = y + ENERGY_ICON_Y + ENERGY_ICON_HEIGHT - energyHeight;
                        int textureY = 2 + ENERGY_ICON_HEIGHT - energyHeight;
guiGraphics.blit(ATLAS_TEXTURE, x + ENERGY_ICON_X, drawY, 0, (float) 144, (float) textureY, ENERGY_ICON_WIDTH, energyHeight, ATLAS_WIDTH, ATLAS_HEIGHT);
                }
            }
        }
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        super.renderLabels(guiGraphics, mouseX, mouseY);

        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;

        com.singularity_iteration.mio_icif.Menu.Producer.RedstoneReactorCoolantInjectorMenu menu = this.menu;
        if (menu == null) return;

        // 检查鼠标是否在能量条区域
        if (mouseX >= x + ENERGY_ICON_X && mouseX <= x + ENERGY_ICON_X + ENERGY_ICON_WIDTH &&
            mouseY >= y + ENERGY_ICON_Y && mouseY <= y + ENERGY_ICON_Y + ENERGY_ICON_HEIGHT) {
            guiGraphics.renderTooltip(this.font,
                Component.literal("EU: " + menu.getEnergy() + "/" + menu.getMaxEnergy()),
                mouseX - x, mouseY - y);
        }

        // 显示红石块数量
        guiGraphics.drawString(this.font,
            Component.translatable("gui.mio_icif.redstone_injector.redstone_blocks", menu.getRedstoneBlockCount()),
            28, 6, 0x404040, false);
    }

    @Override
    public void containerTick() {
        super.containerTick();
        // 数据通过 ContainerData 自动同步，无需手动同步
    }
}