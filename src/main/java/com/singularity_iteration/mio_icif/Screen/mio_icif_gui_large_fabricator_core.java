package com.singularity_iteration.mio_icif.Screen;

import com.singularity_iteration.mio_icif.Blocks.Environment.fluid.mio_icif_fluids;
import com.singularity_iteration.mio_icif.Menu.Producer.LargeFabricatorCoreMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.fluids.FluidStack;

@OnlyIn(Dist.CLIENT)
@SuppressWarnings("null")
public class mio_icif_gui_large_fabricator_core extends mio_icif_screen<LargeFabricatorCoreMenu> {

    private static final ResourceLocation GUI_TEXTURE =
        ResourceLocation.parse("mio_icif:textures/gui/gui_default.png");

    private static final int GUI_WIDTH = 176;
    private static final int GUI_HEIGHT = 166;

    private static final int UU_TANK_X = 96;
    private static final int UU_TANK_Y = 22;

    private static final int AMPLIFIER_LABEL_X = 8;
    private static final int AMPLIFIER_VALUE_X = 8;

    private static final int MODULE_INFO_X = 8;
    private static final int MODULE_INFO_Y = 22;

    private final Component amplifierLabel;
    private final Component inputModuleLabel;
    private final Component tankModuleLabel;
    private final Component scrapModuleLabel;

    public mio_icif_gui_large_fabricator_core(LargeFabricatorCoreMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = GUI_WIDTH;
        this.imageHeight = GUI_HEIGHT;
        this.inventoryLabelY = this.imageHeight - 94;

        this.amplifierLabel = Component.translatable("gui.mio_icif.matter_elc.amplifier");
        this.inputModuleLabel = Component.translatable("gui.mio_icif.large_fabricator.input_module");
        this.tankModuleLabel = Component.translatable("gui.mio_icif.large_fabricator.tank_module");
        this.scrapModuleLabel = Component.translatable("gui.mio_icif.large_fabricator.scrap_module");
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;

        guiGraphics.blit(GUI_TEXTURE, x, y, 0, 0, this.imageWidth, this.imageHeight);

        LargeFabricatorCoreMenu menu = this.menu;
        if (menu != null) {
            drawFluidTank(guiGraphics, x + UU_TANK_X, y + UU_TANK_Y,
                new FluidStack(mio_icif_fluids.UUMATTER.get(), menu.getFluidAmount()),
                menu.getFluidAmount(),
                menu.getFluidCapacity());
        }
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        guiGraphics.drawString(this.font, this.title, this.titleLabelX, this.titleLabelY, 4210752, false);

        LargeFabricatorCoreMenu menu = this.menu;
        if (menu != null) {
            int scrap = menu.getScrapValue();
            int inputCount = menu.getInputModuleCount();
            int tankCount = menu.getTankModuleCount();
            int scrapCount = menu.getScrapModuleCount();

            int currentY = MODULE_INFO_Y;

            if (scrap > 0) {
                guiGraphics.drawString(this.font, this.amplifierLabel, AMPLIFIER_LABEL_X, currentY, 4210752, false);
                Component scrapValue = Component.literal(String.valueOf(scrap));
                guiGraphics.drawString(this.font, scrapValue, AMPLIFIER_VALUE_X, currentY + 12, 4210752, false);
                currentY += 24;
            }

            if (inputCount > 0 || tankCount > 0 || scrapCount > 0) {
                guiGraphics.drawString(this.font,
                    Component.literal(this.inputModuleLabel.getString() + ": " + inputCount),
                    MODULE_INFO_X, currentY, 4210752, false);
                currentY += 10;
                guiGraphics.drawString(this.font,
                    Component.literal(this.tankModuleLabel.getString() + ": " + tankCount),
                    MODULE_INFO_X, currentY, 4210752, false);
                currentY += 10;
                guiGraphics.drawString(this.font,
                    Component.literal(this.scrapModuleLabel.getString() + ": " + scrapCount),
                    MODULE_INFO_X, currentY, 4210752, false);
            }
        }
    }

    @Override
    protected void renderCustomTooltips(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        super.renderCustomTooltips(guiGraphics, mouseX, mouseY);

        LargeFabricatorCoreMenu menu = this.menu;
        if (menu == null) return;

        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;
        int tankScreenX = x + UU_TANK_X;
        int tankScreenY = y + UU_TANK_Y;

        if (mouseX >= tankScreenX && mouseX < tankScreenX + 20 &&
            mouseY >= tankScreenY && mouseY < tankScreenY + 55) {
            Component tooltip = Component.literal(mio_icif_fluids.UUMATTER.get().getFluidType().getDescription().getString() + ": " + menu.getFluidAmount() + "/" + menu.getFluidCapacity() + " mB");
            guiGraphics.renderTooltip(this.font, tooltip, mouseX, mouseY);
        }
    }

    @Override
    public void containerTick() {
        super.containerTick();
    }
}