package com.singularity_iteration.mio_icif.Blocks.entity.HUEntity.HuGenerator;

import com.singularity_iteration.mio_icif.Blocks.Environment.fluid.mio_icif_fluids;
import com.singularity_iteration.mio_icif.Blocks.entity.mio_icif_block_entities;
import com.singularity_iteration.mio_icif.Blocks.entity.slot.MachineItemHandler;
import com.singularity_iteration.mio_icif.Blocks.entity.slot.SlotLayout;
import com.singularity_iteration.mio_icif.Items.Cell.mio_icif_cells;
import com.singularity_iteration.mio_icif.api.MioIcifAPI;
import com.singularity_iteration.mio_icif.api.capability.IMioIcifCapabilities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Container;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.neoforged.neoforge.items.IItemHandler;
import org.jetbrains.annotations.Nullable;

/**
 * 流体加热机
 * 通过燃烧沼气液体来产生热能
 */
@SuppressWarnings("null")
public class mio_icif_fluid_heat_generator extends com.singularity_iteration.mio_icif.Blocks.entity.HUEntity.mio_icif_HeatU_Block implements WorldlyContainer {

    private static final SlotLayout LAYOUT = SlotLayout.builder()
        .extra(1)
        .output(1)
        .build();

    public static final int FUEL_BUCKET_SLOT = 0;
    public static final int EMPTY_BUCKET_SLOT = 1;
    public static final int TOTAL_SLOTS = 2;

    private static final int MAX_HEAT_GENERATION_RATE = 32;

    public static final int FUEL_CAPACITY = 25000;
    public static final int FUEL_PER_BUCKET = 1000;
    public static final int HU_PER_BUCKET = 64000;

    protected MachineItemHandler itemHandler;
    protected final FluidTank fuelTank;

    private int burnTime = 0;
    private int maxBurnTime = 0;
    private boolean isWorking = false;

    private int currentFuelBurning = 0;
    private int currentFuelHeatGenerated = 0;

    public mio_icif_fluid_heat_generator(BlockPos pos, BlockState state) {
        super(mio_icif_block_entities.FLUID_HEAT_GENERATOR.get(), pos, state, 1, 0, 0, 20, 1000, 0);

        this.slotLayout = LAYOUT;
        this.itemHandler = createItemHandler(LAYOUT);
        this.itemHandler.setValidator((slot, stack, slotType) -> mio_icif_fluid_heat_generator.this.isItemValidForSlot(slot, stack));

        this.fuelTank = new FluidTank(FUEL_CAPACITY, fluidStack ->
            fluidStack.getFluid() == mio_icif_fluids.BIOGAS.get());
    }

