package com.singularity_iteration.mio_icif.Screen;

import com.singularity_iteration.mio_icif.Menu.Producer.IndustrialWorkbenchMenu;
import com.singularity_iteration.mio_icif.network.WorkbenchClearPacket;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.network.PacketDistributor;

@OnlyIn(Dist.CLIENT)
@SuppressWarnings("null")
public class mio_icif_gui_industrial_workbench extends mio_icif_screen<IndustrialWorkbenchMenu> {

    private static final ResourceLocation GUI_TEXTURE =
        ResourceLocation.parse("mio_icif:textures/gui/gui_industrial_workbench.png");

    private static final int GUI_WIDTH = 194;
    private static final int GUI_HEIGHT = 228;

    @SuppressWarnings("unused")
    private static final int CANCEL_U = 194;
    @SuppressWarnings("unused")
    private static final int CANCEL_V = 0;
    private static final int CANCEL_W = 14;
    private static final int CANCEL_H = 14;
    private static final int CANCEL_X = 14;
    private static final int CANCEL_Y = 62;

    public mio_icif_gui_industrial_workbench(IndustrialWorkbenchMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = GUI_WIDTH;
        this.imageHeight = GUI_HEIGHT;
    }

    @Override
    protected void init() {
        super.init();
        this.inventoryLabelY = this.imageHeight - 96;
        int guiLeft = (this.width - this.imageWidth) / 2;
        int guiTop = (this.height - this.imageHeight) / 2;

        this.addRenderableWidget(new AbstractWidget(guiLeft + CANCEL_X, guiTop + CANCEL_Y, CANCEL_W, CANCEL_H, Component.empty()) {
            @Override
            protected void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
                if (this.isHoveredOrFocused()) {
                    guiGraphics.fill(this.getX(), this.getY(), this.getX() + this.getWidth(), this.getY() + this.getHeight(), 0x80FFFFFF);
                }
            }

            @Override
            protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {
            }

            @Override
            public void onClick(double mouseX, double mouseY) {
                PacketDistributor.sendToServer(new WorkbenchClearPacket());
            }
        });
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;

        guiGraphics.blit(GUI_TEXTURE, x, y, 0, 0, this.imageWidth, this.imageHeight);
guiGraphics.blit(ATLAS_TEXTURE, x + CANCEL_X, y + CANCEL_Y, 0, (float) 34, (float) 243, CANCEL_W, CANCEL_H, ATLAS_WIDTH, ATLAS_HEIGHT);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
    }

    @Override
    public void containerTick() {
        super.containerTick();
    }
}