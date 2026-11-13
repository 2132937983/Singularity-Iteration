// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.core.api.suit;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

/**
 * <p><b>EN:</b> Read-only view of the quantum core of a player (public API since 0.1.10.0): the
 * coherence (0..maximum), its maximum (100, more with coherence capacitors) and the decoherence
 * lock. Values are exact on the server; on the client they are the last synced values of the local
 * player and 0 for other players.
 * <p><b>ZH:</b> 玩家量子核心的只读视图（0.1.10.0 起的公共 API）：相干度（0..上限）、上限（100，装有相干电容器时更多）与退相干锁定。
 * 服务器上为精确值；客户端上为本地玩家最近同步的值，其他玩家为 0。
 * <p><b>JA:</b> プレイヤーの量子コアの読み取り専用ビュー（0.1.10.0 以降の公開 API）：コヒーレンス（0..上限）、上限
 * （100、コヒーレンスキャパシタで増える）、デコヒーレンスのロック。サーバーでは正確な値。クライアントではローカルプレイヤーの
 * 最後に同期した値で、他のプレイヤーは 0。
 * <p><b>EN:</b> Since 0.1.11.2 an addon can also spend coherence on the server: {@link #trySpendCoherence}.
 * <p><b>ZH:</b> 0.1.11.2 起附属也可以在服务端花费相干度：{@link #trySpendCoherence}。
 * <p><b>JA:</b> 0.1.11.2 以降、アドオンはサーバーでコヒーレンスを消費することもできる：{@link #trySpendCoherence}。
 */
public final class SuitQuantumCore {
    private SuitQuantumCore() {}

    /** EN: Implementation hook of Singularity Iteration (not for addons). ZH: 奇点迭代的实现挂钩（附属请勿使用）。JA: 奇点迭代の実装フック（アドオンは使わない）。 */
    public interface View {
        float coherence(Player player);
        float coherenceMax(Player player);
        boolean decoherent(Player player);

        /** EN: Since 0.1.11.2; the default spends nothing. ZH: 0.1.11.2 起；默认不花费。JA: 0.1.11.2 以降。既定では消費しない。 */
        default boolean trySpendCoherence(ServerPlayer player, float amount) { return false; }
    }

    private static volatile View view = new View() {
        @Override public float coherence(Player player) { return 0; }
        @Override public float coherenceMax(Player player) { return 100; }
        @Override public boolean decoherent(Player player) { return false; }
    };

    /** EN: Coherence 0..{@link #coherenceMax}. ZH: 相干度 0..{@link #coherenceMax}。JA: コヒーレンス 0..{@link #coherenceMax}。 */
    public static float coherence(Player player) { return view.coherence(player); }

    /** EN: Coherence maximum (100 without capacitors). ZH: 相干度上限（无电容器时为 100）。JA: コヒーレンス上限（キャパシタなしで 100）。 */
    public static float coherenceMax(Player player) { return view.coherenceMax(player); }

    /** EN: True while actions are locked by decoherence. ZH: 因退相干锁定行动时为真。JA: デコヒーレンスでアクションがロック中なら真。 */
    public static boolean decoherent(Player player) { return view.decoherent(player); }

    /**
     * EN: Spends {@code amount} coherence of a player who wears the quantum suit (since 0.1.11.2). When the coherence
     * is at least {@code amount} and the core is not decoherent, the method takes the amount, delays the regeneration
     * the same as an action, syncs the change to the client and returns true. Coherence that goes to 0 decoheres the
     * core (the usual rule). Else, or when {@code amount} is negative or not finite, the player wears no quantum piece,
     * the call is not on the server or Singularity Iteration is not installed, nothing changes and the method returns
     * false. An amount of 0 returns true and changes nothing.
     * ZH: 花费穿量子套装的玩家 {@code amount} 点相干度（0.1.11.2 起）。相干度不少于 {@code amount} 且未退相干时扣除，像行动一样推迟回复，
     * 同步到客户端并返回真；相干度降到 0 时按原规则退相干。否则，或 {@code amount} 为负数或非有限数、玩家没有穿量子部件、不在服务端、
     * 未安装奇点迭代时，不做任何改动并返回假。{@code amount} 为 0 时返回真且不改动。
     * JA: 量子スーツを着たプレイヤーのコヒーレンスを {@code amount} 消費する（0.1.11.2 以降）。コヒーレンスが {@code amount} 以上で
     * デコヒーレンス中でなければ差し引き、アクションと同じく回復を遅らせ、クライアントへ同期して true を返す。0 になると通常どおりデコヒーレンスする。
     * それ以外、または {@code amount} が負・非有限、量子パーツを着ていない、サーバーでない、奇点迭代がないときは何も変えず false を返す。
     * 0 は何も変えず true を返す。
     */
    public static boolean trySpendCoherence(ServerPlayer player, float amount) {
        if (player == null || !Float.isFinite(amount) || amount < 0 || player.level().isClientSide) return false;
        return view.trySpendCoherence(player, amount);
    }

    /** EN: Called once by Singularity Iteration. ZH: 由奇点迭代调用一次。JA: 奇点迭代が 1 回だけ呼ぶ。 */
    public static void setView(View implementation) { view = java.util.Objects.requireNonNull(implementation); }
}
