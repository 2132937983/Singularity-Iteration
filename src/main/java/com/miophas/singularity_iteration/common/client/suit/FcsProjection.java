// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.client.suit;

import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Vector4f;

/**
 * World -> GUI projection for HUD tags. The world pass stores this frame's camera matrices; the
 * HUD pass of the same frame maps world points to GUI pixels with them.
 */
public final class FcsProjection {
    private FcsProjection() {}

    private static final Matrix4f PROJECTION = new Matrix4f();
    private static final Matrix4f VIEW = new Matrix4f();
    private static Vec3 camera = Vec3.ZERO;
    private static boolean valid;
    private static final Vector4f TMP = new Vector4f();

    public static void capture(Matrix4f projection, Matrix4f modelView, Vec3 cameraPos) {
        PROJECTION.set(projection);
        VIEW.set(modelView);
        camera = cameraPos;
        valid = true;
    }

    public static Vec3 camera() { return camera; }

    /**
     * GUI coordinates {x, y} of a world point, or null when it is behind the camera.
     * Points off screen are returned too (callers clamp them to the edge when they want to).
     */
    @Nullable
    public static float[] project(double x, double y, double z) {
        if (!valid) return null;
        TMP.set((float) (x - camera.x), (float) (y - camera.y), (float) (z - camera.z), 1.0F);
        VIEW.transform(TMP);
        PROJECTION.transform(TMP);
        if (TMP.w <= 0.05F) return null;
        float ndcX = TMP.x / TMP.w, ndcY = TMP.y / TMP.w;
        var window = Minecraft.getInstance().getWindow();
        float w = window.getGuiScaledWidth(), h = window.getGuiScaledHeight();
        return new float[]{(ndcX * 0.5F + 0.5F) * w, (0.5F - ndcY * 0.5F) * h};
    }

    @Nullable
    public static float[] project(Vec3 p) {
        return project(p.x, p.y, p.z);
    }

    /**
     * Screen-space bearing of a world point around the screen centre (radians, 0 = up,
     * clockwise). Works for points behind the camera too (used by the edge threat markers).
     */
    public static float bearing(double x, double y, double z) {
        TMP.set((float) (x - camera.x), (float) (y - camera.y), (float) (z - camera.z), 1.0F);
        VIEW.transform(TMP);
        // view space: +x right, +y up, -z forward
        float right = TMP.x, up = TMP.y, forward = -TMP.z;
        if (forward < 0) up = -Math.abs(up) - Math.abs(forward) * 0.25F;   // behind: show at the bottom half
        return (float) Math.atan2(right, up);
    }
}
