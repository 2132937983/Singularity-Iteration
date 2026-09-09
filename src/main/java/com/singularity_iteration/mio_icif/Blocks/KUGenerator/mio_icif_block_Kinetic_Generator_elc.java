package com.singularity_iteration.mio_icif.Blocks.KUGenerator;

import com.singularity_iteration.mio_icif.Blocks.entity.KUEntity.KUGenerator.mio_icif_Kinetic_Generator_elc;
import com.singularity_iteration.mio_icif.Blocks.entity.mio_icif_block_entities;
import com.singularity_iteration.mio_icif.Blocks.mio_icif_entity_block;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
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
 * 电力动能机方法? * 使用 FE 电力产生 KU 动能
 */
@SuppressWarnings("null")
public class mio_icif_block_Kinetic_Generator_elc extends mio_icif_entity_block {
    
    // 定义方块状态：是否正在工作
    public static final BooleanProperty ACTIVE = BooleanProperty.create("active");
    
    public mio_icif_block_Kinetic_Generator_elc(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(ACTIVE, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        // FACING 由父类添加，只需要添�?ACTIVE
        builder.add(ACTIVE);
    }

    
    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return simpleCodec(properties -> new mio_icif_block_Kinetic_Generator_elc(properties));
    }
    
    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }
    
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState()
            .setValue(FACING, context.getHorizontalDirection().getOpposite())
            .setValue(ACTIVE, false);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (!level.isClientSide()) {
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity instanceof MenuProvider menuProvider) {
                player.openMenu(menuProvider);
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide());
    }

    @Nullable
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return type == mio_icif_block_entities.KINETIC_GENERATOR_ELC_ENTITY_TYPE.get() ?
            (l, p, s, be) -> {
                if (be instanceof mio_icif_Kinetic_Generator_elc generator) {
                    mio_icif_Kinetic_Generator_elc.tick(l, p, s, generator);
                }
            } : null;
    }

    @Nullable
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return mio_icif_block_entities.KINETIC_GENERATOR_ELC_ENTITY_TYPE.get().create(pos, state);
    }
}


