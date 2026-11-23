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



    // 0.1.10.38: constant slot lists (they were new arrays on every call, several per tick); never modify them.
    private static final int[] SIDE_SLOTS = {INPUT_SLOT, BATTERY_SLOT, OUTPUT_SLOT_1, OUTPUT_SLOT_2, OUTPUT_SLOT_3}, INPUT_SLOTS = {INPUT_SLOT}, OUTPUT_SLOTS = {OUTPUT_SLOT_1, OUTPUT_SLOT_2, OUTPUT_SLOT_3};

    /**
     * Gets the slots that hoppers can access from a side.
     * All sides can access the same slots.
     */
    @Override
    protected int[] getSlotsForDirection(Direction side) {
        // All sides can access the same slots.
        return SIDE_SLOTS;
    }

    @Override
    protected int[] getInputSlots() {
        return INPUT_SLOTS;
    }

    @Override
    protected int[] getOutputSlots() {
        return OUTPUT_SLOTS;
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
        if (input.isEmpty() || !hasEnoughEnergy() || heatStorage < requiredHeat()) return false;   // 0.1.11.114: the recipe's own minimum heat
        return com.miophas.singularity_iteration.core.runtime.processing.RecipeSlots.fits(itemHandler, INPUT_SLOT, 1,
            OUTPUT_SLOTS, getProcessingResults(input));
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
        // 0.1.11.114: below full rotor speed the centrifuge runs that share of the ticks (no energy, no progress otherwise)
        rotorCarry += rotorSpeed;
        advancedThisTick = rotorCarry >= 1;
        if (!advancedThisTick) { isWorking = true; return; }
        rotorCarry -= 1;
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
            OUTPUT_SLOTS, getProcessingResults(input));
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
        blockEntity.turnRotor();   // 0.1.11.114

        // Update the LIT block state to show if the machine is working.
        blockEntity.syncLit(level, pos, state, com.miophas.singularity_iteration.common.block.producer.mio_icif_block_centrifuge_elc.LIT);   // 0.1.10.38: off after LIT_OFF_DELAY idle ticks
    }

    /**
     * Updates the heat system.
     * - With power, the machine converts energy into heat until it reaches the maximum.
     * - Without power, the machine loses heat each tick.
     */
    private void updateHeat() {
        // 0.1.11.114: the heater runs only while the centrifuge has something to do (input with a recipe and room for
        // the output, not stopped by redstone or a remote switch); it used to heat whenever it had power
        int before = heatStorage;
        if (wantsHeat() && heatStorage < MAX_HEAT && hasEnoughEnergy()) {
            int heat = (int) Math.min(Math.min(HEAT_GENERATION_RATE, MAX_HEAT - heatStorage), energyStorage.getAmount());
            if (heat > 0) {
                apiUseEnergy(heat, false);
                heatStorage += heat;
                heatChanged(before);
            }
        } else if (!wantsHeat() && heatStorage > 0) {
            heatStorage = Math.max(0, heatStorage - HEAT_COOLING_RATE);
            heatChanged(before);
        }
    }

    // ---- 0.1.11.114 (processing overhaul M7): the recipe's heat and the centrifuge rotor ----

    /**
     * A thermal centrifuge heats the charge so its minerals separate, then spins it so they settle by density. Each
     * recipe says how hot (clay 250 HU, cobblestone 100, the crushed ores 500 to 4,000; 5,000 when it says nothing): the
     * centrifuge starts once that heat is reached instead of always waiting for the full 5,000 HU (about a minute). The
     * rotor has inertia like the cyclone macerator's: +10 % a second while it works, −5 % a second otherwise; below full
     * speed it runs that share of the ticks, so the energy per operation stays the same. At full speed it turns 12,000 rpm
     * at a 10 cm radius, about 16,000 g.
     */
    public static final double SPIN_UP_PER_TICK = 0.10 / 20, COAST_PER_TICK = 0.05 / 20, FULL_RPM = 12_000, RADIUS_M = 0.10;

    private double rotorSpeed, rotorCarry;
    private boolean advancedThisTick;

    /** The heat the current input needs (HU): the recipe's minimum, 5,000 without input. */
    public int requiredHeat() {
        return getRecipe(itemHandler.getStackInSlot(INPUT_SLOT)).map(mio_icif_CentrifugeRecipe::getMinHeatRequired).orElse(MIN_HEAT_FOR_WORK);
    }

    /** Something to heat for: input with a recipe, room for the output, not stopped by redstone or a remote switch. */
    private boolean wantsHeat() {
        if (level == null || !canWorkRedstone()) return false;
        ItemStack input = itemHandler.getStackInSlot(INPUT_SLOT);
        return !input.isEmpty() && getRecipe(input).isPresent()
            && com.miophas.singularity_iteration.core.runtime.processing.RecipeSlots.fits(itemHandler, INPUT_SLOT, 1, OUTPUT_SLOTS, getProcessingResults(input));
    }

    public double getRotorSpeed() { return rotorSpeed; }

    /** Rotor speed (rpm). */
    public double rpm() { return FULL_RPM * rotorSpeed; }

    /** Relative centrifugal force (g): ω² r / g. */
    public double relativeCentrifugalForce() {
        double omega = rpm() * 2 * Math.PI / 60;
        return omega * omega * RADIUS_M / 9.81;
    }

    /** The progress moves only on the ticks the rotor ran. */
    @Override
    protected void updateProgress() {
        if (advancedThisTick) super.updateProgress();
        advancedThisTick = false;
    }

    /** Once a tick after production: the rotor speeds up while the centrifuge works, coasts down otherwise. */
    private void turnRotor() {
        double before = rotorSpeed;
        rotorSpeed = isWorking() ? Math.min(1, rotorSpeed + SPIN_UP_PER_TICK) : Math.max(0, rotorSpeed - COAST_PER_TICK);
        if (rotorSpeed == 0) rotorCarry = 0;
        if (rotorSpeed != before && (rotorSpeed == 0 || rotorSpeed == 1)) setChanged();
    }

    /** Why the centrifuge runs, heats or stands (a {@link com.miophas.singularity_iteration.common.processing.ProcessStatus} code). */
    public int processStatus() {
        if (level == null) return com.miophas.singularity_iteration.common.processing.ProcessStatus.NO_INPUT;
        if (!canWorkRedstone()) return com.miophas.singularity_iteration.common.processing.ProcessStatus.REDSTONE_OFF;
        ItemStack input = itemHandler.getStackInSlot(INPUT_SLOT);
        if (input.isEmpty()) return com.miophas.singularity_iteration.common.processing.ProcessStatus.NO_INPUT;
        if (getRecipe(input).isEmpty()) return com.miophas.singularity_iteration.common.processing.ProcessStatus.NO_RECIPE;
        if (!com.miophas.singularity_iteration.core.runtime.processing.RecipeSlots.fits(itemHandler, INPUT_SLOT, 1, OUTPUT_SLOTS, getProcessingResults(input)))
            return com.miophas.singularity_iteration.common.processing.ProcessStatus.OUTPUT_FULL;
        if (heatStorage < requiredHeat()) return hasEnoughEnergy()
            ? com.miophas.singularity_iteration.common.processing.ProcessStatus.HEATING : com.miophas.singularity_iteration.common.processing.ProcessStatus.NO_POWER;
        if (isWorking()) return com.miophas.singularity_iteration.common.processing.ProcessStatus.RUNNING;
        // 0.1.11.174: idle with everything ready means it starts on the next tick; "not enough power" only when it is short
        return hasEnoughEnergy() ? com.miophas.singularity_iteration.common.processing.ProcessStatus.RUNNING : com.miophas.singularity_iteration.common.processing.ProcessStatus.NO_POWER;
    }


    /**
     * 0.1.10.38: a heat change is saved with a light mark. The full setChanged() (comparators; it also wakes the idle machine)
     * comes only when canWork can change (MIN_HEAT_FOR_WORK crossed) or the comparator output changes. It used to run on
     * every tick of heating up and cooling down.
     */
    private void heatChanged(int before) {
        int need = requiredHeat();   // 0.1.11.114: the recipe's heat (it was the fixed 5,000), so a sleeping centrifuge wakes when it is reached
        boolean work = (before >= need) != (heatStorage >= need);
        if (work || heatSignal(before) != heatSignal(heatStorage)) setChanged();
        else com.miophas.singularity_iteration.core.runtime.TickMarks.saved(this);
    }

    /** The heat part (0-8) of the comparator output (mio_icif_block_centrifuge_elc.getAnalogOutputSignal). */
    public static int heatSignal(int heat) {
        return heat * 8 / MAX_HEAT;
    }

    /**
     * 0.1.10.38: canWork depends on the inventory, the stored energy and the heat; heat wakes the machine when it crosses
     * MIN_HEAT_FOR_WORK (heatChanged). The heat itself keeps changing while the machine sleeps (tick runs updateHeat first).
     */
    @Override
    protected boolean idleSleepAllowed() {
        return rotorSpeed <= 0;   // 0.1.11.114: a turning rotor keeps it awake (the heat is updated every tick, asleep too)
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
        tag.putDouble("centrifuge_speed", rotorSpeed);   // 0.1.11.114
    }

    @Override
    public void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        // Load the stored heat.
        if (tag.contains("heat_storage")) {
            heatStorage = tag.getInt("heat_storage");
        }
        rotorSpeed = Math.max(0, Math.min(1, tag.getDouble("centrifuge_speed")));   // 0.1.11.114: an old save starts at rest
    }
}