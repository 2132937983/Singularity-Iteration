package com.miophas.singularity_iteration.common.client.screen.widget;

import com.miophas.singularity_iteration.common.client.screen.SiGuiTheme;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/**
 * Button in the SI GUI theme: matte light key, steel-blue selected state, pressed shading. 0.1.11.183: the vanilla-like
 * raised key of the processing machines (white upper left, dark lower right, grey face, lighter on hover); a selected
 * button is pressed in (sunk, dark, white text).
 */
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
        if (selected) {   // pressed in
            com.miophas.singularity_iteration.common.client.screen.VanillaFrame.inset(g, x, y, w, h, 0xFF6B6B6B);
        } else {
            g.fill(x, y, x + w, y + h, com.miophas.singularity_iteration.common.client.screen.VanillaFrame.SHADOW);
            g.fill(x, y, x + w - 1, y + h - 1, active ? com.miophas.singularity_iteration.common.client.screen.VanillaFrame.LIGHT : 0xFFD8D8D8);
            g.fill(x + 1, y + 1, x + w - 1, y + h - 1, hot ? 0xFFD8D8E8 : active ? 0xFFB8B8B8 : 0xFFA8A8A8);
        }
        int color = !active && !selected ? 0xFF6E6E6E : selected ? 0xFFFFFFFF : 0xFF404040;
        var font = Minecraft.getInstance().font;
        int tw = font.width(getMessage());
        int ty = y + (h - 8) / 2 + (h >= 11 ? 1 : 0);
        if (tw <= w - 4) g.drawString(font, getMessage(), x + (w - tw) / 2, ty, color, false);
        else {
            // longer label (translation): shrink, then ellipsis - never past the button edge
            int avail = w - 4;
            int drawn = Math.min(avail, (int) Math.ceil(tw * Math.max(0.7F, (float) avail / tw)));
            com.miophas.singularity_iteration.common.client.screen.mio_icif_screen.drawFitted(g, font, getMessage(), x + (w - drawn) / 2, ty, avail, color, false);
        }
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
