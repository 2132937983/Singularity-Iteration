// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.client.screen;

import com.miophas.singularity_iteration.common.menu.base.mio_icif_generator_menu;
import com.miophas.singularity_iteration.common.menu.generator.ModernSolarView;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

/**
 * Molecular-Transformer-style GUI for the METS solar series (advanced / photon resonance /
 * ultimate photon resonance): sun status -> flow arrow -> charge slot, segmented buffer gauge.
 * Generators only charge items; there is no discharge slot.
 */
public abstract class ModernSolarScreen<M extends mio_icif_generator_menu & ModernSolarView> extends mio_icif_screen<M> {
    private static final ResourceLocation TEXTURE = ResourceLocation.parse("mio_icif:textures/gui/gui_adv_solar.png");
    private static final int SUN_X = 57, SUN_Y = 36, SUN = 14, ARROW_X = 79, ARROW_Y = 35;
    private static final int SUN_U = 200, SUN_V = 227;
    private static final int BAR_X = 44, BAR_Y = 58;
    private static final int CHARGE_X = 115, SLOT_Y = 35;

    protected ModernSolarScreen(M menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.imageWidth = 176;
        this.imageHeight = 166;
    }

    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        int x = (this.width - this.imageWidth) / 2, y = (this.height - this.imageHeight) / 2;
        g.blit(TEXTURE, x, y, 0, 0, this.imageWidth, this.imageHeight);
        boolean on = menu.isGenerating();
        if (!on) g.setColor(0.45F, 0.47F, 0.5F, 0.55F);
        g.blit(ATLAS_TEXTURE, x + SUN_X, y + SUN_Y, 0, SUN_U, SUN_V, SUN, SUN, ATLAS_WIDTH, ATLAS_HEIGHT);
        g.setColor(1, 1, 1, 1);
        drawProgressArrow(g, x + ARROW_X, y + ARROW_Y, on ? ARROW_WIDTH : 0);   // sun -> buffer -> charge slot
        drawModernEnergyBar(g, x + BAR_X, y + BAR_Y, menu.getEnergy(), menu.getMaxEnergy());
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
        super.renderLabels(g, mouseX, mouseY);
        int x = (this.width - this.imageWidth) / 2, y = (this.height - this.imageHeight) / 2;
        small(g, Component.translatable("gui.mio_icif.solar.charge"), CHARGE_X + 8, SLOT_Y - 9);
        if (isHovering(mouseX, mouseY, x + SUN_X, y + SUN_Y, SUN, SUN)) {
            Component tip = menu.isGenerating()
                ? Component.translatable("gui.mio_icif.solar.generating", String.format("%,d", menu.getLiveOutput()))
                : Component.translatable("gui.mio_icif.solar.idle");
            g.renderTooltip(font, tip, mouseX - x, mouseY - y);
        } else if (isHovering(mouseX, mouseY, x + BAR_X, y + BAR_Y, KINETIC_ENERGY_BAR_WIDTH, KINETIC_ENERGY_BAR_HEIGHT)) {
            renderEnergyTooltip(g, mouseX - x, mouseY - y, menu.getEnergy(), menu.getMaxEnergy());
        } else if (hoveredSlot != null && !hoveredSlot.hasItem() && hoveredSlot.index == 0) {
            g.renderComponentTooltip(font, List.of(Component.translatable("gui.mio_icif.solar.charge.tip")), mouseX - x, mouseY - y);
        }
    }

    private void small(GuiGraphics g, Component text, int cx, int y) {
        g.pose().pushPose();
        g.pose().translate(cx, y, 0);
        g.pose().scale(0.6F, 0.6F, 1);
        g.drawString(font, text, -font.width(text) / 2, 0, 0xFF6B737D, false);
        g.pose().popPose();
    }
}
