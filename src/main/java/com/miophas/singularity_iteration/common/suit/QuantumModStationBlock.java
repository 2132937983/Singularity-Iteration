// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.suit;

import com.miophas.singularity_iteration.common.block.mio_icif_entity_block;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/** Quantum modification station block: a work pad with an emitter frame. */
@SuppressWarnings("null")
public class QuantumModStationBlock extends mio_icif_entity_block {
    public static final MapCodec<QuantumModStationBlock> CODEC = simpleCodec(QuantumModStationBlock::new);
    private static final VoxelShape BASE = box(0, 0, 0, 16, 7, 16);
    /** Emitter frame at the back (model faces north, back = south). */
    private static final VoxelShape[] SHAPES = new VoxelShape[4];
    static {
        SHAPES[net.minecraft.core.Direction.NORTH.get2DDataValue()] = Shapes.or(BASE, box(1, 7, 12, 15, 16, 15));
        SHAPES[net.minecraft.core.Direction.SOUTH.get2DDataValue()] = Shapes.or(BASE, box(1, 7, 1, 15, 16, 4));
        SHAPES[net.minecraft.core.Direction.EAST.get2DDataValue()] = Shapes.or(BASE, box(1, 7, 1, 4, 16, 15));
        SHAPES[net.minecraft.core.Direction.WEST.get2DDataValue()] = Shapes.or(BASE, box(12, 7, 1, 15, 16, 15));
    }

    public QuantumModStationBlock(Properties properties) {
        super(properties);
    }

    @Override protected MapCodec<? extends BaseEntityBlock> codec() { return CODEC; }
    @Override public RenderShape getRenderShape(BlockState state) { return RenderShape.MODEL; }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPES[state.getValue(FACING).get2DDataValue()];
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.isClientSide) return InteractionResult.SUCCESS;
        if (level.getBlockEntity(pos) instanceof QuantumModStationBlockEntity station) {
            player.openMenu(station, buf -> buf.writeBlockPos(pos));
        }
        return InteractionResult.CONSUME;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new QuantumModStationBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide) return null;
        return createTickerHelper(type, SuitRegistry.STATION_ENTITY.get(), QuantumModStationBlockEntity::tick);
    }
}
