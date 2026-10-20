package com.miophas.singularity_iteration.common.client.lathe;

import com.miophas.singularity_iteration.common.item.lathe.LatheShapes;
import com.miophas.singularity_iteration.core.api.item.ILatheItem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/**
 * Draws a lathe workpiece by its current profile, as IC2 GuiLathe.renderILatheItemIntoGUI and
 * LatheRenderer do: the profile sheet (256 x 256, the profile in its top 120 x 32 px) is cut into
 * 5 segments of 24 px, each as wide as its position is thick and centred in the 32 px band.
 * <ul>
 *   <li>{@link #renderGui}: into a GUI at 1 px per sheet pixel (lathe GUI, tooltip).</li>
 *   <li>{@link #renderByItem}: the item model of the turning blanks, the profile mapped onto
 *       15 x 10 px of the item and 1 px thick: outlined in the GUI, with edges elsewhere.</li>
 * </ul>
 */
@OnlyIn(Dist.CLIENT)
@SuppressWarnings("null")
public final class LatheProfileRenderer extends BlockEntityWithoutLevelRenderer {

    public static final int POSITIONS = 5;
    /** IC2 GuiLathe: segLength, renderWidth; the profile is 5 x 24 = 120 px long. */
    public static final int SEGMENT_LENGTH = 24;
    public static final int RENDER_WIDTH = 32;
    private static final float SHEET = 256F;
    private static final float TEXEL = 1F / SHEET;

    // item model: x from 0.5 to 15.5 px (3 px per position), band from 3 to 13 px, z from 7.5 to 8.5 px
    private static final float X0 = 0.5F / 16F;
    private static final float SEG = 3F / 16F;
    private static final float Y_TOP = 13F / 16F;
    private static final float BAND = 10F / 16F;
    private static final float Z_FRONT = 8.5F / 16F;
    private static final float Z_BACK = 7.5F / 16F;
    // GUI icon outline: half a pixel wide, the profile texture at 30 % brightness
    private static final float OUTLINE = 0.5F / 16F;
    private static final float Z_OUTLINE = Z_FRONT - 0.25F / 16F;
    private static final int OUTLINE_COLOR = 0xFF4D4D4D;
    /** A finished part (a named profile of {@link LatheShapes}) is outlined in teal. */
    private static final int NAMED_OUTLINE_COLOR = 0xFF00C8D8;

    public LatheProfileRenderer() {
        super(Minecraft.getInstance().getBlockEntityRenderDispatcher(), Minecraft.getInstance().getEntityModels());
    }

    /** IC2: the thickness of a position in sheet pixels (of the 32 px band). */
    public static int segmentWidth(int state, int width) {
        return (int) ((float) RENDER_WIDTH / width * state + 0.5F);
    }

    /** IC2: the distance of a segment from the top of the band, in sheet pixels. */
    public static int segmentOffset(int segWidth) {
        return (int) ((RENDER_WIDTH - segWidth) / 2.0F + 0.5F);
    }

    /** IC2 renderILatheItemIntoGUI: the profile at (x, y), 120 x 32 px. */
    public static void renderGui(GuiGraphics guiGraphics, ItemStack stack, int x, int y) {
        if (!(stack.getItem() instanceof ILatheItem lathe)) return;
        ResourceLocation texture = lathe.getTexture(stack);
        int[] state = lathe.getCurrentState(stack);
        int width = lathe.getWidth(stack);
        if (texture == null || width <= 0 || state.length < POSITIONS) return;

        for (int j = 0; j < POSITIONS; j++) {
            int segWidth = segmentWidth(state[j], width);
            int offset = segmentOffset(segWidth);
            guiGraphics.blit(texture, x + SEGMENT_LENGTH * j, y + offset,
                SEGMENT_LENGTH * j, offset, SEGMENT_LENGTH, segWidth, 256, 256);
        }
    }

