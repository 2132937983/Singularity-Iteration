package com.miophas.singularity_iteration.common.client.screen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import com.miophas.singularity_iteration.common.menu.huentity.HeatGeneratorElcMenu;

/**
 * 电力发热机的 GUI类
 */
@OnlyIn(Dist.CLIENT)
@SuppressWarnings("null")
public class mio_icif_gui_heat_generator_elc extends mio_icif_screen<com.miophas.singularity_iteration.common.menu.huentity.HeatGeneratorElcMenu> {

    private static final ResourceLocation GUI_TEXTURE =
            ResourceLocation.parse("mio_icif:textures/gui/gui_heat_generator_elc.png");

    // GUI 尺寸
    private static final int GUI_WIDTH = 176;
    private static final int GUI_HEIGHT = 166;

    // 闪电标志位置
    private static final int ENERGY_ICON_X = 9;
    private static final int ENERGY_ICON_Y = 43;

    // 热能标志位置
    private static final int HEAT_ICON_X = 106;
    private static final int HEAT_ICON_Y = 36;
    private static final int HEAT_ICON_WIDTH = 14;
    private static final int HEAT_ICON_HEIGHT = 14;

    // 热能标志纹理位置
    private static final int HEAT_ICON_TEXTURE_X = 168;
    private static final int HEAT_ICON_TEXTURE_Y = 227;

    // 电池槽位位置

    // 线圈槽位置（2排5列，紧凑排列18x18）
    @SuppressWarnings("unused")
    private static final int COIL_START_X = 44;
    @SuppressWarnings("unused")
    private static final int COIL_START_Y = 27;
    @SuppressWarnings("unused")
    private static final int COIL_COLS = 5;
    @SuppressWarnings("unused")
    private static final int COIL_ROWS = 2;
    @SuppressWarnings("unused")
    private static final int COIL_SPACING = 18; // 紧凑排列，无间隔

    // 热能文本显示位置
    private static final int HEAT_TEXT_X = 35;
    private static final int HEAT_TEXT_Y = 68;
    // 热能文本颜色 (57c4da)
    private static final int HEAT_TEXT_COLOR = 0x2A2E33;

    public mio_icif_gui_heat_generator_elc(HeatGeneratorElcMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = GUI_WIDTH;
        this.imageHeight = GUI_HEIGHT;
    }

    /** 0.1.11.146: the calm layout (title on the left, no lamp or upgrade hint). */
    @Override
    protected boolean calmChrome() { return true; }

    /**
     * 0.1.11.146: drawn in code in the vanilla look instead of the old texture: the panel and sunk slots, the energy bolt
     * over the battery, the heat output twice the font size under the coils with the most the coils give beside it (or
     * what to do when there are none). Empty slots show what goes in them.
     */
    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int x = leftPos, y = topPos;
        VanillaFrame.panel(guiGraphics, x, y, imageWidth, imageHeight);
        for (var slot : menu.slots) {
            if (slot.x < 0 || slot.x > imageWidth) continue;
            VanillaFrame.slot(guiGraphics, x + slot.x - 1, y + slot.y - 1);
        }
        HeatGeneratorElcMenu menu = this.getMenu();
        long max = Math.max(1, Integer.toUnsignedLong(menu.getMaxEnergy()));
        VanillaFrame.bolt(guiGraphics, x + BOLT_X, y + BOLT_Y, Integer.toUnsignedLong(menu.getEnergy()) / (double) max);
        VanillaFrame.bigText(guiGraphics, Component.literal(menu.getTransmitHeat() + " HU/t"), x + COIL_START_X, y + HEAD_Y, 0x1C2524);
        // the most the coils give beside it when there is room; with no coils the ghost coils and their tooltip say what to do
        if (menu.getCoilCount() > 0) {
            Component side = Component.translatable("gui.mio_icif.max").append(" " + menu.getMaxHeatEmittedPerTick() + " HU/t");
            int headEnd = x + COIL_START_X + 2 * this.font.width(menu.getTransmitHeat() + " HU/t");
            int sx = x + imageWidth - 8 - this.font.width(side);
            if (sx > headEnd + 4) guiGraphics.drawString(this.font, side, sx, y + HEAD_Y + 4, ReadoutPanel.MUTED, false);
        }
        boolean noCoils = menu.getCoilCount() == 0;
        for (var slot : menu.slots) {
            if (slot.hasItem() || slot.x < 0 || slot.y > 80) continue;
            if (slot.index == HeatGeneratorElcMenu.BATTERY_SLOT)
                VanillaFrame.ghost(guiGraphics, item("mio_icif:normal/item_chargebat_lev0"), x + slot.x, y + slot.y);
            else if (noCoils) VanillaFrame.ghost(guiGraphics, item("mio_icif:resource/item_coil"), x + slot.x, y + slot.y);
        }
    }

    /** The bolt centred over the battery slot's column (outer edge 7..25), its centre of mass on the column. */
    private static final int BOLT_X = 11, BOLT_Y = 45, HEAD_Y = 66;

    private static net.minecraft.world.item.ItemStack item(String id) {
        return new net.minecraft.world.item.ItemStack(net.minecraft.core.registries.BuiltInRegistries.ITEM.get(net.minecraft.resources.ResourceLocation.parse(id)));
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        // 不调用super.renderLabels() 以避免渲染Inventory 文本标签; the title was missing entirely
        drawTitle(guiGraphics);

        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;

        HeatGeneratorElcMenu menu = this.getMenu();
        if (menu == null) return;

        // 线圈槽为空时，悬停任意一个线圈槽都会提示应放入的物品。
        if (menu.getCoilCount() == 0 && mouseX >= x + COIL_START_X && mouseX < x + COIL_START_X + COIL_COLS * COIL_SPACING
                && mouseY >= y + COIL_START_Y && mouseY < y + COIL_START_Y + COIL_ROWS * COIL_SPACING) {
            guiGraphics.renderTooltip(this.font,
                    Component.translatable("gui.mio_icif.heat_generator_elc.insert_coil"), mouseX - x, mouseY - y);
        }

        // 检查鼠标是否在电能标志区域
        if (mouseX >= x + BOLT_X - 1 && mouseX <= x + BOLT_X + 10 && mouseY >= y + BOLT_Y - 1 && mouseY <= y + BOLT_Y + 14) {
            guiGraphics.renderTooltip(this.font,
                    Component.literal("Energy: " + menu.getEnergy() + "/" + menu.getMaxEnergy() + " EU"),
                    mouseX - x, mouseY - y);
        }

        // 0.1.11.146: the heat buffer's icon is gone (the output says it); its tooltip with it
    }

    // 0.1.11.3: no containerTick override. It copied the client block entity's values (never ticked on the client and
    // only updated when the chunk loaded) over the values the menu syncs from the server each tick, so the progress,
    // energy and heat bars stayed frozen or flickered.
}
