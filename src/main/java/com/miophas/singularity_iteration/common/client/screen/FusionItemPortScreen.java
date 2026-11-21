package com.miophas.singularity_iteration.common.client.screen;

import com.miophas.singularity_iteration.common.blockentity.fusion.FusionItemPortBlockEntity;
import com.miophas.singularity_iteration.common.blockentity.fusion.FusionItemPortBlockEntity.Kind;
import com.miophas.singularity_iteration.common.item.normal.mio_icif_normal;
import com.miophas.singularity_iteration.common.item.reactor.mio_icif_reactors;
import com.miophas.singularity_iteration.common.menu.fusion.FusionItemPortMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/**
 * 0.1.11.97: the fuel injection port's and the disruption mitigation port's screen, drawn in code with {@link SiGuiTheme} like
 * the controller's: the port's slots (an empty one shows a faint picture of what goes in), the link to the controller and what
 * the controller holds, and the player's inventory.
 */
@OnlyIn(Dist.CLIENT)
public class FusionItemPortScreen extends AbstractContainerScreen<FusionItemPortMenu> {
    private static final int W = FusionItemPortMenu.W, H = FusionItemPortMenu.H, WELL_Y = 51, WELL_H = 24;

    public FusionItemPortScreen(FusionItemPortMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = W;
        imageHeight = H;
        inventoryLabelX = 8;
        inventoryLabelY = FusionItemPortMenu.INV_Y - 10;
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        renderTooltip(g, mouseX, mouseY);
    }

    /** What a slot takes, for its faint picture. */
    private ItemStack icon(int slot) {
        if (menu.kind() == Kind.COOLANT) return new ItemStack(com.miophas.singularity_iteration.common.item.normal.mio_icif_normal.FROZEN_NEON_PELLET.get());   // 0.1.11.137
        if (slot < FusionItemPortBlockEntity.TRITIUM_0) return new ItemStack(mio_icif_normal.DEUTERIUM_CELL.get());
        if (slot < FusionItemPortBlockEntity.EMPTY_0) return new ItemStack(mio_icif_normal.TRITIUM_CELL.get());
        return new ItemStack(mio_icif_normal.FUEL_ROD.get());
    }

    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        int x = leftPos, y = topPos;
        SiGuiTheme.panel(g, x, y, W, H);
        int[] xs = FusionItemPortMenu.slotX(menu.kind());
        for (int i = 0; i < xs.length; i++) {
            SiGuiTheme.slot(g, x + xs[i] - 1, y + FusionItemPortMenu.SLOT_Y - 1);
            // what goes in: its silhouette (0.1.11.151; a faint picture of the item before)
            if (!menu.getSlot(i).hasItem()) VanillaFrame.ghost(g, icon(i), x + xs[i], y + FusionItemPortMenu.SLOT_Y);
        }
        if (menu.kind() == Kind.FUEL) {
            // the empty rods come out: an arrow from the fuel to them
            int ax = x + xs[3] + 22, ay = y + FusionItemPortMenu.SLOT_Y + 7;
            g.fill(ax, ay, ax + 10, ay + 2, SiGuiTheme.ACCENT_DIM);
            g.fill(ax + 10, ay - 2, ax + 11, ay + 4, SiGuiTheme.ACCENT_DIM);
            g.fill(ax + 11, ay - 1, ax + 12, ay + 3, SiGuiTheme.ACCENT_DIM);
            g.fill(ax + 12, ay, ax + 13, ay + 2, SiGuiTheme.ACCENT_DIM);
        }
        SiGuiTheme.well(g, x + 7, y + WELL_Y, W - 14, WELL_H);
        for (int row = 0; row < 3; row++)
            for (int col = 0; col < 9; col++)
                SiGuiTheme.slot(g, x + 7 + col * 18, y + FusionItemPortMenu.INV_Y - 1 + row * 18);
        for (int col = 0; col < 9; col++) SiGuiTheme.slot(g, x + 7 + col * 18, y + FusionItemPortMenu.HOTBAR_Y - 1);
        SiGuiTheme.led(g, x + W - 14, y + 6, menu.linked() ? SiGuiTheme.GOOD : SiGuiTheme.LED_OFF);
    }

    /** A label centred over the slots from {@code first} to {@code last}. */
    private void groupLabel(GuiGraphics g, String key, int first, int last, int maxWidth) {
        int[] xs = FusionItemPortMenu.slotX(menu.kind());
        Component text = Component.translatable(key);
        int centre = (xs[first] + xs[last] + 16) / 2, w = Math.min(maxWidth, font.width(text));
        mio_icif_screen.drawFitted(g, font, text, centre - w / 2, FusionItemPortMenu.SLOT_Y - 11, maxWidth, SiGuiTheme.TEXT, false);
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
        mio_icif_screen.drawFitted(g, font, title, 8, 6, W - 30, SiGuiTheme.TEXT, false);
        g.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, SiGuiTheme.TEXT, false);
        if (menu.kind() == Kind.FUEL) {
            groupLabel(g, "gui.mio_icif.fusion_port.deuterium", 0, 1, 54);
            groupLabel(g, "gui.mio_icif.fusion_port.tritium", 2, 3, 54);
            groupLabel(g, "gui.mio_icif.fusion_port.empty", 4, 5, 46);
        } else {
            groupLabel(g, "gui.mio_icif.fusion_port.coolant", 0, 3, W - 16);
        }
        int tx = 11, tw = W - 22;
        Component link = Component.translatable(menu.linked() ? "gui.mio_icif.fusion_port.linked" : "gui.mio_icif.fusion_port.unlinked");
        mio_icif_screen.drawFitted(g, font, link, tx, WELL_Y + 3, tw, menu.linked() ? SiGuiTheme.GOOD : SiGuiTheme.BAD, false);
        if (menu.linked()) {
            Component stock = menu.kind() == Kind.FUEL
                ? Component.translatable("gui.mio_icif.fusion_port.fuel_stock", FusionControllerScreen.compact(menu.first()), FusionControllerScreen.compact(menu.second()))
                : Component.translatable("gui.mio_icif.fusion_port.coolant_stock", menu.first());
            mio_icif_screen.drawFitted(g, font, stock, tx, WELL_Y + 13, tw, SiGuiTheme.TEXT_SCREEN, false);
        }
    }
}
