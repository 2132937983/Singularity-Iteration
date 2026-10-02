package com.miophas.singularity_iteration.common.client.screen;

import com.miophas.singularity_iteration.common.menu.generator.GeomagneticGeneratorMenu;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/** Geomagnetic generator, Molecular-Transformer style: charge slot, field status, live output, buffer gauge. */
@OnlyIn(Dist.CLIENT)
public class mio_icif_gui_geomagnetic_generator extends mio_icif_screen<GeomagneticGeneratorMenu> {
    private static final ResourceLocation GUI_TEXTURE = ResourceLocation.parse("mio_icif:textures/gui/gui_adv_geomagnetic.png");
    private static final int STATUS_X = 57, STATUS_Y = 36, ICON = 14, ARROW_X = 79, ARROW_Y = 35, CHARGE_X = 115;
    private static final int ACTIVE_U = 88, INACTIVE_U = 104, ICON_V = 227;
    private static final int BAR_X = 44, BAR_Y = 58;

    public mio_icif_gui_geomagnetic_generator(GeomagneticGeneratorMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 166;
    }

    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        int x = (this.width - this.imageWidth) / 2, y = (this.height - this.imageHeight) / 2;
        g.blit(GUI_TEXTURE, x, y, 0, 0, this.imageWidth, this.imageHeight);
        boolean on = menu.isGenerating();
        if (!on) g.setColor(0.45F, 0.47F, 0.5F, 0.6F);
        g.blit(ATLAS_TEXTURE, x + STATUS_X, y + STATUS_Y, 0, ACTIVE_U, ICON_V, ICON, ICON, ATLAS_WIDTH, ATLAS_HEIGHT);
        g.setColor(1, 1, 1, 1);
        drawProgressArrow(g, x + ARROW_X, y + ARROW_Y, on ? ARROW_WIDTH : 0);   // field -> buffer -> charge slot
        drawModernEnergyBar(g, x + BAR_X, y + BAR_Y, menu.getEnergy(), menu.getMaxEnergy());
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
        super.renderLabels(g, mouseX, mouseY);
        int x = (this.width - this.imageWidth) / 2, y = (this.height - this.imageHeight) / 2;
        boolean on = menu.isGenerating();
        Component status = Component.translatable(on ? "gui.mio_icif.geomagnetic.active" : "gui.mio_icif.geomagnetic.inactive");
        String line = status.getString() + "  \u00b7  " + String.format("%,d EU/t", on ? menu.getLiveOutput() : 0);
        g.drawString(font, line, (this.imageWidth - font.width(line)) / 2, 18, on ? 0xFF2E7D4F : 0xFF9A4040, false);
        g.pose().pushPose();
        g.pose().translate(CHARGE_X + 8, 26, 0);
        g.pose().scale(0.6F, 0.6F, 1);
        Component charge = Component.translatable("gui.mio_icif.solar.charge");
        g.drawString(font, charge, -font.width(charge) / 2, 0, 0xFF6B737D, false);
        g.pose().popPose();
        if (isHovering(mouseX, mouseY, x + BAR_X, y + BAR_Y, KINETIC_ENERGY_BAR_WIDTH, KINETIC_ENERGY_BAR_HEIGHT)) {
            renderEnergyTooltip(g, mouseX - x, mouseY - y, menu.getEnergy(), menu.getMaxEnergy());
        } else if (isHovering(mouseX, mouseY, x + STATUS_X, y + STATUS_Y, ICON, ICON)) {
            g.renderComponentTooltip(font, List.of(Component.translatable(on ? "gui.mio_icif.geomagnetic.active.tip"
                : "gui.mio_icif.geomagnetic.inactive.tip")), mouseX - x, mouseY - y);
        } else if (hoveredSlot != null && !hoveredSlot.hasItem() && hoveredSlot.index == 0) {
            g.renderComponentTooltip(font, List.of(Component.translatable("gui.mio_icif.solar.charge.tip")), mouseX - x, mouseY - y);
        }
    }
}
