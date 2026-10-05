// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.client.suit;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

/**
 * HUD layout editor: drag a panel to move it, scroll on the holomap to resize it. Positions are
 * stored as fractions of the screen in the client config.
 */
public class HudLayoutScreen extends Screen {
    @Nullable private final Screen parent;
    @Nullable private FcsClientConfig.Panel dragging;
    private double grabDx, grabDy;

    public HudLayoutScreen(@Nullable Screen parent) {
        super(Component.translatable("screen.mio_icif.hud_layout"));
        this.parent = parent;
    }

    /** Approximate panel size in GUI px (same as the HUD draws). */
    static int[] size(FcsClientConfig.Panel panel) {
        return switch (panel) {
            case STATUS -> new int[]{104, 54};
            case TELEMETRY -> new int[]{112, 74};
            case HOLOMAP -> {
                int s = (int) (112 * FcsClientConfig.holomapScale());
                yield new int[]{s, (int) (s * 0.92F)};
            }
            case BALLISTIC -> new int[]{92, 40};
            case BLAST -> new int[]{128, 30};
        };
    }

    @Override
    protected void init() {
        int y = height - 28;
        addRenderableWidget(Button.builder(Component.translatable("screen.mio_icif.hud_layout.reset"), b -> {
            for (FcsClientConfig.Panel p : FcsClientConfig.Panel.values()) p.reset();
            if (FcsClientConfig.loaded()) FcsClientConfig.HOLOMAP_SCALE.set(1.0);
            FcsClientConfig.save();
        }).bounds(width / 2 - 104, y, 100, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.done"), b -> onClose()).bounds(width / 2 + 4, y, 100, 20).build());
        addRenderableWidget(Button.builder(compassLabel(), b -> {
            FcsClientConfig.setShowCompass(!FcsClientConfig.showCompass());
            b.setMessage(compassLabel());
        }).bounds(width / 2 - 104, y - 24, 208, 20).build());
    }

    private static Component compassLabel() {
        return Component.translatable("screen.mio_icif.hud_layout.compass",
            Component.translatable(FcsClientConfig.showCompass() ? "options.on" : "options.off"));
    }

    private int[] rect(FcsClientConfig.Panel p) {
        int[] s = size(p);
        int cx = (int) (p.x() * width), cy = (int) (p.y() * height);
        return new int[]{cx - s[0] / 2, cy - s[1] / 2, s[0], s[1]};
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partial) {
        g.fill(0, 0, width, height, 0x60000000);
        int primary = FcsClientConfig.primary();
        // grid
        for (int x = 0; x < width; x += 20) g.fill(x, 0, x + 1, height, 0x14FFFFFF);
        for (int y = 0; y < height; y += 20) g.fill(0, y, width, y + 1, 0x14FFFFFF);
        g.fill(width / 2, 0, width / 2 + 1, height, 0x30FFFFFF);
        g.fill(0, height / 2, width, height / 2 + 1, 0x30FFFFFF);
        for (FcsClientConfig.Panel p : FcsClientConfig.Panel.values()) {
            int[] r = rect(p);
            boolean hot = p == dragging || (mouseX >= r[0] && mouseX < r[0] + r[2] && mouseY >= r[1] && mouseY < r[1] + r[3]);
            FcsDraw.panel(g, r[0], r[1], r[2], r[3], Component.translatable("screen.mio_icif.hud_layout." + p.id).getString(),
                hot ? FcsDraw.YELLOW : primary);
            FcsDraw.textCentered(g, String.format(java.util.Locale.ROOT, "%.0f%% / %.0f%%", p.x() * 100, p.y() * 100),
                r[0] + r[2] / 2F, r[1] + r[3] / 2F, FcsDraw.argb(FcsDraw.WHITE, 0.8F), 0.6F);
        }
        g.drawCenteredString(font, title, width / 2, 10, 0xFFFFFF);
        g.drawCenteredString(font, Component.translatable("screen.mio_icif.hud_layout.hint"), width / 2, height - 66, 0xA0C0C8);
        super.render(g, mouseX, mouseY, partial);
    }

    @Override
    public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partial) { }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (super.mouseClicked(mx, my, button)) return true;
        FcsClientConfig.Panel[] panels = FcsClientConfig.Panel.values();
        for (int i = panels.length - 1; i >= 0; i--) {
            int[] r = rect(panels[i]);
            if (mx >= r[0] && mx < r[0] + r[2] && my >= r[1] && my < r[1] + r[3]) {
                dragging = panels[i];
                grabDx = mx - panels[i].x() * width;
                grabDy = my - panels[i].y() * height;
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean mouseDragged(double mx, double my, int button, double dx, double dy) {
        if (dragging != null) {
            int[] s = size(dragging);
            double cx = Math.max(s[0] / 2.0, Math.min(width - s[0] / 2.0, mx - grabDx));
            double cy = Math.max(s[1] / 2.0, Math.min(height - s[1] / 2.0, my - grabDy));
            // snap to the centre lines
            if (Math.abs(cx - width / 2.0) < 4) cx = width / 2.0;
            if (Math.abs(cy - height / 2.0) < 4) cy = height / 2.0;
            dragging.set(cx / width, cy / height);
            return true;
        }
        return super.mouseDragged(mx, my, button, dx, dy);
    }

    @Override
    public boolean mouseReleased(double mx, double my, int button) {
        if (dragging != null) {
            dragging = null;
            FcsClientConfig.save();
            return true;
        }
        return super.mouseReleased(mx, my, button);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double sx, double sy) {
        int[] r = rect(FcsClientConfig.Panel.HOLOMAP);
        if (FcsClientConfig.loaded() && mx >= r[0] && mx < r[0] + r[2] && my >= r[1] && my < r[1] + r[3]) {
            double next = Math.max(0.6, Math.min(1.8, FcsClientConfig.HOLOMAP_SCALE.get() + sy * 0.1));
            FcsClientConfig.HOLOMAP_SCALE.set(next);
            FcsClientConfig.save();
            return true;
        }
        return super.mouseScrolled(mx, my, sx, sy);
    }

    @Override
    public void onClose() {
        FcsClientConfig.save();
        minecraft.setScreen(parent);
    }

    @Override
    public boolean isPauseScreen() { return false; }
}
