// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.core.runtime.energy.grid;

import com.miophas.singularity_iteration.core.api.energy.grid.IEnergyEmitter;
import com.miophas.singularity_iteration.core.api.energy.grid.IEnergySink;
import com.miophas.singularity_iteration.core.api.energy.grid.ILocatableTile;

import com.miophas.singularity_iteration.core.api.energy.storage.EUApi;
import com.miophas.singularity_iteration.core.api.energy.storage.ILongEnergyStorage;
import com.miophas.singularity_iteration.core.platform.neoforge.energy.EnergyCompatibility;
import com.miophas.singularity_iteration.core.api.energy.grid.IEnergySink;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;

/** Live sided capability adapter; never retains a capability beyond a transaction. */
public final class FECompatTile implements IEnergySink, ILocatableTile, IEnergyStorage {
    private final Level world;
    private final BlockPos pos;
    private final Direction input;
    private boolean registered;

    /** direction is the existing SI caller's direction from its wire to the receiver. */
    public FECompatTile(Level world, BlockPos pos, Direction direction) {
        this.world = java.util.Objects.requireNonNull(world);
        this.pos = pos.immutable();
        this.input = java.util.Objects.requireNonNull(direction).getOpposite();
    }
    public boolean loaded() {
        if (!(world instanceof ServerLevel level) || !level.getServer().isSameThread()) return false;
        return level.getChunkSource().getChunkNow(pos.getX() >> 4, pos.getZ() >> 4) != null
            && level.shouldTickBlocksAt(net.minecraft.world.level.ChunkPos.asLong(pos));
    }
    // Call only after the public entry has checked thread/chunk availability.
    private IEnergyStorage capabilityWhenLoaded() {
        var eu = world.getCapability(EUApi.SIDED, pos, input);
        if (eu != null) return eu;
        var wide = world.getCapability(ILongEnergyStorage.BLOCK, pos, input);
        if (wide != null) return wide;
        var fe = world.getCapability(Capabilities.EnergyStorage.BLOCK, pos, input);
        if (fe != null) return fe;
        return EnergyCompatibility.get().findEuStorage(world, pos, input);
    }
    private IEnergySink ae2WhenLoaded() {
        return EnergyCompatibility.get().findNetworkSink(world, pos);
    }
    public boolean isValid() {
        if (!loaded()) return false;
        boolean valid = capabilityWhenLoaded() != null || ae2WhenLoaded() != null;
        return valid && loaded();
    }
    public boolean isRegistered() { return registered; }
    public void setRegistered(boolean value) { registered = value; }
    public Direction inputSide() { return input; }
    @Override public Level getWorld() { return world; }
    @Override public BlockPos getPos() { return pos; }
    @Override public boolean acceptsEnergyFrom(IEnergyEmitter source, Direction side) { return side == input && canReceive(); }
    @Override public double getDemandedEnergy() { return receiveEnergy(Integer.MAX_VALUE, true) / 4.0; }
    @Override public int getSinkTier() { return 14; }
    @Override public double injectEnergy(Direction side, double amount, double voltage) {
        if (side != input || !Double.isFinite(amount) || amount <= 0) return amount;
        int offer = (int) Math.min(Integer.MAX_VALUE, Math.floor(amount * 4.0));
        return amount - receiveEnergy(offer, false) / 4.0;
    }
    @Override public int receiveEnergy(int amount, boolean simulate) {
        if (amount <= 0 || !loaded()) return 0;
        var sink = ae2WhenLoaded();
        if (sink != null) {
            int offered = (int)Math.min(amount, Math.floor(Math.max(0, sink.getDemandedEnergy()) * 4.0));
            if (!loaded()) return 0;
            if (simulate || offered == 0) return offered;
            double rejected = sink.injectEnergy(input, offered / 4.0, offered / 4.0);
            if (!Double.isFinite(rejected) || rejected < 0 || rejected > offered / 4.0)
                throw new IllegalStateException("Invalid AE2 energy receipt at " + pos);
            // FE has integer granularity. Round the debit upwards, never create EU by truncation.
            return Math.min(offered, (int)Math.ceil((offered / 4.0 - rejected) * 4.0));
        }
        var target = capabilityWhenLoaded();
        // Providers and policy callbacks are foreign code; they can unload the target.
        if (target == null || !loaded() || !target.canReceive() || !loaded()) return 0;
        int accepted = target.receiveEnergy(amount, simulate);
        if (accepted < 0 || accepted > amount) throw new IllegalStateException("Invalid external FE receipt at " + pos);
        return accepted;
    }
    @Override public int extractEnergy(int amount, boolean simulate) {
        if (amount <= 0 || !loaded()) return 0;
        var target = capabilityWhenLoaded();
        if (target == null || !loaded() || !target.canExtract() || !loaded()) return 0;
        int extracted = target.extractEnergy(amount, simulate);
        if (extracted < 0 || extracted > amount) throw new IllegalStateException("Invalid external FE extraction at " + pos);
        return extracted;
    }
    @Override public int getEnergyStored() {
        if (!loaded()) return 0;
        var target = capabilityWhenLoaded();
        return target == null || !loaded() ? 0 : target.getEnergyStored();
    }
    @Override public int getMaxEnergyStored() {
        if (!loaded()) return 0;
        var target = capabilityWhenLoaded();
        return target == null || !loaded() ? 0 : target.getMaxEnergyStored();
    }
    @Override public boolean canReceive() {
        if (!loaded()) return false;
        var sink = ae2WhenLoaded();
        if (sink != null) return sink.getDemandedEnergy() > 0 && loaded();
        var target = capabilityWhenLoaded();
        return target != null && loaded() && target.canReceive();
    }
    @Override public boolean canExtract() {
        if (!loaded()) return false;
        var target = capabilityWhenLoaded();
        return target != null && loaded() && target.canExtract();
    }
}
