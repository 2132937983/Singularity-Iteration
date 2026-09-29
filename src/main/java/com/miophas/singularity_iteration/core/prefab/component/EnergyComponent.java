package com.miophas.singularity_iteration.core.prefab.component;

import com.miophas.singularity_iteration.core.api.energy.EnergyPortPolicy;
import com.miophas.singularity_iteration.core.api.energy.storage.CableTier;
import com.miophas.singularity_iteration.core.api.energy.storage.IEUEnergyStorage;
import com.miophas.singularity_iteration.core.runtime.energy.CustomEUEnergyStorage;
import com.miophas.singularity_iteration.core.runtime.energy.FeLedger;
import com.miophas.singularity_iteration.core.runtime.energy.IndependentSiEnergy;
import java.util.Objects;
import java.util.function.Supplier;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import net.neoforged.neoforge.energy.IEnergyStorage;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

/**
 * Composable native EU storage. Forward setLevel/onLoad/setRemoved/clearRemoved and NBT calls
 * from the owning block entity. Register the block ID with EnergyNodeRegistry during setup.
 * Every port uses the same owned balance. External EU/FE quotas and native output
 * share the ledger; native input retains the engine's packet-voltage semantics.
 */
public final class EnergyComponent {
    private final BlockEntity owner;
    private final CustomEUEnergyStorage storage;
    private final Supplier<EnergyPortPolicy> policy;
    private final FeLedger ledger;
    private final IEUEnergyStorage[] euPorts = new IEUEnergyStorage[7];
    private final IEnergyStorage[] fePorts = new IEnergyStorage[7];
    private boolean loaded;
    private @Nullable Level attachedLevel;

    public EnergyComponent(BlockEntity owner, long capacity, long maxReceive, long maxExtract,
            CableTier tier, Supplier<EnergyPortPolicy> policy) {
        this.owner = Objects.requireNonNull(owner);
        if (capacity < 0 || maxReceive < 0 || maxExtract < 0) throw new IllegalArgumentException("Negative energy limit");
        if (!(owner instanceof EnergyComponentHost)) throw new IllegalArgumentException("Owner must implement EnergyComponentHost");
        this.policy = Objects.requireNonNull(policy);
        storage = new CustomEUEnergyStorage(capacity, maxReceive, maxExtract, tier);
        storage.scexSetNetworkControlled(true);
        ledger = new FeLedger(storage, () -> owner.getLevel() instanceof ServerLevel world ? world.getServer().getTickCount() : 0, this::active);
    }

    public EnergyPortPolicy ports() { return Objects.requireNonNull(policy.get(), "energy port policy"); }
    public long stored() { return storage.getAmount(); }
    public long capacity() { return storage.getCapacity(); }
    public CableTier tier() { return storage.getCableTier(); }

    public long consume(long amount, boolean simulate) {
        return active() ? storage.consumeEnergyInternal(amount, simulate) : 0;
    }

    public long generate(long amount, boolean simulate) {
        return active() ? storage.generateEnergyInternal(amount, simulate) : 0;
    }

    public void setLevel(Level level) {
        if (attachedLevel != level) { loaded = false; storage.scexRetireNetworkCell(); }
        attachedLevel = level;
        storage.setBlockContext(level, owner.getBlockPos());
    }

    public void onLoad() {
        if (owner.getLevel() == null || owner.getLevel().isClientSide) return;
        if (!IndependentSiEnergy.controls(owner.getBlockState()))
            throw new IllegalStateException("Register an energy profile before loading " + owner.getBlockPos());
        setLevel(owner.getLevel());
        loaded = true;
        IndependentSiEnergy.changed(owner);
    }

    public void setRemoved() {
        loaded = false;
        if (owner.getLevel() != null && !owner.getLevel().isClientSide) IndependentSiEnergy.changed(owner);
        storage.scexRetireNetworkCell();
    }

    public void clearRemoved() { onLoad(); }

    public void onChunkUnloaded() { setRemoved(); }

    public void portsChanged() {
        if (owner.getLevel() != null && !owner.getLevel().isClientSide) {
            owner.getLevel().invalidateCapabilities(owner.getBlockPos());
            IndependentSiEnergy.changed(owner);
        }
    }

    public void save(CompoundTag tag) {
        tag.putLong("energy", stored());
        tag.putLong("scex_energy_fraction", storage.scexSavedFraction());
        tag.putInt("scex_fe_uncertain_output", ledger.uncertainOutput());
    }

    public void load(CompoundTag tag) {
        if (tag.contains("energy", Tag.TAG_ANY_NUMERIC)) storage.setEnergy(tag.getLong("energy"));
        storage.scexLoadFraction(tag.getLong("scex_energy_fraction"));
        ledger.loadUncertainOutput(tag.getInt("scex_fe_uncertain_output"));
    }

    public IEUEnergyStorage euPort(@Nullable Direction side) {
        int index = side == null ? 6 : side.ordinal();
        if (euPorts[index] == null) euPorts[index] = new EuPort(side);
        return euPorts[index];
    }

    public IEnergyStorage fePort(@Nullable Direction side) {
        int index = side == null ? 6 : side.ordinal();
        if (fePorts[index] == null) fePorts[index] = ledger.port(() -> ports().accepts(side), () -> ports().emits(side));
        return fePorts[index];
    }

    private boolean active() {
        if (!loaded || owner.isRemoved() || !(owner.getLevel() instanceof ServerLevel world)
                || !world.getServer().isSameThread()) return false;
        var pos = owner.getBlockPos();
        var chunk = world.getChunkSource().getChunkNow(pos.getX() >> 4, pos.getZ() >> 4);
        return chunk != null && world.shouldTickBlocksAt(net.minecraft.world.level.ChunkPos.asLong(pos))
            && chunk.getBlockEntity(pos, LevelChunk.EntityCreationType.CHECK) == owner;
    }

    /** Engine implementation access; addons should use the operations and ports above. */
    @ApiStatus.Internal public CustomEUEnergyStorage internalStorage() { return storage; }
    /** Engine implementation access, sharing the same allowance as every published port. */
    @ApiStatus.Internal public FeLedger internalLedger() { return ledger; }

    private final class EuPort implements IEUEnergyStorage {
        private final @Nullable Direction side;
        private EuPort(@Nullable Direction side) { this.side = side; }
        @Override public long receive(long amount, boolean simulate) { return ports().accepts(side) ? ledger.receiveWholeEu(amount, simulate) : 0; }
        @Override public long extract(long amount, boolean simulate) { return ports().emits(side) ? ledger.extractWholeEu(amount, simulate) : 0; }
        @Override public long getAmount() { return stored(); }
        @Override public long getCapacity() { return capacity(); }
        @Override public boolean canReceive() { return active() && ports().accepts(side) && storage.getMaxReceive() > 0; }
        @Override public boolean canExtract() { return active() && ports().emits(side) && storage.isOutputEnabled() && storage.getMaxExtract() > 0 && ledger.uncertainOutput() == 0; }
        @Override public boolean canConnect(CableTier cableTier) { return active() && (ports().accepts(side) || ports().emits(side)) && storage.canConnect(cableTier); }
        @Override public long generateEnergy(long amount, boolean simulate) { return receive(amount, simulate); }
        @Override public long useEnergy(long amount, boolean simulate) { return extract(amount, simulate); }
    }
}
