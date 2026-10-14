// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.core.api.suit;

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
 */
public final class SuitQuantumCore {
    private SuitQuantumCore() {}

    /** EN: Implementation hook of Singularity Iteration (not for addons). ZH: 奇点迭代的实现挂钩（附属请勿使用）。JA: 奇点迭代の実装フック（アドオンは使わない）。 */
    public interface View {
        float coherence(Player player);
        float coherenceMax(Player player);
        boolean decoherent(Player player);
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

    /** EN: Called once by Singularity Iteration. ZH: 由奇点迭代调用一次。JA: 奇点迭代が 1 回だけ呼ぶ。 */
    public static void setView(View implementation) { view = java.util.Objects.requireNonNull(implementation); }
}
