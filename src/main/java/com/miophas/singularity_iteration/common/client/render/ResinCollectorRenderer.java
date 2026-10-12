// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.client.render;

import com.miophas.singularity_iteration.common.block.producer.ResinCollectorBlock;
import com.miophas.singularity_iteration.common.blockentity.producer.ResinCollectorBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/**
 * Resin collector fluid animation (0.1.8.0). The tray of the model holds liquid resin: an amber,
 * animated surface whose level rises with the stored resin (0..64). For
 * {@link #STREAM_TICKS} ticks after a collection, a resin stream runs from the spout down the
 * guide channel into the tray. Coordinates are model pixels of the "facing = west" model (the
 * log is at +x); the blockstate rotation is applied first.
 */
@OnlyIn(Dist.CLIENT)
public class ResinCollectorRenderer implements BlockEntityRenderer<ResinCollectorBlockEntity> {
    static final int STREAM_TICKS = 60;
    private static final ResourceLocation STILL = ResourceLocation.withDefaultNamespace("block/water_still");
    private static final ResourceLocation FLOW = ResourceLocation.withDefaultNamespace("block/water_flow");
    /** Amber resin colour (ARGB). */
    private static final int RESIN = 0xE0C27A1E, RESIN_STREAM = 0xF0D08A2A;

    public ResinCollectorRenderer(BlockEntityRendererProvider.Context context) { }

    @Override
    public void render(ResinCollectorBlockEntity be, float partial, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        if (be.getLevel() == null) return;
        int count = be.clientCount();
        long since = be.getLevel().getGameTime() - be.clientLastCollect();
        boolean stream = be.clientLastCollect() != Long.MIN_VALUE && since >= 0 && since < STREAM_TICKS;
        if (count <= 0 && !stream) return;
        var atlas = Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS);
        TextureAtlasSprite still = atlas.apply(STILL), flow = atlas.apply(FLOW);
        VertexConsumer vc = buffers.getBuffer(RenderType.translucent());
        pose.pushPose();
        Direction facing = be.getBlockState().hasProperty(ResinCollectorBlock.FACING) ? be.getBlockState().getValue(ResinCollectorBlock.FACING) : Direction.WEST;
        int y = switch (facing) { case NORTH -> 90; case EAST -> 180; case SOUTH -> 270; default -> 0; };
        pose.translate(0.5, 0, 0.5);
        pose.mulPose(Axis.YP.rotationDegrees(-y));                      // blockstate y rotation is clockwise from above
        pose.translate(-0.5, 0, -0.5);
        pose.scale(1 / 16F, 1 / 16F, 1 / 16F);
        float level = 4.05F + 3.9F * Math.min(64, count) / 64F;
        if (count > 0) {
            // tray surface (interior of the basin: x 3..13, z 3..13)
            quad(vc, pose, still, 3, level, 3, 13, level, 3, 13, level, 13, 3, level, 13, RESIN, light, 0, 1, 0);
        }
        if (stream) {
            float fade = since < 6 ? since / 6F : since > STREAM_TICKS - 10 ? (STREAM_TICKS - since) / 10F : 1F;
            int color = ((int) (((RESIN_STREAM >>> 24) & 255) * Math.max(0, Math.min(1, fade))) << 24) | (RESIN_STREAM & 0xFFFFFF);
            float t = (be.getLevel().getGameTime() % 32 + partial) / 32F;
            // spout mouth -> guide channel (45 degrees) -> tray
            float end = Math.max(level, 4.1F);
            ribbon(vc, pose, flow, 12.5F, 10.4F, 11.2F, 9.9F, color, light, t);       // out of the spout
            ribbon(vc, pose, flow, 11.2F, 9.9F, 6.2F, 4.9F, color, light, t);         // down the channel
            ribbon(vc, pose, flow, 6.2F, 4.9F, 5.6F, end, color, light, t);           // into the tray
            if (count > 0) {
                // ripple ring where the stream meets the surface
                float r = 0.6F + 0.8F * t;
                quad(vc, pose, still, 5.6F - r, level + 0.02F, 8 - r, 5.6F + r, level + 0.02F, 8 - r, 5.6F + r, level + 0.02F, 8 + r,
                    5.6F - r, level + 0.02F, 8 + r, (color & 0xFFFFFF) | 0x60000000, light, 0, 1, 0);
            }
        }
        pose.popPose();
    }

    /** A stream segment 1.4 px wide (z 7.3..8.7) in the x-y plane, with flowing texture coordinates. */
    private static void ribbon(VertexConsumer vc, PoseStack pose, TextureAtlasSprite sprite, float x0, float y0, float x1, float y1,
                               int color, int light, float t) {
        float z0 = 7.3F, z1 = 8.7F, lift = 0.15F;
        float u0 = sprite.getU(0.3F), u1 = sprite.getU(0.45F);
        float len = (float) Math.hypot(x1 - x0, y1 - y0) / 16F;
        float v0 = sprite.getV(t), v1 = sprite.getV(Math.min(1F, t + len));
        var last = pose.last();
        // top
        vc.addVertex(last, x0, y0 + lift, z0).setColor(color).setUv(u0, v0).setLight(light).setNormal(last, 0, 1, 0);
        vc.addVertex(last, x0, y0 + lift, z1).setColor(color).setUv(u1, v0).setLight(light).setNormal(last, 0, 1, 0);
        vc.addVertex(last, x1, y1 + lift, z1).setColor(color).setUv(u1, v1).setLight(light).setNormal(last, 0, 1, 0);
        vc.addVertex(last, x1, y1 + lift, z0).setColor(color).setUv(u0, v1).setLight(light).setNormal(last, 0, 1, 0);
        // underside (seen from below the spout)
        vc.addVertex(last, x1, y1 - lift, z0).setColor(color).setUv(u0, v1).setLight(light).setNormal(last, 0, -1, 0);
        vc.addVertex(last, x1, y1 - lift, z1).setColor(color).setUv(u1, v1).setLight(light).setNormal(last, 0, -1, 0);
        vc.addVertex(last, x0, y0 - lift, z1).setColor(color).setUv(u1, v0).setLight(light).setNormal(last, 0, -1, 0);
        vc.addVertex(last, x0, y0 - lift, z0).setColor(color).setUv(u0, v0).setLight(light).setNormal(last, 0, -1, 0);
    }

    private static void quad(VertexConsumer vc, PoseStack pose, TextureAtlasSprite sprite,
                             float x0, float y0, float z0, float x1, float y1, float z1, float x2, float y2, float z2, float x3, float y3, float z3,
                             int color, int light, float nx, float ny, float nz) {
        var last = pose.last();
        float u0 = sprite.getU(x0 / 16F), u1 = sprite.getU(x2 / 16F), v0 = sprite.getV(z0 / 16F), v1 = sprite.getV(z2 / 16F);
        vc.addVertex(last, x0, y0, z0).setColor(color).setUv(u0, v0).setLight(light).setNormal(last, nx, ny, nz);
        vc.addVertex(last, x3, y3, z3).setColor(color).setUv(u0, v1).setLight(light).setNormal(last, nx, ny, nz);
        vc.addVertex(last, x2, y2, z2).setColor(color).setUv(u1, v1).setLight(light).setNormal(last, nx, ny, nz);
        vc.addVertex(last, x1, y1, z1).setColor(color).setUv(u1, v0).setLight(light).setNormal(last, nx, ny, nz);
    }
}
