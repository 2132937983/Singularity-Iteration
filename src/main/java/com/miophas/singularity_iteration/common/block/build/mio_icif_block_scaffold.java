package com.miophas.singularity_iteration.common.block.build;

import com.miophas.singularity_iteration.common.registry.mio_icif_blocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 脚手架方块基类 - 对齐 IC2 1.12.2 原版 {@code ic2.core.block.BlockScaffold}。
 *
 * <p>对齐要点：
 * <ul>
 *     <li><b>悬空强度</b>：从与地面（固体方块）相连的脚手架柱出发，可水平悬空的最大格数。
 *         IC2 数值：木质 2 / 加固木 5 / 铁质 5 / 加固铁 12。</li>
 *     <li><b>加固</b>：右键木质脚手架（消耗 2 根木棍）或铁质脚手架（消耗 1 个铁栅栏）加固，
 *         必须位于与地面相连的立柱上；钢/碳纤维等附加档不可加固。</li>
 *     <li><b>掉落</b>：加固脚手架破坏时额外返还对应加固材料。</li>
 *     <li><b>攀爬</b>：可作为梯子攀爬，并在 {@link #entityInside} 中对齐 IC2 的运动修正。</li>
 *     <li><b>支撑检查</b>：邻居变化立即检查（延迟 tick），随机刻以 1/8 概率兜底。</li>
 * </ul>
 */
@SuppressWarnings({"null", "deprecation"})
public class mio_icif_block_scaffold extends Block {

    /** 是否已加固（IC2：reinforced_wood / reinforced_iron）。 */
    public static final BooleanProperty REINFORCED = BooleanProperty.create("reinforced");

    // 脚手架的碰撞箱（略微缩小，像IC2一样）
    private static final VoxelShape SCAFFOLD_SHAPE = Block.box(0.5, 0, 0.5, 15.5, 16, 15.5);

    /** 放置时使用的支撑距离（对齐 IC2：放置检查固定按木质脚手架 2 格判定）。 */
    private static final int PLACEMENT_SUPPORT_DISTANCE = 2;

    // 水平悬空支撑距离（IC2：木质 2、铁质 5）
    private final int strength;
    /** 加固后的悬空强度；{@code < 0} 表示该档位不可加固。 */
    private final int reinforcedStrength;
    /** 加固材料：true = 木棍 x2，false = 铁栅栏 x1。 */
    private final boolean reinforceWithSticks;

    private static final Direction[] HORIZONTAL_DIRECTIONS = {
        Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST
    };

    /**
     * A malformed/add-on scaffold must not turn placement into an unbounded graph walk.
     * All bundled scaffold variants are <= 12; this bound is deliberately generous while
     * keeping the work and temporary search grid finite.
     */
    private static final int MAX_HORIZONTAL_SUPPORT_DISTANCE = 32;

    /** Mutable lookup state is thread-local because client and server can tick concurrently. */
    private static final ThreadLocal<BlockPos.MutableBlockPos> SUPPORT_QUERY_POS =
        ThreadLocal.withInitial(BlockPos.MutableBlockPos::new);

    public mio_icif_block_scaffold(Properties properties, int strength) {
        this(properties, strength, -1, false);
    }

    /**
     * @param properties          方块属性
     * @param strength            基础悬空强度
     * @param reinforcedStrength  加固后的悬空强度（&lt; 0 表示不可加固）
     * @param reinforceWithSticks 加固材料：true = 木棍 x2，false = 铁栅栏 x1
     */
    public mio_icif_block_scaffold(Properties properties, int strength, int reinforcedStrength, boolean reinforceWithSticks) {
        super(properties);
        this.strength = Math.max(0, strength);
        this.reinforcedStrength = reinforcedStrength;
        this.reinforceWithSticks = reinforceWithSticks;
        this.registerDefaultState(this.stateDefinition.any().setValue(REINFORCED, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(REINFORCED);
    }

    /** 基础悬空强度（格）。 */
    public int getStrength() {
        return strength;
    }

    /** 指定状态下的有效悬空强度（加固后更高）。 */
    public int getStrength(BlockState state) {
        if (state.getValue(REINFORCED) && reinforcedStrength >= 0) {
            return Math.max(strength, reinforcedStrength);
        }
        return strength;
    }

    /** 该档位是否可加固。 */
    public boolean canReinforce() {
        return reinforcedStrength >= 0;
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

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SCAFFOLD_SHAPE;
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SCAFFOLD_SHAPE;
    }

    /**
     * 可作为梯子攀爬（对齐 IC2 {@code isLadder} 返回 true）。
     * 该方法对应 NeoForge {@code IBlockExtension#isLadder}；不标注 {@code @Override}
     * 以避免不同映射下的签名漂移导致编译失败。
     */
    public boolean isLadder(BlockState state, LevelReader level, BlockPos pos, LivingEntity entity) {
        return true;
    }

    /**
     * 放置检查 - 脚手架需要有支撑（对齐 IC2：使用木质脚手架的强度判定）。
     */
    @Override
    public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        return hasSupport(level, pos, PLACEMENT_SUPPORT_DISTANCE);
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
     * 4. 多源有界 BFS：从相邻脚手架开始寻找垂直支撑或固体墙面。
     * 原实现递归遍历整个连通分量，长链/环路会造成 O(N^2) 和 StackOverflow。
     */
    private boolean hasSupport(LevelReader world, BlockPos pos, int rawMaxDistance) {
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
        int maxDistance = Math.min(rawMaxDistance, MAX_HORIZONTAL_SUPPORT_DISTANCE);
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

    /** 随机刻是漏网变更的低频兜底（对齐 IC2：1/8 概率检查一次）。 */
    @Override
    public void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (random.nextInt(8) == 0) {
            checkSupport(level, pos);
        }
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
        if (!(state.getBlock() instanceof mio_icif_block_scaffold scaffold)) {
            return;
        }

        if (!hasSupport(level, pos, scaffold.getStrength(state))) {
            level.destroyBlock(pos, true);
        }
    }

    /**
     * 对齐 IC2 {@code BlockScaffold.onEntityCollidedWithBlock}：
     * 重置摔落距离、限制水平速度、并在潜行/贴墙时提供上升速度。
     */
    @Override
    public void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        if (entity instanceof LivingEntity living) {
            living.resetFallDistance();

            double limit = 0.15D;
            Vec3 movement = living.getDeltaMovement();
            double x = Mth.clamp(movement.x, -limit, limit);
            double z = Mth.clamp(movement.z, -limit, limit);

            double y;
            if (living.isShiftKeyDown() && living instanceof Player) {
                y = living.isInWater() ? 0.02D : 0.08D;
            } else if (living.horizontalCollision) {
                y = 0.2D;
            } else {
                y = Math.max(movement.y, -0.07D);
            }

            living.setDeltaMovement(x, y, z);
        }
        super.entityInside(state, level, pos, entity);
    }

    /**
     * 右键加固 - 对齐 IC2 {@code BlockScaffold.onBlockActivated}：
     * 木质 + 2 木棍 → 加固木；铁质 + 1 铁栅栏 → 加固铁；需要接地立柱。
     */
    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult) {
        if (player.isShiftKeyDown()) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (state.getValue(REINFORCED) || reinforcedStrength < 0 || stack.isEmpty()) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }

        final int cost;
        if (reinforceWithSticks) {
            if (!stack.is(Items.STICK) || stack.getCount() < 2) {
                return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
            }
            cost = 2;
        } else {
            if (!stack.is(mio_icif_blocks.BLOCK_FENCE_IRON.get().asItem())) {
                return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
            }
            cost = 1;
        }

        if (level.isClientSide) {
            return ItemInteractionResult.SUCCESS;
        }
        if (!isPillar(level, pos)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }

        if (!player.getAbilities().instabuild) {
            stack.shrink(cost);
        }
        level.setBlockAndUpdate(pos, state.setValue(REINFORCED, true));
        level.playSound(null, pos,
                reinforceWithSticks ? SoundEvents.WOOD_PLACE : SoundEvents.STONE_PLACE,
                SoundSource.BLOCKS, 0.8F, 1.0F);
        return ItemInteractionResult.sidedSuccess(false);
    }

    /** 对齐 IC2 {@code isPillar}：脚手架列必须一路向下连接到一个固体方块。 */
    private boolean isPillar(LevelReader level, BlockPos pos) {
        BlockPos.MutableBlockPos cursor = pos.mutable();
        int minY = level.getMinBuildHeight();
        while (cursor.getY() >= minY
                && level.getBlockState(cursor).getBlock() instanceof mio_icif_block_scaffold) {
            cursor.move(Direction.DOWN);
        }
        return isSolidBlock(level.getBlockState(cursor), level, cursor);
    }

    /**
     * 对齐 IC2 掉落：加固脚手架额外返还加固材料（木脚手架 + 2 木棍 / 铁脚手架 + 铁栅栏）。
     */
    @Override
    public List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        List<ItemStack> drops = super.getDrops(state, params);
        if (state.getValue(REINFORCED)) {
            drops = new ArrayList<>(drops);
            if (reinforceWithSticks) {
                drops.add(new ItemStack(Items.STICK, 2));
            } else {
                drops.add(new ItemStack(mio_icif_blocks.BLOCK_FENCE_IRON.get().asItem()));
            }
        }
        return drops;
    }
}
