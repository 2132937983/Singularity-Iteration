// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.core.api.energy.grid;

/**
 * <p><b>EN:</b> Implemented by the block entity of a generator that declares its {@link DispatchClass} (public API
 * since 0.1.10.22). The grid reads the class in every round, so it may change (for example with the fuel in a slot).
 * <p><b>ZH:</b> 由声明自身 {@link DispatchClass} 的发电机方块实体实现（0.1.10.22 起的公共 API）。电网每一轮读取该类别，因此它可以变化
 * （例如随燃料槽中的燃料变化）。
 * <p><b>JA:</b> 自分の {@link DispatchClass} を申告する発電機のブロックエンティティが実装する（0.1.10.22 以降の公開 API）。電力網は毎ラウンド
 * 種別を読むので、変わってもよい（例えば燃料スロットの燃料に応じて）。
 */
public interface IGridDispatch {
    /**
     * <p><b>EN:</b> The dispatch class of this generator now. Must not be null.
     * <p><b>ZH:</b> 此发电机当前的调度类别，不能为 null。
     * <p><b>JA:</b> この発電機の現在の給電種別。null は不可。
     */
    DispatchClass dispatchClass();
}
