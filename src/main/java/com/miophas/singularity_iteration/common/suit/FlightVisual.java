// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.suit;

import java.util.Map;

/**
 * 0.1.11.125 (armour and flight overhaul E2): the flight look of a quantum flyer, one state machine per flyer, and the pose of
 * the suit's moving parts from its animation clips ({@link SuitAnimation}, {@code animations/quantum_armor.animation.json}).
 *
 * <p>Modes ({@link #mode}):
 * <ul>
 *   <li>{@link Mode#STOWED}: not in maneuver flight: clip {@code stowed} (the parts at rest, the flight glows off).</li>
 *   <li>{@link Mode#HOVER}: in maneuver flight with the throttle under {@link #HOVER_THROTTLE}: clip {@code hover} (the head
 *       ring and the back wing float up and down over 3.2 s, the boot strips glow).</li>
 *   <li>{@link Mode#MANEUVER}: powered flight below the sound barrier: clip {@code rcs_flight}, which moves nothing, so the
 *       parts and glows are as in {@code stowed} (the attitude pulses come from the reaction control).</li>
 *   <li>{@link Mode#HIGH_SPEED}: past the sound barrier (Mach 1 up, back below Mach 0.9, {@link ManeuverMode#supersonic}),
 *       with the pods of a base or propulsion frame (a graviton frame has none: it stays in {@link Mode#MANEUVER}): clip
 *       {@code mach4_boost} (the pods swing out and slide back in 0.5 s, held), then {@code mach4_sustain} looping. On the
 *       way out {@code mach4_retract} (0.6 s) folds them first.</li>
 * </ul>
 * Between the other modes the pose crossfades over {@link #BLEND} seconds. Times are seconds of the flyer's own clock.
 */
public final class FlightVisual {
    /** Below this throttle a flyer in maneuver flight hovers. */
    public static final float HOVER_THROTTLE = 0.35F;
    /** Seconds of a crossfade between two modes. */
    public static final float BLEND = 0.25F;

    public static final String STOWED_CLIP = "animation.quantum.stowed", HOVER_CLIP = "animation.quantum.hover",
        MANEUVER_CLIP = "animation.quantum.rcs_flight", BOOST_CLIP = "animation.quantum.mach4_boost",
        SUSTAIN_CLIP = "animation.quantum.mach4_sustain", RETRACT_CLIP = "animation.quantum.mach4_retract";

    public enum Mode { STOWED, HOVER, MANEUVER, HIGH_SPEED }

    /** The mode for a flyer: in maneuver flight or not, its throttle, past the barrier or not, with pods or not. */
    public static Mode mode(boolean flying, float throttle, boolean supersonic, boolean pods) {
        if (!flying) return Mode.STOWED;
        if (supersonic && pods) return Mode.HIGH_SPEED;
        return throttle < HOVER_THROTTLE ? Mode.HOVER : Mode.MANEUVER;
    }

    private Mode mode = Mode.STOWED, from = Mode.STOWED;
    private double since = Double.NEGATIVE_INFINITY, fromSince = Double.NEGATIVE_INFINITY, switchedAt = Double.NEGATIVE_INFINITY;

    /** Takes the flyer's mode at time {@code now}. */
    public void update(Mode next, double now) {
        if (next == mode) return;
        from = mode;
        fromSince = since;
        switchedAt = now;
        mode = next;
        since = now;
    }

    public Mode mode() { return mode; }

    /** True while the moving parts are anywhere but at rest: in flight, or folding back. */
    public boolean posed(double now, Map<String, SuitAnimation.Clip> clips) {
        return mode != Mode.STOWED || now - switchedAt < transition(clips) + BLEND;
    }

    /** Seconds the change out of the last mode takes before the crossfade: the retract after a high-speed flight. */
    private double transition(Map<String, SuitAnimation.Clip> clips) {
        SuitAnimation.Clip r = clips.get(RETRACT_CLIP);
        return from == Mode.HIGH_SPEED && mode != Mode.HIGH_SPEED && r != null ? r.length() : 0;
    }

    /**
     * The value of {@code channel} of {@code bone} at {@code now}: {x, y, z} in the clip's units, the rest value (0 for
     * position and rotation, 1 for scale) where no clip moves it.
     */
    public float[] sample(Map<String, SuitAnimation.Clip> clips, String bone, String channel, double now) {
        float[] rest = channel.equals("scale") ? new float[]{1, 1, 1} : new float[]{0, 0, 0};
        float[] to = of(clips, mode, since, bone, channel, now, rest);
        double out = transition(clips), t = now - switchedAt;
        float[] fromPose;
        if (out > 0) {
            if (t >= out + BLEND) return to;
            // folding the pods back first (the retract clip, where it moves the bone; elsewhere the old pose held)
            SuitAnimation.Clip r = clips.get(RETRACT_CLIP);
            float[] v = r.moves(bone) ? r.sample(bone, channel, t) : null;
            fromPose = v != null ? v : of(clips, from, fromSince, bone, channel, switchedAt, rest);
            if (t < out) return fromPose;
            t -= out;
        } else {
            if (t >= BLEND) return to;
            fromPose = of(clips, from, fromSince, bone, channel, now, rest);
        }
        float k = (float) Math.max(0, Math.min(1, t / BLEND));
        k = k * k * (3 - 2 * k);
        return new float[]{fromPose[0] + (to[0] - fromPose[0]) * k, fromPose[1] + (to[1] - fromPose[1]) * k, fromPose[2] + (to[2] - fromPose[2]) * k};
    }

    /** The value in mode {@code m} entered at {@code start}. */
    private static float[] of(Map<String, SuitAnimation.Clip> clips, Mode m, double start, String bone, String channel, double now, float[] rest) {
        double t = Double.isInfinite(start) ? 0 : now - start;
        SuitAnimation.Clip c;
        switch (m) {
            case HIGH_SPEED -> {
                SuitAnimation.Clip boost = clips.get(BOOST_CLIP);
                if (boost != null && t < boost.length()) c = boost;
                else { c = clips.get(SUSTAIN_CLIP); t -= boost == null ? 0 : boost.length(); }
            }
            case HOVER -> c = clips.get(HOVER_CLIP);
            case MANEUVER -> c = clips.get(MANEUVER_CLIP);
            default -> c = clips.get(STOWED_CLIP);
        }
        float[] v = c == null ? null : c.sample(bone, channel, t);
        if (v == null && m == Mode.MANEUVER) {   // rcs_flight moves nothing: the parts and glows as stowed (the flight glows off)
            SuitAnimation.Clip stowed = clips.get(STOWED_CLIP);
            v = stowed == null ? null : stowed.sample(bone, channel, t);
        }
        return v != null ? v : rest;
    }
}
