package com.singularity_iteration.mio_icif.energy.grid;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.NeoForge;

/**
 * Base class for energy tile entities that auto-register with the energy net
 * on load and unregister on unload.
 */
@SuppressWarnings("null")
public abstract class EnergyTileBase extends BlockEntity implements IEnergyTile {

    private boolean registered = false;

    public EnergyTileBase(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (level != null && !level.isClientSide && !registered) {
            NeoForge.EVENT_BUS.post(new EnergyTileLoadEvent(this, level));
            registered = true;
        }
    }

    @Override
    public void setRemoved() {
        if (level != null && !level.isClientSide && registered) {
            NeoForge.EVENT_BUS.post(new EnergyTileUnloadEvent(this, level));
            registered = false;
        }
        super.setRemoved();
    }

    @Override
    public void clearRemoved() {
        super.clearRemoved();
        if (level != null && !level.isClientSide && !registered) {
            NeoForge.EVENT_BUS.post(new EnergyTileLoadEvent(this, level));
            registered = true;
        }
    }

    /**
     * Called when this tile's energy net registration should be refreshed.
     */
    protected void markForGridUpdate() {
        if (level != null && !level.isClientSide && registered) {
            // Re-register by remove + add
            NeoForge.EVENT_BUS.post(new EnergyTileUnloadEvent(this, level));
            NeoForge.EVENT_BUS.post(new EnergyTileLoadEvent(this, level));
        }
    }
}

