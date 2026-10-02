package com.miophas.singularity_iteration.common.armory;

import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

/**
 * Flight-path library shared by server simulation, client rendering and the remote's
 * distance read-out, so all three agree on where a piece is.
 *
 * <p>Suit-up choreography of one delivered piece (ticks relative to its launch):
 * <pre>
 *  -HATCH..0   pressurise: the piece rises out of the hatch, shaking, venting
 *  0..F        LAUNCH (explosive initial acceleration) -> CRUISE (full speed, arcing round
 *              to come in from in front of the wearer) -> AIR BRAKE (retro-thrust, hard
 *              deceleration over the last stretch) - ends at the hold point HOLD_DISTANCE
 *              in front of its body slot
 *  F..+HOVER   hover: a beat of stillness in front of the wearer, small thruster pulses
 *  ..+SNAP     snap-in: sucked onto the body, accelerating
 *  ..+LATCH    latch / bolt lock / pressure hiss, then the piece counts as worn
 * </pre>
 * Pieces launch {@link #STAGGER} ticks apart in {@link #SEQUENCE} order (boots ... helmet),
 * after a {@link #SEQUENCE_LEAD} warning; even a summon from the next room takes ~3 s.
 */
public final class ArmoryFlight {
    private ArmoryFlight() {}

    /** Cruise speed used to size a flight, in blocks per tick. */
    public static final double SPEED = 1.0;
    public static final int MIN_TICKS = 24, MAX_TICKS = 80;
    /** Pieces launch this many ticks apart (0.4 s): one piece at a time, never a swarm. */
    public static final int STAGGER = 8;
    /** Warning buzzer and hatch opening before the first launch. */
    public static final int SEQUENCE_LEAD = 14;
    /** Visible pressurise-and-rise out of the hatch before a piece launches. */
    public static final int HATCH_TICKS = 10;
    /** Beat of stillness in front of the wearer after the air brake. */
    public static final int HOVER_TICKS = 9;
    /** Final suction onto the body slot. */
    public static final int SNAP_TICKS = 7;
    /** Hold point distance in front of the body. */
    public static final double HOLD_DISTANCE = 2.6;
    /** Fraction of the flight at which the retro-thrust air brake fires. */
    public static final double BRAKE_AT = 0.76;
    /** Launch / assembly order: legs up, helmet last. */
    public static final ArmoryPiece[] SEQUENCE = {ArmoryPiece.FEET, ArmoryPiece.LEGS, ArmoryPiece.CHEST,
        ArmoryPiece.OFFHAND, ArmoryPiece.MAINHAND, ArmoryPiece.HEAD};
    public static final float DOCK_FRACTION = 0.25F;
    /** Within this distance pieces launch from the Armory itself, beyond it they arrive from the sky. */
    public static final double LOCAL_LAUNCH_RANGE = 64.0;

    /** Body point a piece docks to, from an entity position (feet) and body yaw in degrees. */
    public static Vec3 dockPoint(Vec3 feet, float height, float bodyYaw, ArmoryPiece piece) {
        double yaw = Math.toRadians(bodyYaw);
        // Minecraft: yaw 0 faces +Z; the entity's right is -X rotated by yaw
        double rx = -Math.cos(yaw), rz = -Math.sin(yaw);
        return new Vec3(feet.x + rx * piece.side, feet.y + height * piece.heightFraction, feet.z + rz * piece.side);
    }

    public static Vec3 dockPoint(Entity entity, float partialTick, ArmoryPiece piece) {
        Vec3 feet = entity.getPosition(partialTick);
        float yaw = entity instanceof net.minecraft.world.entity.LivingEntity living
            ? Mth.rotLerp(partialTick, living.yBodyRotO, living.yBodyRot) : entity.getYRot();
        return dockPoint(feet, entity.getBbHeight(), yaw, piece);
    }

