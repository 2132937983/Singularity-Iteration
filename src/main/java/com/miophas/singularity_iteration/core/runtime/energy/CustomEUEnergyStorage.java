package com.miophas.singularity_iteration.core.runtime.energy;

import com.miophas.singularity_iteration.core.api.energy.IEnergyStorageAccess;
import com.miophas.singularity_iteration.core.prefab.component.EnergyComponentHost;
import com.miophas.singularity_iteration.core.api.energy.tile.IExplosionPowerOverride;
import com.miophas.singularity_iteration.core.api.energy.storage.CableTier;
import com.miophas.singularity_iteration.core.api.energy.storage.IEUEnergyStorage;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import com.miophas.singularity_iteration.core.runtime.energy.engine.EnergyAmount;

@SuppressWarnings("null")
public class CustomEUEnergyStorage implements IEUEnergyStorage, IEnergyStorageAccess {

    protected long energy;
    protected long capacity;
    protected long maxReceive;
    protected long maxExtract;
    protected final CableTier cableTier;

    protected BlockPos pos;
    protected Level level;
    
    protected long powerOutput = 0;
    protected boolean isPowerSource = false;
    protected boolean outputEnabled = true; // 红石控制输出开关

    // Independent-network ownership is explicit and fixed by the containing
    // block's development policy before its saved balance is read.
    private boolean scexNetworkControlled;
    private final CustomEUEnergyStorage scexViewedStorage;
    private com.miophas.singularity_iteration.core.runtime.energy.engine.NetworkCell scexCell;
    private long scexFraction;
    private final com.miophas.singularity_iteration.core.runtime.energy.InternalPaymentLedger scexPayments = new com.miophas.singularity_iteration.core.runtime.energy.InternalPaymentLedger();

    /**
     * Pay from this storage before invoking an effect with foreign callbacks.
     * Both network modes use the same balance. The caller must settle the token
     * in the same synchronous server-thread operation, including exceptional paths.
     */
    public final InternalPayment scexReserveInternal(long amount) {
        scexRequireOwnedStorage();
        if (amount <= 0) throw new IllegalArgumentException("Payment must be positive");
        if (!(level instanceof ServerLevel world) || pos == null) return null;
        if (!world.getServer().isSameThread()) throw new IllegalStateException("Payment requires server thread");
        var chunk = world.getChunkSource().getChunkNow(pos.getX() >> 4, pos.getZ() >> 4);
        if (chunk == null || !world.shouldTickBlocksAt(net.minecraft.world.level.ChunkPos.asLong(pos))) return null;
        var tile = chunk.getBlockEntity(pos, net.minecraft.world.level.chunk.LevelChunk.EntityCreationType.CHECK);
        boolean owns = tile instanceof com.miophas.singularity_iteration.core.prefab.blockentity.AbstractEnergyBlockEntity machine
            && machine.getEnergyStorageInternal() == this
            || tile instanceof EnergyComponentHost host && host.energyComponent().internalStorage() == this
            || tile instanceof com.miophas.singularity_iteration.core.runtime.energy.DemandEnergySource demand && demand.ownedEnergy() == this;
        if (!owns || tile.isRemoved()) return null;
        var ticket = scexPayments.reserve(energy, amount);
        if (ticket == null) return null;
        scexSetWholePreservingFraction(energy - amount);
        return new InternalPayment(this, ticket, world);
    }

