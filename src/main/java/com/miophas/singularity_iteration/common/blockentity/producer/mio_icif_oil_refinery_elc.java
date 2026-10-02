package com.miophas.singularity_iteration.common.blockentity.producer;

import com.miophas.singularity_iteration.common.registry.mio_icif_block_entities;
import com.miophas.singularity_iteration.common.item.cell.mio_icif_cells;
import com.miophas.singularity_iteration.core.api.MioIcifAPI;
import com.miophas.singularity_iteration.core.api.energy.ICableTier;
import com.miophas.singularity_iteration.core.prefab.blockentity.GenericMachineBlockEntity;
import com.miophas.singularity_iteration.core.api.machine.ISlotLayout;
import com.miophas.singularity_iteration.core.api.machine.builder.IElectricMachineBuilder;
import com.miophas.singularity_iteration.core.api.machine.builder.IMachineBuilderAPI;
import com.miophas.singularity_iteration.core.api.recipe.IRecipeAPI;
import com.miophas.singularity_iteration.core.runtime.energy.ContainerToTank;
import com.miophas.singularity_iteration.core.runtime.processing.OwnedFluidConversion;
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
    private final IFluidHandler combinedFluidHandler;

    private int overclockerCount = 0;
    private int fluidPerCycle = 1;
    private String paidRecipe = "";
    private FluidStack paidInput = FluidStack.EMPTY, paidOutput = FluidStack.EMPTY;
    private long paidRate;
    private int paidCycle, paidDuration;

    private void refreshPaidRecipe() {
        var recipe = findRecipe();
        if (recipe.isEmpty()) return;
        String currentId = recipe.get().id().toString();
        var input = inputTank.getFluid().copyWithAmount(1);
        var output = MioIcifAPI.instance().getRecipeAPI().getFluidRefiningOutputFluid(recipe.get()).copy();
        if (!paidRecipe.isEmpty() && (!paidRecipe.equals(currentId) || !FluidStack.matches(paidInput, input)
                || !FluidStack.matches(paidOutput, output) || paidRate != getEffectiveEnergyPerTick()
                || paidCycle != fluidPerCycle || paidDuration != maxProgress)) progress = 0;
        paidRecipe = currentId; paidInput = input; paidOutput = output;
        paidRate = getEffectiveEnergyPerTick(); paidCycle = fluidPerCycle; paidDuration = maxProgress;
    }

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
        this.inputTank = new FluidTank(INPUT_TANK_CAPACITY) {
            @Override protected void onContentsChanged() { ContainerToTank.markUnsaved(mio_icif_oil_refinery_elc.this); }
        };
        this.outputTank = new FluidTank(OUTPUT_TANK_CAPACITY) {
            @Override protected void onContentsChanged() { ContainerToTank.markUnsaved(mio_icif_oil_refinery_elc.this); }
        };
        this.combinedFluidHandler = com.miophas.singularity_iteration.core.prefab.fluid.FluidTankGroup
            .inputOutput(inputTank, outputTank);
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
            .useStandardLayout(2, 2, true, 4)
            .setSupportsUpgrades(true)
            .setSupportsFluids(true)
            .addFluidTank(INPUT_TANK_CAPACITY)
            .addFluidTank(OUTPUT_TANK_CAPACITY)
            .setMachineType(com.miophas.singularity_iteration.core.api.machine.IMachineAPI.MachineType.CUSTOM)
            .withEntityType(mio_icif_block_entities.OIL_REFINERY_ELC_ENTITY_TYPE.get());

        builder.buildAndRegister("mio_icif");
        return builder.getConfiguration();
    }

    @Nullable
    @Override
    public IFluidHandler getFluidHandlerCapability(@Nullable Direction side) {
        return combinedFluidHandler;
    }

    public FluidTank getInputTank() {
        return inputTank;
    }

    public FluidTank getOutputTank() {
        return outputTank;
    }

    public FluidStack getInputFluid() {
        return inputTank.getFluid().copy();
    }

    public FluidStack getOutputFluid() {
        return outputTank.getFluid().copy();
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
        // Base producer owns the immutable upgrade snapshot; do not rescan the inventory here.
        int updatedCount = upgradeStats.getOverclockerCount();
        if (updatedCount == overclockerCount) return;
        overclockerCount = updatedCount;
        fluidPerCycle = (int) Math.min(Integer.MAX_VALUE, Math.max(1L, (long) overclockerCount + 1));
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
        return (progress >= maxProgress || hasEnoughEnergy()) && prepareOperation().isPresent();
    }

    private Optional<OwnedFluidConversion.Prepared> prepareOperation() {
        var recipe = findRecipe();
        if (recipe.isEmpty()) return Optional.empty();
        var product = MioIcifAPI.instance().getRecipeAPI().getFluidRefiningOutputFluid(recipe.get());
        long amount = (long) product.getAmount() * fluidPerCycle;
        if (product.isEmpty() || amount <= 0 || amount > Integer.MAX_VALUE) return Optional.empty();
        // Existing SI refining data has no separate input amount: retain its 1:1 fluid-volume setting.
        return OwnedFluidConversion.prepare(inputTank, inputTank.getFluid().copyWithAmount((int) amount),
            outputTank, product.copyWithAmount((int) amount), itemHandler, -1, ItemStack.EMPTY);
    }

    @Override
    protected void doWork() {
        if (prepareOperation().isEmpty()) { stopWork(); return; }
        if (progress < maxProgress) {
            if (getEffectiveEnergyPerTick() <= 0 || !hasEnoughEnergy() || !consumeEnergy()) { stopWork(); return; }
            progress = (int) Math.min(maxProgress, (long) progress + getProgressPerTick());
        }
        isWorking = true;
        if (progress >= maxProgress) finishRefining();
    }

    @Override
    protected void updateProgress() { /* doWork accounts for each paid tick exactly once. */ }

    private void finishRefining() {
        var operation = prepareOperation();
        if (operation.isEmpty()) { stopWork(); return; }
        int completed = progress;
        progress = 0;
        if (!operation.get().commit()) { progress = completed; stopWork(); return; }
        finishWork();
        setChanged();
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
        var single = cellStack.copyWithCount(1);
        if (ContainerToTank.transfer(itemHandler, inputSlot, inputEmptySlot, inputTank,
                mio_icif_cells.getCellFluid(single), mio_icif_cells.getEmptyCellForStack(single))) setChanged();
    }

    private void handleInputCellFromGenericContainer(ItemStack containerStack) {
        // Operate on a detached single container. Preserve the original until its complete contents fit.
        var handler = FluidUtil.getFluidHandler(containerStack.copyWithCount(1)).orElse(null);
        if (handler == null) return;
        var quoted = handler.drain(Integer.MAX_VALUE, IFluidHandler.FluidAction.SIMULATE);
        if (quoted.isEmpty() || inputTank.fill(quoted, IFluidHandler.FluidAction.SIMULATE) != quoted.getAmount()) return;
        var extracted = handler.drain(quoted, IFluidHandler.FluidAction.EXECUTE);
        if (!FluidStack.matches(quoted, extracted)) return;
        for (int tank = 0; tank < handler.getTanks(); tank++) if (!handler.getFluidInTank(tank).isEmpty()) return;
        var empty = handler.getContainer();
        if (empty.isEmpty() || empty.getCount() != 1) return;
        if (ContainerToTank.transfer(itemHandler, inputSlot, inputEmptySlot, inputTank, extracted, empty)) setChanged();
    }



    private void handleOutputCellSlot() {
        if (level == null || level.isClientSide || outputTank.isEmpty()
                || !mio_icif_cells.isEmptyCell(itemHandler.getStackInSlot(outputEmptySlot))) return;
        var filled = mio_icif_cells.getFilledCellForFluidStack(outputTank.getFluid().getFluid());
        if (filled.isEmpty()) return;
        var content = mio_icif_cells.getCellFluid(filled.copyWithCount(1));
        if (ContainerToTank.drainToContainer(itemHandler, outputEmptySlot, outputSlot, outputTank, content, filled)) setChanged();
    }

    @Override
    protected void saveAdditional(@NotNull CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putString("scex_paid_recipe", paidRecipe);
        tag.put("scex_paid_input", paidInput.saveOptional(registries));
        tag.put("scex_paid_output", paidOutput.saveOptional(registries));
        tag.putLong("scex_paid_rate", paidRate);
        tag.putInt("scex_paid_cycle", paidCycle);
        tag.putInt("scex_paid_duration", paidDuration);
        tag.put("InputTank", inputTank.writeToNBT(registries, new CompoundTag()));
        tag.put("OutputTank", outputTank.writeToNBT(registries, new CompoundTag()));
    }

    @Override
    public void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        paidRecipe = tag.getString("scex_paid_recipe");
        paidInput = FluidStack.parseOptional(registries, tag.getCompound("scex_paid_input"));
        paidOutput = FluidStack.parseOptional(registries, tag.getCompound("scex_paid_output"));
        paidRate = Math.max(0, tag.getLong("scex_paid_rate"));
        paidCycle = Math.max(0, tag.getInt("scex_paid_cycle"));
        paidDuration = Math.max(0, tag.getInt("scex_paid_duration"));
        progress = Math.max(0, Math.min(maxProgress, progress));
        if (tag.contains("InputTank")) {
            inputTank.readFromNBT(registries, tag.getCompound("InputTank"));
        }
        if (tag.contains("OutputTank")) {
            outputTank.readFromNBT(registries, tag.getCompound("OutputTank"));
        }
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag(); saveAdditional(tag, registries); return tag;
    }

    @Override
    public void handleUpdateTag(CompoundTag tag, HolderLookup.Provider registries) { loadAdditional(tag, registries); }

    public static void tick(Level level, BlockPos pos, BlockState state, mio_icif_oil_refinery_elc blockEntity) {
        if (!(level instanceof net.minecraft.server.level.ServerLevel server)
                || !server.getServer().isSameThread() || blockEntity.isRemoved()) return;

        blockEntity.recalculateUpgradeStats();
        blockEntity.handleInputCellSlot();
        blockEntity.handleOutputCellSlot();
        blockEntity.refreshPaidRecipe();

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
        return new com.miophas.singularity_iteration.common.menu.producer.OilRefineryElcMenu(containerId, playerInventory, this);
    }

    public ContainerData getContainerData() {
        return containerData;
    }

}
