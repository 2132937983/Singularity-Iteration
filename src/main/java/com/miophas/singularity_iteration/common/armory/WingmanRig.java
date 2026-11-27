// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.armory;

import org.joml.Matrix3f;
import org.joml.Vector3f;

/**
 * 0.1.11.225 (Wingman motion A0): the keyframe rig of batch AN1 ({@code docs/art/an1/spec.json}) flattened onto the six
 * parts of the vanilla humanoid model, pure math so that tests run it offline.
 *
 * <p>The rig has seven bones: {@code root} (at the feet) carries {@code body} and both legs; {@code body} carries the head
 * and both arms. The vanilla model has six parts side by side (head, body, arms, legs, each turned about its own pivot), and
 * every worn armour, every layer and SlashBlade draw on those. So the animation is not drawn by GeckoLib: each frame the
 * bones' local turns and offsets ({@link #flatten}) are chained down the hierarchy, and every part gets the place and the
 * turn its bone ends up with. A body that bends carries the head and the arms along; a root that rises lifts everything.
 *
 * <p>Units: local turns in radians in the vanilla sense (a part turned by {@code xRot < 0} swings forward, the head turned
 * by {@code yRot < 0} looks to its own left), offsets in model pixels in the vanilla sense (y down). The rig's authored
 * values are in these senses (the AN1 calibration pose: right arm x -90 is held forward); {@link #SIGN} maps an authored
 * turn to a vanilla one per axis, checked in game with {@code animation.wingman.calibrate}.
 */
public final class WingmanRig {
    private WingmanRig() {}

    /** Bones, in the order of the arrays of {@link #flatten}. */
    public static final int ROOT = 0, BODY = 1, HEAD = 2, RIGHT_ARM = 3, LEFT_ARM = 4, RIGHT_LEG = 5, LEFT_LEG = 6, BONES = 7;
    public static final String[] NAMES = {"root", "body", "head", "right_arm", "left_arm", "right_leg", "left_leg"};
    /** Parent of each bone (-1: none). */
    static final int[] PARENT = {-1, ROOT, BODY, BODY, BODY, ROOT, ROOT};
    /**
     * Pivot of each bone in vanilla model pixels (y down, 0 at the neck, 24 at the feet): the authored pivots of the spec
     * ({@code root} 0,0,0; {@code body} and {@code head} 0,24,0; arms -+5,22,0; legs -+1.9,12,0, y up from the feet).
     */
    static final float[][] PIVOT = {{0, 24, 0}, {0, 0, 0}, {0, 0, 0}, {-5, 2, 0}, {5, 2, 0}, {-1.9F, 12, 0}, {1.9F, 12, 0}};
    /** Authored turn to vanilla turn, per axis (x, y, z). */
    public static final float[] SIGN = {1, 1, 1};

    /** The pose of one vanilla part: its pivot place (model pixels) and its turn (radians, applied z, then y, then x). */
    public record Part(float x, float y, float z, float xRot, float yRot, float zRot) { }

    /**
     * The world pose of every bone. {@code rot[b]} = local turn (x, y, z; radians, vanilla sense); {@code pos[b]} = local offset
     * (x, y, z; model pixels, vanilla sense). Returns, per bone, the place of its pivot and its turn; index {@link #ROOT}
     * is the whole model (the root's place relative to the feet and its turn).
     */
    public static Part[] flatten(float[][] rot, float[][] pos) {
        Matrix3f[] r = new Matrix3f[BONES];
        Vector3f[] at = new Vector3f[BONES];
        Part[] out = new Part[BONES];
        for (int b = 0; b < BONES; b++) {
            Matrix3f local = new Matrix3f().rotationZYX(rot[b][2], rot[b][1], rot[b][0]);
            Vector3f offset = new Vector3f(pos[b][0], pos[b][1], pos[b][2]);
            int p = PARENT[b];
            if (p < 0) {
                r[b] = local;
                at[b] = new Vector3f(PIVOT[b][0], PIVOT[b][1], PIVOT[b][2]).add(offset);
            } else {
                // the bone's pivot, seen from its parent's pivot, turns and moves with the parent
                Vector3f rel = new Vector3f(PIVOT[b][0] - PIVOT[p][0], PIVOT[b][1] - PIVOT[p][1], PIVOT[b][2] - PIVOT[p][2]).add(offset);
                at[b] = r[p].transform(rel, new Vector3f()).add(at[p]);
                r[b] = new Matrix3f(r[p]).mul(local);
            }
            Vector3f e = eulerZYX(r[b]);
            out[b] = new Part(at[b].x, at[b].y, at[b].z, e.x, e.y, e.z);
        }
        return out;
    }

    /**
     * The turns (x, y, z) with {@code R = Rz(z) Ry(y) Rx(x)} (the vanilla part order), for a rotation matrix. Near the
     * gimbal lock (y at +-90 degrees) x takes the whole turn about the remaining axis.
     */
    static Vector3f eulerZYX(Matrix3f m) {
        // R = Rz Ry Rx: m20 = -sin(y), m21 = cos(y) sin(x), m22 = cos(y) cos(x), m10 = sin(z) cos(y), m00 = cos(z) cos(y)
        float sy = -m.m02;   // joml: mCR is column C, row R; row 2 column 0 is m02
        sy = Math.max(-1, Math.min(1, sy));
        float y = (float) Math.asin(sy);
        float x, z;
        if (Math.abs(sy) < 0.9999F) {
            x = (float) Math.atan2(m.m12, m.m22);
            z = (float) Math.atan2(m.m01, m.m00);
        } else {
            x = (float) Math.atan2(-m.m21, m.m11);
            z = 0;
        }
        return new Vector3f(x, y, z);
    }

    /**
     * Converts authored values (degrees, rig pixels with y up) into the arrays of {@link #flatten}. {@code deg[b]} and
     * {@code px[b]} are x, y, z of bone b.
     */
    public static float[][][] fromAuthored(float[][] deg, float[][] px) {
        float[][] rot = new float[BONES][3], pos = new float[BONES][3];
        for (int b = 0; b < BONES; b++) {
            for (int a = 0; a < 3; a++) rot[b][a] = (float) Math.toRadians(deg[b][a]) * SIGN[a];
            pos[b][0] = px[b][0];
            pos[b][1] = -px[b][1];
            pos[b][2] = px[b][2];
        }
        return new float[][][]{rot, pos};
    }
}
