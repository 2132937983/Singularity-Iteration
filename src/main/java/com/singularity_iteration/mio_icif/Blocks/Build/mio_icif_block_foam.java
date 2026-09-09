package com.singularity_iteration.mio_icif.Blocks.Build;

import com.singularity_iteration.mio_icif.Blocks.Wire.mio_icif_block_wire;
import com.singularity_iteration.mio_icif.Blocks.entity.build.mio_icif_block_foam_entity;
import com.singularity_iteration.mio_icif.Blocks.mio_icif_blocks;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.phys.shapes.Shapes;
import org.jetbrains.annotations.Nullable;

/**
 * 建筑泡沫方块 - IC2风格的CF泡沫
 * 普通泡沫会在光照下逐渐硬化，强化泡沫需要更长时间
 * 用沙子右键可以加速硬化
 * 支持遮蔽器伪装纹理功能
 *
 * IC2原版特性：泡沫没有碰撞箱，实体可以穿过泡沫方块
 * 只有硬化后才会变成固体方块
 */
@SuppressWarnings("null")
public class mio_icif_block_foam extends BaseEntityBlock {

    public static final MapCodec<mio_icif_block_foam> CODEC = simpleCodec(props -> new mio_icif_block_foam(props, false));

    // 是否为强化泡沫（铁脚手架内填充的泡沫）
    public static final BooleanProperty REINFORCED = BooleanProperty.create("reinforced");

    // 是否有伪装纹理（遮蔽器粘贴后为true）
    public static final BooleanProperty DISGUISED = BooleanProperty.create("disguised");

    // 硬化时间（秒），普通泡沫10秒，强化泡沫60秒
    @SuppressWarnings("unused")
    private final boolean isReinforced;

    // 空碰撞箱 - 实体可以穿过泡沫（IC2原版特性）
    private static final VoxelShape EMPTY_SHAPE = Shapes.empty();

    public mio_icif_block_foam(Properties properties) {
        this(properties, false);
    }

    public mio_icif_block_foam(Properties properties, boolean reinforced) {
        super(properties);
        this.isReinforced = reinforced;
        this.registerDefaultState(this.stateDefinition.any().setValue(REINFORCED, reinforced).setValue(DISGUISED, false));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        // 有伪装时由BER渲染伪装方块，无伪装时用默认模型渲染泡沫
        return state.getValue(DISGUISED) ? RenderShape.ENTITYBLOCK_ANIMATED : RenderShape.MODEL;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new mio_icif_block_foam_entity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> blockEntityType) {
        return null; // 泡沫不需要tick更新
    }

