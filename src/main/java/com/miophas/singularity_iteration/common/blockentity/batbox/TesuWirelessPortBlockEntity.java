package com.miophas.singularity_iteration.common.blockentity.batbox;

import com.miophas.singularity_iteration.common.registry.mio_icif_block_entities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * 0.1.11.158: the TESU wireless output port. In a port slot of a formed TESU it is the centre of a wireless field
 * ({@link TesuWireless}); it holds no energy itself: machines inside the field take theirs from the controller. The controller
 * binds it when the structure forms and lets it go when it breaks.
 */
@SuppressWarnings("null")
public class TesuWirelessPortBlockEntity extends BlockEntity {
    @Nullable private BlockPos controller;

    public TesuWirelessPortBlockEntity(BlockPos pos, BlockState state) {
        super(mio_icif_block_entities.TESU_WIRELESS_PORT.get(), pos, state);
    }

    /** 0.1.11.206: for the wireless induction port, a wireless output port of its own type. */
    protected TesuWirelessPortBlockEntity(net.minecraft.world.level.block.entity.BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    /** Bound to a controller, or let go ({@code null}). */
    public void bind(@Nullable BlockPos pos) {
        if (java.util.Objects.equals(controller, pos)) return;
        controller = pos == null ? null : pos.immutable();
        setChanged();
    }

    /** The formed controller feeding this port, or {@code null}. */
    @Nullable
    public TesuControllerBlockEntity controller() {
        if (controller == null || level == null || !level.isLoaded(controller)) return null;
        return level.getBlockEntity(controller) instanceof TesuControllerBlockEntity core && core.isStructureComplete() ? core : null;
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (level != null && !level.isClientSide) TesuWireless.add(level, worldPosition);
    }

    @Override
    public void setRemoved() {
        if (level != null && !level.isClientSide) TesuWireless.remove(level, worldPosition);
        super.setRemoved();
    }
}
