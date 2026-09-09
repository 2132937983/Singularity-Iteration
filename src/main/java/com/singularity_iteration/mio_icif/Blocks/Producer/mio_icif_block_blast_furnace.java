package com.singularity_iteration.mio_icif.Blocks.Producer;

import com.singularity_iteration.mio_icif.Blocks.entity.mio_icif_block_entities;
import com.singularity_iteration.mio_icif.Blocks.entity.producer.mio_icif_blast_furnace;
import com.singularity_iteration.mio_icif.Blocks.mio_icif_entity_block;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleMenuProvider;
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
 * 高炉方块
 * 使用 HU 热量炼制钢锭的机器方块
 */
@SuppressWarnings("null")
public class mio_icif_block_blast_furnace extends mio_icif_entity_block {

    public static final DirectionProperty FACING = DirectionalBlock.FACING;
    public static final BooleanProperty LIT = BooleanProperty.create("lit");

    public static final MapCodec<mio_icif_block_blast_furnace> CODEC = RecordCodecBuilder.mapCodec(instance ->
        instance.group(propertiesCodec()).apply(instance, mio_icif_block_blast_furnace::new));

    public mio_icif_block_blast_furnace(Properties properties) {
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
        // 支持六面放置
    // 非潜行时：方块正面朝向玩家（像原版发射器那样）
    // 潜行时：根据点击的面放置
        Direction direction;
        // if (context.isSecondaryUseActive()) {
        //     // 潜行时：根据点击的面放置
        //     direction = context.getClickedFace();
        // } else {
            // 非潜行时：方块正面朝向玩家
        direction = context.getNearestLookingDirection().getOpposite();
        // }
        return this.defaultBlockState()
            .setValue(FACING, direction)
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
     * 玩家右键点击方块时的处理
     * 鎵撳紑楂樼倝 GUI 鐣岄潰
     */
    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (!level.isClientSide()) {
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity instanceof mio_icif_blast_furnace furnace) {
                MenuProvider menuProvider = new SimpleMenuProvider(
                    (containerId, playerInventory, playerEntity) -> new com.singularity_iteration.mio_icif.Menu.Producer.BlastFurnaceMenu(containerId, playerInventory, furnace),
                    Component.translatable("container.mio_icif.blast_furnace")
                );
                player.openMenu(menuProvider);
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide());
    }

    /**
     * 方块被移除时的处理
 * 掉落方块实体中的物品
     */
    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (state.getBlock() != newState.getBlock()) {
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity instanceof mio_icif_blast_furnace furnace) {
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

    /**
     * 创建新的方块实体
     */
    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new mio_icif_blast_furnace(pos, state, mio_icif_block_entities.BLAST_FURNACE_ENTITY_TYPE.get());
    }

    /**
     * 获取方块实体更新器（Ticker）
 */
    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> blockEntityType) {
        if (level.isClientSide()) {
            return null;
        }
        return (lvl, pos, blockState, blockEntity) -> {
            if (blockEntity instanceof mio_icif_blast_furnace furnace) {
                mio_icif_blast_furnace.tick(lvl, pos, blockState, furnace);
            }
        };
    }
}