// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.core.platform.neoforge.energy;

import com.miophas.singularity_iteration.core.runtime.energy.engine.NetworkCell;
import com.miophas.singularity_iteration.core.runtime.energy.engine.EnergyAmount;
import com.miophas.singularity_iteration.core.runtime.energy.engine.TransformerAccounting;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Independently authored platform entity. Registration, mode interpretation,
 * direction routing and replacement of the host block's ticker are supplied by
 * its integration. It neither implements nor calls an old mod energy API.
 *
 * <p>The ordinary buffer/mode tag names come from retained SI runtime saves.
 * Unsupported numeric saves are preserved, not truncated into an energy quote.
 */
public final class IndependentTransformerBlockEntity extends BlockEntity {
    private final long lowPacket;
    private double savedBuffer;
    private long savedFraction;
    private int savedMode;
    private NetworkCell cell;
    private boolean modeObserved, observedStepUp;

    public record Snapshot(NetworkCell.Quote energy, BlockState state, int mode,
                           boolean stepUp, TransformerAccounting.Configuration limits) { }

    public IndependentTransformerBlockEntity(BlockEntityType<?> type, BlockPos position,
            BlockState state, long lowPacket, int initialMode) {
        super(type, position, state);
        // Validate the tier using the independently measured numeric policy.
        new TransformerAccounting.Configuration(lowPacket, false);
        this.lowPacket = lowPacket;
        savedMode = initialMode;
    }

    private void requireServerThread() {
        if (!(level instanceof ServerLevel world) || !world.getServer().isSameThread())
            throw new IllegalStateException("Transformer storage requires its server thread");
    }

    /** Caller supplies the interpreted mode; raw mode numbers have no guessed meaning here. */
    public Optional<Snapshot> snapshot(boolean stepUp) {
        requireServerThread();
        if (isRemoved()) return Optional.empty();
        if (cell == null) {
            // Ordinary double saves cannot preserve every integer beyond 2^53.
            // Retain out-of-scope input for a future migration rather than lose it.
            if (!Double.isFinite(savedBuffer) || savedBuffer < 0
                    || savedBuffer > 9_007_199_254_740_992d)
                return Optional.empty();
            cell = new NetworkCell(Math.rint(savedBuffer) == savedBuffer
                ? new EnergyAmount((long) savedBuffer, savedFraction) : EnergyAmount.fromDouble(savedBuffer));
        }
        return Optional.of(new Snapshot(cell.quote(), getBlockState(), savedMode, stepUp,
            new TransformerAccounting.Configuration(lowPacket, stepUp)));
    }

    /** World/chunk registration identity must additionally be checked by the engine. */
    public boolean isCurrent(Snapshot expected, boolean currentStepUp) {
        requireServerThread();
        return !isRemoved() && cell != null && expected.energy() == cell.quote()
            && expected.state() == getBlockState() && expected.mode() == savedMode
            && expected.stepUp() == currentStepUp
            && expected.limits().equals(new TransformerAccounting.Configuration(lowPacket, currentStepUp));
    }

    public int savedMode() { return savedMode; }

    /** Packet size on the low-voltage side (EU). */
    public long lowPacket() { return lowPacket; }

    /** Packet size on the high-voltage side. This implementation uses a 4x step per tier, the same as the legacy CableTier sequence. */
    public long highPacket() { return lowPacket * 4; }

    /** Buffer capacity. It uses the legacy formula (low-side rating x 8). */
    public double bufferCapacity() { return lowPacket * 8.0d; }

    /** Current buffered energy (EU, integer part). */
    public double storedEnergy() { return cell == null ? savedBuffer : cell.quote().amount(); }

    public boolean hasNeighborRedstoneSignal() {
        return level != null && level.hasNeighborSignal(worldPosition);
    }

    /** Existing SI fixed modes remain 0/1. Mode 2 is this independent adapter's automatic mode. */
    public boolean validMode() { return savedMode >= 0 && savedMode <= 2; }

