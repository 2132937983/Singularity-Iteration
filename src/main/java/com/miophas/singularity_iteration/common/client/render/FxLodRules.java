// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.client.render;

import java.util.Arrays;

/**
 * 0.1.10.32 (P-3): level of detail of the suit and Armory effects by distance, and the budget of whole-body passes. Pure
 * (java.* only); {@code FxBudget} and the effect renderers use it.
 * <ul>
 *   <li><b>Tiers:</b> {@link #NEAR} within {@link #NEAR_BLOCKS} blocks of the camera (everything), {@link #MID} within
 *       {@link #MID_BLOCKS} (fewer segments, no afterimages, half the trail points, no sparks), {@link #FAR} beyond (only
 *       the light: no flame strips, no trails).</li>
 *   <li><b>Budget:</b> a whole-body pass (hit ripple, hologram grid, nano mesh overlay) is the most expensive effect: at
 *       most {@code limit} of them a frame. Requests of one frame decide the next: the nearest {@code limit} entities get
 *       their pass ({@link Budget}).</li>
 * </ul>
 */
public final class FxLodRules {
    private FxLodRules() {}

    public static final int NEAR = 0, MID = 1, FAR = 2;
    public static final double NEAR_BLOCKS = 24, MID_BLOCKS = 64;
    public static final int DEFAULT_BUDGET = 4;

    /** Tier of an effect {@code distSq} (squared blocks) from the camera; with {@code enabled} false always {@link #NEAR}. */
    public static int tier(double distSq, boolean enabled) {
        if (!enabled) return NEAR;
        return distSq < NEAR_BLOCKS * NEAR_BLOCKS ? NEAR : distSq < MID_BLOCKS * MID_BLOCKS ? MID : FAR;
    }

    /** Segments of a round shape: the full count near, 60 % (at least 4) mid, 40 % (at least 4) far. */
    public static int segments(int full, int tier) {
        return switch (tier) {
            case NEAR -> full;
            case MID -> Math.max(4, Math.round(full * 0.6F));
            default -> Math.max(4, Math.round(full * 0.4F));
        };
    }

    /**
     * Budget of whole-body passes. {@link #request} during a frame records who wants a pass and how far it is; {@link #frame}
     * at the start of the next frame grants the nearest {@code limit} of them. {@link #allowed} answers for this frame.
     */
    public static final class Budget {
        private int[] ids = new int[64];
        private double[] dist = new double[64];
        private int count;
        private int[] granted = new int[0];
        private int limit;

        public Budget(int limit) {
            this.limit = Math.max(1, limit);
        }

        public void limit(int limit) {
            this.limit = Math.max(1, limit);
        }

        /** Records a wish of entity {@code id} at {@code distSq}; a repeated id keeps its nearest distance. */
        public void request(int id, double distSq) {
            for (int i = 0; i < count; i++) {
                if (ids[i] == id) {
                    if (distSq < dist[i]) dist[i] = distSq;
                    return;
                }
            }
            if (count == ids.length) {
                ids = Arrays.copyOf(ids, count * 2);
                dist = Arrays.copyOf(dist, count * 2);
            }
            ids[count] = id;
            dist[count] = distSq;
            count++;
        }

        /** Starts a frame: the nearest {@code limit} requests of the last frame are granted, the requests are cleared. */
        public void frame() {
            int n = Math.min(limit, count);
            int[] pick = new int[n];
            boolean[] taken = new boolean[count];
            for (int k = 0; k < n; k++) {
                int best = -1;
                for (int i = 0; i < count; i++) if (!taken[i] && (best < 0 || dist[i] < dist[best])) best = i;
                taken[best] = true;
                pick[k] = ids[best];
            }
            granted = pick;
            count = 0;
        }

        /** True when entity {@code id} may draw its pass this frame (a new effect starts one frame after its first request). */
        public boolean allowed(int id) {
            for (int g : granted) if (g == id) return true;
            return false;
        }

        public int granted() { return granted.length; }

        public int pending() { return count; }
    }
}