    public static void tick(Level level, BlockPos pos, BlockState state, mio_icif_fluid_heat_generator blockEntity) {
        if (level.isClientSide()) {
            return;
        }

        blockEntity.handleFuelBucketSlot();
        blockEntity.handleBurning();

        boolean wasActive = state.getValue(com.singularity_iteration.mio_icif.Blocks.HUGenerator.mio_icif_block_fluid_heat_generator.ACTIVE);
        boolean shouldBeActive = blockEntity.isWorking;

        if (wasActive != shouldBeActive) {
            BlockState newState = state.setValue(com.singularity_iteration.mio_icif.Blocks.HUGenerator.mio_icif_block_fluid_heat_generator.ACTIVE, shouldBeActive);
            level.setBlock(pos, newState, 3);
        }
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
            if (fuelBucketStack.isEmpty()) {
                itemHandler.setStackInSlot(FUEL_BUCKET_SLOT, ItemStack.EMPTY);
            }
            if (emptyBucketStack.isEmpty()) {
                itemHandler.setStackInSlot(EMPTY_BUCKET_SLOT, emptyContainer);
            } else {
                emptyBucketStack.grow(1);
            }
            setChanged();
        }
    }

    private boolean hasHeatConsumer() {
        Direction facing = getBlockState().getValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.FACING);
        BlockPos adjacentPos = worldPosition.relative(facing);

        IMioIcifCapabilities.IHeatStorage adjacentHeat = level.getCapability(
                IMioIcifCapabilities.HEAT_STORAGE_BLOCK, adjacentPos, facing.getOpposite());
        if (adjacentHeat == null) {
            adjacentHeat = MioIcifAPI.instance().getCapabilities().adaptHeatStorage(
                level.getBlockEntity(adjacentPos));
        }

        if (adjacentHeat != null && adjacentHeat.canReceiveHeat()) {
            return adjacentHeat.getHeatStored() < adjacentHeat.getMaxHeatStored();
        }

        return false;
    }

    private void outputHeat() {
        Direction facing = getBlockState().getValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.FACING);
        BlockPos adjacentPos = worldPosition.relative(facing);

        IMioIcifCapabilities.IHeatStorage adjacentHeat = level.getCapability(
                IMioIcifCapabilities.HEAT_STORAGE_BLOCK, adjacentPos, facing.getOpposite());
        if (adjacentHeat == null) {
            adjacentHeat = MioIcifAPI.instance().getCapabilities().adaptHeatStorage(
                level.getBlockEntity(adjacentPos));
        }

        if (adjacentHeat != null && adjacentHeat.canReceiveHeat()) {
            int heatToOutput = (int) Math.min(MAX_HEAT_GENERATION_RATE,
                    adjacentHeat.getMaxHeatStored() - adjacentHeat.getHeatStored());
            if (heatToOutput > 0) {
                adjacentHeat.receiveHeat(heatToOutput, false);
            }
        }
    }

    private void handleBurning() {
        if (burnTime > 0) {
            if (hasHeatConsumer()) {
                burnTime--;
                outputHeat();
                currentFuelHeatGenerated += MAX_HEAT_GENERATION_RATE;
                isWorking = true;

                int mbToRemove = currentFuelHeatGenerated / 32;
                if (mbToRemove > 0) {
                    currentFuelBurning -= mbToRemove;
                    currentFuelHeatGenerated = currentFuelHeatGenerated % 32;
                    if (currentFuelBurning < 0) {
                        currentFuelBurning = 0;
                    }
                }

                if (burnTime <= 0) {
                    currentFuelBurning = 0;
                    currentFuelHeatGenerated = 0;
                    tryStartBurning();
                }
            } else {
                isWorking = false;
            }
        } else {
            tryStartBurning();
        }

        setChanged();
    }

    private void tryStartBurning() {
        if (!hasHeatConsumer()) {
            isWorking = false;
            return;
        }

        if (fuelTank.getFluidAmount() >= FUEL_PER_BUCKET) {
            FluidStack drained = fuelTank.drain(FUEL_PER_BUCKET, IFluidHandler.FluidAction.EXECUTE);
            if (drained.getAmount() >= FUEL_PER_BUCKET) {
                this.currentFuelBurning = FUEL_PER_BUCKET;
                this.currentFuelHeatGenerated = 0;
                this.maxBurnTime = HU_PER_BUCKET / MAX_HEAT_GENERATION_RATE;
                this.burnTime = this.maxBurnTime;
                isWorking = true;
                setChanged();
            }
        } else {
            isWorking = false;
        }
    }

    private boolean isFuelBucket(ItemStack stack) {
        return stack.is(mio_icif_fluids.BIOGAS_BUCKET.get()) || mio_icif_cells.isCellContainingFluid(stack, mio_icif_fluids.BIOGAS.get());
    }

    public boolean isItemValidForSlot(int slot, ItemStack stack) {
        if (slot == FUEL_BUCKET_SLOT) {
            return isFuelBucket(stack);
        }
        return false;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("Items", itemHandler.serializeNBT(registries));
        tag.putInt("burnTime", burnTime);
        tag.putInt("maxBurnTime", maxBurnTime);
        tag.putBoolean("isWorking", isWorking);
        tag.put("fuelTank", fuelTank.writeToNBT(registries, new CompoundTag()));
        tag.putInt("currentFuelBurning", currentFuelBurning);
        tag.putInt("currentFuelHeatGenerated", currentFuelHeatGenerated);
    }

    @Override
    public void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("Items")) {
            itemHandler.deserializeNBT(registries, tag.getCompound("Items"));
        }
        burnTime = tag.getInt("burnTime");
        maxBurnTime = tag.getInt("maxBurnTime");
        isWorking = tag.getBoolean("isWorking");
        if (tag.contains("fuelTank")) {
            fuelTank.readFromNBT(registries, tag.getCompound("fuelTank"));
        }
        currentFuelBurning = tag.getInt("currentFuelBurning");
        currentFuelHeatGenerated = tag.getInt("currentFuelHeatGenerated");
    }

    @Nullable
    @Override
    public IMioIcifCapabilities.IHeatStorage getHeatStorageCapability(@Nullable Direction side) {
        return null;
    }

    @Nullable
    @Override
    public IItemHandler getItemHandler() {
        return itemHandler;
    }

    @Nullable
    public IItemHandler getItemHandlerCapability(@Nullable Direction side) {
        return itemHandler;
    }

    public IFluidHandler getFuelHandler() {
        return fuelTank;
    }

    @Nullable
    @Override
    protected IFluidHandler getFluidHandlerCapability(@Nullable Direction side) {
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

    public int getBurnTime() {
        return burnTime;
    }

    public int getMaxBurnTime() {
        return maxBurnTime;
    }

    public int getCurrentFuelBurning() {
        return currentFuelBurning;
    }

    public boolean isWorking() {
        return isWorking;
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        return new int[]{FUEL_BUCKET_SLOT, EMPTY_BUCKET_SLOT};
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction side) {
        if (slot == FUEL_BUCKET_SLOT) {
            return isFuelBucket(stack);
        }
        return false;
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        if (slot == EMPTY_BUCKET_SLOT) {
            return true;
        }
        if (slot == FUEL_BUCKET_SLOT) {
            return true;
        }
        return false;
    }

    @Override
    public int getContainerSize() {
        return TOTAL_SLOTS;
    }

    @Override
    public boolean isEmpty() {
        for (int i = 0; i < TOTAL_SLOTS; i++) {
            if (!itemHandler.getStackInSlot(i).isEmpty()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public ItemStack getItem(int slot) {
        return itemHandler.getStackInSlot(slot);
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        ItemStack stack = itemHandler.getStackInSlot(slot);
        if (stack.isEmpty()) {
            return ItemStack.EMPTY;
        }
        ItemStack result = stack.split(amount);
        if (stack.isEmpty()) {
            itemHandler.setStackInSlot(slot, ItemStack.EMPTY);
        }
        setChanged();
        return result;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        ItemStack stack = itemHandler.getStackInSlot(slot);
        itemHandler.setStackInSlot(slot, ItemStack.EMPTY);
        return stack;
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        itemHandler.setStackInSlot(slot, stack);
        setChanged();
    }

    @Override
    public boolean stillValid(Player player) {
        return Container.stillValidBlockEntity(this, player);
    }

    @Override
    public void clearContent() {
        for (int i = 0; i < TOTAL_SLOTS; i++) {
            itemHandler.setStackInSlot(i, ItemStack.EMPTY);
        }
        setChanged();
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.mio_icif.fluid_heat_generator");
    }

    public ContainerData createContainerData() {
        return new ContainerData() {
            @Override
            public int get(int index) {
                return switch (index) {
                    case 0 -> burnTime;
                    case 1 -> maxBurnTime;
                    case 2 -> fuelTank.getFluidAmount();
                    case 3 -> FUEL_CAPACITY;
                    default -> 0;
                };
            }

            @Override
            public void set(int index, int value) {
                switch (index) {
                    case 0 -> burnTime = value;
                    case 1 -> maxBurnTime = value;
                }
            }

            @Override
            public int getCount() {
                return 4;
            }
        };
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new com.singularity_iteration.mio_icif.Menu.HUEntity.FluidHeatGeneratorMenu(containerId, playerInventory, this);
    }
}