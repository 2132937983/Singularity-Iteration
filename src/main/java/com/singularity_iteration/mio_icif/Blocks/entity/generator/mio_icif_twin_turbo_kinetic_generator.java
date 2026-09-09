package com.singularity_iteration.mio_icif.Blocks.entity.generator;

import com.singularity_iteration.mio_icif.Blocks.entity.mio_icif_Energy_Block;
import com.singularity_iteration.mio_icif.Blocks.entity.mio_icif_Energy_Generator;
import com.singularity_iteration.mio_icif.Blocks.entity.mio_icif_block_entities;
import com.singularity_iteration.mio_icif.Blocks.entity.slot.SlotLayout;
import com.singularity_iteration.mio_icif.api.MioIcifAPI;
import com.singularity_iteration.mio_icif.api.energy.ICableTier;
import com.singularity_iteration.mio_icif.api.kinetic.IKineticAPI;
import com.singularity_iteration.mio_icif.energy.EnergyUnit.CableTier;
import com.singularity_iteration.mio_icif.energy.grid.*;
import com.singularity_iteration.mio_icif.energy.kinetic.IKineticStorage;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

@SuppressWarnings("null")
public class mio_icif_twin_turbo_kinetic_generator extends mio_icif_Energy_Generator {

    private static final long ENERGY_CAPACITY = 400000L;
    private static final long MAX_RECEIVE = 0L;
    private static final long MAX_EXTRACT = 2048L;
    private static final long ENERGY_GENERATION_RATE = 0L;

    private static final double EU_PER_KU = 1.2D;

    private static final double EFFECTIVE_PERCENT_NATURAL = 1.0D;
    private static final double EFFECTIVE_PERCENT_ARTIFICIAL = 0.1D;

    private final KineticReceiverStorage kineticStorage;

    private long lastEnergyOutput = 0L;
    private double lastProduction = 0.0D;
    private int lastKuInputRate = 0;
    private double effectivePercent = 1.0D;

    private static ICableTier cachedCableTier = null;

    public static ICableTier getOrCreateTier() {
        if (cachedCableTier == null) {
            cachedCableTier = MioIcifAPI.instance().getEnergyNetAPI().getCableTier("ev");
        }
        return cachedCableTier;
    }

    public mio_icif_twin_turbo_kinetic_generator(BlockPos pos, BlockState state) {
        this(pos, state, null);
    }

    public mio_icif_twin_turbo_kinetic_generator(BlockPos pos, BlockState state, BlockEntityType<?> type) {
        super(pos, state, type != null ? type : mio_icif_block_entities.TWIN_TURBO_KINETIC_GENERATOR_ENTITY_TYPE.get(),
              SlotLayout.builder().extra(2).build(), ENERGY_GENERATION_RATE, ENERGY_CAPACITY, MAX_RECEIVE, MAX_EXTRACT, CableTier.EV);
        this.kineticStorage = new KineticReceiverStorage(40000, 4000);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, mio_icif_twin_turbo_kinetic_generator blockEntity) {
        if (level.isClientSide()) {
            return;
        }

        mio_icif_Energy_Block.tick(level, pos, state, blockEntity);

        blockEntity.lastEnergyOutput = 0;
        blockEntity.lastProduction = 0;
        blockEntity.lastKuInputRate = (int) blockEntity.kineticStorage.getAndResetReceivedThisTick();

        blockEntity.updateEffectivePercent(level, pos, state);

        long euBefore = blockEntity.getEnergyStorage().getAmount();
        blockEntity.convertKineticToEnergy();
        long euAfter = blockEntity.getEnergyStorage().getAmount();
        blockEntity.lastProduction = euAfter - euBefore;

        blockEntity.chargeItems();

        boolean shouldBeActive = blockEntity.isBurning();
        boolean isActive = state.getValue(com.singularity_iteration.mio_icif.Blocks.generator.mio_icif_Block_twin_turbo_kinetic_generator.ACTIVE);
        if (shouldBeActive != isActive) {
            level.setBlock(pos, state.setValue(com.singularity_iteration.mio_icif.Blocks.generator.mio_icif_Block_twin_turbo_kinetic_generator.ACTIVE, shouldBeActive), 3);
        }

        blockEntity.setChanged();
    }

    private void updateEffectivePercent(Level level, BlockPos pos, BlockState state) {
        Direction facing = state.getValue(com.singularity_iteration.mio_icif.Blocks.mio_icif_entity_block.FACING);
        BlockPos adjacentPos = pos.relative(facing);

        IKineticAPI kineticAPI = MioIcifAPI.instance().getKineticAPI();
        if (!kineticAPI.hasKineticTile(level, adjacentPos)) {
            effectivePercent = 1.0D;
            return;
        }

        net.minecraft.world.level.block.entity.BlockEntity adjacentBe = level.getBlockEntity(adjacentPos);
        if (adjacentBe == null) {
            effectivePercent = 1.0D;
            return;
        }

        if (isNaturalKineticSource(adjacentBe)) {
            effectivePercent = EFFECTIVE_PERCENT_NATURAL;
        } else {
            effectivePercent = EFFECTIVE_PERCENT_ARTIFICIAL;
        }
    }

    private boolean isNaturalKineticSource(net.minecraft.world.level.block.entity.BlockEntity be) {
        return be instanceof com.singularity_iteration.mio_icif.Blocks.entity.KUEntity.KUGenerator.mio_icif_Wind_Kinetic_Generator
            || be instanceof com.singularity_iteration.mio_icif.Blocks.entity.KUEntity.KUGenerator.mio_icif_Water_Kinetic_Generator;
    }

