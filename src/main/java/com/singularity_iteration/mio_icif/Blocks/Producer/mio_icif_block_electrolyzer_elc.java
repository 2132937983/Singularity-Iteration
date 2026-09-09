package com.singularity_iteration.mio_icif.Blocks.Producer;

import com.singularity_iteration.mio_icif.Blocks.entity.mio_icif_block_entities;
import com.singularity_iteration.mio_icif.Blocks.entity.producer.mio_icif_electrolyzer_elc;
import com.singularity_iteration.mio_icif.Blocks.mio_icif_entity_block;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Containers;
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
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

/**
 * 电解机方块类
 * 用于将电能转化为化学能储存，或将化学能转化为电能释放
 * 需要与储电装置（储电盒、CESU、MFE、MFSU）挨在一起放弃? */
@SuppressWarnings("null")
public class mio_icif_block_electrolyzer_elc extends mio_icif_entity_block {

    // 工作状态属性
public static final BooleanProperty CHARGING = BooleanProperty.create("charging");
    public static final BooleanProperty DISCHARGING = BooleanProperty.create("discharging");

    public static final MapCodec<mio_icif_block_electrolyzer_elc> CODEC = RecordCodecBuilder.mapCodec(instance ->
        instance.group(propertiesCodec()).apply(instance, mio_icif_block_electrolyzer_elc::new));

    public mio_icif_block_electrolyzer_elc(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any()
            .setValue(CHARGING, false)
            .setValue(DISCHARGING, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(CHARGING, DISCHARGING);
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
        if (!level.isClientSide()) {
            BlockEntity blockEntity = level.getBlockEntity(pos);
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
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity instanceof mio_icif_electrolyzer_elc electrolyzer) {
                // 掉落物品栏中的物�
            for (int i = 0; i < electrolyzer.getContainerSize(); i++) {
                    ItemStack stack = electrolyzer.getItem(i);
                    if (!stack.isEmpty()) {
                        Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), stack);
                    }
                }
            }
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
        if (blockEntity instanceof mio_icif_electrolyzer_elc electrolyzer) {
            // 计算红石信号强度（基于化学能储存程度�
        long energy = electrolyzer.getChemicalEnergy();
            long maxEnergy = electrolyzer.getMaxChemicalEnergy();
            return (int) ((energy * 15) / maxEnergy);
        }
        return 0;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new mio_icif_electrolyzer_elc(pos, state, mio_icif_block_entities.ELECTROLYZER.get());
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> blockEntityType) {
        if (level.isClientSide()) {
            return null;
        }
        return (lvl, pos, blockState, blockEntity) -> {
            if (blockEntity instanceof mio_icif_electrolyzer_elc electrolyzer) {
                mio_icif_electrolyzer_elc.tick(lvl, pos, blockState, electrolyzer);

                // 更新方块状态
            boolean isCharging = electrolyzer.isCharging();
                boolean isDischarging = electrolyzer.isDischarging();
                if (blockState.getValue(CHARGING) != isCharging ||
                    blockState.getValue(DISCHARGING) != isDischarging) {
                    lvl.setBlockAndUpdate(pos, blockState
                        .setValue(CHARGING, isCharging)
                        .setValue(DISCHARGING, isDischarging));
                }
            }
        };
    }
}


