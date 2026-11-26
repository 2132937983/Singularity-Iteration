// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.client.integration.emi;

import dev.emi.emi.api.widget.Bounds;
import dev.emi.emi.api.widget.Widget;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

/**
 * 0.1.11.209: wrapped lines in a fixed box (EMI has no scroll box, the JEI pages use one). What does not fit ends in
 * "…", and the whole text is then the tooltip of the box. The lines are built when the box is first drawn, so a page
 * that is never opened computes nothing (the reactor layouts run their model only then).
 */
final class TextBlockWidget extends Widget {
    private static final int LINE = 10, COLOR = 0xFF404040, TOOLTIP_WIDTH = 220;

    private final Supplier<List<Component>> source;
    private final Bounds bounds;
    private List<Component> lines;
    private List<FormattedCharSequence> wrapped;

    TextBlockWidget(int x, int y, int width, int height, Supplier<List<Component>> source) {
        this.bounds = new Bounds(x, y, width, height);
        this.source = source;
    }

    private void build(Font font) {
        if (wrapped != null) return;
        lines = source.get();
        wrapped = new ArrayList<>();
        for (Component line : lines) wrapped.addAll(font.split(line, bounds.width() - 4));
    }

    private boolean overflows() {
        return wrapped != null && wrapped.size() * LINE > bounds.height();
    }

    @Override
    public Bounds getBounds() {
        return bounds;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        Font font = Minecraft.getInstance().font;
        build(font);
        int fit = Math.max(1, bounds.height() / LINE);
        boolean cut = wrapped.size() > fit;
        int shown = cut ? fit - 1 : wrapped.size();
        int y = bounds.y();
        for (int i = 0; i < shown; i++, y += LINE) graphics.drawString(font, wrapped.get(i), bounds.x() + 2, y, COLOR, false);
        if (cut) graphics.drawString(font, Component.literal("…").withStyle(ChatFormatting.GRAY), bounds.x() + 2, y, COLOR, false);
    }

    @Override
    public List<ClientTooltipComponent> getTooltip(int mouseX, int mouseY) {
        if (!overflows()) return List.of();
        Font font = Minecraft.getInstance().font;
        List<ClientTooltipComponent> out = new ArrayList<>();
        for (Component line : lines)
            for (FormattedCharSequence part : font.split(line, TOOLTIP_WIDTH)) out.add(ClientTooltipComponent.create(part));
        return out;
    }
}
