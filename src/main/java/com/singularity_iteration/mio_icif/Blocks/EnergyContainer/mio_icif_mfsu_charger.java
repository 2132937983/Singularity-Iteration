package com.singularity_iteration.mio_icif.Blocks.EnergyContainer;

import com.singularity_iteration.mio_icif.Blocks.entity.batbox.mio_icif_mfsu_charger_entity;
import com.singularity_iteration.mio_icif.Blocks.mio_icif_entity_block;
import com.mojang.serialization.MapCodec;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
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
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.phys.shapes.Shapes;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * MFSU 充电座方法? */
@SuppressWarnings("null")
public class mio_icif_mfsu_charger extends mio_icif_entity_block implements com.singularity_iteration.mio_icif.api.block.IChargepadBlock {

    public static final BooleanProperty LIT = BooleanProperty.create("lit");

    private static final VoxelShape OCCLUSION_SHAPE = Shapes.box(0.0, 0.0, 0.0, 1.0, 0.9375, 1.0);

    public static final MapCodec<mio_icif_mfsu_charger> CODEC = simpleCodec(mio_icif_mfsu_charger::new);

    public mio_icif_mfsu_charger(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(LIT, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(LIT);
    }

    @Override
    public boolean isCharging(BlockState state) {
        return state.getValue(LIT);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public VoxelShape getOcclusionShape(BlockState state, net.minecraft.world.level.BlockGetter level, BlockPos pos) {
        return OCCLUSION_SHAPE;
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new mio_icif_mfsu_charger_entity(pos, state);
    }

    @Override
    @Nullable
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null :
            (l, p, s, blockEntity) -> {
                if (blockEntity instanceof mio_icif_mfsu_charger_entity charger) {
                    mio_icif_mfsu_charger_entity.tick(l, p, s, charger);
                }
            };
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (!level.isClientSide()) {
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity instanceof MenuProvider) {
                player.openMenu((MenuProvider) blockEntity);
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide());
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
        tooltip.add(Component.translatable("tooltip.mio_icif.energy_container.charger")
                .withStyle(ChatFormatting.YELLOW));
    }

    // ==================== 红石信号支持 ====================

    /**
     * 检查方块是否可以发出红石信号
     */
    @Override
    protected boolean isSignalSource(BlockState state) {
        return true;
    }

    /**
     * 获取方块发出的红石信号强度
     * 根据方块实体的红石模式决定是否发出信号
     */
    @Override
    protected int getSignal(BlockState state, net.minecraft.world.level.BlockGetter level, BlockPos pos, Direction direction) {
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity instanceof com.singularity_iteration.mio_icif.Blocks.entity.mio_icif_Energy_Container container) {
            return container.getRedstoneSignalStrength();
        }
        return 0;
    }

    /**
     * 获取方块发出的直接红石信号强度（用于比较器等）
     */
    @Override
    protected int getDirectSignal(BlockState state, net.minecraft.world.level.BlockGetter level, BlockPos pos, Direction direction) {
        return getSignal(state, level, pos, direction);
    }
}