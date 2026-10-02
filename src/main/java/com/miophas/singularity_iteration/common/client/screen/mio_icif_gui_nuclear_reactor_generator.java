package com.miophas.singularity_iteration.common.client.screen;

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
public class mio_icif_gui_nuclear_reactor_generator extends mio_icif_screen<com.miophas.singularity_iteration.common.menu.generator.NuclearReactorGeneratorMenu> {

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

    public mio_icif_gui_nuclear_reactor_generator(com.miophas.singularity_iteration.common.menu.generator.NuclearReactorGeneratorMenu menu, Inventory playerInventory, Component title) {
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

        com.miophas.singularity_iteration.common.menu.generator.NuclearReactorGeneratorMenu menu = this.menu;
        if (menu == null) return;

        // 使用绿色文本显示热量和功率信息
        int heat = menu.getHeat();
        int maxHeat = menu.getMaxHeat();
        int outputPower = menu.getOutputPower();

        // 使用翻译键
        String heatText = Component.translatable("gui.mio_icif.nuclear_reactor.heat", heat, maxHeat).getString();
        String powerText = Component.translatable("gui.mio_icif.nuclear_reactor.power", outputPower).getString();
        String infoText = heatText + "    " + powerText;

        // 绘制绿色文本 (0x2A2E33 是绿色)
        guiGraphics.drawString(this.font, infoText, INFO_TEXT_X, INFO_TEXT_Y, 0x2A2E33, false);
        
        drawFeedRow(guiGraphics, mouseX, mouseY);
        drawCoolant(guiGraphics, mouseX, mouseY);

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


    // ---- liquid cooling gauges (right of the reactor grid) -----------------------------------------
    private static final int COOL_X = 191, HOT_X = 199, GAUGE_Y = 25, GAUGE_W = 7, GAUGE_H = 108;

    private void drawCoolant(GuiGraphics g, int mouseX, int mouseY) {
        int left = (this.width - this.imageWidth) / 2, top = (this.height - this.imageHeight) / 2;
        int cap = com.miophas.singularity_iteration.common.blockentity.generator.ReactorLiquidCooling.CAPACITY;
        gauge(g, COOL_X, this.menu.getCoolantAmount() / (float) cap, 0xFF7FD8F5, 0xFF2C7FB8);
        gauge(g, HOT_X, this.menu.getHotCoolantAmount() / (float) cap, 0xFFFFB04A, 0xFFD2452A);
        if (isHovering(mouseX, mouseY, left + COOL_X, top + GAUGE_Y, HOT_X + GAUGE_W - COOL_X, GAUGE_H)) {
            java.util.List<Component> tip = new java.util.ArrayList<>();
            int hu = this.menu.getCoolantHuPerMB();
            if (this.menu.getCoolantAmount() <= 0) tip.add(Component.translatable("tooltip.mio_icif.reactor.coolant.empty"));
            else {
                tip.add(Component.translatable("tooltip.mio_icif.reactor.coolant", String.format("%,d", this.menu.getCoolantAmount()), ""));
                tip.add(Component.translatable("tooltip.mio_icif.reactor.coolant.rate", hu,
                    String.format("%,d", (long) hu * com.miophas.singularity_iteration.common.blockentity.generator.ReactorLiquidCooling.MAX_FLOW_PER_CYCLE)));
            }
            tip.add(Component.translatable("tooltip.mio_icif.reactor.hot", String.format("%,d", this.menu.getHotCoolantAmount()), String.format("%,d", cap)));
            tip.add(Component.translatable("tooltip.mio_icif.reactor.coolant.tip").withStyle(net.minecraft.ChatFormatting.GRAY));
            g.renderComponentTooltip(this.font, tip, mouseX - left, mouseY - top);
        }
    }

    private void gauge(GuiGraphics g, int x, float fraction, int from, int to) {
        g.fill(x, GAUGE_Y, x + GAUGE_W, GAUGE_Y + GAUGE_H, 0xFF70757B);
        g.fill(x + 1, GAUGE_Y + 1, x + GAUGE_W - 1, GAUGE_Y + GAUGE_H - 1, 0xFF2A2E33);
        int h = Math.round(Math.max(0, Math.min(1, fraction)) * (GAUGE_H - 2));
        if (h > 0) g.fillGradient(x + 1, GAUGE_Y + GAUGE_H - 1 - h, x + GAUGE_W - 1, GAUGE_Y + GAUGE_H - 1, from, to);
        for (int k = 1; k < 4; k++) g.fill(x + GAUGE_W - 3, GAUGE_Y + k * GAUGE_H / 4, x + GAUGE_W - 1, GAUGE_Y + k * GAUGE_H / 4 + 1, 0x80FFFFFF);
    }

    // ---- feed rate configurator ------------------------------------------------------------------
    private static final int FEED_Y = 149;
    private static final int[][] FEED_BUTTONS = {{66, FEED_Y - 1, 11, 11}, {79, FEED_Y - 1, 11, 11}, {92, FEED_Y - 1, 17, 11}};
    private static final String[] FEED_LABELS = {"-", "+", "1x"};

    private void drawFeedRow(GuiGraphics g, int mouseX, int mouseY) {
        int left = (this.width - this.imageWidth) / 2, top = (this.height - this.imageHeight) / 2;
        int rate = this.menu.getFeedRate();
        g.drawString(this.font, Component.translatable("gui.mio_icif.nuclear_reactor.feed", rate), 7, FEED_Y, 0x2A2E33, false);
        for (int i = 0; i < 3; i++) {
            int[] b = FEED_BUTTONS[i];
            boolean hover = isHovering(mouseX, mouseY, left + b[0], top + b[1], b[2], b[3]);
            g.fill(b[0], b[1], b[0] + b[2], b[1] + b[3], 0xFF5E6670);
            g.fill(b[0] + 1, b[1] + 1, b[0] + b[2] - 1, b[1] + b[3] - 1, hover ? 0xFFF4F6F8 : 0xFFD9DDE1);
            g.drawString(this.font, FEED_LABELS[i], b[0] + (b[2] - this.font.width(FEED_LABELS[i])) / 2 + 1, b[1] + 2, 0xFF2A2E33, false);
        }
        double heat = com.miophas.singularity_iteration.common.blockentity.generator.mio_icif_nuclear_reactor_generator.heatFactor(rate);
        int heatColor = rate == 1 ? 0xFF4A525C : rate < 6 ? 0xFFB06A10 : 0xFFC0282A;
        String eu = Component.translatable("gui.mio_icif.nuclear_reactor.output_mult", String.format("%.1f", (double) rate)).getString();
        String ht = Component.translatable("gui.mio_icif.nuclear_reactor.heat_mult", String.format("%.1f", heat)).getString();
        g.pose().pushPose();
        g.pose().translate(114, FEED_Y + 1, 0);
        g.pose().scale(0.8F, 0.8F, 1);
        g.drawString(this.font, eu, 0, 0, 0xFF2E7D4F, false);
        g.drawString(this.font, ht, this.font.width(eu) + 6, 0, heatColor, false);
        g.pose().popPose();
        if (isHovering(mouseX, mouseY, left + 7, top + FEED_Y - 1, 200, 11)) {
            g.renderComponentTooltip(this.font, java.util.List.of(
                Component.translatable("gui.mio_icif.nuclear_reactor.feed.tip1"),
                Component.translatable("gui.mio_icif.nuclear_reactor.feed.tip2"),
                Component.translatable("gui.mio_icif.nuclear_reactor.feed.tip3")), mouseX - left, mouseY - top + 12);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && this.minecraft != null && this.minecraft.gameMode != null) {
            int left = (this.width - this.imageWidth) / 2, top = (this.height - this.imageHeight) / 2;
            for (int i = 0; i < 3; i++) {
                int[] b = FEED_BUTTONS[i];
                if (isHovering((int) mouseX, (int) mouseY, left + b[0], top + b[1], b[2], b[3])) {
                    int id = i == 2 ? 2 : (hasShiftDown() ? i + 3 : i);
                    this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, id);
                    return true;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }
}
