package com.singularity_iteration.mio_icif.api.item;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * 增强覆盖层提供者接口。
 * <p>
 * 对应 IC2 1.12.2 的 {@code IEnhancedOverlayProvider}。
 * 实现此接口的物品可以在手持时为方块提供增强覆盖层，
 * 显示额外的信息或交互界面。
 */
public interface IEnhancedOverlayProvider {

    /**
     * 检查此物品是否为指定方块提供增强覆盖层。
     *
     * @param world 世界
     * @param pos 方块位置
     * @param side 看向的面
     * @param player 玩家
     * @param stack 手持物品
     * @return 如果提供增强覆盖层则返回 true
     */
    boolean providesEnhancedOverlay(Level world, BlockPos pos, Direction side, Player player, ItemStack stack);
}
