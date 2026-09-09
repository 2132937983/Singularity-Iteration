package com.singularity_iteration.mio_icif.Blocks.entity.producer;

import com.singularity_iteration.mio_icif.Blocks.entity.mio_icif_block_entities;
import com.singularity_iteration.mio_icif.Items.Cell.mio_icif_cells;
import com.singularity_iteration.mio_icif.api.MioIcifAPI;
import com.singularity_iteration.mio_icif.api.energy.ICableTier;
import com.singularity_iteration.mio_icif.api.internal.machine.GenericMachineBlockEntity;
import com.singularity_iteration.mio_icif.api.machine.ISlotLayout;
import com.singularity_iteration.mio_icif.api.machine.builder.IElectricMachineBuilder;
import com.singularity_iteration.mio_icif.api.machine.builder.IMachineBuilderAPI;
import com.singularity_iteration.mio_icif.api.recipe.IRecipeAPI;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidUtil;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.IFluidHandlerItem;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

@SuppressWarnings("null")
public class mio_icif_oil_refinery_elc extends GenericMachineBlockEntity {

    public static final long DEFAULT_CAPACITY = 2000L;
    public static final long DEFAULT_MAX_RECEIVE = 128L;
    public static final long DEFAULT_MAX_EXTRACT = 0L;
    public static final int DEFAULT_WORK_TIME = 10;
    public static final long DEFAULT_ENERGY_PER_TICK = 8L;

    public static final int INPUT_TANK_CAPACITY = 10000;
    public static final int OUTPUT_TANK_CAPACITY = 10000;

    private static IMachineBuilderAPI.MachineConfiguration CACHED_CONFIGURATION;
    private static ISlotLayout CACHED_LAYOUT;
    private static ICableTier CACHED_TIER;

    private final int inputSlot;
    private final int inputEmptySlot;
    private final int outputEmptySlot;
    private final int outputSlot;
    private final int batterySlot;

    protected final FluidTank inputTank;
    protected final FluidTank outputTank;

    private int overclockerCount = 0;
    private int fluidPerCycle = 1;

    private final ContainerData containerData = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> progress;
                case 1 -> maxProgress;
                case 2 -> isWorking ? 1 : 0;
                case 3 -> (int) energyStorage.getAmount();
                case 4 -> (int) energyStorage.getCapacity();
                case 5 -> inputTank.getFluidAmount();
                case 6 -> inputTank.getCapacity();
                case 7 -> outputTank.getFluidAmount();
                case 8 -> outputTank.getCapacity();
                case 9 -> getInputFluidTypeId();
                case 10 -> getOutputFluidTypeId();
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {}

