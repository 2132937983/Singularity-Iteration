package com.singularity_iteration.mio_icif.Blocks.EnergyContainer;

import com.singularity_iteration.mio_icif.Blocks.entity.batbox.mio_icif_mfsu_entity;
import com.singularity_iteration.mio_icif.Blocks.mio_icif_entity_block;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
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
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import com.mojang.serialization.MapCodec;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import org.slf4j.Logger;
import com.mojang.logging.LogUtils;

/**
 * MFSU (Maximum Energy Storage Unit) 最大储能箱
 * 拥有巨大的能量存储容量
 * 容量 40,000,000 EU，输入/输出速率 2048 EU/tick
 * 对齐原版 IC2：tier=4, output=2048, maxStorage=40000000
 */
@SuppressWarnings("null")
public class mio_icif_mfsu extends mio_icif_entity_block {
    private static final Logger LOGGER = LogUtils.getLogger();
    
    public static final MapCodec<mio_icif_mfsu> CODEC = simpleCodec(mio_icif_mfsu::new);
    public static final DirectionProperty FACING = DirectionalBlock.FACING;
    
    public mio_icif_mfsu(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
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
        return this.defaultBlockState().setValue(FACING, direction);
    }
    
    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        // 创建 MFSU 的方块实体
        // 容量 40000000 EU，输入/输出速率 2048 EU/tick
        return new mio_icif_mfsu_entity(pos, state);
    }

    @Override
    @Nullable
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        // 注册 tick 逻辑
        return level.isClientSide ? null :
            (l, p, s, blockEntity) -> {
                if (blockEntity instanceof mio_icif_mfsu_entity container) {
                    mio_icif_mfsu_entity.tick(l, p, s, container);
                }
            };
    }
    
    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        LOGGER.info("MFSU right-clicked (useWithoutItem)!");
        if (!level.isClientSide) {
            BlockEntity blockEntity = level.getBlockEntity(pos);
            LOGGER.info("BlockEntity: " + (blockEntity != null ? blockEntity.getClass().getName() : "null"));
            LOGGER.info("Is MenuProvider: " + (blockEntity instanceof MenuProvider));
            if (blockEntity instanceof MenuProvider) {
                LOGGER.info("Opening GUI...");
                player.openMenu((MenuProvider) blockEntity);
                LOGGER.info("GUI opened");
            } else {
                player.sendSystemMessage(net.minecraft.network.chat.Component.literal("This block does not have a GUI!"));
                LOGGER.info("BlockEntity is NOT a MenuProvider");
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected void addBlockTooltip(ItemStack stack, List<Component> tooltip) {
        long storedEnergy = mio_icif_entity_block.getStoredEnergyFromStack(stack);
        if (storedEnergy >= 0) {
            tooltip.add(Component.translatable("tooltip.mio_icif.energy_container.stored", storedEnergy, 40000000)
                    .withStyle(ChatFormatting.GREEN));
        }
        tooltip.add(Component.translatable("tooltip.mio_icif.energy_container.capacity", 40000000)
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.mio_icif.energy_container.io_rate", 2048)
                .withStyle(ChatFormatting.GRAY));
    }
    
    // ==================== 红石信号支持 ====================
    
    @Override
    protected boolean isSignalSource(BlockState state) {
        return true;
    }
    
    @Override
    protected int getSignal(BlockState state, net.minecraft.world.level.BlockGetter level, BlockPos pos, Direction direction) {
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity instanceof com.singularity_iteration.mio_icif.Blocks.entity.mio_icif_Energy_Container container) {
            return container.getRedstoneSignalStrength();
        }
        return 0;
    }
    
    @Override
    protected int getDirectSignal(BlockState state, net.minecraft.world.level.BlockGetter level, BlockPos pos, Direction direction) {
        return getSignal(state, level, pos, direction);
    }
}