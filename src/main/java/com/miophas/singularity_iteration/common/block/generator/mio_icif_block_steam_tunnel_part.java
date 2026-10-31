package com.miophas.singularity_iteration.common.block.generator;

import com.miophas.singularity_iteration.common.blockentity.generator.SteamTunnelBlockEntity;
import com.miophas.singularity_iteration.common.blockentity.generator.SteamTunnelFluidPortBlockEntity;
import com.miophas.singularity_iteration.common.blockentity.generator.SteamTunnelPowerPortBlockEntity;
import com.miophas.singularity_iteration.common.blockentity.generator.SteamTunnelStructure;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
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
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

/**
 * 0.1.10.39: a shell block of the Steam Tunnel: the casing ({@link Casing}), and the fluid and power ports ({@link Port}). Removing
 * one makes the controllers in reach check their structure at once. Shell blocks join the shell blocks next to them ({@link #LINKS}):
 * the connected-texture model ({@code SteamTunnelShellModel}) draws the border of a face only toward its sides without one, so a
 * wall of casings and ports reads as one panel.
 */
@SuppressWarnings("null")
public class mio_icif_block_steam_tunnel_part extends Block {
    public static final Map<Direction, BooleanProperty> LINKS = PipeBlock.PROPERTY_BY_DIRECTION;

    public mio_icif_block_steam_tunnel_part(Properties properties) {
        super(properties);
        BlockState state = stateDefinition.any();
        for (BooleanProperty link : LINKS.values()) state = state.setValue(link, false);
        registerDefaultState(state);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(LINKS.values().toArray(BooleanProperty[]::new));
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return linked(defaultBlockState(), context.getLevel(), context.getClickedPos());
    }

    @Override
    protected BlockState updateShape(BlockState state, Direction direction, BlockState neighbour, LevelAccessor level, BlockPos pos, BlockPos neighbourPos) {
        return state.setValue(LINKS.get(direction), joins(neighbour));
    }

    /** Placed without looking at its neighbours (a plain setBlock, the blueprint builder): it joins them on the next tick. */
    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState old, boolean movedByPiston) {
        super.onPlace(state, level, pos, old, movedByPiston);
        if (!level.isClientSide && !old.is(this) && linked(state, level, pos) != state) level.scheduleTick(pos, this, 1);
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        BlockState linked = linked(state, level, pos);
        if (linked != state) level.setBlock(pos, linked, UPDATE_ALL);
    }

    private static boolean joins(BlockState neighbour) {
        return neighbour.getBlock() instanceof mio_icif_block_steam_tunnel_part;
    }

    private static BlockState linked(BlockState state, BlockGetter level, BlockPos pos) {
        for (Map.Entry<Direction, BooleanProperty> link : LINKS.entrySet())
            state = state.setValue(link.getValue(), joins(level.getBlockState(pos.relative(link.getKey()))));
        return state;
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        BlockState out = state;
        for (Direction side : Direction.Plane.HORIZONTAL) out = out.setValue(LINKS.get(rotation.rotate(side)), state.getValue(LINKS.get(side)));
        return out;
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        BlockState out = state;
        for (Direction side : Direction.Plane.HORIZONTAL) out = out.setValue(LINKS.get(mirror.mirror(side)), state.getValue(LINKS.get(side)));
        return out;
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        boolean replaced = !state.is(newState.getBlock());
        super.onRemove(state, level, pos, newState, movedByPiston);
        if (replaced) SteamTunnelBlockEntity.partRemoved(level, pos);
    }

    /**
     * The casing. On the front ring of a formed tunnel it shows its piece of the big fan instead of the panel ({@link #FAN}),
     * spinning while the tunnel runs ({@link #SPINNING}); the controller sets and clears both.
     */
    public static class Casing extends mio_icif_block_steam_tunnel_part {
        public static final EnumProperty<SteamTunnelStructure.Fan> FAN = EnumProperty.create("fan", SteamTunnelStructure.Fan.class);
        public static final BooleanProperty SPINNING = BooleanProperty.create("spinning");

        public Casing(Properties properties) {
            super(properties);
            registerDefaultState(defaultBlockState().setValue(FAN, SteamTunnelStructure.Fan.NONE).setValue(SPINNING, false));
        }

        @Override
        protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
            super.createBlockStateDefinition(builder);
            builder.add(FAN, SPINNING);
        }

        @Override
        protected BlockState rotate(BlockState state, Rotation rotation) {
            return super.rotate(state, rotation).setValue(FAN, state.getValue(FAN).rotate(rotation));
        }

        @Override
        protected BlockState mirror(BlockState state, Mirror mirror) {
            return super.mirror(state, mirror).setValue(FAN, state.getValue(FAN).mirror(mirror));
        }
    }

    /** A fluid port ({@code fluid}; its menu holds a Fluid Ejector or Fluid Pulling Upgrade) or a power port. */
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

        @Override
        protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
            if (!fluid) return InteractionResult.PASS;
            if (!level.isClientSide && level.getBlockEntity(pos) instanceof SteamTunnelFluidPortBlockEntity port) player.openMenu(port);
            return InteractionResult.sidedSuccess(level.isClientSide);
        }

        @Override
        protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
            if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof SteamTunnelFluidPortBlockEntity port)
                for (int i = 0; i < port.getItemHandler().getSlots(); i++) {
                    ItemStack stack = port.getItemHandler().getStackInSlot(i);
                    if (!stack.isEmpty()) Block.popResource(level, pos, stack);
                }
            super.onRemove(state, level, pos, newState, movedByPiston);
        }
    }
}
