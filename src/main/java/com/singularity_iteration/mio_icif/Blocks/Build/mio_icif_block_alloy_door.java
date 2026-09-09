package com.singularity_iteration.mio_icif.Blocks.Build;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockSetType;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.phys.BlockHitResult;

@SuppressWarnings("null")
public class mio_icif_block_alloy_door extends DoorBlock {

    public static final MapCodec<mio_icif_block_alloy_door> CODEC = RecordCodecBuilder.mapCodec(
        p -> p.group(
            BlockSetType.CODEC.fieldOf("block_set_type").forGetter(mio_icif_block_alloy_door::type),
            propertiesCodec()
        ).apply(p, mio_icif_block_alloy_door::new)
    );

    public mio_icif_block_alloy_door(BlockSetType type, Properties properties) {
        super(type, properties);
    }

    @Override
    public MapCodec<? extends DoorBlock> codec() {
        return CODEC;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (state.getValue(HALF) == DoubleBlockHalf.UPPER) {
            pos = pos.below();
            state = level.getBlockState(pos);
        }
        state = state.cycle(OPEN);
        level.setBlock(pos, state, 10);
        level.levelEvent(player, state.getValue(OPEN) ? 1005 : 1011, pos, 0);
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}

