// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.core.api.suit;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * <p><b>EN:</b> What an addon unit does (public API since 0.1.8.0). It runs on the server main thread.
 * <p><b>ZH:</b> 附属单元的行为（0.1.8.0 起的公共 API）。在服务器主线程上运行。
 * <p><b>JA:</b> アドオンユニットの動作（0.1.8.0 以降の公開 API）。サーバーのメインスレッドで動く。
 */
@FunctionalInterface
public interface SuitModuleBehavior {
    /** EN: Does nothing (a unit that other code only queries with isActive). ZH: 不做任何事（只供其他代码用 isActive 查询的单元）。JA: 何もしない（他のコードが isActive で問い合わせるだけのユニット）。 */
    SuitModuleBehavior NONE = (player, piece) -> { };

    /**
     * <p><b>EN:</b> Called once each server tick for each player on whom the unit is active (installed,
     * switched on, powered, visor present when needed). A behavior that throws is logged, and the
     * unit stays off for that tick.
     * <p><b>ZH:</b> 每个服务器 tick，对单元生效（已安装、已开启、有能量、需要时有面罩）的每位玩家调用一次。抛出异常的行为会被记录日志，该 tick 内单元保持关闭。
     * <p><b>JA:</b> ユニットが有効（取り付け済み、オン、電力あり、必要ならバイザー）な各プレイヤーについて、毎サーバー tick 1 回呼ばれる。
     * 例外を投げた動作はログに記録され、その tick の間ユニットはオフのまま。
     *
     * @param player the wearer
     * @param piece  the worn suit piece that carries the unit (do not replace it; change its data only)
     */
    void activeTick(Player player, ItemStack piece);
}
