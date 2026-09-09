package com.singularity_iteration.mio_icif.Blocks.Producer;

import com.singularity_iteration.mio_icif.Blocks.entity.mio_icif_block_entities;
import com.singularity_iteration.mio_icif.Blocks.entity.producer.mio_icif_furnace_elc;
import com.singularity_iteration.mio_icif.Blocks.mio_icif_entity_block;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
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
import org.slf4j.Logger;
import com.mojang.logging.LogUtils;

/**
 * 电炉方方块?
 * 使用电力进行冶炼的机器方法?
 */
@SuppressWarnings("null")
public class mio_icif_block_furnace_elc extends mio_icif_entity_block {
    private static final Logger LOGGER = LogUtils.getLogger();

    public static final BooleanProperty LIT = BooleanProperty.create("lit");

    public static final MapCodec<mio_icif_block_furnace_elc> CODEC = RecordCodecBuilder.mapCodec(instance ->
        instance.group(propertiesCodec()).apply(instance, mio_icif_block_furnace_elc::new));

    public mio_icif_block_furnace_elc(Properties properties) {
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
        LOGGER.info("Furnace right-clicked! isClientSide: " + level.isClientSide());
        if (!level.isClientSide()) {
            BlockEntity blockEntity = level.getBlockEntity(pos);
            LOGGER.info("Is MenuProvider: " + (blockEntity instanceof MenuProvider));
            if (blockEntity instanceof MenuProvider) {
                player.openMenu((MenuProvider) blockEntity);
            } else {
                player.sendSystemMessage(Component.literal("This block does not have a GUI!"));
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide());
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (state.getBlock() != newState.getBlock()) {
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity instanceof mio_icif_furnace_elc furnace) {
                // 掉落物品栏中的物�?
                for (int i = 0; i < furnace.getItemHandler().getSlots(); i++) {
                    ItemStack stack = furnace.getItemHandler().getStackInSlot(i);
                    if (!stack.isEmpty()) {
                        Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), stack);
                    }
                }
            }
            super.onRemove(state, level, pos, newState, movedByPiston);
        }
    }

    @Override
    protected boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity instanceof mio_icif_furnace_elc furnace) {
            // 计算红石信号强度（基于物品栏填充程度�?
            var handler = furnace.getItemHandler();
            int totalSlots = handler.getSlots();
            int filledSlots = 0;
            for (int i = 0; i < totalSlots; i++) {
                if (!handler.getStackInSlot(i).isEmpty()) {
                    filledSlots++;
                }
            }
            return (filledSlots * 15) / totalSlots;
        }
        return 0;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new mio_icif_furnace_elc(pos, state, mio_icif_block_entities.FURNACE_ELC_ENTITY_TYPE.get());
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> blockEntityType) {
        if (level.isClientSide()) {
            return null;
        }
        return (lvl, pos, blockState, blockEntity) -> {
            if (blockEntity instanceof mio_icif_furnace_elc furnace) {
                mio_icif_furnace_elc.tick(lvl, pos, blockState, furnace);
            }
        };
    }
}

