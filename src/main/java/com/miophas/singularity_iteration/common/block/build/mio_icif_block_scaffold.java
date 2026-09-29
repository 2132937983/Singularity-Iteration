package com.miophas.singularity_iteration.common.block.build;

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

import java.util.Arrays;

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

    private static final Direction[] HORIZONTAL_DIRECTIONS = {
        Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST
    };

    /**
     * A malformed/add-on scaffold must not turn placement into an unbounded graph walk.
     * All bundled scaffold variants are <= 9; this bound is deliberately generous while
     * keeping the work and temporary search grid finite.
     */
    private static final int MAX_HORIZONTAL_SUPPORT_DISTANCE = 32;

    /** Mutable lookup state is thread-local because client and server can tick concurrently. */
    private static final ThreadLocal<BlockPos.MutableBlockPos> SUPPORT_QUERY_POS =
        ThreadLocal.withInitial(BlockPos.MutableBlockPos::new);

    public mio_icif_block_scaffold(Properties properties, int strength) {
        super(properties);
        this.strength = Math.max(0, strength);
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
        BlockPos.MutableBlockPos queryPos = SUPPORT_QUERY_POS.get();

        // 1. 下方是固体方块 → 直接有支撑
        if (setAndRead(world, queryPos, pos.getX(), pos.getY() - 1, pos.getZ())) {
            BlockState belowState = world.getBlockState(queryPos);
            if (isSolidBlock(belowState, world, queryPos)) {
                return true;
            }

            // 2. 下方是脚手架 → 有支撑
            if (belowState.getBlock() instanceof mio_icif_block_scaffold) {
                return true;
            }
        }

        // 3. 旁边有固体方块 → 有支撑（贴墙放置）
        for (Direction dir : HORIZONTAL_DIRECTIONS) {
            setAndRead(world, queryPos, pos.getX() + dir.getStepX(), pos.getY(), pos.getZ() + dir.getStepZ());
            if (isSolidBlock(world.getBlockState(queryPos), world, queryPos)) {
                return true;
            }
        }

        // 4. 多源有界 BFS：从相邻脚手架开始寻找垂直支撑或固体墙面。
        // 原实现递归遍历整个连通分量，长链/环路会造成 O(N^2) 和 StackOverflow。
        int maxDistance = Math.min(strength, MAX_HORIZONTAL_SUPPORT_DISTANCE);
        int gridRadius = maxDistance + 1;
        int gridSide = gridRadius * 2 + 1;
        int gridSize = gridSide * gridSide;
        int[] distance = new int[gridSize];
        Arrays.fill(distance, -1);
        int[] queue = new int[gridSize];
        int head = 0;
        int tail = 0;
        int originX = pos.getX();
        int originY = pos.getY();
        int originZ = pos.getZ();

        for (Direction dir : HORIZONTAL_DIRECTIONS) {
            int relativeX = dir.getStepX();
            int relativeZ = dir.getStepZ();
            int index = toGridIndex(relativeX, relativeZ, gridRadius, gridSide);
            if (distance[index] >= 0) {
                continue;
            }
            setAndRead(world, queryPos, originX + relativeX, originY, originZ + relativeZ);
            if (world.getBlockState(queryPos).getBlock() instanceof mio_icif_block_scaffold) {
                distance[index] = 0;
                queue[tail++] = index;
            }
        }

        while (head < tail) {
            int index = queue[head++];
            int depth = distance[index];
            int relativeZ = index / gridSide - gridRadius;
            int relativeX = index % gridSide - gridRadius;
            int worldX = originX + relativeX;
            int worldZ = originZ + relativeZ;

            if (hasVerticalSupport(world, queryPos, worldX, originY, worldZ)) {
                return true;
            }
            if (depth >= maxDistance) {
                continue;
            }

            int nextDepth = depth + 1;
            for (Direction dir : HORIZONTAL_DIRECTIONS) {
                int nextRelativeX = relativeX + dir.getStepX();
                int nextRelativeZ = relativeZ + dir.getStepZ();
                int nextWorldX = worldX + dir.getStepX();
                int nextWorldZ = worldZ + dir.getStepZ();
                setAndRead(world, queryPos, nextWorldX, originY, nextWorldZ);
                BlockState nextState = world.getBlockState(queryPos);
                if (nextState.getBlock() instanceof mio_icif_block_scaffold) {
                    int nextIndex = toGridIndex(nextRelativeX, nextRelativeZ, gridRadius, gridSide);
                    if (distance[nextIndex] < 0) {
                        distance[nextIndex] = nextDepth;
                        queue[tail++] = nextIndex;
                    }
                } else if (isSolidBlock(nextState, world, queryPos)) {
                    // 与旧实现一致：墙面距离从相邻脚手架算作 1。
                    return true;
                }
            }
        }

        return false;
    }

    private static int toGridIndex(int relativeX, int relativeZ, int gridRadius, int gridSide) {
        return (relativeZ + gridRadius) * gridSide + relativeX + gridRadius;
    }

    private static boolean setAndRead(LevelReader world, BlockPos.MutableBlockPos pos,
                                      int x, int y, int z) {
        pos.set(x, y, z);
        return y >= world.getMinBuildHeight() && y < world.getMaxBuildHeight();
    }

    /** 判断方块是否为固体方块（可以作为支撑）。 */
    private boolean isSolidBlock(BlockState state, LevelReader world, BlockPos pos) {
        if (state.isAir() || state.canBeReplaced()
                || state.getBlock() instanceof mio_icif_block_scaffold) {
            return false;
        }
        return state.isSolidRender(world, pos) || state.isSolid();
    }

    /** 检查指定列是否通过脚手架链连接到固体方块；使用迭代避免深塔 StackOverflow。 */
    private boolean hasVerticalSupport(LevelReader world, BlockPos.MutableBlockPos queryPos,
                                       int x, int y, int z) {
        int currentY = y - 1;
        while (currentY >= world.getMinBuildHeight()) {
            queryPos.set(x, currentY, z);
            BlockState belowState = world.getBlockState(queryPos);
            if (isSolidBlock(belowState, world, queryPos)) {
                return true;
            }
            if (!(belowState.getBlock() instanceof mio_icif_block_scaffold)) {
                return false;
            }
            currentY--;
        }
        return false;
    }

    /**
     * 邻居方块更新时检查支撑
     */
    @Override
    public void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighborBlock, BlockPos neighborPos, boolean movedByPiston) {
        super.neighborChanged(state, level, pos, neighborBlock, neighborPos, movedByPiston);
        if (!level.isClientSide && level.getBlockState(pos).getBlock() instanceof mio_icif_block_scaffold) {
            // Defer invalidation by one tick.  A direct destroyBlock here recursively
            // notifies the next scaffold and can overflow the call stack on long towers.
            level.scheduleTick(pos, this, 1);
        }
    }

    /** 随机刻是漏网变更的低频兜底；正常变更走上面的延迟 tick。 */
    @Override
    public void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        checkSupport(level, pos);
    }

    @Override
    public void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        checkSupport(level, pos);
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