    /** Ticks the old piece is purged before the new one arrives. */
    public static final int PURGE_LEAD = 14;
    /** Ticks a detached piece spends popping off the body before its thrusters fire. */
    public static final int PURGE_TICKS = 12;
    /** Ticks a docked piece spends seating and locking before it counts as worn. */
    public static final int LATCH_TICKS = 11;
    /** Detached pieces leave faster than suits arrive. */
    public static final double RETURN_SPEED = 1.7;

    // ------------------------------------------------------------------ segmented assembly
    /*
     * A delivered piece flies in closed, UNFOLDS into its segments while it hovers in front of
     * the wearer (torso plate / arm guards, hip plate / leg guards, left / right boot, helmet
     * shell), is sucked onto the body still open, and then each segment swings shut onto its
     * own joint and locks - one after another, heaviest first. A purged piece does the reverse
     * in one violent pop: every lock releases at once and the segments blow apart.
     */

    /** Number of segments a piece opens into (model parts that close one after another). */
    public static int segments(ArmoryPiece piece) {
        return switch (piece) {
            case CHEST, LEGS -> 3;
            case FEET -> 2;
            default -> 1;
        };
    }

    /** Opening during the hover beat: 0 = closed, 1 = fully unfolded. */
    public static double unfold(float hoverTicks) {
        return ease(hoverTicks / (HOVER_TICKS * 0.7));
    }

    /** Ticks after the start of the snap at which segment j of n is fully locked. */
    public static float segmentLockTick(int j, int n) {
        float window = SNAP_TICKS + LATCH_TICKS - 3;
        return window * (j + 1) / n;
    }

    /** Closure of segment j (0 = open, 1 = locked) at {@code sinceSnap} ticks after the snap started. */
    public static double segmentClose(float sinceSnap, int j, int n) {
        float end = segmentLockTick(j, n);
        float start = Math.max(0, end - (SNAP_TICKS + 2.5F));
        double t = Mth.clamp((sinceSnap - start) / (end - start), 0, 1);
        return t * t * t;                                    // heavy: slow swing, slam shut
    }

    /** Segment spread of a purged piece: blown fully open at once, folding back up for the flight home. */
    public static double burstSpread(float age) {
        if (age < 0) return 0;
        if (age < 2) return age / 2.0;
        return Math.max(0, 1 - (age - 2) / (PURGE_TICKS + 6.0));
    }

    /** Ease: smooth start and smooth docking. */
    public static double ease(double t) {
        t = Mth.clamp(t, 0, 1);
        return t * t * (3 - 2 * t);
    }

    /**
     * Flight progress with a trapezoidal speed profile: accelerate over the first quarter,
     * cruise, then brake over the last quarter so the piece glides into its dock point with
     * zero speed (the latch spring takes over from there).
     */
    public static double cruise(double t) {
        t = Mth.clamp(t, 0, 1);
        final double a = 0.25, v = 1 / (1 - a);
        if (t < a) return v * t * t / (2 * a);
        if (t > 1 - a) return 1 - v * (1 - t) * (1 - t) / (2 * a);
        return v * (t - a / 2);
    }

    /** Total ticks from a piece's launch until it is worn, for a flight of {@code flight} ticks. */
    public static int arrivalTicks(int flight) {
        return flight + HOVER_TICKS + SNAP_TICKS + LATCH_TICKS;
    }

    // speed profile: explosive launch, cruise, hard air brake; integrated once into a table
    private static final double[] DISTANCE = new double[257];
    static {
        double sum = 0;
        DISTANCE[0] = 0;
        for (int i = 1; i <= 256; i++) {
            double t = (i - 0.5) / 256;
            sum += speed(t);
            DISTANCE[i] = sum;
        }
        for (int i = 1; i <= 256; i++) DISTANCE[i] /= sum;
    }

