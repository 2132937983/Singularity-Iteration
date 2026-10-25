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
    }

    /** Block answers per cell, valid for one tick ({@link #clear} at the start of each tick). */
    public static final class Cache implements Blocks {
        private final Blocks world;
        private final java.util.HashMap<Long, Byte> answers = new java.util.HashMap<>();
        private long queries;

        public Cache(Blocks world) { this.world = world; }

        public void clear() { answers.clear(); }

        /** Queries that went to the world (for tests and diagnostics). */
        public long queries() { return queries; }

        private static long key(int x, int y, int z) {
            return ((long) x & 0x3FFFFFF) << 38 | ((long) z & 0x3FFFFFF) << 12 | ((long) y & 0xFFF);
        }

        private byte bits(int x, int y, int z, int bit) {
            long k = key(x, y, z);
            Byte known = answers.get(k);
            int b = known == null ? 0 : known;
            if ((b & (bit << 4)) == 0) {
                queries++;
                boolean value = bit == 1 ? world.solid(x, y, z) : world.air(x, y, z);
                b |= bit << 4;
                if (value) b |= bit;
                answers.put(k, (byte) b);
            }
            return (byte) b;
        }

        @Override public boolean solid(int x, int y, int z) { return (bits(x, y, z, 1) & 1) != 0; }
        @Override public boolean air(int x, int y, int z) { return (bits(x, y, z, 2) & 2) != 0; }
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

    private static int floor(double v) { return (int) Math.floor(v); }
}
