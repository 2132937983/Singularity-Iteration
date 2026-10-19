// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.armory;

import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;

/**
 * 0.1.10.13: the arm pose of a Wingman, as pure math (no client classes), so it can be checked
 * outside the game. {@code WingmanRenderer} applies it to the model.
 *
 * <h2>Why</h2>
 * Up to 0.1.10.12 the arms of a Wingman that held anything but a SlashBlade blade jittered:
 * <ul>
 *   <li>The flight pose was replaced by the vanilla pose for the whole length of every swing
 *       ({@code attackTime > 0}), so both arms jumped at the start and at the end of each hit, and a
 *       gun (one swing per shot, every 2 to 4 ticks) switched between the two poses all the time.</li>
 *   <li>The vanilla pose contains the walking swing of the arms, driven by the flight speed: during a
 *       hit-and-run pass the arms flapped with a period of about 9 ticks.</li>
 *   <li>A swing that was still running was restarted (vanilla restarts after half the swing).</li>
 * </ul>
 *
 * <h2>Model</h2>
 * <ul>
 *   <li>{@link #base}: the target pose of both arms from smoothed, per-tick states only (flight,
 *       landing, combat, the held item's {@link Hold} kind, the head angles). No walking swing.</li>
 *   <li>{@link #smooth}: a fail-safe low-pass in game time (time constant {@link #SMOOTH_TICKS},
 *       at most {@link #MAX_RATE} radians per tick) on that target, so no state change (item switch,
 *       combat start, landing) can make the arms jump; non-finite values keep the last pose.</li>
 *   <li>Additive motions that are zero at both ends, so they start and end without a step:
 *       {@link #swing} (the cut of a melee hit), {@link #recoil} (firing), {@link #slash} (blade).</li>
 * </ul>
 * Angles: radians, model space, [right arm x, y, z, left arm x, y, z]; the main arm is the right arm
 * unless the Wingman is left-handed (then the pose is mirrored).
 */
public final class WingmanPose {
    private WingmanPose() { }

    /** What the main hand holds, for the pose. */
    public enum Hold { EMPTY, HELD, RANGED, BLADE }

    public static final int RX = 0, RY = 1, RZ = 2, LX = 3, LY = 4, LZ = 5;
    /** Time constant of the fail-safe smoothing, ticks. */
    public static final float SMOOTH_TICKS = 1.6F;
    /** Fastest change of the smoothed pose, radians per tick (a new item or the aim is raised in a few ticks). */
    public static final float MAX_RATE = 0.45F;
    /** Largest angle the pose may take, radians. */
    public static final float LIMIT = Mth.PI;

    /** Ranged weapons (the same test as the Wingman's combat logic). */
    public static boolean ranged(ItemStack stack) {
        if (stack.isEmpty()) return false;
        var item = stack.getItem();
        if (item instanceof com.miophas.singularity_iteration.core.api.item.IWeaponItem gun && gun.isRanged()) return true;
        return item instanceof net.minecraft.world.item.ProjectileWeaponItem || item instanceof net.minecraft.world.item.TridentItem;
    }

    /** The {@link Hold} kind of {@code stack}; {@code blade}: the stack is a SlashBlade blade. */
    public static Hold holdOf(ItemStack stack, boolean blade) {
        if (stack.isEmpty()) return Hold.EMPTY;
        if (blade) return Hold.BLADE;
        return ranged(stack) ? Hold.RANGED : Hold.HELD;
    }

