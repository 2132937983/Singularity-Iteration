package com.miophas.singularity_iteration.common.block.fusion;

import com.miophas.singularity_iteration.common.blockentity.fusion.FusionControllerBlockEntity;
import com.miophas.singularity_iteration.common.blockentity.fusion.FusionItemPortBlockEntity;
import com.miophas.singularity_iteration.common.blockentity.fusion.FusionPowerPortBlockEntity;
import com.miophas.singularity_iteration.common.registry.mio_icif_block_entities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

/**
 * 0.1.11.89: a block of the fusion reactor (a tokamak). {@link Facing} is a part with a front (the ports): it faces the player
 * who places it. 0.1.11.92: removing a part makes the controllers in reach check their structure at once (a plasma loses its
 * structure and disrupts); {@link PowerPort} is the power output port, which sends the reactor's EU. 0.1.11.97: {@link ItemPort}
 * is the fuel injection port and the disruption mitigation port, with slots and a screen.
 */
public class mio_icif_block_fusion_part extends Block {
    public mio_icif_block_fusion_part(Properties properties) {
        super(properties);
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        boolean replaced = !state.is(newState.getBlock());
        super.onRemove(state, level, pos, newState, movedByPiston);
        if (replaced) FusionControllerBlockEntity.partRemoved(level, pos);
    }

    /** A fusion reactor part with a front: the ports. */
    public static class Facing extends mio_icif_block_fusion_part {
        public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;

        public Facing(Properties properties) {
            super(properties);
            registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
        }

        @Override
        protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
            builder.add(FACING);
        }

        @Override
        public BlockState getStateForPlacement(BlockPlaceContext context) {
            return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
        }

        @Override
        protected BlockState rotate(BlockState state, Rotation rotation) {
            return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
        }

        @Override
        protected BlockState mirror(BlockState state, Mirror mirror) {
            return state.rotate(mirror.getRotation(state.getValue(FACING)));
        }
    }

    /**
     * 0.1.11.97: the fuel injection port and the disruption mitigation port: slots of their own and a screen (a right click
     * opens it); breaking one drops what it holds.
     */
    public static class ItemPort extends Facing implements EntityBlock {
        public ItemPort(Properties properties) {
            super(properties);
        }

        @Override
        public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
            return new FusionItemPortBlockEntity(pos, state);
        }

        @Override
        protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
            if (!level.isClientSide && level.getBlockEntity(pos) instanceof FusionItemPortBlockEntity port) player.openMenu(port);
            return InteractionResult.sidedSuccess(level.isClientSide);
        }

        @Override
        protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
            if (!state.is(newState.getBlock()) && !level.isClientSide && level.getBlockEntity(pos) instanceof FusionItemPortBlockEntity port)
                for (ItemStack stack : port.contents()) Block.popResource(level, pos, stack);
            super.onRemove(state, level, pos, newState, movedByPiston);
        }
    }

    /** 0.1.11.92: the power output port: a block entity that sends the reactor's EU into the network. */
    public static class PowerPort extends Facing implements EntityBlock {
        public PowerPort(Properties properties) {
            super(properties);
        }

        @Override
        public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
            return new FusionPowerPortBlockEntity(pos, state);
        }

        @Override
        public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
            if (level.isClientSide || type != mio_icif_block_entities.FUSION_POWER_PORT_ENTITY_TYPE.get()) return null;
            return (l, p, s, be) -> { if (be instanceof FusionPowerPortBlockEntity port) FusionPowerPortBlockEntity.tick(l, p, s, port); };
        }
    }
}
