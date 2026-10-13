// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.core.api.suit;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

/**
 * <p><b>EN:</b> Burst ability of an addon nano unit (public API since 0.1.8.5). Runs on the server
 * when the wearer presses the burst key the unit is assigned to. Singularity Iteration checks the
 * unit, the reserve, the heat and the cooldown first, and pays the {@link SuitBurstSpec} costs only
 * when {@link #fire} returns true.
 * <p><b>ZH:</b> 附属纳米单元的爆发能力（0.1.8.5 起的公共 API）。穿戴者按下该单元所分配的爆发键时在服务器运行。
 * 奇点迭代会先检查单元、储备、热量与冷却，并且只有在 {@link #fire} 返回真时才支付 {@link SuitBurstSpec} 的消耗。
 * <p><b>JA:</b> アドオンのナノユニットのバースト能力（0.1.8.5 以降の公開 API）。着用者がユニットの割り当てられた
 * バーストキーを押したとき、サーバーで動く。奇点迭代は先にユニット・予備量・熱・クールダウンを確認し、
 * {@link #fire} が真を返したときだけ {@link SuitBurstSpec} のコストを払う。
 */
@FunctionalInterface
public interface SuitBurstBehavior {
    /**
     * @param player the wearer (server side)
     * @param piece  the worn piece that carries the unit
     * @param input  horizontal movement input of the wearer in world space (length 0..1; zero when no key is held)
     * @return true when the burst fired
     */
    boolean fire(Player player, ItemStack piece, Vec3 input);
}
