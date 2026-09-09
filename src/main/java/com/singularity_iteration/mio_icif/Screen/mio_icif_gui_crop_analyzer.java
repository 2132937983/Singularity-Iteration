package com.singularity_iteration.mio_icif.Screen;

import com.singularity_iteration.mio_icif.Items.Crop.CropSeedItem;
import com.singularity_iteration.mio_icif.Menu.Tool.CropAnalyzerMenu;
import com.singularity_iteration.mio_icif.api.crop.PlantType;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import java.util.List;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
@SuppressWarnings("null")
public class mio_icif_gui_crop_analyzer extends mio_icif_screen<CropAnalyzerMenu> {

    private static final ResourceLocation BACKGROUND =
        ResourceLocation.parse("mio_icif:textures/gui/gui_cropnalyzer.png");

    public mio_icif_gui_crop_analyzer(CropAnalyzerMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 223;
    }

    @Override
    protected void init() {
        super.init();
        this.inventoryLabelX = 8;
        this.inventoryLabelY = 129;
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;
        guiGraphics.blit(BACKGROUND, x, y, 0, 0, this.imageWidth, this.imageHeight);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {

        ItemStack output = this.menu.getSlot(CropAnalyzerMenu.SLOT_OUTPUT).getItem();
        if (output.isEmpty() || !(output.getItem() instanceof CropSeedItem)) {
            drawText(guiGraphics, 8, 37, "UNKNOWN", 0xFFFFFF);
            return;
        }

        int scannedLevel = CropSeedItem.getScanLevel(output);
        PlantType crop = CropSeedItem.getPlantType(output);

        if (scannedLevel == 0) {
            drawText(guiGraphics, 8, 37, "UNKNOWN", 0xFFFFFF);
            return;
        }

        if (scannedLevel >= 1 && crop != null) {
            String name = Component.translatable(crop.getTranslationKey()).getString();
            drawText(guiGraphics, 8, 37, name, 0xFFFFFF);
        }

        if (scannedLevel >= 2 && crop != null) {
            drawText(guiGraphics, 8, 50, "Tier: " + toRomanNumeral(crop.getStats().getLevel()), 0xFFFFFF);
            drawText(guiGraphics, 8, 73, "Discovered by:", 0xFFFFFF);
            drawText(guiGraphics, 8, 86, crop.getFoundBy(), 0xFFFFFF);
        }

        if (scannedLevel >= 3 && crop != null) {
            List<String> extraInfo = crop.getExtraInfo();
            if (extraInfo.size() > 0) {
                drawText(guiGraphics, 8, 109, extraInfo.get(0), 0xFFFFFF);
            }
            if (extraInfo.size() > 1) {
                drawText(guiGraphics, 8, 122, extraInfo.get(1), 0xFFFFFF);
            }
        }

        if (scannedLevel >= 4) {
            int growth = CropSeedItem.getGrowthFromStack(output);
            int gain = CropSeedItem.getGainFromStack(output);
            int resistance = CropSeedItem.getResistanceFromStack(output);

            drawText(guiGraphics, 118, 37, Component.translatable("gui.mio_icif.crop_analyzer.growth").getString(), 0xAEAEBE);
            drawText(guiGraphics, 118, 50, Integer.toString(growth), 0xAEAEBE);
            drawText(guiGraphics, 118, 73, Component.translatable("gui.mio_icif.crop_analyzer.yield").getString(), 0xEEB422);
            drawText(guiGraphics, 118, 86, Integer.toString(gain), 0xEEB422);
            drawText(guiGraphics, 118, 109, Component.translatable("gui.mio_icif.crop_analyzer.resilience").getString(), 0x00CEC9);
            drawText(guiGraphics, 118, 122, Integer.toString(resistance), 0x00CEC9);
        }
    }

    private void drawText(GuiGraphics guiGraphics, int x, int y, String text, int color) {
        guiGraphics.drawString(this.font, text, x, y, color, false);
    }

    private static String toRomanNumeral(int level) {
        return switch (level) {
            case 1 -> "I";
            case 2 -> "II";
            case 3 -> "III";
            case 4 -> "IV";
            case 5 -> "V";
            case 6 -> "VI";
            case 7 -> "VII";
            case 8 -> "VIII";
            case 9 -> "IX";
            case 10 -> "X";
            case 11 -> "XI";
            case 12 -> "XII";
            case 13 -> "XIII";
            case 14 -> "XIV";
            case 15 -> "XV";
            default -> "XVI";
        };
    }
}

