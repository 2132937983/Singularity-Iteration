package com.miophas.singularity_iteration.common.client.screen;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Text colour that stays readable on a GUI background texture whatever a resource pack paints it:
 * the region behind a label is sampled once from the texture and dark or light ink is chosen.
 * Lets replacement texture packs ship dark panels without the labels disappearing.
 */
@OnlyIn(Dist.CLIENT)
public final class GuiInk {
    private GuiInk() {}

    public static final int DARK = SiGuiTheme.TEXT, LIGHT = 0xFFE6ECF1;
    private static final ConcurrentHashMap<String, Integer> CACHE = new ConcurrentHashMap<>();
    private static Object resources;

    /**
     * Ink for a label over {@code [u, v, w, h]} of {@code texture}, a {@code texW x texH} image
     * blitted at its natural scale. Falls back to dark ink when the texture cannot be read.
     */
    public static int on(ResourceLocation texture, int u, int v, int w, int h, int texW, int texH) {
        var manager = Minecraft.getInstance().getResourceManager();
        if (resources != manager) { CACHE.clear(); resources = manager; }   // new pack stack after a reload
        return CACHE.computeIfAbsent(texture + "@" + u + "," + v + "," + w + "," + h, k -> sample(manager, texture, u, v, w, h, texW, texH));
    }

    private static int sample(net.minecraft.server.packs.resources.ResourceManager manager, ResourceLocation texture,
                              int u, int v, int w, int h, int texW, int texH) {
        var res = manager.getResource(texture);
        if (res.isEmpty()) return DARK;
        try (var in = res.get().open(); NativeImage img = NativeImage.read(in)) {
            double sx = img.getWidth() / (double) texW, sy = img.getHeight() / (double) texH;
            long sum = 0, n = 0;
            for (int y = v; y < v + h; y++) for (int x = u; x < u + w; x++) {
                int px = (int) (x * sx), py = (int) (y * sy);
                if (px < 0 || py < 0 || px >= img.getWidth() || py >= img.getHeight()) continue;
                int abgr = img.getPixelRGBA(px, py);
                if (((abgr >>> 24) & 255) < 128) continue;
                int r = abgr & 255, g = (abgr >> 8) & 255, b = (abgr >> 16) & 255;
                sum += (r * 299 + g * 587 + b * 114) / 1000; n++;
            }
            return n > 0 && sum / n < 110 ? LIGHT : DARK;
        } catch (Exception e) {
            return DARK;
        }
    }
}