    public static final class InternalPayment {
        private final CustomEUEnergyStorage storage;
        private final com.miophas.singularity_iteration.core.runtime.energy.InternalPaymentLedger.Ticket ticket;
        private final ServerLevel world;
        private InternalPayment(CustomEUEnergyStorage storage, com.miophas.singularity_iteration.core.runtime.energy.InternalPaymentLedger.Ticket ticket, ServerLevel world) {
            this.storage = storage; this.ticket = ticket; this.world = world;
        }
        public long amount() { return ticket.amount(); }
        public boolean isPending() { return ticket.isPending(); }
        public boolean commit() {
            checkThread();
            return ticket.commit();
        }
        public boolean cancel() {
            checkThread();
            if (!ticket.isPending()) return false;
            long restored = Math.addExact(storage.energy, ticket.amount());
            if (!ticket.cancel()) return false;
            // Restore only this escrow, preserving every callback credit/debit and fraction.
            // Capacity shrink never deletes already owned EU, as with setCapacity itself.
            storage.scexSetWholePreservingFraction(restored);
            return true;
        }
        private void checkThread() {
            if (!world.getServer().isSameThread()) throw new IllegalStateException("Payment requires original server thread");
        }
    }

    private void scexRequireNoPayment(String operation) {
        scexRequireOwnedStorage();
        if (scexPayments.reserved() != 0) throw new IllegalStateException(operation + " during pending internal payment");
    }
    private void scexRequireOwnedStorage() {
        if (scexViewedStorage != null) throw new UnsupportedOperationException("Capability views cannot mutate owned network state");
    }
    public final boolean scexNetworkControlled() {
        return scexViewedStorage == null ? scexNetworkControlled : scexViewedStorage.scexNetworkControlled();
    }
    public final void scexSetNetworkControlled(boolean controlled) {
        if (controlled == scexNetworkControlled) return;
        scexRequireNoPayment("Ownership change");
        if (scexCell != null) { scexCell.retire(); scexCell = null; }
        scexNetworkControlled = controlled;
    }

    /** Retire the independent-network identity when its owning block entity leaves the world. */
    public final void scexRetireNetworkCell() {
        scexRequireNoPayment("Network cell retirement");
        if (scexCell != null) {
            scexCell.retire();
            scexCell = null;
        }
    }

    public record NetworkQuote(Level ownerLevel, BlockPos ownerPosition, long amount, long capacity, long maxReceive, long maxExtract,
                               long output, boolean source, boolean outputEnabled, com.miophas.singularity_iteration.core.runtime.energy.engine.NetworkCell.Quote cell) {
        public EnergyAmount exactAmount() { return cell == null ? EnergyAmount.of(amount) : cell.exactAmount(); }
    }
    public record NetworkWrite(CustomEUEnergyStorage storage, NetworkQuote expected, long nextAmount, long nextFraction) {
        public NetworkWrite(CustomEUEnergyStorage storage, NetworkQuote expected, long nextAmount) {
            this(storage, expected, nextAmount, expected.exactAmount().fraction());
        }
        public NetworkWrite(CustomEUEnergyStorage storage, NetworkQuote expected, EnergyAmount next) {
            this(storage, expected, next.whole(), next.fraction());
        }
        public EnergyAmount exactNextAmount() { return new EnergyAmount(nextAmount, nextFraction); }
    }

    public final NetworkQuote scexNetworkQuote() {
        scexRequireOwnedStorage();
        com.miophas.singularity_iteration.core.runtime.energy.engine.NetworkCell.Quote cellQuote = null;
        if (scexNetworkControlled) {
            if (!(level instanceof ServerLevel world) || !world.getServer().isSameThread())
                throw new IllegalStateException("Controlled quote requires server thread");
            if (scexCell == null) scexCell = new com.miophas.singularity_iteration.core.runtime.energy.engine.NetworkCell(scexExactAmount());
            cellQuote = scexCell.quote();
            if (cellQuote.amount() != energy || cellQuote.fraction() != scexFraction) throw new IllegalStateException("Independent balance mirror diverged");
        }
        return new NetworkQuote(level, pos == null ? null : pos.immutable(), energy, getCapacity(), maxReceive, maxExtract, powerOutput, isPowerSource, outputEnabled, cellQuote);
    }

    /**
     * An independently authored, all-or-nothing main-thread balance commit.
     * Validation and conservation precede every write. The mutation loop only
     * assigns primitive fields; dirty marking follows the complete commit.
     * No old grid/API method is invoked and nominal capacity is not a clamp.
     */
    public static boolean scexCommitNetwork(java.util.List<NetworkWrite> requested,
                                            long dissipated, java.util.function.BooleanSupplier current) {
        return scexCommitNetwork(requested, java.util.List.of(), dissipated, current);
    }

