package com.singularity_iteration.mio_icif.Blocks.entity.producer;

import com.singularity_iteration.mio_icif.Blocks.entity.HUEntity.mio_icif_HeatU_Block;
import com.singularity_iteration.mio_icif.Blocks.entity.mio_icif_block_entities;
import com.singularity_iteration.mio_icif.Blocks.entity.slot.SlotLayout;
import com.singularity_iteration.mio_icif.Blocks.Environment.fluid.mio_icif_fluids;
import com.singularity_iteration.mio_icif.api.capability.IMioIcifCapabilities;
import com.singularity_iteration.mio_icif.Items.Cell.mio_icif_cells;
import com.singularity_iteration.mio_icif.recipe.blast_furnace.mio_icif_BlastFurnaceRecipe;
import com.singularity_iteration.mio_icif.recipe.blast_furnace.mio_icif_BlastFurnaceRecipes;
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
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@SuppressWarnings("null")
public class mio_icif_blast_furnace_advanced extends mio_icif_HeatU_Block {

    private static final SlotLayout LAYOUT = SlotLayout.builder()
        .input(1)
        .output(2)
        .extra(2)
        .upgrade(2)
        .build();

    public static final int INPUT_SLOT = 0;
    public static final int OUTPUT_SLOT_1 = 1;
    public static final int OUTPUT_SLOT_2 = 2;
    public static final int AIR_CELL_SLOT = 3;
    public static final int EMPTY_CELL_SLOT = 4;
    public static final int UPGRADE_SLOT_START = 5;
    public static final int UPGRADE_SLOT_COUNT = 2;
    public static final int TOTAL_SLOTS = 7;

    public static final int HEAT_CAPACITY = 50000;
    public static final int HEAT_STORAGE_CAPACITY = 50100;
    public static final int MAX_HEAT_RECEIVE = 1000;
    public static final int MAX_HEAT_EXTRACT = 0;
    public static final int MAX_TEMP = 1000;
    public static final float HEAT_LOSS_FACTOR = 0.0f;

    public static final int AIR_TANK_CAPACITY = 8000;
    public static final int AIR_PER_TICK = 1;

    public static final int BASE_OPERATION_TICKS = 6000;
    public static final int AIR_PER_OPERATION = 6000;

    public static final int BLAST_PROCESS_SPEED = 3;

    protected final FluidTank airTank;

    private int progress;
    private int maxProgress;

    private boolean isWorking;

