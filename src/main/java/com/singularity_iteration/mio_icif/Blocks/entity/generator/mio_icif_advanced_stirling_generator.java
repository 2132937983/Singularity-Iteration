package com.singularity_iteration.mio_icif.Blocks.entity.generator;

import com.singularity_iteration.mio_icif.energy.CustomEUEnergyStorage;
import com.singularity_iteration.mio_icif.Blocks.entity.HUEntity.mio_icif_HeatU_Block;
import com.singularity_iteration.mio_icif.Blocks.entity.mio_icif_block_entities;
import com.singularity_iteration.mio_icif.energy.EnergyUnit.CableTier;
import com.singularity_iteration.mio_icif.energy.EnergyUnit.IEUEnergyStorage;
import com.singularity_iteration.mio_icif.energy.grid.*;
import com.singularity_iteration.mio_icif.api.capability.IMioIcifCapabilities;
import com.singularity_iteration.mio_icif.api.energy.ICableTier;
import com.singularity_iteration.mio_icif.api.machine.IGeneratorBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.items.IItemHandler;
import org.jetbrains.annotations.Nullable;

@SuppressWarnings("null")
public class mio_icif_advanced_stirling_generator extends mio_icif_HeatU_Block implements IEnergySource, IGeneratorBlock {

    private boolean registered = false;

    private final CustomEUEnergyStorage energyStorage;

    private static final int HEAT_CAPACITY = 100000;
    private static final int MAX_HEAT_RECEIVE = 2000;
    private static final int MAX_HEAT_EXTRACT = 2000;
    private static final int BASE_TEMP = 20;
    private static final int MAX_TEMP = 2000;
    private static final float LOSS_FACTOR = 0.005f;

    private static final long ENERGY_CAPACITY = 1000000;
    private static final long MAX_ENERGY_EXTRACT = 128;
    private static final long MAX_ENERGY_RECEIVE = 0;

    private static final int HU_PER_CONVERSION = 100;
    private static final int EU_PER_CONVERSION = 85;

    private long heatBuffer = 0;
    private boolean isWorking = false;

    private static final CableTier CABLE_TIER = CableTier.MV;

    public mio_icif_advanced_stirling_generator(BlockPos pos, BlockState state) {
        super(mio_icif_block_entities.ADVANCED_STIRLING_GENERATOR_ENTITY_TYPE.get(), pos, state, HEAT_CAPACITY, MAX_HEAT_RECEIVE, MAX_HEAT_EXTRACT,
                BASE_TEMP, MAX_TEMP, LOSS_FACTOR);

        this.energyStorage = new CustomEUEnergyStorage(ENERGY_CAPACITY, MAX_ENERGY_RECEIVE, MAX_ENERGY_EXTRACT, CABLE_TIER);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, mio_icif_advanced_stirling_generator blockEntity) {
        if (level.isClientSide()) {
            return;
        }

        mio_icif_HeatU_Block.tick(level, pos, state, blockEntity);

        blockEntity.convertHeatToEnergy();

        boolean wasWorking = blockEntity.isWorking;
        blockEntity.isWorking = blockEntity.heatBuffer >= HU_PER_CONVERSION ||
                blockEntity.heatStorage.getHeatStored() >= HU_PER_CONVERSION;

        if (wasWorking != blockEntity.isWorking) {
            blockEntity.updateBlockState(blockEntity.isWorking);
        }
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

    private void convertHeatToEnergy() {
        long heatInStorage = heatStorage.getHeatStored();
        long maxExtract = heatStorage.getMaxExtract();

        if (heatInStorage > 0) {
            long heatToBuffer = Math.min(heatInStorage, maxExtract);
            if (heatToBuffer > 0) {
                long extracted = heatStorage.extractHeat(heatToBuffer, false);
                heatBuffer += extracted;
                setChanged();
            }
        }

        if (heatBuffer >= HU_PER_CONVERSION) {
            long energySpace = energyStorage.getCapacity() - energyStorage.getAmount();
            if (energySpace >= EU_PER_CONVERSION) {
                long conversions = Math.min(heatBuffer / HU_PER_CONVERSION, energySpace / EU_PER_CONVERSION);
                long heatToConvert = conversions * HU_PER_CONVERSION;
                long energyToGenerate = conversions * EU_PER_CONVERSION;

                heatBuffer -= heatToConvert;
                energyStorage.generateEnergyInternal(energyToGenerate, false);
                setChanged();
            }
        }
    }

    private void updateBlockState(boolean working) {
        if (level == null || level.isClientSide()) {
            return;
        }

        BlockState currentState = level.getBlockState(worldPosition);
        Boolean currentActive = currentState.getValue(
                com.singularity_iteration.mio_icif.Blocks.generator.mio_icif_block_advanced_stirling_generator.ACTIVE);
        if (currentActive != working) {
            level.setBlock(worldPosition, currentState.setValue(
                com.singularity_iteration.mio_icif.Blocks.generator.mio_icif_block_advanced_stirling_generator.ACTIVE,
                working), 3);
        }
    }

    public CustomEUEnergyStorage getEnergyStorage() {
        return energyStorage;
    }

    public IEUEnergyStorage getEnergyStorageCapability(@Nullable Direction side) {
        return energyStorage;
    }

    public IMioIcifCapabilities.IHeatStorage getHeatStorage() {
        return heatStorage;
    }

    public long getEnergyOutput() {
        if (!isWorking) {
            return 0;
        }
        long availableHeat = heatBuffer + heatStorage.getHeatStored();
        long conversions = availableHeat / HU_PER_CONVERSION;
        return conversions * EU_PER_CONVERSION;
    }

    @Override
    public double getOfferedEnergy() {
        long available = Math.min(energyStorage.getAmount(), energyStorage.getMaxExtract());
        return Math.min(available, CABLE_TIER.powerRating);
    }

    @Override
    public void drawEnergy(double amount) {
        if (amount > 0.0D) {
            long request = Math.min((long) amount, CABLE_TIER.powerRating);
            energyStorage.extract(request, false);
        }
    }

    @Override
    public int getSourceTier() {
        return EnergyNetGlobal.cableTierToSourceTier(CABLE_TIER);
    }

    public long extractPowerForConsumer(long amount, boolean simulate) {
        long request = Math.min(amount, CABLE_TIER.powerRating);
        return energyStorage.extract(request, simulate);
    }

    @Override
    public boolean emitsEnergyTo(IEnergyAcceptor acceptor, Direction direction) {
        Direction facing = getBlockState().getValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.FACING);
        Direction front = facing.getOpposite();
        return direction != front;
    }