    /**
     * Join independently owned endpoint cells to the same numeric transaction.
     * Their platform identity and policy snapshots must be covered by current;
     * NetworkCell additionally enforces thread ownership and revision validity.
     * No extra cell is wrapped through a legacy energy interface or setter.
     */
    public static boolean scexCommitNetwork(java.util.List<NetworkWrite> requested,
                                            java.util.List<com.miophas.singularity_iteration.core.runtime.energy.engine.NetworkCell.Write> additional,
                                            long dissipated, java.util.function.BooleanSupplier current) {
        return scexCommitNetwork(requested, additional, EnergyAmount.of(dissipated), current);
    }
    public static boolean scexCommitNetwork(java.util.List<NetworkWrite> requested,
                                            java.util.List<com.miophas.singularity_iteration.core.runtime.energy.engine.NetworkCell.Write> additional,
                                            EnergyAmount dissipated, java.util.function.BooleanSupplier current) {
        var writes = java.util.List.copyOf(requested);
        var extraWrites = java.util.List.copyOf(additional);
        var identities = new java.util.IdentityHashMap<CustomEUEnergyStorage, Boolean>();
        var balance = dissipated.units();
        for (var write : writes) {
            var storage = java.util.Objects.requireNonNull(write.storage());
            if (write.nextAmount() < 0 || identities.put(storage, Boolean.TRUE) != null)
                throw new IllegalArgumentException("Negative balance or duplicate storage");
            if (storage.pos == null || !(storage.level instanceof ServerLevel serverLevel) || !serverLevel.getServer().isSameThread())
                throw new IllegalStateException("Network commit requires a server-owned storage");
            balance = balance.add(write.exactNextAmount().units()).subtract(write.expected().exactAmount().units());
        }
        for (var write : extraWrites) {
            balance = balance.add(write.exactNextAmount().units()).subtract(write.expected().exactAmount().units());
        }
        if (balance.signum() != 0) throw new IllegalArgumentException("Nonconserving network commit");
        if (!current.getAsBoolean()) return false;
        for (var write : writes) {
            if (!write.storage().scexNetworkControlled || write.storage().scexPayments.reserved() != 0
                    || !write.storage().scexNetworkQuote().equals(write.expected())) return false;
            var storage = write.storage(); var world = (ServerLevel) storage.level;
            var chunk = world.getChunkSource().getChunkNow(storage.pos.getX() >> 4, storage.pos.getZ() >> 4);
            if (chunk == null || !world.shouldTickBlocksAt(net.minecraft.world.level.ChunkPos.asLong(storage.pos))) return false;
            var tile = chunk.getBlockEntity(storage.pos, net.minecraft.world.level.chunk.LevelChunk.EntityCreationType.CHECK);
            boolean owns = tile instanceof com.miophas.singularity_iteration.core.prefab.blockentity.AbstractEnergyBlockEntity machine
                && machine.getEnergyStorageInternal() == storage
                || tile instanceof EnergyComponentHost host && host.energyComponent().internalStorage() == storage
                || tile instanceof com.miophas.singularity_iteration.core.runtime.energy.DemandEnergySource demand && demand.ownedEnergy() == storage;
            if (!owns || tile.isRemoved()) return false;
        }
        var cellWrites = new java.util.ArrayList<com.miophas.singularity_iteration.core.runtime.energy.engine.NetworkCell.Write>();
        for (var write : writes) cellWrites.add(new com.miophas.singularity_iteration.core.runtime.energy.engine.NetworkCell.Write(write.expected().cell(), write.exactNextAmount()));
        cellWrites.addAll(extraWrites);
        if (!com.miophas.singularity_iteration.core.runtime.energy.engine.NetworkCell.commit(cellWrites, dissipated, () -> true)) return false;
        // Mirror the completed owned-state transaction without invoking setters
        // or callbacks; existing save and local machine readers still use energy.
        for (var write : writes) { write.storage().energy = write.nextAmount(); write.storage().scexFraction = write.nextFraction(); }
        for (var write : writes) {
            var storage = write.storage();
            if (storage.scexExactAmount().equals(write.expected().exactAmount())) continue;
            var serverLevel = (ServerLevel) storage.level;
            var chunk = serverLevel.getChunkSource().getChunkNow(storage.pos.getX() >> 4, storage.pos.getZ() >> 4);
            if (chunk != null) chunk.setUnsaved(true);
        }
        return true;
    }