    @Override
    public void renderByItem(ItemStack stack, ItemDisplayContext context, PoseStack poseStack,
                             MultiBufferSource buffer, int packedLight, int packedOverlay) {
        if (!(stack.getItem() instanceof ILatheItem lathe)) return;
        ResourceLocation texture = lathe.getTexture(stack);
        int[] state = lathe.getCurrentState(stack);
        int width = lathe.getWidth(stack);
        if (texture == null || width <= 0 || state.length < POSITIONS) return;

        VertexConsumer vc = buffer.getBuffer(RenderType.entityCutoutNoCull(texture));
        PoseStack.Pose pose = poseStack.last();
        boolean flat = context == ItemDisplayContext.GUI;
        Face f = new Face(vc, pose, packedLight, packedOverlay, -1);
        Face outline = new Face(vc, pose, packedLight, packedOverlay,
            LatheShapes.of(stack).isPresent() ? NAMED_OUTLINE_COLOR : OUTLINE_COLOR);

        float[] top = new float[POSITIONS];
        float[] bottom = new float[POSITIONS];
        int[] offsets = new int[POSITIONS];
        for (int j = 0; j < POSITIONS; j++) {
            int segWidth = segmentWidth(state[j], width);
            int offset = segmentOffset(segWidth);
            offsets[j] = offset;
            top[j] = Y_TOP - BAND * offset / RENDER_WIDTH;
            bottom[j] = top[j] - BAND * segWidth / RENDER_WIDTH;

            float x0 = X0 + SEG * j, x1 = x0 + SEG;
            float u0 = SEGMENT_LENGTH * j / SHEET, u1 = (SEGMENT_LENGTH * j + SEGMENT_LENGTH) / SHEET;
            float v0 = offset / SHEET, v1 = (offset + segWidth) / SHEET;

            f.xy(x0, bottom[j], x1, top[j], Z_FRONT, u0, v1, u1, v0, 1);
            if (flat) {
                // a darkened copy half a pixel larger behind the profile outlines it on the slot
                outline.xy(x0 - OUTLINE, bottom[j] - OUTLINE, x1 + OUTLINE, top[j] + OUTLINE, Z_OUTLINE, u0, v1, u1, v0, 1);
                continue;
            }
            f.xy(x0, bottom[j], x1, top[j], Z_BACK, u0, v1, u1, v0, -1);
            f.xz(x0, x1, top[j], u0, v0, u1, v0 + TEXEL, 1);
            f.xz(x0, x1, bottom[j], u0, v1 - TEXEL, u1, v1, -1);
        }
        if (flat) return;

        // the two ends of the workpiece
        f.yz(X0, bottom[0], top[0], 0, offsets[0], top[0], -1);
        float xEnd = X0 + SEG * POSITIONS;
        f.yz(xEnd, bottom[4], top[4], SEGMENT_LENGTH * POSITIONS - 1, offsets[4], top[4], 1);
        // the steps between neighbouring positions, faced by the thicker one
        for (int j = 0; j < POSITIONS - 1; j++) {
            float x = X0 + SEG * (j + 1);
            int k = j + 1;
            if (top[j] > top[k]) {
                f.yz(x, top[k], top[j], SEGMENT_LENGTH * k - 1, offsets[j], top[j], 1);
                f.yz(x, bottom[j], bottom[k], SEGMENT_LENGTH * k - 1, offsets[j], top[j], 1);
            } else if (top[k] > top[j]) {
                f.yz(x, top[j], top[k], SEGMENT_LENGTH * k, offsets[k], top[k], -1);
                f.yz(x, bottom[k], bottom[j], SEGMENT_LENGTH * k, offsets[k], top[k], -1);
            }
        }
    }

    /** Quads of the item model with their normals, tinted by {@code color} (ARGB). */
    private record Face(VertexConsumer vc, PoseStack.Pose pose, int light, int overlay, int color) {
        /** Front (dir 1) or back (dir -1) face at depth z. */
        void xy(float x0, float y0, float x1, float y1, float z, float u0, float vBottom, float u1, float vTop, int dir) {
            vertex(x0, y0, z, u0, vBottom, 0, 0, dir);
            vertex(x1, y0, z, u1, vBottom, 0, 0, dir);
            vertex(x1, y1, z, u1, vTop, 0, 0, dir);
            vertex(x0, y1, z, u0, vTop, 0, 0, dir);
        }

        /** Top (dir 1) or bottom (dir -1) edge at height y, textured with one texel row. */
        void xz(float x0, float x1, float y, float u0, float v0, float u1, float v1, int dir) {
            vertex(x0, y, Z_BACK, u0, v0, 0, dir, 0);
            vertex(x1, y, Z_BACK, u1, v0, 0, dir, 0);
            vertex(x1, y, Z_FRONT, u1, v1, 0, dir, 0);
            vertex(x0, y, Z_FRONT, u0, v1, 0, dir, 0);
        }

        /**
         * Side face (normal +x for dir 1, -x for dir -1) at x from y0 to y1, textured with the
         * sheet column {@code column} of the segment whose band starts {@code offset} px down at
         * {@code segTop}.
         */
        void yz(float x, float y0, float y1, int column, int offset, float segTop, int dir) {
            float u0 = column / SHEET, u1 = u0 + TEXEL;
            float vTop = (offset + (segTop - y1) / BAND * RENDER_WIDTH) / SHEET;
            float vBottom = (offset + (segTop - y0) / BAND * RENDER_WIDTH) / SHEET;
            vertex(x, y0, Z_BACK, u0, vBottom, dir, 0, 0);
            vertex(x, y0, Z_FRONT, u1, vBottom, dir, 0, 0);
            vertex(x, y1, Z_FRONT, u1, vTop, dir, 0, 0);
            vertex(x, y1, Z_BACK, u0, vTop, dir, 0, 0);
        }

        private void vertex(float x, float y, float z, float u, float v, float nx, float ny, float nz) {
            vc.addVertex(pose, x, y, z).setColor(color).setUv(u, v).setOverlay(overlay).setLight(light).setNormal(pose, nx, ny, nz);
        }
    }
}
