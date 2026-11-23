// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.builder;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

/** Infinite Armory Material Interface block (0.1.11.187). See {@link InfiniteMaterialInterfaceBlockEntity}; no screen. */
@SuppressWarnings("null")
public class InfiniteMaterialInterfaceBlock extends BaseEntityBlock {
    public static final MapCodec<InfiniteMaterialInterfaceBlock> CODEC = simpleCodec(InfiniteMaterialInterfaceBlock::new);

    public InfiniteMaterialInterfaceBlock(Properties properties) { super(properties); }

    @Override protected MapCodec<? extends BaseEntityBlock> codec() { return CODEC; }
    @Override public RenderShape getRenderShape(BlockState state) { return RenderShape.MODEL; }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new InfiniteMaterialInterfaceBlockEntity(pos, state); }

    /** No screen: a line over the hotbar says what it does. */
    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide) player.displayClientMessage(Component.translatable("message.mio_icif.builder.infinite_interface",
            BlueprintBuilderBlockEntity.INTERFACE_RANGE), true);
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