    /** Relative speed at flight fraction t: 0 -> full in 10 % (explosive), cruise, then brake to rest. */
    public static double speed(double t) {
        if (t < 0.10) {
            double u = t / 0.10;
            return 1 - (1 - u) * (1 - u) * (1 - u);
        }
        if (t < BRAKE_AT) return 1 + 0.08 * (t - 0.10);       // still gaining a little
        double u = (t - BRAKE_AT) / (1 - BRAKE_AT);
        double v = 1 + 0.08 * (BRAKE_AT - 0.10);
        return v * Math.pow(1 - u, 3);                         // strongest deceleration first
    }

    /** Distance fraction covered at flight fraction t. */
    public static double flightProfile(double t) {
        t = Mth.clamp(t, 0, 1);
        double x = t * 256;
        int i = Math.min(255, (int) x);
        return Mth.lerp(x - i, DISTANCE[i], DISTANCE[i + 1]);
    }

    /** Where a piece waits in front of its body slot (body forward from yaw). */
    public static Vec3 holdPoint(Vec3 dock, float bodyYaw) {
        double yaw = Math.toRadians(bodyYaw);
        return dock.add(-Math.sin(yaw) * HOLD_DISTANCE, 0.25, Math.cos(yaw) * HOLD_DISTANCE);
    }

    public static Vec3 forward(float bodyYaw) {
        double yaw = Math.toRadians(bodyYaw);
        return new Vec3(-Math.sin(yaw), 0, Math.cos(yaw));
    }

    /**
     * Delivery curve from the launch point to the hold point: climbs out of the hatch,
     * swings wide to one side (alternating per piece) and comes in from in front of the
     * wearer, so pieces line up face-on before the snap.
     */
    public static Vec3 approach(Vec3 start, Vec3 hold, Vec3 forward, ArmoryPiece piece, double e) {
        double d = start.distanceTo(hold);
        Vec3 dir = hold.subtract(start);
        Vec3 flat = new Vec3(dir.x, 0, dir.z);
        flat = flat.lengthSqr() < 1.0E-4 ? new Vec3(0, 0, 1) : flat.normalize();
        Vec3 side = new Vec3(-flat.z, 0, flat.x).scale((piece.ordinal() % 2 == 0 ? 1 : -1) * (1.5 + Math.min(d, 40) * 0.08));
        Vec3 c1 = start.add(0, 3.0 + 0.12 * Math.min(d, 60), 0).add(side);
        Vec3 c2 = hold.add(forward.scale(Math.min(5.0, 2.0 + d / 4))).add(0, 0.9, 0);
        double u = 1 - e;
        return start.scale(u * u * u).add(c1.scale(3 * u * u * e)).add(c2.scale(3 * u * e * e)).add(hold.scale(e * e * e));
    }

    public static double approachLength(Vec3 start, Vec3 hold, Vec3 forward, ArmoryPiece piece) {
        double len = 0;
        Vec3 prev = start;
        for (int i = 1; i <= 32; i++) {
            Vec3 p = approach(start, hold, forward, piece, i / 32.0);
            len += p.distanceTo(prev);
            prev = p;
        }
        return len;
    }

    /** Detached pieces accelerate hard away from the body. */
    public static double boost(double t) {
        t = Mth.clamp(t, 0, 1);
        return Math.pow(t, 2.2);
    }

    /** Damped spring overshoot used for the "click into place" scale pulse, starting at 1. */
    public static float latchScale(float ticks) {
        if (ticks <= 0) return 1;
        return 1 + 0.13F * (float) Math.exp(-ticks / 2.2) * (float) Math.cos(ticks * 1.9);
    }

    /** How far (blocks) a purged piece is blown off the body. */
    public static final double BURST_DISTANCE = 1.9;

    /** Burst displacement curve over the purge: explosive pop in 5 ticks, then a slow drift. */
    public static double burstCurve(float age) {
        if (age <= 0) return 0;
        if (age < 5) return 1 - Math.pow(1 - age / 5.0, 4);
        return 1 + 0.025 * (age - 5);
    }

