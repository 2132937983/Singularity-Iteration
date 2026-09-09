package com.singularity_iteration.mio_icif.Blocks.Producer;

import com.singularity_iteration.mio_icif.Blocks.entity.mio_icif_block_entities;
import com.singularity_iteration.mio_icif.Blocks.entity.producer.mio_icif_condenser;
import com.singularity_iteration.mio_icif.Blocks.mio_icif_entity_block;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import org.jetbrains.annotations.Nullable;

/**
 * 冷凝机方块类
 */
@SuppressWarnings("null")
public class mio_icif_block_condenser extends mio_icif_entity_block {

    public static final BooleanProperty LIT = BooleanProperty.create("lit");

    public static final MapCodec<mio_icif_block_condenser> CODEC = RecordCodecBuilder.mapCodec(instance ->
        instance.group(propertiesCodec()).apply(instance, mio_icif_block_condenser::new));

    public mio_icif_block_condenser(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(LIT, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(LIT);
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
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (!level.isClientSide()) {
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity instanceof mio_icif_condenser condenser) {
                MenuProvider menuProvider = new SimpleMenuProvider(
                    (containerId, playerInventory, playerEntity) -> new com.singularity_iteration.mio_icif.Menu.Producer.CondenserMenu(containerId, playerInventory, condenser),
                    Component.translatable("container.mio_icif.condenser")
                );
                player.openMenu(menuProvider);
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide());
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new mio_icif_condenser(pos, state, mio_icif_block_entities.CONDENSER_ENTITY_TYPE.get());
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> blockEntityType) {
        if (level.isClientSide()) return null;
        return (lvl, pos, blockState, blockEntity) -> {
            if (blockEntity instanceof mio_icif_condenser condenser) {
                mio_icif_condenser.tick(lvl, pos, blockState, condenser);
            }
        };
    }
}

