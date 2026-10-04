package com.miophas.singularity_iteration.core.api.item;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

/**
 * 装备 HUD 提供者。
 *
 * <p>物品在被手持（主/副手）或穿戴（护甲槽）时，可提供一行显示在 actionbar 上的状态文本。
 * 客户端 {@code EquipmentHudCycler} 会统一收集所有提供者的文本，并按固定顺序循环显示，
 * 避免多个装备每 tick 各自抢占 actionbar 造成文字闪烁。
 *
 * <p>注意：本方法仅在客户端被调用，实现中可安全使用客户端逻辑。
 */
public interface IEquipmentHudProvider {

    /**
     * 获取当前要显示的 HUD 文本。
     *
     * @param stack 该物品的物品堆
     * @return 要显示的文本；返回 {@code null} 表示当前不显示
     */
    Component getEquipmentHudText(ItemStack stack);

    /**
     * Whether the line should stay on screen continuously (e.g. a jetpack while airborne). By
     * default a line is shown for a few seconds whenever its wording changes (mode switch, item
     * equipped) and then left to fade; only numbers changing does not re-show it.
     */
    default boolean keepEquipmentHudVisible(ItemStack stack, net.minecraft.world.entity.player.Player player) {
        return false;
    }
}
