// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.client.suit.nano;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;

/**
 * Buffer source that draws a whole entity (body, armour, held items) with one effect render
 * type (0.1.8.5): every entity-format render type the renderer asks for becomes {@code target},
 * and each vertex colour is multiplied by the tint. Other formats (enchantment glint, lines) are
 * dropped, so the effect never gets vertices it cannot take.
 */
final class RemapBuffers implements MultiBufferSource {
    private final MultiBufferSource delegate;
    private final RenderType target;
    private final float r, g, b, a;

    RemapBuffers(MultiBufferSource delegate, RenderType target, float r, float g, float b, float a) {
        this.delegate = delegate;
        this.target = target;
        this.r = r;
        this.g = g;
        this.b = b;
        this.a = a;
    }

    @Override
    public VertexConsumer getBuffer(RenderType type) {
        if (type.format() != DefaultVertexFormat.NEW_ENTITY || type.mode() != com.mojang.blaze3d.vertex.VertexFormat.Mode.QUADS) return NoOp.INSTANCE;
        return new Tint(delegate.getBuffer(target), r, g, b, a);
    }

    /** Multiplies the vertex colour. */
    static final class Tint implements VertexConsumer {
        private final VertexConsumer d;
        private final float r, g, b, a;

        Tint(VertexConsumer d, float r, float g, float b, float a) {
            this.d = d;
            this.r = r;
            this.g = g;
            this.b = b;
            this.a = a;
        }

        @Override public VertexConsumer addVertex(float x, float y, float z) { d.addVertex(x, y, z); return this; }
        @Override public VertexConsumer setColor(int red, int green, int blue, int alpha) {
            d.setColor(Math.round(red * r), Math.round(green * g), Math.round(blue * b), Math.round(alpha * a));
            return this;
        }
        @Override public VertexConsumer setUv(float u, float v) { d.setUv(u, v); return this; }
        @Override public VertexConsumer setUv1(int u, int v) { d.setUv1(u, v); return this; }
        @Override public VertexConsumer setUv2(int u, int v) { d.setUv2(u, v); return this; }
        @Override public VertexConsumer setNormal(float x, float y, float z) { d.setNormal(x, y, z); return this; }
    }

    /** Swallows every vertex. */
    static final class NoOp implements VertexConsumer {
        static final NoOp INSTANCE = new NoOp();
        @Override public VertexConsumer addVertex(float x, float y, float z) { return this; }
        @Override public VertexConsumer setColor(int red, int green, int blue, int alpha) { return this; }
        @Override public VertexConsumer setUv(float u, float v) { return this; }
        @Override public VertexConsumer setUv1(int u, int v) { return this; }
        @Override public VertexConsumer setUv2(int u, int v) { return this; }
        @Override public VertexConsumer setNormal(float x, float y, float z) { return this; }
    }
}
