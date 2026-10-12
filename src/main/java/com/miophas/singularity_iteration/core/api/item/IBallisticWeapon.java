// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.core.api.item;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * <p><b>EN:</b> A ranged weapon item that tells the ballistic calculator how its projectile flies
 * (public API since 0.1.8.0). The suit fire-control HUD, the Smart Trajectory Guidance module and
 * the Wingman drones use it. For an item of a different mod, register a provider in
 * {@link BallisticProfiles} instead.
 * <p><b>ZH:</b> 告诉弹道计算器其弹射物如何飞行的远程武器物品（0.1.8.0 起的公共 API）。套装火控 HUD、智能弹道诱导单元与僚机无人机使用它。
 * 对其他模组的物品，请改为在 {@link BallisticProfiles} 中注册提供器。
 * <p><b>JA:</b> 弾の飛び方を弾道計算機に伝える遠距離武器アイテム（0.1.8.0 以降の公開 API）。スーツの火器管制 HUD、スマート弾道誘導ユニット、
 * ウィングマンのドローンが使う。他の MOD のアイテムには、代わりに {@link BallisticProfiles} へプロバイダーを登録する。
 */
public interface IBallisticWeapon {
    /**
     * <p><b>EN:</b> The launch profile of a shot fired now, or null when the weapon cannot fire now.
     * <p><b>ZH:</b> 此刻射击的发射参数；武器此刻无法射击时返回 null。
     * <p><b>JA:</b> 今撃った場合の発射パラメータ。今撃てないなら null。
     *
     * @param stack    the weapon
     * @param shooter  the entity that holds it
     * @param useTicks ticks of use so far (draw or charge time; 0 when the item is not in use)
     * @return the profile, or null when the weapon cannot fire now
     */
    @Nullable
    BallisticProfile ballistics(ItemStack stack, LivingEntity shooter, int useTicks);
}