    private final ContainerData dataAccess = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> (int) getHeatStored();
                case 1 -> getHeatCapacity();
                case 2 -> progress;
                case 3 -> maxProgress;
                case 4 -> airTank.getFluidAmount();
                case 5 -> airTank.getCapacity();
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            switch (index) {
                case 2 -> progress = value;
                case 3 -> maxProgress = value;
            }
        }

        @Override
        public int getCount() {
            return 6;
        }
    };

    public mio_icif_blast_furnace_advanced(BlockPos pos, BlockState state) {
        this(pos, state, mio_icif_block_entities.BLAST_FURNACE_ADVANCED_ENTITY_TYPE.get());
    }

    public mio_icif_blast_furnace_advanced(BlockPos pos, BlockState state, BlockEntityType<?> type) {
        super(type, pos, state, HEAT_STORAGE_CAPACITY, MAX_HEAT_RECEIVE, MAX_HEAT_EXTRACT,
              20, MAX_TEMP, HEAT_LOSS_FACTOR);

        this.progress = 0;
        this.maxProgress = BASE_OPERATION_TICKS;
        this.isWorking = false;

        this.slotLayout = LAYOUT;
        this.itemHandler = createItemHandler(LAYOUT);
        this.itemHandler.setValidator((slot, stack, slotType) -> mio_icif_blast_furnace_advanced.this.isItemValidForSlot(slot, stack));

        this.airTank = new FluidTank(AIR_TANK_CAPACITY, fluidStack ->
            fluidStack.getFluid() == mio_icif_fluids.AIR.get());
    }

    public boolean isItemValidForSlot(int slot, ItemStack stack) {
        if (isUpgradeSlot(slot)) {
            return stack.getItem() instanceof com.singularity_iteration.mio_icif.Items.Upgrade.mio_icif_upgrade;
        }
        return switch (slot) {
            case INPUT_SLOT -> isValidBlastFurnaceInput(stack);
            case OUTPUT_SLOT_1, OUTPUT_SLOT_2 -> false;
            case AIR_CELL_SLOT -> mio_icif_cells.isCellContainingFluid(stack, mio_icif_fluids.AIR.get());
            case EMPTY_CELL_SLOT -> false;
            default -> false;
        };
    }

    private boolean isValidBlastFurnaceInput(ItemStack stack) {
        if (level == null || stack.isEmpty()) return false;
        var recipeManager = level.getRecipeManager();
        var input = new com.singularity_iteration.mio_icif.recipe.blast_furnace.mio_icif_BlastFurnaceRecipeInput(stack);
        return recipeManager.getRecipeFor(mio_icif_BlastFurnaceRecipes.BLAST_FURNACE_TYPE.get(), input, level).isPresent();
    }

    private Direction getFrontSide() {
        BlockState state = getBlockState();
        if (state.hasProperty(net.minecraft.world.level.block.state.properties.BlockStateProperties.FACING)) {
            return state.getValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.FACING);
        }
        return Direction.NORTH;
    }

    @Override
    public IMioIcifCapabilities.IHeatStorage getHeatStorageCapability(@Nullable Direction side) {
        if (side == null || side == getFrontSide()) {
            return heatStorage;
        }
        return null;
    }

    public static void tick(Level level, BlockPos pos, BlockState state, mio_icif_blast_furnace_advanced blockEntity) {
        if (level.isClientSide()) {
            return;
        }

        blockEntity.recalculateUpgradeStats();

        blockEntity.heatup();

        blockEntity.handleAirCellSlot();

        mio_icif_BlastFurnaceRecipe recipe = blockEntity.getMatchingRecipe();
        int airPerTick = recipe != null
            ? Math.max(1, (int) Math.ceil(recipe.getAirCostPerTick() * blockEntity.getProcessingCostMultiplier())) : 0;

        if (blockEntity.canWorkRedstone() && recipe != null && blockEntity.isHot() && blockEntity.canOperate(recipe, airPerTick)) {
            blockEntity.isWorking = true;
            int progressPerTick = (1 + BLAST_PROCESS_SPEED) * blockEntity.getProcessingSpeedMultiplier();
            blockEntity.progress += progressPerTick;
            blockEntity.maxProgress = recipe.getDuration();

            blockEntity.airTank.drain(airPerTick, IFluidHandler.FluidAction.EXECUTE);

            if (blockEntity.progress >= recipe.getDuration()) {
                blockEntity.progress = 0;
                blockEntity.operate(recipe);
            }
        } else {
            blockEntity.isWorking = false;
            if (recipe == null || !blockEntity.isHot()) {
                blockEntity.progress = 0;
            }
        }

        boolean isLit = state.getValue(com.singularity_iteration.mio_icif.Blocks.Producer.mio_icif_block_blast_furnace_advanced.LIT);
        if (blockEntity.isWorking != isLit) {
            level.setBlock(pos, state.setValue(com.singularity_iteration.mio_icif.Blocks.Producer.mio_icif_block_blast_furnace_advanced.LIT, blockEntity.isWorking), 3);
        }

        blockEntity.handleAutomationUpgrades();

        blockEntity.setChanged();
    }

    private void heatup() {
        int heatRequested = 0;
        int gainHU = 0;

        boolean hasInput = !itemHandler.getStackInSlot(INPUT_SLOT).isEmpty();
        boolean hasProgress = progress >= 1;
        boolean hasRedstone = isRedstonePowered();

        if ((hasInput || hasProgress || hasRedstone) && heatStorage.getHeatStored() <= HEAT_CAPACITY) {
            heatRequested = (int) (HEAT_CAPACITY - heatStorage.getHeatStored() + 100);
        }

        if (heatRequested > 0) {
            Direction frontDir = getFrontSide();
            BlockPos frontPos = worldPosition.relative(frontDir);

            IMioIcifCapabilities.IHeatStorage frontHeat = level.getCapability(
                IMioIcifCapabilities.HEAT_STORAGE_BLOCK, frontPos, frontDir.getOpposite());

            if (frontHeat != null && frontHeat.canExtractHeat()) {
                long available = frontHeat.getHeatStored();
                long toDraw = Math.min(heatRequested, available);
                long drawn = frontHeat.extractHeat(toDraw, false);
                if (drawn > 0) {
                    long received = heatStorage.receiveHeat(drawn, false);
                    gainHU = (int) received;
                    if (received < drawn) {
                        frontHeat.receiveHeat(drawn - received, false);
                    }
                }
            }

            if (gainHU == 0) {
                heatStorage.consumeHeatInternal(1, false);
            }
        } else {
            heatStorage.consumeHeatInternal(1, false);
        }
    }

    private void handleAirCellSlot() {
        ItemStack cellStack = itemHandler.getStackInSlot(AIR_CELL_SLOT);
        if (cellStack.isEmpty() || !mio_icif_cells.isCellContainingFluid(cellStack, mio_icif_fluids.AIR.get())) {
            return;
        }

        int fillable = airTank.fill(new FluidStack(mio_icif_fluids.AIR.get(), 1000), IFluidHandler.FluidAction.SIMULATE);
        if (fillable < 1000) {
            return;
        }

        ItemStack currentEmptyCellSlot = itemHandler.getStackInSlot(EMPTY_CELL_SLOT);
        ItemStack emptyCell = mio_icif_cells.getEmptyCellForStack(cellStack);
        if (emptyCell.isEmpty()) emptyCell = new ItemStack(mio_icif_cells.CELL_EMPTY.get());
        if (!currentEmptyCellSlot.isEmpty()) {
            if (!ItemStack.isSameItem(currentEmptyCellSlot, emptyCell) ||
                currentEmptyCellSlot.getCount() >= currentEmptyCellSlot.getMaxStackSize()) {
                return;
            }
        }

        int filled = airTank.fill(new FluidStack(mio_icif_fluids.AIR.get(), 1000), IFluidHandler.FluidAction.EXECUTE);
        if (filled >= 1000) {
            cellStack.shrink(1);
            if (currentEmptyCellSlot.isEmpty()) {
                itemHandler.setStackInSlot(EMPTY_CELL_SLOT, emptyCell);
            } else {
                currentEmptyCellSlot.grow(1);
            }
            setChanged();
        }
    }

    private boolean isRedstonePowered() {
        if (level == null) return false;
        return level.hasNeighborSignal(worldPosition);
    }

    public boolean isHot() {
        return heatStorage.getHeatStored() >= HEAT_CAPACITY;
    }

    @Nullable
    private mio_icif_BlastFurnaceRecipe getMatchingRecipe() {
        ItemStack input = itemHandler.getStackInSlot(INPUT_SLOT);
        if (input.isEmpty() || level == null) {
            return null;
        }

        var recipeManager = level.getRecipeManager();
        var recipeInput = new com.singularity_iteration.mio_icif.recipe.blast_furnace.mio_icif_BlastFurnaceRecipeInput(input);
        return recipeManager.getRecipeFor(mio_icif_BlastFurnaceRecipes.BLAST_FURNACE_TYPE.get(), recipeInput, level).map(r -> r.value()).orElse(null);
    }

    private boolean canOperate(mio_icif_BlastFurnaceRecipe recipe, int airPerTick) {
        if (airTank.getFluidAmount() < airPerTick) {
            return false;
        }

        if (!canAddItem(OUTPUT_SLOT_1, recipe.getResult())) {
            return false;
        }
        if (!canAddItem(OUTPUT_SLOT_2, recipe.getSecondaryResult())) {
            return false;
        }

        return true;
    }

    private boolean canAddItem(int slot, ItemStack stack) {
        ItemStack current = itemHandler.getStackInSlot(slot);
        if (current.isEmpty()) {
            return true;
        }
        if (!ItemStack.isSameItem(current, stack)) {
            return false;
        }
        return current.getCount() + stack.getCount() <= current.getMaxStackSize();
    }

    private void operate(mio_icif_BlastFurnaceRecipe recipe) {
        ItemStack input = itemHandler.getStackInSlot(INPUT_SLOT);
        if (input.isEmpty()) {
            return;
        }

        input.shrink(recipe.getIngredientCount());

        addItemToSlot(OUTPUT_SLOT_1, recipe.getResult().copy());
        addItemToSlot(OUTPUT_SLOT_2, recipe.getSecondaryResult().copy());

        setChanged();
    }

    private void addItemToSlot(int slot, ItemStack stack) {
        ItemStack current = itemHandler.getStackInSlot(slot);
        if (current.isEmpty()) {
            itemHandler.setStackInSlot(slot, stack);
        } else if (ItemStack.isSameItem(current, stack)) {
            current.grow(stack.getCount());
        }
    }

    @Override
    protected int[] getOutputSlots() {
        return new int[]{OUTPUT_SLOT_1, OUTPUT_SLOT_2, EMPTY_CELL_SLOT};
    }

    public ItemStackHandler getItemHandler() {
        return itemHandler;
    }

    public IFluidHandler getAirTank() {
        return airTank;
    }

    public int getAirAmount() {
        return airTank.getFluidAmount();
    }

    public int getAirCapacity() {
        return airTank.getCapacity();
    }

    public int getProgress() {
        return progress;
    }

    public int getMaxProgress() {
        return maxProgress;
    }

    public long getHeatStored() {
        return Math.min(heatStorage.getHeatStored(), HEAT_CAPACITY);
    }

    public int getHeatCapacity() {
        return HEAT_CAPACITY;
    }

    public boolean isWorking() {
        return isWorking;
    }

    public net.neoforged.neoforge.items.IItemHandler getItemHandlerCapability(@Nullable Direction side) {
        return new SidedItemHandler(side);
    }

    private boolean canInsertItem(int slot, ItemStack stack, @Nullable Direction side) {
        if (side == null) return true;

        if (side == getFrontSide()) return false;

        if (side == Direction.DOWN) return false;

        if (slot == INPUT_SLOT && isValidBlastFurnaceInput(stack)) {
            return true;
        }

        if (side.getAxis().isHorizontal() && slot == AIR_CELL_SLOT && mio_icif_cells.isCellContainingFluid(stack, mio_icif_fluids.AIR.get())) {
            return true;
        }

        return false;
    }

    private boolean canExtractItem(int slot, @Nullable Direction side) {
        if (side == null) return true;

        if (side == getFrontSide()) return false;

        if (slot == OUTPUT_SLOT_1 || slot == OUTPUT_SLOT_2) {
            return true;
        }

        if (slot == EMPTY_CELL_SLOT) {
            return true;
        }

        return false;
    }

    @Override
    public IFluidHandler getFluidHandlerCapability(@Nullable Direction side) {
        return airTank;
    }

    private class SidedItemHandler implements net.neoforged.neoforge.items.IItemHandler {
        private final Direction side;

        public SidedItemHandler(@Nullable Direction side) {
            this.side = side;
        }

        @Override
        public int getSlots() {
            return itemHandler.getSlots();
        }

        @Override
        public @NotNull ItemStack getStackInSlot(int slot) {
            return itemHandler.getStackInSlot(slot);
        }

        @Override
        public @NotNull ItemStack insertItem(int slot, @NotNull ItemStack stack, boolean simulate) {
            if (!canInsertItem(slot, stack, side)) {
                return stack;
            }
            return itemHandler.insertItem(slot, stack, simulate);
        }

        @Override
        public @NotNull ItemStack extractItem(int slot, int amount, boolean simulate) {
            if (!canExtractItem(slot, side)) {
                return ItemStack.EMPTY;
            }
            return itemHandler.extractItem(slot, amount, simulate);
        }

        @Override
        public int getSlotLimit(int slot) {
            return itemHandler.getSlotLimit(slot);
        }

        @Override
        public boolean isItemValid(int slot, @NotNull ItemStack stack) {
            return itemHandler.isItemValid(slot, stack);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("inventory", itemHandler.serializeNBT(registries));
        tag.put("airTank", airTank.writeToNBT(registries, new CompoundTag()));
        tag.putInt("progress", progress);
        tag.putBoolean("isWorking", isWorking);
    }

    @Override
    public void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("inventory")) {
            itemHandler.deserializeNBT(registries, tag.getCompound("inventory"));
        }
        if (tag.contains("airTank")) {
            airTank.readFromNBT(registries, tag.getCompound("airTank"));
        }
        progress = tag.getInt("progress");
        isWorking = tag.getBoolean("isWorking");
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.mio_icif.blast_furnace_advanced");
    }

    public ContainerData getContainerData() {
        return dataAccess;
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new com.singularity_iteration.mio_icif.Menu.Producer.BlastFurnaceAdvancedMenu(containerId, playerInventory, this.getItemHandler(), this.dataAccess);
    }
}