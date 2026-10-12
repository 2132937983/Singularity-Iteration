// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.machine;

/**
 * <p><b>EN:</b> A block entity that syncs its own "running" flag to the client (in its update
 * tag). The client sound and animation code reads this flag when the block has no run-state
 * property and no status lamp. Implement it only when the flag is already synced; this
 * interface adds no network traffic.
 *
 * <p><b>ZH:</b> 自行把“运行中”标志同步到客户端（写在更新标签中）的方块实体。方块既没有运行状态属性也没有状态灯时，
 * 客户端的音效与动画代码读取此标志。仅在该标志已同步时实现本接口；本接口不增加网络流量。
 *
 * <p><b>JA:</b> 自分の「稼働中」フラグを（更新タグで）クライアントへ同期するブロックエンティティ。ブロックに稼働状態の
 * プロパティもステータスランプもないとき、クライアントの音とアニメーションのコードがこのフラグを読む。
 * フラグがすでに同期されている場合だけ実装する。このインターフェースは通信量を増やさない。
 */
public interface ClientRunState {
    /**
     * <p><b>EN:</b> True while the machine runs, as last synced to this side.
     * <p><b>ZH:</b> 机器运行中时为真（以最后一次同步到本端的值为准）。
     * <p><b>JA:</b> 機械が稼働中なら真（この側へ最後に同期された値）。
     */
    boolean clientRunning();
}
