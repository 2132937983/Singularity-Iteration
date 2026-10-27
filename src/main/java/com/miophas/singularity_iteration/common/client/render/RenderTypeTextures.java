// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.client.render;

import java.util.Optional;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;

/**
 * 0.1.10.30: the texture of a render type (through the access transformer of the mod: {@code CompositeRenderType.state()},
 * {@code CompositeState.textureState}, {@code cutoutTexture()}). The holo projector draws any projected gear (vanilla armour,
 * the suits, armour of other mods with their own render types) again with that texture in its hologram render types.
 */
public final class RenderTypeTextures {
    private RenderTypeTextures() {}

    /** The texture of {@code type}, empty for a type without one (or not a composite type). */
    public static Optional<ResourceLocation> of(RenderType type) {
        if (type instanceof RenderType.CompositeRenderType composite) return composite.state().textureState.cutoutTexture();
        return Optional.empty();
    }

    /** True for the enchantment glint types (their texture is the glint, not the gear). */
    public static boolean isGlint(ResourceLocation texture) {
        return texture.getPath().contains("enchanted_glint");
    }
}
