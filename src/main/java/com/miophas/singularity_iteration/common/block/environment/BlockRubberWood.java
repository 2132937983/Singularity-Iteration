package com.miophas.singularity_iteration.common.block.environment;

import com.miophas.singularity_iteration.common.registry.mio_icif_blocks;
import com.miophas.singularity_iteration.common.item.resource.mio_icif_resources;
import com.miophas.singularity_iteration.core.api.block.IRubberWood;
import com.miophas.singularity_iteration.core.runtime.world.RubberTreeSystem;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.common.ItemAbilities;
import org.jetbrains.annotations.Nullable;

@SuppressWarnings("null")
public class BlockRubberWood extends HorizontalDirectionalBlock implements IRubberWood {

    public static final MapCodec<BlockRubberWood> CODEC = simpleCodec(BlockRubberWood::new);

    public static final BooleanProperty HAS_HARZ = BooleanProperty.create("has_harz");
    public static final BooleanProperty HAS_SPOT = BooleanProperty.create("has_spot");

    public BlockRubberWood(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any()
            .setValue(HAS_HARZ, false)
            .setValue(HAS_SPOT, false)
            .setValue(FACING, Direction.NORTH));
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(HAS_HARZ, HAS_SPOT, FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState()
            .setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rot) {
        return state.setValue(FACING, rot.rotate(state.getValue(FACING)));
    }

    // ==================== IRubberWood ====================

    @Override
    public boolean hasResin(BlockState state) {
        return state.getValue(HAS_HARZ);
    }

    @Override
    public boolean isTappable(BlockState state) {
        return state.getValue(HAS_SPOT);
    }

    @Override
    public BlockState withResin(BlockState state, boolean hasResin) {
        return state.setValue(HAS_HARZ, hasResin);
    }

    // ==================== 采集 ====================

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult) {
        if (!RubberTreeSystem.canUseTapOn(state, stack)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }

        RubberTreeSystem.harvest(level, pos, state, player, hand, stack, mio_icif_resources.HARZ.get());
        return ItemInteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        RubberTreeSystem.tryRegrowResin(level, pos, state, random);
    }

    @Override
    public @Nullable BlockState getToolModifiedState(BlockState state, UseOnContext context, net.neoforged.neoforge.common.ItemAbility itemAbility, boolean simulate) {
        // 检查是否是斧头剥皮动作
        if (itemAbility == ItemAbilities.AXE_STRIP) {
            // 返回被剥皮的橡胶木，保持朝向
            return mio_icif_blocks.BLOCK_STRIPPED_RUBBER_WOOD.get().defaultBlockState()
                .setValue(FACING, state.getValue(FACING));
        }
        return super.getToolModifiedState(state, context, itemAbility, simulate);
    }
}
