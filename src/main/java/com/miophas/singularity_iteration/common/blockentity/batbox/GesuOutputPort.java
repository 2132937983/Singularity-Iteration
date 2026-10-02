package com.miophas.singularity_iteration.common.blockentity.batbox;

import com.miophas.singularity_iteration.core.api.energy.ICableTier;
import com.miophas.singularity_iteration.core.api.energy.grid.IEnergyAcceptor;
import com.miophas.singularity_iteration.core.api.energy.grid.IEnergyEmitter;
import com.miophas.singularity_iteration.core.api.energy.storage.CableTier;
import com.miophas.singularity_iteration.core.prefab.blockentity.MultiblockEnergyPart;
import com.miophas.singularity_iteration.core.runtime.energy.IndependentSiEnergy;
import com.miophas.singularity_iteration.core.runtime.energy.grid.EnergyNetGlobal;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * GESU output port (IV / LuV).
 *
 * <p>Bug fixed: the energy engine quotes a source from <i>its own</i> storage. The port
 * was a 1 EU, zero-extract storage that only implemented the legacy
 * {@code getOfferedEnergy/drawEnergy} pair, so the network never saw any energy to take
 * and the GESU could charge but never discharge. In addition the port's role switch
 * (consumer while unformed, source once formed) never told the engine, so a port that
 * joined the network before the structure formed kept its old "no outputs" face mask.
 *
 * <p>Now the port is a small relay buffer (two packets) that is refilled from the core
 * every tick and drained by the network like any other storage source, and every role
 * change republishes the port's topology.
 */
@SuppressWarnings("null")
public abstract class GesuOutputPort extends MultiblockEnergyPart {
    private final ICableTier tier;
    private final long rate;
    private boolean sourceRole;

    protected GesuOutputPort(BlockEntityType<?> type, BlockPos pos, BlockState state, ICableTier tier) {
        super(pos, state, type, tier.getPowerRating() * 2, 0, tier.getPowerRating(), tier);
        this.tier = tier;
        this.rate = tier.getPowerRating();
    }

    public long rate() { return rate; }

    protected static void tickPort(Level level, GesuOutputPort port) {
        if (level.isClientSide()) return;
        boolean formed = port.isStructureCompleted() && port.getCorePosition() != null && port.core() != null;
        if (formed != port.sourceRole) {
            port.sourceRole = formed;
            if (formed) port.setAsPowerSource(port.rate);
            else port.setAsConsumer();
            port.getEnergyStorageInternal().setMaxExtract(formed ? port.rate : 0);
            port.setChanged();
            IndependentSiEnergy.changed(port);      // republish faces: source <-> consumer
        } else if (formed && !port.isPowerSource()) {
            port.setAsPowerSource(port.rate);       // restored from a save
        }
        if (formed) port.refill();
    }

    /** Moves energy from the core into the relay buffer (at most one packet per tick plus the buffer gap). */
    private void refill() {
        mio_icif_gesu_core_entity core = core();
        if (core == null || !core.isStructureComplete()) return;
        long space = getEnergyCapacity() - getStoredEnergy();
        if (space <= 0) return;
        long want = Math.min(space, Math.min(core.getStoredEnergy(), rate * 2));
        if (want <= 0) return;
        long taken = core.consumeEnergy(want, false);
        if (taken > 0) {
            long stored = generateEnergyInternal(taken, false);
            if (stored < taken) core.generateEnergyInternal(taken - stored, false);   // never lose energy
            core.setChanged();
            setChanged();
        }
    }

    @Nullable
    protected mio_icif_gesu_core_entity core() {
        if (getCorePosition() == null || level == null || !level.isLoaded(getCorePosition())) return null;
        BlockEntity be = level.getBlockEntity(getCorePosition());
        return be instanceof mio_icif_gesu_core_entity core ? core : null;
    }

    // ---- legacy grid API (used only when the independent engine is disabled)
    @Override public double getOfferedEnergy() { return isPowerSource() ? Math.min(rate, getStoredEnergy()) : 0.0D; }
    @Override public void drawEnergy(double amount) { if (isPowerSource() && amount > 0) consumeEnergy((long) amount, false); }
    @Override public int getSourceTier() { return isPowerSource() ? EnergyNetGlobal.cableTierToSourceTier((CableTier) tier) : -1; }
    @Override public boolean emitsEnergyTo(IEnergyAcceptor acceptor, Direction direction) { return isPowerSource(); }
    @Override public double getDemandedEnergy() { return 0.0D; }
    @Override public double injectEnergy(Direction direction, double amount, double voltage) { return amount; }
    @Override public boolean acceptsEnergyFrom(IEnergyEmitter emitter, Direction direction) { return false; }
}
