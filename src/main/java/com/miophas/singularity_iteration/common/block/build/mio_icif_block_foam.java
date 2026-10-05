package com.miophas.singularity_iteration.common.block.build;

import com.miophas.singularity_iteration.common.block.wire.mio_icif_block_wire;
import com.miophas.singularity_iteration.common.blockentity.build.mio_icif_block_foam_entity;
import com.miophas.singularity_iteration.common.registry.mio_icif_blocks;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 建筑泡沫方块 - 对齐 IC2 1.12.2 原版 {@code ic2.core.block.BlockFoam}。
 *
 * <p>对齐要点：
 * <ul>
 *     <li><b>随机刻硬化</b>：{@code chance = 1/(hardenTime*(16-light)*20)}，
 *         再乘以 {@code 4096/randomTickSpeed} 换算到随机刻；光照越高越快。
 *         hardenTime：普通 300、强化 600。</li>
 *     <li><b>无碰撞箱</b>：未硬化前实体可以穿过（对齐 {@code getCollisionBoundingBox} 返回 null）。</li>
 *     <li><b>沙子加速</b>：手持沙子右键立即硬化并消耗 1 个沙子。</li>
 *     <li><b>硬化产物</b>：普通 → CF 墙；强化 → 强化石（防爆石）。</li>
 *     <li><b>掉落</b>：普通泡沫不掉落任何物品；强化泡沫掉落 1 个铁脚手架。</li>
 * </ul>
 */
@SuppressWarnings("null")
public class mio_icif_block_foam extends BaseEntityBlock {

    public static final MapCodec<mio_icif_block_foam> CODEC = simpleCodec(props -> new mio_icif_block_foam(props, false));

    // 是否为强化泡沫（铁脚手架内填充的泡沫）
    public static final BooleanProperty REINFORCED = BooleanProperty.create("reinforced");

    // 是否有伪装纹理（遮蔽器粘贴后为true）
    public static final BooleanProperty DISGUISED = BooleanProperty.create("disguised");

    /** IC2 BlockFoam.FoamType.normal 的硬化时间参数。 */
    public static final int HARDEN_TIME_NORMAL = 300;

    /** IC2 BlockFoam.FoamType.reinforced 的硬化时间参数。 */
    public static final int HARDEN_TIME_REINFORCED = 600;

    // 空碰撞箱 - 实体可以穿过泡沫（IC2原版特性）
    private static final VoxelShape EMPTY_SHAPE = Shapes.empty();

    public mio_icif_block_foam(Properties properties) {
        this(properties, false);
    }

    public mio_icif_block_foam(Properties properties, boolean reinforced) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(REINFORCED, reinforced)
                .setValue(DISGUISED, false));
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
        return null; // 泡沫不需要方块实体 tick
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
     * 显示形状（用于渲染和交互，但不是碰撞形状）。
     * 泡沫在视觉上仍然存在，只是实体可以穿过。
     */
    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.block();
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(REINFORCED, DISGUISED);
    }

    /**
     * 随机刻硬化 - 完全对齐 IC2 {@code BlockFoam.updateTick}。
     */
    @Override
    public void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        int tickSpeed = level.getGameRules().getInt(GameRules.RULE_RANDOMTICKING);
        if (tickSpeed <= 0) {
            return; // 世界未启用随机刻时不可能硬化
        }
        int hardenTime = state.getValue(REINFORCED) ? HARDEN_TIME_REINFORCED : HARDEN_TIME_NORMAL;
        float chance = getHardenChance(level, pos, state, hardenTime) * 4096.0F / tickSpeed;
        if (random.nextFloat() < chance) {
            harden(level, pos, state);
        }
    }

    /**
     * 对齐 IC2 {@code BlockFoam.getHardenChance}：
     * {@code 1 / (hardenTime * (16 - light) * 20)}。
     */
    public static float getHardenChance(Level level, BlockPos pos, BlockState state, int hardenTime) {
        int light = level.getMaxLocalRawBrightness(pos);

        // IC2：若方块非完整遮挡且透光，则取相邻方块（方块光）光照的最大值
        if (state.getLightBlock(level, pos) == 0) {
            for (Direction side : Direction.values()) {
                light = Math.max(light, level.getBrightness(LightLayer.BLOCK, pos.relative(side)));
            }
        }

        int avgTime = hardenTime * (16 - light);
        return 1.0F / (avgTime * 20);
    }

    /**
     * 硬化泡沫 - 将泡沫方块替换为硬化后的方块。
     * 普通泡沫 → CF 墙；强化泡沫 → 强化石（防爆石）。
     */
    private void harden(Level level, BlockPos pos, BlockState state) {
        BlockState result = state.getValue(REINFORCED)
                ? mio_icif_blocks.CONSTRUCTION_WALL.get().defaultBlockState()
                : mio_icif_blocks.CONSTRUCTION_FOAM_WALL.get().defaultBlockState();
        level.setBlockAndUpdate(pos, result);
        level.playSound(null, pos, SoundEvents.SAND_HIT, SoundSource.BLOCKS, 0.5F, 0.5F);
    }

    /**
     * 对齐 IC2 {@code BlockFoam.FoamType#getDrops}：
     * 普通泡沫不掉落任何物品；强化泡沫掉落 1 个铁脚手架。
     */
    @Override
    public List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        if (state.getValue(REINFORCED)) {
            List<ItemStack> drops = new ArrayList<>(1);
            drops.add(new ItemStack(mio_icif_blocks.SCAFFOLD_IRON.get().asItem()));
            return drops;
        }
        return Collections.emptyList();
    }

    /**
     * 右键交互 - 用沙子立即硬化（对齐 IC2，消耗 1 个沙子）。
     */
    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult) {
        if (stack.is(Items.SAND)) {
            if (!level.isClientSide()) {
                harden(level, pos, state);
                if (!player.getAbilities().instabuild) {
                    stack.shrink(1);
                }
            }
            return ItemInteractionResult.sidedSuccess(level.isClientSide());
        }

        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
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
}
