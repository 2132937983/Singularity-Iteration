// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.fcs;

/**
 * 0.1.10.23 (FCS P2, data link): the predicted path of a creature along the next nodes of its navigation path, which
 * the server sends to a Behavior Predictor Mk.II wearer. Pure arithmetic over a flat array of node positions (x, y, z
 * triples, the bottom centres of the node blocks).
 *
 * <p>The creature walks from where it is to each node in turn at its current ground speed and stops at the last
 * node (the end of its path). A node it already reached (closer than {@link #REACHED} horizontally) is skipped, as the
 * vanilla path follower does.
 */
public final class PathFollower {
    private PathFollower() {}

    /** Horizontal distance at which a node counts as reached. */
    public static final double REACHED = 0.5;

    /**
     * Every second point for {@code ticks} ticks, the first being the start; empty when the speed is below 0.02 blocks
     * per tick or there are no nodes.
     *
     * @param nodes x, y, z triples (at least one node)
     * @param speed blocks per tick along the path
     */
    public static double[] follow(double[] nodes, double px, double py, double pz, double speed, int ticks) {
        if (nodes == null || nodes.length < 3 || !(speed >= 0.02)) return new double[0];
        int count = nodes.length / 3, next = 0;
        while (next < count - 1 && Math.hypot(nodes[3 * next] - px, nodes[3 * next + 2] - pz) < REACHED) next++;
        double[] out = new double[3 * (ticks / 2 + 1)];
        int n = 0;
        out[n++] = px; out[n++] = py; out[n++] = pz;
        double x = px, y = py, z = pz;
        for (int t = 1; t <= ticks; t++) {
            double left = speed;
            while (left > 0 && next < count) {
                double nx = nodes[3 * next], ny = nodes[3 * next + 1], nz = nodes[3 * next + 2];
                double d = Math.sqrt((nx - x) * (nx - x) + (ny - y) * (ny - y) + (nz - z) * (nz - z));
                if (d <= left) { x = nx; y = ny; z = nz; left -= d; next++; }
                else { x += (nx - x) * left / d; y += (ny - y) * left / d; z += (nz - z) * left / d; left = 0; }
            }
            if (t % 2 == 0) { out[n++] = x; out[n++] = y; out[n++] = z; }
        }
        return java.util.Arrays.copyOf(out, n);
    }
}