    public CustomEUEnergyStorage(long capacity, long maxReceive, long maxExtract, CableTier cableTier) {
        this.capacity = capacity;
        this.maxReceive = maxReceive;
        this.maxExtract = maxExtract;
        this.cableTier = cableTier;
        this.energy = 0;
        this.scexViewedStorage = null;
    }

    /** ABI-compatible capability shell: reads an existing balance but never owns one. */
    protected CustomEUEnergyStorage(CustomEUEnergyStorage viewedStorage) {
        this.scexViewedStorage = java.util.Objects.requireNonNull(viewedStorage);
        this.cableTier = viewedStorage.getCableTier();
    }

    public CustomEUEnergyStorage(long capacity, long maxTransfer, CableTier cableTier) {
        this(capacity, maxTransfer, maxTransfer, cableTier);
    }

    // Explicitly override to resolve duplicate default method conflict between IEnergyStorageAccess and ILongEnergyStorage
    @Override
    public int getEnergyStored() {
        return (int) Math.min(energy, Integer.MAX_VALUE);
    }

    @Override
    public int getMaxEnergyStored() {
        return (int) Math.min(getCapacity(), Integer.MAX_VALUE);
    }

    public void setBlockContext(Level level, BlockPos pos) {
        if (this.level != level || !java.util.Objects.equals(this.pos, pos)) scexRequireNoPayment("Owner context change");
        this.level = level;
        this.pos = pos;
    }

    public void setAsPowerSource(long powerOutput) {
        this.isPowerSource = true;
        this.powerOutput = powerOutput;
    }

    public long getPowerOutput() {
        return isPowerSource ? powerOutput : 0;
    }

    public boolean isPowerSource() {
        return isPowerSource;
    }
    
    /**
     * 设置能量输出是否启用（红石控制）
     * @param enabled true=允许输出, false=禁止输出
     */
    public void setOutputEnabled(boolean enabled) {
        this.outputEnabled = enabled;
    }
    
    /**
     * 检查能量输出是否启用
     */
    public boolean isOutputEnabled() {
        return this.outputEnabled;
    }

    public long getPowerRating() {
        return cableTier.getPowerRating();
    }

    public long extractPowerForConsumer(long amount, boolean simulate) {
        if (!isPowerSource) {
            return 0;
        }
        return extract(amount, simulate);
    }

    public boolean isOverloaded(long gridPower) {
        return gridPower > getPowerRating();
    }

    public void triggerOverloadExplosion() {
        if (level == null || pos == null) {
            return;
        }

        if (!level.isClientSide) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof IExplosionPowerOverride override && !override.shouldExplode()) {
                return;
            }

            float explosionPower = calculateExplosionPower();
            if (be instanceof IExplosionPowerOverride override) {
                explosionPower = override.getExplosionPower(cableTier.getTierIndex(), explosionPower);
            }

            if (explosionPower > 0.0F) {
                level.explode(
                    null,
                    pos.getX() + 0.5,
                    pos.getY() + 0.5,
                    pos.getZ() + 0.5,
                    explosionPower,
                    Level.ExplosionInteraction.BLOCK
                );
            }

