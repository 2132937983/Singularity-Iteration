// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.blockentity.kuentity.kugenerator;

/**
 * 0.1.11.9: the last {@link #SAMPLES} wind samples of a wind machine (bearing and speed), packed for the screen's wind
 * rose. Not saved: a reloaded machine starts a fresh rose.
 */
public final class WindRoseBuffer {
    public static final int SAMPLES = 64;
    /** Menu ints the rose takes: 4 for the share per 16 bearings, 4 for their mean wind / 2 (one byte each). */
    public static final int DATA_INTS = 8;

    private final byte[] bearing = new byte[SAMPLES];
    private final short[] wind = new short[SAMPLES];
    private int count, next;

    public void add(int bearingDegrees, int windMcu) {
        bearing[next] = (byte) Math.round(Math.floorMod(bearingDegrees, 360) * 256 / 360F);
        wind[next] = (short) Math.max(0, Math.min(Short.MAX_VALUE, windMcu));
        next = (next + 1) % SAMPLES;
        count = Math.min(SAMPLES, count + 1);
    }

    /** Menu int {@code i} (0 .. 7) of the rose. */
    public int data(int i) {
        if (i < 0 || i >= DATA_INTS) return 0;
        int[] n = new int[16], sum = new int[16];
        for (int k = 0; k < count; k++) {
            int b = Math.floorMod(Math.round((bearing[k] & 0xFF) * 360 / 256F / 22.5F), 16);
            n[b]++;
            sum[b] += wind[k];
        }
        int v = 0;
        for (int k = 0; k < 4; k++) {
            int bin = (i % 4) * 4 + k;
            int b = i < 4 ? Math.min(255, n[bin] * 255 / Math.max(1, count)) : n[bin] == 0 ? 0 : Math.min(255, sum[bin] / n[bin] / 2);
            v |= b << (8 * k);
        }
        return v;
    }
}
