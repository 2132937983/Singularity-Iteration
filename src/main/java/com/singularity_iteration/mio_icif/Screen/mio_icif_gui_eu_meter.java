package com.singularity_iteration.mio_icif.Screen;

import com.singularity_iteration.mio_icif.Menu.Tool.mio_icif_meter_menu;
import com.singularity_iteration.mio_icif.Menu.Tool.mio_icif_meter_menu.MeterMode;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
@SuppressWarnings("null")
public class mio_icif_gui_eu_meter extends mio_icif_screen<mio_icif_meter_menu> {

    private static final ResourceLocation GUI_TEXTURE =
        ResourceLocation.parse("mio_icif:textures/gui/gui_tool_eu_meter.png");

    private static final int MODE_BTN_SIZE = 20;
    private static final int MODE_BTN_X1 = 112;
    private static final int MODE_BTN_X2 = 132;
    private static final int MODE_BTN_Y1 = 55;
    private static final int MODE_BTN_Y2 = 75;

    private static final int RESET_BTN_X = 26;
    private static final int RESET_BTN_Y = 111;
    private static final int RESET_BTN_W = 57;
    private static final int RESET_BTN_H = 12;

    public mio_icif_gui_eu_meter(mio_icif_meter_menu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 219;
    }

    @Override
    protected void init() {
        super.init();
        this.inventoryLabelY = this.imageHeight - 95;
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;

        guiGraphics.blit(GUI_TEXTURE, x, y, 0, 0, this.imageWidth, this.imageHeight);

        mio_icif_meter_menu menu = this.getMenu();
        MeterMode mode = menu.getMode();

        int modeU = 86;
        int modeV = switch (mode) {
            case EnergyIn -> 80;
            case EnergyOut -> 80;
            case EnergyGain -> 80;
            case Voltage -> 80;
        };
        int modeOffsetU = switch (mode) {
            case EnergyIn -> 0;
            case EnergyOut -> 42;
            case EnergyGain -> 84;
            case Voltage -> 126;
        };
guiGraphics.blit(ATLAS_TEXTURE, x + MODE_BTN_X1, y + MODE_BTN_Y1, 0, (float) modeU + modeOffsetU, (float) modeV, 40, 40, ATLAS_WIDTH, ATLAS_HEIGHT);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        super.renderLabels(guiGraphics, mouseX, mouseY);

        mio_icif_meter_menu menu = this.getMenu();
        MeterMode mode = menu.getMode();

        String unit = menu.isVoltageMode() ? "V" : "EU/t";

        guiGraphics.drawString(this.font,
            Component.translatable("item.mio_icif.item_tool_meter.gui.mode"), 115, 43, 0x215E32, false);

        guiGraphics.drawString(this.font,
            Component.translatable("item.mio_icif.item_tool_meter.gui.avg"), 15, 41, 0x215E32, false);
        guiGraphics.drawString(this.font,
            formatSI(menu.getResultAvg()) + unit, 15, 51, 0x215E32, false);

        guiGraphics.drawString(this.font,
            Component.translatable("item.mio_icif.item_tool_meter.gui.max_min"), 15, 64, 0x215E32, false);
        guiGraphics.drawString(this.font,
            formatSI(menu.getResultMax()) + unit, 15, 74, 0x215E32, false);
        guiGraphics.drawString(this.font,
            formatSI(menu.getResultMin()) + unit, 15, 84, 0x215E32, false);

        int seconds = menu.getResultCount() / 20;
        guiGraphics.drawString(this.font,
            Component.translatable("item.mio_icif.item_tool_meter.gui.cycle", seconds), 15, 100, 0x215E32, false);

        guiGraphics.drawString(this.font,
            Component.translatable("item.mio_icif.item_tool_meter.gui.reset"), 39, 114, 0x215E32, false);

        Component modeText = switch (mode) {
            case EnergyIn -> Component.translatable("item.mio_icif.item_tool_meter.mode.EnergyIn");
            case EnergyOut -> Component.translatable("item.mio_icif.item_tool_meter.mode.EnergyOut");
            case EnergyGain -> Component.translatable("item.mio_icif.item_tool_meter.mode.EnergyGain");
            case Voltage -> Component.translatable("item.mio_icif.item_tool_meter.mode.Voltage");
        };
        guiGraphics.drawString(this.font, modeText, 105, 100, 0x215E32, false);
    }

    private String formatSI(double value) {
        if (Math.abs(value) >= 1_000_000_000.0) {
            return String.format("%.2f G", value / 1_000_000_000.0);
        } else if (Math.abs(value) >= 1_000_000.0) {
            return String.format("%.2f M", value / 1_000_000.0);
        } else if (Math.abs(value) >= 1_000.0) {
            return String.format("%.2f k", value / 1_000.0);
        } else {
            return String.format("%.2f ", value);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && this.menu != null && this.minecraft != null && this.minecraft.gameMode != null) {
            int x = (this.width - this.imageWidth) / 2;
            int y = (this.height - this.imageHeight) / 2;

            int relX = (int) mouseX - x;
            int relY = (int) mouseY - y;

            if (relX >= MODE_BTN_X1 && relX < MODE_BTN_X1 + MODE_BTN_SIZE
                && relY >= MODE_BTN_Y1 && relY < MODE_BTN_Y1 + MODE_BTN_SIZE) {
                this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, MeterMode.EnergyIn.ordinal());
                return true;
            }
            if (relX >= MODE_BTN_X2 && relX < MODE_BTN_X2 + MODE_BTN_SIZE
                && relY >= MODE_BTN_Y1 && relY < MODE_BTN_Y1 + MODE_BTN_SIZE) {
                this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, MeterMode.EnergyOut.ordinal());
                return true;
            }
            if (relX >= MODE_BTN_X1 && relX < MODE_BTN_X1 + MODE_BTN_SIZE
                && relY >= MODE_BTN_Y2 && relY < MODE_BTN_Y2 + MODE_BTN_SIZE) {
                this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, MeterMode.EnergyGain.ordinal());
                return true;
            }
            if (relX >= MODE_BTN_X2 && relX < MODE_BTN_X2 + MODE_BTN_SIZE
                && relY >= MODE_BTN_Y2 && relY < MODE_BTN_Y2 + MODE_BTN_SIZE) {
                this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, MeterMode.Voltage.ordinal());
                return true;
            }

            if (relX >= RESET_BTN_X && relX < RESET_BTN_X + RESET_BTN_W
                && relY >= RESET_BTN_Y && relY < RESET_BTN_Y + RESET_BTN_H) {
                this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, 100);
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }
}

