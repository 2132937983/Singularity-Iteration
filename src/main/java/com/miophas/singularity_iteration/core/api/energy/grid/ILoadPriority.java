// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.core.api.energy.grid;

/**
 * <p><b>EN:</b> Implemented by the block entity of a load or storage block that has a {@link LoadPriority} (public API
 * since 0.1.10.22). The energy blocks of Singularity Iteration implement it and save the value. The grid reads the
 * value in every round; a change takes effect in the next round without a topology change.
 * <p><b>ZH:</b> 由带有 {@link LoadPriority} 的用电器或储能方块实体实现（0.1.10.22 起的公共 API）。奇点迭代的能源方块实现它并保存该值。
 * 电网每一轮读取该值；修改在下一轮生效，不改变拓扑。
 * <p><b>JA:</b> {@link LoadPriority} を持つ負荷や蓄電のブロックエンティティが実装する（0.1.10.22 以降の公開 API）。奇点迭代のエネルギーブロックは
 * これを実装し、値を保存する。電力網は毎ラウンド値を読む。変更は次のラウンドから効き、トポロジーは変わらない。
 */
public interface ILoadPriority {
    /**
     * <p><b>EN:</b> The current priority. Must not be null.
     * <p><b>ZH:</b> 当前的优先级，不能为 null。
     * <p><b>JA:</b> 現在の優先度。null は不可。
     */
    LoadPriority loadPriority();

    /**
     * <p><b>EN:</b> Sets the priority (server side) and saves it with the block.
     * <p><b>ZH:</b> 设置优先级（服务器端），并随方块保存。
     * <p><b>JA:</b> 優先度を設定し（サーバー側）、ブロックと一緒に保存する。
     */
    void setLoadPriority(LoadPriority priority);
}
