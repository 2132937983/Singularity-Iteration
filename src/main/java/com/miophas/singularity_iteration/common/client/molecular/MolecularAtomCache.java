// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.client.molecular;

import com.miophas.singularity_iteration.common.Singularity_Iteration;
import com.mojang.blaze3d.platform.NativeImage;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * 0.1.11.213 (MT1 X1): the atoms of each item, read once from the texture of the item's model (its particle sprite: the
 * item icon for flat items, a face texture for blocks). Cleared on a resource reload.
 */
public final class MolecularAtomCache {
    private MolecularAtomCache() {}

    private static final Map<ResourceLocation, List<MolecularAtoms.Atom>> CACHE = new HashMap<>();

    public static synchronized void invalidate() {
        CACHE.clear();
    }

    public static synchronized List<MolecularAtoms.Atom> of(ItemStack stack, Level level) {
        if (stack.isEmpty()) return List.of();
        ResourceLocation sprite;
        try {
            sprite = Minecraft.getInstance().getItemRenderer().getModel(stack, level, null, 0).getParticleIcon().contents().name();
        } catch (RuntimeException ex) {
            return MolecularAtoms.fallback();
        }
        return CACHE.computeIfAbsent(sprite, MolecularAtomCache::load);
    }

    private static List<MolecularAtoms.Atom> load(ResourceLocation sprite) {
        ResourceLocation file = ResourceLocation.fromNamespaceAndPath(sprite.getNamespace(), "textures/" + sprite.getPath() + ".png");
        var resource = Minecraft.getInstance().getResourceManager().getResource(file);
        if (resource.isEmpty()) return MolecularAtoms.fallback();
        try (InputStream in = resource.get().open(); NativeImage image = NativeImage.read(in)) {
            int w = image.getWidth(), h = image.getHeight();
            int[] argb = new int[w * h];
            for (int y = 0; y < h; y++) {
                for (int x = 0; x < w; x++) {
                    int abgr = image.getPixelRGBA(x, y);
                    argb[y * w + x] = (abgr & 0xFF00FF00) | ((abgr & 0xFF) << 16) | ((abgr >> 16) & 0xFF);
                }
            }
            List<MolecularAtoms.Atom> atoms = MolecularAtoms.sample(argb, w, h);
            return atoms.isEmpty() ? MolecularAtoms.fallback() : List.copyOf(atoms);
        } catch (Exception ex) {
            Singularity_Iteration.LOGGER.warn("Cannot read {} for the molecular transformer", file, ex);
            return MolecularAtoms.fallback();
        }
    }
}
