// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.client.suit;

import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Vector4f;

/**
 * World to GUI projection for HUD tags.
 *
 * <p><b>Capture.</b> {@code FcsClient.onRenderLevel} calls {@link #capture} at the
 * {@code AFTER_TRANSLUCENT_BLOCKS} stage of each frame. At this stage the level renderer has set
 * the final camera matrices for the frame. They include the FOV change, the view bobbing and the
 * camera roll. The HUD pass of the same frame comes after the level pass. It uses the stored
 * matrices to put tags on the world points. Thus the tags and the world use the same camera, with
 * no lag of one frame.
 *
 * <p><b>Frames.</b> The model-view matrix is the camera rotation only. It has no translation.
 * Thus {@link #project} subtracts the camera position first, in double precision. Then it converts
 * to float. This keeps the precision far from the world origin.
 *
 * <p><b>Thread.</b> Call all methods on the render thread only. The class uses one shared
 * scratch vector.
 */
public final class FcsProjection {
    private FcsProjection() {}

    private static final Matrix4f PROJECTION = new Matrix4f();
    private static final Matrix4f VIEW = new Matrix4f();
    /** Camera position of the last capture, in world blocks. */
    private static Vec3 camera = Vec3.ZERO;
    /** True after the first capture. {@link #reset} sets it back to false at logout. */
    private static boolean valid;
    /** Scratch vector. It is not thread safe. */
    private static final Vector4f TMP = new Vector4f();

    /**
     * Stores the camera of this frame. The method copies the matrices, so the caller can change
     * its own matrices after the call.
     *
     * @param projection the projection matrix of the frame.
     * @param modelView  the view rotation matrix of the frame (no translation).
     * @param cameraPos  the camera position in world blocks.
     */
    public static void capture(Matrix4f projection, Matrix4f modelView, Vec3 cameraPos) {
        PROJECTION.set(projection);
        VIEW.set(modelView);
        camera = cameraPos;
        valid = true;
    }

    /** Camera position of the last capture, in world blocks. */
    public static Vec3 camera() { return camera; }

    /**
     * Forgets the last capture. Called when the client leaves a world, so the first frame of the
     * next world does not project markers with the camera of the old world (0.1.7.36).
     */
    public static void reset() {
        valid = false;
        camera = Vec3.ZERO;
    }

    /**
     * Projects a world point to GUI coordinates.
     *
     * <p>Math:
     * <ol>
     *   <li>{@code clip = PROJECTION * VIEW * (p - camera, 1)}.</li>
     *   <li>Behind test: for a perspective projection, {@code clip.w} is the distance in front of
     *       the camera along the view axis. When {@code w <= 0.05} the point is behind the camera
     *       or nearer than the vanilla near plane (0.05 blocks). The method returns null. Without
     *       this test, the divide by w mirrors a point behind the camera to the front.</li>
     *   <li>NDC: {@code ndc = clip.xy / clip.w}. Each axis is -1..1 on the screen. NDC y is up.</li>
     *   <li>GUI: {@code x = (ndc.x * 0.5 + 0.5) * width} and {@code y = (0.5 - ndc.y * 0.5) * height}.
     *       The y formula flips the axis, because GUI y is down. Width and height are the
     *       GUI-scaled window size.</li>
     * </ol>
     *
     * <p>The method also returns points that are off the screen. Callers clamp them to the edge
     * when they need to.
     *
     * @return {x, y} in GUI pixels, or null when the point is behind the camera or when no capture
     *         was done.
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

    /** Same as {@link #project(double, double, double)}. */
    @Nullable
    public static float[] project(Vec3 p) {
        return project(p.x, p.y, p.z);
    }

    /**
     * Screen-space bearing of a world point around the screen centre. The edge threat markers use
     * it.
     *
     * <p>The method uses view space only (no projection). The bearing is
     * {@code atan2(right, up)}. 0 = up. Positive values go clockwise on the screen.
     *
     * <p>Behind handling: for a point behind the camera, the method sets {@code up} to
     * {@code -|up| - 0.25 * |forward|}. Thus the bearing is always in the bottom half of the
     * screen, and the left or right side stays correct. A point far behind the camera moves to the
     * bottom centre.
     *
     * <p>The method does not test {@code valid}. Before the first capture, the result is the
     * bearing for an identity camera at the world origin.
     *
     * @return the bearing in radians, -pi..pi.
     */
    public static float bearing(double x, double y, double z) {
        TMP.set((float) (x - camera.x), (float) (y - camera.y), (float) (z - camera.z), 1.0F);
        VIEW.transform(TMP);
        // view space: +x right, +y up, -z forward
        float right = TMP.x, up = TMP.y, forward = -TMP.z;
        if (forward < 0) up = -Math.abs(up) - Math.abs(forward) * 0.25F;   // behind: show in the bottom half
        return (float) Math.atan2(right, up);
    }
}
