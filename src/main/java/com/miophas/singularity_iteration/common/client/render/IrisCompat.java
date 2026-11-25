// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.client.render;

import com.mojang.logging.LogUtils;
import java.lang.reflect.Method;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

/**
 * Iris shader-pack detection and render-stage alignment (optional dependency, read via public {@code IrisApi}
 * reflection):
 * <ul>
 *   <li>{@link #shaderPackInUse()}: whether an Iris shader pack is active (cached up to 1 s).</li>
 *   <li>{@link #isShadowPass()}: whether Iris is currently rendering the shadow map pass (queried live when a shader
 *       pack is active so pure light/energy FX never cast black shadows on the ground).</li>
 *   <li>{@link #isFxStage(RenderLevelStageEvent)}: aligns world FX to {@code AFTER_TRANSLUCENT_BLOCKS} while a shader
 *       pack is active (so emissive FX enter the shader's HDR bloom &amp; depth pipeline cleanly) and keeps
 *       {@code AFTER_PARTICLES} in vanilla rendering.</li>
 * </ul>
 */
public final class IrisCompat {
    private IrisCompat() {}

    private static boolean probed, present, inUse, logged;
    private static Object api;
    private static Method isShaderPackInUse;
    private static Method isRenderingShadowPass;
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
                LogUtils.getLogger().info("Iris shader pack in use: routing energy FX through shader-safe emissive pipeline");
            }
        }
        return inUse;
    }

    /**
     * True while Iris is rendering the shadow-map pass. Pure additive/emissive FX (energy wings, rings, trails,
     * beams, holo scans) should skip drawing during the shadow pass so they never cast dark geometry shadows.
     */
    public static boolean isShadowPass() {
        if (!shaderPackInUse() || isRenderingShadowPass == null) return false;
        try {
            return (boolean) isRenderingShadowPass.invoke(api);
        } catch (Throwable t) {
            isRenderingShadowPass = null;
            return false;
        }
    }

    /** World render stage where 3D electromagnetic/energy FX should draw for the current pipeline. */
    public static RenderLevelStageEvent.Stage fxStage() {
        return shaderPackInUse()
            ? RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS
            : RenderLevelStageEvent.Stage.AFTER_PARTICLES;
    }

    /** True when {@code event} is at the active FX world render stage (and never during a shadow pass). */
    public static boolean isFxStage(RenderLevelStageEvent event) {
        return event.getStage() == fxStage() && !isShadowPass();
    }

    private static void probe() {
        probed = true;
        try {
            Class<?> type = Class.forName("net.irisshaders.iris.api.v0.IrisApi");
            api = type.getMethod("getInstance").invoke(null);
            isShaderPackInUse = type.getMethod("isShaderPackInUse");
            try {
                isRenderingShadowPass = type.getMethod("isRenderingShadowPass");
            } catch (Throwable ignored) {
                isRenderingShadowPass = null;
            }
            present = api != null;
        } catch (Throwable t) {
            present = false;
        }
    }
}
