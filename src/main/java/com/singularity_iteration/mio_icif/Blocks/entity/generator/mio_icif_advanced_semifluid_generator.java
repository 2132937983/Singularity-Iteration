package com.singularity_iteration.mio_icif.Blocks.entity.generator;

import com.singularity_iteration.mio_icif.Blocks.Environment.fluid.mio_icif_fluids;
import com.singularity_iteration.mio_icif.Blocks.entity.mio_icif_block_entities;
import com.singularity_iteration.mio_icif.Blocks.entity.mio_icif_Energy_Generator;
import com.singularity_iteration.mio_icif.Blocks.entity.slot.SlotLayout;
import com.singularity_iteration.mio_icif.Blocks.generator.mio_icif_Block_Advanced_Semifluid_Generator;
import com.singularity_iteration.mio_icif.Items.Cell.mio_icif_cells;
import com.singularity_iteration.mio_icif.energy.EnergyUnit.CableTier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import org.jetbrains.annotations.Nullable;

@SuppressWarnings("null")
public class mio_icif_advanced_semifluid_generator extends mio_icif_Energy_Generator {

    public static final int SLOT_COUNT = 3;
    public static final int FUEL_BUCKET_SLOT = 0;
    public static final int EMPTY_BUCKET_SLOT = 1;
    public static final int BATTERY_SLOT = 2;

    public static final int FUEL_CAPACITY = 30000;
    public static final int FUEL_PER_BUCKET = 1000;
    public static final long ENERGY_PER_BUCKET = 24000L;

    public static final long ENERGY_GENERATION_RATE = 36L;

    public static final long ENERGY_CAPACITY = 100000L;
    public static final long MAX_RECEIVE = 0L;
    public static final long MAX_EXTRACT = 128L;

    protected final FluidTank fuelTank;

    private int currentFuelBurning = 0;
    private int currentFuelEnergyGenerated = 0;

    public mio_icif_advanced_semifluid_generator(BlockPos pos, BlockState state) {
        this(pos, state, null);
    }

    public mio_icif_advanced_semifluid_generator(BlockPos pos, BlockState state, BlockEntityType<?> type) {
        super(pos, state, type != null ? type : mio_icif_block_entities.ADVANCED_SEMIFLUID_GENERATOR_ENTITY_TYPE.get(),
            SlotLayout.builder().extra(2).battery().build(), ENERGY_GENERATION_RATE, ENERGY_CAPACITY, MAX_RECEIVE, MAX_EXTRACT, CableTier.MV);

        this.fuelTank = new FluidTank(FUEL_CAPACITY, fluidStack ->
            fluidStack.getFluid() == mio_icif_fluids.BIOGAS.get());
    }

    private boolean isFuelBucket(ItemStack stack) {
        return stack.is(mio_icif_fluids.BIOGAS_BUCKET.get()) || mio_icif_cells.isCellContainingFluid(stack, mio_icif_fluids.BIOGAS.get());
    }

    @Override
    public int getFuelBurnTime(ItemStack fuel) {
        if (isFuelBucket(fuel)) {
            return Integer.MAX_VALUE;
        }
        return 0;
    }

