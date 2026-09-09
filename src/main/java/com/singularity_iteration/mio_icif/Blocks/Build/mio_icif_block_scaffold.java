package com.singularity_iteration.mio_icif.Blocks.Build;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * 脚手架方块基类
 * 实现IC2风格的脚手架功能
 * - 可攀爬（梯子功能，通过NeoForge事件系统实现）
 * - 支撑系统（脚手架可以水平延伸，受支撑强度限制）
 * - 随机刻检查支撑
 */
@SuppressWarnings({"null", "deprecation"})
public class mio_icif_block_scaffold extends Block {

    // 脚手架的碰撞箱（略微缩小，像IC2一样）
    private static final VoxelShape SCAFFOLD_SHAPE = Block.box(0.5, 0, 0.5, 15.5, 16, 15.5);

    // 支撑强度（决定脚手架可以水平延伸多远而不需要固体方块支撑）
    private final int strength;

    public mio_icif_block_scaffold(Properties properties, int strength) {
        super(properties);
        this.strength = strength;
    }

    public int getStrength() {
        return strength;
    }

    /**
     * 脚手架不是完整方块
     */
    @Override
    public boolean isCollisionShapeFullBlock(BlockState state, BlockGetter level, BlockPos pos) {
        return false;
    }

    /**
     * 脚手架不阻挡视线
     */
    @Override
    public boolean isOcclusionShapeFullBlock(BlockState state, BlockGetter level, BlockPos pos) {
        return false;
    }

    /**
     * 获取形状
     */
    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SCAFFOLD_SHAPE;
    }

    /**
     * 获取碰撞形状
     */
    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SCAFFOLD_SHAPE;
    }

    /**
     * 放置检查 - 脚手架需要有支撑
     * 支持水平悬空放置（贴墙或连接到其他脚手架）
     */
    @Override
    public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        return hasSupport(level, pos);
    }

    /**
     * 启用随机刻
     */
    @Override
    public boolean isRandomlyTicking(BlockState state) {
        return true;
    }

    /**
     * 检查脚手架是否有支撑
     * 支撑来源：
     * 1. 下方是固体方块 → 直接有支撑
     * 2. 下方是脚手架 → 有支撑（脚手架链式支撑）
     * 3. 旁边有固体方块 → 有支撑（贴墙放置）
     * 4. 旁边有脚手架且有支撑 → 有支撑（水平延伸，受强度限制）
     */
    private boolean hasSupport(LevelReader world, BlockPos pos) {
        // 1. 下方是固体方块 → 直接有支撑
        BlockState belowState = world.getBlockState(pos.below());
        if (isSolidBlock(belowState, world, pos.below())) {
            return true;
        }

        // 2. 下方是脚手架 → 有支撑
        if (belowState.getBlock() instanceof mio_icif_block_scaffold) {
            return true;
        }

        // 3. 旁边有固体方块 → 有支撑（贴墙放置）
        for (Direction dir : new Direction[]{Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST}) {
            BlockState sideState = world.getBlockState(pos.relative(dir));
            if (isSolidBlock(sideState, world, pos.relative(dir))) {
                return true;
            }
        }

        // 4. 旁边有脚手架且该脚手架有支撑 → 有支撑（水平延伸）
        // 但需要检查水平延伸距离是否在支撑强度范围内
        for (Direction dir : new Direction[]{Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST}) {
            BlockState sideState = world.getBlockState(pos.relative(dir));
            if (sideState.getBlock() instanceof mio_icif_block_scaffold) {
                // 检查从该脚手架到最近的垂直支撑的距离
                if (getHorizontalDistanceToSupport(world, pos.relative(dir), new java.util.HashSet<>()) <= this.strength) {
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * 判断方块是否为固体方块（可以作为支撑）
     * 使用 isSolidRender 与 isAir 双重检查，更宽泛
     */
    private boolean isSolidBlock(BlockState state, LevelReader world, BlockPos pos) {
        // 空气不算
        if (state.isAir()) {
            return false;
        }
        // 可替换方块（如草、水等）不算
        if (state.canBeReplaced()) {
            return false;
        }
        // 脚手架自身不算固体支撑
        if (state.getBlock() instanceof mio_icif_block_scaffold) {
            return false;
        }
        // 其他非空气、非可替换方块都算固体支撑
        return state.isSolidRender(world, pos) || state.isSolid();
    }

    /**
     * 计算从指定脚手架到最近的垂直支撑（固体方块或向下连接到地面的脚手架）的水平距离
     * 返回距离（相同为0，相邻为1），如果没有支撑则返回Integer.MAX_VALUE
     */
    private int getHorizontalDistanceToSupport(LevelReader world, BlockPos pos, java.util.Set<BlockPos> visited) {
        if (visited.contains(pos)) {
            return Integer.MAX_VALUE;
        }
        visited.add(pos);

        // 如果这个脚手架下方是固体方块或脚手架链到地面，距离为0
        if (hasVerticalSupport(world, pos, new java.util.HashSet<>())) {
            return 0;
        }

        // 向旁边搜索其他脚手架
        int minDist = Integer.MAX_VALUE;
        for (Direction dir : new Direction[]{Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST}) {
            BlockPos neighborPos = pos.relative(dir);
            BlockState neighborState = world.getBlockState(neighborPos);

            if (neighborState.getBlock() instanceof mio_icif_block_scaffold) {
                int dist = getHorizontalDistanceToSupport(world, neighborPos, visited);
                if (dist != Integer.MAX_VALUE) {
                    minDist = Math.min(minDist, dist + 1);
                }
            }
            // 旁边是固体方块也算垂直支撑
            else if (isSolidBlock(neighborState, world, neighborPos)) {
                minDist = Math.min(minDist, 1);
            }
        }

        return minDist;
    }

    /**
     * 检查脚手架是否有垂直支撑（下方连接到固体方块或地面）
     */
    private boolean hasVerticalSupport(LevelReader world, BlockPos pos, java.util.Set<BlockPos> visited) {
        if (visited.contains(pos)) {
            return false;
        }
        visited.add(pos);

        BlockPos belowPos = pos.below();
        BlockState belowState = world.getBlockState(belowPos);

        // 下方是固体方块
        if (isSolidBlock(belowState, world, belowPos)) {
            return true;
        }

        // 下方是脚手架，继续向下检查
        if (belowState.getBlock() instanceof mio_icif_block_scaffold) {
            return hasVerticalSupport(world, belowPos, visited);
        }

        return false;
    }

    /**
     * 邻居方块更新时检查支撑
     */
    @Override
    public void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighborBlock, BlockPos neighborPos, boolean movedByPiston) {
        super.neighborChanged(state, level, pos, neighborBlock, neighborPos, movedByPiston);
        if (!level.isClientSide) {
            checkSupport(level, pos);
        }
    }

    /**
     * 随机刻更新 - 偶尔检查支撑
     */
    @Override
    public void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (random.nextInt(8) == 0) {
            checkSupport(level, pos);
        }
    }

    /**
     * 检查支撑，如果无支撑则掉落
     */
    private void checkSupport(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (!(state.getBlock() instanceof mio_icif_block_scaffold)) {
            return;
        }

        if (!hasSupport(level, pos)) {
            level.destroyBlock(pos, true);
        }
    }
}

