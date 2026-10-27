// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.client.render;

import com.mojang.logging.LogUtils;
import java.lang.reflect.Method;

/**
 * 0.1.10.30 (P-1): whether an Iris shader pack is on (Iris is optional: read through its public API by reflection,
 * {@code net.irisshaders.iris.api.v0.IrisApi#isShaderPackInUse}, at most once a second).
 *
 * <p>A shader pack renders the world into its own buffers, so the scene copy of the nano refraction (the camouflage, the dash
 * and EMP shock waves, the heat haze of the thrusters) would read the wrong image: while a pack is on, those effects use
 * their fallbacks (the hologram grid and the additive glow) without the player turning the option off.
 */
public final class IrisCompat {
    private IrisCompat() {}

    private static boolean probed, present, inUse, logged;
    private static Object api;
    private static Method isShaderPackInUse;
    private static long checkedAt;

    /** True while an Iris shader pack is in use. */
    public static boolean shaderPackInUse() {
        if (!probed) probe();
        if (!present) return false;
        long now = System.currentTimeMillis();
        if (now - checkedAt >= 1000L) {
            checkedAt = now;
            try {
                inUse = (boolean) isShaderPackInUse.invoke(api);
            } catch (Throwable t) {
                present = false;
                inUse = false;
            }
            if (inUse && !logged) {
                logged = true;
                LogUtils.getLogger().info("Iris shader pack in use: the nano refraction and heat haze use their fallbacks");
            }
        }
        return inUse;
    }

    private static void probe() {
        probed = true;
        try {
            Class<?> type = Class.forName("net.irisshaders.iris.api.v0.IrisApi");
            api = type.getMethod("getInstance").invoke(null);
            isShaderPackInUse = type.getMethod("isShaderPackInUse");
            present = api != null;
        } catch (Throwable t) {
            present = false;
        }
    }
}
