// SCEX 2026-09-12: repaired malformed UTF-8 bytes in comments only.
package com.miophas.singularity_iteration.common.block.transformer;

import com.miophas.singularity_iteration.common.block.mio_icif_entity_block;
import com.miophas.singularity_iteration.common.blockentity.transformer.mio_icif_transformer;
import com.miophas.singularity_iteration.core.api.energy.storage.EUApi;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
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
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        notifyTopology(level, pos, oldState, state);
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        super.onRemove(state, level, pos, newState, movedByPiston);
        notifyTopology(level, pos, state, newState);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        // 支持六面放置???
        // 非潜行时：方块正面朝向玩家（像原版发射器那样???
        // 潜行时：根据点击的面放置
        Direction direction;
        // if (context.isSecondaryUseActive()) {
        //     // 潜行时：根据点击的面放置
        //     direction = context.getClickedFace();
        // } else {
            // 非潜行时：方块正面朝向玩???
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

    /**
     * 变压器没有自己的 ticker：独立实现由能源引擎每世界帧驱动，
     * 遗留档位（IV/LUV 及以上）的实体同样由引擎事件处理。
     */
    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> blockEntityType) {
        return null;
    }

    /**
     * 通知电网拓扑本位置的物理方块发生变化。
     *
     * <p>原先由 mixin 在所有 {@code setBlockState} 之后统一上报，现在只在本方块的
     * 放置/移除/状态变化时上报——SCEX 网络只索引本模组的导线与变压器，因此覆盖等价。
     */
    protected void notifyTopology(Level level, BlockPos pos, BlockState before, BlockState after) {
        if (level instanceof net.minecraft.server.level.ServerLevel server
                && com.miophas.singularity_iteration.core.runtime.energy.engine.IndependentEnergyMode.enabled()
                && before != null && before.getBlock() != after.getBlock()) {
            com.miophas.singularity_iteration.core.platform.neoforge.energy.PlatformTopology
                .physicalBlockChanged(server, pos, before, after);
        }
    }

    /** 返回自身所属的变压器实体视图；无实体时返回 null。 */
    @Nullable
    protected com.miophas.singularity_iteration.common.menu.storage.TransformerView transformerView(BlockEntity entity) {
        if (entity instanceof com.miophas.singularity_iteration.core.platform.neoforge.energy.IndependentTransformerBlockEntity independent) {
            return new com.miophas.singularity_iteration.common.menu.storage.IndependentTransformerView(independent);
        }
        return entity instanceof com.miophas.singularity_iteration.common.menu.storage.TransformerView legacy
            ? legacy : null;
    }

    /**
     * 右键打开GUI
     */
    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (!level.isClientSide) {
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity != null) {
                var view = transformerView(blockEntity);
                if (view != null) {
                    player.openMenu(new net.minecraft.world.SimpleMenuProvider(
                        (id, inventory, ignored) -> new com.miophas.singularity_iteration.common.menu.storage.TransformerMenu(id, inventory, view),
                        Component.translatable("container.mio_icif.transformer")));
                } else if (blockEntity instanceof MenuProvider menuProvider) {
                    player.openMenu(menuProvider);
                }
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}

