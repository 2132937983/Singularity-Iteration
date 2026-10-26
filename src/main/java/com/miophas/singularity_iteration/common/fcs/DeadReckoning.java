// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.fcs;

/**
 * 0.1.10.20 (FCS P8): the predicted path of a creature for the behaviour predictor (the rules of
 * {@code SensorTracker} since 0.1.7.36), with fewer block queries. Pure arithmetic over a {@link Blocks} view.
 *
 * <p>Rules per tick: on the ground the velocity stays; in the air (with gravity) {@code vy = (vy - 0.08) *
 * 0.98} and {@code vxz *= 0.91}; the path stops at a wall (a block with a collision shape 0.3 blocks above the
 * new feet position); a falling creature lands when the block 0.05 blocks below the new feet is not air, and
 * stays on the ground. Every second point is kept (at most 11 points for 20 ticks).
 *
 * <p>The block answers of one tick are the same for every query, so {@link Cache} keeps them per block cell
 * for one tick: a slow creature crosses a cell boundary only every few ticks, and creatures in a group share
 * cells. Before 0.1.10.20 each step asked the level twice.
 */
public final class DeadReckoning {
    private DeadReckoning() {}

    /** The world as the predictor sees it. */
    public interface Blocks {
        /** The block at the cell has a collision shape. */
        boolean solid(int x, int y, int z);
        /** The block at the cell is air. */
        boolean air(int x, int y, int z);
        /** 0.1.10.23: top of the collision shape in the cell, from its floor: 0 none, 0.5 a slab, 1 a full block, 1.5 a fence. */
        default double top(int x, int y, int z) { return solid(x, y, z) ? 1 : 0; }
        /** 0.1.10.23: the cell holds water. */
        default boolean water(int x, int y, int z) { return false; }
        /** 0.1.10.23: slipperiness of the block (0.6 most blocks, 0.8 slime, 0.98 ice). */
        default double friction(int x, int y, int z) { return 0.6; }
    }

    /** Block answers per cell, valid for one tick ({@link #clear} at the start of each tick). */
    public static final class Cache implements Blocks {
        private final Blocks world;
        private final java.util.HashMap<Long, Cell> answers = new java.util.HashMap<>();
        private long queries;

        /** The answers known for one cell (bit set in {@code known} = asked). */
        private static final class Cell {
            int known;
            boolean solid, air, water;
            double top, friction;
        }

        public Cache(Blocks world) { this.world = world; }

        public void clear() { answers.clear(); }

        /** Queries that went to the world (for tests and diagnostics). */
        public long queries() { return queries; }

        private static long key(int x, int y, int z) {
            return ((long) x & 0x3FFFFFF) << 38 | ((long) z & 0x3FFFFFF) << 12 | ((long) y & 0xFFF);
        }

        private Cell cell(int x, int y, int z, int bit) {
            Cell c = answers.computeIfAbsent(key(x, y, z), k -> new Cell());
            if ((c.known & bit) == 0) {
                queries++;
                c.known |= bit;
                switch (bit) {
                    case 1 -> c.solid = world.solid(x, y, z);
                    case 2 -> c.air = world.air(x, y, z);
                    case 4 -> c.top = world.top(x, y, z);
                    case 8 -> c.water = world.water(x, y, z);
                    default -> c.friction = world.friction(x, y, z);
                }
            }
            return c;
        }

        @Override public boolean solid(int x, int y, int z) { return cell(x, y, z, 1).solid; }
        @Override public boolean air(int x, int y, int z) { return cell(x, y, z, 2).air; }
        @Override public double top(int x, int y, int z) { return cell(x, y, z, 4).top; }
        @Override public boolean water(int x, int y, int z) { return cell(x, y, z, 8).water; }
        @Override public double friction(int x, int y, int z) { return cell(x, y, z, 16).friction; }
    }

