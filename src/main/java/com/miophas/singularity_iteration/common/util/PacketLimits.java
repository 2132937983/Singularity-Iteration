// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.util;

import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.DecoderException;
import net.minecraft.network.VarInt;

/**
 * Bounds for list counts in payload codecs (0.1.7.36).
 *
 * <p>Why: a decoder that clamps a count ({@code Math.min(max, readVarInt())}) leaves the bytes of
 * the extra entries unread. The next field then reads those bytes and decodes garbage. A
 * negative count made {@code new int[n]} throw a raw {@code NegativeArraySizeException}.
 *
 * <p>Contract:
 * <ul>
 *   <li>The encoder writes {@code min(size, max)} with {@link #writeCount} and then writes
 *       exactly that number of entries.</li>
 *   <li>The decoder reads the count with {@link #readCount}. A count below 0 or above
 *       {@code max} throws {@link DecoderException}. Netty then closes the connection with a
 *       clear message.</li>
 * </ul>
 */
public final class PacketLimits {
    private PacketLimits() {}

    /** Writes {@code min(size, max)} and returns it. The caller writes that many entries. */
    public static int writeCount(ByteBuf buf, int size, int max) {
        int n = Math.max(0, Math.min(size, max));
        VarInt.write(buf, n);
        return n;
    }

    /** Reads a count in {@code 0..max}. Throws {@link DecoderException} for any other value. */
    public static int readCount(ByteBuf buf, int max, String what) {
        int n = VarInt.read(buf);
        if (n < 0 || n > max) throw new DecoderException("Bad " + what + " count " + n + " (max " + max + ")");
        return n;
    }
}
