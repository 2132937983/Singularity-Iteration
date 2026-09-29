package com.miophas.singularity_iteration.common.block.producer;

import com.miophas.singularity_iteration.common.registry.mio_icif_block_entities;
import com.miophas.singularity_iteration.common.blockentity.producer.mio_icif_pump_elc;
import com.miophas.singularity_iteration.common.block.mio_icif_entity_block;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

/**
 * 电力泵方块类
 * 能够将水或岩浆等流体抽到机器�?
 */
@SuppressWarnings("null")
public class mio_icif_block_pump_elc extends mio_icif_entity_block {

    // 运行状态属性，用于控制方块的光照和纹理变化
    public static final BooleanProperty LIT = BooleanProperty.create("lit");

    // 方块编码器，用于数据生成和序列化
    public static final MapCodec<mio_icif_block_pump_elc> CODEC = RecordCodecBuilder.mapCodec(instance ->
        instance.group(propertiesCodec()).apply(instance, mio_icif_block_pump_elc::new));

    public mio_icif_block_pump_elc(Properties properties) {
        super(properties);
        // 注册默认状态：未点�?
        this.registerDefaultState(this.stateDefinition.any().setValue(LIT, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        // 对齐 IC2：泵支持六面朝向，使用 3D 的 FACING 而非基类的水平 HORIZONTAL_FACING。
        // 注意：这里不调用基类实现，否则会重复注册同名 "facing" 属性（基类为水平 4 向）。
        builder.add(BlockStateProperties.FACING, LIT);
    }

    /**
     * 对齐 IC2：泵可朝向任意六个面，放置时正面朝向玩家。
     */
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction facing = context.getNearestLookingDirection().getOpposite();
        return this.defaultBlockState().setValue(BlockStateProperties.FACING, facing);
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

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof mio_icif_pump_elc pump) {
            // Always replace any packed block-entity owner with the current placer.
            pump.setActionOwnerFromPlacer(placer);
        }
    }

    /**
     * 玩家右键点击方块时的处理
     * 打开 GUI 界面
     */
    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (!level.isClientSide()) {
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity instanceof mio_icif_pump_elc pump) {
                MenuProvider menuProvider = new SimpleMenuProvider(
                    (containerId, playerInventory, playerEntity) -> new com.miophas.singularity_iteration.common.menu.producer.PumpElcMenu(containerId, playerInventory, pump),
                    Component.translatable("container.mio_icif.pump_elc")
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
        if (level.isClientSide()) {
            super.onRemove(state, level, pos, newState, movedByPiston);
            return;
        }
        if (state.getBlock() != newState.getBlock()) {
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity instanceof mio_icif_pump_elc pump) {
                // 掉落物品栏中的所有物�?
                for (int i = 0; i < pump.getItemHandler().getSlots(); i++) {
                    ItemStack stack = pump.getItemHandler().getStackInSlot(i);
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
     * 基于物品栏填充程度和流体储量计算
     */
    @Override
    protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity instanceof mio_icif_pump_elc pump) {
            // 计算物品栏填充程�?
            var handler = pump.getItemHandler();
            int totalSlots = handler.getSlots();
            int filledSlots = 0;
            for (int i = 0; i < totalSlots; i++) {
                if (!handler.getStackInSlot(i).isEmpty()) {
                    filledSlots++;
                }
            }
            // 物品栏占50%权重，流体储量占50%权重
            int itemSignal = (filledSlots * 7) / totalSlots; // 0-7
            int fluidSignal = (pump.getFluidAmount() * 8) / pump.getFluidCapacity(); // 0-8
            return Math.min(15, itemSignal + fluidSignal);
        }
        return 0;
    }

    /**
     * 创建新的方块实体
     */
    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new mio_icif_pump_elc(pos, state, mio_icif_block_entities.PUMP_ELC_ENTITY_TYPE.get());
    }

    /**
     * 获取方块实体更新器（Ticker�?
     * 用于�?tick 更新方块实体逻辑
     */
    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> blockEntityType) {
        if (level.isClientSide()) {
            return null;
        }
        return (lvl, pos, blockState, blockEntity) -> {
            if (blockEntity instanceof mio_icif_pump_elc pump) {
                mio_icif_pump_elc.tick(lvl, pos, blockState, pump);
            }
        };
    }
}

