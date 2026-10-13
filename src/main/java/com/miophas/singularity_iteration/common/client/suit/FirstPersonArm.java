// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.client.suit;

/**
 * Marks the player model set-up for the first-person arm (0.1.8.3, render thread only).
 *
 * <p>The first-person arm reuses the shared third-person player model, posed with zero motion.
 * Pose transforms meant for the third-person body (the Special Maneuver flight pose, the gun
 * carry and aim poses) must not run there: they turned and twisted the arm the player sees.
 * They check {@link #active()}.
 *
 * <p>Two sources set the mark: {@link SuitHandRenderer} around its own set-up, and the
 * {@code setupAnim} hook for vanilla's {@code PlayerRenderer.renderHand}, which calls
 * {@code setupAnim(player, 0, 0, 0, 0, 0)}. A third-person frame never passes all zeros, because
 * the age argument is the entity age plus the partial tick.
 */
public final class FirstPersonArm {
    private FirstPersonArm() { }

    private static int depth;

    public static boolean active() {
        return depth > 0;
    }

    /** Runs {@code setup} with the mark set. */
    public static void run(Runnable setup) {
        depth++;
        try {
            setup.run();
        } finally {
            depth--;
        }
    }

    /** True when the {@code setupAnim} arguments are those of the vanilla first-person arm. */
    public static boolean isHandSetup(float limbSwing, float limbSwingAmount, float age, float headYaw, float headPitch) {
        return limbSwing == 0.0F && limbSwingAmount == 0.0F && age == 0.0F && headYaw == 0.0F && headPitch == 0.0F;
    }

    /** Hook entry for the vanilla hand set-up (see the class comment). */
    public static void enter() { depth++; }

    public static void exit() { if (depth > 0) depth--; }
}
