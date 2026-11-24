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

    /**
     * What the main hand holds, for the pose. 0.1.11.46 (motion audit M2): a bow, a crossbow and a trident have poses of
     * their own (draw, hold and reload, throw); RANGED is the two-handed aim of the energy guns and other ranged weapons.
     */
    public enum Hold { EMPTY, HELD, RANGED, BLADE, BOW, CROSSBOW, SPEAR;
        /** True for every ranged kind. */
        public boolean ranged() { return this == RANGED || this == BOW || this == CROSSBOW || this == SPEAR; }
    }

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
        if (!ranged(stack)) return Hold.HELD;
        var item = stack.getItem();
        if (item instanceof com.miophas.singularity_iteration.core.api.item.IWeaponItem) return Hold.RANGED;
        if (item instanceof net.minecraft.world.item.TridentItem) return Hold.SPEAR;
        if (item instanceof net.minecraft.world.item.CrossbowItem) return Hold.CROSSBOW;
        return Hold.BOW;   // bows and other projectile weapons
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
        return base(hold, leftHanded, flight, air, combat, sway, headPitch, headYaw, lean, 0, 0);
    }

    /**
     * 0.1.11.46 (M2): the target pose with the melee wind-up ({@code windup} 0..1: the weapon raised back as the target
     * comes near) and the shield ({@code shield} 0..1: the off arm raises a held shield; escort).
     */
    public static float[] base(Hold hold, boolean leftHanded, float flight, float air, float combat, float sway,
                               float headPitch, float headYaw, float lean, float windup, float shield) {
        float f = clamp01(flight), a = clamp01(air), c = clamp01(combat);
        float hp = Mth.clamp(finite(headPitch), -1.2F, 1.2F), hy = Mth.clamp(finite(headYaw), -0.9F, 0.9F);
        float tilt = Mth.clamp(finite(lean), 0, 1.2F);
        float sw = Mth.clamp(finite(sway), -0.2F, 0.2F);
        float[] main = new float[3], off = new float[3];
        free(main, f, a, sw);
        free(off, f, a, -sw);
        float aimYaw = 0;
        float aimX = -Mth.HALF_PI + hp - tilt;
        switch (hold) {
            case HELD -> {
                held(main, f, c);
                // 0.1.11.46: the weapon goes up and back as the target comes near (the wind-up of the cut)
                blend(main, c * clamp01(windup), -2.45F, 0.30F, 0.12F);
            }
            case RANGED -> {
                held(main, f, 0);
                blend(main, c, aimX, -0.10F, 0);
                blend(off, c, aimX, 0.50F, 0);
                aimYaw = c * hy;   // the head yaw turns both arms the same way, whatever the hand
            }
            case BOW -> {          // 0.1.11.46: the bow arm straight at the target, the string hand beside it (the draw is added)
                held(main, f, 0);
                blend(main, c, aimX, -0.08F, 0);
                blend(off, c, aimX, 0.45F, 0);
                aimYaw = c * hy;
            }
            case CROSSBOW -> {     // 0.1.11.46: the vanilla crossbow hold (the reload is added)
                held(main, f, 0);
                blend(main, c, aimX + 0.10F, -0.30F, 0);
                blend(off, c, aimX + 0.07F, 0.60F, 0);
                aimYaw = c * hy;
            }
            case SPEAR -> {        // 0.1.11.46: the trident raised back over the shoulder (the throw is added)
                held(main, f, 0);
                blend(main, c, -Mth.PI + 0.35F + hp * 0.5F, -0.15F, 0.05F);
                blend(off, c, -0.55F, 0.35F, -0.10F);
                aimYaw = c * hy * 0.5F;
            }
            case BLADE -> {   // the still ready pose of 0.1.10.11 (the blade is drawn by SlashBlade)
                main[0] = -0.42F; main[1] = 0.32F; main[2] = 0.04F;
                off[0] = -0.12F; off[1] = 0.0F; off[2] = -0.10F;
            }
            default -> { }
        }
        // 0.1.11.46: a shield in the off hand is raised in front (the vanilla block pose)
        float sh = clamp01(shield) * c;
        if (sh > 0) blend(off, sh, -0.95F + hp * 0.5F, 0.52F, 0);
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

    // ------------------------------------------------------------------ attitude (0.1.11.44 M0 lean, 0.1.11.45 M1 flight pose)
    //
    // The suit flies like a small jet (user decision two A): it tilts along its flight (forward, back, to the side), banks
    // into turns, raises its head in a climb and dips it in a dive, flares back when it brakes, and tucks its legs in a
    // thruster boost. All terms come from smoothed per-tick states, so they are continuous in time.

    /** Forward lean at full speed, degrees (0.1.11.45: 45, was 62: the suit no longer lies flat like a board). */
    public static final float MAX_LEAN = 45.0F;
    /** Lean backwards (flying backwards) at full speed: a fraction of {@link #MAX_LEAN}. */
    public static final float BACK_LEAN = 0.5F;
    /** Side lean (sliding sideways) at full speed: a fraction of {@link #MAX_LEAN}. */
    public static final float SIDE_LEAN = 0.6F;
    /** Speed (blocks per tick) of a full lean: the flight factor of 0.1.10.11 ({@code speed * 1.6}, at most 1). */
    public static final float FULL_SPEED = 1 / 1.6F;
    /** Bank at a full turn ({@link #FULL_TURN} degrees per tick) at full speed, degrees. */
    public static final float BANK = 28.0F, FULL_TURN = 15.0F;
    /** Head up in a full climb, down in a full dive ({@link #FULL_CLIMB} blocks per tick), degrees. */
    public static final float CLIMB = 16.0F, FULL_CLIMB = 0.4F;
    /** Flare back at a full brake ({@link #FULL_BRAKE} blocks per tick of lost forward speed per tick), degrees. */
    public static final float BRAKE = 20.0F, FULL_BRAKE = 0.12F;
    /** Bounds of the pitch and the roll, degrees. */
    public static final float MIN_PITCH = -32.0F, MAX_PITCH = 55.0F, MAX_ROLL = 42.0F;

    /** 0.1.11.44: the tilt from the flight direction alone (no turn, no climb, no brake); see {@link #attitude}. */
    public static float[] lean(float forward, float right, float air) {
        return attitude(forward, right, 0, 0, 0, air);
    }

    /**
     * The tilt of the suit, degrees: {pitch (positive: head forward), roll (positive: head to the right)}.
     *
     * @param forward smoothed velocity ahead in the body frame, blocks per tick
     * @param right smoothed velocity to the right, blocks per tick
     * @param up smoothed vertical velocity, blocks per tick
     * @param turn smoothed turn rate of the body, degrees per tick (positive: to the right)
     * @param brake smoothed loss of forward speed, blocks per tick per tick (positive: slowing down)
     * @param air 1 in the air, 0 on the ground
     */
    public static float[] attitude(float forward, float right, float up, float turn, float brake, float air) {
        float a = clamp01(air);
        float f = Mth.clamp(finite(forward) / FULL_SPEED, -1, 1), r = Mth.clamp(finite(right) / FULL_SPEED, -1, 1);
        float speed = Math.min(1, Mth.sqrt(f * f + r * r));
        float pitch = (f >= 0 ? f : f * BACK_LEAN) * MAX_LEAN;
        pitch -= Mth.clamp(finite(up) / FULL_CLIMB, -1, 1) * CLIMB;                          // climb: head up; dive: head down
        if (f > 0) pitch -= Mth.clamp(finite(brake) / FULL_BRAKE, 0, 1) * BRAKE * f;      // braking: a flare back
        float roll = r * SIDE_LEAN * MAX_LEAN + Mth.clamp(finite(turn) / FULL_TURN, -1, 1) * BANK * speed;   // slide and bank
        pitch = Mth.clamp(pitch, MIN_PITCH, MAX_PITCH) * a;
        roll = Mth.clamp(roll, -MAX_ROLL, MAX_ROLL) * a;
        return new float[]{pitch, roll};
    }

    // ---- the high-speed pose (0.1.11.51, escort and open Armory phase F2, decision four A)
    //
    // Above PRONE_START blocks per tick ahead the suit lies down into the flight like its owner in maneuver flight or on
    // an elytra: the pitch goes to PRONE_PITCH, the bank softens, the arms lie along the body and the legs stretch out.
    // The weight is smoothed by the entity, so the change is gradual; a fight (combat) keeps the arms free.

    /** Pitch of the full high-speed pose, degrees. */
    public static final float PRONE_PITCH = 80.0F;
    /** Forward speeds (blocks per tick) where the high-speed pose starts and is full. */
    public static final float PRONE_START = 1.5F, PRONE_FULL = 2.5F;

    /** Target weight 0..1 of the high-speed pose for a forward speed (blocks per tick); none on the ground. */
    public static float proneTarget(float forward, float air) {
        return Mth.clamp((finite(forward) - PRONE_START) / (PRONE_FULL - PRONE_START), 0, 1) * clamp01(air);
    }

    /** The attitude {pitch, roll} with the high-speed pose of weight {@code prone} (0..1). */
    public static float[] prone(float[] attitude, float prone) {
        float p = clamp01(prone);
        return new float[]{Mth.lerp(p, attitude[0], PRONE_PITCH), attitude[1] * (1 - 0.5F * p)};
    }

    /** Lays both arms along the body (weight {@code prone} x (1 - combat)): a slight spread, the weapon trailing. */
    public static void proneArms(float[] pose, float prone, float combat) {
        float p = clamp01(prone) * (1 - clamp01(combat));
        if (p <= 0) return;
        pose[RX] = Mth.lerp(p, pose[RX], 0.12F);
        pose[RY] = Mth.lerp(p, pose[RY], 0.0F);
        pose[RZ] = Mth.lerp(p, pose[RZ], 0.16F);
        pose[LX] = Mth.lerp(p, pose[LX], 0.12F);
        pose[LY] = Mth.lerp(p, pose[LY], 0.0F);
        pose[LZ] = Mth.lerp(p, pose[LZ], -0.16F);
    }

    /** Stretches the legs (see {@link #legs}) out behind the body with the high-speed pose of weight {@code prone}. */
    public static float[] proneLegs(float[] legs, float prone) {
        float p = clamp01(prone);
        float[] out = legs.clone();
        out[LEG_RX] = Mth.lerp(p, legs[LEG_RX], 0.04F);
        out[LEG_LX] = Mth.lerp(p, legs[LEG_LX], -0.02F);
        out[LEG_RZ] = Mth.lerp(p, legs[LEG_RZ], 0.05F);
        out[LEG_LZ] = Mth.lerp(p, legs[LEG_LZ], -0.05F);
        return out;
    }

    // ---- constructivist flight effects (0.1.11.62, hit feel and constructivist FX phase Q1): the timing, pure

    /** The two-state beat of a thrust wedge: full on even pairs of ticks, {@link #BEAT_LOW} on odd ones (no noise). */
    public static final float BEAT_LOW = 0.82F;

    /** Length (blocks) of a boot thrust wedge at {@code speed} (blocks per tick) and the high-speed pose weight. */
    public static float thrustLength(float speed, float prone, int tick) {
        float base = 0.35F + Mth.clamp(finite(speed), 0, 4) * 0.45F + clamp01(prone) * 0.6F;
        return base * (((tick >> 1) & 1) == 0 ? 1 : BEAT_LOW);
    }

    /** Opacity of the speed bars: none below 0.9 blocks per tick, full from 1.5. */
    public static float speedBarAlpha(float speed) {
        return Mth.clamp((finite(speed) - 0.9F) / 0.6F, 0, 1);
    }

    /** Ticks a boost ring and wedge stand before the hard cut, and the ring radius at {@code age} ticks (0: gone). */
    public static final int BOOST_CUT = 6;

    public static float boostRing(float age) {
        return age < 0 || age >= BOOST_CUT ? 0 : 0.45F + age * 0.4F;
    }

    /**
     * 0.1.11.65 (Q4): the engine loop of a Wingman at {@code speed} (blocks per tick), {@code sinceBoost} ticks after a boost,
     * landed or not: {volume, pitch}. A low drone in the hover, louder and higher with speed, a kick after a boost; silent on
     * the ground.
     */
    public static float[] engineMix(float speed, int sinceBoost, boolean landed) {
        if (landed) return new float[]{0, 0.8F};
        float s = Mth.clamp(finite(speed) / 2F, 0, 1);
        float kick = sinceBoost >= 0 && sinceBoost < 10 ? (1 - sinceBoost / 10F) * 0.35F : 0;
        return new float[]{Math.min(1F, 0.16F + 0.5F * s + kick), 0.82F + 0.5F * s + kick * 0.6F};
    }

    // ---- battle damage (0.1.11.58, gaze and Iron Man phase P6)

    /** Durability fraction below which a piece counts as badly damaged (sparks start at 2 x this). */
    public static final float DAMAGED_FRACTION = 0.25F;

    /** 0 sound, 1 sparks (worst piece under 50 %), 2 sparks and smoke and an unsteady flight (under 25 %). */
    public static int damageLevel(float worstFraction) {
        if (!Float.isFinite(worstFraction) || worstFraction >= DAMAGED_FRACTION * 2) return 0;
        return worstFraction < DAMAGED_FRACTION ? 2 : 1;
    }

    /** The unsteady flight of a badly damaged suit: {pitch, roll} degrees, smooth in time, within 3.5 degrees. */
    public static float[] damageShake(float time, int level, float air) {
        if (level < 2) return new float[]{0, 0};
        float a = clamp01(air), t = finite(time);
        float pitch = (Mth.sin(t * 0.73F) * 1.6F + Mth.sin(t * 1.91F + 0.4F) * 0.9F) * a;
        float roll = (Mth.sin(t * 0.57F + 1.1F) * 2.0F + Mth.sin(t * 2.37F) * 1.2F) * a;
        return new float[]{pitch, roll};
    }

    /** Leg angles: [right x, y, z, left x, y, z]. */
    public static final int LEG_RX = 0, LEG_RZ = 2, LEG_LX = 3, LEG_LZ = 5;
    /** Ticks of the leg tuck of a thruster boost. */
    public static final int TUCK_TICKS = 12;

    /**
     * 0.1.11.45: the legs. In flight they trail a little behind (more with speed), spread with a slide or a bank, and swing
     * slowly in the hover; a thruster boost tucks both thighs forward and lets them out again ({@code boostAge} ticks since
     * the boost, partial; out of {@code [0, TUCK_TICKS]}: none). On the ground they stand straight.
     *
     * @param flight 0..1, speed; @param air 1 in the air; @param sway the hover sway; @param roll the roll, degrees
     */
    public static float[] legs(float flight, float air, float sway, float roll, float boostAge) {
        float a = clamp01(air), f = clamp01(flight), sw = Mth.clamp(finite(sway), -0.2F, 0.2F);
        float t = finite(boostAge);
        float tuck = t >= 0 && t <= TUCK_TICKS ? Mth.sin(t / TUCK_TICKS * Mth.PI) : 0;
        tuck = tuck * tuck * (3 - 2 * tuck);
        float trail = Mth.lerp(f, 0, 0.22F);
        float spread = 0.03F + Math.abs(Mth.clamp(finite(roll), -MAX_ROLL, MAX_ROLL)) / MAX_ROLL * 0.10F;
        float rx = (trail + sw - tuck * 0.95F) * a, lx = (trail - sw * 0.7F - tuck * 0.80F) * a;
        float rz = spread * a + 0.01F, lz = -rz;
        return new float[]{rx, 0, rz, lx, 0, lz};
    }

    /** {forward, right} components (blocks per tick) of the horizontal motion {@code dx, dz} for a body yaw in degrees. */
    public static float[] bodyFrame(double dx, double dz, float bodyYawDegrees) {
        double yaw = Math.toRadians(bodyYawDegrees);
        double fx = -Math.sin(yaw), fz = Math.cos(yaw);       // forward
        double rx = -Math.cos(yaw), rz = -Math.sin(yaw);      // right
        return new float[]{(float) (dx * fx + dz * fz), (float) (dx * rx + dz * rz)};
    }

    // ------------------------------------------------------------------ idle and landing (0.1.11.47, motion audit M4)

    /** Ticks of the crouch at a touchdown and of the push at a take-off. */
    public static final int CROUCH_TICKS = 8, PUSH_TICKS = 6;

    /** A bell 0 -> 1 -> 0 over {@code length} ticks, peaking at {@code peak} (0..1 of it); 0 outside. */
    static float bell(float t, float length, float peak) {
        float s = finite(t) / length;
        if (s <= 0 || s >= 1) return 0;
        return s < peak ? ease(s / peak) : 1 - ease((s - peak) / (1 - peak));
    }

    /** The cushion of a touchdown: 0..1, {@code t} ticks after it (a quick dip, a slower rise). */
    public static float landingCrouch(float t) { return bell(t, CROUCH_TICKS, 0.3F); }

    /** The push-off of a take-off: 0..1, {@code t} ticks after it. */
    public static float takeoffPush(float t) { return bell(t, PUSH_TICKS, 0.35F); }

    /** The slow float of the hover, blocks (the suit drifts up and down a few centimetres in the air). */
    public static float hoverBob(float age, float air, float flight) {
        return Mth.sin(finite(age) * 0.11F) * 0.035F * clamp01(air) * (1 - clamp01(flight));
    }

    /**
     * The look-around of an idle Wingman: a head yaw offset, radians, that drifts slowly from side to side (two slow
     * waves), only when nothing is going on ({@code idle} 1: no target, no speed).
     */
    public static float idleLook(float age, float idle) {
        float a = finite(age);
        float look = Mth.sin(a * 0.021F) * 0.45F + Mth.sin(a * 0.053F + 1.3F) * 0.15F;
        return look * clamp01(idle);
    }

    // ------------------------------------------------------------------ weapon motions (0.1.11.46, motion audit M2)

    /**
     * The cut of a melee hit from the wind-up: a fast strike down and across (to 35 % of the swing), a held
     * follow-through low across the body (to 70 %), then back to the guard. Zero at 0 and at 1. Returns the body twist.
     * {@code a}: 0..1 of the swing (the vanilla swing time); {@code windup} as in {@link #base}.
     */
    /** The arm angle at the end of the strike (forward and low). */
    public static final float STRIKE_X = -0.15F;

    public static float cut(float[] pose, boolean leftHanded, float a, float windup) {
        a = clamp01(a);
        if (a <= 0 || a >= 1) return 0;
        float w = 0.55F + 0.45F * clamp01(windup);       // a cut from a full wind-up travels further
        float k = a < 0.35F ? ease(a / 0.35F) : a < 0.70F ? 1 : 1 - ease((a - 0.70F) / 0.30F);
        int x = leftHanded ? LX : RX;
        float side = leftHanded ? -1 : 1;
        pose[x] += (STRIKE_X - pose[x]) * k;              // down from wherever the arm is to the strike, never past it
        pose[x + 1] += side * -0.95F * w * k;             // across the body
        pose[x + 2] += side * -0.20F * k;
        float twist = side * 0.38F * k;                   // the body turns into the cut
        pose[RY] += twist;
        pose[LY] += twist;
        return twist;
    }

    /**
     * Alternating 3-hit melee combo with off-hand counter-balance and heavy-weapon slam scaling:
     * <ul>
     *   <li>{@code combo == 0}: forehand high-to-low diagonal slash</li>
     *   <li>{@code combo == 1}: reverse backhand horizontal sweep</li>
     *   <li>{@code combo == 2}: overhead cleave (amplified for axes/maces)</li>
     * </ul>
     * Zero at {@code a <= 0} and {@code a >= 1}. Returns torso twist (radians).
     */
    public static float cutCombo(float[] pose, boolean leftHanded, float a, float windup, int combo, boolean heavy) {
        a = clamp01(a);
        if (a <= 0 || a >= 1) return 0;
        int step = Math.floorMod(combo, 3);
        float w = (0.60F + 0.40F * clamp01(windup)) * (heavy ? 1.18F : 1.0F);
        int x = leftHanded ? LX : RX;
        int o = leftHanded ? RX : LX;
        float side = leftHanded ? -1 : 1;
        // Fast strike (0..0.32), brief impact hold (0.32..0.64), smooth recovery (0.64..1.0)
        float strikeEnd = heavy ? 0.38F : 0.30F;
        float holdEnd = heavy ? 0.68F : 0.62F;
        float k = a < strikeEnd ? ease(a / strikeEnd) : a < holdEnd ? 1F : 1F - ease((a - holdEnd) / (1F - holdEnd));
        float twist;
        switch (step) {
            case 1 -> {   // backhand reverse sweep (inside-out)
                pose[x] += (-0.75F - pose[x]) * k;
                pose[x + 1] += side * 1.05F * w * k;
                pose[x + 2] += side * 0.28F * k;
                pose[o] += 0.24F * k;
                pose[o + 2] += side * 0.16F * k;
                twist = side * -0.40F * k;
            }
            case 2 -> {   // heavy overhead cleave
                float cleaveTarget = heavy ? 0.22F : 0.05F;
                pose[x] += (cleaveTarget - pose[x]) * k;
                pose[x + 1] += side * -0.45F * w * k;
                pose[x + 2] += side * -0.14F * k;
                pose[o] += -0.45F * k;   // off-hand flares back for balance
                pose[o + 1] += side * -0.25F * k;
                twist = side * 0.46F * k;
            }
            default -> {  // forehand diagonal slash
                pose[x] += (STRIKE_X - pose[x]) * k;
                pose[x + 1] += side * -1.02F * w * k;
                pose[x + 2] += side * -0.24F * k;
                pose[o] += 0.26F * k;
                pose[o + 2] += side * -0.14F * k;
                twist = side * 0.42F * k;
            }
        }
        pose[RY] += twist;
        pose[LY] += twist;
        return twist;
    }

    /** Crisp per-shot muzzle kick over 0..3.5 ticks so rapid-fire weapons show distinct recoil per round. */
    public static void shotKick(float[] pose, boolean leftHanded, float shotAge) {
        float t = finite(shotAge);
        if (t <= 0 || t >= 3.5F) return;
        float k = t < 0.8F ? ease(t / 0.8F) : 1F - ease((t - 0.8F) / 2.7F);
        int main = leftHanded ? LX : RX;
        int off = leftHanded ? RX : LX;
        pose[main] -= 0.26F * k;
        pose[off] -= 0.15F * k;
    }

    /**
     * The draw of a bow: the string hand comes back toward the chest as {@code draw} goes from 0 (just shot) to 1 (full
     * draw); the release is the drop of {@code draw} at the shot.
     */
    public static void draw(float[] pose, boolean leftHanded, float draw, float combat) {
        float d = clamp01(draw) * clamp01(combat);
        int x = leftHanded ? LX : RX;
        float side = leftHanded ? -1 : 1;
        pose[x] += 0.28F * d;
        pose[x + 1] += side * 0.55F * d;
    }

    /** The reload of a crossbow after a shot: {@code reload} 0..1 of the cooldown; the arms pull the string, then hold. */
    public static void reload(float[] pose, boolean leftHanded, float reload, float combat) {
        float r = clamp01(reload);
        float k = (r < 0.15F ? ease(r / 0.15F) : r < 0.65F ? 1 : 1 - ease((r - 0.65F) / 0.35F)) * clamp01(combat);
        int x = leftHanded ? LX : RX, o = leftHanded ? RX : LX;
        float side = leftHanded ? -1 : 1;
        pose[x] += 0.55F * k;
        pose[x + 1] += side * -0.50F * k;
        pose[o] += 0.50F * k;
        pose[o + 1] += side * -0.25F * k;
    }

    /** The throw of a trident: {@code t} ticks since the throw (partial); the arm whips forward and comes back up. */
    public static void spearThrow(float[] pose, boolean leftHanded, float t, float combat) {
        float s = finite(t);
        if (s < 0 || s > 10) return;
        float k = (s < 2 ? ease(s / 2) : 1 - ease((s - 2) / 8)) * clamp01(combat);
        int x = leftHanded ? LX : RX;
        pose[x] += 2.3F * k;
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
