package com.miophas.singularity_iteration.common.client.screen.widget;

import com.miophas.singularity_iteration.common.client.screen.SiGuiTheme;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/** Button in the SI GUI theme: matte light key, steel-blue selected state, pressed shading. */
@OnlyIn(Dist.CLIENT)
public class SiButton extends Button {
    private boolean selected;

    public SiButton(int x, int y, int w, int h, Component message, OnPress onPress) {
        super(x, y, w, h, message, onPress, DEFAULT_NARRATION);
    }

    /** A selected button (e.g. the current tab) is drawn lit and does not react. */
    public SiButton selected(boolean selected) { this.selected = selected; return this; }
    public boolean isSelected() { return selected; }

    @Override
    protected void renderWidget(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        int x = getX(), y = getY(), w = width, h = height;
        boolean hot = active && isHoveredOrFocused() && !selected;
        int top = selected ? 0xFF4A7EB5 : hot ? 0xFFFAFBFC : active ? 0xFFF0F1F2 : 0xFFE2E3E4;
        int bottom = selected ? 0xFF3A6EA5 : hot ? 0xFFE0E6EE : active ? 0xFFD5D8DB : 0xFFDADCDE;
        g.fill(x, y, x + w, y + h, selected ? 0xFF2F5A88 : 0xFF8A9096);
        g.fillGradient(x + 1, y + 1, x + w - 1, y + h - 1, top, bottom);
        if (!selected && active) g.fill(x + 1, y + h - 2, x + w - 1, y + h - 1, hot ? SiGuiTheme.ACCENT : 0xFFB8BCC1);
        if (selected) g.fill(x + 1, y + 1, x + w - 1, y + 2, 0xFF2F5A88);
        int color = !active && !selected ? 0xFFA0A4A8 : selected ? 0xFFFFFFFF : 0xFF2A2E33;
        var font = Minecraft.getInstance().font;
        int tw = font.width(getMessage());
        g.drawString(font, getMessage(), x + (w - tw) / 2, y + (h - 8) / 2 + (h >= 11 ? 1 : 0), color, false);
    }

    /** Precision key click (original synthesized UI sound) instead of the vanilla button click. */
    @Override
    public void playDownSound(net.minecraft.client.sounds.SoundManager sounds) {
        sounds.play(net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(
            com.miophas.singularity_iteration.common.registry.mio_icif_sounds.UI_CLICK.get(), 1.0F, 0.55F));
    }

    @Override
    public void onPress() {
        if (!selected) super.onPress();
    }
}
