// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.builder;

/**
 * <p><b>EN:</b> Packed block-state indices of the Litematica format (0.1.8.0). Unlike the vanilla chunk
 * format, an entry can span two longs. No Minecraft dependency (a standalone test checks it).
 * <p><b>ZH:</b> Litematica 格式中打包的方块状态索引（0.1.8.0）。与原版区块格式不同，一个条目可以跨越两个 long。不依赖 Minecraft（由独立测试检查）。
 * <p><b>JA:</b> Litematica 形式で詰め込まれたブロックステート番号（0.1.8.0）。バニラのチャンク形式と違い、1 項目が 2 つの long にまたがることがある。
 * Minecraft に依存しない（単体テストで検査する）。
 */
public final class LitematicBits {
    private LitematicBits() {}

    /** EN: Bits per entry for a palette of {@code size} states (at least 2). ZH: 调色板含 {@code size} 个状态时每个条目的位数（至少 2）。JA: パレットが {@code size} 状態のときの 1 項目のビット数（最低 2）。 */
    public static int bitsFor(int size) {
        return Math.max(2, 32 - Integer.numberOfLeadingZeros(Math.max(1, size - 1)));
    }

    /**
     * <p><b>EN:</b> Entry {@code index} of {@code data} with {@code bits} bits per entry. When the entry
     * crosses a long boundary, the low bits come from {@code data[word]} and the high bits from {@code data[word + 1]}.
     * <p><b>ZH:</b> 读取每条目 {@code bits} 位的 {@code data} 中第 {@code index} 个条目。条目跨越 long 边界时，低位取自 {@code data[word]}，高位取自 {@code data[word + 1]}。
     * <p><b>JA:</b> 1 項目 {@code bits} ビットの {@code data} から {@code index} 番目を読む。項目が long の境界をまたぐときは、下位ビットを
     * {@code data[word]}、上位ビットを {@code data[word + 1]} から取る。
     */
    public static int get(long[] data, int bits, long index) {
        long mask = (1L << bits) - 1;
        long start = index * bits;
        int word = (int) (start >> 6), end = (int) (((index + 1) * bits - 1) >> 6), off = (int) (start & 63);
        long v = word == end ? (data[word] >>> off) & mask : ((data[word] >>> off) | (data[end] << (64 - off))) & mask;
        return (int) v;
    }

    /** EN: True when {@code data} holds {@code volume} entries of {@code bits} bits. ZH: {@code data} 能容纳 {@code volume} 个 {@code bits} 位条目时为真。JA: {@code data} が {@code bits} ビットの項目を {@code volume} 個保持できれば真。 */
    public static boolean fits(long[] data, int bits, long volume) {
        return volume * bits <= (long) data.length * 64;
    }
}
