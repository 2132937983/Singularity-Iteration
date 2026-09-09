package com.singularity_iteration.mio_icif.Blocks.entity;

import com.singularity_iteration.mio_icif.api.energy.ICableTier;
import com.singularity_iteration.mio_icif.api.energy.IEnergyStorageAccess;
import com.singularity_iteration.mio_icif.api.upgrade.tile.IUpgradableBlock;
import com.singularity_iteration.mio_icif.api.upgrade.tile.UpgradableProperty;
import com.singularity_iteration.mio_icif.energy.CustomEUEnergyStorage;
import com.singularity_iteration.mio_icif.energy.EnergyUnit.CableTier;
import com.singularity_iteration.mio_icif.energy.grid.*;
import com.singularity_iteration.mio_icif.api.energy.IEnergyTileAccess;
import com.singularity_iteration.mio_icif.api.machine.IMachineAPI;
import com.singularity_iteration.mio_icif.api.tool.IWrenchable;
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
public abstract class mio_icif_Energy_Block extends BlockEntity implements MenuProvider,
        IEnergySource, IEnergySink,
        com.singularity_iteration.mio_icif.api.machine.IEnergyBlock,
        IUpgradableBlock, IWrenchable {
    
    protected final CustomEUEnergyStorage energyStorage;
    
    protected boolean isPowerSource = false;
    protected long powerOutput = 0;
    
    protected boolean registered = false;

    public mio_icif_Energy_Block(BlockPos pos, BlockState state, BlockEntityType<?> type) {
        this(pos, state, type, 10000, 100, 100, CableTier.LV);
    }
    
    public mio_icif_Energy_Block(BlockPos pos, BlockState state, BlockEntityType<?> type, 
                                 long capacity, long maxReceive, long maxExtract, CableTier cableTier) {
        super(type, pos, state);
        this.energyStorage = new CustomEUEnergyStorage(capacity, maxReceive, maxExtract, cableTier);
    }

    public mio_icif_Energy_Block(BlockPos pos, BlockState state, BlockEntityType<?> type,
                                 long capacity, long maxReceive, long maxExtract, ICableTier cableTier) {
        super(type, pos, state);
        this.energyStorage = new CustomEUEnergyStorage(capacity, maxReceive, maxExtract, CableTier.fromICableTier(cableTier));
    }
    
    @Override
    public void setLevel(Level level) {
        super.setLevel(level);
        if (energyStorage != null) {
            energyStorage.setBlockContext(level, worldPosition);
        }
    }

    @Override
    public void onLoad() {
        super.onLoad();
        boolean isClient = level != null && level.isClientSide;
        if (level != null && !isClient) {
            if (!registered) {
                NeoForge.EVENT_BUS.post(new EnergyTileLoadEvent(this, level));
                registered = true;
            }
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
        boolean isClient = level != null && level.isClientSide;
        if (level == null) {
            System.out.println("[EnergyNet] WARNING: level is null in clearRemoved!");
        } else if (!isClient && !registered) {
            NeoForge.EVENT_BUS.post(new EnergyTileLoadEvent(this, level));
            registered = true;
        }
    }

    public void refreshRegistration() {
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
        return new EnergyTileAccessWrapper();
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
        public long getOfferedEnergy() { return (long) mio_icif_Energy_Block.this.getOfferedEnergy(); }
        
        @Override
        public long getDemandedEnergy() { return (long) mio_icif_Energy_Block.this.getDemandedEnergy(); }
        
        @Override
        public int getSourceTier() { return mio_icif_Energy_Block.this.getSourceTier(); }
        
        @Override
        public int getSinkTier() { return mio_icif_Energy_Block.this.getSinkTier(); }
        
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

    public static void tick(Level level, BlockPos pos, BlockState state, mio_icif_Energy_Block blockEntity) {
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
        tag.putString("cable_tier", apiGetCableTier().name);
        tag.putBoolean("is_power_source", isPowerSource);
        tag.putLong("power_output", powerOutput);
    }
    
    @Override
    public void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("energy", net.minecraft.nbt.Tag.TAG_LONG)) {
            apiSetEnergy(tag.getLong("energy"));
        } else if (tag.contains("energy", net.minecraft.nbt.Tag.TAG_INT)) {
            apiSetEnergy(tag.getInt("energy"));
        }
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

    public CustomEUEnergyStorage getEnergyStorageCapability(@Nullable Direction side) {
        return energyStorage;
    }

    // ==================== grid.IEnergySource 接口实现 ====================

    @Override
    public double getOfferedEnergy() {
        if (!isPowerSource) return 0.0D;
        return energyStorage.getAmount();
    }

    @Override
    public void drawEnergy(double amount) {
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
        if (isPowerSource) return amount;
        long toAdd = (long) amount;
        long spaceAvailable = getEffectiveCapacity() - energyStorage.getAmount();
        long accepted = Math.min(toAdd, spaceAvailable);
        energyStorage.setEnergy(energyStorage.getAmount() + accepted);
        return amount - accepted;
    }

    // ==================== grid.IEnergyAcceptor / IEnergyEmitter ====================

    @Override
    public boolean acceptsEnergyFrom(IEnergyEmitter emitter, Direction direction) {
        return !isPowerSource;
    }

    @Override
    public boolean emitsEnergyTo(IEnergyAcceptor acceptor, Direction direction) {
        return isPowerSource;
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