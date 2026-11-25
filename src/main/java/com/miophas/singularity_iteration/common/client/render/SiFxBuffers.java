// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.client.render;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;

/**
 * Shader-aware FX buffer facade.
 *
 * <p>Callers continue writing standard {@code addVertex(m, x, y, z).setColor(r, g, b, a)} quads. When no shader pack
 * is active, this returns the fast vanilla {@code POSITION_COLOR} buffers ({@link SiRenderTypes#FIELD_GLOW} /
 * {@link SiRenderTypes#VAPOR}) directly with zero wrapping overhead. When an Iris shader pack is active, it routes
 * through {@link SiRenderTypes#SHADER_GLOW} / {@link SiRenderTypes#SHADER_TRANSLUCENT} and automatically supplies the
 * full {@code NEW_ENTITY} attributes (UV, overlay, full-bright lightmap, normal) plus gentle HDR bloom alpha calibration.
 */
public final class SiFxBuffers {
    private SiFxBuffers() {}

    /** Active additive glow RenderType for the current shader state. */
    public static RenderType glowType() {
        return IrisCompat.shaderPackInUse() ? SiRenderTypes.SHADER_GLOW : SiRenderTypes.FIELD_GLOW;
    }

    /** Active alpha-blended translucent RenderType for the current shader state. */
    public static RenderType translucentType() {
        return IrisCompat.shaderPackInUse() ? SiRenderTypes.SHADER_TRANSLUCENT : SiRenderTypes.VAPOR;
    }

    /** Returns an additive glow {@link VertexConsumer} safe for both vanilla and Iris shader packs. */
    public static VertexConsumer glow(MultiBufferSource buffers) {
        if (!IrisCompat.shaderPackInUse()) return buffers.getBuffer(SiRenderTypes.FIELD_GLOW);
        return new EntityEmissiveAdapter(buffers.getBuffer(SiRenderTypes.SHADER_GLOW), true);
    }

    /** Returns an alpha-blended translucent {@link VertexConsumer} safe for both vanilla and Iris shader packs. */
    public static VertexConsumer translucent(MultiBufferSource buffers) {
        if (!IrisCompat.shaderPackInUse()) return buffers.getBuffer(SiRenderTypes.VAPOR);
        return new EntityEmissiveAdapter(buffers.getBuffer(SiRenderTypes.SHADER_TRANSLUCENT), false);
    }

    /** Flushes the active additive glow batch. */
    public static void endGlow(MultiBufferSource.BufferSource buffers) {
        buffers.endBatch(glowType());
    }

    /** Flushes the active alpha-blended translucent batch. */
    public static void endTranslucent(MultiBufferSource.BufferSource buffers) {
        buffers.endBatch(translucentType());
    }

    /**
     * Lightweight {@link VertexConsumer} adapter that upgrades {@code POSITION_COLOR} vertex calls into complete
     * {@code NEW_ENTITY} vertices (UV 0.5/0.5, no overlay, full-bright 0xF000F0 lightmap, up normal) for Iris.
     */
    private static final class EntityEmissiveAdapter implements VertexConsumer {
        private final VertexConsumer d;
        private final boolean additive;

        EntityEmissiveAdapter(VertexConsumer delegate, boolean additive) {
            this.d = delegate;
            this.additive = additive;
        }

        @Override
        public VertexConsumer addVertex(float x, float y, float z) {
            d.addVertex(x, y, z);
            return this;
        }

        @Override
        public VertexConsumer setColor(int r, int g, int b, int a) {
            d.setColor(r, g, b, SiFxRules.calibrateAlphaByte(a, additive))
                .setUv(0.5F, 0.5F)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(LightTexture.FULL_BRIGHT)
                .setNormal(0F, 1F, 0F);
            return this;
        }

        @Override public VertexConsumer setUv(float u, float v) { d.setUv(u, v); return this; }
        @Override public VertexConsumer setUv1(int u, int v) { d.setUv1(u, v); return this; }
        @Override public VertexConsumer setUv2(int u, int v) { d.setUv2(u, v); return this; }
        @Override public VertexConsumer setNormal(float x, float y, float z) { d.setNormal(x, y, z); return this; }
    }
}
