package com.singularity_iteration.mio_icif.Screen;

import com.singularity_iteration.mio_icif.Menu.OilRig.OilRigPanelMenu;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
@SuppressWarnings("null")
public class mio_icif_gui_oil_rig_panel extends mio_icif_screen<OilRigPanelMenu> {

    private static final ResourceLocation GUI_TEXTURE =
        ResourceLocation.parse("mio_icif:textures/gui/gui_default.png");

    public mio_icif_gui_oil_rig_panel(OilRigPanelMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 166;
        this.inventoryLabelY = this.imageHeight - 94;
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;
        guiGraphics.blit(GUI_TEXTURE, x, y, 0, 0, this.imageWidth, this.imageHeight);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        OilRigPanelMenu menu = this.getMenu();

        guiGraphics.drawString(this.font, this.title, this.titleLabelX, this.titleLabelY, 4210752, false);

        int drillX = menu.getDrillX();
        int drillY = menu.getDrillY();
        int drillZ = menu.getDrillZ();
        int coreState = menu.getCoreState();

        int labelY = 20;
        int valueY = 32;
        int leftX = 8;

        guiGraphics.drawString(this.font,
            Component.translatable("gui.mio_icif.oil_rig.coordinate"),
            leftX, labelY, 0x404040, false);

        String coordStr = String.format("X: %d  Y: %d  Z: %d", drillX, drillY, drillZ);
        guiGraphics.drawString(this.font, coordStr, leftX, valueY, 0x404040, false);

        labelY = 48;
        valueY = 60;
        guiGraphics.drawString(this.font,
            Component.translatable("gui.mio_icif.oil_rig.state"),
            leftX, labelY, 0x404040, false);

        Component stateText = getCoreStateText(coreState);
        guiGraphics.drawString(this.font, stateText, leftX, valueY, 0x404040, false);
    }

    private Component getCoreStateText(int coreState) {
        return switch (coreState) {
            case -1 -> Component.translatable("gui.mio_icif.oil_rig.state.no_energy")
                    .withStyle(ChatFormatting.RED);
            case 2 -> Component.translatable("gui.mio_icif.oil_rig.state.finished")
                    .withStyle(ChatFormatting.GREEN);
            case 3 -> Component.translatable("gui.mio_icif.oil_rig.state.working")
                    .withStyle(ChatFormatting.AQUA);
            default -> Component.translatable("gui.mio_icif.oil_rig.state.incomplete")
                    .withStyle(ChatFormatting.YELLOW);
        };
    }
}