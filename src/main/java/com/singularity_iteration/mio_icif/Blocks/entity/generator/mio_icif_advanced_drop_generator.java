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
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.items.IItemHandler;

import java.util.List;

@SuppressWarnings("null")
public class mio_icif_advanced_drop_generator extends mio_icif_Energy_Block implements IGeneratorBlock {

    private boolean registered = false;

    private static final long ENERGY_CAPACITY = 100000L;
    private static final long MAX_ENERGY_RECEIVE = 0L;
    private static final long MAX_ENERGY_EXTRACT = 512L;
    private static final CableTier CABLE_TIER = CableTier.HV;
    private static final long PRODUCTION = 40L;

    private static final int SCAN_RANGE = 5;
    private static final double CONSUME_DISTANCE_SQ = 1.0D;

    private int fuel = 0;
    private boolean isWorking = false;

    public mio_icif_advanced_drop_generator(BlockPos pos, BlockState state) {
        super(pos, state, mio_icif_block_entities.ADVANCED_DROP_GENERATOR_ENTITY_TYPE.get(),
              ENERGY_CAPACITY, MAX_ENERGY_RECEIVE, MAX_ENERGY_EXTRACT, CABLE_TIER);
        setAsPowerSource(PRODUCTION);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, mio_icif_advanced_drop_generator blockEntity) {
        if (level.isClientSide()) {
            return;
        }

        blockEntity.scanAndConsumeItems(level, pos);

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

    private void scanAndConsumeItems(Level level, BlockPos pos) {
        BlockPos minPos = pos.offset(-SCAN_RANGE, -SCAN_RANGE, -SCAN_RANGE);
        BlockPos maxPos = pos.offset(SCAN_RANGE, SCAN_RANGE, SCAN_RANGE);
        AABB bb = new AABB(minPos.getX(), minPos.getY(), minPos.getZ(), maxPos.getX() + 1, maxPos.getY() + 1, maxPos.getZ() + 1);

        List<ItemEntity> items = level.getEntitiesOfClass(ItemEntity.class, bb);

        for (ItemEntity entityItem : items) {
            double dist = pos.distToCenterSqr(entityItem.getX(), entityItem.getY(), entityItem.getZ());

            if (dist <= CONSUME_DISTANCE_SQ) {
                ItemStack stack = entityItem.getItem();
                if (stack.isEmpty()) {
                    continue;
                }
                Rarity rarity = stack.getRarity();
                int itemValue = getItemValue(rarity);
                fuel += itemValue;
                stack.shrink(1);
                entityItem.setItem(stack);
            } else {
                double d1 = (pos.getX() + 0.5D - entityItem.getX()) / 8.0D;
                double d2 = (pos.getY() + 0.5D - entityItem.getY()) / 8.0D;
                double d3 = (pos.getZ() + 0.5D - entityItem.getZ()) / 8.0D;
                double d4 = Math.sqrt(d1 * d1 + d2 * d2 + d3 * d3);
                double d5 = 1.0D - d4;

                if (d5 > 0.0D) {
                    d5 = d5 * d5;
                    entityItem.setDeltaMovement(
                            entityItem.getDeltaMovement().add(
                                    d1 / d4 * d5 * 0.05D,
                                    d2 / d4 * d5 * 0.05D,
                                    d3 / d4 * d5 * 0.05D));
                }
            }
        }
    }

    private int getItemValue(Rarity rarity) {
        if (rarity == Rarity.COMMON) return 40;
        if (rarity == Rarity.UNCOMMON) return 800;
        if (rarity == Rarity.EPIC) return 10000;
        if (rarity == Rarity.RARE) return 20000;
        return 40;
    }

    private void updateBlockState(boolean working) {
        if (level == null || level.isClientSide()) {
            return;
        }
        BlockState currentState = level.getBlockState(worldPosition);
        Boolean currentActive = currentState.getValue(
                com.singularity_iteration.mio_icif.Blocks.generator.mio_icif_block_advanced_drop_generator.ACTIVE);
        if (currentActive != working) {
            level.setBlock(worldPosition, currentState.setValue(
                    com.singularity_iteration.mio_icif.Blocks.generator.mio_icif_block_advanced_drop_generator.ACTIVE,
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
        return Component.translatable("container.mio_icif.advanced_drop_generator");
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new com.singularity_iteration.mio_icif.Menu.Generator.AdvancedDropGeneratorMenu(containerId, playerInventory, this);
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