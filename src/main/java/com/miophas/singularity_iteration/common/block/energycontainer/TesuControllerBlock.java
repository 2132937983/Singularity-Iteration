package com.miophas.singularity_iteration.common.block.energycontainer;

import com.miophas.singularity_iteration.common.block.mio_icif_entity_block;
import com.miophas.singularity_iteration.common.blockentity.batbox.TesuControllerBlockEntity;
import com.mojang.serialization.MapCodec;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

/**
 * 0.1.11.157: the TESU (太级储电罐) controller: stores the energy, checks the structure, opens the screen. It faces out of the
 * structure's front; the structure turns with it.
 */
@SuppressWarnings("null")
public class TesuControllerBlock extends mio_icif_entity_block {
    public static final MapCodec<TesuControllerBlock> CODEC = simpleCodec(TesuControllerBlock::new);

    public TesuControllerBlock(Properties properties) { super(properties); }

    @Override protected MapCodec<? extends BaseEntityBlock> codec() { return CODEC; }
    @Override public RenderShape getRenderShape(BlockState state) { return RenderShape.MODEL; }
    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new TesuControllerBlockEntity(pos, state); }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide()) return null;
        return (l, p, s, be) -> { if (be instanceof TesuControllerBlockEntity c) TesuControllerBlockEntity.tick(l, p, s, c); };
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof MenuProvider menu) player.openMenu(menu);
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moved) {
        if (!level.isClientSide() && !state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof TesuControllerBlockEntity c) c.released();
        super.onRemove(state, level, pos, newState, moved);
    }

    @Override
    protected void addBlockTooltip(ItemStack stack, List<Component> tooltip) {
        long stored = getStoredEnergyFromStack(stack);
        if (stored >= 0) tooltip.add(Component.translatable("tooltip.mio_icif.energy_container.stored", stored, TesuControllerBlockEntity.CAPACITY).withStyle(ChatFormatting.GREEN));
        tooltip.add(Component.translatable("tooltip.mio_icif.energy_container.capacity", TesuControllerBlockEntity.CAPACITY).withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.mio_icif.tesu.controller").withStyle(ChatFormatting.LIGHT_PURPLE));
        tooltip.add(Component.translatable("tooltip.mio_icif.tesu.multiblock_hint").withStyle(ChatFormatting.YELLOW));
    }
}