            level.removeBlock(pos, false);
        }
    }

    protected float calculateExplosionPower() {
        return (float) Math.min(cableTier.powerRating / 1024.0D, 2.0D + cableTier.tierIndex * 0.5D);
    }

    /**
     * 检查实现 {@link IExplosionPowerOverride} 的方块是否应爆炸。
     * 未实现该接口时默认返回 true。
     */
    public static boolean shouldTileExplode(BlockEntity tile) {
        return !(tile instanceof IExplosionPowerOverride override) || override.shouldExplode();
    }

    /**
     * 解析实现 {@link IExplosionPowerOverride} 的方块的实际爆炸威力。
     * 未实现该接口时返回 basePower。
     */
    public static float resolveTileExplosionPower(BlockEntity tile, int tier, float basePower) {
        if (tile instanceof IExplosionPowerOverride override) {
            return override.getExplosionPower(tier, basePower);
        }
        return basePower;
    }

    @Override
    public boolean canConnect(CableTier cableTier) {
        return cableTier.powerRating <= this.cableTier.powerRating;
    }

    @Override
    public long receive(long maxReceive, boolean simulate) {
        if (maxReceive <= 0) return 0;
        long energyReceived = Math.min(scexWholeRoom(), Math.min(this.maxReceive, maxReceive));
        if (!simulate) {
            scexSetWholePreservingFraction(energy + energyReceived);
        }
        return energyReceived;
    }

    @Override
    public long extract(long maxExtract, boolean simulate) {
        if (maxExtract <= 0) return 0;
        // 如果输出被禁用（红石控制），则不允许提取能量
        if (!this.outputEnabled) {
            return 0;
        }
        long energyExtracted = Math.min(energy, Math.min(this.maxExtract, maxExtract));
        if (!simulate) {
            scexSetWholePreservingFraction(energy - energyExtracted);
        }
        return energyExtracted;
    }

    @Override
    public long getAmount() {
        return energy;
    }

    @Override
    public long getCapacity() {
        // Escrow still occupies capacity; reentrant input cannot steal its refund room.
        return scexPayments.availableCapacity(capacity);
    }

    @Override
    public void setStored(long amount) {
        setEnergy(amount);
    }

    @Override
    public boolean canExtract() {
        return maxExtract > 0;
    }

    @Override
    public boolean canReceive() {
        return maxReceive > 0;
    }

    public void setEnergy(long energy) {
        scexRequireNoPayment("Absolute balance replacement");
        long updated = Math.max(0, scexNetworkControlled ? energy : Math.min(capacity, energy));
        scexReplaceEnergy(EnergyAmount.of(updated));
    }

    public final EnergyAmount scexExactAmount() {
        return scexViewedStorage == null ? new EnergyAmount(energy, scexFraction) : scexViewedStorage.scexExactAmount();
    }
    public final long scexSavedFraction() {
        if (scexViewedStorage != null) return scexViewedStorage.scexSavedFraction();
        scexRequireNoPayment("Save"); return scexFraction;
    }
    /** Missing legacy tags mean zero; malformed fractional tags cannot create EU. */
    public final void scexLoadFraction(long fraction) {
        scexRequireNoPayment("Fraction load");
        scexReplaceEnergy(new EnergyAmount(energy,
            fraction >= 0 && fraction < EnergyAmount.UNITS ? fraction : 0));
    }
    private long scexWholeRoom() {
        return scexExactAmount().roomBelow(getCapacity()).whole();
    }
    private void scexSetWholePreservingFraction(long whole) {
        scexReplaceEnergy(new EnergyAmount(whole, scexFraction));
    }
    public final EnergyAmount scexGenerateEnergy(EnergyAmount amount, boolean simulate) {
        scexRequireOwnedStorage();
        if (!scexNetworkControlled) throw new IllegalStateException("Fractional generation requires independent ownership");
        var generated = amount.min(scexExactAmount().roomBelow(getCapacity()));
        if (!simulate && !generated.isZero()) scexReplaceEnergy(scexExactAmount().add(generated));
        return generated;
    }
    /** Exact owned machine-fuel debit; no external callback is performed between quotation and commit. */
    public final EnergyAmount scexConsumeEnergy(EnergyAmount amount, boolean simulate) {
        scexRequireOwnedStorage();
        if (!scexNetworkControlled) throw new IllegalStateException("Fractional consumption requires independent ownership");
        var paid = amount.min(scexExactAmount());
        if (!simulate && !paid.isZero()) scexReplaceEnergy(scexExactAmount().subtract(paid));
        return paid;
    }
    /** Exact standard FE adjustment; the bridge owns admission, rate and sided checks. */
    public final void scexTransferFe(int fe, boolean receive) {
        scexRequireOwnedStorage();
        var value = com.miophas.singularity_iteration.core.runtime.energy.FeLedger.eu(fe);
        if (receive && scexPayments.reserved() != 0 && value.compareTo(scexExactAmount().roomBelow(getCapacity())) > 0)
            throw new IllegalStateException("FE input would consume internal payment escrow");
        scexReplaceEnergy(receive ? scexExactAmount().add(value) : scexExactAmount().subtract(value));
    }
    private void scexReplaceEnergy(EnergyAmount updated) {
        scexRequireOwnedStorage();
        scexPayments.checkPendingThread();
        if (updated.whole() > Long.MAX_VALUE - scexPayments.reserved())
            throw new IllegalArgumentException("Balance and internal escrow exceed representable EU");
        if (updated.whole() == energy && updated.fraction() == scexFraction) return;
        if (scexCell != null) scexCell.replace(updated);
        this.energy = updated.whole(); this.scexFraction = updated.fraction();
        // Normal chunk saves skip clean chunks. Energy changes must persist even
        // between completed operations, without serializing NBT or notifying all
        // comparators every tick. Never load a chunk merely to mark it dirty.
        if (level instanceof ServerLevel serverLevel && pos != null) {
            var chunk = serverLevel.getChunkSource().getChunkNow(pos.getX() >> 4, pos.getZ() >> 4);
            if (chunk != null) chunk.setUnsaved(true);
        }
    }

    public long getMaxExtract() {
        return maxExtract;
    }

    public long getMaxReceive() {
        return maxReceive;
    }

    public void setCapacity(long capacity) {
        scexPayments.checkPendingThread();
        this.capacity = Math.max(0, capacity);
        // 对齐 IC2：容量收缩（取下储能/超频升级后 recalculateUpgradeStats 会缩小容量）不删除已存的 EU，
        // 机器允许短期处于 energy > capacity。IC2 的 TileEntityElectricMachine 读档、injectEnergy、
        // getDemandedEnergy 都不夹取存量，只在 GUI 侧把比例按 1 夹住（guiChargeLevel = min(1, e/max)）。
        scexSetWholePreservingFraction(this.energy);
    }

    public void setMaxReceive(long maxReceive) {
        this.maxReceive = Math.max(0, maxReceive);
    }

    public void setMaxExtract(long maxExtract) {
        this.maxExtract = Math.max(0, maxExtract);
    }

    public CableTier getCableTier() {
        return cableTier;
    }

    public long consumeEnergyInternal(long amount, boolean simulate) {
        if (amount <= 0) {
            return 0;
        }
        long energyConsumed = Math.min(this.energy, amount);
        if (!simulate) {
            scexSetWholePreservingFraction(this.energy - energyConsumed);
        }
        return energyConsumed;
    }

    public long generateEnergyInternal(long amount, boolean simulate) {
        if (amount <= 0) {
            return 0;
        }
        long energyGenerated = Math.min(scexWholeRoom(), amount);
        if (!simulate) {
            scexSetWholePreservingFraction(this.energy + energyGenerated);
        }
        return energyGenerated;
    }

    @Override
    public long useEnergy(long amount, boolean simulate) {
        return consumeEnergyInternal(amount, simulate);
    }

    @Override
    public long generateEnergy(long amount, boolean simulate) {
        return generateEnergyInternal(amount, simulate);
    }
}
