// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.core.runtime.crop;

import net.minecraft.core.BlockPos;

/**
 * Scan order shared by crop-tending machines (Cropmatron, Crop Harvester): one
 * position per operation over a 9 x 3 x 9 box around the machine, x fastest,
 * then z, then y; y spans -1..1 and x/z span -4..4. The first operation after
 * placement visits (-3, -1, -4), matching IC2 2.8's observable behaviour.
 *
 * <p>The cursor is a single index so it persists as one int (saved index 0 is
 * the fresh state). Older saves used an 11 x 3 x 11 index; they are folded into
 * range, which only moves the cursor to another valid position.
 */
public final class CropScanCursor {
    public static final int RADIUS = 4;
    public static final int WIDTH = 2 * RADIUS + 1;
    public static final int VOLUME = WIDTH * WIDTH * 3;

    private CropScanCursor() {}

    /** Advances a saved index by one operation. */
    public static int next(int index) {
        return Math.floorMod(index + 1, VOLUME);
    }

    /** Normalises an index read from a save. */
    public static int restore(int saved) {
        return Math.floorMod(saved, VOLUME);
    }

    /** World position scanned at this index, relative to the machine. */
    public static BlockPos target(BlockPos machine, int index) {
        return machine.offset(index % WIDTH - RADIUS, index / (WIDTH * WIDTH) - 1, index / WIDTH % WIDTH - RADIUS);
    }
}
