// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.builder;

/**
 * Packed block state indices of the Litematica format (0.1.8.0). Unlike the vanilla chunk
 * format, an entry can span two longs. No Minecraft dependency (tested on its own).
 */
public final class LitematicBits {
    private LitematicBits() {}

    /** Bits per entry for a palette of {@code size} states: at least 2. */
    public static int bitsFor(int size) {
        return Math.max(2, 32 - Integer.numberOfLeadingZeros(Math.max(1, size - 1)));
    }

    /** Entry {@code index} of {@code data} with {@code bits} bits per entry. */
    public static int get(long[] data, int bits, long index) {
        long mask = (1L << bits) - 1;
        long start = index * bits;
        int word = (int) (start >> 6), end = (int) (((index + 1) * bits - 1) >> 6), off = (int) (start & 63);
        long v = word == end ? (data[word] >>> off) & mask : ((data[word] >>> off) | (data[end] << (64 - off))) & mask;
        return (int) v;
    }

    /** True when {@code data} holds {@code volume} entries of {@code bits} bits. */
    public static boolean fits(long[] data, int bits, long volume) {
        return volume * bits <= (long) data.length * 64;
    }
}
