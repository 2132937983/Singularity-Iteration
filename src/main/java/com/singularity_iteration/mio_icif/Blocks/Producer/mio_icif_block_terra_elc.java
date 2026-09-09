package com.singularity_iteration.mio_icif.Blocks.Producer;

import com.singularity_iteration.mio_icif.Blocks.entity.producer.mio_icif_terra_elc;
import com.singularity_iteration.mio_icif.Blocks.mio_icif_entity_block;
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
 * 地形转换机方块类
 * 环境改造机，能够改造环境（改变方块?
 * 没有GUI，通过手持转换模板右键放入/蹲下取出
 * 作用范围：以机器为圆心直?56x256?
 * 需要通电才能工作，会慢改造而非瞬间完成
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

        // 玩家蹲下时：尝试取出模板
        if (player.isCrouching()) {
            ItemStack template = terraEntity.extractTemplate();
            if (!template.isEmpty()) {
                // 尝试将模板放入玩家背?
                if (!player.getInventory().add(template)) {
                    // 如果背包满了，掉落物?
                    Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), template);
                }
                player.sendSystemMessage(Component.translatable("message.mio_icif.terra_elc.template_removed"));
                return ItemInteractionResult.SUCCESS;
            }
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }

        // 玩家站立时：尝试放入模板
        if (!stack.isEmpty()) {
            // 检查是否是地形转换模板
            // TODO: 创建地形转换模板物品后，这里需要更新为检查具体物品类?
            // 暂时接受任何物品作为模板

            // 如果已有模板，提示玩?
            if (terraEntity.hasTemplate()) {
                player.sendSystemMessage(Component.translatable("message.mio_icif.terra_elc.has_template"));
                return ItemInteractionResult.FAIL;
            }

            // 插入模板
            if (terraEntity.insertTemplate(stack.copyWithCount(1))) {
                // 消耗玩家手中的一个物?
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

        // 玩家蹲下时：尝试取出模板
        if (player.isCrouching()) {
            ItemStack template = terraEntity.extractTemplate();
            if (!template.isEmpty()) {
                // 尝试将模板放入玩家背?
                if (!player.getInventory().add(template)) {
                    // 如果背包满了，掉落物?
                    Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), template);
                }
                player.sendSystemMessage(Component.translatable("message.mio_icif.terra_elc.template_removed"));
                return InteractionResult.SUCCESS;
            }
            return InteractionResult.PASS;
        }

        // 空手右键：显示状态信息?
        if (terraEntity.hasTemplate()) {
            player.sendSystemMessage(Component.translatable("message.mio_icif.terra_elc.status.has_template",
                terraEntity.getTemplate().getDisplayName().getString()));
        } else {
            player.sendSystemMessage(Component.translatable("message.mio_icif.terra_elc.status.no_template"));
        }

        // 显示能量状?
        long energy = terraEntity.getEnergyStorage().getAmount();
        long capacity = terraEntity.getEnergyStorage().getCapacity();
        player.sendSystemMessage(Component.translatable("message.mio_icif.terra_elc.status.energy", energy, capacity));

        return InteractionResult.SUCCESS;
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
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

