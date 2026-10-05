// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.client.suit;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.miophas.singularity_iteration.common.client.render.SiRenderTypes;
import com.miophas.singularity_iteration.common.suit.QuantumModStationBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix4f;

/** Shows the suit piece hovering over the station pad, with a scan plane while a unit installs. */
public class QuantumModStationRenderer implements BlockEntityRenderer<QuantumModStationBlockEntity> {
    public QuantumModStationRenderer(BlockEntityRendererProvider.Context context) { }

    @Override
    public void render(QuantumModStationBlockEntity station, float partial, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        ItemStack suit = station.suit();
        if (suit.isEmpty() || station.getLevel() == null) return;
        float time = station.getLevel().getGameTime() + partial;
        pose.pushPose();
        pose.translate(0.5, 0.95 + Math.sin(time / 18.0) * 0.03, 0.5);
        pose.mulPose(Axis.YP.rotationDegrees(time * 1.5F));
        pose.scale(0.62F, 0.62F, 0.62F);
        Minecraft.getInstance().getItemRenderer().renderStatic(suit, ItemDisplayContext.FIXED, LightTexture.FULL_BRIGHT,
            OverlayTexture.NO_OVERLAY, pose, buffers, station.getLevel(), 0);
        pose.popPose();
        if (!station.isClientWorking()) return;
        // scan plane sweeping up and down through the piece
        float h = 0.62F + 0.5F * (0.5F + 0.5F * (float) Math.sin(time / 6.0));
        VertexConsumer vc = buffers.getBuffer(SiRenderTypes.FIELD_GLOW);
        Matrix4f m = pose.last().pose();
        float a = 0.45F;
        quad(vc, m, 0.12F, h, 0.12F, 0.88F, h, 0.88F, 0.3F, 0.9F, 0.8F, a);
        // emitter beams from the frame to the plane edges
        for (int i = 0; i < 4; i++) {
            float x = 0.2F + i * 0.2F;
            quad(vc, m, x, 0.44F, 0.5F, x + 0.02F, h, 0.5F, 0.2F, 0.7F, 0.6F, a * 0.6F);
        }
    }

    private static void quad(VertexConsumer vc, Matrix4f m, float x0, float y0, float z0, float x1, float y1, float z1,
                             float r, float g, float b, float a) {
        if (y0 == y1) {
            vc.addVertex(m, x0, y0, z0).setColor(r, g, b, a);
            vc.addVertex(m, x1, y0, z0).setColor(r, g, b, a);
            vc.addVertex(m, x1, y0, z1).setColor(r, g, b, a);
            vc.addVertex(m, x0, y0, z1).setColor(r, g, b, a);
        } else {
            vc.addVertex(m, x0, y0, z0).setColor(r, g, b, a);
            vc.addVertex(m, x1, y0, z0).setColor(r, g, b, a);
            vc.addVertex(m, x1, y1, z1).setColor(r, g, b, 0);
            vc.addVertex(m, x0, y1, z1).setColor(r, g, b, 0);
        }
    }
}
