package com.miophas.singularity_iteration.common.block.producer;

import com.miophas.singularity_iteration.common.blockentity.producer.mio_icif_terra_elc;
import com.miophas.singularity_iteration.common.item.envtemplate.mio_icif_EvT_default;
import com.miophas.singularity_iteration.common.block.mio_icif_entity_block;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
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

/**
 * Block class for the terrain converter.
 * The terrain converter changes the environment. It replaces blocks.
 * It has no GUI. Right-click with a conversion template to insert it. Crouch and right-click to remove it.
 * Work area: a 256x256 area with the machine at the center.
 * The machine needs electric power to work. It changes the area slowly, not at once.
 */
@SuppressWarnings("null")
public class mio_icif_block_terra_elc extends mio_icif_entity_block {

    public static final BooleanProperty LIT = BooleanProperty.create("lit");

    public static final MapCodec<mio_icif_block_terra_elc> CODEC = RecordCodecBuilder.mapCodec(instance ->
        instance.group(propertiesCodec()).apply(instance, mio_icif_block_terra_elc::new));

    public mio_icif_block_terra_elc(Properties properties) {
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
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult) {
        if (level.isClientSide()) {
            return ItemInteractionResult.SUCCESS;
        }

        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (!(blockEntity instanceof mio_icif_terra_elc terraEntity)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }

        // When the player crouches, try to remove the template.
        if (player.isCrouching()) {
            ItemStack template = terraEntity.extractTemplate();
            if (!template.isEmpty()) {
                // Try to put the template into the player inventory.
                if (!player.getInventory().add(template)) {
                    // If the inventory is full, drop the item.
                    Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), template);
                }
                player.sendSystemMessage(Component.translatable("message.mio_icif.terra_elc.template_removed"));
                return ItemInteractionResult.SUCCESS;
            }
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }

        // When the player stands, try to insert the template.
        if (!stack.isEmpty()) {
            // Only registered environment templates are valid.  Rejecting the
            // item at the block boundary avoids consuming arbitrary items when
            // the machine slot later rejects them.
            if (!(stack.getItem() instanceof mio_icif_EvT_default template) || !template.isUsable()) {
                return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
            }

            // If the machine already has a template, tell the player.
            if (terraEntity.hasTemplate()) {
                player.sendSystemMessage(Component.translatable("message.mio_icif.terra_elc.has_template"));
                return ItemInteractionResult.FAIL;
            }

            // Insert the template.
            if (terraEntity.insertTemplate(stack.copyWithCount(1))) {
                // Remove one item from the player hand.
                stack.shrink(1);
                player.sendSystemMessage(Component.translatable("message.mio_icif.terra_elc.template_inserted"));
                return ItemInteractionResult.SUCCESS;
            }
        }

        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }

        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (!(blockEntity instanceof mio_icif_terra_elc terraEntity)) {
            return InteractionResult.PASS;
        }

        // When the player crouches, try to remove the template.
        if (player.isCrouching()) {
            ItemStack template = terraEntity.extractTemplate();
            if (!template.isEmpty()) {
                // Try to put the template into the player inventory.
                if (!player.getInventory().add(template)) {
                    // If the inventory is full, drop the item.
                    Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), template);
                }
                player.sendSystemMessage(Component.translatable("message.mio_icif.terra_elc.template_removed"));
                return InteractionResult.SUCCESS;
            }
            return InteractionResult.PASS;
        }

        // Right-click with an empty hand: show the status.
        if (terraEntity.hasTemplate()) {
            player.sendSystemMessage(Component.translatable("message.mio_icif.terra_elc.status.has_template",
                terraEntity.getTemplate().getDisplayName().getString()));
        } else {
            player.sendSystemMessage(Component.translatable("message.mio_icif.terra_elc.status.no_template"));
        }

        // Show the energy status.
        long energy = terraEntity.getEnergyStorage().getAmount();
        long capacity = terraEntity.getEnergyStorage().getCapacity();
        player.sendSystemMessage(Component.translatable("message.mio_icif.terra_elc.status.energy", energy, capacity));

        return InteractionResult.SUCCESS;
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (level.isClientSide()) {
            super.onRemove(state, level, pos, newState, movedByPiston);
            return;
        }
        if (state.getBlock() != newState.getBlock()) {
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity instanceof mio_icif_terra_elc terraEntity) {
                terraEntity.dropContents(level, pos);
            }
            super.onRemove(state, level, pos, newState, movedByPiston);
        }
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new mio_icif_terra_elc(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> blockEntityType) {
        if (level.isClientSide()) {
            return null;
        }
        return (lvl, pos, blockState, blockEntity) -> {
            if (blockEntity instanceof mio_icif_terra_elc terra) {
                mio_icif_terra_elc.tick(lvl, pos, blockState, terra);
            }
        };
    }
}

