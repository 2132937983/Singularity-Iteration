package com.singularity_iteration.mio_icif.Blocks.generator;

import com.singularity_iteration.mio_icif.Blocks.entity.generator.mio_icif_rt_generator;
import com.singularity_iteration.mio_icif.Blocks.entity.mio_icif_block_entities;
import com.singularity_iteration.mio_icif.Blocks.mio_icif_entity_block;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
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
 * 放射性同位素温差发电力?(RTG) 方块
 *
 * 特点�? * - 6个槽位用于放置RTG燃料靶丸
 * - 靶丸无限耐久，不需要更新? * - 发电量与放入的靶丸数量有关：
 *   1�?= 1 EU/t
 *   2�?= 2 EU/t
 *   3�?= 4 EU/t
 *   4�?= 8 EU/t
 *   5�?= 16 EU/t
 *   6�?= 32 EU/t
 */
@SuppressWarnings("null")
public class mio_icif_Block_RT_Generator extends mio_icif_entity_block {

    // 定义方块状态：是否正在发电（有靶丸在工作）
    public static final BooleanProperty ACTIVE = BooleanProperty.create("active");

    public mio_icif_Block_RT_Generator(Properties properties) {
        super(properties);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        // FACING 由父类添加，只需要添�?ACTIVE
        builder.add(ACTIVE);
        super.createBlockStateDefinition(builder);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return simpleCodec(properties -> new mio_icif_Block_RT_Generator(properties));
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

    @Nullable
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return type == mio_icif_block_entities.RT_GENERATOR_ENTITY_TYPE.get() ?
            (l, p, s, be) -> {
                if (be instanceof mio_icif_rt_generator generator) {
                    mio_icif_rt_generator.tick(l, p, s, generator);
                }
            } : null;
    }

    @Nullable
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return mio_icif_block_entities.RT_GENERATOR_ENTITY_TYPE.get().create(pos, state);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
            BlockHitResult hitResult) {
        if (!level.isClientSide) {
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity instanceof MenuProvider) {
                player.openMenu((MenuProvider) blockEntity);
            } else {
                player.sendSystemMessage(Component.translatable("message.mio_icif.generator.gui_open_failed", Component.translatable("block.mio_icif.generator.block_rt_generator")));
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}


