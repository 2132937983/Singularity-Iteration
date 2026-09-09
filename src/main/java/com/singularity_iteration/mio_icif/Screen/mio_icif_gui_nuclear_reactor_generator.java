package com.singularity_iteration.mio_icif.Screen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/**
 * 核反应堆发电机的 GUI类
 * 54个槽位（6排 x 9列）用于放置燃料棒和散热器
 */
@OnlyIn(Dist.CLIENT)
@SuppressWarnings("null")
public class mio_icif_gui_nuclear_reactor_generator extends mio_icif_screen<com.singularity_iteration.mio_icif.Menu.Generator.NuclearReactorGeneratorMenu> {

    private static final ResourceLocation GUI_TEXTURE =
        ResourceLocation.parse("mio_icif:textures/gui/gui_nuclear_reactor_generator.png");

    // GUI 尺寸 - 大GUI以容纳54个槽位
    private static final int GUI_WIDTH = 212;
    private static final int GUI_HEIGHT = 243;

    // 反应堆槽位区域（54个槽位，6排 x 9列，但只有部分可用）
    private static final int REACTOR_SLOTS_X = 26;
    private static final int REACTOR_SLOTS_Y = 25;
    /* private static final int REACTOR_SLOTS_COLS = 9; */
    private static final int REACTOR_SLOTS_ROWS = 6;
    private static final int SLOT_SIZE = 18;
    
    // X号标志纹理位置（用于显示不可用的槽位）
    @SuppressWarnings("unused")
    private static final int X_MARK_TEXTURE_X = 213;
    @SuppressWarnings("unused")
    private static final int X_MARK_TEXTURE_Y = 1;
    private static final int X_MARK_SIZE = 16;

    // 电池槽位位置
    /* private static final int BATTERY_SLOT_X = 188; */
    /* private static final int BATTERY_SLOT_Y = 25; */

    // 热量条位置
    /* private static final int HEAT_BAR_X = 188; */
    /* private static final int HEAT_BAR_Y = 49; */
    /* private static final int HEAT_BAR_WIDTH = 16; */
    /* private static final int HEAT_BAR_HEIGHT = 54; */

    // 热量条纹理位置
    /* private static final int HEAT_BAR_TEXTURE_X = 212; */
    /* private static final int HEAT_BAR_TEXTURE_Y = 0; */

    // 能量条位置
    /* private static final int ENERGY_BAR_X = 188; */
    /* private static final int ENERGY_BAR_Y = 109; */
    /* private static final int ENERGY_BAR_WIDTH = 16; */
    /* private static final int ENERGY_BAR_HEIGHT = 54; */

    // 能量条纹理位置
    /* private static final int ENERGY_BAR_TEXTURE_X = 228; */
    /* private static final int ENERGY_BAR_TEXTURE_Y = 0; */

    // 玩家物品栏位置
    /* private static final int PLAYER_INV_START_X = 26; */
    /* private static final int PLAYER_INV_Y = 161; */
    /* private static final int HOTBAR_Y = 219; */

    public mio_icif_gui_nuclear_reactor_generator(com.singularity_iteration.mio_icif.Menu.Generator.NuclearReactorGeneratorMenu menu, Inventory playerInventory, Component title) {
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
    }

    // 文本显示位置
    private static final int INFO_TEXT_X = 7;
    private static final int INFO_TEXT_Y = 138;

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        super.renderLabels(guiGraphics, mouseX, mouseY);

        com.singularity_iteration.mio_icif.Menu.Generator.NuclearReactorGeneratorMenu menu = this.menu;
        if (menu == null) return;

        // 使用绿色文本显示热量和功率信息
        int heat = menu.getHeat();
        int maxHeat = menu.getMaxHeat();
        int outputPower = menu.getOutputPower();

        // 使用翻译键
        String heatText = Component.translatable("gui.mio_icif.nuclear_reactor.heat", heat, maxHeat).getString();
        String powerText = Component.translatable("gui.mio_icif.nuclear_reactor.power", outputPower).getString();
        String infoText = heatText + "    " + powerText;

        // 绘制绿色文本 (0x00FF00 是绿色)
        guiGraphics.drawString(this.font, infoText, INFO_TEXT_X, INFO_TEXT_Y, 0x00FF00, false);
        
        // 为不可用的槽位绘制 X 号标志（根据当前可用列数）
        int availableColumns = menu.getAvailableColumns();
        for (int row = 0; row < REACTOR_SLOTS_ROWS; row++) {
            for (int col = availableColumns; col < 9; col++) {
                int slotX = REACTOR_SLOTS_X + col * SLOT_SIZE;
                int slotY = REACTOR_SLOTS_Y + row * SLOT_SIZE;
                // 绘制 X 号标志（往左上偏移 1 格）
                guiGraphics.blit(ATLAS_TEXTURE, slotX, slotY, 0, (float) 64, (float) 192, X_MARK_SIZE, X_MARK_SIZE, ATLAS_WIDTH, ATLAS_HEIGHT);
            }
        }
    }

}