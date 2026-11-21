package com.miophas.singularity_iteration.common.client.screen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import com.miophas.singularity_iteration.common.menu.huentity.FluidHeatGeneratorMenu;

/**
 * 流体加热机的 GUI类
 */
@OnlyIn(Dist.CLIENT)
@SuppressWarnings("null")
public class mio_icif_gui_fluid_heat_generator extends mio_icif_screen<com.miophas.singularity_iteration.common.menu.huentity.FluidHeatGeneratorMenu> {

    private static final ResourceLocation GUI_TEXTURE =
        ResourceLocation.parse("mio_icif:textures/gui/gui_fluid_heat_generator.png");

    // GUI 尺寸
    private static final int GUI_WIDTH = 176;
    private static final int GUI_HEIGHT = 166;

    // 槽位位置
    /* private static final int FUEL_BUCKET_SLOT_X = 27; */
    /* private static final int FUEL_BUCKET_SLOT_Y = 21; */
    /* private static final int EMPTY_BUCKET_SLOT_X = 27; */
    /* private static final int EMPTY_BUCKET_SLOT_Y = 54; */

    // 燃料槽位置（相对背景）
    private static final int FUEL_TANK_X = 70;
    private static final int FUEL_TANK_Y = 20;

    // 火焰标志位置
    private static final int FLAME_ICON_X = 81;
    private static final int FLAME_ICON_Y = 29;
    private static final int FLAME_ICON_WIDTH = 14;
    private static final int FLAME_ICON_HEIGHT = 14;

    // 火焰标志纹理位置 (176, 0)，uv(14, 14)

    // 发热效率文本位置
    private static final int HEAT_RATE_TEXT_X = 95;
    private static final int HEAT_RATE_TEXT_Y = 32;
    // 文本颜色 (57c4da)
    private static final int TEXT_COLOR = 0x2A2E33;

    public mio_icif_gui_fluid_heat_generator(FluidHeatGeneratorMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = GUI_WIDTH;
        this.imageHeight = GUI_HEIGHT;
    }

    /** 0.1.11.146: the calm layout (title on the left, no lamp or upgrade hint). */
    @Override
    protected boolean calmChrome() { return true; }

    /**
     * 0.1.11.146: drawn in code in the vanilla look instead of the old texture: the panel and sunk slots, the fuel tank as a
     * sunk window with the fluid's own texture, the heat output twice the font size with the most it can give under it,
     * and a flat bar for the millibucket burning now. Empty slots show what goes in them.
     */
    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int x = leftPos, y = topPos;
        VanillaFrame.panel(guiGraphics, x, y, imageWidth, imageHeight);
        for (var slot : menu.slots) {
            if (slot.x < 0 || slot.x > imageWidth) continue;
            VanillaFrame.slot(guiGraphics, x + slot.x - 1, y + slot.y - 1);
        }
        FluidHeatGeneratorMenu menu = this.getMenu();
        VanillaFrame.inset(guiGraphics, x + FUEL_TANK_X, y + FUEL_TANK_Y, FLUID_TANK_BG_WIDTH, FLUID_TANK_BG_HEIGHT, VanillaFrame.SLOT);
        int room = FLUID_TANK_BG_HEIGHT - 2, cap = Math.max(1, menu.getFuelCapacity());
        int level = menu.getFuelAmount() <= 0 ? 0 : Math.max(1, Math.round(room * Math.min(1F, menu.getFuelAmount() / (float) cap)));
        VanillaFrame.fluid(guiGraphics, x + FUEL_TANK_X + 1, y + FUEL_TANK_Y + 1 + room - level, FLUID_TANK_BG_WIDTH - 2, level, menu.getFuelFluid());
        // the output: twice the font size; the most it gives under it (IC2's Emit / MaxEmit, the sticky transmitHeat)
        VanillaFrame.bigText(guiGraphics, Component.literal(menu.getTransmitHeat() + " HU/t"), x + TEXT_X, y + 22, 0x1C2524);
        guiGraphics.drawString(this.font, Component.translatable("gui.mio_icif.max").append(" " + menu.getMaxHeatOutput() + " HU/t"),
            x + TEXT_X, y + 42, ReadoutPanel.MUTED, false);
        // the millibucket burning now, left of the bar used up
        int burn = menu.getBurnProgress();
        VanillaFrame.flatBar(guiGraphics, x + TEXT_X, y + 56, imageWidth - 8 - TEXT_X, menu.getBurnTime() > 0 && burn < 100 ? (100 - burn) / 100.0 : 0);
        if (!menu.getSlot(FluidHeatGeneratorMenu.FUEL_BUCKET_SLOT).hasItem())
            VanillaFrame.ghost(guiGraphics, ghost("mio_icif:cell/item_cell_biogas"), x + 27, y + 21);
        if (!menu.getSlot(FluidHeatGeneratorMenu.EMPTY_BUCKET_SLOT).hasItem())
            VanillaFrame.ghost(guiGraphics, ghost("mio_icif:cell/item_cell_empty"), x + 27, y + 54);
    }

    private static final int TEXT_X = 98;

    private static net.minecraft.world.item.ItemStack ghost(String id) {
        return new net.minecraft.world.item.ItemStack(net.minecraft.core.registries.BuiltInRegistries.ITEM.get(ResourceLocation.parse(id)));
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        super.renderLabels(guiGraphics, mouseX, mouseY);

        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;

        FluidHeatGeneratorMenu menu = this.getMenu();
        if (menu == null) return;

        // 检查鼠标是否在燃料槽区域
        if (mouseX >= x + FUEL_TANK_X && mouseX <= x + FUEL_TANK_X + FLUID_TANK_BG_WIDTH &&
            mouseY >= y + FUEL_TANK_Y && mouseY <= y + FUEL_TANK_Y + FLUID_TANK_BG_HEIGHT) {
            guiGraphics.renderTooltip(this.font,
                Component.literal((!menu.getFuelFluid().isEmpty() ? menu.getFuelFluid().getHoverName().getString() + ": " : "") + menu.getFuelAmount() + "/" + menu.getFuelCapacity() + " mB"),
                mouseX - x, mouseY - y);
        }
    }

    @Override
    public void containerTick() {
        super.containerTick();
        // 数据通过 broadcastChanges() 方法同步
    }


}