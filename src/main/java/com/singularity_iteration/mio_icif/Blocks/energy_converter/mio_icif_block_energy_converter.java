package com.singularity_iteration.mio_icif.Blocks.energy_converter;

import com.singularity_iteration.mio_icif.Blocks.entity.energy_converter.mio_icif_energy_converter_entity;
import com.singularity_iteration.mio_icif.Blocks.mio_icif_entity_block;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
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
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * 能源转换器方块
 * 两种模式：
 * 1. EU转FE（默认）：正面输出EU，其余5面输入FE，转换比例 1EU = 4FE
 * 2. FE转EU：正面输出FE，其余5面输入EU，转换比例 4FE = 1EU
 * 
 * 蹲下右键切换模式
 */
@SuppressWarnings("null")
public class mio_icif_block_energy_converter extends mio_icif_entity_block {
    
    // 方块状态属性
    public static final BooleanProperty MODE = BooleanProperty.create("mode"); // false = EU->FE, true = FE->EU
    // 使用支持所有六个方向的FACING属性（覆盖基类的水平方向FACING）
    public static final DirectionProperty FACING = BlockStateProperties.FACING;
    
    public static final MapCodec<mio_icif_block_energy_converter> CODEC = RecordCodecBuilder.mapCodec(instance ->
        instance.group(propertiesCodec()).apply(instance, mio_icif_block_energy_converter::new));

    public mio_icif_block_energy_converter(Properties properties) {
        super(properties);
        // 默认状态：EU转FE模式
        this.registerDefaultState(this.stateDefinition.any()
            .setValue(FACING, Direction.NORTH)
            .setValue(MODE, false));
    }
    
    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        // 不调用super，自己添加FACING和MODE
        builder.add(FACING, MODE);
    }
    
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        // 正面朝向玩家（与发射器逻辑一致，支持所有六个方向）
        return this.defaultBlockState().setValue(FACING, context.getNearestLookingDirection().getOpposite());
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
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        
        // 蹲下右键切换模式
        if (player.isShiftKeyDown()) {
            boolean currentMode = state.getValue(MODE);
            boolean newMode = !currentMode;
            level.setBlock(pos, state.setValue(MODE, newMode), 3);
            
            // 通知玩家模式切换
            if (player instanceof ServerPlayer serverPlayer) {
                if (newMode) {
                    serverPlayer.displayClientMessage(
                        Component.translatable("message.mio_icif.energy_converter.mode_fe_to_eu"), true);
                } else {
                    serverPlayer.displayClientMessage(
                        Component.translatable("message.mio_icif.energy_converter.mode_eu_to_fe"), true);
                }
            }
            
            // 更新方块实体
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof mio_icif_energy_converter_entity entity) {
                entity.setMode(newMode);
            }
            
            return InteractionResult.CONSUME;
        }
        
        return InteractionResult.PASS;
    }
    
    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean isMoving) {
        super.onPlace(state, level, pos, oldState, isMoving);
        if (!level.isClientSide) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof mio_icif_energy_converter_entity entity) {
                entity.setMode(state.getValue(MODE));
            }
        }
    }
    
    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new mio_icif_energy_converter_entity(pos, state);
    }
    
    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, 
                                                                   BlockEntityType<T> type) {
        if (level.isClientSide) {
            return null;
        }
        return (lvl, p, st, blockEntity) -> {
            if (blockEntity instanceof mio_icif_energy_converter_entity entity) {
                entity.tick();
            }
        };
    }
    
    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, 
                                 TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        tooltip.add(Component.translatable("tooltip.mio_icif.energy_converter.desc"));
        tooltip.add(Component.translatable("tooltip.mio_icif.energy_converter.sneak_to_switch"));
    }
}