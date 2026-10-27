// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.client.screen;

import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.systems.RenderSystem;
import java.util.function.UnaryOperator;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaternionf;

/**
 * 0.1.10.32 (G-1, G-2): a 3D preview panel beside a console window: a dark gradient, a floor grid in perspective, the figure
 * turned by dragging and zoomed with the wheel ({@link View}), and a scan line that runs down the figure when its colours change
 * (the caller draws the old look, then the new one under {@link #scanY}). The panel sits to the right of the window, or to
 * its left when the right side has no room; on a narrow screen it is not shown ({@link #place}).
 */
public final class SuitPreview {
    private SuitPreview() {}

    public static final int W = 104;
    public static final long SCAN_MS = 650;

    /** The state of one preview: turn (degrees), zoom, the drag in progress, the last scan. */
    public static final class View {
        public float yaw = 205F, zoom = 1F;
        boolean dragging;
        long scanAt = -1;

        /** Starts the scan line (the colours changed). */
        public void scan() {
            scanAt = System.currentTimeMillis();
        }

        /** True while the scan line runs. */
        public boolean scanning() {
            return scanAt >= 0 && System.currentTimeMillis() - scanAt < SCAN_MS;
        }

        public boolean click(double mx, double my, int[] box) {
            if (box == null || !in(mx, my, box)) return false;
            dragging = true;
            return true;
        }

        public boolean drag(double dx) {
            if (!dragging) return false;
            yaw = (float) ((yaw + dx * 1.6) % 360);
            return true;
        }

        public void release() {
            dragging = false;
        }

        public boolean scroll(double mx, double my, double amount, int[] box) {
            if (box == null || !in(mx, my, box)) return false;
            zoom = Math.max(0.6F, Math.min(1.8F, zoom + (float) amount * 0.1F));
            return true;
        }
    }

    static boolean in(double mx, double my, int[] box) {
        return mx >= box[0] && mx < box[0] + box[2] && my >= box[1] && my < box[1] + box[3];
    }

    /**
     * The panel box {x, y, w, h} beside a window at {@code left, top} of {@code width} x {@code height} on a screen
     * {@code screenW} wide, or null when neither side has room.
     */
    @Nullable
    public static int[] place(int left, int top, int width, int height, int screenW) {
        if (left + width + 4 + W <= screenW - 2) return new int[]{left + width + 4, top, W, height};
        if (left - 4 - W >= 2) return new int[]{left - 4 - W, top, W, height};
        return null;
    }

    /** Height (pixels from the top of the figure box) of the scan line, 0..1 of the box. */
    public static float scanY(View view) {
        if (view.scanAt < 0) return 1F;
        return Math.min(1F, (System.currentTimeMillis() - view.scanAt) / (float) SCAN_MS);
    }

    /** Background: frame, gradient, floor grid and the title. */
    public static void background(GuiGraphics g, Font font, int[] box, String title, String hint) {
        int x = box[0], y = box[1], w = box[2], h = box[3];
        g.fill(x - 1, y - 1, x + w + 1, y + h + 1, 0xFF2A4A60);
        g.fillGradient(x, y, x + w, y + h, 0xFF0C1822, 0xFF02070B);
        // floor grid: lines that close in towards the horizon, and rays from the vanishing point
        int horizon = y + h - 46, floor = y + h - 14, cx = x + w / 2;
        for (int k = 0; k <= 5; k++) {
            float t = k / 5F;
            int gy = Math.round(horizon + (floor - horizon) * t * t);
            int half = Math.round(w * (0.18F + 0.32F * t * t));
            g.fill(cx - half, gy, cx + half, gy + 1, (k == 5 ? 0x7037C8FF : 0x3037C8FF));
        }
        for (int k = -4; k <= 4; k++) {
            for (int s = 0; s <= 12; s++) {
                float t = s / 12F;
                int gy = Math.round(horizon + (floor - horizon) * t * t);
                int gx = Math.round(cx + k * w * (0.04F + 0.08F * t * t));
                g.fill(gx, gy, gx + 1, gy + 1, 0x3037C8FF);
            }
        }
        g.drawString(font, title, x + 4, y + 4, 0xFF8FEFFF, false);
        g.pose().pushPose();
        g.pose().translate(x + 4, y + h - 9, 0);
        g.pose().scale(0.65F, 0.65F, 1);
        g.drawString(font, hint, 0, 0, 0xFF5E7C8C, false);
        g.pose().popPose();
    }

