package com.miophas.singularity_iteration.common.blockentity.producer;

import com.miophas.singularity_iteration.common.registry.mio_icif_block_entities;
import com.miophas.singularity_iteration.core.prefab.blockentity.AbstractProcessingMachineBlockEntity;
import com.miophas.singularity_iteration.core.prefab.inventory.SlotLayout;
import com.miophas.singularity_iteration.core.api.energy.storage.CableTier;
import com.miophas.singularity_iteration.common.recipe.centrifuge.mio_icif_CentrifugeRecipe;
import com.miophas.singularity_iteration.common.recipe.centrifuge.mio_icif_CentrifugeRecipeInput;
import com.miophas.singularity_iteration.common.recipe.centrifuge.mio_icif_CentrifugeRecipes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;

/**
 * Block entity for the electric centrifuge.
 * It uses electricity to make heat and uses the heat to process items.
 * Slots: 1 input + 3 outputs + 1 battery + 4 upgrades = 9 slots.
 *
 * Heat rules:
 * - The machine stores a maximum of 5000 HU.
 * - 1 EU makes 1 HU.
 * - Processing starts only when the heat reaches 5000 HU.
 * - A recipe uses 48 EU/t and takes 500 ticks.
 * - The machine accepts MV-tier energy.
 */
@SuppressWarnings("null")
public class mio_icif_centrifuge_elc extends AbstractProcessingMachineBlockEntity {
    // Per-call-site recipe memo: canWork() runs every tick, getRecipeFor is a linear scan.
    private final com.miophas.singularity_iteration.core.runtime.processing.RecipeLookupCache<com.miophas.singularity_iteration.common.recipe.centrifuge.mio_icif_CentrifugeRecipeInput, com.miophas.singularity_iteration.common.recipe.centrifuge.mio_icif_CentrifugeRecipe> recipeCache1 = new com.miophas.singularity_iteration.core.runtime.processing.RecipeLookupCache<>(mio_icif_CentrifugeRecipes.CENTRIFUGE_TYPE);
    private final com.miophas.singularity_iteration.core.runtime.processing.RecipeLookupCache<com.miophas.singularity_iteration.common.recipe.centrifuge.mio_icif_CentrifugeRecipeInput, com.miophas.singularity_iteration.common.recipe.centrifuge.mio_icif_CentrifugeRecipe> recipeCache2 = new com.miophas.singularity_iteration.core.runtime.processing.RecipeLookupCache<>(mio_icif_CentrifugeRecipes.CENTRIFUGE_TYPE);

    private static final SlotLayout LAYOUT = SlotLayout.builder()
        .input(1)
        .output(3)
        .battery()
        .upgrade(4)
        .build();

    // Total number of slots.
    public static final int SLOT_COUNT = 9;
    // Index of the input slot.
    public static final int INPUT_SLOT = 0;
    // Indexes of the 3 output slots.
    public static final int OUTPUT_SLOT_1 = 1;
    public static final int OUTPUT_SLOT_2 = 2;
    public static final int OUTPUT_SLOT_3 = 3;
    // Index of the battery slot.
    public static final int BATTERY_SLOT = 4;
    // First index of the 4 upgrade slots.
    public static final int UPGRADE_SLOT_START = 5;

    // Default energy settings. The values match the IC2 centrifuge.
    public static final long DEFAULT_CAPACITY = 24000L; // 48 EU/t x 500 ticks = 24000 EU
    public static final long DEFAULT_MAX_RECEIVE = 128L; // MV tier input limit
    public static final long DEFAULT_MAX_EXTRACT = 0L;
    public static final int DEFAULT_WORK_TIME = 500; // 25 seconds = 500 ticks
    public static final long DEFAULT_ENERGY_PER_TICK = 48L; // 48 EU/t, the same as the IC2 centrifuge

    // Heat system settings.
    public static final int MAX_HEAT = 5000; // Maximum heat: 5000 HU
    public static final int HEAT_PER_EU = 1; // 1 EU makes 1 HU
    public static final int HEAT_GENERATION_RATE = 4; // Heat made per tick (HU)
    public static final int HEAT_COOLING_RATE = 2; // Heat lost per tick when the machine has no power (HU)
    public static final int MIN_HEAT_FOR_WORK = 5000; // Minimum heat to start processing (HU)

    // Current stored heat in HU.
    protected int heatStorage;

    /**
     * Constructor for the BlockEntityType.Builder.
     */
    public mio_icif_centrifuge_elc(BlockPos pos, BlockState state) {
        this(pos, state, mio_icif_block_entities.CENTRIFUGE_ELC_ENTITY_TYPE.get());
    }

    public mio_icif_centrifuge_elc(BlockPos pos, BlockState state, BlockEntityType<?> type) {
        super(pos, state, type,
            DEFAULT_CAPACITY,
            DEFAULT_MAX_RECEIVE,
            DEFAULT_MAX_EXTRACT,
            DEFAULT_WORK_TIME,
            LAYOUT,
            DEFAULT_ENERGY_PER_TICK,
            CableTier.MV);

        this.heatStorage = 0;
    }



