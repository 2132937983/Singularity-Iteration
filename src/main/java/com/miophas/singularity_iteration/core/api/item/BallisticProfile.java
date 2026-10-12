// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.core.api.item;

/**
 * Launch profile of a ranged weapon for the ballistic calculator (0.1.8.0, public API).
 *
 * <p>The calculator steps the projectile one game tick at a time: move ({@code pos += vel}),
 * drag ({@code vel *= drag}, or {@code waterDrag} in a fluid source block), gravity
 * ({@code vel.y -= gravity}). Vanilla arrows and thrown projectiles use this order.
 *
 * @param name      short HUD label, for example "BOW" (at most about 8 characters)
 * @param speed     launch speed along the view axis, blocks per tick
 * @param gravity   downward acceleration, blocks per tick per tick (0 = no gravity)
 * @param drag      velocity multiplier per tick in air (1 = no drag; arrows 0.99)
 * @param waterDrag velocity multiplier per tick in a fluid source block (arrows 0.6)
 * @param ready     true when a shot released now fires at {@code speed} (fully drawn, loaded)
 */
public record BallisticProfile(String name, double speed, double gravity, double drag, double waterDrag, boolean ready) {
    /** Vanilla arrow physics (gravity 0.05, drag 0.99, water drag 0.6) at {@code speed}. */
    public static BallisticProfile arrow(String name, double speed, boolean ready) {
        return new BallisticProfile(name, speed, 0.05, 0.99, 0.6, ready);
    }

    /** Vanilla thrown projectile physics (gravity 0.03, drag 0.99, water drag 0.8) at {@code speed}. */
    public static BallisticProfile thrown(String name, double speed, boolean ready) {
        return new BallisticProfile(name, speed, 0.03, 0.99, 0.8, ready);
    }

    /** No gravity (energy bolts, no-gravity arrows), with the given drag. */
    public static BallisticProfile straight(String name, double speed, double drag, double waterDrag, boolean ready) {
        return new BallisticProfile(name, speed, 0.0, drag, waterDrag, ready);
    }

    /** Vanilla bow draw power for {@code useTicks} of draw with a full draw at {@code fullDrawTicks} (0..1). */
    public static double drawPower(int useTicks, int fullDrawTicks) {
        double f = Math.max(0, useTicks) / (double) Math.max(1, fullDrawTicks);
        f = (f * f + f * 2.0) / 3.0;
        return Math.min(1.0, f);
    }
}
