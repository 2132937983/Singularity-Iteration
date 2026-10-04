package com.miophas.singularity_iteration.common.block.producer;

import com.miophas.singularity_iteration.common.blockentity.producer.ResinCollectorBlockEntity;
import com.miophas.singularity_iteration.common.registry.mio_icif_block_entities;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.*;
import org.jetbrains.annotations.Nullable;

/** FACING is the outward front: the rubber log and the tap are behind it. */
public class ResinCollectorBlock extends BaseEntityBlock {
    public static final MapCodec<ResinCollectorBlock> CODEC = simpleCodec(ResinCollectorBlock::new);
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    private static final VoxelShape BOWL = Shapes.join(Block.box(0, 0, 0, 16, 16, 16),
        Block.box(2, 2, 2, 14, 16, 14), BooleanOp.ONLY_FIRST);

    public ResinCollectorBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }
    @Override protected MapCodec<? extends BaseEntityBlock> codec() { return CODEC; }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) { builder.add(FACING); }
    @Override protected RenderShape getRenderShape(BlockState state) { return RenderShape.MODEL; }
    @Override protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) { return BOWL; }
    @Override public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction front = context.getClickedFace().getAxis().isHorizontal()
            ? context.getClickedFace() : context.getHorizontalDirection().getOpposite();
        return defaultBlockState().setValue(FACING, front);
    }
    @Override protected BlockState rotate(BlockState state, Rotation rotation) { return state.setValue(FACING, rotation.rotate(state.getValue(FACING))); }
    @Override protected BlockState mirror(BlockState state, Mirror mirror) { return state.rotate(mirror.getRotation(state.getValue(FACING))); }
    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new ResinCollectorBlockEntity(pos, state); }
    @Override public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null : createTickerHelper(type, mio_icif_block_entities.RESIN_COLLECTOR.get(), ResinCollectorBlockEntity::serverTick);
    }
    @Override protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean moved) {
        super.onPlace(state, level, pos, oldState, moved);
        if (!level.isClientSide) level.scheduleTick(pos, this, 1);
    }
    @Override protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighbor, BlockPos neighborPos, boolean moved) {
        if (!level.isClientSide) level.scheduleTick(pos, this, 1);
    }
    @Override protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (level.getBlockEntity(pos) instanceof ResinCollectorBlockEntity collector) collector.collectResin();
    }
    @Override protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.mayInteract(player, pos)) return InteractionResult.FAIL;
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof ResinCollectorBlockEntity collector) player.openMenu(collector);
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
    @Override protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState next, boolean moved) {
        if (!state.is(next.getBlock()) && !level.isClientSide && level.getBlockEntity(pos) instanceof ResinCollectorBlockEntity collector) {
            var items = collector.getItemHandler();
            var stored = items.getStackInSlot(0).copy();
            items.setStackInSlot(0, net.minecraft.world.item.ItemStack.EMPTY);
            Block.popResource(level, pos, stored);
        }
        super.onRemove(state, level, pos, next, moved);
    }
}
