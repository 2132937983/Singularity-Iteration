// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.core.api.suit;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

/**
 * <p><b>EN:</b> Read-only view of the nano core of a player (public API since 0.1.8.5): the burst
 * reserve (0..100), the heat (0..heat cap) and the overheat lock. Values are exact on the server;
 * on the client they are the last synced values of the local player and 0 for other players.
 * <p><b>ZH:</b> 玩家纳米核心的只读视图（0.1.8.5 起的公共 API）：爆发储备（0..100）、热量（0..热量上限）与过热锁定。
 * 服务器上为精确值；客户端上为本地玩家最近同步的值，其他玩家为 0。
 * <p><b>JA:</b> プレイヤーのナノコアの読み取り専用ビュー（0.1.8.5 以降の公開 API）：バースト予備量（0..100）、熱（0..熱上限）、
 * 過熱ロック。サーバーでは正確な値。クライアントではローカルプレイヤーの最後に同期した値で、他のプレイヤーは 0。
 * <p><b>EN:</b> Since 0.1.11.2 an addon can also spend the reserve and add heat on the server: {@link #trySpendReserve},
 * {@link #addHeat}.
 * <p><b>ZH:</b> 0.1.11.2 起附属也可以在服务端花费储备、增加热量：{@link #trySpendReserve}、{@link #addHeat}。
 * <p><b>JA:</b> 0.1.11.2 以降、アドオンはサーバーで予備量を消費し、熱を加えることもできる：{@link #trySpendReserve}、{@link #addHeat}。
 */
public final class SuitNanoCore {
    private SuitNanoCore() {}

    /** EN: Implementation hook of Singularity Iteration (not for addons). ZH: 奇点迭代的实现挂钩（附属请勿使用）。JA: 奇点迭代の実装フック（アドオンは使わない）。 */
    public interface View {
        float reserve(Player player);
        float heat(Player player);
        float heatCap(Player player);
        boolean overheated(Player player);

        /** EN: Since 0.1.11.2; the default spends nothing. ZH: 0.1.11.2 起；默认不花费。JA: 0.1.11.2 以降。既定では消費しない。 */
        default boolean trySpendReserve(ServerPlayer player, float amount) { return false; }

        /** EN: Since 0.1.11.2; the default does nothing. ZH: 0.1.11.2 起；默认无操作。JA: 0.1.11.2 以降。既定では何もしない。 */
        default void addHeat(ServerPlayer player, float amount) {}
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

    /**
     * EN: Spends {@code amount} of the burst reserve of a player whose nano core is online (a worn nano piece carries
     * a unit; since 0.1.11.2). When the reserve is at least {@code amount}, the method takes it and returns true; the
     * client gets the change with the usual core sync. Else, or when {@code amount} is negative or not finite, the
     * core is offline, the call is not on the server or Singularity Iteration is not installed, nothing changes and the
     * method returns false. An amount of 0 returns true and changes nothing.
     * ZH: 花费纳米核心在线（穿戴的纳米部件装有单元）的玩家 {@code amount} 点爆发储备（0.1.11.2 起）。储备不少于 {@code amount} 时扣除并返回真，
     * 客户端经常规的核心同步得到变化。否则，或 {@code amount} 为负数或非有限数、核心离线、不在服务端、未安装奇点迭代时，不做任何改动并返回假。
     * {@code amount} 为 0 时返回真且不改动。
     * JA: ナノコアがオンライン（着用したナノパーツにユニットがある）プレイヤーのバースト予備量を {@code amount} 消費する（0.1.11.2 以降）。
     * 予備量が {@code amount} 以上なら差し引いて true を返し、クライアントには通常のコア同期で届く。それ以外、または {@code amount} が負・非有限、
     * コアがオフライン、サーバーでない、奇点迭代がないときは何も変えず false を返す。0 は何も変えず true を返す。
     */
    public static boolean trySpendReserve(ServerPlayer player, float amount) {
        if (player == null || !Float.isFinite(amount) || amount < 0 || player.level().isClientSide) return false;
        return view.trySpendReserve(player, amount);
    }

    /**
     * EN: Adds {@code amount} heat to the nano core of a player whose nano core is online (since 0.1.11.2). The heat
     * stops at {@link #heatCap}; at the cap the usual overheat rules apply (bursts lock, the camouflage ends, the
     * effect plays). Nothing occurs when {@code amount} is negative or not finite, the core is offline, the call is not
     * on the server or Singularity Iteration is not installed.
     * ZH: 给纳米核心在线的玩家增加 {@code amount} 点热量（0.1.11.2 起）。热量不超过 {@link #heatCap}；达到上限时按原有的过热规则处理
     * （爆发锁定、伪装结束、播放效果）。{@code amount} 为负数或非有限数、核心离线、不在服务端、未安装奇点迭代时无任何效果。
     * JA: ナノコアがオンラインのプレイヤーに熱を {@code amount} 加える（0.1.11.2 以降）。熱は {@link #heatCap} で止まり、上限で通常の過熱規則が
     * 働く（バーストのロック、迷彩の終了、演出）。{@code amount} が負・非有限、コアがオフライン、サーバーでない、奇点迭代がないときは何も起きない。
     */
    public static void addHeat(ServerPlayer player, float amount) {
        if (player == null || !Float.isFinite(amount) || amount < 0 || player.level().isClientSide) return;
        view.addHeat(player, amount);
    }

    /** EN: Called once by Singularity Iteration. ZH: 由奇点迭代调用一次。JA: 奇点迭代が 1 回だけ呼ぶ。 */
    public static void setView(View implementation) { view = java.util.Objects.requireNonNull(implementation); }
}
