// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.core.api.suit;

import net.minecraft.world.entity.player.Player;

/**
 * <p><b>EN:</b> Read-only view of the nano core of a player (public API since 0.1.8.5): the burst
 * reserve (0..100), the heat (0..heat cap) and the overheat lock. Values are exact on the server;
 * on the client they are the last synced values of the local player and 0 for other players.
 * <p><b>ZH:</b> 玩家纳米核心的只读视图（0.1.8.5 起的公共 API）：爆发储备（0..100）、热量（0..热量上限）与过热锁定。
 * 服务器上为精确值；客户端上为本地玩家最近同步的值，其他玩家为 0。
 * <p><b>JA:</b> プレイヤーのナノコアの読み取り専用ビュー（0.1.8.5 以降の公開 API）：バースト予備量（0..100）、熱（0..熱上限）、
 * 過熱ロック。サーバーでは正確な値。クライアントではローカルプレイヤーの最後に同期した値で、他のプレイヤーは 0。
 */
public final class SuitNanoCore {
    private SuitNanoCore() {}

    /** EN: Implementation hook of Singularity Iteration (not for addons). ZH: 奇点迭代的实现挂钩（附属请勿使用）。JA: 奇点迭代の実装フック（アドオンは使わない）。 */
    public interface View {
        float reserve(Player player);
        float heat(Player player);
        float heatCap(Player player);
        boolean overheated(Player player);
    }

    private static volatile View view = new View() {
        @Override public float reserve(Player player) { return 0; }
        @Override public float heat(Player player) { return 0; }
        @Override public float heatCap(Player player) { return 100; }
        @Override public boolean overheated(Player player) { return false; }
    };

    /** EN: Burst reserve 0..100. ZH: 爆发储备 0..100。JA: バースト予備量 0..100。 */
    public static float reserve(Player player) { return view.reserve(player); }

    /** EN: Heat 0..{@link #heatCap}. ZH: 热量 0..{@link #heatCap}。JA: 熱 0..{@link #heatCap}。 */
    public static float heat(Player player) { return view.heat(player); }

    /** EN: Heat cap (100 without heat-sink units). ZH: 热量上限（无散热单元时为 100）。JA: 熱上限（放熱ユニットなしで 100）。 */
    public static float heatCap(Player player) { return view.heatCap(player); }

    /** EN: True while bursts are locked by overheat. ZH: 因过热锁定爆发时为真。JA: 過熱でバーストがロック中なら真。 */
    public static boolean overheated(Player player) { return view.overheated(player); }

    /** EN: Called once by Singularity Iteration. ZH: 由奇点迭代调用一次。JA: 奇点迭代が 1 回だけ呼ぶ。 */
    public static void setView(View implementation) { view = java.util.Objects.requireNonNull(implementation); }
}
