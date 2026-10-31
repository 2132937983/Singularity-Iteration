package com.miophas.singularity_iteration.common.block.generator;

import com.miophas.singularity_iteration.common.block.mio_icif_entity_block;
import com.miophas.singularity_iteration.common.blockentity.generator.SteamTunnelBlockEntity;
import com.miophas.singularity_iteration.common.registry.mio_icif_block_entities;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

/**
 * 0.1.10.39: the Steam Tunnel controller. It faces out of the front of the tunnel, which lies behind it (see
 * {@code SteamTunnelStructure}). No menu: a right-click shows the state (running, idle, or what is missing where). While the
 * tunnel is formed ({@link #FORMED}) its front is the hub of the big fan that the 8 casings around it show.
 */
@SuppressWarnings("null")
public class mio_icif_block_steam_tunnel extends mio_icif_entity_block {
    public static final BooleanProperty ACTIVE = BooleanProperty.create("active");
    public static final BooleanProperty FORMED = BooleanProperty.create("formed");

    public mio_icif_block_steam_tunnel(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(ACTIVE, false).setValue(FORMED, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(ACTIVE, FORMED);
        super.createBlockStateDefinition(builder);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return simpleCodec(mio_icif_block_steam_tunnel::new);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    /** Removed or turned: the casings of its front ring stop showing its fan. */
    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!newState.is(this) || newState.getValue(FACING) != state.getValue(FACING))
            SteamTunnelBlockEntity.showFan(level, pos, state.getValue(FACING), false);
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof SteamTunnelBlockEntity tunnel) {
            tunnel.validate();
            player.displayClientMessage(tunnel.status(), true);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new SteamTunnelBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide || type != mio_icif_block_entities.STEAM_TUNNEL_ENTITY_TYPE.get() ? null
            : (l, p, s, be) -> { if (be instanceof SteamTunnelBlockEntity tunnel) SteamTunnelBlockEntity.tick(l, p, s, tunnel); };
    }
}
