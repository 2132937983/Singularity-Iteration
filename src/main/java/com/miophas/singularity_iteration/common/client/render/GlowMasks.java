// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.client.render;

import com.miophas.singularity_iteration.common.Singularity_Iteration;
import com.mojang.blaze3d.platform.NativeImage;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

/**
 * 0.1.10.30: the glow masks of the baked suit pieces ({@code textures/models/armor/<piece>_glow.png}, made by
 * {@code tools/glow_masks_1130.py}; a piece without one glows on its {@code _accent} mask).
 *
 * <p>For each piece the quads whose texture rectangle holds at least one lit mask pixel are kept per part, once per resource
 * reload: the glow pass draws only those (the full quantum suit: 1,218 quads, of which the glow covers a few hundred), so the
 * rest of the armour costs nothing. A piece without any mask has no glow.
 */
public final class GlowMasks {
    private GlowMasks() {}

    /** The glow of one baked piece: the mask texture and the lit quads per part (same arrays as the baked geometry rows). */
    public record Mask(@Nullable ResourceLocation texture, Map<String, float[][]> quads, int count) {
        public static final Mask NONE = new Mask(null, Map.of(), 0);

        public boolean isEmpty() { return texture == null || count == 0; }
    }

    private record Entry(Map<String, float[][]> geometry, Mask mask) { }

    private static final Map<String, Entry> CACHE = new HashMap<>();

    /** Called by the client reload listener with the baked models. */
    public static synchronized void invalidate() {
        CACHE.clear();
    }

    /** The glow of baked piece {@code name} (for example {@code quantum_chestplate}) over its {@code geometry}. */
    public static synchronized Mask of(String name, Map<String, float[][]> geometry) {
        Entry e = CACHE.get(name);
        if (e != null && e.geometry() == geometry) return e.mask();
        Mask mask = load(name, geometry);
        CACHE.put(name, new Entry(geometry, mask));
        return mask;
    }

    private static Mask load(String name, Map<String, float[][]> geometry) {
        for (String suffix : new String[]{"_glow", "_accent"}) {
            ResourceLocation id = ResourceLocation.fromNamespaceAndPath(Singularity_Iteration.MOD_ID, "textures/models/armor/" + name + suffix + ".png");
            var resource = Minecraft.getInstance().getResourceManager().getResource(id);
            if (resource.isEmpty()) continue;
            try (InputStream in = resource.get().open(); NativeImage image = NativeImage.read(in)) {
                return filter(id, geometry, image);
            } catch (Exception ex) {
                Singularity_Iteration.LOGGER.warn("Cannot read the glow mask {}", id, ex);
                return Mask.NONE;
            }
        }
        return Mask.NONE;
    }

    private static Mask filter(ResourceLocation id, Map<String, float[][]> geometry, NativeImage image) {
        int w = image.getWidth(), h = image.getHeight();
        boolean[] lit = new boolean[w * h];
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int abgr = image.getPixelRGBA(x, y);
                int a = abgr >>> 24, r = abgr & 255, g = (abgr >> 8) & 255, b = (abgr >> 16) & 255;
                lit[y * w + x] = a > 0 && Math.max(r, Math.max(g, b)) > 8;
            }
        }
        Map<String, float[][]> out = new HashMap<>();
        int count = 0;
        for (Map.Entry<String, float[][]> part : geometry.entrySet()) {
            List<float[]> kept = new ArrayList<>();
            for (float[] q : part.getValue()) if (covers(q, lit, w, h)) kept.add(q);
            if (kept.isEmpty()) continue;
            out.put(part.getKey(), kept.toArray(new float[0][]));
            count += kept.size();
        }
        return new Mask(id, Map.copyOf(out), count);
    }

    /** True when the texture rectangle of quad {@code q} (float[23]: 4 x (x y z u v) + normal) holds a lit pixel. */
    static boolean covers(float[] q, boolean[] lit, int w, int h) {
        float u0 = Math.min(Math.min(q[3], q[8]), Math.min(q[13], q[18])), u1 = Math.max(Math.max(q[3], q[8]), Math.max(q[13], q[18]));
        float v0 = Math.min(Math.min(q[4], q[9]), Math.min(q[14], q[19])), v1 = Math.max(Math.max(q[4], q[9]), Math.max(q[14], q[19]));
        int x0 = Math.max(0, (int) Math.floor(u0 * w + 1.0E-4)), x1 = Math.min(w, Math.max(x0 + 1, (int) Math.ceil(u1 * w - 1.0E-4)));
        int y0 = Math.max(0, (int) Math.floor(v0 * h + 1.0E-4)), y1 = Math.min(h, Math.max(y0 + 1, (int) Math.ceil(v1 * h - 1.0E-4)));
        for (int y = y0; y < y1; y++) for (int x = x0; x < x1; x++) if (lit[y * w + x]) return true;
        return false;
    }
}