    /**
     * The path from the feet position {@code (px, py, pz)} with velocity {@code (vx, vy, vz)}: flat array of
     * x, y, z triples, the first being the start. Empty when the creature does not move (horizontal speed below
     * 0.02 and vertical below 0.02 blocks per tick). {@code turnRate} (rad per tick, 0 for a straight path) turns
     * the horizontal velocity each tick: the path of a circling creature ({@code TargetTracker#turnRate}).
     */
    public static double[] path(Blocks blocks, double px, double py, double pz, double vx, double vy, double vz,
                                boolean onGround, boolean noGravity, int ticks, double turnRate) {
        double cos = Math.cos(turnRate), sin = Math.sin(turnRate);
        if (vx * vx + vz * vz < 0.0004 && Math.abs(vy) < 0.02) return new double[0];
        double[] out = new double[3 * (ticks / 2 + 1)];
        int n = 0;
        out[n++] = px; out[n++] = py; out[n++] = pz;
        boolean grounded = onGround;
        for (int t = 1; t <= ticks; t++) {
            if (turnRate != 0) { double rx = vx * cos - vz * sin; vz = vx * sin + vz * cos; vx = rx; }
            if (!grounded && !noGravity) { vx *= 0.91; vy = (vy - 0.08) * 0.98; vz *= 0.91; }
            double nx = px + vx, ny = py + vy, nz = pz + vz;
            if (blocks.solid(floor(nx), floor(ny + 0.3), floor(nz))) break;
            if (!grounded && vy < 0 && !blocks.air(floor(nx), floor(ny - 0.05), floor(nz))) {
                grounded = true;
                vy = 0;
            }
            px = nx; py = ny; pz = nz;
            if (t % 2 == 0) { out[n++] = px; out[n++] = py; out[n++] = pz; }
        }
        return java.util.Arrays.copyOf(out, n);
    }

    // ------------------------------------------------------------------ 0.1.10.23 (P2): the vanilla movement rules

    /** Highest step a creature walks up without a jump (vanilla {@code maxUpStep}). */
    public static final double STEP = 0.6;
    /** Jump speed (vanilla {@code BASE_JUMP_POWER}). */
    public static final double JUMP = 0.42;
    /** Highest ledge that a jump of {@link #JUMP} reaches (the top of the arc is 1.25 blocks). */
    public static final double JUMP_REACH = 1.25;
    private static final int GROUND = 0, WATER = 1, AIR = 2;
    /** Distance from the wearer at which a melee hunter stops pushing (the vanilla attack reach of a zombie is 1.43). */
    public static final double HUNTER_STOP = 1.2;

    /** 0.1.10.23 (P2): what {@link #walk} knows about the creature besides its position. */
    public static final class Body {
        /** Its velocity (blocks per tick): the motion seen in the last tick, or with {@link #packet} its stored velocity. */
        public double vx, vy, vz;
        /** The motion per tick it keeps under its own power (its velocity in steady motion, before a hit when hit). */
        public double driveX, driveZ;
        /** (vx, vy, vz) is the stored velocity of the creature (the server's velocity packet), not its motion seen per tick. */
        public boolean packet;
        public boolean onGround, noGravity;
        /** Its height (head room of a step). */
        public double height = 1.95;
        /** Turns its velocity and its push each tick (rad per tick, 0 for a straight path). */
        public double turnRate;
        /** A melee hunter: heads for (stopX, stopZ) and stops pushing within {@code stopRadius}; 0 = no. */
        public double stopX, stopZ, stopRadius;
    }

