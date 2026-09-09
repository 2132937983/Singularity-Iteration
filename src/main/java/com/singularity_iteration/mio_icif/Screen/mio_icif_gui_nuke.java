package com.singularity_iteration.mio_icif.Screen;

import com.singularity_iteration.mio_icif.Menu.Generator.NukeMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/**
 * 核弹方块的GUI类
 */
@OnlyIn(Dist.CLIENT)
@SuppressWarnings("null")
public class mio_icif_gui_nuke extends mio_icif_screen<NukeMenu> {

    private static final ResourceLocation GUI_TEXTURE =
        ResourceLocation.parse("mio_icif:textures/gui/gui_reactor_nuke.png");

    public mio_icif_gui_nuke(NukeMenu menu, Inventory playerInventory, Component title) {
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

        // 绘制爆炸威力指示条
        NukeMenu menu = this.getMenu();
        if (menu != null) {
            int powerProgress = menu.getExplosionPowerProgress();
            // 威力条位置 x=79, y=100, 宽度=24, 高度=17
            if (powerProgress > 0) {
                guiGraphics.blit(ATLAS_TEXTURE, x + 79, y + 100, 0, (float) 2, (float) 192, powerProgress, 17, ATLAS_WIDTH, ATLAS_HEIGHT);
            }
        }
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        super.renderLabels(guiGraphics, mouseX, mouseY);

        // 绘制威力信息提示（当鼠标悬停在威力条上时）
        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;

        // 检查鼠标是否在威力条区域（x + 79, y + 100, 宽度 24, 高度 17）
        if (mouseX >= x + 79 && mouseX <= x + 79 + 24 && mouseY >= y + 100 && mouseY <= y + 100 + 17) {
            NukeMenu menu = this.getMenu();
            if (menu != null) {
                float power = menu.getExplosionPower();
                guiGraphics.renderTooltip(this.font,
                    Component.translatable("gui.mio_icif.nuke.explosion_power", power),
                    mouseX - x, mouseY - y);
            }
        }
    }
}