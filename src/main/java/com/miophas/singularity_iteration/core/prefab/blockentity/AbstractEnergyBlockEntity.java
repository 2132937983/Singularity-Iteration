package com.miophas.singularity_iteration.core.prefab.blockentity;



import com.miophas.singularity_iteration.core.api.energy.ICableTier;
import com.miophas.singularity_iteration.core.api.energy.IEnergyStorageAccess;
import com.miophas.singularity_iteration.core.api.upgrade.tile.IUpgradableBlock;
import com.miophas.singularity_iteration.core.api.upgrade.tile.UpgradableProperty;
import com.miophas.singularity_iteration.core.runtime.energy.CustomEUEnergyStorage;
import com.miophas.singularity_iteration.core.api.energy.storage.CableTier;
import com.miophas.singularity_iteration.core.api.energy.grid.*;
import com.miophas.singularity_iteration.core.runtime.energy.grid.*;
import com.miophas.singularity_iteration.core.api.energy.IEnergyTileAccess;
import com.miophas.singularity_iteration.core.api.machine.IMachineAPI;
import com.miophas.singularity_iteration.core.api.tool.IWrenchable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.NeoForge;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.Set;

@SuppressWarnings("null")
public abstract class AbstractEnergyBlockEntity extends BlockEntity implements MenuProvider,
        IEnergySource, IEnergySink,
        com.miophas.singularity_iteration.core.api.machine.IEnergyBlock,
        IUpgradableBlock, IWrenchable, com.miophas.singularity_iteration.core.api.energy.IRemoteSwitchable {
    
    /** Legacy implementation field. Addons should use getEnergyStorage() and the protected api* operations. */
    @org.jetbrains.annotations.ApiStatus.Internal
    protected final CustomEUEnergyStorage energyStorage;
    private com.miophas.singularity_iteration.core.runtime.energy.FeMachineBridge scexFe;
    private CustomEUEnergyStorage[] scexEuPorts;
    // The wrapper is a live view over this block entity; reuse it on hot API paths
    // instead of allocating a new adapter for every upgrade/energy query.
    private IEnergyTileAccess energyApi;
    @org.jetbrains.annotations.ApiStatus.Internal
    public final com.miophas.singularity_iteration.core.runtime.energy.FeMachineBridge scexFeBridge() {
        if (scexFe == null) scexFe = new com.miophas.singularity_iteration.core.runtime.energy.FeMachineBridge(this);
        return scexFe;
    }
    public net.neoforged.neoforge.energy.IEnergyStorage scexFeCapability(@Nullable Direction side) {
        // Conductors, transformers and converter/multiblock proxy ports retain their own interfaces.
        if (!(this instanceof AbstractProcessingMachineBlockEntity) && !(this instanceof AbstractEnergyStorageBlockEntity)
                && !supportsFeCapability()) return null;
        return scexFeBridge().port(side);
    }
    
    protected boolean supportsFeCapability() { return false; }

    protected boolean isPowerSource = false;
    protected long powerOutput = 0;
    /** Cut from the grid by a remote switch (energy management terminal). */
    private boolean remoteDisabled;

    @Override
    public boolean isRemotelyDisabled() { return remoteDisabled; }

    @Override
    public void setRemotelyDisabled(boolean disabled) {
        if (remoteDisabled == disabled) return;
        remoteDisabled = disabled;
        setChanged();
        // Ports change: revoke routes now and republish the endpoint's faces.
        com.miophas.singularity_iteration.core.runtime.energy.IndependentSiEnergy.conductorPortsChanged(this);
        com.miophas.singularity_iteration.core.runtime.energy.IndependentSiEnergy.changed(this);
    }
    
    protected boolean registered = false;

    public AbstractEnergyBlockEntity(BlockPos pos, BlockState state, BlockEntityType<?> type) {
        this(pos, state, type, 10000, 100, 100, CableTier.LV);
    }
    
    public AbstractEnergyBlockEntity(BlockPos pos, BlockState state, BlockEntityType<?> type, 
                                 long capacity, long maxReceive, long maxExtract, CableTier cableTier) {
        super(type, pos, state);
        this.energyStorage = new CustomEUEnergyStorage(capacity, maxReceive, maxExtract, cableTier);
        this.energyStorage.scexSetNetworkControlled(com.miophas.singularity_iteration.core.runtime.energy.IndependentSiEnergy.controls(state));
    }

    public AbstractEnergyBlockEntity(BlockPos pos, BlockState state, BlockEntityType<?> type,
                                 long capacity, long maxReceive, long maxExtract, ICableTier cableTier) {
        super(type, pos, state);
        this.energyStorage = new CustomEUEnergyStorage(capacity, maxReceive, maxExtract, CableTier.fromICableTier(cableTier));
        this.energyStorage.scexSetNetworkControlled(com.miophas.singularity_iteration.core.runtime.energy.IndependentSiEnergy.controls(state));
    }
    
    @Override
    public void setLevel(Level level) {
        if (scexFe != null) scexFe.clear();
        if (energyStorage != null && this.level != level && energyStorage.scexNetworkControlled()) {
            energyStorage.scexRetireNetworkCell();
        }
        super.setLevel(level);
        if (energyStorage != null) {
            energyStorage.setBlockContext(level, worldPosition);
        }
    }

    // ------------------------------------------------------------------ status lamp
    /**
     * Server-side lifecycle listener of the status-lamp tracker (installed by the mod): called with
     * {@code true} when this machine loads and {@code false} when it is removed / unloaded.
     */
    @Nullable
    public static volatile java.util.function.BiConsumer<AbstractEnergyBlockEntity, Boolean> STATUS_LIFECYCLE;

    private com.miophas.singularity_iteration.core.api.machine.MachineStatus clientMachineStatus =
        com.miophas.singularity_iteration.core.api.machine.MachineStatus.OFF;

    /**
     * Server: the current lamp state. Default: green while the block's run flag (lit / active /
     * working / running) is set, dark otherwise. Machines and generators refine this.
     */
    public com.miophas.singularity_iteration.core.api.machine.MachineStatus machineStatus() {
        return runFlag() ? com.miophas.singularity_iteration.core.api.machine.MachineStatus.RUNNING
            : com.miophas.singularity_iteration.core.api.machine.MachineStatus.OFF;
    }

    /** True when the block state carries a set run flag (lit / active / working / running). */
    protected boolean runFlag() {
        BlockState state = getBlockState();
        for (var property : state.getProperties()) {
            if (property instanceof net.minecraft.world.level.block.state.properties.BooleanProperty flag) {
                String name = flag.getName();
                if ((name.equals("lit") || name.equals("active") || name.equals("working") || name.equals("running"))
                        && state.getValue(flag)) return true;
            }
        }
        return false;
    }

    /** Client: lamp state last received from the server. */
    public com.miophas.singularity_iteration.core.api.machine.MachineStatus clientMachineStatus() {
        return clientMachineStatus;
    }

    /** Client: applies a synced lamp state and re-meshes the block (only when it changed). */
    public void acceptClientMachineStatus(com.miophas.singularity_iteration.core.api.machine.MachineStatus status) {
        if (status == null || status == clientMachineStatus) return;
        clientMachineStatus = status;
        if (level != null && level.isClientSide) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 8);
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (level != null && !level.isClientSide && STATUS_LIFECYCLE != null) STATUS_LIFECYCLE.accept(this, true);
        boolean isClient = level != null && level.isClientSide;
        if (level != null && !isClient) {
            if (energyStorage.scexNetworkControlled()) {
                com.miophas.singularity_iteration.core.runtime.energy.IndependentSiEnergy.changed(this);
            } else if (!registered) {
                NeoForge.EVENT_BUS.post(new EnergyTileLoadEvent(this, level));
                registered = true;
            }
        }
    }
    
    @Override
    public void setRemoved() {
        if (level != null && !level.isClientSide && STATUS_LIFECYCLE != null) STATUS_LIFECYCLE.accept(this, false);
        if (level != null && !level.isClientSide && registered) {
            NeoForge.EVENT_BUS.post(new EnergyTileUnloadEvent(this, level));
            registered = false;
        }
        if (level != null && !level.isClientSide && energyStorage.scexNetworkControlled()) {
            com.miophas.singularity_iteration.core.runtime.energy.IndependentSiEnergy.changed(this);
            energyStorage.scexRetireNetworkCell();
        }
        if (scexFe != null) scexFe.clear();
        super.setRemoved();
    }
    
    @Override
    public void clearRemoved() {
        super.clearRemoved();
        boolean isClient = level != null && level.isClientSide;
        if (level != null && !isClient) {
            if (energyStorage.scexNetworkControlled()) {
                com.miophas.singularity_iteration.core.runtime.energy.IndependentSiEnergy.changed(this);
            } else if (!registered) {
                NeoForge.EVENT_BUS.post(new EnergyTileLoadEvent(this, level));
                registered = true;
            }
        }
    }

    public void refreshRegistration() {
        if (energyStorage.scexNetworkControlled()) { com.miophas.singularity_iteration.core.runtime.energy.IndependentSiEnergy.changed(this); return; }
        if (level != null && !level.isClientSide && registered) {
            NeoForge.EVENT_BUS.post(new EnergyTileUnloadEvent(this, level));
            NeoForge.EVENT_BUS.post(new EnergyTileLoadEvent(this, level));
        }
    }
    
    protected void setAsPowerSource(long powerOutput) {
        this.isPowerSource = true;
        this.powerOutput = powerOutput;
        this.energyStorage.setAsPowerSource(powerOutput);
    }
    
    protected void setAsConsumer() {
        this.isPowerSource = false;
        this.powerOutput = 0;
    }
    
    public boolean isPowerSource() {
        return isPowerSource;
    }
    
    @Override
    public IMachineAPI.MachineType getMachineType() {
        return IMachineAPI.MachineType.CUSTOM;
    }

    @Override
    public IEnergyStorageAccess getEnergyStorage() {
        return energyStorage;
    }
    
    /** @deprecated Engine-only escape hatch. Use getEnergyStorage() for machine work, euPort(side) for transfers. */
    @Deprecated(since = "0.1.7.14", forRemoval = false)
    @org.jetbrains.annotations.ApiStatus.Internal
    public CustomEUEnergyStorage getEnergyStorageInternal() {
        return energyStorage;
    }
    
    protected long apiGetStoredEnergy() {
        return energyStorage.getAmount();
    }
    
    protected long apiGetMaxEnergy() {
        return energyStorage.getCapacity();
    }
    
    protected void apiSetEnergy(long amount) {
        energyStorage.setEnergy(amount);
    }
    
    protected long apiReceiveEnergy(long amount, boolean simulate) {
        return energyStorage.receive(amount, simulate);
    }
    
    protected long apiExtractEnergy(long amount, boolean simulate) {
        return energyStorage.extract(amount, simulate);
    }

    protected long apiUseEnergy(long amount, boolean simulate) {
        return energyStorage.consumeEnergyInternal(amount, simulate);
    }

    protected long apiGenerateEnergy(long amount, boolean simulate) {
        return energyStorage.generateEnergyInternal(amount, simulate);
    }
    
    protected CableTier apiGetCableTier() {
        return energyStorage.getCableTier();
    }
    
    protected long apiGetMaxReceive() {
        return energyStorage.getMaxReceive();
    }
    
    protected long apiGetMaxExtract() {
        return energyStorage.getMaxExtract();
    }
    
    protected IEnergyTileAccess getEnergyAPI() {
        if (energyApi == null) {
            energyApi = new EnergyTileAccessWrapper();
        }
        return energyApi;
    }

    protected class EnergyTileAccessWrapper implements IEnergyTileAccess {
        @Override
        public Level getWorld() { return level; }
        
        @Override
        public BlockPos getPos() { return worldPosition; }
        
        @Override
        public long getStoredEnergy() { return apiGetStoredEnergy(); }
        
        @Override
        public long getMaxEnergy() { return apiGetMaxEnergy(); }
        
        @Override
        public long getOfferedEnergy() { return (long) AbstractEnergyBlockEntity.this.getOfferedEnergy(); }
        
        @Override
        public long getDemandedEnergy() { return (long) AbstractEnergyBlockEntity.this.getDemandedEnergy(); }
        
        @Override
        public int getSourceTier() { return AbstractEnergyBlockEntity.this.getSourceTier(); }
        
        @Override
        public int getSinkTier() { return AbstractEnergyBlockEntity.this.getSinkTier(); }
        
        @Override
        public Optional<ICableTier> getCableTier() { return Optional.ofNullable(apiGetCableTier()); }
        
        @Override
        public boolean acceptsEnergyFrom(Direction direction) {
            return !isPowerSource;
        }
        
        @Override
        public boolean emitsEnergyTo(Direction direction) {
            return isPowerSource;
        }
        
        @Override
        public boolean isSource() { return isPowerSource; }
        
        @Override
        public boolean isSink() { return !isPowerSource; }
        
        @Override
        public boolean isConductor() { return false; }
        
        @Override
        public long chargeEnergy(long amount, boolean simulate) {
            return apiReceiveEnergy(amount, simulate);
        }
        
        @Override
        public long dischargeEnergy(long amount, boolean simulate) {
            return apiExtractEnergy(amount, simulate);
        }

        @Override
        public long useEnergy(long amount, boolean simulate) {
            return apiUseEnergy(amount, simulate);
        }

        @Override
        public long generateEnergy(long amount, boolean simulate) {
            return apiGenerateEnergy(amount, simulate);
        }
        
        @Override
        public boolean setEnergy(long amount) {
            apiSetEnergy(amount);
            return true;
        }
        
        @Override
        public boolean setCapacity(long capacity) {
            energyStorage.setCapacity(capacity);
            return true;
        }
        
        @Override
        public long getMaxReceive() { return apiGetMaxReceive(); }
        
        @Override
        public long getMaxExtract() { return apiGetMaxExtract(); }
    }

    public static void tick(Level level, BlockPos pos, BlockState state, AbstractEnergyBlockEntity blockEntity) {
        if (level.isClientSide()) return;
    }
    
    public long getPowerOutput() {
        return isPowerSource ? powerOutput : 0;
    }

    @Override
    public ICableTier getEffectiveCableTier() {
        return (ICableTier) apiGetCableTier();
    }

    public long getEffectiveCapacity() {
        return apiGetMaxEnergy();
    }

    public long getEffectiveMaxReceive() {
        return apiGetMaxReceive();
    }
    
    public void triggerOverloadExplosion() {
        energyStorage.triggerOverloadExplosion();
    }
    
    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putLong("energy", apiGetStoredEnergy());
        tag.putLong("scex_energy_fraction", energyStorage.scexSavedFraction());
        if (scexFe != null) tag.putInt("scex_fe_uncertain_output", scexFe.uncertainOutput());
        tag.putString("cable_tier", apiGetCableTier().name);
        tag.putBoolean("is_power_source", isPowerSource);
        tag.putLong("power_output", powerOutput);
        if (remoteDisabled) tag.putBoolean("remote_disabled", true);
    }
    
    @Override
    public void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("energy", net.minecraft.nbt.Tag.TAG_LONG)) {
            apiSetEnergy(tag.getLong("energy"));
        } else if (tag.contains("energy", net.minecraft.nbt.Tag.TAG_INT)) {
            apiSetEnergy(tag.getInt("energy"));
        }
        energyStorage.scexLoadFraction(tag.getLong("scex_energy_fraction"));
        remoteDisabled = tag.getBoolean("remote_disabled");
        int uncertainFe = tag.getInt("scex_fe_uncertain_output");
        if (uncertainFe != 0 || scexFe != null) scexFeBridge().loadUncertainOutput(uncertainFe);
        if (tag.contains("is_power_source")) {
            isPowerSource = tag.getBoolean("is_power_source");
        }
        if (tag.contains("power_output", net.minecraft.nbt.Tag.TAG_LONG)) {
            powerOutput = tag.getLong("power_output");
        }
    }
    
    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        tag.putLong("energy", apiGetStoredEnergy());
        if (energyStorage.scexNetworkControlled()) tag.putLong("scex_energy_fraction", energyStorage.scexSavedFraction());
        tag.putString("cable_tier", apiGetCableTier().name);
        tag.putBoolean("is_power_source", isPowerSource);
        tag.putLong("power_output", powerOutput);
        return tag;
    }

    @Override
    public void handleUpdateTag(CompoundTag tag, HolderLookup.Provider registries) {
        super.handleUpdateTag(tag, registries);
        if (tag.contains("energy", net.minecraft.nbt.Tag.TAG_LONG)) {
            apiSetEnergy(tag.getLong("energy"));
        }
        energyStorage.scexLoadFraction(tag.getLong("scex_energy_fraction"));
        if (tag.contains("is_power_source")) {
            isPowerSource = tag.getBoolean("is_power_source");
        }
        if (tag.contains("power_output", net.minecraft.nbt.Tag.TAG_LONG)) {
            powerOutput = tag.getLong("power_output");
        }
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.mio_icif.energy_block");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int id, Inventory playerInventory, Player player) {
        return null;
    }

    /** Public transfer port sharing direction checks and the native/FE ledger. */
    public com.miophas.singularity_iteration.core.api.energy.storage.IEUEnergyStorage euPort(@Nullable Direction side) {
        return getEnergyStorageCapability(side);
    }

    /** @deprecated Historical JVM descriptor; addons should use euPort(side). */
    @Deprecated(since = "0.1.7.14", forRemoval = false)
    @org.jetbrains.annotations.ApiStatus.Internal
    public CustomEUEnergyStorage getEnergyStorageCapability(@Nullable Direction side) {
        // Keep the historical JVM descriptor without exposing the controlled
        // balance directly. All external EU faces share the native/item/FE ledger.
        if (!energyStorage.scexNetworkControlled()) return energyStorage;
        if (scexEuPorts == null) scexEuPorts = new CustomEUEnergyStorage[7];
        int index = side == null ? 6 : side.ordinal();
        if (scexEuPorts[index] == null) scexEuPorts[index] = new LedgerEuPort(side);
        return scexEuPorts[index];
    }

    private final class LedgerEuPort extends CustomEUEnergyStorage {
        private final @Nullable Direction side;

        private LedgerEuPort(@Nullable Direction side) {
            super(AbstractEnergyBlockEntity.this.energyStorage);
            this.side = side;
        }

        private boolean live() {
            if (!energyStorage.scexNetworkControlled() || isRemoved()
                    || !(AbstractEnergyBlockEntity.this.level instanceof net.minecraft.server.level.ServerLevel world)
                    || !world.getServer().isSameThread()) return false;
            var pos = worldPosition;
            var chunk = world.getChunkSource().getChunkNow(pos.getX() >> 4, pos.getZ() >> 4);
            return chunk != null && world.shouldTickBlocksAt(net.minecraft.world.level.ChunkPos.asLong(pos))
                && chunk.getBlockEntity(pos, net.minecraft.world.level.chunk.LevelChunk.EntityCreationType.CHECK)
                    == AbstractEnergyBlockEntity.this;
        }

        private boolean accepts() {
            if (!live() || AbstractEnergyBlockEntity.this instanceof com.miophas.singularity_iteration.core.api.energy.grid.IEnergyConductor
                    || !AbstractEnergyBlockEntity.this.canConnect(side)) return false;
            if (AbstractEnergyBlockEntity.this instanceof AbstractEnergyStorageBlockEntity box)
                // Legacy wireless input queries the aggregate (null) EU port.
                return side == null || box.canConsumePowerFromSide(side);
            return !AbstractEnergyBlockEntity.this.isPowerSource();
        }

        private boolean emits() {
            if (!live() || AbstractEnergyBlockEntity.this instanceof com.miophas.singularity_iteration.core.api.energy.grid.IEnergyConductor
                    || !AbstractEnergyBlockEntity.this.canConnect(side)) return false;
            if (AbstractEnergyBlockEntity.this instanceof AbstractEnergyStorageBlockEntity box)
                return side == null || box.canProvidePowerFromSide(side);
            if (AbstractEnergyBlockEntity.this instanceof com.miophas.singularity_iteration.core.runtime.energy.DemandEnergySource demand
                    && (demand.outputFaces() & (side == null ? 63 : 1 << side.ordinal())) == 0) return false;
            return AbstractEnergyBlockEntity.this.isPowerSource();
        }

        @Override public long receive(long amount, boolean simulate) { return accepts() ? scexFeBridge().receiveWholeEu(amount, simulate) : 0; }
        @Override public long extract(long amount, boolean simulate) { return emits() ? scexFeBridge().extractWholeEu(amount, simulate) : 0; }
        @Override public boolean canReceive() { return accepts() && energyStorage.getMaxReceive() > 0; }
        @Override public boolean canExtract() { return emits() && energyStorage.isOutputEnabled() && energyStorage.getMaxExtract() > 0 && scexFeBridge().uncertainOutput() == 0; }
        @Override public boolean canConnect(CableTier tier) { return (accepts() || emits()) && energyStorage.canConnect(tier); }
        @Override public long getAmount() { return energyStorage.getAmount(); }
        @Override public long getCapacity() { return energyStorage.getCapacity(); }
        @Override public int getEnergyStored() { return energyStorage.getEnergyStored(); }
        @Override public int getMaxEnergyStored() { return energyStorage.getMaxEnergyStored(); }
        @Override public long getMaxReceive() { return accepts() ? energyStorage.getMaxReceive() : 0; }
        @Override public long getMaxExtract() { return emits() ? energyStorage.getMaxExtract() : 0; }
        @Override public long getPowerOutput() { return energyStorage.getPowerOutput(); }
        @Override public boolean isPowerSource() { return energyStorage.isPowerSource(); }
        @Override public boolean isOutputEnabled() { return energyStorage.isOutputEnabled(); }
        @Override public CableTier getCableTier() { return energyStorage.getCableTier(); }
        @Override public long getPowerRating() { return energyStorage.getPowerRating(); }
        @Override public boolean isOverloaded(long power) { return energyStorage.isOverloaded(power); }
        @Override public long extractPowerForConsumer(long amount, boolean simulate) { return extract(amount, simulate); }
        @Override public long useEnergy(long amount, boolean simulate) { return extract(amount, simulate); }
        @Override public long generateEnergy(long amount, boolean simulate) { return receive(amount, simulate); }
        @Override public long consumeEnergyInternal(long amount, boolean simulate) { return extract(amount, simulate); }
        @Override public long generateEnergyInternal(long amount, boolean simulate) { return receive(amount, simulate); }

        // Administrative setters are not transfer APIs. In particular, do not
        // let an old addon overwrite the real balance or create a second one.
        private UnsupportedOperationException readOnlyPolicy() {
            return new UnsupportedOperationException("Use receive/extract on the sided EU capability");
        }
        @Override public void setStored(long amount) { throw readOnlyPolicy(); }
        @Override public void setEnergy(long amount) { throw readOnlyPolicy(); }
        @Override public void setCapacity(long capacity) { throw readOnlyPolicy(); }
        @Override public void setMaxReceive(long amount) { throw readOnlyPolicy(); }
        @Override public void setMaxExtract(long amount) { throw readOnlyPolicy(); }
        @Override public void setAsPowerSource(long output) { throw readOnlyPolicy(); }
        @Override public void setOutputEnabled(boolean enabled) { throw readOnlyPolicy(); }
        @Override public void setBlockContext(Level owner, BlockPos pos) { throw readOnlyPolicy(); }
        @Override public void triggerOverloadExplosion() { throw readOnlyPolicy(); }
    }

    /** Query the platform's existing ticking state without loading a chunk. */
    protected final boolean canTransferGridEnergy() {
        return !energyStorage.scexNetworkControlled() && !isRemoved() && level instanceof net.minecraft.server.level.ServerLevel serverLevel
                && serverLevel.shouldTickBlocksAt(net.minecraft.world.level.ChunkPos.asLong(worldPosition));
    }

    // ==================== grid.IEnergySource 接口实现 ====================

    @Override
    public double getOfferedEnergy() {
        if (!canTransferGridEnergy()) return 0.0D;
        if (!isPowerSource) return 0.0D;
        return energyStorage.getAmount();
    }

    @Override
    public void drawEnergy(double amount) {
        if (!canTransferGridEnergy()) return;
        if (isPowerSource && amount > 0.0D) {
            energyStorage.extract((long) amount, false);
        }
    }

    @Override
    public int getSourceTier() {
        if (!isPowerSource) return -1;
        return EnergyNetGlobal.cableTierToSourceTier((CableTier) getEffectiveCableTier());
    }

    // ==================== grid.IEnergySink 接口实现 ====================

    @Override
    public double getDemandedEnergy() {
        if (!canTransferGridEnergy()) return 0.0D;
        if (isPowerSource) return 0.0D;
        long spaceAvailable = getEffectiveCapacity() - energyStorage.getAmount();
        if (spaceAvailable <= 0) return 0.0D;
        return spaceAvailable;
    }

    @Override
    public int getSinkTier() {
        return EnergyNetGlobal.cableTierToSourceTier((CableTier) getEffectiveCableTier());
    }

    @Override
    public double injectEnergy(Direction direction, double amount, double voltage) {
        if (!canTransferGridEnergy()) return amount;
        if (isPowerSource) return amount;
        long accepted = energyStorage.generateEnergyInternal((long) amount, false);
        return amount - accepted;
    }

    // ==================== grid.IEnergyAcceptor / IEnergyEmitter ====================

    @Override
    public boolean acceptsEnergyFrom(IEnergyEmitter emitter, Direction direction) {
        return !energyStorage.scexNetworkControlled() && !isPowerSource;
    }

    @Override
    public boolean emitsEnergyTo(IEnergyAcceptor acceptor, Direction direction) {
        return !energyStorage.scexNetworkControlled() && isPowerSource;
    }

    // ==================== IUpgradableBlock 接口实现 ====================

    @Override
    public long getEnergy() {
        return apiGetStoredEnergy();
    }

    @Override
    public boolean useEnergy(long amount) {
        return apiUseEnergy(amount, false) >= amount;
    }

    @Override
    public Set<UpgradableProperty> getUpgradableProperties() {
        return Set.of(UpgradableProperty.values());
    }

    // ==================== IEnergyBlock 接口实现 ====================

    @Override
    public long useEnergy(long amount, boolean simulate) {
        return apiUseEnergy(amount, simulate);
    }

    @Override
    public long generateEnergy(long amount, boolean simulate) {
        return apiGenerateEnergy(amount, simulate);
    }

    @Override
    public boolean canConnect(@Nullable Direction side) {
        return true;
    }

    @Override
    public Level getLevel() {
        return level;
    }

    @Override
    public BlockPos getBlockPos() {
        return worldPosition;
    }

    @Override
    public net.minecraft.world.level.block.state.BlockState getBlockState() {
        return super.getBlockState();
    }
}
