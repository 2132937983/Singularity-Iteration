// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.block.generator;

import com.miophas.singularity_iteration.common.block.mio_icif_entity_block;
import com.miophas.singularity_iteration.common.blockentity.generator.mio_icif_creative_generator;
import com.miophas.singularity_iteration.common.registry.mio_icif_block_entities;
import com.mojang.serialization.MapCodec;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
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
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

/**
 * 0.1.10.11: Creative Generator block (debug). Creative mode only:
 * <ul>
 *   <li>only a player in creative mode can place it (the item is only in the creative tab and has no recipe);</li>
 *   <li>it cannot be mined in survival, resists explosions and drops nothing;</li>
 *   <li>only a creative player or an operator (permission level 2) can open its screen.</li>
 * </ul>
 */
@SuppressWarnings("null")
public class mio_icif_block_creative_generator extends mio_icif_entity_block {
    public static final BooleanProperty ACTIVE = BooleanProperty.create("active");

    public mio_icif_block_creative_generator(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any()
            .setValue(FACING, net.minecraft.core.Direction.NORTH)
            .setValue(ACTIVE, true));
    }

    /** True when {@code player} may place, configure or use the generator. */
    public static boolean allowed(@Nullable Player player) {
        return player != null && (player.isCreative() || player.hasPermissions(2));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(ACTIVE);
        super.createBlockStateDefinition(builder);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return simpleCodec(mio_icif_block_creative_generator::new);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Player player = context.getPlayer();
        if (player == null || !player.isCreative()) return null;
        return this.defaultBlockState()
            .setValue(FACING, context.getHorizontalDirection().getOpposite())
            .setValue(ACTIVE, true);
    }

    @Override
    public List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        return List.of();
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide || type != mio_icif_block_entities.CREATIVE_GENERATOR_ENTITY_TYPE.get()) return null;
        return (l, p, s, be) -> {
            if (be instanceof mio_icif_creative_generator generator) mio_icif_creative_generator.tick(l, p, s, generator);
        };
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return mio_icif_block_entities.CREATIVE_GENERATOR_ENTITY_TYPE.get().create(pos, state);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (!level.isClientSide) {
            if (!allowed(player)) {
                player.displayClientMessage(Component.translatable("message.mio_icif.creative_generator.denied")
                    .withStyle(ChatFormatting.RED), true);
            } else if (level.getBlockEntity(pos) instanceof mio_icif_creative_generator generator) {
                player.openMenu(generator, buf -> buf.writeBlockPos(pos));
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public int getLightEmission(BlockState state, BlockGetter level, BlockPos pos) {
        return state.hasProperty(ACTIVE) && state.getValue(ACTIVE) ? 15 : 4;
    }

    @Override
    public void appendHoverText(ItemStack stack, net.minecraft.world.item.Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        // no voltage-tier line: the output has no tier
        tooltip.add(Component.translatable("tooltip.mio_icif.creative_generator.line1").withStyle(ChatFormatting.LIGHT_PURPLE));
        tooltip.add(Component.translatable("tooltip.mio_icif.creative_generator.line2").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.mio_icif.creative_generator.line3").withStyle(ChatFormatting.DARK_GRAY));
    }
}
