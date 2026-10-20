package com.miophas.singularity_iteration.common.block.producer;

import com.miophas.singularity_iteration.common.block.mio_icif_entity_block;
import com.miophas.singularity_iteration.common.blockentity.producer.AbstractReactorCoolantInjector;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

/**
 * The Redstone and Lapis Coolant Injector blocks (IC2 1.12 {@code rci_rsh} / {@code rci_lzh}). Any of
 * the six faces can be the front (IC2 {@code Util.allFacings}): it faces whoever placed it. The reactor,
 * or a chamber of it, may touch any face (the back first, as in IC2). ACTIVE while it can refill condensators.
 */
@SuppressWarnings("null")
public abstract class mio_icif_block_reactor_coolant_injector extends mio_icif_entity_block {

    /** All six directions, unlike the horizontal {@link mio_icif_entity_block#FACING}. */
    public static final DirectionProperty FACING = DirectionalBlock.FACING;
    public static final BooleanProperty ACTIVE = BooleanProperty.create("active");

    protected mio_icif_block_reactor_coolant_injector(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(ACTIVE, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, ACTIVE);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getNearestLookingDirection().getOpposite());
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof MenuProvider menu) player.openMenu(menu);
        return InteractionResult.sidedSuccess(level.isClientSide());
    }

    /** The reserve of an injector saved before 0.1.10.14; the shared machine removal spills the slots. */
    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!level.isClientSide() && !state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof AbstractReactorCoolantInjector injector) {
            injector.dropReserve(level, pos);
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    @Override
    protected boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    /** How full the 9 coolant slots are. */
    @Override
    protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
        if (!(level.getBlockEntity(pos) instanceof AbstractReactorCoolantInjector injector)) return 0;
        int filled = 0;
        for (int i = 0; i < AbstractReactorCoolantInjector.INPUT_SLOT_COUNT; i++) {
            if (!injector.getItemHandler().getStackInSlot(i).isEmpty()) filled++;
        }
        return filled * 15 / AbstractReactorCoolantInjector.INPUT_SLOT_COUNT;
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide()) return null;
        return (tickLevel, pos, tickState, blockEntity) -> {
            if (blockEntity instanceof AbstractReactorCoolantInjector injector) AbstractReactorCoolantInjector.tick(tickLevel, pos, tickState, injector);
        };
    }
}