    @Override
    public net.minecraft.network.chat.Component getDisplayName() {
        return net.minecraft.network.chat.Component.translatable("container.mio_icif.centrifuge_elc");
    }

    @Override
    public boolean isItemValidForSlot(int slot, ItemStack stack) {
        if (slot == INPUT_SLOT) {
            // Input slot: accept only items that have a centrifuge recipe.
            return isProcessable(stack);
        } else if (slot >= OUTPUT_SLOT_1 && slot <= OUTPUT_SLOT_3) {
            // Output slots: players cannot put items here.
            return false;
        } else if (slot == BATTERY_SLOT) {
            // Battery slot: accept only batteries.
            return isBattery(stack);
        } else if (slot >= UPGRADE_SLOT_START && slot < UPGRADE_SLOT_START + 4) {
            // Upgrade slots: accept only upgrade items.
            return getItemAPI().isUpgrade(stack);
        }
        return false;
    }

    /**
     * Checks if the item can be processed.
     * The check uses the recipe system.
     */
    private boolean isProcessable(ItemStack stack) {
        if (stack.isEmpty() || level == null) {
            return false;
        }
        mio_icif_CentrifugeRecipeInput recipeInput = new mio_icif_CentrifugeRecipeInput(stack);
        // Look for a matching recipe.
        Optional<RecipeHolder<mio_icif_CentrifugeRecipe>> recipe = recipeCache1.find(level, recipeInput);
        return recipe.isPresent();
    }

    /**
     * Gets the centrifuge recipe that matches the input item.
     */
    private Optional<mio_icif_CentrifugeRecipe> getRecipe(ItemStack input) {
        if (level == null || input.isEmpty()) {
            return Optional.empty();
        }
        mio_icif_CentrifugeRecipeInput recipeInput = new mio_icif_CentrifugeRecipeInput(input);
        return recipeCache2.find(level, recipeInput)
            .map(RecipeHolder::value);
    }



    /**
     * Gets the slots that hoppers can access from a side.
     * All sides can access the same slots.
     */
    @Override
    protected int[] getSlotsForDirection(Direction side) {
        // All sides can access the same slots.
        return new int[]{INPUT_SLOT, BATTERY_SLOT, OUTPUT_SLOT_1, OUTPUT_SLOT_2, OUTPUT_SLOT_3};
    }

    @Override
    protected int[] getInputSlots() {
        return new int[]{INPUT_SLOT};
    }

    @Override
    protected int[] getOutputSlots() {
        return new int[]{OUTPUT_SLOT_1, OUTPUT_SLOT_2, OUTPUT_SLOT_3};
    }

    @Override
    protected int getBatterySlot() {
        return BATTERY_SLOT;
    }

    /**
     * Checks if automation can insert the item into the slot.
     */
    @Override
    protected boolean canInsertItem(int slot, ItemStack stack, @Nullable Direction side) {
        // Automation cannot insert into output slots.
        if (slot >= OUTPUT_SLOT_1 && slot <= OUTPUT_SLOT_3) {
            return false;
        }

        // Battery slot: accept only batteries.
        if (slot == BATTERY_SLOT) {
            return isBattery(stack);
        }

        // Input slot: accept only items that have a recipe.
        if (slot == INPUT_SLOT) {
            return isProcessable(stack);
        }

        // Automation cannot insert into other slots, for example upgrade slots.
        return false;
    }

    /**
     * Checks if automation can extract the item from the slot.
     * Automation can extract from the 3 output slots on all sides.
     */
    @Override
    protected boolean canExtractItem(int slot, @Nullable Direction side) {
        return slot >= OUTPUT_SLOT_1 && slot <= OUTPUT_SLOT_3;
    }

    /**
     * Checks if the progress must go back to zero.
     * This occurs when the input slot is empty or the input has no recipe.
     */
    @Override
    protected boolean shouldResetProgress() {
        ItemStack input = itemHandler.getStackInSlot(INPUT_SLOT);
        // Input slot is empty: reset the progress.
        if (input.isEmpty()) {
            return true;
        }
        // No recipe matches the input: reset the progress.
        return !isProcessable(input);
    }

    @Override
    protected boolean canWork() {
        var input = itemHandler.getStackInSlot(INPUT_SLOT);
        if (input.isEmpty() || !hasEnoughEnergy() || heatStorage < MIN_HEAT_FOR_WORK) return false;
        return com.miophas.singularity_iteration.core.runtime.processing.RecipeSlots.prepare(itemHandler, INPUT_SLOT, 1,
            new int[]{OUTPUT_SLOT_1, OUTPUT_SLOT_2, OUTPUT_SLOT_3}, getProcessingResults(input)).isPresent();
    }

    /**
     * Gets the output items of the recipe for the input.
     * @param input the input item
     * @return the list of output items, or an empty list if no recipe matches
     */
    private List<ItemStack> getProcessingResults(ItemStack input) {
        Optional<mio_icif_CentrifugeRecipe> recipe = getRecipe(input);
        if (recipe.isPresent()) {
            return recipe.get().getAllResults();
        }
        // No recipe matches: return an empty list.
        return java.util.Collections.emptyList();
    }