    /**
     * The target pose.
     *
     * @param flight 0 (hover or landed) .. 1 (full speed), smoothed
     * @param air 1 in the air, 0 standing on the ground, smoothed
     * @param combat 0 (no target) .. 1 (target locked), smoothed
     * @param sway the slow hover sway (continuous in time)
     * @param headPitch head pitch, radians (positive: down)
     * @param headYaw head yaw relative to the body, radians
     * @param lean forward lean of the suit, radians (the aim makes up for it)
     */
    public static float[] base(Hold hold, boolean leftHanded, float flight, float air, float combat, float sway,
                               float headPitch, float headYaw, float lean) {
        float f = clamp01(flight), a = clamp01(air), c = clamp01(combat);
        float hp = Mth.clamp(finite(headPitch), -1.2F, 1.2F), hy = Mth.clamp(finite(headYaw), -0.9F, 0.9F);
        float tilt = Mth.clamp(finite(lean), 0, 1.2F);
        float sw = Mth.clamp(finite(sway), -0.2F, 0.2F);
        float[] main = new float[3], off = new float[3];
        free(main, f, a, sw);
        free(off, f, a, -sw);
        float aimYaw = 0;
        switch (hold) {
            case HELD -> held(main, f, c);
            case RANGED -> {
                held(main, f, 0);
                float aimX = -Mth.HALF_PI + hp - tilt;
                blend(main, c, aimX, -0.10F, 0);
                blend(off, c, aimX, 0.50F, 0);
                aimYaw = c * hy;   // the head yaw turns both arms the same way, whatever the hand
            }
            case BLADE -> {   // the still ready pose of 0.1.10.11 (the blade is drawn by SlashBlade)
                main[0] = -0.42F; main[1] = 0.32F; main[2] = 0.04F;
                off[0] = -0.12F; off[1] = 0.0F; off[2] = -0.10F;
            }
            default -> { }
        }
        float[] pose = place(main, off, leftHanded);
        pose[RY] += aimYaw;
        pose[LY] += aimYaw;
        for (int i = 0; i < 6; i++) pose[i] = Mth.clamp(pose[i], -LIMIT, LIMIT);
        return pose;
    }

    /** An empty arm: trails along the body in flight, hangs by the side in the hover and on the ground. */
    private static void free(float[] arm, float f, float a, float sway) {
        arm[0] = a * Mth.lerp(f, sway, 0.45F);
        arm[1] = 0;
        arm[2] = Mth.lerp(a, 0.04F, Mth.lerp(f, 0.10F, 0.22F));
    }

    /** The weapon arm: the item held forward (as the vanilla item pose), raised to a guard in combat. */
    private static void held(float[] arm, float f, float c) {
        arm[0] = Mth.lerp(f, -0.32F, 0.15F);
        arm[1] = Mth.lerp(f, -0.05F, 0.0F);
        arm[2] = Mth.lerp(f, 0.06F, 0.16F);
        blend(arm, c, -0.85F, -0.35F, 0.02F);
    }

    private static void blend(float[] arm, float t, float x, float y, float z) {
        arm[0] = Mth.lerp(t, arm[0], x);
        arm[1] = Mth.lerp(t, arm[1], y);
        arm[2] = Mth.lerp(t, arm[2], z);
    }

    /** Main/off arm (right-arm convention) to [right, left]; the left arm mirrors y and z. */
    private static float[] place(float[] main, float[] off, boolean leftHanded) {
        float[] r = leftHanded ? off : main, l = leftHanded ? main : off;
        return new float[]{r[0], r[1], r[2], l[0], -l[1], -l[2]};
    }

    /**
     * Fail-safe smoothing of {@code target} (changed in place) toward which the pose moves; {@code state}
     * (7 floats: the pose and the time of the last update) belongs to one Wingman. A first call, or a
     * gap of more than 10 ticks, takes the target. Several calls at the same time (several render
     * passes of one frame) give the same pose. Non-finite targets keep the last pose.
     */
    public static void smooth(float[] state, float[] target, float time) {
        boolean fresh = !(state[6] > 0) || time < state[6] || time - state[6] > 10;
        for (int i = 0; i < 6; i++) {
            float t = target[i];
            if (!Float.isFinite(t)) t = fresh ? 0 : state[i];
            target[i] = Mth.clamp(t, -LIMIT, LIMIT);
        }
        if (fresh) {
            System.arraycopy(target, 0, state, 0, 6);
            state[6] = Math.max(time, Float.MIN_VALUE);
            return;
        }
        float dt = time - state[6];
        if (dt > 0) {
            float k = 1 - (float) Math.exp(-dt / SMOOTH_TICKS), step = MAX_RATE * dt;
            for (int i = 0; i < 6; i++) state[i] += Mth.clamp((target[i] - state[i]) * k, -step, step);
            state[6] = time;
        }
        System.arraycopy(state, 0, target, 0, 6);
    }

