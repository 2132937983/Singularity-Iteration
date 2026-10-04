package com.miophas.singularity_iteration.common.client.screen;

import com.miophas.singularity_iteration.common.menu.producer.ResinCollectorMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class ResinCollectorScreen extends mio_icif_screen<ResinCollectorMenu> {
    public ResinCollectorScreen(ResinCollectorMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 176; imageHeight = 149;
        inventoryLabelY = 55;
    }
    @Override protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        SiGuiTheme.panel(graphics, leftPos, topPos, imageWidth, imageHeight);
        for (var slot : menu.slots) SiGuiTheme.slot(graphics, leftPos + slot.x - 1, topPos + slot.y - 1);
    }
}
