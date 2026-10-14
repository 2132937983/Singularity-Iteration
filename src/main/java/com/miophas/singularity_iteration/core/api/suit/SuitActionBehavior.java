// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.core.api.suit;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

/**
 * <p><b>EN:</b> Quantum action of an addon unit (public API since 0.1.10.0). Runs on the server when
 * the wearer presses the action key the unit is assigned to. Singularity Iteration checks the unit,
 * the coherence, the decoherence lock and the cooldown first, and pays the {@link SuitActionSpec}
 * cost only when {@link #fire} returns true. A sustained action then gets {@link #sustain} once per
 * tick. It ends when sustain returns false, the key is pressed again, the coherence runs out or
 * the unit stops; {@link #end} follows in each case.
 * <p><b>ZH:</b> 附属单元的量子行动（0.1.10.0 起的公共 API）。穿戴者按下该单元所分配的行动键时在服务器运行。奇点迭代会先检查单元、
 * 相干度、退相干锁定与冷却，只有在 {@link #fire} 返回真时才支付 {@link SuitActionSpec} 的消耗。持续行动之后每 tick 调用一次
 * {@link #sustain}，直到它返回假、再次按键、相干度耗尽或单元停止；每种情况下随后都会调用 {@link #end}。
 * <p><b>JA:</b> アドオンユニットの量子アクション（0.1.10.0 以降の公開 API）。着用者がユニットの割り当てられたアクションキーを
 * 押したとき、サーバーで動く。奇点迭代は先にユニット・コヒーレンス・デコヒーレンスのロック・クールダウンを確認し、{@link #fire}
 * が真を返したときだけ {@link SuitActionSpec} のコストを払う。持続アクションはその後、{@link #sustain} が偽を返す、キーを再度押す、
 * コヒーレンスが尽きる、ユニットが止まる、のいずれかまで毎 tick {@link #sustain} を受け、どの場合も最後に {@link #end} が呼ばれる。
 */
@FunctionalInterface
public interface SuitActionBehavior {
    /**
     * @param player the wearer (server side)
     * @param piece  the worn piece that carries the unit
     * @param input  horizontal movement input of the wearer in world space (length 0..1; zero when no key is held)
     * @return true when the action fired
     */
    boolean fire(Player player, ItemStack piece, Vec3 input);

    /**
     * EN: Each tick of a running sustained action; false ends it. ZH: 持续行动运行中的每个 tick；返回假则结束。
     * JA: 持続アクションの実行中の毎 tick。偽を返すと終わる。
     */
    default boolean sustain(Player player, ItemStack piece) { return true; }

    /** EN: A sustained action ends. ZH: 持续行动结束。JA: 持続アクションが終わる。 */
    default void end(Player player, ItemStack piece) { }
}
