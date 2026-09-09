package com.singularity_iteration.mio_icif.Blocks.Producer;

import com.singularity_iteration.mio_icif.Blocks.entity.mio_icif_block_entities;
import com.singularity_iteration.mio_icif.Blocks.entity.producer.mio_icif_replicator_elc;
import com.singularity_iteration.mio_icif.Blocks.mio_icif_entity_block;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleMenuProvider;
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
 * 复制机方块类
 * 用于根据记忆水晶中的信息复制物品
 */
@SuppressWarnings("null")
public class mio_icif_block_replicator_elc extends mio_icif_entity_block {

    // 运行状态属性，用于控制方块的光照和纹理变化
    public static final BooleanProperty LIT = BooleanProperty.create("lit");
    // 复制中状态?
    public static final BooleanProperty REPLICATING = BooleanProperty.create("replicating");

    // 方块编码器，用于数据生成和序列化
    public static final MapCodec<mio_icif_block_replicator_elc> CODEC = RecordCodecBuilder.mapCodec(instance ->
        instance.group(propertiesCodec()).apply(instance, mio_icif_block_replicator_elc::new));

    public mio_icif_block_replicator_elc(Properties properties) {
        super(properties);
        // 注册默认状态：未点亮，未复�?
        this.registerDefaultState(this.stateDefinition.any()
            .setValue(LIT, false)
            .setValue(REPLICATING, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        // 添加 LIT �?REPLICATING 属性到方块状态定�?
        builder.add(LIT, REPLICATING);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        // 使用模型渲染
        return RenderShape.MODEL;
    }

    /**
     * 玩家右键点击方块时的处理
     * 打开 GUI 界面
     */
    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (!level.isClientSide()) {
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity instanceof mio_icif_replicator_elc replicator) {
                MenuProvider menuProvider = new SimpleMenuProvider(
                    (containerId, playerInventory, playerEntity) -> new com.singularity_iteration.mio_icif.Menu.Producer.ReplicatorElcMenu(containerId, playerInventory, replicator),
                    Component.translatable("container.mio_icif.replicator_elc")
                );
                player.openMenu(menuProvider);
            } else {
                player.sendSystemMessage(Component.literal("This block does not have a GUI!"));
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide());
    }

    /**
     * 方块被移除时的处理?
     * 掉落方块实体中的物品
     */
    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (state.getBlock() != newState.getBlock()) {
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity instanceof mio_icif_replicator_elc replicator) {
                // 掉落物品栏中的所有物�?
                for (int i = 0; i < replicator.getItemHandler().getSlots(); i++) {
                    ItemStack stack = replicator.getItemHandler().getStackInSlot(i);
                    if (!stack.isEmpty()) {
                        Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), stack);
                    }
                }
            }
            super.onRemove(state, level, pos, newState, movedByPiston);
        }
    }

    /**
     * 是否有模拟输出信号（用于红石比较器）
     */
    @Override
    protected boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    /**
     * 获取模拟输出信号强度（用于红石比较器�?
     * 基于UU物质储量计算
     */
    @Override
    protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity instanceof mio_icif_replicator_elc replicator) {
            // 基于UU物质储量计算信号强度 (0-15)
            int fluidAmount = replicator.getUuMatterAmount();
            int fluidCapacity = replicator.getUuMatterCapacity();
            return (fluidAmount * 15) / fluidCapacity;
        }
        return 0;
    }

    /**
     * 创建新的方块实体
     */
    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new mio_icif_replicator_elc(pos, state, mio_icif_block_entities.REPLICATOR_ELC_ENTITY_TYPE.get());
    }

    /**
     * 获取方块实体�?ticker
     */
    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> blockEntityType) {
        if (level.isClientSide()) {
            return null;
        }
        return (lvl, pos, blockState, blockEntity) -> {
            if (blockEntity instanceof mio_icif_replicator_elc replicator) {
                mio_icif_replicator_elc.tick(lvl, pos, blockState, replicator);

                // 更新方块状态?（只在状态变化时更新，避免闪烁）
                // LIT 和 REPLICATING 都只在真正工作时才为 true
                boolean isWorking = replicator.isReplicating();

                boolean currentReplicating = blockState.getValue(REPLICATING);
                boolean currentLit = blockState.getValue(LIT);

                if (currentReplicating != isWorking || currentLit != isWorking) {
                    BlockState newState = blockState
                        .setValue(REPLICATING, isWorking)
                        .setValue(LIT, isWorking);
                    // 使用 2 而不是 3，避免不必要的区块重新渲染
                    lvl.setBlock(pos, newState, 2);
                }
            }
        };
    }
}