    /**
     * Draws {@code entity} standing on the grid of the panel, turned by the view; {@code wrap} may replace the buffers (a
     * hologram look). The entity is shown facing front, without head turn and pitch (restored afterwards).
     */
    public static void figure(GuiGraphics g, int[] box, LivingEntity entity, View view, @Nullable UnaryOperator<MultiBufferSource> wrap) {
        int x = box[0], y = box[1], w = box[2], h = box[3];
        float scale = (h - 40) / 2.1F * view.zoom;
        float bodyRot = entity.yBodyRot, bodyRotO = entity.yBodyRotO, yRot = entity.getYRot(), yRotO = entity.yRotO;
        float xRot = entity.getXRot(), xRotO = entity.xRotO, head = entity.yHeadRot, headO = entity.yHeadRotO;
        entity.yBodyRot = entity.yBodyRotO = 180F;
        entity.setYRot(180F);
        entity.yRotO = 180F;
        entity.setXRot(0F);
        entity.xRotO = 0F;
        entity.yHeadRot = entity.yHeadRotO = 180F;
        g.enableScissor(x, y, x + w, y + h);
        g.pose().pushPose();
        g.pose().translate(x + w / 2F, y + h - 16, 150);
        g.pose().scale(scale, scale, -scale);
        g.pose().mulPose(new Quaternionf().rotateZ((float) Math.PI).rotateX((float) Math.toRadians(-10)).rotateY((float) Math.toRadians(view.yaw)));
        Lighting.setupForEntityInInventory();
        EntityRenderDispatcher dispatcher = Minecraft.getInstance().getEntityRenderDispatcher();
        dispatcher.setRenderShadow(false);
        MultiBufferSource buffers = wrap == null ? g.bufferSource() : wrap.apply(g.bufferSource());
        RenderSystem.runAsFancy(() -> dispatcher.render(entity, 0, 0, 0, 0F, 1F, g.pose(), buffers, LightTexture.FULL_BRIGHT));
        g.flush();
        dispatcher.setRenderShadow(true);
        g.pose().popPose();
        Lighting.setupFor3DItems();
        g.disableScissor();
        entity.yBodyRot = bodyRot;
        entity.yBodyRotO = bodyRotO;
        entity.setYRot(yRot);
        entity.yRotO = yRotO;
        entity.setXRot(xRot);
        entity.xRotO = xRotO;
        entity.yHeadRot = head;
        entity.yHeadRotO = headO;
    }

    /** The scan line at {@code t} (0..1 of the figure box) and its glow. */
    public static void scanLine(GuiGraphics g, int[] box, float t) {
        int y = box[1] + 14 + Math.round((box[3] - 30) * t);
        g.fill(box[0] + 2, y - 2, box[0] + box[2] - 2, y, 0x3037C8FF);
        g.fill(box[0] + 2, y, box[0] + box[2] - 2, y + 1, 0xFF8FEFFF);
        g.fill(box[0] + 2, y + 1, box[0] + box[2] - 2, y + 3, 0x3037C8FF);
    }

    /** Scissor box of the part of the figure the scan has passed (the new look). */
    public static int[] scanned(int[] box, float t) {
        int y = box[1] + 14 + Math.round((box[3] - 30) * t);
        return new int[]{box[0], box[1], box[2], Math.max(0, y - box[1])};
    }
}
