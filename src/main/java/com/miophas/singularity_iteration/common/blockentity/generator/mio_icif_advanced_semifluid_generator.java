package com.miophas.singularity_iteration.common.blockentity.generator;

import com.miophas.singularity_iteration.common.block.environment.fluid.mio_icif_fluids;
import com.miophas.singularity_iteration.common.registry.mio_icif_block_entities;
import com.miophas.singularity_iteration.core.prefab.blockentity.AbstractGeneratorBlockEntity;
import com.miophas.singularity_iteration.core.prefab.inventory.SlotLayout;
import com.miophas.singularity_iteration.common.block.generator.mio_icif_Block_Advanced_Semifluid_Generator;
import com.miophas.singularity_iteration.common.item.cell.mio_icif_cells;
import com.miophas.singularity_iteration.core.api.energy.storage.CableTier;
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
public class mio_icif_advanced_semifluid_generator extends AbstractGeneratorBlockEntity {

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
    private long scexFuelCredit;

    private int currentFuelBurning = 0;
    private int currentFuelEnergyGenerated = 0;

    public mio_icif_advanced_semifluid_generator(BlockPos pos, BlockState state) {
        this(pos, state, null);
    }

    public mio_icif_advanced_semifluid_generator(BlockPos pos, BlockState state, BlockEntityType<?> type) {
        super(pos, state, type != null ? type : mio_icif_block_entities.ADVANCED_SEMIFLUID_GENERATOR_ENTITY_TYPE.get(),
            SlotLayout.builder().extra(2).battery().build(), ENERGY_GENERATION_RATE, ENERGY_CAPACITY, MAX_RECEIVE, MAX_EXTRACT, CableTier.MV);

        this.fuelTank = new FluidTank(FUEL_CAPACITY, fluidStack ->
            fluidStack.getFluid() == mio_icif_fluids.BIOGAS.get()) {
            @Override protected void onContentsChanged() {
                if (energyStorage.scexNetworkControlled()) com.miophas.singularity_iteration.core.runtime.energy.ContainerToTank.markUnsaved(mio_icif_advanced_semifluid_generator.this);
                else mio_icif_advanced_semifluid_generator.this.setChanged();
            }
        };
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
        if (energyStorage.scexNetworkControlled()) { scexFillFuelContainer(); return; }
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
        if (energyStorage.scexNetworkControlled()) {
            if (com.miophas.singularity_iteration.core.runtime.energy.SolarItemCharging.chargeRange(itemHandler, BATTERY_SLOT, 1, energyStorage, getItemAPI())) setChanged();
            return;
        }
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
        if (blockEntity.energyStorage.scexNetworkControlled()) { blockEntity.scexTickFuel(level, pos); return; }

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
        if (energyStorage.scexNetworkControlled() || scexFuelCredit > 0) tag.putLong("scex_fuel_credit_eu", scexFuelCredit);
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
        scexReadFuelCredit(tag);
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        if (energyStorage.scexNetworkControlled() || scexFuelCredit > 0) tag.putLong("scex_fuel_credit_eu", scexFuelCredit);
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
        scexReadFuelCredit(tag);
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.mio_icif.advanced_semifluid_generator");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new com.miophas.singularity_iteration.common.menu.generator.AdvancedSemifluidGeneratorMenu(containerId, playerInventory, this, this.getItemHandler(), null);
    }

    public static void registerCapabilities(net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(
            Capabilities.FluidHandler.BLOCK,
            mio_icif_block_entities.ADVANCED_SEMIFLUID_GENERATOR_ENTITY_TYPE.get(),
            (be, side) -> be.getFuelHandlerCapability(side)
        );
    }

    private void scexFillFuelContainer() {
        var input = itemHandler.getStackInSlot(FUEL_BUCKET_SLOT);
        if (input.isEmpty() || !isFuelBucket(input)) return;
        var api = getItemAPI();
        boolean cell = api.isFluidCell(input);
        var fluid = mio_icif_fluids.BIOGAS.get();
        if (fluid == null) return;
        var contents = cell ? api.getFluidCellContent(input.copyWithCount(1)).copy()
            : new FluidStack(fluid, FUEL_PER_BUCKET);
        var empty = cell ? api.getFluidCellEmptyContainer(input.copyWithCount(1)) : new ItemStack(Items.BUCKET);
        if (contents.isEmpty() || contents.getFluid() != fluid || empty.isEmpty()) return;
        if (com.miophas.singularity_iteration.core.runtime.energy.ContainerToTank.transfer(itemHandler, FUEL_BUCKET_SLOT, EMPTY_BUCKET_SLOT, fuelTank, contents, empty)) setChanged();
    }

    private void scexTickFuel(Level level, BlockPos pos) {
        boolean wasBurning = burnTime > 0;
        handleFuelBucketSlot();
        chargeItems();
        var step = com.miophas.singularity_iteration.core.runtime.energy.FluidFuelGeneration.tick(fuelTank, energyStorage,
            scexFuelCredit, 1, ENERGY_PER_BUCKET / FUEL_PER_BUCKET, ENERGY_GENERATION_RATE);
        scexFuelCredit = step.bufferedEnergy();
        burnTime = step.generated() > 0 ? 1 : 0;
        burnDuration = 1;
        if (wasBurning != (burnTime > 0)) {
            var state = level.getBlockState(pos);
            if (state.hasProperty(mio_icif_Block_Advanced_Semifluid_Generator.ACTIVE)) level.setBlock(pos, state.setValue(mio_icif_Block_Advanced_Semifluid_Generator.ACTIVE, burnTime > 0), 3);
        }
        setChanged();
    }

    private void scexReadFuelCredit(CompoundTag tag) {
        if (tag.contains("scex_fuel_credit_eu", net.minecraft.nbt.Tag.TAG_LONG)) {
            scexFuelCredit = Math.clamp(tag.getLong("scex_fuel_credit_eu"), 0L, ENERGY_PER_BUCKET);
        } else if (energyStorage.scexNetworkControlled()) {
            scexFuelCredit = com.miophas.singularity_iteration.core.runtime.energy.engine.ConsumableGeneration.legacyCredit(currentFuelBurning,
                currentFuelEnergyGenerated, ENERGY_PER_BUCKET / FUEL_PER_BUCKET, FUEL_PER_BUCKET);
            currentFuelBurning = 0;
            currentFuelEnergyGenerated = 0;
        }
    }
}
