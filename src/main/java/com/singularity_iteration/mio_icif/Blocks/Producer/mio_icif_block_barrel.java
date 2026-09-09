package com.singularity_iteration.mio_icif.Blocks.Producer;

import com.singularity_iteration.mio_icif.Blocks.entity.mio_icif_block_entities;
import com.singularity_iteration.mio_icif.Blocks.entity.producer.mio_icif_barrel_entity;
import com.singularity_iteration.mio_icif.Blocks.mio_icif_entity_block;
import com.singularity_iteration.mio_icif.Items.Normal.mio_icif_normal;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

/**
 * 酒桶方方块?- 基于IC2原版机制
 *
 * 交互方式：通过GUI操作
 */
@SuppressWarnings("null")
public class mio_icif_block_barrel extends mio_icif_entity_block {

    public static final DirectionProperty FACING = DirectionalBlock.FACING;
    public static final BooleanProperty LIT = BooleanProperty.create("lit");

    public static final MapCodec<mio_icif_block_barrel> CODEC = RecordCodecBuilder.mapCodec(instance ->
        instance.group(propertiesCodec()).apply(instance, mio_icif_block_barrel::new));

    public mio_icif_block_barrel(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any()
            .setValue(FACING, Direction.NORTH)
            .setValue(LIT, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, LIT);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState()
            .setValue(FACING, context.getHorizontalDirection().getOpposite())
            .setValue(LIT, false);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    /**
     * 玩家右键点击方块时：
     * - 潜行：显示酿造进�?
     * - 正常：打开GUI
     */
    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (level.isClientSide()) return InteractionResult.SUCCESS;

        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity instanceof mio_icif_barrel_entity barrel) {
            // 潜行右键显示酿造进�?
            if (player.isShiftKeyDown()) {
                sendProgressMessage(player, barrel);
                return InteractionResult.SUCCESS;
            }
            player.openMenu(barrel);
            return InteractionResult.SUCCESS;
        }

        return InteractionResult.PASS;
    }

    /**
     * 发送酿造进度信息给玩家
     */
    private void sendProgressMessage(Player player, mio_icif_barrel_entity barrel) {
        int progress = barrel.getBrewProgressPercent();
        String status = barrel.getStatusMessage();

        Component message;
        if (barrel.isEmpty()) {
            message = Component.literal("§7[酒桶] §f空酒�?- 请放入原料开始酿�?");
        } else if (barrel.type == mio_icif_barrel_entity.TYPE_BEER) {
            int timeRatio = barrel.timeRatio;
            String stageName = switch (timeRatio) {
                case 0 -> "酿造液";
                case 1 -> "新手包";
                case 2 -> "啤酒";
                case 3 -> "麦酒";
                case 4 -> "龙血";
                case 5 -> "黑啤";
                default -> "未知";
            };
            message = Component.literal("§7[酒桶] §e啤酒酿造中 §7| §f阶段: §a" + stageName + " §7| §f进度: §a" + progress + "%");
        } else if (barrel.type == mio_icif_barrel_entity.TYPE_RUM) {
            message = Component.literal("§7[酒桶] §e朗姆酒酿造中 §7| §f进度: §a" + progress + "%");
        } else {
            message = Component.literal("§7[酒桶] §f" + status);
        }

        player.sendSystemMessage(message);
    }

    /**
     * 使用物品右键时：
     * - 潜行：显示酿造进�?
     * - 空酒杯：取出酒水
     * - 其他：打开GUI
     */
    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult) {
        if (level.isClientSide()) return ItemInteractionResult.SUCCESS;

        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (!(blockEntity instanceof mio_icif_barrel_entity barrel)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }

        // 潜行右键显示酿造进�?
        if (player.isShiftKeyDown()) {
            sendProgressMessage(player, barrel);
            return ItemInteractionResult.SUCCESS;
        }

        // 拿着空酒杯时，取出酒�?
        if (stack.is(mio_icif_normal.EMPTY_MUG.get())) {
            ItemStack output = barrel.takeOutput();
            if (!output.isEmpty()) {
                stack.shrink(1);
                if (!player.getInventory().add(output)) {
                    player.drop(output, false);
                }
                return ItemInteractionResult.SUCCESS;
            }
            // 酒桶为空时，仍然打开GUI
            player.openMenu(barrel);
            return ItemInteractionResult.SUCCESS;
        }

        // 其他物品打开GUI
        player.openMenu(barrel);
        return ItemInteractionResult.SUCCESS;
    }

    /**
     * 方块被移除时的处理?
     * 掉落方块实体中的物品
     */
    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (state.getBlock() != newState.getBlock()) {
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity instanceof mio_icif_barrel_entity barrel) {
                barrel.dropContents(level, pos);
                level.updateNeighbourForOutputSignal(pos, this);
            }
            super.onRemove(state, level, pos, newState, isMoving);
        }
    }

    /**
     * 创建方块实体
     */
    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new mio_icif_barrel_entity(pos, state);
    }

    /**
     * 获取方块实体Ticker
     */
    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return createTickerHelper(type, mio_icif_block_entities.BARREL.get(), mio_icif_barrel_entity::tick);
    }
}