        @Override
        public int getCount() { return 11; }
    };

    public mio_icif_oil_refinery_elc(BlockPos pos, BlockState state) {
        this(pos, state, mio_icif_block_entities.OIL_REFINERY_ELC_ENTITY_TYPE.get());
    }

    public mio_icif_oil_refinery_elc(BlockPos pos, BlockState state, BlockEntityType<?> type) {
        super(pos, state, type,
            DEFAULT_CAPACITY,
            DEFAULT_MAX_RECEIVE,
            DEFAULT_MAX_EXTRACT,
            DEFAULT_WORK_TIME,
            getOrCreateLayout(),
            DEFAULT_ENERGY_PER_TICK,
            getOrCreateTier());
        ISlotLayout layout = getOrCreateLayout();
        this.inputSlot = layout.getInputSlots()[0];
        this.inputEmptySlot = layout.getInputSlots()[1];
        this.outputEmptySlot = layout.getOutputSlots()[0];
        this.outputSlot = layout.getOutputSlots()[1];
        this.batterySlot = layout.getBatterySlots()[0];
        setConfiguration(getOrCreateConfiguration());
        this.inputTank = new FluidTank(INPUT_TANK_CAPACITY);
        this.outputTank = new FluidTank(OUTPUT_TANK_CAPACITY);
    }

    public static ISlotLayout getOrCreateLayout() {
        if (CACHED_LAYOUT == null) {
            CACHED_LAYOUT = MioIcifAPI.instance().getMachineBuilderAPI()
                .createStandardLayout(2, 2, true, 4);
        }
        return CACHED_LAYOUT;
    }

    public static ICableTier getOrCreateTier() {
        if (CACHED_TIER == null) {
            CACHED_TIER = MioIcifAPI.instance().getEnergyNetAPI().getCableTier("mv");
        }
        return CACHED_TIER;
    }

    public static IMachineBuilderAPI.MachineConfiguration getOrCreateConfiguration() {
        if (CACHED_CONFIGURATION == null) {
            CACHED_CONFIGURATION = buildMachineDefinition();
        }
        return CACHED_CONFIGURATION;
    }

    private static IMachineBuilderAPI.MachineConfiguration buildMachineDefinition() {
        IElectricMachineBuilder builder = MioIcifAPI.instance().getMachineBuilderAPI()
            .createElectricMachineBuilder()
            .setName("oil_refinery_elc")
            .setTranslationKey("container.mio_icif.oil_refinery_elc")
            .setEnergyCapacity(DEFAULT_CAPACITY)
            .setMaxReceive(DEFAULT_MAX_RECEIVE)
            .setMaxExtract(DEFAULT_MAX_EXTRACT)
            .setEnergyPerTick(DEFAULT_ENERGY_PER_TICK)
            .setProcessTime(DEFAULT_WORK_TIME)
            .setCableTier(getOrCreateTier())
            .useStandardLayout(1, 1, true, 4)
            .setSupportsUpgrades(true)
            .setSupportsFluids(true)
            .addFluidTank(INPUT_TANK_CAPACITY)
            .addFluidTank(OUTPUT_TANK_CAPACITY)
            .setMachineType(com.singularity_iteration.mio_icif.api.machine.IMachineAPI.MachineType.CUSTOM)
            .withEntityType(mio_icif_block_entities.OIL_REFINERY_ELC_ENTITY_TYPE.get());

        builder.buildAndRegister("mio_icif");
        return builder.getConfiguration();
    }

    @Nullable
    @Override
    public IFluidHandler getFluidHandlerCapability(@Nullable Direction side) {
        return new CombinedFluidHandler(inputTank, outputTank);
    }

    public FluidTank getInputTank() {
        return inputTank;
    }

    public FluidTank getOutputTank() {
        return outputTank;
    }

    public FluidStack getInputFluid() {
        return inputTank.getFluid();
    }

    public FluidStack getOutputFluid() {
        return outputTank.getFluid();
    }

    public int getInputFluidTypeId() {
        FluidStack fluid = inputTank.getFluid();
        if (fluid.isEmpty()) return -1;
        return net.minecraft.core.registries.BuiltInRegistries.FLUID.getId(fluid.getFluid());
    }

    public int getOutputFluidTypeId() {
        FluidStack fluid = outputTank.getFluid();
        if (fluid.isEmpty()) return -1;
        return net.minecraft.core.registries.BuiltInRegistries.FLUID.getId(fluid.getFluid());
    }

    private Optional<? extends RecipeHolder<?>> findRecipe() {
        if (level == null) return Optional.empty();
        FluidStack inputFluid = inputTank.getFluid();
        if (inputFluid.isEmpty()) return Optional.empty();

        IRecipeAPI recipeAPI = MioIcifAPI.instance().getRecipeAPI();
        return recipeAPI.findFluidRefiningRecipeForFluid(inputFluid, level);
    }

    @Override
    protected void recalculateUpgradeStats() {
        super.recalculateUpgradeStats();
        int upgradeStart = slotLayout.getStart(com.singularity_iteration.mio_icif.Blocks.entity.slot.SlotType.UPGRADE);
        int upgradeCount = slotLayout.getCount(com.singularity_iteration.mio_icif.Blocks.entity.slot.SlotType.UPGRADE);
        com.singularity_iteration.mio_icif.Items.Upgrade.MachineUpgradeStats stats =
            com.singularity_iteration.mio_icif.Items.Upgrade.MachineUpgradeStats.fromInventory(itemHandler, upgradeStart, upgradeCount);
        overclockerCount = stats.overclockerCount;
        fluidPerCycle = Math.max(1, overclockerCount + 1);
    }

    @Override
    public int getProgressPerTick() {
        return 1;
    }

    @Override
    public long getEffectiveEnergyPerTick() {
        return DEFAULT_ENERGY_PER_TICK * fluidPerCycle;
    }

    public int getFluidPerCycle() {
        return fluidPerCycle;
    }

    @Override
    public boolean isItemValidForSlot(int slot, ItemStack stack) {
        if (slot == inputSlot) return isValidInputCell(stack);
        if (slot == inputEmptySlot) return isEmptyContainer(stack);
        if (slot == outputEmptySlot) return mio_icif_cells.isEmptyCell(stack);
        if (slot == outputSlot) return false;
        if (slot == batterySlot) return isBattery(stack);
        if (isUpgradeSlot(slot)) return getItemAPI().isUpgrade(stack);
        return false;
    }

    private boolean isEmptyContainer(ItemStack stack) {
        if (mio_icif_cells.isEmptyCell(stack)) return true;
        if (stack.getItem() == net.minecraft.world.item.Items.BUCKET) return true;
        var containedOpt = FluidUtil.getFluidContained(stack);
        if (containedOpt.isPresent() && containedOpt.get().isEmpty()) return true;
        return false;
    }

    private boolean isValidInputCell(ItemStack stack) {
        if (stack.isEmpty()) return false;

        if (mio_icif_cells.isFluidCell(stack)) {
            FluidStack fluid = mio_icif_cells.getCellFluid(stack);
            if (fluid.isEmpty()) return false;
            return findRecipeForFluid(fluid.getFluid()).isPresent();
        }

        var containedOpt = FluidUtil.getFluidContained(stack);
        if (containedOpt.isPresent()) {
            FluidStack fluid = containedOpt.get();
            if (!fluid.isEmpty()) {
                return findRecipeForFluid(fluid.getFluid()).isPresent();
            }
        }

        return false;
    }

    private Optional<? extends RecipeHolder<?>> findRecipeForFluid(Fluid fluid) {
        if (level == null) return Optional.empty();
        IRecipeAPI recipeAPI = MioIcifAPI.instance().getRecipeAPI();
        return recipeAPI.findFluidRefiningRecipeForFluid(new FluidStack(fluid, 1), level);
    }

    @Override
    protected int[] getSlotsForDirection(Direction side) {
        return new int[]{inputSlot, inputEmptySlot, outputEmptySlot, outputSlot, batterySlot};
    }

    @Override
    protected boolean hasValidRecipe() {
        return findRecipe().isPresent();
    }

    @Override
    protected boolean canWork() {
        if (!hasEnoughEnergy()) return false;

        Optional<? extends RecipeHolder<?>> recipeOpt = findRecipe();
        if (recipeOpt.isEmpty()) return false;

        IRecipeAPI recipeAPI = MioIcifAPI.instance().getRecipeAPI();
        FluidStack outputFluid = recipeAPI.getFluidRefiningOutputFluid(recipeOpt.get());

        int neededAmount = outputFluid.getAmount() * fluidPerCycle;

        if (inputTank.getFluidAmount() < neededAmount) return false;

        int spaceLeft = outputTank.getCapacity() - outputTank.getFluidAmount();
        if (spaceLeft < neededAmount) return false;

        return true;
    }

    @Override
    protected void doWork() {
        if (!consumeEnergy()) {
            stopWork();
            return;
        }

        isWorking = true;
        progress++;

        if (progress >= maxProgress) {
            finishRefining();
        }
    }

    private void finishRefining() {
        Optional<? extends RecipeHolder<?>> recipeOpt = findRecipe();
        if (recipeOpt.isEmpty()) {
            stopWork();
            return;
        }

        IRecipeAPI recipeAPI = MioIcifAPI.instance().getRecipeAPI();
        FluidStack outputFluid = recipeAPI.getFluidRefiningOutputFluid(recipeOpt.get());

        int neededAmount = outputFluid.getAmount() * fluidPerCycle;

        if (inputTank.getFluidAmount() < neededAmount) {
            stopWork();
            return;
        }

        int spaceLeft = outputTank.getCapacity() - outputTank.getFluidAmount();
        if (spaceLeft < neededAmount) {
            stopWork();
            return;
        }

        inputTank.drain(neededAmount, IFluidHandler.FluidAction.EXECUTE);
        outputTank.fill(outputFluid.copyWithAmount(neededAmount), IFluidHandler.FluidAction.EXECUTE);

        finishWork();

        if (canWork()) {
            isWorking = true;
        }
    }

    private void handleInputCellSlot() {
        if (level == null || level.isClientSide) return;
        ItemStack cellStack = itemHandler.getStackInSlot(inputSlot);
        if (cellStack.isEmpty()) return;

        if (mio_icif_cells.isFluidCell(cellStack)) {
            handleInputCellFromCell(cellStack);
        } else {
            handleInputCellFromGenericContainer(cellStack);
        }
    }

    private void handleInputCellFromCell(ItemStack cellStack) {
        FluidStack cellFluid = mio_icif_cells.getCellFluid(cellStack);
        if (cellFluid.isEmpty()) return;

        if (!inputTank.isEmpty() && inputTank.getFluid().getFluid() != cellFluid.getFluid()) return;

        if (inputTank.getFluidAmount() >= inputTank.getCapacity()) return;

        ItemStack emptyCell = mio_icif_cells.getEmptyCellForStack(cellStack);
        if (emptyCell.isEmpty()) emptyCell = new ItemStack(mio_icif_cells.CELL_EMPTY.get());

        ItemStack currentEmptySlot = itemHandler.getStackInSlot(inputEmptySlot);
        if (!currentEmptySlot.isEmpty()) {
            if (!ItemStack.isSameItem(currentEmptySlot, emptyCell) ||
                currentEmptySlot.getCount() >= currentEmptySlot.getMaxStackSize()) {
                return;
            }
        }

        int space = inputTank.getCapacity() - inputTank.getFluidAmount();
        int toFill = Math.min(cellFluid.getAmount(), space);
        if (toFill <= 0) return;

        inputTank.fill(cellFluid.copyWithAmount(toFill), IFluidHandler.FluidAction.EXECUTE);
        cellStack.shrink(1);
        if (currentEmptySlot.isEmpty()) {
            itemHandler.setStackInSlot(inputEmptySlot, emptyCell.copy());
        } else {
            currentEmptySlot.grow(1);
        }
        setChanged();
    }

    private void handleInputCellFromGenericContainer(ItemStack containerStack) {
        var containedOpt = FluidUtil.getFluidContained(containerStack);
        if (containedOpt.isEmpty()) return;
        FluidStack contained = containedOpt.get();
        if (contained.isEmpty()) return;

        if (!inputTank.isEmpty() && !FluidStack.isSameFluid(inputTank.getFluid(), contained)) return;

        if (inputTank.getFluidAmount() >= inputTank.getCapacity()) return;

        int space = inputTank.getCapacity() - inputTank.getFluidAmount();
        int toFill = Math.min(contained.getAmount(), space);
        if (toFill <= 0) return;

        ItemStack emptyContainer = getEmptyContainerFor(containerStack);
        if (emptyContainer.isEmpty()) return;

        ItemStack currentEmptySlot = itemHandler.getStackInSlot(inputEmptySlot);
        if (!currentEmptySlot.isEmpty()) {
            if (!ItemStack.isSameItem(currentEmptySlot, emptyContainer) ||
                currentEmptySlot.getCount() >= currentEmptySlot.getMaxStackSize()) {
                return;
            }
        }

        inputTank.fill(contained.copyWithAmount(toFill), IFluidHandler.FluidAction.EXECUTE);
        containerStack.shrink(1);
        if (currentEmptySlot.isEmpty()) {
            itemHandler.setStackInSlot(inputEmptySlot, emptyContainer.copy());
        } else {
            currentEmptySlot.grow(1);
        }
        setChanged();
    }

    private ItemStack getEmptyContainerFor(ItemStack filledContainer) {
        if (filledContainer.getItem() instanceof net.minecraft.world.item.BucketItem) {
            return new ItemStack(net.minecraft.world.item.Items.BUCKET);
        }
        IFluidHandlerItem handler = FluidUtil.getFluidHandler(filledContainer.copy()).orElse(null);
        if (handler != null) {
            handler.drain(Integer.MAX_VALUE, IFluidHandler.FluidAction.EXECUTE);
            ItemStack result = handler.getContainer();
            if (!result.isEmpty() && result.getItem() != filledContainer.getItem()) {
                return result;
            }
        }
        return ItemStack.EMPTY;
    }

    private void handleOutputCellSlot() {
        if (level == null || level.isClientSide) return;
        if (outputTank.isEmpty()) return;

        ItemStack emptyCellStack = itemHandler.getStackInSlot(outputEmptySlot);
        if (emptyCellStack.isEmpty() || !mio_icif_cells.isEmptyCell(emptyCellStack)) return;

        if (outputTank.getFluidAmount() < 1000) return;

        FluidStack outputFluid = outputTank.getFluid();
        ItemStack filledCell = mio_icif_cells.getFilledCellForFluidStack(outputFluid.getFluid());
        if (filledCell.isEmpty()) return;

        ItemStack currentOutputSlot = itemHandler.getStackInSlot(outputSlot);
        if (!currentOutputSlot.isEmpty()) {
            if (!ItemStack.isSameItem(currentOutputSlot, filledCell) ||
                currentOutputSlot.getCount() >= currentOutputSlot.getMaxStackSize()) {
                return;
            }
        }

        FluidStack drained = outputTank.drain(1000, IFluidHandler.FluidAction.EXECUTE);
        if (drained.getAmount() >= 1000) {
            emptyCellStack.shrink(1);
            if (currentOutputSlot.isEmpty()) {
                itemHandler.setStackInSlot(outputSlot, filledCell.copy());
            } else {
                currentOutputSlot.grow(1);
            }
            setChanged();
        }
    }

    @Override
    protected void saveAdditional(@NotNull CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("InputTank", inputTank.writeToNBT(registries, new CompoundTag()));
        tag.put("OutputTank", outputTank.writeToNBT(registries, new CompoundTag()));
    }

    @Override
    public void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("InputTank")) {
            inputTank.readFromNBT(registries, tag.getCompound("InputTank"));
        }
        if (tag.contains("OutputTank")) {
            outputTank.readFromNBT(registries, tag.getCompound("OutputTank"));
        }
    }

    public static void tick(Level level, BlockPos pos, BlockState state, mio_icif_oil_refinery_elc blockEntity) {
        if (level.isClientSide()) return;

        blockEntity.recalculateUpgradeStats();
        blockEntity.handleInputCellSlot();
        blockEntity.handleOutputCellSlot();

        if (!blockEntity.canWorkRedstone()) {
            blockEntity.stopWork();
        } else {
            if (blockEntity.canWork()) {
                blockEntity.doWork();
            } else {
                if (blockEntity.shouldResetProgress()) {
                    blockEntity.progress = 0;
                }
                blockEntity.stopWork();
            }
            blockEntity.updateProgress();
        }

        blockEntity.handleBatterySlot();
        blockEntity.handleAutomationUpgrades();
        blockEntity.setLit(blockEntity.isWorking());
        blockEntity.setChanged();
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.mio_icif.oil_refinery_elc");
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new com.singularity_iteration.mio_icif.Menu.Producer.OilRefineryElcMenu(containerId, playerInventory, this);
    }

    public ContainerData getContainerData() {
        return containerData;
    }

    private static class CombinedFluidHandler implements IFluidHandler {
        private final FluidTank inputTank;
        private final FluidTank outputTank;

        public CombinedFluidHandler(FluidTank inputTank, FluidTank outputTank) {
            this.inputTank = inputTank;
            this.outputTank = outputTank;
        }

        @Override
        public int getTanks() {
            return 2;
        }

        @Override
        public FluidStack getFluidInTank(int tank) {
            if (tank == 0) return inputTank.getFluid();
            if (tank == 1) return outputTank.getFluid();
            return FluidStack.EMPTY;
        }

        @Override
        public int getTankCapacity(int tank) {
            if (tank == 0) return inputTank.getCapacity();
            if (tank == 1) return outputTank.getCapacity();
            return 0;
        }

        @Override
        public boolean isFluidValid(int tank, FluidStack stack) {
            if (tank == 0) return inputTank.isFluidValid(stack);
            return false;
        }

        @Override
        public int fill(FluidStack resource, FluidAction action) {
            return inputTank.fill(resource, action);
        }

        @Override
        public FluidStack drain(FluidStack resource, FluidAction action) {
            FluidStack result = outputTank.drain(resource, action);
            if (result.isEmpty()) result = inputTank.drain(resource, action);
            return result;
        }

        @Override
        public FluidStack drain(int maxDrain, FluidAction action) {
            FluidStack result = outputTank.drain(maxDrain, action);
            if (result.isEmpty()) result = inputTank.drain(maxDrain, action);
            return result;
        }
    }
}