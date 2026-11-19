package com.miophas.singularity_iteration.common.block.fusion;

import com.miophas.singularity_iteration.common.block.mio_icif_entity_block;
import com.miophas.singularity_iteration.common.blockentity.fusion.FusionControllerBlockEntity;
import com.miophas.singularity_iteration.common.registry.mio_icif_block_entities;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

/**
 * 0.1.11.92: the fusion reactor's controller block (it was a plain part in 0.1.11.89-91). It faces the player who places it; the
 * structure turns with it. {@code formed}: the structure is complete; {@code active}: there is plasma.
 */
public class mio_icif_block_fusion_controller extends mio_icif_entity_block {
    public static final BooleanProperty ACTIVE = BooleanProperty.create("active");
    public static final BooleanProperty FORMED = BooleanProperty.create("formed");
    public static final MapCodec<mio_icif_block_fusion_controller> CODEC = simpleCodec(mio_icif_block_fusion_controller::new);

    public mio_icif_block_fusion_controller(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, net.minecraft.core.Direction.NORTH).setValue(ACTIVE, false).setValue(FORMED, false));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(ACTIVE, FORMED);
        super.createBlockStateDefinition(builder);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    /** The controller turns with a rotated or mirrored structure (a structure block, a schematic). */
    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new FusionControllerBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide || type != mio_icif_block_entities.FUSION_CONTROLLER_ENTITY_TYPE.get()) return null;
        return (l, p, s, be) -> { if (be instanceof FusionControllerBlockEntity c) FusionControllerBlockEntity.tick(l, p, s, c); };
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof FusionControllerBlockEntity c) {
            c.validate();
            player.openMenu(c);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock()) && !level.isClientSide && level.getBlockEntity(pos) instanceof FusionControllerBlockEntity c) {
            for (int i = 0; i < c.getItems().getSlots(); i++) {
                ItemStack stack = c.getItems().getStackInSlot(i);
                if (!stack.isEmpty()) Block.popResource(level, pos, stack.copy());
            }
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }
}