    public boolean stepUpNow() {
        requireServerThread();
        return savedMode == 0 || savedMode == 2 && level.hasNeighborSignal(worldPosition);
    }

    /** Observe public redstone input once per world frame; energy changes do not renew routing. */
    public boolean refreshMode() {
        requireServerThread();
        boolean stepUp = stepUpNow();
        boolean changed = modeObserved && observedStepUp != stepUp;
        modeObserved = true;
        observedStepUp = stepUp;
        if (changed) {
            if (cell != null) cell.replace(cell.quote().exactAmount());
            setChanged();
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 2);
            applyActiveProperty();
        }
        return changed;
    }

    /**
     * Writes the step-up visual state to the {@code active} property of the host block. (It finds the property by name,
     * because core does not know the block class. If the block has no such property, it skips silently.)
     */
    private void applyActiveProperty() {
        if (level == null || level.isClientSide || isRemoved()) return;
        var property = getBlockState().getBlock().getStateDefinition().getProperty("active");
        if (!(property instanceof net.minecraft.world.level.block.state.properties.BooleanProperty active)
                || !getBlockState().hasProperty(active)) return;
        boolean shouldBe = stepUpNow();
        if (getBlockState().getValue(active) != shouldBe) {
            level.setBlock(worldPosition, getBlockState().setValue(active, shouldBe), 3);
        }
    }

    public int routingSignature() { return savedMode * 2 + (stepUpNow() ? 1 : 0); }

    public void setSavedMode(int mode) {
        requireServerThread();
        if (mode == savedMode) return;
        if (cell != null) cell.replace(cell.quote().exactAmount());
        savedMode = mode;
        setChanged();
        level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 2);
        applyActiveProperty();
    }

    /** Called after the complete numeric transaction, never from inside its write phase. */
    public void markNetworkChanged() { requireServerThread(); setChanged(); }

    private void revokeCell() {
        if (cell == null) return;
        savedBuffer = cell.quote().amount();
        savedFraction = cell.quote().fraction();
        cell.retire();
        cell = null;
    }

    @Override public void setLevel(Level newLevel) {
        if (level != newLevel) revokeCell();
        super.setLevel(newLevel);
    }

    @Override public void setRemoved() {
        revokeCell();
        super.setRemoved();
    }

    @Override protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        revokeCell();
        super.loadAdditional(tag, registries);
        // Legacy saves use buffer_long / buffer_fraction / redstone_signal.
        // This code moves them to the new names one time, so that stored energy is not lost after the change.
        if (tag.contains("buffer_long")) {
            savedBuffer = tag.getLong("buffer_long");
            double fraction = tag.getDouble("buffer_fraction");
            // Legacy saves store an EU fraction, new saves store 2^-52 EU units.
            var decoded = Double.isFinite(fraction) && fraction >= 0 && fraction < 1
                ? EnergyAmount.fromDouble(fraction) : EnergyAmount.ZERO;
            savedBuffer += decoded.whole();
            savedFraction = decoded.fraction();
        } else {
            savedBuffer = tag.getDouble("buffer");
            savedFraction = tag.getLong("scex_energy_fraction");
        }
        if (savedFraction < 0 || savedFraction >= EnergyAmount.UNITS) savedFraction = 0;
        if (tag.contains("mode")) savedMode = tag.getInt("mode");
        modeObserved = false;
        observedStepUp = tag.getBoolean("active") || tag.getBoolean("redstone_signal");
    }

    @Override protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putDouble("buffer", cell == null ? savedBuffer : cell.quote().amount());
        tag.putLong("scex_energy_fraction", cell == null ? savedFraction : cell.quote().fraction());
        tag.putInt("mode", savedMode);
        tag.putBoolean("active", observedStepUp);
    }

    /**
     * 0.1.10.17 (E-2): the client gets the mode and the step-up flag only. The stored energy reaches the
     * screen through the menu data; the buffer is server data.
     */
    @Override public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        tag.putInt("mode", savedMode);
        tag.putBoolean("active", observedStepUp);
        return tag;
    }

    @Override public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