    /**
     * 泡沫没有碰撞箱 - 实体可以穿过（IC2原版特性）
     */
    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return EMPTY_SHAPE;
    }

    /**
     * 泡沫不是完整方块 - 不阻挡实体移动
     */
    @Override
    public boolean isCollisionShapeFullBlock(BlockState state, BlockGetter level, BlockPos pos) {
        return false;
    }

    /**
     * 泡沫不阻挡视线
     */
    @Override
    public boolean isOcclusionShapeFullBlock(BlockState state, BlockGetter level, BlockPos pos) {
        return false;
    }

    /**
     * 显示形状（用于渲染和交互，但不是碰撞形状）
     * 泡沫在视觉上仍然存在，只是实体可以穿过
     */
    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.block();
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(REINFORCED, DISGUISED);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        BlockState replacedState = level.getBlockState(pos);

        if (replacedState.getBlock() instanceof mio_icif_block_wire) {
            if (!replacedState.getValue(mio_icif_block_wire.FOAMLOGGED)) {
                boolean reinforced = this.defaultBlockState().getValue(REINFORCED);
                BlockState newWireState = replacedState
                    .setValue(mio_icif_block_wire.FOAMLOGGED, true)
                    .setValue(mio_icif_block_wire.FOAM_REINFORCED, reinforced);
                if (!level.isClientSide) {
                    level.setBlockAndUpdate(pos, newWireState);
                    level.playSound(null, pos, SoundEvents.SLIME_BLOCK_PLACE, SoundSource.BLOCKS, 0.8F, 1.2F);
                    Player player = context.getPlayer();
                    if (player == null || !player.getAbilities().instabuild) {
                        context.getItemInHand().shrink(1);
                    }
                }
                return null;
            }
        }
        return super.getStateForPlacement(context);
    }

    @Override
    public boolean canBeReplaced(BlockState state, BlockPlaceContext context) {
        ItemStack itemStack = context.getItemInHand();
        if (itemStack.getItem() instanceof net.minecraft.world.item.BlockItem blockItem) {
            Block block = blockItem.getBlock();
            if (block instanceof mio_icif_block_foam) {
                return false;
            }
            VoxelShape shape = block.defaultBlockState().getShape(context.getLevel(), context.getClickedPos(), CollisionContext.of(context.getPlayer()));
            return !shape.isEmpty() && shape != Shapes.block();
        }
        return false;
    }

    /**
     * 随机tick - 泡沫硬化逻辑
     * 硬化速度取决于光照等级：光照越高硬化越快
     */
    @Override
    public void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        float chance = getHardenChance(level, pos, state);
        if (random.nextFloat() < chance) {
            harden(level, pos, state);
        }
    }

    /**
     * 计算硬化概率
     * 基于IC2逻辑：光照越高，硬化越快
     */
    public float getHardenChance(Level level, BlockPos pos, BlockState state) {
        int light = level.getMaxLocalRawBrightness(pos);

        // 检查周围光照取最大值
        for (Direction side : Direction.values()) {
            light = Math.max(light, level.getMaxLocalRawBrightness(pos.relative(side)));
        }

        // 光照为0时几乎不硬化
        if (light <= 0) return 0.0F;

        // 硬化基础时间（ticks）：普通泡沫7.5秒，强化泡沫15秒（缩短至原版1/4）
        // 使用方块状态判断而不是实例字段，以支持动态设置强化状态
        boolean reinforced = state.getValue(REINFORCED);
        int hardenTimeTicks = reinforced ? 300 : 150;

        // 光照越高硬化越快，光照越低硬化越慢
        return (float) light / (hardenTimeTicks * 16);
    }

    /**
     * 硬化泡沫 - 将泡沫方块替换为硬化后的方块
     * 普通泡沫 → 石头
     * 强化泡沫 → 防爆石（Construction Wall，防爆）
     */
    private void harden(Level level, BlockPos pos, BlockState state) {
        // 使用方块状态判断而不是实例字段，以支持动态设置强化状态
        boolean reinforced = state.getValue(REINFORCED);
        if (reinforced) {
            // 强化泡沫硬化为防爆石
            level.setBlockAndUpdate(pos, mio_icif_blocks.CONSTRUCTION_WALL.get().defaultBlockState());
        } else {
            // 普通泡沫硬化为石头
            level.setBlockAndUpdate(pos, Blocks.STONE.defaultBlockState());
        }
        level.playSound(null, pos, SoundEvents.SAND_HIT, SoundSource.BLOCKS, 0.5F, 0.5F);
    }

    /**
     * 实体在泡沫内时的效果（IC2风格）
     * - 移速变慢（缓慢效果）
     * - 减少下落速度（模拟泡沫的阻力）
     */
    @Override
    public void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        if (entity instanceof LivingEntity living) {
 // 慢 III 效果（大幅降低移速）
            living.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 20, 2, false, false, false));
            // 减少下落速度（模拟泡沫的阻力）
            var delta = living.getDeltaMovement();
            if (delta.y < 0) {
                living.setDeltaMovement(delta.x, delta.y * 0.5, delta.z);
            }
            // 重置摔落距离
            living.resetFallDistance();
        }
        super.entityInside(state, level, pos, entity);
    }

    /**
     * 右键交互 - 用沙子加速硬化
     */
    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult) {
        // 用沙子右键加速硬化
        if (stack.is(Items.SAND)) {
            if (!level.isClientSide()) {
                hardenByState(level, pos, state);
                if (!player.getAbilities().instabuild) {
                    stack.shrink(1);
                }
            }
            return ItemInteractionResult.sidedSuccess(level.isClientSide());
        }

        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    /**
     * 根据方块状态中的REINFORCED属性硬化泡沫
     */
    private void hardenByState(Level level, BlockPos pos, BlockState state) {
        if (state.getValue(REINFORCED)) {
            level.setBlockAndUpdate(pos, mio_icif_blocks.CONSTRUCTION_WALL.get().defaultBlockState());
        } else {
            level.setBlockAndUpdate(pos, Blocks.STONE.defaultBlockState());
        }
        level.playSound(null, pos, SoundEvents.SAND_HIT, SoundSource.BLOCKS, 0.5F, 0.5F);
    }

    /**
     * 破坏方块时始终掉落建筑泡沫本身
     * 忽略BlockEntity中存储的伪装数据
     */
    @Override
    public void playerDestroy(Level level, Player player, BlockPos pos, BlockState state, @Nullable BlockEntity blockEntity, ItemStack tool) {
        super.playerDestroy(level, player, pos, state, blockEntity, tool);
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        super.onRemove(state, level, pos, newState, movedByPiston);
    }
}