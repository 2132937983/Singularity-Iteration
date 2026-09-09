package com.singularity_iteration.mio_icif.Blocks.entity.generator;

import com.singularity_iteration.mio_icif.Blocks.entity.mio_icif_Energy_Block;
import com.singularity_iteration.mio_icif.Blocks.entity.mio_icif_block_entities;
import com.singularity_iteration.mio_icif.energy.EnergyUnit.CableTier;
import com.singularity_iteration.mio_icif.energy.grid.*;
import com.singularity_iteration.mio_icif.api.energy.ICableTier;
import com.singularity_iteration.mio_icif.api.machine.IGeneratorBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.items.IItemHandler;

import java.util.List;

@SuppressWarnings("null")
public class mio_icif_advanced_experience_generator extends mio_icif_Energy_Block implements IGeneratorBlock {

    private boolean registered = false;

    private static final long ENERGY_CAPACITY = 4000000L;
    private static final long MAX_ENERGY_RECEIVE = 0L;
    private static final long MAX_ENERGY_EXTRACT = 2048L;
    private static final CableTier CABLE_TIER = CableTier.EV;
    private static final long PRODUCTION = 320L;

    private static final int SCAN_RANGE = 7;
    private static final double CONSUME_DISTANCE_SQ = 1.0D;

    private int fuel = 0;
    private boolean isWorking = false;

    public mio_icif_advanced_experience_generator(BlockPos pos, BlockState state) {
        super(pos, state, mio_icif_block_entities.ADVANCED_EXPERIENCE_GENERATOR_ENTITY_TYPE.get(),
              ENERGY_CAPACITY, MAX_ENERGY_RECEIVE, MAX_ENERGY_EXTRACT, CABLE_TIER);
        setAsPowerSource(PRODUCTION);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, mio_icif_advanced_experience_generator blockEntity) {
        if (level.isClientSide()) {
            return;
        }

        blockEntity.scanAndConsumeOrbs(level, pos);

        boolean wasWorking = blockEntity.isWorking;
        blockEntity.isWorking = blockEntity.fuel > 0 &&
                blockEntity.energyStorage.getAmount() < blockEntity.energyStorage.getCapacity();

        if (blockEntity.fuel > 0 &&
                blockEntity.energyStorage.getAmount() < blockEntity.energyStorage.getCapacity()) {
            long energyGenerated = Math.min(PRODUCTION,
                    blockEntity.energyStorage.getCapacity() - blockEntity.energyStorage.getAmount());
            blockEntity.energyStorage.generateEnergyInternal(energyGenerated, false);
            blockEntity.fuel--;
        }

        if (wasWorking != blockEntity.isWorking) {
            blockEntity.updateBlockState(blockEntity.isWorking);
        }

        blockEntity.setChanged();
    }

    private void scanAndConsumeOrbs(Level level, BlockPos pos) {
        BlockPos minPos = pos.offset(-SCAN_RANGE, -SCAN_RANGE, -SCAN_RANGE);
        BlockPos maxPos = pos.offset(SCAN_RANGE, SCAN_RANGE, SCAN_RANGE);
        AABB bb = new AABB(minPos.getX(), minPos.getY(), minPos.getZ(), maxPos.getX() + 1, maxPos.getY() + 1, maxPos.getZ() + 1);

        List<ExperienceOrb> orbs = level.getEntitiesOfClass(ExperienceOrb.class, bb);

        for (ExperienceOrb orb : orbs) {
            double dist = pos.distToCenterSqr(orb.getX(), orb.getY(), orb.getZ());

            if (dist <= CONSUME_DISTANCE_SQ) {
                fuel += orb.getValue();
                orb.discard();
            } else {
                double d1 = (pos.getX() + 0.5D - orb.getX()) / 8.0D;
                double d2 = (pos.getY() + 0.5D - orb.getY()) / 8.0D;
                double d3 = (pos.getZ() + 0.5D - orb.getZ()) / 8.0D;
                double d4 = Math.sqrt(d1 * d1 + d2 * d2 + d3 * d3);
                double d5 = 1.0D - d4;

                if (d5 > 0.0D) {
                    d5 = d5 * d5;
                    orb.setDeltaMovement(
                            orb.getDeltaMovement().add(
                                    d1 / d4 * d5 * 0.05D,
                                    d2 / d4 * d5 * 0.05D,
                                    d3 / d4 * d5 * 0.05D));
                }
            }
        }
    }

    private void updateBlockState(boolean working) {
        if (level == null || level.isClientSide()) {
            return;
        }
        BlockState currentState = level.getBlockState(worldPosition);
        Boolean currentActive = currentState.getValue(
                com.singularity_iteration.mio_icif.Blocks.generator.mio_icif_block_advanced_experience_generator.ACTIVE);
        if (currentActive != working) {
            level.setBlock(worldPosition, currentState.setValue(
                    com.singularity_iteration.mio_icif.Blocks.generator.mio_icif_block_advanced_experience_generator.ACTIVE,
                    working), 3);
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

    @Override
    public boolean emitsEnergyTo(IEnergyAcceptor acceptor, Direction direction) {
        return true;
    }

    public long getEnergyStored() {
        return energyStorage.getAmount();
    }

    public long getEnergyCapacity() {
        return energyStorage.getCapacity();
    }

    public int getFuel() {
        return fuel;
    }

    public boolean isWorking() {
        return isWorking;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("Fuel", fuel);
        tag.putLong("Energy", energyStorage.getAmount());
        tag.putBoolean("IsWorking", isWorking);
    }

    @Override
    public void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        fuel = tag.getInt("Fuel");
        if (tag.contains("Energy", net.minecraft.nbt.Tag.TAG_LONG)) {
            energyStorage.setEnergy(tag.getLong("Energy"));
        }
        if (tag.contains("IsWorking", net.minecraft.nbt.Tag.TAG_BYTE)) {
            isWorking = tag.getBoolean("IsWorking");
        }
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.mio_icif.advanced_experience_generator");
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new com.singularity_iteration.mio_icif.Menu.Generator.AdvancedExperienceGeneratorMenu(containerId, playerInventory, this);
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
        return fuel;
    }

    @Override
    public int getMaxBurnTime() {
        return fuel;
    }

    @Override
    public long getPowerOutput() {
        return PRODUCTION;
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
        return fuel;
    }

    @Override
    public void setBurnTime(int ticks) {
        this.fuel = ticks;
        this.setChanged();
    }
}