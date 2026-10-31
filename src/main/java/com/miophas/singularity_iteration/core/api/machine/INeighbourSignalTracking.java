// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.core.api.machine;

/**
 * <p><b>EN:</b> A machine block that tells its block entity when a neighbour changes (public API since 0.1.10.38). An
 * {@code AbstractProcessingMachineBlockEntity} on such a block keeps its redstone input from those calls
 * ({@code neighbourSignalChanged()}) instead of reading the six neighbours every tick, and reads it again at most every 20
 * ticks as a safety net. The block must call {@code neighbourSignalChanged()} from {@code neighborChanged} (and an override of
 * {@code neighborChanged} must call {@code super}). A machine on another block reads its input every tick, as before.
 * <p><b>ZH:</b> 邻居变化时通知其方块实体的机器方块（0.1.10.38 起的公共 API）。位于此类方块上的 {@code AbstractProcessingMachineBlockEntity}
 * 根据这些通知（{@code neighbourSignalChanged()}）保存红石输入，不再每 tick 读取六个邻居，并最多每 20 tick 重新读取一次作为保险。
 * 方块必须在 {@code neighborChanged} 中调用 {@code neighbourSignalChanged()}（覆写 {@code neighborChanged} 时必须调用 {@code super}）。
 * 其他方块上的机器仍每 tick 读取输入。
 * <p><b>JA:</b> 隣接ブロックが変わったときにブロックエンティティへ知らせる機械ブロック（0.1.10.38 以降の公開 API）。このブロック上の
 * {@code AbstractProcessingMachineBlockEntity} はその通知（{@code neighbourSignalChanged()}）でレッドストーン入力を保持し、毎 tick 6 方向を
 * 読まない。保険として最大 20 tick ごとに読み直す。ブロックは {@code neighborChanged} から {@code neighbourSignalChanged()} を呼ぶこと
 * （{@code neighborChanged} を上書きするなら {@code super} を呼ぶこと）。ほかのブロック上の機械は従来どおり毎 tick 読む。
 */
public interface INeighbourSignalTracking {
}
