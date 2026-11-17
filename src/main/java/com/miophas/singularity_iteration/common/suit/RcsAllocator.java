// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.suit;

import java.util.List;

/**
 * 0.1.11.67 (quantum flight phase R1, decision two A): the reaction control of the quantum suit. It turns what the flight
 * asks of the body into the output of each nozzle of {@link QuantumNozzles}, the way a real RCS does: the main thrust
 * follows the throttle, the hover stands on the lift nozzles only, and the attitude jets fire in hard on/off pulses
 * (pulse-width modulated on a four-tick frame) where their push and turn serve the demand.
 *
 * <p>Body frame (the model space of the nozzles): +x the left of the wearer, +y towards the feet, +z the back. A
 * {@link Command} is the push wanted on the body (force, -1..1 per axis) and the turn wanted (torque, -1..1 per axis):
 * <ul>
 *   <li>hover (upright): forward push is -z, push to the left +x, yaw to the right +y torque;</li>
 *   <li>cruise (lying along the path, head first, back up): the brake pushes towards the feet (+y), yaw to the right is
 *       -z torque, nose up is -x torque.</li>
 * </ul>
 * A nozzle at r pushes along -d (d its exhaust) and turns the body by (r - centre of mass) x (-d); its share of the
 * demand is the projection of both on the command.
 */
public final class RcsAllocator {
    private RcsAllocator() {}

    /** Ticks in one pulse frame; a jet burns 0..PERIOD ticks of each frame. */
    public static final int PERIOD = 4;
    /** Demand under which a jet stays dark (no flicker on noise). */
    public static final float DEADBAND = 0.15F;
    /** The centre of mass of the wearer in the body frame (pixels): the waist. */
    private static final float COM_Y = 11F;
    /** The lever that makes a torque comparable with a force (pixels). */
    private static final float LEVER = 6F;

    /** Push and turn wanted of the body, each -1..1. */
    public record Command(float fx, float fy, float fz, float tx, float ty, float tz) {
        public static final Command NONE = new Command(0, 0, 0, 0, 0, 0);
    }

    /** The place of a nozzle in the body frame (the part's pivot in the rest pose plus its offset), pixels. */
    public static float[] bodyPosition(QuantumNozzles.Nozzle n) {
        float px = 0, py = 0;
        switch (n.part()) {
            case RIGHT_ARM -> { px = -5; py = 2; }
            case LEFT_ARM -> { px = 5; py = 2; }
            case RIGHT_LEG -> { px = -1.9F; py = 12; }
            case LEFT_LEG -> { px = 1.9F; py = 12; }
            default -> { }
        }
        return new float[]{n.x() + px, n.y() + py, n.z()};
    }

    /** How much a jet serves the command: its push and its turn projected on it (unbounded, may be negative). */
    public static float score(QuantumNozzles.Nozzle n, Command c) {
        float fx = -n.dx(), fy = -n.dy(), fz = -n.dz();
        float[] r = bodyPosition(n);
        float rx = r[0], ry = r[1] - COM_Y, rz = r[2];
        float tx = (ry * fz - rz * fy) / LEVER, ty = (rz * fx - rx * fz) / LEVER, tz = (rx * fy - ry * fx) / LEVER;
        return fx * c.fx() + fy * c.fy() + fz * c.fz() + tx * c.tx() + ty * c.ty() + tz * c.tz();
    }

    /**
     * The pulse of a jet: dark under the dead band, else on for {@code round(duty * PERIOD)} ticks of each frame (at least
     * one), the frames of the jets shifted by {@code phase}. Always 0 or 1.
     */
    public static float pulse(float score, int tick, int phase) {
        if (!(score >= DEADBAND)) return 0F;
        int on = Math.max(1, Math.round(Math.min(1F, score) * PERIOD));
        return Math.floorMod(tick + phase, PERIOD) < on ? 1F : 0F;
    }

    /**
     * Output of every nozzle, 0..1, in the order of {@code nozzles}: main and lift and field as {@link QuantumNozzles#output}
     * (the main thrust cut by the {@code brake}, 0..1), attitude jets as pulses for the command.
     */
    public static float[] allocate(List<QuantumNozzles.Nozzle> nozzles, Command c, float[] limbs, float throttle, float brake, int tick) {
        float cut = 1 - clamp(brake);
        float[] out = new float[nozzles.size()];
        for (int i = 0; i < out.length; i++) {
            QuantumNozzles.Nozzle n = nozzles.get(i);
            float base = QuantumNozzles.output(n, limbs, throttle);
            out[i] = switch (n.kind()) {
                case RCS -> pulse(score(n, c), tick, i);
                case MAIN -> base * cut;
                default -> base;
            };
        }
        return out;
    }

    /**
     * The command of the flight, blended from the hover to the cruise by {@code cruise} (0..1).
     *
     * @param stickForward the forward stick (-1..1); {@code stickLeft} the stick to the left (-1..1)
     * @param velocity     the velocity in the frame of the view (blocks per tick): {left, down, back}
     * @param yawRate      turning to the right, degrees per second
     * @param pitchRate    the nose rising, degrees per second
     * @param brake        the deceleration of the cruise, 0..1
     */
    public static Command command(float cruise, float stickForward, float stickLeft, float[] velocity, float yawRate, float pitchRate, float brake) {
        float h = 1 - clamp(cruise), k = clamp(cruise);
        float vl = at(velocity, 0), vb = at(velocity, 2);
        // hover: the stick pushes; on a released axis the jets stop the drift
        float fx = Math.abs(stickLeft) > 0.1F ? stickLeft : -vl * 6F;
        float fz = Math.abs(stickForward) > 0.1F ? -stickForward : -vb * 6F;
        float yaw = clampSigned(yawRate / 90F), pitch = clampSigned(pitchRate / 90F), b = clamp(brake);
        return new Command(clampSigned(h * fx), clampSigned(k * b), clampSigned(h * fz),
            clampSigned(-k * pitch), clampSigned(h * yaw), clampSigned(-k * yaw));
    }

    private static float at(float[] a, int i) { return a == null || i >= a.length || !Float.isFinite(a[i]) ? 0F : a[i]; }

    private static float clamp(float v) { return Float.isFinite(v) ? Math.max(0F, Math.min(1F, v)) : 0F; }

    private static float clampSigned(float v) { return Float.isFinite(v) ? Math.max(-1F, Math.min(1F, v)) : 0F; }
}
