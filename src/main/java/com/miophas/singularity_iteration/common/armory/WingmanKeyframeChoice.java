// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.armory;

import org.jetbrains.annotations.Nullable;

/**
 * 0.1.11.225 (Wingman motion A0): which keyframe animation of batch AN1 a Wingman plays, pure so that tests can ask.
 *
 * <p>For now only a Wingman on the ground plays keyframes: it lands with {@code land_light} ({@link #LAND_TICKS} ticks), then
 * stands with {@code idle_stand}, now and then looking round ({@code idle_look}) or checking its suit ({@code idle_check}),
 * chosen per window of {@link #IDLE_WINDOW} ticks from its id so that a squad does not move as one. A hit plays one of the
 * four {@code hit_*} reactions on the upper body. In flight the code-driven poses stay ({@code null}); the walk of the cave
 * overhaul (G3) adds the rest.
 */
public final class WingmanKeyframeChoice {
    private WingmanKeyframeChoice() {}

    public static final String PREFIX = "animation.wingman.";
    /** Ticks of {@code land_light} (0.3 s). */
    public static final int LAND_TICKS = 6;
    /** Ticks per idle window. */
    public static final int IDLE_WINDOW = 120;

    /**
     * Development only: {@code -Dsi.wingman.keyframe=<short name>} makes every landed Wingman play that animation and hold its
     * last frame (the screenshot scene {@code wingman_keyframes} checks the calibration pose with it). Null in play.
     */
    @Nullable public static final String FORCED = System.getProperty("si.wingman.keyframe");

    /** The base animation (short name) of a Wingman in this state, or null for the code-driven poses. */
    @Nullable
    public static String base(boolean landed, float sinceLanding, int id, int tick) {
        if (!landed) return null;
        if (FORCED != null) return FORCED;
        if (sinceLanding < LAND_TICKS) return "land_light";
        int window = Math.floorDiv(tick + id * 37, IDLE_WINDOW);
        int roll = Math.floorMod(window * 1103515245 + id * 12345, 10);
        if (roll < 2) return "idle_look";
        if (roll == 2) return "idle_check";
        return "idle_stand";
    }

    /** 0.1.11.226 (G3): an animation and its playback speed. */
    public record Pick(String name, float speed) { }

    /** Reference speeds (blocks per tick) of {@code walk} (4.3 m/s) and {@code run} (7 m/s) in batch AN1. */
    public static final float WALK_REF = 0.215F, RUN_REF = 0.35F, SKATE_REF = 0.6F;
    /** Foot speeds (blocks per tick) above which the Wingman walks, and runs. */
    public static final float WALKS = 0.03F, RUNS = 0.27F, SKATES = 0.45F;
    /** Ticks of a hop's take-off ({@code jump_takeoff} 0.25 s, {@code thrust_hop} 0.4 s) and of a heavy landing (0.8 s). */
    public static final int JUMP_TICKS = 5, THRUST_TICKS = 8, HEAVY_TICKS = 16;

    /**
     * 0.1.11.226 (G3): the base animation of a Wingman on foot. In the air: the take-off of its last hop ({@code thrust_hop}
     * with the thrusters, else {@code jump_takeoff}), then {@code air_rise} or {@code air_fall}. On the floor: the landing
     * ({@code land_heavy} after a fall faster than the heavy touch-down speed, else {@code land_light}), then {@code skate} (the
     * thruster skate), {@code run},
     * {@code walk} (played at the speed it moves at, against the AN1 reference speeds) or the idles of {@link #base}.
     */
    public static Pick onFoot(boolean onFloor, float footSpeed, double vy, float sinceHop, boolean thrust, float sinceTouch, boolean heavy,
                              int id, int tick) {
        if (!onFloor) {
            if (sinceHop < (thrust ? THRUST_TICKS : JUMP_TICKS)) return new Pick(thrust ? "thrust_hop" : "jump_takeoff", 1);
            return new Pick(vy > 0.02 ? "air_rise" : "air_fall", 1);
        }
        if (sinceTouch < (heavy ? HEAVY_TICKS : LAND_TICKS)) return new Pick(heavy ? "land_heavy" : "land_light", 1);
        if (footSpeed > SKATES) return new Pick("skate", clamp(footSpeed / SKATE_REF, 0.7F, 1.5F));
        if (footSpeed > RUNS) return new Pick("run", clamp(footSpeed / RUN_REF, 0.6F, 1.8F));
        if (footSpeed > WALKS) return new Pick("walk", clamp(footSpeed / WALK_REF, 0.5F, 1.8F));
        String idle = base(true, LAND_TICKS, id, tick);
        return new Pick(idle == null ? "idle_stand" : idle, 1);
    }

