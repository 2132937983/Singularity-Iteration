package com.miophas.singularity_iteration.common.client.screen;

import com.miophas.singularity_iteration.common.menu.storage.TesuControllerMenu;
import com.miophas.singularity_iteration.common.multiblock.TesuStructure;
import java.util.Locale;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/**
 * 0.1.11.157: the TESU controller's screen, on the GESU core's frame: the stored energy (bar and tooltip), the ports and the
 * output they can send; while the structure is not formed, the first wrong position and what belongs there.
 */
@OnlyIn(Dist.CLIENT)
@SuppressWarnings("null")
public class TesuControllerScreen extends mio_icif_screen<TesuControllerMenu> {
    private static final ResourceLocation GUI_TEXTURE = ResourceLocation.parse("mio_icif:textures/gui/gui_bat_box.png");
    private static final int ENERGY_BAR_X = 79, ENERGY_BAR_Y = 34, TEXT_X = 80, TEXT_W = 176 - 6 - 80;

    public TesuControllerScreen(TesuControllerMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 196;
    }

    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        int x = (width - imageWidth) / 2, y = (height - imageHeight) / 2;
        g.blit(GUI_TEXTURE, x, y, 0, 0, imageWidth, imageHeight);
        drawEnergyBar(g, x + ENERGY_BAR_X, y + ENERGY_BAR_Y, menu.getEnergyProgressPixels());
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
        super.renderLabels(g, mouseX, mouseY);
        int x = (width - imageWidth) / 2, y = (height - imageHeight) / 2;
        if (mouseX >= x + ENERGY_BAR_X && mouseX <= x + ENERGY_BAR_X + ENERGY_BAR_WIDTH && mouseY >= y + ENERGY_BAR_Y && mouseY <= y + ENERGY_BAR_Y + ENERGY_BAR_HEIGHT)
            g.renderTooltip(font, Component.literal(String.format(Locale.ROOT, "%,d / %,d EU", menu.getEnergy(), menu.getMaxEnergy())), mouseX - x, mouseY - y);
        if (menu.isFormed()) {
            drawFitted(g, font, Component.translatable("gui.mio_icif.tesu.ports", menu.getInputPorts(), menu.getOutputPorts(), menu.getWirelessPorts()), TEXT_X, 56, TEXT_W, 0x404040, false);
            drawFitted(g, font, Component.translatable("gui.mio_icif.tesu.output_rate", compact(menu.getOutputRate())), TEXT_X, 68, TEXT_W, 0x404040, false);
        } else {
            drawFitted(g, font, Component.translatable("gui.mio_icif.tesu.no_structure"), TEXT_X, 56, TEXT_W, 0xFF5555, false);
            BlockPos wrong = menu.getWrongAt();
            int part = menu.getExpectedPart();
            if (wrong != null && part >= 0 && part < TesuStructure.Part.values().length) {
                String key = "gui.mio_icif.tesu.part." + TesuStructure.Part.values()[part].name().toLowerCase(Locale.ROOT);
                drawFitted(g, font, Component.translatable("gui.mio_icif.tesu.wrong_at", wrong.getX(), wrong.getY(), wrong.getZ(), Component.translatable(key)),
                    TEXT_X, 68, TEXT_W, 0xAA3333, false);
            }
        }
    }

    /** EU in full below ten million, else in k, M, G or T with three figures. */
    static String compact(long v) {
        if (v < 10_000_000L) return String.format(Locale.ROOT, "%,d", v);
        String[] units = {"k", "M", "G", "T", "P"};
        double d = v;
        int u = -1;
        while (d >= 1000 && u < units.length - 1) { d /= 1000; u++; }
        return String.format(Locale.ROOT, d >= 100 ? "%.0f%s" : d >= 10 ? "%.1f%s" : "%.2f%s", d, units[u]);
    }
}