    @Override
    protected void distributeHeat() {
    }

    @Override
    public IMioIcifCapabilities.IHeatStorage getHeatStorageCapability(@Nullable Direction side) {
        return new HeatStorageWrapper(heatStorage, side);
    }

    public long getHeatBuffer() {
        return heatBuffer;
    }

    public long getEnergyStored() {
        return energyStorage.getAmount();
    }

    public long getEnergyCapacity() {
        return energyStorage.getCapacity();
    }

    public boolean isWorking() {
        return isWorking;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putLong("HeatBuffer", heatBuffer);
        tag.putLong("Energy", energyStorage.getAmount());
        tag.putBoolean("IsWorking", isWorking);
    }

    @Override
    public void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("HeatBuffer", net.minecraft.nbt.Tag.TAG_LONG)) {
            heatBuffer = tag.getLong("HeatBuffer");
        } else if (tag.contains("HeatBuffer", net.minecraft.nbt.Tag.TAG_INT)) {
            heatBuffer = tag.getInt("HeatBuffer");
        }
        if (tag.contains("Energy", net.minecraft.nbt.Tag.TAG_LONG)) {
            energyStorage.setEnergy(tag.getLong("Energy"));
        } else if (tag.contains("Energy", net.minecraft.nbt.Tag.TAG_INT)) {
            energyStorage.setEnergy(tag.getInt("Energy"));
        }
        if (tag.contains("IsWorking", net.minecraft.nbt.Tag.TAG_BYTE)) {
            isWorking = tag.getBoolean("IsWorking");
        }
    }

    @Override
    public net.minecraft.network.chat.Component getDisplayName() {
        return net.minecraft.network.chat.Component.translatable("container.mio_icif.advanced_stirling_generator");
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new com.singularity_iteration.mio_icif.Menu.Generator.AdvancedStirlingGeneratorMenu(containerId, playerInventory, this, this.getItemHandler(), null);
    }

    @Override
    public ICableTier getCableTier() {
        return CABLE_TIER;
    }

    @Override
    public boolean isBurning() {
        return isWorking;
    }

    @Override
    public int getBurnTime() {
        return 0;
    }

    @Override
    public int getMaxBurnTime() {
        return 0;
    }

    @Override
    public long getPowerOutput() {
        return (long) getOfferedEnergy();
    }

    @Override
    public ItemStack getFuelSlotItem() {
        return ItemStack.EMPTY;
    }

    @Override
    public ItemStack getChargeSlotItem() {
        return ItemStack.EMPTY;
    }

    @Override
    public IItemHandler getItemHandler() {
        return null;
    }

    @Override
    public int getDefaultBurnTime() {
        return 0;
    }

    @Override
    public void setBurnTime(int ticks) {
    }

    @SuppressWarnings("unused")
    private class HeatStorageWrapper implements IMioIcifCapabilities.IHeatStorage {
        private final IMioIcifCapabilities.IHeatStorage internal;
        private final Direction side;

        public HeatStorageWrapper(IMioIcifCapabilities.IHeatStorage internal, Direction side) {
            this.internal = internal;
            this.side = side;
        }

        @Override
        public long receiveHeat(long maxReceive, boolean simulate) {
            return internal.receiveHeat(maxReceive, simulate);
        }

        @Override
        public long extractHeat(long maxExtract, boolean simulate) {
            return 0;
        }

        @Override
        public long getHeatStored() {
            return internal.getHeatStored();
        }

        @Override
        public long getMaxHeatStored() {
            return internal.getMaxHeatStored();
        }

        @Override
        public boolean canReceiveHeat() {
            return internal.canReceiveHeat() && energyStorage.getAmount() < energyStorage.getCapacity();
        }

        @Override
        public boolean canExtractHeat() {
            return false;
        }

        @Override
        public int getTemperature() {
            return internal.getTemperature();
        }

        @Override
        public boolean isOverheated() {
            return internal.isOverheated();
        }

        @Override
        public long getHeatLossPerTick() {
            return internal.getHeatLossPerTick();
        }

        @Override
        public long getMaxReceive() {
            return internal.getMaxReceive();
        }

        @Override
        public long getMaxExtract() {
            return 0;
        }
    }
}