    /**
     * Where a piece pops to when it is purged off the body: every piece blows out in its own
     * direction (helmet up and back, chest forward and up, legs out front-low, boots back and
     * down, hands sideways) so the suit bursts open in all directions at once; in blocks,
     * rotated by body yaw.
     */
    public static Vec3 purgeOffset(ArmoryPiece piece, float bodyYaw, double amount) {
        double fwd, up, right;
        switch (piece) {
            case HEAD -> { fwd = -0.3; up = 0.62; right = 0.12; }
            case CHEST -> { fwd = 0.62; up = 0.3; right = -0.1; }
            case LEGS -> { fwd = 0.35; up = -0.1; right = 0.55; }
            case FEET -> { fwd = -0.55; up = -0.05; right = -0.4; }
            case OFFHAND -> { fwd = 0.1; up = 0.2; right = -0.7; }
            default -> { fwd = 0.1; up = 0.2; right = 0.7; }
        }
        double yaw = Math.toRadians(bodyYaw);
        double fx = -Math.sin(yaw), fz = Math.cos(yaw);       // body forward
        double rx = -Math.cos(yaw), rz = -Math.sin(yaw);      // body right
        return new Vec3((fx * fwd + rx * right) * amount, up * amount, (fz * fwd + rz * right) * amount);
    }

    /** Point on the flight path at raw progress t in [0,1]. */
    public static Vec3 position(Vec3 start, Vec3 end, double t) {
        return bezier(start, end, cruise(t));
    }

    /** Point on the curve at already-eased parameter e. */
    public static Vec3 bezier(Vec3 start, Vec3 end, double e) {
        double d = start.distanceTo(end);
        Vec3 c1 = start.add(0, 2.5 + 0.12 * Math.min(d, 60), 0);
        Vec3 back = start.subtract(end);
        Vec3 flat = new Vec3(back.x, 0, back.z);
        flat = flat.lengthSqr() < 1.0E-4 ? new Vec3(0, 0, 1) : flat.normalize();
        Vec3 c2 = end.add(flat.scale(Math.min(3.0, 0.3 + d / 3))).add(0, 1.2, 0);
        double u = 1 - e;
        return start.scale(u * u * u).add(c1.scale(3 * u * u * e)).add(c2.scale(3 * u * e * e)).add(end.scale(e * e * e));
    }

    /** Length of the curve, sampled. */
    public static double length(Vec3 start, Vec3 end) {
        double len = 0;
        Vec3 prev = start;
        for (int i = 1; i <= 24; i++) {
            Vec3 p = position(start, end, i / 24.0);
            len += p.distanceTo(prev);
            prev = p;
        }
        return len;
    }

    public static int ticksFor(double length) {
        return Mth.clamp((int) Math.round(length / SPEED), MIN_TICKS, MAX_TICKS);
    }

    public static int returnTicksFor(double length) {
        return Mth.clamp((int) Math.round(length / RETURN_SPEED), 12, MAX_TICKS);
    }

    /**
     * Launch point: the Armory top when it is in the same dimension and close by,
     * otherwise a point high in the sky towards the Armory (or straight above for
     * another dimension) so the flight stays visible and inside loaded chunks.
     */
    public static Vec3 launchPoint(Vec3 player, Vec3 armoryTop, boolean sameDimension, ArmoryPiece piece) {
        if (sameDimension && player.distanceTo(armoryTop) <= LOCAL_LAUNCH_RANGE) {
            return armoryTop.add((piece.ordinal() - 2.5) * 0.12, 0, 0);
        }
        Vec3 dir = sameDimension ? armoryTop.subtract(player) : new Vec3(1, 0, 0.35);
        Vec3 flat = new Vec3(dir.x, 0, dir.z);
        flat = flat.lengthSqr() < 1.0E-4 ? new Vec3(0, 0, 1) : flat.normalize();
        double spread = (piece.ordinal() - 2.5) * 1.5;
        Vec3 side = new Vec3(-flat.z, 0, flat.x);
        return player.add(flat.scale(30)).add(side.scale(spread)).add(0, 16 + piece.ordinal(), 0);
    }
}
