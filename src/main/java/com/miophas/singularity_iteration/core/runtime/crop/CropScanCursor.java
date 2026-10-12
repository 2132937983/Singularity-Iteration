// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.core.runtime.crop;

import net.minecraft.core.BlockPos;

/**
 * <p><b>EN:</b> Scan order shared by crop-tending machines (Cropmatron, Crop Harvester): one
 * position per operation over a 9 x 3 x 9 box around the machine, x fastest, then z, then y;
 * y spans -1..1 and x/z span -4..4. The first operation after placement visits (-3, -1, -4), like
 * IC2 2.8. The cursor is one index, so it saves as one int (saved index 0 is the fresh state).
 * Older saves used an 11 x 3 x 11 index; they are folded into range, which only moves the cursor
 * to another valid position.
 * <p><b>ZH:</b> 作物照料机器（作物监护机、作物收割机）共用的扫描顺序：每次操作访问机器周围 9 × 3 × 9 区域中的一个位置，
 * x 最快，其次 z，最后 y；y 范围 -1..1，x/z 范围 -4..4。放置后的第一次操作访问 (-3, -1, -4)，与 IC2 2.8 相同。
 * 游标是一个索引，保存为一个 int（保存值 0 为初始状态）。旧存档使用 11 × 3 × 11 的索引，会被折回范围内，只是把游标移到另一个有效位置。
 * <p><b>JA:</b> 作物を世話する機械（作物管理機・作物収穫機）が共有する走査順。1 回の操作で機械の周囲 9 × 3 × 9 の 1 か所を訪れる。
 * x が最も速く、次に z、最後に y。y は -1..1、x/z は -4..4。設置後の最初の操作は (-3, -1, -4) を訪れる（IC2 2.8 と同じ）。
 * カーソルは 1 つの番号なので int 1 つで保存する（保存値 0 が初期状態）。旧セーブは 11 × 3 × 11 の番号を使っていたが、範囲内に折り返す。
 * これはカーソルを別の有効な位置へ動かすだけである。
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
