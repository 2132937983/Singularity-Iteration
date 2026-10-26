package com.miophas.singularity_iteration.common.block.wiring;

import com.miophas.singularity_iteration.common.registry.mio_icif_block_entities;
import com.miophas.singularity_iteration.core.platform.neoforge.energy.IndependentSpecialCableBlockEntity;
import com.miophas.singularity_iteration.core.runtime.energy.engine.ProtectionRelay;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/**
 * 0.1.10.22 (G5): EU Fuse Cable. A cheap one-time protection: it blows for an overload, a short circuit or an
 * overvoltage and stays open. Right-click a blown fuse with a new fuse cable to restore it (the new one is used up).
 * Right-click with an empty hand opens the settings (rated flow, rated voltage, delay).
 */
@SuppressWarnings("null")
public class mio_icif_block_wire_fuse extends mio_icif_block_wire_breaker {

    public static final MapCodec<mio_icif_block_wire_fuse> CODEC = RecordCodecBuilder.mapCodec(instance ->
        instance.group(propertiesCodec()).apply(instance, mio_icif_block_wire_fuse::new));

    public mio_icif_block_wire_fuse(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected ProtectionRelay.Kind protectionKind() { return ProtectionRelay.Kind.FUSE; }

    @Override
    protected BlockEntityType<?> entityType() { return mio_icif_block_entities.WIRE_FUSE.get(); }

    /** A new fuse cable on a blown fuse restores it. */
    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
                                              InteractionHand hand, BlockHitResult hit) {
        if (stack.is(asItem()) && level.getBlockEntity(pos) instanceof IndependentSpecialCableBlockEntity cable && cable.protective()
                && cable.relay().state() == ProtectionRelay.State.BLOWN) {
            if (!level.isClientSide && cable.resetProtection()) {
                if (!player.getAbilities().instabuild) stack.shrink(1);
                level.playSound(null, pos, SoundEvents.ITEM_FRAME_ADD_ITEM, SoundSource.BLOCKS, 0.8F, 1.2F);
            }
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }
        return super.useItemOn(stack, state, level, pos, player, hand, hit);
    }
}