    private void handleFuelBucketSlot() {
        ItemStack fuelBucketStack = itemHandler.getStackInSlot(FUEL_BUCKET_SLOT);
        if (fuelBucketStack.isEmpty() || !isFuelBucket(fuelBucketStack)) {
            return;
        }

        if (fuelTank.getFluidAmount() >= fuelTank.getCapacity()) {
            return;
        }

        boolean isCell = mio_icif_cells.isFluidCell(fuelBucketStack);
        ItemStack emptyContainer = isCell ? mio_icif_cells.getEmptyCellForStack(fuelBucketStack) : new ItemStack(Items.BUCKET);
        if (isCell && emptyContainer.isEmpty()) emptyContainer = new ItemStack(mio_icif_cells.CELL_EMPTY.get());

        ItemStack emptyBucketStack = itemHandler.getStackInSlot(EMPTY_BUCKET_SLOT);
        if (!emptyBucketStack.isEmpty()) {
            if (!ItemStack.isSameItem(emptyBucketStack, emptyContainer) || emptyBucketStack.getCount() >= emptyBucketStack.getMaxStackSize()) {
                return;
            }
        }

        int filled = fuelTank.fill(new FluidStack(mio_icif_fluids.BIOGAS.get(), FUEL_PER_BUCKET), IFluidHandler.FluidAction.EXECUTE);
        if (filled >= FUEL_PER_BUCKET) {
            fuelBucketStack.shrink(1);
            if (emptyBucketStack.isEmpty()) {
                itemHandler.setStackInSlot(EMPTY_BUCKET_SLOT, emptyContainer);
            } else {
                emptyBucketStack.grow(1);
            }
            setChanged();
            if (level != null && !level.isClientSide()) {
                level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), 3);
            }
        }
    }

    @Override
    protected void consumeFuel() {
        boolean isEnergyFull = getEnergyStorage().getAmount() >=
                              getEnergyStorage().getCapacity();

        if (isEnergyFull) {
            return;
        }

        if (fuelTank.getFluidAmount() >= FUEL_PER_BUCKET) {
            FluidStack drained = fuelTank.drain(FUEL_PER_BUCKET, IFluidHandler.FluidAction.EXECUTE);
            if (drained.getAmount() >= FUEL_PER_BUCKET) {
                this.currentFuelBurning = FUEL_PER_BUCKET;
                this.currentFuelEnergyGenerated = 0;
                this.burnDuration = (int) (ENERGY_PER_BUCKET / ENERGY_GENERATION_RATE);
                this.burnTime = this.burnDuration;
                setChanged();
            }
        }
    }

    @Override
    protected void generateEnergy() {
        long totalEnergyGenerated = 0;
        
        if (currentFuelBurning > 0) {
            long fuelBucketEnergy = Math.min(energyGenerationRate,
                getEnergyStorage().getCapacity() - getEnergyStorage().getAmount());
            
            if (fuelBucketEnergy > 0) {
                totalEnergyGenerated += fuelBucketEnergy;
                currentFuelEnergyGenerated += (int) fuelBucketEnergy;
                
                int mbToRemove = currentFuelEnergyGenerated / (int)(ENERGY_PER_BUCKET / FUEL_PER_BUCKET);
                if (mbToRemove > 0) {
                    currentFuelBurning -= mbToRemove;
                    currentFuelEnergyGenerated = currentFuelEnergyGenerated % (int)(ENERGY_PER_BUCKET / FUEL_PER_BUCKET);
                    
                    if (currentFuelBurning < 0) {
                        currentFuelBurning = 0;
                    }
                }
            }
        }
        
        if (totalEnergyGenerated > 0) {
            apiGenerateEnergy(totalEnergyGenerated, false);
        }
    }

    @Override
    protected void chargeItems() {
        ItemStack chargeStack = itemHandler.getStackInSlot(BATTERY_SLOT);
        if (chargeStack.isEmpty()) {
            return;
        }

        if (getItemAPI().isBattery(chargeStack)) {
            var api = getItemAPI();
            long currentEnergy = api.getBatteryStored(chargeStack);
            long batteryMaxEnergy = api.getBatteryCapacity(chargeStack);
            long batteryChargeRate = api.getChargeRate(chargeStack);

            if (currentEnergy >= batteryMaxEnergy) {
                return;
            }

            long availableEnergy = getEnergyStorage().getAmount();
            if (availableEnergy <= 0) {
                return;
            }

            long energyToCharge = Math.min(batteryChargeRate, batteryMaxEnergy - currentEnergy);
            energyToCharge = Math.min(energyToCharge, availableEnergy);

            long energyExtracted = getEnergyStorageInternal().extract(energyToCharge, false);

            api.chargeBattery(chargeStack, energyExtracted, false);
            setChanged();
        }
    }

    public static void tick(Level level, BlockPos pos, BlockState state, mio_icif_advanced_semifluid_generator blockEntity) {
        if (level.isClientSide()) {
            return;
        }

        boolean wasBurning = blockEntity.isBurning();

        blockEntity.handleFuelBucketSlot();

        blockEntity.chargeItems();
        
        if (blockEntity.shouldDirectlyDistributeEnergy()) {
            blockEntity.distributeEnergy();
        }

        boolean isEnergyFull = blockEntity.getEnergyStorage().getAmount() >=
                              blockEntity.getEnergyStorage().getCapacity();

        if (!isEnergyFull && blockEntity.isBurning()) {
            blockEntity.generateEnergy();
        }

        if (blockEntity.isBurning()) {
            if (isEnergyFull) {
                blockEntity.burnTime--;
                if (blockEntity.burnTime <= 0) {
                    blockEntity.burnTime = 0;
                    blockEntity.currentFuelBurning = 0;
                    blockEntity.currentFuelEnergyGenerated = 0;
                }
            } else {
                blockEntity.burnTime--;

                if (blockEntity.burnTime <= 0) {
                    blockEntity.currentFuelBurning = 0;
                    blockEntity.currentFuelEnergyGenerated = 0;
                    blockEntity.consumeFuel();
                }
            }
        } else {
            if (!isEnergyFull) {
                blockEntity.consumeFuel();
            }
        }

        boolean isBurning = blockEntity.isBurning();
        if (wasBurning != isBurning) {
            BlockState newState = level.getBlockState(pos);
            if (newState.hasProperty(mio_icif_Block_Advanced_Semifluid_Generator.ACTIVE)) {
                newState = newState.setValue(mio_icif_Block_Advanced_Semifluid_Generator.ACTIVE, isBurning);
                level.setBlock(pos, newState, 3);
            }
        }

        blockEntity.setChanged();
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        return new int[]{FUEL_BUCKET_SLOT, EMPTY_BUCKET_SLOT, BATTERY_SLOT};
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction side) {
        if (slot == FUEL_BUCKET_SLOT) {
            return isFuelBucket(stack);
        }
        if (slot == EMPTY_BUCKET_SLOT) {
            return false;
        }
        if (slot == BATTERY_SLOT) {
            return isBattery(stack);
        }
        return false;
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        if (slot == EMPTY_BUCKET_SLOT) {
            return true;
        }
        if (slot == BATTERY_SLOT) {
            return true;
        }
        if (slot == FUEL_BUCKET_SLOT) {
            return true;
        }
        return false;
    }

    public IFluidHandler getFuelHandler() {
        return fuelTank;
    }

    @Nullable
    public IFluidHandler getFuelHandlerCapability(@Nullable Direction side) {
        return fuelTank;
    }

    public int getFuelAmount() {
        return fuelTank.getFluidAmount();
    }

    public int getFuelCapacity() {
        return fuelTank.getCapacity();
    }

    public FluidStack getFuel() {
        return fuelTank.getFluid();
    }

    public int getFuelProgress() {
        if (fuelTank.getCapacity() <= 0) {
            return 0;
        }
        return (fuelTank.getFluidAmount() * 100) / fuelTank.getCapacity();
    }

    public int getFuelBuckets() {
        return fuelTank.getFluidAmount() / FUEL_PER_BUCKET;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("FuelTank", fuelTank.writeToNBT(registries, new CompoundTag()));
        tag.putInt("CurrentFuelBurning", currentFuelBurning);
        tag.putInt("CurrentFuelEnergyGenerated", currentFuelEnergyGenerated);
    }

    @Override
    public void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("FuelTank")) {
            fuelTank.readFromNBT(registries, tag.getCompound("FuelTank"));
        }
        currentFuelBurning = tag.getInt("CurrentFuelBurning");
        currentFuelEnergyGenerated = tag.getInt("CurrentFuelEnergyGenerated");
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        tag.put("FuelTank", fuelTank.writeToNBT(registries, new CompoundTag()));
        tag.putInt("CurrentFuelBurning", currentFuelBurning);
        tag.putInt("CurrentFuelEnergyGenerated", currentFuelEnergyGenerated);
        return tag;
    }

    @Override
    public void handleUpdateTag(CompoundTag tag, HolderLookup.Provider registries) {
        super.handleUpdateTag(tag, registries);
        if (tag.contains("FuelTank")) {
            fuelTank.readFromNBT(registries, tag.getCompound("FuelTank"));
        }
        currentFuelBurning = tag.getInt("CurrentFuelBurning");
        currentFuelEnergyGenerated = tag.getInt("CurrentFuelEnergyGenerated");
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.mio_icif.advanced_semifluid_generator");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new com.singularity_iteration.mio_icif.Menu.Generator.AdvancedSemifluidGeneratorMenu(containerId, playerInventory, this, this.getItemHandler(), null);
    }

    public static void registerCapabilities(net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(
            Capabilities.FluidHandler.BLOCK,
            mio_icif_block_entities.ADVANCED_SEMIFLUID_GENERATOR_ENTITY_TYPE.get(),
            (be, side) -> be.getFuelHandlerCapability(side)
        );
    }
}