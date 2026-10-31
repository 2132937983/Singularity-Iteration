package com.miophas.singularity_iteration.common.block.generator;

import com.miophas.singularity_iteration.common.blockentity.generator.SteamTunnelBlockEntity;
import com.miophas.singularity_iteration.common.blockentity.generator.SteamTunnelFluidPortBlockEntity;
import com.miophas.singularity_iteration.common.blockentity.generator.SteamTunnelPowerPortBlockEntity;
import com.miophas.singularity_iteration.common.blockentity.generator.SteamTunnelStructure;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.PipeBlock;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import org.jetbrains.annotations.Nullable;

/**
 * 0.1.10.39: a shell block of the Steam Tunnel: the casing ({@link Casing}), and the fluid and power ports ({@link Port}). Removing
 * one makes the controllers in reach check their structure at once.
 */
@SuppressWarnings("null")
public class mio_icif_block_steam_tunnel_part extends Block {
    public mio_icif_block_steam_tunnel_part(Properties properties) {
        super(properties);
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        boolean replaced = !state.is(newState.getBlock());
        super.onRemove(state, level, pos, newState, movedByPiston);
        if (replaced) SteamTunnelBlockEntity.partRemoved(level, pos);
    }

    /**
     * The casing, with connected textures: each face draws its border only toward the sides without a casing next to it
     * ({@link #LINKS}), so a wall of casings reads as one panel. On the front ring of a formed tunnel it shows a piece of the big fan
     * instead ({@link #FAN}, set and cleared by the controller).
     */
    public static class Casing extends mio_icif_block_steam_tunnel_part {
        public static final Map<Direction, BooleanProperty> LINKS = PipeBlock.PROPERTY_BY_DIRECTION;
        public static final EnumProperty<SteamTunnelStructure.Fan> FAN = EnumProperty.create("fan", SteamTunnelStructure.Fan.class);

        public Casing(Properties properties) {
            super(properties);
            BlockState state = stateDefinition.any().setValue(FAN, SteamTunnelStructure.Fan.NONE);
            for (BooleanProperty link : LINKS.values()) state = state.setValue(link, false);
            registerDefaultState(state);
        }

        @Override
        protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
            builder.add(LINKS.values().toArray(BooleanProperty[]::new)).add(FAN);
        }

        @Override
        public BlockState getStateForPlacement(BlockPlaceContext context) {
            return linked(defaultBlockState(), context.getLevel(), context.getClickedPos());
        }

        @Override
        protected BlockState updateShape(BlockState state, Direction direction, BlockState neighbour, LevelAccessor level, BlockPos pos, BlockPos neighbourPos) {
            return state.setValue(LINKS.get(direction), neighbour.is(this));
        }

        /**
         * Placed without looking at its neighbours (a plain setBlock, the blueprint builder): it joins them now. The outer setBlock
         * then skips its updates (the state changed under it), so this one makes them all, the neighbours' shape updates included.
         */
        @Override
        protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState old, boolean movedByPiston) {
            super.onPlace(state, level, pos, old, movedByPiston);
            if (level.isClientSide || old.is(this)) return;
            BlockState linked = linked(state, level, pos);
            if (linked != state) level.setBlock(pos, linked, UPDATE_ALL);
        }

        private BlockState linked(BlockState state, BlockGetter level, BlockPos pos) {
            for (Map.Entry<Direction, BooleanProperty> link : LINKS.entrySet())
                state = state.setValue(link.getValue(), level.getBlockState(pos.relative(link.getKey())).is(this));
            return state;
        }

        @Override
        protected BlockState rotate(BlockState state, Rotation rotation) {
            BlockState out = state.setValue(FAN, state.getValue(FAN).rotate(rotation));
            for (Direction side : Direction.Plane.HORIZONTAL) out = out.setValue(LINKS.get(rotation.rotate(side)), state.getValue(LINKS.get(side)));
            return out;
        }

        @Override
        protected BlockState mirror(BlockState state, Mirror mirror) {
            BlockState out = state.setValue(FAN, state.getValue(FAN).mirror(mirror));
            for (Direction side : Direction.Plane.HORIZONTAL) out = out.setValue(LINKS.get(mirror.mirror(side)), state.getValue(LINKS.get(side)));
            return out;
        }
    }

    /** A fluid port ({@code fluid}) or a power port. */
    public static class Port extends mio_icif_block_steam_tunnel_part implements EntityBlock {
        private final boolean fluid;

        public Port(Properties properties, boolean fluid) {
            super(properties);
            this.fluid = fluid;
        }

        @Nullable
        @Override
        public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
            return fluid ? new SteamTunnelFluidPortBlockEntity(pos, state) : new SteamTunnelPowerPortBlockEntity(pos, state);
        }

        @Nullable
        @Override
        public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
            if (level.isClientSide) return null;
            return fluid ? (l, p, s, be) -> { if (be instanceof SteamTunnelFluidPortBlockEntity port) SteamTunnelFluidPortBlockEntity.tick(l, p, s, port); }
                : (l, p, s, be) -> { if (be instanceof SteamTunnelPowerPortBlockEntity port) SteamTunnelPowerPortBlockEntity.tick(l, p, s, port); };
        }
    }
}
