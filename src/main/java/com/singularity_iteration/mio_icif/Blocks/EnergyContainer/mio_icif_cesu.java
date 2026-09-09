package com.singularity_iteration.mio_icif.Blocks.EnergyContainer;

import com.singularity_iteration.mio_icif.Blocks.entity.mio_icif_Energy_Container;
import com.singularity_iteration.mio_icif.Blocks.entity.batbox.mio_icif_cesu_entity;
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

/**
 * CESU (Charge Energy Storage Unit) 充电储能单元，基础容量的能量存储方块
 * 容量 300,000 EU，输入/输出速率 128 EU/tick
 */
@SuppressWarnings("null")
public class mio_icif_cesu extends mio_icif_entity_block {

    public static final MapCodec<mio_icif_cesu> CODEC = simpleCodec(mio_icif_cesu::new);
    public static final DirectionProperty FACING = DirectionalBlock.FACING;

    public mio_icif_cesu(Properties properties) {
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
        // 创建 CESU 的方块实体
        // 容量 300000 EU，输入/输出速率 128 EU/tick
        return new mio_icif_cesu_entity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> blockEntityType) {
        if (level.isClientSide()) {
            return null;
        }
        return (lvl, pos, blockState, blockEntity) -> {
            if (blockEntity instanceof mio_icif_cesu_entity cesuEntity) {
                mio_icif_Energy_Container.tick(lvl, pos, blockState, cesuEntity);
            }
        };
    }

    /**
     * 右键打开GUI
     */
    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (!level.isClientSide) {
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity instanceof MenuProvider) {
                player.openMenu((MenuProvider) blockEntity);
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected void addBlockTooltip(ItemStack stack, List<Component> tooltip) {
        long storedEnergy = mio_icif_entity_block.getStoredEnergyFromStack(stack);
        if (storedEnergy >= 0) {
            tooltip.add(Component.translatable("tooltip.mio_icif.energy_container.stored", storedEnergy, 300000)
                    .withStyle(ChatFormatting.GREEN));
        }
        tooltip.add(Component.translatable("tooltip.mio_icif.energy_container.capacity", 300000)
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.mio_icif.energy_container.io_rate", 128)
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