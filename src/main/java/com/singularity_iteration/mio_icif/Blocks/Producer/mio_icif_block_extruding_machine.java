package com.singularity_iteration.mio_icif.Blocks.Producer;

import com.singularity_iteration.mio_icif.Blocks.entity.producer.mio_icif_metal_former_advanced;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

@Deprecated
@SuppressWarnings("null")
public class mio_icif_block_extruding_machine extends mio_icif_block_metal_former_advanced {

    public static final MapCodec<mio_icif_block_extruding_machine> CODEC = RecordCodecBuilder.mapCodec(instance ->
        instance.group(propertiesCodec()).apply(instance, mio_icif_block_extruding_machine::new));

    public mio_icif_block_extruding_machine(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new mio_icif_metal_former_advanced(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> blockEntityType) {
        if (level.isClientSide()) {
            return null;
        }
        return (lvl, pos, blockState, blockEntity) -> {
            if (blockEntity instanceof mio_icif_metal_former_advanced metalFormer) {
                mio_icif_metal_former_advanced.tick(lvl, pos, blockState, metalFormer);
            }
        };
    }
}