    @Override
    protected boolean shouldDirectlyDistributeEnergy() {
        return false;
    }

    private void convertKineticToEnergy() {
        long kineticStored = kineticStorage.getKineticStored();
        if (kineticStored <= 0) {
            return;
        }

        long energySpace = ENERGY_CAPACITY - getEnergyStorage().getAmount();
        if (energySpace <= 0) {
            return;
        }

        double actualEuPerKu = EU_PER_KU * effectivePercent;
        long maxEuFromKinetic = (long) (kineticStored * actualEuPerKu);
        long euToGenerate = Math.min(maxEuFromKinetic, energySpace);

        if (euToGenerate > 0) {
            long kuToConsume = (long) Math.ceil(euToGenerate / actualEuPerKu);
            kuToConsume = Math.min(kuToConsume, kineticStored);
            kineticStorage.extractKinetic(kuToConsume, false);

            apiGenerateEnergy(euToGenerate, false);
        }
    }

    public IKineticStorage getKineticStorage() {
        return kineticStorage;
    }

    public IKineticStorage getKineticStorageCapability(@Nullable Direction side) {
        if (side == null) {
            return null;
        }
        Direction facing = getBlockState().getValue(
            com.singularity_iteration.mio_icif.Blocks.mio_icif_entity_block.FACING);
        if (side == facing) {
            return kineticStorage;
        }
        return null;
    }

    @Override
    public int getFuelBurnTime(ItemStack fuel) {
        return 0;
    }

    @Override
    public boolean isBurning() {
        return kineticStorage.getKineticStored() > 0 || getEnergyStorage().getAmount() > 0;
    }

    public long getLastEnergyOutput() {
        return lastEnergyOutput;
    }

    public double getLastProduction() {
        return lastProduction;
    }

    public double getEffectivePercent() {
        return effectivePercent;
    }

    public double getEuPerKu() {
        return EU_PER_KU * effectivePercent;
    }

    @Override
    public long getPowerOutput() {
        return (long) Math.min(getEnergyStorage().getAmount(), EnergyNetGlobal.getPowerFromTier(getSourceTier()));
    }

    @Override
    public int getSourceTier() {
        double potentialEuPerTick = lastKuInputRate * EU_PER_KU * effectivePercent;
        return Math.max(EnergyNetGlobal.getTierFromPower(potentialEuPerTick), 4);
    }

    @Override
    public void drawEnergy(double amount) {
        if (amount > 0.0D) {
            apiUseEnergy((long) amount, false);
        }
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.mio_icif.twin_turbo_kinetic_generator");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new com.singularity_iteration.mio_icif.Menu.Generator.TwinTurboKineticGeneratorMenu(containerId, playerInventory, this, this.getItemHandler(), null);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        kineticStorage.saveToNBT(tag);
        tag.putDouble("EffectivePercent", effectivePercent);
    }

    @Override
    public void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        kineticStorage.loadFromNBT(tag);
        effectivePercent = tag.contains("EffectivePercent") ? tag.getDouble("EffectivePercent") : 1.0D;
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        kineticStorage.saveToNBT(tag);
        tag.putDouble("EffectivePercent", effectivePercent);
        return tag;
    }

    @Override
    public void handleUpdateTag(CompoundTag tag, HolderLookup.Provider registries) {
        super.handleUpdateTag(tag, registries);
        kineticStorage.loadFromNBT(tag);
        effectivePercent = tag.contains("EffectivePercent") ? tag.getDouble("EffectivePercent") : 1.0D;
    }

    private class KineticReceiverStorage implements IKineticStorage {
        private long kineticStored = 0;
        private final long maxKinetic;
        private final long maxReceive;
        private long receivedThisTick = 0;

        public KineticReceiverStorage(long maxKinetic, long maxReceive) {
            this.maxKinetic = maxKinetic;
            this.maxReceive = maxReceive;
        }

        @Override
        public long receiveKinetic(long amount, boolean simulate) {
            long space = maxKinetic - kineticStored;
            long toReceive = Math.min(amount, Math.min(space, maxReceive));
            if (!simulate) {
                kineticStored += toReceive;
                receivedThisTick += toReceive;
                mio_icif_twin_turbo_kinetic_generator.this.setChanged();
            }
            return toReceive;
        }

        public long getAndResetReceivedThisTick() {
            long received = receivedThisTick;
            receivedThisTick = 0;
            return received;
        }

        @Override
        public long extractKinetic(long amount, boolean simulate) {
            long toExtract = Math.min(amount, kineticStored);
            if (!simulate) {
                kineticStored -= toExtract;
                mio_icif_twin_turbo_kinetic_generator.this.setChanged();
            }
            return toExtract;
        }

        @Override
        public long getKineticStored() {
            return kineticStored;
        }

        @Override
        public long getMaxKineticStored() {
            return maxKinetic;
        }

        @Override
        public boolean canReceiveKinetic() {
            return kineticStored < maxKinetic;
        }

        @Override
        public boolean canExtractKinetic() {
            return false;
        }

        @Override
        public long getMaxReceive() {
            return maxReceive;
        }

        @Override
        public long getMaxExtract() {
            return 0;
        }

        @Override
        public int getRPM() {
            return 0;
        }

        public void saveToNBT(CompoundTag tag) {
            tag.putLong("KineticStored", kineticStored);
        }

        public void loadFromNBT(CompoundTag tag) {
            kineticStored = tag.getLong("KineticStored");
        }
    }
}