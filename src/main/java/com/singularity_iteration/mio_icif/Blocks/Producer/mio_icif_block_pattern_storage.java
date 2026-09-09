package com.singularity_iteration.mio_icif.Blocks.Producer;

import com.singularity_iteration.mio_icif.Blocks.entity.mio_icif_block_entities;
import com.singularity_iteration.mio_icif.Blocks.entity.producer.mio_icif_pattern_storage;
import com.singularity_iteration.mio_icif.Blocks.mio_icif_entity_block;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Player;
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
 * 模式存储机方块类
 * 用于存储模式扫描机的扫描结果
 */
@SuppressWarnings("null")
public class mio_icif_block_pattern_storage extends mio_icif_entity_block {
    private static final Logger LOGGER = LogUtils.getLogger();

    public static final BooleanProperty LIT = BooleanProperty.create("lit");

    public static final MapCodec<mio_icif_block_pattern_storage> CODEC = RecordCodecBuilder.mapCodec(instance ->
        instance.group(propertiesCodec()).apply(instance, mio_icif_block_pattern_storage::new));

    public mio_icif_block_pattern_storage(Properties properties) {
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
        BlockEntity blockEntity = level.getBlockEntity(pos);

        // 调试输出：显示存储信息
    if (blockEntity instanceof mio_icif_pattern_storage storage) {
            String side = level.isClientSide() ? "客户端? ": "服务�?";
            LOGGER.info("[模式存储机调�?" + side + "] 位置: " + pos);
            LOGGER.info("[模式存储机调�?" + side + "] 存储数量: " + storage.getStoredCount() + "/" + mio_icif_pattern_storage.MAX_PATTERNS);
            LOGGER.info("[模式存储机调�?" + side + "] 当前索引: " + storage.getCurrentIndex());

            // 显示存储的物品信息
        var patterns = storage.getStoredPatterns();
            if (!patterns.isEmpty()) {
                LOGGER.info("[模式存储机调�?" + side + "] 存储的物�?");
                for (int i = 0; i < patterns.size(); i++) {
                    var result = patterns.get(i);
                    LOGGER.info("[模式存储机调�?" + side + "]   [" + i + "] " + result.item.getHoverName().getString() +
                        " - UU: " + result.uuMatterCostBuckets + "B, EU: " + result.energyCost);
                }
            } else {
                LOGGER.info("[模式存储机调�?" + side + "] 存储列表为空");
            }

            // 向玩家发送聊天信息
        player.sendSystemMessage(Component.translatable("message.mio_icif.pattern_storage.info_header", side));
            player.sendSystemMessage(Component.translatable("message.mio_icif.pattern_storage.stored_count", storage.getStoredCount(), mio_icif_pattern_storage.MAX_PATTERNS));
            player.sendSystemMessage(Component.translatable("message.mio_icif.pattern_storage.current_index", storage.getCurrentIndex()));

            if (!patterns.isEmpty()) {
                player.sendSystemMessage(Component.translatable("message.mio_icif.pattern_storage.stored_items"));
                for (int i = 0; i < Math.min(patterns.size(), 5); i++) {
                    var result = patterns.get(i);
                    player.sendSystemMessage(Component.translatable("message.mio_icif.pattern_storage.item_entry", i, result.item.getHoverName().getString()));
                }
                if (patterns.size() > 5) {
                    player.sendSystemMessage(Component.translatable("message.mio_icif.pattern_storage.more_items", patterns.size() - 5));
                }
            } else {
                player.sendSystemMessage(Component.translatable("message.mio_icif.pattern_storage.empty"));
            }
        }

        if (!level.isClientSide()) {
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
            // 模式存储机不需要掉落物品，因为它没有物品栏
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
        if (blockEntity instanceof mio_icif_pattern_storage storage) {
            // 根据存储数量返回信号强度
            int count = storage.getStoredCount();
            int max = mio_icif_pattern_storage.MAX_PATTERNS;
            return (count * 15) / max;
        }
        return 0;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new mio_icif_pattern_storage(pos, state, mio_icif_block_entities.PATTERN_STORAGE_ENTITY_TYPE.get());
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> blockEntityType) {
        if (level.isClientSide()) {
            return null;
        }
        return (lvl, pos, blockState, blockEntity) -> {
            if (blockEntity instanceof mio_icif_pattern_storage storage) {
                // 更新方块状态
            boolean hasData = storage.getStoredCount() > 0;
                if (blockState.getValue(LIT) != hasData) {
                    lvl.setBlock(pos, blockState.setValue(LIT, hasData), 3);
                }
            }
        };
    }
}