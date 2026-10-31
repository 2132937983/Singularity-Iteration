// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.core.api.energy.grid;

/**
 * <p><b>EN:</b> Priority of a load or a storage block when the grid has too little energy (public API since
 * 0.1.10.22). The grid serves the critical tier first, then the normal tier, then the deferrable tier: a shortage
 * stops the deferrable loads first and the critical loads last. The player sets the priority at the energy
 * management terminal; the default is {@link #NORMAL}. With only normal loads, the grid shares as before. Priority
 * needs the fair distribution (the default); with {@code distributionMode = legacy} in the server config the grid
 * walks the loads in a random order and ignores it.
 * <p><b>ZH:</b> 电力不足时用电器或储能的优先级（0.1.10.22 起的公共 API）。电网先满足重要级，再满足普通级，最后满足可延后级：
 * 电力不足时先停可延后的负荷，最后才停重要负荷。玩家在能源管理终端设置优先级，默认为 {@link #NORMAL}。全部为普通级时，分配与此前相同。
 * 优先级需要公平分配（默认）；服务器配置为 {@code distributionMode = legacy} 时，电网按随机顺序遍历负荷，忽略优先级。
 * <p><b>JA:</b> 電力が足りないときの負荷や蓄電の優先度（0.1.10.22 以降の公開 API）。電力網は重要、次に通常、最後に後回しの順に満たす。
 * 電力不足では後回しの負荷から止まり、重要な負荷は最後に止まる。プレイヤーはエネルギー管理端末で優先度を設定する。既定は {@link #NORMAL}。
 * すべて通常なら、配分は以前と同じ。優先度には公平配分（既定）が必要で、サーバー設定が {@code distributionMode = legacy} のとき
 * 電力網は負荷をランダムな順に回り、優先度を無視する。
 */
public enum LoadPriority {
    /**
     * <p><b>EN:</b> Critical: served first (reactor coolant injectors, chunk loaders, teleporters).
     * <p><b>ZH:</b> 重要：最先满足（反应堆冷却剂注入器、区块加载器、传送机）。
     * <p><b>JA:</b> 重要：最初に満たす（原子炉の冷却材注入器、チャンクローダー、テレポーター）。
     */
    CRITICAL,
    /**
     * <p><b>EN:</b> Normal: the default.
     * <p><b>ZH:</b> 普通：默认值。
     * <p><b>JA:</b> 通常：既定値。
     */
    NORMAL,
    /**
     * <p><b>EN:</b> Deferrable: served last (mass fabricators, chargers, storage that only fills up).
     * <p><b>ZH:</b> 可延后：最后满足（物质生成机、充电器、只用于充满的储能）。
     * <p><b>JA:</b> 後回し：最後に満たす（物質生成機、充電器、満たすだけの蓄電）。
     */
    DEFERRABLE;

    /**
     * <p><b>EN:</b> The next priority in the order critical, normal, deferrable, critical (for a cycle button).
     * <p><b>ZH:</b> 按 重要、普通、可延后、重要 的顺序返回下一个优先级（用于循环按钮）。
     * <p><b>JA:</b> 重要、通常、後回し、重要の順で次の優先度を返す（切り替えボタン用）。
     */
    public LoadPriority next() {
        return values()[(ordinal() + 1) % values().length];
    }
}
