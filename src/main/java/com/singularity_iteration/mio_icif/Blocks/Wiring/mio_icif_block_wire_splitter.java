package com.singularity_iteration.mio_icif.Blocks.Wiring;

import com.singularity_iteration.mio_icif.Blocks.Wire.mio_icif_block_wire;
import com.singularity_iteration.mio_icif.Blocks.entity.mio_icif_block_entities;
import com.singularity_iteration.mio_icif.Blocks.entity.wiring.mio_icif_wire_splitter;
import com.singularity_iteration.mio_icif.energy.EnergyUnit.CableTier;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import org.jetbrains.annotations.Nullable;

@SuppressWarnings("null")
public class mio_icif_block_wire_splitter extends mio_icif_block_wire {

    public static final BooleanProperty ACTIVE = BooleanProperty.create("active");

    public static final MapCodec<mio_icif_block_wire_splitter> CODEC = RecordCodecBuilder.mapCodec(instance ->
        instance.group(propertiesCodec()).apply(instance, mio_icif_block_wire_splitter::new));

    public mio_icif_block_wire_splitter(Properties properties) {
        super(properties, CableTier.IV, true);
        this.registerDefaultState(this.defaultBlockState().setValue(ACTIVE, true));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(ACTIVE);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new mio_icif_wire_splitter(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (!level.isClientSide()) {
            return createTickerHelper(type, mio_icif_block_entities.WIRE_SPLITTER.get(),
                (lvl, pos, st, blockEntity) -> mio_icif_wire_splitter.tick(lvl, pos, st, blockEntity));
        }
        return null;
    }
}