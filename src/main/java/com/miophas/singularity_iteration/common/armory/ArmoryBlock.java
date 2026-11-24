package com.miophas.singularity_iteration.common.armory;

import com.miophas.singularity_iteration.common.block.mio_icif_entity_block;
import com.mojang.serialization.MapCodec;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/**
 * Armory machine block: 1x2 tall launch silo + twin-arm suit gantry, lit while bound and powered.
 *
 * <p>The {@link DoubleBlockHalf#LOWER} half holds the {@link mio_icif_armory} block entity and drops;
 * the {@link DoubleBlockHalf#UPPER} half is the open twin-arm gantry and magnetic launch ring, forwarding
 * interaction, ownership protection and breaking to the lower base.
 */
@SuppressWarnings("null")
public class ArmoryBlock extends mio_icif_entity_block {
    public static final BooleanProperty LIT = BooleanProperty.create("lit");
    public static final EnumProperty<DoubleBlockHalf> HALF = BlockStateProperties.DOUBLE_BLOCK_HALF;
    public static final MapCodec<ArmoryBlock> CODEC = simpleCodec(ArmoryBlock::new);

    private static final VoxelShape UPPER_SHAPE_NS = Shapes.or(
        Block.box(0.2, 0, 4.0, 3.8, 15.6, 12.0),
        Block.box(12.2, 0, 4.0, 15.8, 15.6, 12.0));
    private static final VoxelShape UPPER_SHAPE_EW = Shapes.or(
        Block.box(4.0, 0, 0.2, 12.0, 15.6, 3.8),
        Block.box(4.0, 0, 12.2, 12.0, 15.6, 15.8));

    public ArmoryBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any()
            .setValue(FACING, Direction.NORTH)
            .setValue(LIT, false)
            .setValue(HALF, DoubleBlockHalf.LOWER));
    }

    /** Resolves either half of an Armory block to the base (lower) position that owns the block entity. */
    public static BlockPos basePos(BlockState state, BlockPos pos) {
        return state.hasProperty(HALF) && state.getValue(HALF) == DoubleBlockHalf.UPPER ? pos.below() : pos;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(LIT, HALF);
    }

    @Override protected MapCodec<? extends BaseEntityBlock> codec() { return CODEC; }
    @Override public RenderShape getRenderShape(BlockState state) { return RenderShape.MODEL; }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        if (state.getValue(HALF) == DoubleBlockHalf.UPPER) {
            return state.getValue(FACING).getAxis() == Direction.Axis.Z ? UPPER_SHAPE_NS : UPPER_SHAPE_EW;
        }
        return Shapes.block();
    }

    @Override
    protected VoxelShape getOcclusionShape(BlockState state, BlockGetter level, BlockPos pos) {
        return Shapes.empty();
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        if (pos.getY() + 1 >= level.getMaxBuildHeight()) return null;
        if (!level.getBlockState(pos.above()).canBeReplaced(context)) return null;
        BlockState base = super.getStateForPlacement(context);
        return base == null ? null : base.setValue(HALF, DoubleBlockHalf.LOWER).setValue(LIT, false);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (!level.isClientSide && state.getValue(HALF) == DoubleBlockHalf.LOWER) {
            BlockPos above = pos.above();
            if (above.getY() < level.getMaxBuildHeight()) {
                level.setBlock(above, state.setValue(HALF, DoubleBlockHalf.UPPER), Block.UPDATE_ALL);
            }
        }
    }

    @Override
    protected BlockState updateShape(BlockState state, Direction direction, BlockState neighbor, LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        if (state.getValue(HALF) == DoubleBlockHalf.UPPER && direction == Direction.DOWN) {
            if (!neighbor.is(this) || neighbor.getValue(HALF) != DoubleBlockHalf.LOWER) {
                return Blocks.AIR.defaultBlockState();
            }
            return state.setValue(FACING, neighbor.getValue(FACING)).setValue(LIT, neighbor.getValue(LIT));
        }
        return super.updateShape(state, direction, neighbor, level, pos, neighborPos);
    }

    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide && state.getValue(HALF) == DoubleBlockHalf.UPPER) {
            BlockPos base = pos.below();
            BlockState lower = level.getBlockState(base);
            if (lower.is(this) && lower.getValue(HALF) == DoubleBlockHalf.LOWER) {
                level.destroyBlock(base, !player.isCreative() && player.hasCorrectToolForDrops(lower), player);
            }
        }
        return super.playerWillDestroy(level, pos, state, player);
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock()) && !level.isClientSide && state.getValue(HALF) == DoubleBlockHalf.LOWER) {
            BlockPos above = pos.above();
            BlockState up = level.getBlockState(above);
            if (up.is(this) && up.getValue(HALF) == DoubleBlockHalf.UPPER) {
                level.setBlock(above, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL | Block.UPDATE_SUPPRESS_DROPS);
            }
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    @Override
    public List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        if (state.getValue(HALF) == DoubleBlockHalf.UPPER) return List.of();
        return super.getDrops(state, params);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.isClientSide) return InteractionResult.SUCCESS;
        BlockPos base = basePos(state, pos);
        if (!(level.getBlockEntity(base) instanceof mio_icif_armory armory)) return InteractionResult.PASS;
        if (armory.owner() == null) {
            Component message = armory.tryBind(player);
            if (message != null) player.displayClientMessage(message, true);
            if (armory.owner() == null) return InteractionResult.CONSUME;   // unpowered: no GUI until bound
        }
        if (!armory.mayAccess(player)) {
            player.displayClientMessage(Component.translatable("message.mio_icif.armory.owned", armory.ownerName()), true);
            return InteractionResult.CONSUME;
        }
        player.openMenu(armory, buf -> buf.writeBlockPos(base));
        return InteractionResult.CONSUME;
    }

    // Contents drop through mio_icif_entity_block.onRemove (the entity is a Container).

    /** Only the owner (or an operator) can break a bound Armory. */
    @Override
    public float getDestroyProgress(BlockState state, Player player, BlockGetter level, BlockPos pos) {
        BlockPos base = basePos(state, pos);
        if (level.getBlockEntity(base) instanceof mio_icif_armory armory && !armory.mayAccess(player)) return 0.0F;
        return super.getDestroyProgress(state, player, level, pos);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return state.getValue(HALF) == DoubleBlockHalf.LOWER ? new mio_icif_armory(pos, state) : null;
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide || state.getValue(HALF) != DoubleBlockHalf.LOWER) return null;
        return createTickerHelper(type, ArmoryRegistry.ARMORY_ENTITY.get(), mio_icif_armory::tick);
    }
}
