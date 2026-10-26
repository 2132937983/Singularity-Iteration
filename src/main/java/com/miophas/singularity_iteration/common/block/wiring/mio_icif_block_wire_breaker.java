package com.miophas.singularity_iteration.common.block.wiring;

import com.miophas.singularity_iteration.common.block.wire.mio_icif_block_wire;
import com.miophas.singularity_iteration.common.registry.mio_icif_block_entities;
import com.miophas.singularity_iteration.core.api.energy.storage.CableTier;
import com.miophas.singularity_iteration.core.platform.neoforge.energy.IndependentSpecialCableBlockEntity;
import com.miophas.singularity_iteration.core.runtime.energy.engine.ProtectionRelay;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

/**
 * 0.1.10.22 (G5): EU Breaker Cable. An IV cable with a circuit breaker: it opens for an overload (inverse time), a
 * short circuit or an overvoltage, and closes again by itself after 3 s (at most three times in a row, then it locks
 * out). A redstone signal holds it open, like the splitter cable. Right-click opens its settings (rated flow,
 * rated voltage, delay, automatic reclosing) and the reset of a locked-out breaker. The engine runs the protection
 * ({@link ProtectionRelay}); the block only shows the state ({@link #ACTIVE}).
 */
@SuppressWarnings("null")
public class mio_icif_block_wire_breaker extends mio_icif_block_wire {

    public static final BooleanProperty ACTIVE = BooleanProperty.create("active");

    public static final MapCodec<mio_icif_block_wire_breaker> CODEC = RecordCodecBuilder.mapCodec(instance ->
        instance.group(propertiesCodec()).apply(instance, mio_icif_block_wire_breaker::new));

    public mio_icif_block_wire_breaker(Properties properties) {
        super(properties, CableTier.IV, 1, 2);
        this.registerDefaultState(this.defaultBlockState().setValue(ACTIVE, true));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(ACTIVE);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    /** The protection of this cable. */
    protected ProtectionRelay.Kind protectionKind() { return ProtectionRelay.Kind.BREAKER; }

    protected BlockEntityType<?> entityType() { return mio_icif_block_entities.WIRE_BREAKER.get(); }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new IndependentSpecialCableBlockEntity(entityType(), pos, state, protectionKind());
    }

    /** The energy engine evaluates the protection at the end of each tick; the block needs no ticker. */
    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return null;
    }

    /** Right-click with an empty hand: the settings screen (client). */
    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof IndependentSpecialCableBlockEntity cable) || !cable.protective())
            return super.useWithoutItem(state, level, pos, player, hit);
        if (level.isClientSide) com.miophas.singularity_iteration.common.client.screen.ProtectionSettingsScreen.open(pos);
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