    private static float clamp(float v, float lo, float hi) { return Math.max(lo, Math.min(hi, v)); }

    /** 0.1.11.229: an action animation and its trigger (a new trigger starts it again; 0 for a pose that just holds or loops). */
    public record Act(String name, int trigger) { }

    /** Ticks of the glare (0.3 s), of a recoil (light 0.15 s, heavy 0.35 s), of the bow's release (0.3 s) and of the trident's throw (0.6 s). */
    public static final int GLARE_TICKS = 6, RECOIL_TICKS = 3, HEAVY_RECOIL_TICKS = 7, RELEASE_TICKS = 6, THROW_TICKS = 12;
    /** Ticks before a shot at which a Wingman in cover leans out, and after it before it ducks back. */
    public static final int PEEK_BEFORE = 8, PEEK_AFTER = 4;

    /**
     * 0.1.11.229 (AN1 part 2): the action animation of a Wingman, played over its gait or its flight pose, or null.
     *
     * <ul>
     *   <li>A combat action of the server ({@link WingmanStrikes.Action}: the combo cuts, the heavy blow, the kick, the dash
     *       stab, the dive slam, the shove, the intercept, the flourish) for its length.</li>
     *   <li>Otherwise the stare at a new lock ({@code glare}).</li>
     *   <li>With a ranged weapon in a fight: the bow's release after a shot, then its draw; the crossbow's reload after a
     *       shot; the trident's throw; a gun's recoil (heavy for a splash gun) after a shot, else its aim. On foot behind
     *       cover a gun crouches between shots and leans out to the open side a moment before and after each.</li>
     *   <li>A blade (SlashBlade has its own motion) and a melee weapon between strikes: none.</li>
     * </ul>
     *
     * @param shotAt the tick of the last shot (its trigger)
     */
    @Nullable
    public static Act action(WingmanPose.Hold hold, boolean heavyGun, float combat, float shotAge, int shotAt, int cooldown,
                             WingmanStrikes.Action act, float sinceAction, int actionAt, float sinceGlare, int glareAt, byte cover, boolean onFloor) {
        if (hold == WingmanPose.Hold.BLADE) return null;
        if (act != WingmanStrikes.Action.NONE && act.animation != null && sinceAction < act.length) return new Act(act.animation, actionAt);
        if (sinceGlare < GLARE_TICKS) return new Act("glare", glareAt);
        if (combat < 0.3F) return null;
        switch (hold) {
            case BOW -> { return shotAge < RELEASE_TICKS ? new Act("bow_release", shotAt) : new Act("bow_draw", shotAt + 1); }
            case CROSSBOW -> { return shotAge < cooldown ? new Act("crossbow_reload", shotAt) : new Act("aim_rifle", 0); }
            case SPEAR -> { return shotAge < THROW_TICKS ? new Act("spear_throw", shotAt) : new Act("aim_pistol", 0); }
            case RANGED -> {
                if (cover != 0 && onFloor) {
                    boolean out = shotAge < PEEK_AFTER || cooldown - shotAge <= PEEK_BEFORE;
                    return out ? new Act(cover == 1 ? "peek_left" : "peek_right", shotAt) : new Act("cover_crouch", 0);
                }
                if (shotAge < (heavyGun ? HEAVY_RECOIL_TICKS : RECOIL_TICKS)) return new Act(heavyGun ? "recoil_heavy" : "recoil_light", shotAt);
                return new Act("aim_rifle", 0);
            }
            default -> { return null; }
        }
    }

    /**
     * The hit reaction (short name) for a hit from {@code fromYawDeg} (the direction the blow came from, degrees, Minecraft
     * yaw) on a Wingman facing {@code bodyYawDeg}: front, back, left or right of the Wingman.
     */
    public static String hit(float bodyYawDeg, float fromYawDeg) {
        float d = wrap(fromYawDeg - bodyYawDeg);
        if (Math.abs(d) <= 45) return "hit_front";
        if (Math.abs(d) >= 135) return "hit_back";
        // a positive yaw turns right in Minecraft: a blow from the Wingman's right
        return d > 0 ? "hit_right" : "hit_left";
    }

    private static float wrap(float deg) {
        float d = deg % 360;
        if (d >= 180) d -= 360;
        if (d < -180) d += 360;
        return d;
    }
}