    @Override
    protected void doWork() {
        // Use energy. Stop if the machine does not have sufficient energy.
        if (!consumeEnergy()) {
            stopWork();
            return;
        }

        isWorking = true;

        // Finish the recipe when the progress is complete.
        if (progress >= maxProgress) {
            finishProcessing();
        }
    }

    /**
     * Completes the recipe and puts the outputs into the 3 output slots.
     */
    private void finishProcessing() {
        var input = itemHandler.getStackInSlot(INPUT_SLOT);
        var plan = com.miophas.singularity_iteration.core.runtime.processing.RecipeSlots.prepare(itemHandler, INPUT_SLOT, 1,
            new int[]{OUTPUT_SLOT_1, OUTPUT_SLOT_2, OUTPUT_SLOT_3}, getProcessingResults(input));
        if (plan.isEmpty()) { stopWork(); return; }
        int completedProgress = progress;
        progress = 0; // An inventory observer must never save completed work against the next input.
        if (!plan.get().commit()) { progress = completedProgress; stopWork(); return; }
        finishWork();
        if (canWork()) isWorking = true;
    }

    /**
     * Tries to put a result into an output slot.
     * @param slot the output slot index
     * @param result the item to put into the slot
     * @return true if the slot accepted the item
     */
    private boolean tryPlaceResult(int slot, ItemStack result) {
        if (result.isEmpty()) {
            return true; // An empty result counts as a success
        }
        
        ItemStack currentOutput = itemHandler.getStackInSlot(slot);
        if (currentOutput.isEmpty()) {
            itemHandler.setStackInSlot(slot, result.copy());
            return true;
        } else if (ItemStack.isSameItem(currentOutput, result) && 
                   ItemStack.isSameItemSameComponents(currentOutput, result)) {
            int newCount = currentOutput.getCount() + result.getCount();
            if (newCount <= currentOutput.getMaxStackSize()) {
                currentOutput.grow(result.getCount());
                return true;
            }
        }
        return false;
    }

    /**
     * Runs one server tick.
     */
    public static void tick(Level level, BlockPos pos, BlockState state, mio_icif_centrifuge_elc blockEntity) {
        if (level.isClientSide()) {
            return;
        }

        // Update the heat system.
        blockEntity.updateHeat();

        // The parent tick does the processing, progress and battery slot logic.
        AbstractProcessingMachineBlockEntity.tick(level, pos, state, blockEntity);

        // Update the LIT block state to show if the machine is working.
        boolean isLit = state.getValue(com.miophas.singularity_iteration.common.block.producer.mio_icif_block_centrifuge_elc.LIT);
        if (blockEntity.isWorking() != isLit) {
            level.setBlock(pos, state.setValue(com.miophas.singularity_iteration.common.block.producer.mio_icif_block_centrifuge_elc.LIT, blockEntity.isWorking()), 3);
        }
    }

    /**
     * Updates the heat system.
     * - With power, the machine converts energy into heat until it reaches the maximum.
     * - Without power, the machine loses heat each tick.
     */
    private void updateHeat() {
        if (!hasEnoughEnergy()) {
            if (heatStorage > 0) {
                heatStorage = Math.max(0, heatStorage - HEAT_COOLING_RATE);
                setChanged();
            }
            return;
        }

        int maxHeatToGenerate = Math.min(HEAT_GENERATION_RATE, MAX_HEAT - heatStorage);
        if (maxHeatToGenerate > 0) {
            long energyAvailable = Math.min(energyStorage.getAmount(), maxHeatToGenerate);
            int heatGenerated = (int) energyAvailable;

            if (heatGenerated > 0) {
                apiUseEnergy(heatGenerated, false);
                heatStorage += heatGenerated;
                setChanged();
            }
        }
    }



    /**
     * Gets the current stored heat in HU.
     */
    public int getHeatStorage() {
        return heatStorage;
    }

    /**
     * Gets the maximum heat in HU.
     */
    public int getMaxHeat() {
        return MAX_HEAT;
    }

    /**
     * Gets the heat as a percentage (0-100) for the GUI.
     */
    public int getHeatProgress() {
        return (heatStorage * 100) / MAX_HEAT;
    }

    /**
     * Gets the item in the input slot.
     */
    public ItemStack getInputItem() {
        return itemHandler.getStackInSlot(INPUT_SLOT);
    }

    /**
     * Gets the item in an output slot.
     * @param index the output index (0-2)
     */
    public ItemStack getOutputItem(int index) {
        if (index >= 0 && index < 3) {
            return itemHandler.getStackInSlot(OUTPUT_SLOT_1 + index);
        }
        return ItemStack.EMPTY;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        // Save the stored heat.
        tag.putInt("heat_storage", heatStorage);
    }

    @Override
    public void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        // Load the stored heat.
        if (tag.contains("heat_storage")) {
            heatStorage = tag.getInt("heat_storage");
        }
    }
}