    /**
     * 0.1.10.23 (P2): the path with the vanilla movement of a living creature, as {@link #path} (every second point, the
     * first being the start, at most {@code ticks / 2 + 1} points; empty when it does not move). Per tick, in the
     * order of {@code LivingEntity.travel}: its own push is added, it moves, then the velocity is multiplied by the
     * friction of the medium and gravity applies:
     * <ul>
     *   <li>on the ground: friction = the slipperiness of the block under its feet x 0.91 (0.546 on most blocks, 0.89 on
     *       ice), the push on a block of slipperiness {@code s} = its ground speed x {@code 0.216 / s^3};</li>
     *   <li>in water: horizontal and vertical x 0.8, vertical - 0.005 (gravity / 16), push 0.02 (a swimmer keeps the push
     *       it has now);</li>
     *   <li>in the air: horizontal x 0.91, vertical {@code (vy - 0.08) x 0.98}, push 0.02 (a jumper keeps its push).</li>
     * </ul>
     * The push comes from {@link Body#driveX} / {@link Body#driveZ}, the motion the creature keeps under its own power: in steady motion it is
     * the velocity itself (the path is then the straight line of {@link #path}); for a creature that was hit it is its
     * velocity before the hit, so the knockback dies away with the friction of the block (fast on stone, slowly on ice).
     * Obstacles at the feet: up to {@link #STEP} it steps up; up to {@link #JUMP_REACH} with head room it jumps
     * ({@link #JUMP}) and moves on once its feet are above the edge; higher it stops (the path ends, as before). A
     * creature that walks off an edge falls; it lands on the top of the collision shape. A creature without gravity
     * keeps the rules of {@link #path}.
     *
     * <p>Vanilla moves a creature by its stored velocity plus its push and then applies the friction, so the motion
     * seen per tick is {@code stored + push} and the stored velocity is that motion x the friction. Without
     * {@link Body#packet} the velocity is the motion seen in the last tick (a {@link TargetTracker} velocity) and is
     * turned into the stored velocity; with it, it is the stored velocity itself (the velocity packet of the server,
     * which also carries a knockback at once).
     *
     * <p>A melee creature that hunts the wearer ({@link Body#stopRadius}) walks toward the wearer and stops pushing at
     * {@link #HUNTER_STOP} (where it stands while it attacks); without it a knocked-back hunter was predicted to walk
     * through the wearer.
     */
    public static double[] walk(Blocks blocks, double px, double py, double pz, Body body, int ticks) {
        double vx = body.vx, vy = body.vy, vz = body.vz, wx = body.driveX, wz = body.driveZ, turnRate = body.turnRate, height = body.height;
        if (body.noGravity) return path(blocks, px, py, pz, vx, vy, vz, body.onGround, true, ticks, turnRate);
        if (vx * vx + vz * vz < 0.0004 && Math.abs(vy) < 0.02 && wx * wx + wz * wz < 0.0004) return new double[0];
        boolean packet = body.packet, hunts = body.stopRadius > 0;
        double cos = Math.cos(turnRate), sin = Math.sin(turnRate);
        boolean grounded = body.onGround;
        // the push that keeps the motion |w| in the medium it is in now; its ground speed (vanilla getSpeed) for the others
        int m0 = medium(blocks, px, py, pz, grounded);
        double s0 = m0 == GROUND ? slip(blocks, px, py, pz) : 0.6;
        double f0 = factor(m0, s0), w = Math.hypot(wx, wz);
        double push0 = w * (1 - f0);
        double groundSpeed = m0 == GROUND ? push0 * s0 * s0 * s0 / 0.216 : w * (1 - 0.546);
        if (!packet) {
            // the motion seen -> the stored velocity after this tick's friction and gravity
            vx *= f0; vz *= f0;
            vy = grounded ? 0 : m0 == WATER ? vy * 0.8 - 0.005 : (vy - 0.08) * 0.98;
        }
        boolean driven = w >= 0.01;
        double dx = driven ? wx / w : 0, dz = driven ? wz / w : 0;
        double[] out = new double[3 * (ticks / 2 + 1)];
        int n = 0;
        out[n++] = px; out[n++] = py; out[n++] = pz;
        for (int t = 1; t <= ticks; t++) {
            if (turnRate != 0) {
                double rx = vx * cos - vz * sin; vz = vx * sin + vz * cos; vx = rx;
                double qx = dx * cos - dz * sin; dz = dx * sin + dz * cos; dx = qx;
            }
            int m = medium(blocks, px, py, pz, grounded);
            double s = m == GROUND ? slip(blocks, px, py, pz) : 0.6;
            double push = !driven ? 0 : m == GROUND ? groundSpeed * 0.216 / (s * s * s) : m == m0 ? push0 : 0.02;
            if (hunts && driven) {
                // a hunter heads for the wearer and stops pushing at its attack distance
                double hx = body.stopX - px, hz = body.stopZ - pz, hd = Math.hypot(hx, hz);
                if (hd <= body.stopRadius) push = 0;
                else { dx = hx / hd; dz = hz / hd; }
            }
            vx += push * dx; vz += push * dz;
            // horizontal move against the obstacle at the feet
            double tx = px + vx, tz = pz + vz;
            int fx = floor(tx), fz = floor(tz);
            double top = obstacleTop(blocks, fx, py, fz);
            if (top > py + 1.0E-3) {
                double h = top - py;
                boolean room = headRoom(blocks, fx, top, fz, height);
                if (room && h <= STEP && (grounded || m == WATER)) { py = top; px = tx; pz = tz; }
                else if (room && h <= JUMP_REACH && grounded && driven) { vy = JUMP; grounded = false; }
                else if (!(room && !grounded && vy > 0 && h <= JUMP_REACH)) break;   // a wall: the path ends
            } else {
                px = tx; pz = tz;
            }
            // vertical move: fall off an edge, land on the top of the collision shape
            if (grounded && !supported(blocks, px, py, pz)) grounded = false;
            if (!grounded) {
                double ny = py + vy;
                if (vy < 0) {
                    int cy = floor(ny - 0.05);
                    double ground = blocks.top(floor(px), cy, floor(pz));
                    if (ground > 0 && ny <= cy + ground) { ny = cy + ground; grounded = true; vy = 0; }
                }
                py = ny;
            }
            double f = factor(m, s);
            vx *= f; vz *= f;
            if (!grounded) vy = m == WATER ? vy * 0.8 - 0.005 : (vy - 0.08) * 0.98;
            if (t % 2 == 0) { out[n++] = px; out[n++] = py; out[n++] = pz; }
        }
        return java.util.Arrays.copyOf(out, n);
    }

