package com.singularity_iteration.mio_icif.api.tool;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

import java.util.Collection;
import java.util.List;

/**
 * 扳手交互接口。
 * <p>
 * 对应 IC2 1.12.2 的 {@code IWrenchable}。
 * 实现此接口的方块可以使用扳手进行交互，包括旋转方向和拆卸。
 * <p>
 * 本接口为所有方法提供了基于方块 {@code FACING}/{@code HORIZONTAL_FACING}
 * 属性的通用默认实现，机器方块只需声明 {@code implements IWrenchable} 即可被
 * API 统一管理，无需自行编写旋转/拆卸逻辑。需要特殊行为的方块可覆盖对应方法。
 */
public interface IWrenchable {

    /**
     * 获取方块当前的朝向。
     *
     * @param world 世界
     * @param pos   方块位置
     * @return 当前朝向
     */
    default Direction getFacing(Level world, BlockPos pos) {
        BlockState state = world.getBlockState(pos);
        if (state.hasProperty(BlockStateProperties.FACING)) {
            return state.getValue(BlockStateProperties.FACING);
        }
        if (state.hasProperty(BlockStateProperties.HORIZONTAL_FACING)) {
            return state.getValue(BlockStateProperties.HORIZONTAL_FACING);
        }
        return Direction.NORTH;
    }

    /**
     * 设置方块的朝向。
     *
     * @param world  世界
     * @param pos    方块位置
     * @param facing 新朝向
     * @param player 执行操作的玩家
     * @return 如果设置成功则返回 true
     */
    default boolean setFacing(Level world, BlockPos pos, Direction facing, Player player) {
        if (!canSetFacing(world, pos, facing)) {
            return false;
        }
        BlockState state = world.getBlockState(pos);
        BlockState newState = null;
        if (state.hasProperty(BlockStateProperties.FACING)) {
            if (state.getValue(BlockStateProperties.FACING) == facing) {
                return false;
            }
            newState = state.setValue(BlockStateProperties.FACING, facing);
        } else if (state.hasProperty(BlockStateProperties.HORIZONTAL_FACING)) {
            if (!facing.getAxis().isHorizontal()) {
                return false;
            }
            if (state.getValue(BlockStateProperties.HORIZONTAL_FACING) == facing) {
                return false;
            }
            newState = state.setValue(BlockStateProperties.HORIZONTAL_FACING, facing);
        }
        if (newState != null) {
            world.setBlock(pos, newState, 3);
            return true;
        }
        return false;
    }

    /**
     * 检查是否可以设置指定朝向。
     *
     * @param world  世界
     * @param pos    方块位置
     * @param facing 目标朝向
     * @return 如果可以设置则返回 true
     */
    default boolean canSetFacing(Level world, BlockPos pos, Direction facing) {
        return true;
    }

    /**
     * 检查扳手是否可以移除此方块。
     *
     * @param world  世界
     * @param pos    方块位置
     * @param player 执行操作的玩家
     * @return 如果可以移除则返回 true
     */
    default boolean wrenchCanRemove(Level world, BlockPos pos, Player player) {
        return true;
    }

    /**
     * 获取扳手拆卸此方块时的掉落物品。
     *
     * @param world 世界
     * @param pos   方块位置
     * @param state 方块状态
     * @return 掉落物品列表
     */
    default Collection<ItemStack> getWrenchDrops(Level world, BlockPos pos, BlockState state) {
        BlockEntity blockEntity = world.getBlockEntity(pos);
        if (world instanceof ServerLevel serverLevel) {
            return Block.getDrops(state, serverLevel, pos, blockEntity);
        }
        return List.of(new ItemStack(state.getBlock()));
    }
}
