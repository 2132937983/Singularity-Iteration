package com.singularity_iteration.mio_icif.Blocks.Producer;

import com.singularity_iteration.mio_icif.Blocks.entity.mio_icif_block_entities;
import com.singularity_iteration.mio_icif.Blocks.entity.producer.mio_icif_industrial_workbench;
import com.singularity_iteration.mio_icif.Blocks.mio_icif_entity_block;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

/**
 * 工业工作台方块类
 * 提供3x3合成、工具合成（锻造锤+材料、剪线钳+材料）和存储功能
 */
public class mio_icif_block_industrial_workbench extends mio_icif_entity_block {

    public static final MapCodec<mio_icif_block_industrial_workbench> CODEC = RecordCodecBuilder.mapCodec(instance ->
        instance.group(propertiesCodec()).apply(instance, mio_icif_block_industrial_workbench::new));

    public mio_icif_block_industrial_workbench(Properties properties) {
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
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult) {
        System.out.println("[IndustrialWorkbenchDebug] useItemOn called! level.isClientSide=" + level.isClientSide());
        if (level.isClientSide()) {
            System.out.println("[IndustrialWorkbenchDebug] Client side, returning SUCCESS");
            return ItemInteractionResult.SUCCESS;
        }

        System.out.println("[IndustrialWorkbenchDebug] Server side, getting block entity at " + pos);
        BlockEntity blockEntity = level.getBlockEntity(pos);
        System.out.println("[IndustrialWorkbenchDebug] Block entity type: " + (blockEntity != null ? blockEntity.getClass().getSimpleName() : "null"));
        
        if (blockEntity instanceof MenuProvider) {
            System.out.println("[IndustrialWorkbenchDebug] Block entity IS a MenuProvider, opening menu");
            player.openMenu((MenuProvider) blockEntity);
            return ItemInteractionResult.SUCCESS;
        } else {
            System.out.println("[IndustrialWorkbenchDebug] Block entity is NOT a MenuProvider!");
            player.sendSystemMessage(Component.literal("This block does not have a GUI!"));
            return ItemInteractionResult.FAIL;
        }
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (state.getBlock() != newState.getBlock()) {
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity instanceof mio_icif_industrial_workbench workbench) {
                // 掉落所有物品栏中的物品
                var handler = workbench.getItemHandler();
                for (int i = 0; i < handler.getSlots(); i++) {
                    ItemStack stack = handler.getStackInSlot(i);
                    if (!stack.isEmpty()) {
                        Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), stack);
                    }
                }
            }
            super.onRemove(state, level, pos, newState, movedByPiston);
        }
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new mio_icif_industrial_workbench(pos, state, mio_icif_block_entities.INDUSTRIAL_WORKBENCH_ENTITY_TYPE.get());
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> blockEntityType) {
        // 工业工作台不需要tick更新
        return null;
    }
}