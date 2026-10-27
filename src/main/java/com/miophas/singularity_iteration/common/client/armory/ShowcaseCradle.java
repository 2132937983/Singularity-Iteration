// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.client.armory;

import com.miophas.singularity_iteration.common.client.render.SiRenderTypes;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;

/**
 * 0.1.10.31 (A-3): the mechanical cradle of the physical display of the holo projector: a mast behind the figure, a yoke
 * with two clamp arms to the shoulders and a hip arm, in the projector's own side texture, lit by the world; two status lights
 * breathe on the mast. When the projector switches to the physical display the mast rises and then the arms reach in
 * ({@link #extension}).
 *
 * <p>Frame: blocks, the feet of the figure at the origin, y up, +z behind the figure (after the turn of the figure).
 */
final class ShowcaseCradle {
    private ShowcaseCradle() {}

    static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath("mio_icif", "textures/block/producer/holo_projector_side.png");
    /** Ticks for the cradle to unfold after the switch. */
    static final float UNFOLD_TICKS = 20F;
    /** The fixed boxes {x0, y0, z0, x1, y1, z1}: plinth, mast (its top follows the extension), yoke. */
    static final float[][] FRAME = {
        {-0.10F, -0.03F, 0.38F, 0.10F, 0.04F, 0.52F},
        {-0.04F, -0.03F, 0.47F, 0.04F, 1.60F, 0.53F},
        {-0.38F, 1.30F, 0.47F, 0.38F, 1.37F, 0.53F},
    };
    /** The arms {x0, y0, z0 (reached in), x1, y1, z1 (at the frame)}: two shoulder arms and the hip arm, with their pads. */
    static final float[][] ARMS = {
        {0.31F, 1.31F, 0.15F, 0.37F, 1.36F, 0.47F},
        {-0.37F, 1.31F, 0.15F, -0.31F, 1.36F, 0.47F},
        {-0.03F, 0.64F, 0.23F, 0.03F, 0.70F, 0.47F},
    };
    static final float[][] PADS = {
        {0.29F, 1.27F, 0.09F, 0.39F, 1.40F, 0.16F},
        {-0.39F, 1.27F, 0.09F, -0.29F, 1.40F, 0.16F},
        {-0.07F, 0.60F, 0.17F, 0.07F, 0.74F, 0.24F},
    };

    /** Mast rise (first half) and arm reach (second half), each 0..1, {@code ticks} after the switch. */
    static float[] extension(float ticks) {
        float e = Math.max(0F, Math.min(1F, ticks / UNFOLD_TICKS));
        return new float[]{Math.min(1F, e * 2F), Math.max(0F, e * 2F - 1F)};
    }

    /** Draws the cradle of a figure turned to {@code yaw}, in the pose at the feet of the figure. */
    static void draw(PoseStack pose, MultiBufferSource buffers, float yaw, int light, float ticks, float time) {
        float[] ext = extension(ticks);
        pose.pushPose();
        pose.mulPose(Axis.YP.rotationDegrees(180F - yaw));
        VertexConsumer vc = buffers.getBuffer(RenderType.entityCutoutNoCull(TEXTURE));
        PoseStack.Pose last = pose.last();
        box(vc, last, FRAME[0], light);
        float[] mast = FRAME[1].clone();
        mast[4] = mast[1] + (mast[4] - mast[1]) * ext[0];
        box(vc, last, mast, light);
        if (ext[0] >= 1F) box(vc, last, FRAME[2], light);
        if (ext[1] > 0F) {
            for (int i = 0; i < ARMS.length; i++) {
                float[] arm = ARMS[i].clone();
                arm[2] = arm[5] - (arm[5] - arm[2]) * ext[1];       // reach in from the frame
                box(vc, last, arm, light);
                float[] pad = PADS[i].clone();
                float shift = (ARMS[i][2] - arm[2]);
                pad[2] -= shift;
                pad[5] -= shift;
                box(vc, last, pad, light);
            }
        }
        // status lights on the back of the mast
        if (ext[0] >= 1F) {
            VertexConsumer glow = buffers.getBuffer(SiRenderTypes.FIELD_GLOW);
            float breath = 0.55F + 0.45F * (float) Math.sin(time * 0.15F);
            light(glow, last, 0.30F, breath);
            light(glow, last, 0.95F, 0.55F + 0.45F * (float) Math.sin(time * 0.15F + 1.7F));
        }
        pose.popPose();
    }

    private static void light(VertexConsumer vc, PoseStack.Pose last, float y, float a) {
        float s = 0.022F, z = 0.535F;
        vc.addVertex(last, -s, y - s, z).setColor(0.35F, 0.9F, 1F, a);
        vc.addVertex(last, s, y - s, z).setColor(0.35F, 0.9F, 1F, a);
        vc.addVertex(last, s, y + s, z).setColor(0.35F, 0.9F, 1F, a);
        vc.addVertex(last, -s, y + s, z).setColor(0.35F, 0.9F, 1F, a);
    }

    /** One textured box (each face takes the texture over its size, one block = the whole texture). */
    private static void box(VertexConsumer vc, PoseStack.Pose p, float[] b, int light) {
        float x0 = b[0], y0 = b[1], z0 = b[2], x1 = b[3], y1 = b[4], z1 = b[5];
        if (y1 <= y0 || x1 <= x0 || z1 <= z0) return;
        float w = Math.min(1F, x1 - x0), h = Math.min(1F, y1 - y0), d = Math.min(1F, z1 - z0);
        face(vc, p, light, 0, 0, -1, x1, y0, z0, x0, y0, z0, x0, y1, z0, x1, y1, z0, w, h);
        face(vc, p, light, 0, 0, 1, x0, y0, z1, x1, y0, z1, x1, y1, z1, x0, y1, z1, w, h);
        face(vc, p, light, -1, 0, 0, x0, y0, z0, x0, y0, z1, x0, y1, z1, x0, y1, z0, d, h);
        face(vc, p, light, 1, 0, 0, x1, y0, z1, x1, y0, z0, x1, y1, z0, x1, y1, z1, d, h);
        face(vc, p, light, 0, 1, 0, x0, y1, z1, x1, y1, z1, x1, y1, z0, x0, y1, z0, w, d);
        face(vc, p, light, 0, -1, 0, x0, y0, z0, x1, y0, z0, x1, y0, z1, x0, y0, z1, w, d);
    }

    private static void face(VertexConsumer vc, PoseStack.Pose p, int light, float nx, float ny, float nz,
                             float ax, float ay, float az, float bx, float by, float bz, float cx, float cy, float cz, float dx, float dy, float dz,
                             float u, float v) {
        vertex(vc, p, light, nx, ny, nz, ax, ay, az, 0, v);
        vertex(vc, p, light, nx, ny, nz, bx, by, bz, u, v);
        vertex(vc, p, light, nx, ny, nz, cx, cy, cz, u, 0);
        vertex(vc, p, light, nx, ny, nz, dx, dy, dz, 0, 0);
    }

    private static void vertex(VertexConsumer vc, PoseStack.Pose p, int light, float nx, float ny, float nz, float x, float y, float z,
                               float u, float v) {
        vc.addVertex(p, x, y, z).setColor(0xFFFFFFFF).setUv(u, v).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(p, nx, ny, nz);
    }
}
