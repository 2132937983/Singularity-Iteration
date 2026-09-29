package com.miophas.singularity_iteration.core.api.item;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * 喷气背包 tick 接口。
 * <p>供 Curios 适配器在装了 Curios 时调用飞行逻辑，
 * 使喷气背包类本身不依赖 Curios API。
 */
public interface IJetpackTicker {

    /**
     * 执行一 tick 的喷气背包飞行逻辑。
     *
     * @param player 玩佩玩家
     * @param stack  喷气背包物品堆
     * @param level  世界
     */
    void tickJetpack(Player player, ItemStack stack, Level level);
}
