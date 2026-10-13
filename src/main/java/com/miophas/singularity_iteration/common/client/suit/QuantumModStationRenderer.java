// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.client.suit;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.miophas.singularity_iteration.common.client.render.SiRenderTypes;
import com.miophas.singularity_iteration.common.suit.QuantumModStationBlockEntity;
import com.miophas.singularity_iteration.common.suit.SuitModules;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import org.joml.Matrix4f;

/**
 * Universal Suit Modification Station (0.1.8.5): the suit piece hovers over the pad inside a
 * lattice ring. The ring is cyan for a nano piece (nano lattice), violet for a quantum piece
 * (quantum field). While a unit installs, the emitter head projects a beam fan onto a scan
 * plane, and a nano piece is woven by rising lattice rings.
 */
public class QuantumModStationRenderer implements BlockEntityRenderer<QuantumModStationBlockEntity> {
    public QuantumModStationRenderer(BlockEntityRendererProvider.Context context) { }

    private static final float[] NANO = {0.35F, 0.9F, 1.0F}, QUANTUM = {0.72F, 0.48F, 1.0F};

    @Override
    public void render(QuantumModStationBlockEntity station, float partial, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        ItemStack suit = station.suit();
        if (suit.isEmpty() || station.getLevel() == null) return;
        float time = station.getLevel().getGameTime() + partial;
        boolean nano = SuitModules.isNanoPiece(suit);
        float[] c = nano ? NANO : QUANTUM;
        pose.pushPose();
        pose.translate(0.5, 0.86 + Math.sin(time / 18.0) * 0.03, 0.5);
        pose.mulPose(Axis.YP.rotationDegrees(time * 1.5F));
        pose.scale(0.56F, 0.56F, 0.56F);
        Minecraft.getInstance().getItemRenderer().renderStatic(suit, ItemDisplayContext.FIXED, LightTexture.FULL_BRIGHT,
            OverlayTexture.NO_OVERLAY, pose, buffers, station.getLevel(), 0);
        pose.popPose();

        VertexConsumer vc = buffers.getBuffer(SiRenderTypes.FIELD_GLOW);
        Matrix4f m = pose.last().pose();
        boolean working = station.isClientWorking();
        // lattice ring on the pad: segmented, slowly turning; brighter while working
        float spin = time * (working ? 0.06F : 0.015F);
        ring(vc, m, 0.5F, 0.445F, 0.5F, 0.30F, 0.33F, 12, 0.62F, spin, c, working ? 0.55F : 0.22F);
        ring(vc, m, 0.5F, 0.447F, 0.5F, 0.20F, 0.215F, 24, 0.5F, -spin * 1.7F, c, working ? 0.4F : 0.12F);
        if (!working) return;

        // emitter head: in the model space the head is at the back (south, z 11/16) and looks at the pad
        Direction facing = station.getBlockState().hasProperty(BlockStateProperties.HORIZONTAL_FACING)
            ? station.getBlockState().getValue(BlockStateProperties.HORIZONTAL_FACING) : Direction.NORTH;
        pose.pushPose();
        pose.translate(0.5, 0, 0.5);
        pose.mulPose(Axis.YP.rotationDegrees(-facing.toYRot() + 180F));
        pose.translate(-0.5, 0, -0.5);
        Matrix4f fm = pose.last().pose();
        float h = 0.5F + 0.55F * (0.5F + 0.5F * (float) Math.sin(time / 6.0));
        // scan plane
        plane(vc, fm, 0.16F, h, 0.16F, 0.84F, 0.84F, c, 0.35F);
        // beam fan from the lens (y 14/16, z 11/16) to the plane corners
        float ly = 0.875F, lz = 0.6875F;
        for (int i = 0; i < 5; i++) {
            float lx = 0.3F + i * 0.1F;
            beam(vc, fm, lx, ly, lz, 0.16F + i * 0.17F, h, 0.2F, c, 0.32F);
        }
        if (nano) {
            // nano weave: three rising rings around the piece
            for (int k = 0; k < 3; k++) {
                float t = ((time / 30F) + k / 3F) % 1F;
                float y = 0.47F + t * 0.75F;
                float r = 0.36F - t * 0.08F;
                ring(vc, fm, 0.5F, y, 0.5F, r - 0.012F, r, 18, 0.7F, time * 0.04F + k, c, 0.45F * (1 - t));
            }
        }
        pose.popPose();
    }

    @Override
    public boolean shouldRenderOffScreen(QuantumModStationBlockEntity station) {
        return false;
    }

    private static void ring(VertexConsumer vc, Matrix4f m, float cx, float y, float cz, float r0, float r1, int segments, float fill,
                             float rot, float[] c, float a) {
        float step = (float) (Math.PI * 2 / segments);
        for (int i = 0; i < segments; i++) {
            float a0 = rot + i * step, a1 = a0 + step * fill;
            float s0 = (float) Math.sin(a0), c0 = (float) Math.cos(a0), s1 = (float) Math.sin(a1), c1 = (float) Math.cos(a1);
            vc.addVertex(m, cx + s0 * r0, y, cz + c0 * r0).setColor(c[0], c[1], c[2], a);
            vc.addVertex(m, cx + s0 * r1, y, cz + c0 * r1).setColor(c[0], c[1], c[2], a);
            vc.addVertex(m, cx + s1 * r1, y, cz + c1 * r1).setColor(c[0], c[1], c[2], a);
            vc.addVertex(m, cx + s1 * r0, y, cz + c1 * r0).setColor(c[0], c[1], c[2], a);
        }
    }

    private static void plane(VertexConsumer vc, Matrix4f m, float x0, float y, float z0, float x1, float z1, float[] c, float a) {
        vc.addVertex(m, x0, y, z0).setColor(c[0], c[1], c[2], a);
        vc.addVertex(m, x1, y, z0).setColor(c[0], c[1], c[2], a);
        vc.addVertex(m, x1, y, z1).setColor(c[0], c[1], c[2], a);
        vc.addVertex(m, x0, y, z1).setColor(c[0], c[1], c[2], a);
    }

    /** A thin glowing beam from (x0,y0,z0) to (x1,y1,z1), fading towards the end. */
    private static void beam(VertexConsumer vc, Matrix4f m, float x0, float y0, float z0, float x1, float y1, float z1, float[] c, float a) {
        float w = 0.008F;
        vc.addVertex(m, x0 - w, y0, z0).setColor(c[0], c[1], c[2], a);
        vc.addVertex(m, x0 + w, y0, z0).setColor(c[0], c[1], c[2], a);
        vc.addVertex(m, x1 + w, y1, z1).setColor(c[0], c[1], c[2], a * 0.15F);
        vc.addVertex(m, x1 - w, y1, z1).setColor(c[0], c[1], c[2], a * 0.15F);
    }
}
