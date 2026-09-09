package com.singularity_iteration.mio_icif.api.tool;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * 可开关电力工具接口。
 * 实现此接口的电力工具可以通过 G 键切换开关状态，
 * 例如：纳米剑、等离子射线枪等。
 */
public interface ToggleableElectricTool {

    /**
     * 切换工具的开关状态
     * @param stack 工具物品堆
     * @param player 玩家
     */
    void toggleActive(ItemStack stack, Player player);
}
