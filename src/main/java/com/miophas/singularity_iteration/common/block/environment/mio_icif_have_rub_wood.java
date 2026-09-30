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
import net.neoforged.neoforge.common.ItemAbility;
import org.jetbrains.annotations.Nullable;

@SuppressWarnings("null")
public class mio_icif_have_rub_wood extends HorizontalDirectionalBlock implements IRubberWood {

    public static final MapCodec<mio_icif_have_rub_wood> CODEC = simpleCodec(mio_icif_have_rub_wood::new);

    // 定义有胶属性?
    public static final BooleanProperty HAS_HARZ = BooleanProperty.create("has_harz");

    public mio_icif_have_rub_wood(Properties properties) {
        super(properties);
        // 默认状态为有胶，朝向北
        this.registerDefaultState(this.stateDefinition.any()
            .setValue(HAS_HARZ, true)
            .setValue(FACING, Direction.NORTH));
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(HAS_HARZ, FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        // 根据玩家朝向设置方块方向
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
        // 该方块没有采集口状态，任何时候都具备采集条件
        return true;
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

    /**
     * 处理工具交互（斧头剥皮）
     */
    @Override
    public @Nullable BlockState getToolModifiedState(BlockState state, UseOnContext context, ItemAbility itemAbility, boolean simulate) {
        // 检查是否是斧头剥皮动作
        if (itemAbility == ItemAbilities.AXE_STRIP) {
            // 获取剥皮后的方块状态，保持朝向
            BlockState strippedState = mio_icif_blocks.BLOCK_STRIPPED_RUBBER_WOOD.get().defaultBlockState();
            return strippedState.setValue(FACING, state.getValue(FACING));
        }
        return super.getToolModifiedState(state, context, itemAbility, simulate);
    }
}
