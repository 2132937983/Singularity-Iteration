package com.singularity_iteration.mio_icif.Blocks.OilRig;

import com.singularity_iteration.mio_icif.Blocks.entity.oilrig.mio_icif_oil_rig_base_entity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import com.mojang.serialization.MapCodec;
import org.jetbrains.annotations.Nullable;
import net.minecraft.world.level.Level;

@SuppressWarnings("null")
public class mio_icif_block_oil_rig_base extends mio_icif_Block_OilRig_Multiblock_Base {

    public static final MapCodec<mio_icif_block_oil_rig_base> CODEC = simpleCodec(mio_icif_block_oil_rig_base::new);

    public mio_icif_block_oil_rig_base(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new mio_icif_oil_rig_base_entity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> blockEntityType) {
        return null;
    }
}