    private static int medium(Blocks blocks, double x, double y, double z, boolean grounded) {
        if (blocks.water(floor(x), floor(y + 0.2), floor(z))) return WATER;
        return grounded ? GROUND : AIR;
    }

    /** Slipperiness of the block that affects the movement (vanilla: 0.5 blocks under the feet). */
    private static double slip(Blocks blocks, double x, double y, double z) {
        return Math.max(0.4, blocks.friction(floor(x), floor(y - 0.500001), floor(z)));
    }

    private static double factor(int medium, double slip) {
        return medium == GROUND ? slip * 0.91 : medium == WATER ? 0.8 : 0.91;
    }

    /** Highest top of the collision shapes in the two cells at the feet of a creature at height {@code y}, or -infinity. */
    private static double obstacleTop(Blocks blocks, int x, double y, int z) {
        int cy = floor(y + 1.0E-4);
        double best = Double.NEGATIVE_INFINITY;
        for (int k = 0; k < 2; k++) {
            double top = blocks.top(x, cy + k, z);
            if (top > 0 && cy + k + top > y + 1.0E-3) best = Math.max(best, cy + k + top);
        }
        return best;
    }

    /** No collision shape above {@code top} for {@code height} blocks. */
    private static boolean headRoom(Blocks blocks, int x, double top, int z, double height) {
        for (int cy = floor(top); cy <= floor(top + height - 1.0E-3); cy++) {
            double t = blocks.top(x, cy, z);
            if (t > 0 && cy + t > top + 1.0E-3) return false;
        }
        return true;
    }

    /** Something to stand on right under the feet. */
    private static boolean supported(Blocks blocks, double x, double y, double z) {
        int cy = floor(y - 0.05);
        double top = blocks.top(floor(x), cy, floor(z));
        return top > 0 && cy + top >= y - 0.05;
    }

    private static int floor(double v) { return (int) Math.floor(v); }
}
