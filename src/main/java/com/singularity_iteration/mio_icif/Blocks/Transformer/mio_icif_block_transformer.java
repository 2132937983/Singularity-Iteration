package com.singularity_iteration.mio_icif.Blocks.Transformer;

import com.singularity_iteration.mio_icif.Blocks.mio_icif_entity_block;
import com.singularity_iteration.mio_icif.Blocks.entity.transformer.mio_icif_transformer;
import com.singularity_iteration.mio_icif.energy.EnergyUnit.EUApi;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
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
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import org.jetbrains.annotations.Nullable;

@SuppressWarnings("null")
public abstract class mio_icif_block_transformer extends mio_icif_entity_block {

    public static final DirectionProperty FACING = DirectionalBlock.FACING;
    public static final BooleanProperty ACTIVE = BooleanProperty.create("active");

    public mio_icif_block_transformer(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any()
            .setValue(FACING, Direction.NORTH)
            .setValue(ACTIVE, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, ACTIVE);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        // 支持六面放置�?
        // 非潜行时：方块正面朝向玩家（像原版发射器那样�?
        // 潜行时：根据点击的面放置
        Direction direction;
        // if (context.isSecondaryUseActive()) {
        //     // 潜行时：根据点击的面放置
        //     direction = context.getClickedFace();
        // } else {
            // 非潜行时：方块正面朝向玩�?
            direction = context.getNearestLookingDirection().getOpposite();
        // }
        return this.defaultBlockState().setValue(FACING, direction);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public abstract BlockEntity newBlockEntity(BlockPos pos, BlockState state);

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> blockEntityType) {
        if (level.isClientSide()) {
            return null;
        }
        return (lvl, pos, blockState, blockEntity) -> {
            if (blockEntity instanceof mio_icif_transformer transformer) {
                mio_icif_transformer.tick(lvl, pos, blockState, transformer);
            }
        };
    }

    /**
     * 注册能量能力
     */
    public static void registerCapabilities(RegisterCapabilitiesEvent event, BlockEntityType<? extends mio_icif_transformer> blockEntityType) {
        event.registerBlockEntity(EUApi.SIDED, blockEntityType, (be, direction) -> {
            return be.getEnergyStorage(direction);
        });
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
}

