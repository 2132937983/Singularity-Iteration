package com.singularity_iteration.mio_icif.Screen;

import com.singularity_iteration.mio_icif.Blocks.entity.KUEntity.mio_icif_lathe;
import com.singularity_iteration.mio_icif.Menu.Producer.LatheMenu;
import com.singularity_iteration.mio_icif.api.item.ILatheItem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/**
 * 车床 GUI
 * 对齐 IC2 原版布局：
 * - 5 个按钮对应 5 个位置
 * - 显示加工件纹理（5段）
 * - 显示各段厚度信息
 * - 槽位：车刀(10,30) 加工件(10,12) 输出(10,57)
 */
@OnlyIn(Dist.CLIENT)
@SuppressWarnings("null")
public class mio_icif_gui_lathe extends mio_icif_screen<LatheMenu> {

    private static final ResourceLocation GUI_TEXTURE =
        ResourceLocation.parse("mio_icif:textures/gui/gui_lathe.png");
    private static final int GUI_WIDTH = 176;
    private static final int GUI_HEIGHT = 176;
    private static final int BUTTON_START_X = 40;
    private static final int BUTTON_Y = 15;
    private static final int BUTTON_WIDTH = 24;
    private static final int BUTTON_HEIGHT = 20;
    private static final int BUTTON_COUNT = 5;
    private static final int BUTTON_GAP = 24;

    // 加工件纹理渲染位置
    private static final int LATHE_RENDER_X = 40;
    private static final int LATHE_RENDER_Y = 45;
    
    // 信息文本显示位置
    private static final int INFO_X = 40;
    private static final int INFO_Y = 37;

    public mio_icif_gui_lathe(LatheMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = GUI_WIDTH;
        this.imageHeight = GUI_HEIGHT;
        this.inventoryLabelY = this.imageHeight - 94;
    }

    @Override
    protected void init() {
        super.init();
        // 创建 5 个加工按钮
        for (int i = 0; i < BUTTON_COUNT; i++) {
            final int position = i;
            int buttonX = this.leftPos + BUTTON_START_X + i * BUTTON_GAP;
            int buttonY = this.topPos + BUTTON_Y;
            this.addRenderableWidget(Button.builder(
                    Component.literal(""),
                    btn -> handleButtonClick(position)
                )
                .bounds(buttonX, buttonY, BUTTON_WIDTH, BUTTON_HEIGHT)
                .build());
        }
    }

    private void handleButtonClick(int position) {
        if (minecraft != null && minecraft.player != null) {
            LatheMenu latheMenu = this.getMenu();
            if (latheMenu != null) {
                latheMenu.clickMenuButton(minecraft.player, position);
            }
        }
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;
        guiGraphics.blit(GUI_TEXTURE, x, y, 0, 0, this.imageWidth, this.imageHeight);

        LatheMenu menu = this.getMenu();
        if (menu == null || menu.getBlockEntity() == null) return;

        int kuPixels = menu.getKUBufferPixels();
        if (kuPixels > 0) {
            guiGraphics.blit(ATLAS_TEXTURE, x + 152, y + 8, 0, (float) 11, (float) 2, 16, 60, ATLAS_WIDTH, ATLAS_HEIGHT);
            var be = menu.getBlockEntity();
            int kuHeight = (int)(60.0F * be.kUBuffer / mio_icif_lathe.MAX_KU_BUFFER);
            guiGraphics.blit(ATLAS_TEXTURE, x + 152, y + 68 - kuHeight, 0, (float) 11, (float) 62 - kuHeight, 16, kuHeight, ATLAS_WIDTH, ATLAS_HEIGHT);
        }

        if (menu.getBlockEntity().getLatheTexture() != null) {
            renderLatheItem(guiGraphics, menu, x, y);
        }
    }

    /**
     * 渲染加工件纹理到 GUI
     */
    private void renderLatheItem(GuiGraphics guiGraphics, LatheMenu menu, int guiX, int guiY) {
        ItemStack latheStack = menu.getBlockEntity().getItemHandler().getStackInSlot(mio_icif_lathe.LATHE_SLOT);
        if (latheStack.isEmpty() || !(latheStack.getItem() instanceof ILatheItem)) return;

        ILatheItem l = (ILatheItem) latheStack.getItem();
        int[] state = l.getCurrentState(latheStack);
        int max = l.getWidth(latheStack);
        ResourceLocation texture = l.getTexture(latheStack);

        if (texture == null || state.length == 0) return;

        guiGraphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);

        int segLength = 24;
        int textureWidth = 32;
        for (int j = 0; j < 5 && j < state.length; j++) {
            int segWidth = (int) ((float) textureWidth / max * state[j] + 0.5F);
            int offset = (int) ((textureWidth - segWidth) / 2.0F + 0.5F);
            
            int bx = guiX + LATHE_RENDER_X + segLength * j;
            int by = guiY + LATHE_RENDER_Y + offset;
            
            guiGraphics.blit(texture, bx, by, 
                segLength * j, offset,
                segLength, segWidth,
                120, 32);
        }

        for (int j = 0; j < 5 && j < state.length; j++) {
            int bx = guiX + INFO_X + segLength * j;
            int by = guiY + INFO_Y;
            guiGraphics.drawString(this.font, 
                Component.translatable("ic2.Lathe.gui.info", state[j], max),
                bx, by, 0x404040, false);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        super.renderLabels(guiGraphics, mouseX, mouseY);
        
        guiGraphics.drawString(this.font, this.title, this.titleLabelX, this.titleLabelY, 0x404040, false);
    }
}