    /**
     * The cut of a melee hit (the vanilla attack curve, as an offset): {@code attack} 0..1 of the
     * swing. Zero at 0 and at 1. Returns the body twist (radians) that goes with it.
     * The vanilla twist {@code sin(sqrt(a) * 2 pi)} has an infinite slope at the start of the swing
     * (the arms turned by 0.16 rad within a hundredth of a tick, a flick); this twist has the same
     * forward-then-back shape with a finite slope.
     */
    public static float swing(float[] pose, boolean leftHanded, float attack, float headPitch) {
        float a = clamp01(attack);
        if (a <= 0 || a >= 1) return 0;
        float twist = Mth.sin(a * Mth.TWO_PI) * 0.2F;
        float lift = 1 - a;
        lift = lift * lift * lift * lift;
        float cut = Mth.sin((1 - lift) * Mth.PI) * 1.2F + Mth.sin(a * Mth.PI) * -(Mth.clamp(finite(headPitch), -1.2F, 1.2F) - 0.7F) * 0.75F;
        int x = leftHanded ? LX : RX;
        float side = leftHanded ? -1 : 1;
        float body = side * twist;
        // both arms turn with the body (as vanilla), the main arm cuts
        pose[RY] += body;
        pose[LY] += body;
        pose[LX] += body;
        pose[x] -= cut;
        pose[x + 1] += body * 2.0F;
        pose[x + 2] += side * Mth.sin(a * Mth.PI) * -0.4F;
        return body;
    }

    /** Firing: the weapon kicks up and the support arm follows. {@code r} 0..1, smoothed per tick. */
    public static void recoil(float[] pose, boolean leftHanded, float r) {
        float k = clamp01(r);
        pose[leftHanded ? LX : RX] -= 0.20F * k;
        pose[leftHanded ? RX : LX] -= 0.12F * k;
    }

    /**
     * The cut of a blade slash (0.1.10.11 key frames, as an offset from the ready pose): {@code p} 0..1
     * or below 0 for none. Zero at 0 and at 1.
     */
    public static void slash(float[] pose, int kind, float p) {
        if (p < 0 || p > 1) return;
        float rx = -0.42F, ry = 0.32F, rz = 0.04F, lx = -0.12F, ly = 0.0F, lz = -0.10F;
        float drx, dry, drz, dlx = 0, dly = 0, dlz = 0;
        switch (kind) {
            case WingmanEntity.SLASH_DASH -> {          // flat sweep, right to left
                drx = keys(p, rx, -1.45F, -1.45F, -1.30F, rx) - rx;
                dry = keys(p, ry, -1.05F, 0.95F, 0.80F, ry) - ry;
                drz = keys(p, rz, 0.10F, -0.10F, -0.05F, rz) - rz;
            }
            case WingmanEntity.SLASH_ARTS -> {          // overhead, two hands
                drx = keys(p, rx, -2.95F, -0.45F, -0.55F, rx) - rx;
                dry = keys(p, ry, -0.12F, 0.10F, 0.10F, ry) - ry;
                drz = 0;
                dlx = keys(p, lx, -2.85F, -0.50F, -0.60F, lx) - lx;
                dly = keys(p, ly, 0.30F, 0.40F, 0.35F, ly) - ly;
                dlz = keys(p, lz, 0.05F, 0.05F, 0.0F, lz) - lz;
            }
            default -> {                                // diagonal cut, high right to low left
                drx = keys(p, rx, -2.55F, -0.50F, -0.65F, rx) - rx;
                dry = keys(p, ry, -0.55F, 0.75F, 0.60F, ry) - ry;
                drz = keys(p, rz, 0.15F, -0.05F, 0.0F, rz) - rz;
            }
        }
        pose[RX] += drx; pose[RY] += dry; pose[RZ] += drz;
        pose[LX] += dlx; pose[LY] += dly; pose[LZ] += dlz;
    }

    /**
     * Four phases over 0..1: wind-up (to {@code wind} at 0.25), cut (to {@code cut} at 0.55),
     * follow-through (to {@code follow} at 0.75), back to {@code end} at 1. Smoothstep in between.
     */
    static float keys(float p, float start, float wind, float cut, float follow, float end) {
        if (p < 0.25F) return Mth.lerp(ease(p / 0.25F), start, wind);
        if (p < 0.55F) return Mth.lerp(ease((p - 0.25F) / 0.30F), wind, cut);
        if (p < 0.75F) return Mth.lerp(ease((p - 0.55F) / 0.20F), cut, follow);
        return Mth.lerp(ease((p - 0.75F) / 0.25F), follow, end);
    }

    private static float ease(float t) {
        t = Mth.clamp(t, 0, 1);
        return t * t * (3 - 2 * t);
    }

    private static float clamp01(float v) { return Float.isFinite(v) ? Mth.clamp(v, 0, 1) : 0; }

    private static float finite(float v) { return Float.isFinite(v) ? v : 0; }
}
