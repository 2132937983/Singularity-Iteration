package com.miophas.singularity_iteration.common.blockentity.producer;

import com.miophas.singularity_iteration.common.registry.mio_icif_block_entities;
import com.miophas.singularity_iteration.core.prefab.blockentity.AbstractProcessingMachineBlockEntity;
import com.miophas.singularity_iteration.core.prefab.inventory.SlotLayout;
import com.miophas.singularity_iteration.common.item.cell.mio_icif_cells;
import com.miophas.singularity_iteration.core.api.energy.storage.CableTier;
import com.miophas.singularity_iteration.core.api.energy.storage.EUApi;
import com.miophas.singularity_iteration.core.api.energy.storage.IEUEnergyStorage;
import com.miophas.singularity_iteration.core.runtime.processing.RecipeSlots;
import com.miophas.singularity_iteration.core.runtime.energy.ContainerToTank;
import java.util.Optional;
import java.util.List;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * Block entity for the electrolyzer.
 * It extends AbstractProcessingMachineBlockEntity.
 *
 * Behavior:
 * 1. It uses electricity to electrolyze water cells. It outputs empty cells.
 *    Electrolyzer recipes (recipe API, datapacks) add inputs: each gives its recipe output and
 *    stores ENERGY_PER_WATER_CELL like a water cell (the machine keeps its IC2 rate).
 * 2. It stores the energy as chemical energy, up to DEFAULT_CAPACITY EU.
 * 3. It outputs the chemical energy to adjacent blocks at a maximum of 32 EU/t.
 * 4. It sends energy to an adjacent machine only when that machine is not full.
 */
@SuppressWarnings("null")
public class mio_icif_electrolyzer_elc extends AbstractProcessingMachineBlockEntity {

    // Slot indexes. They must match the slot indexes in the GUI.
    public static final int INPUT_SLOT = 0; // Input: water cells
    public static final int OUTPUT_SLOT = 1; // Output: empty cells
    // Default energy settings. The values match the IC2 electrolyzer.
    public static final long DEFAULT_CAPACITY = 400L; // 10 EU/t x 20 ticks = 200 EU for one cell. The buffer holds 2 cells.
    public static final long DEFAULT_MAX_RECEIVE = 32L; // Maximum input: 32 EU/t
    public static final long DEFAULT_MAX_EXTRACT = 32L; // Maximum output: 32 EU/t
    public static final long ENERGY_PER_WATER_CELL = 200L; // One water cell uses 200 EU
    public static final long DEFAULT_ENERGY_PER_TICK = 10L; // Uses 10 EU per tick, the same as IC2

    // Stored chemical energy in EU.
    protected long chemicalEnergy = 0;

    /** electrolyzer recipes (recipe API, datapacks): more inputs, each stored like a water cell */
    private final com.miophas.singularity_iteration.core.runtime.processing.RecipeLookupCache<
        com.miophas.singularity_iteration.common.recipe.mio_icif_SingleItemRecipeInput,
        com.miophas.singularity_iteration.common.recipe.generic.ElectrolyzerRecipe> recipes =
        new com.miophas.singularity_iteration.core.runtime.processing.RecipeLookupCache<>(
            com.miophas.singularity_iteration.common.recipe.generic.ElectrolyzerRecipes.ELECTROLYZER_TYPE);

    // Energy paid toward the current water cell, in EU.
    protected long energyAccumulator = 0;
    // Legacy accumulator energy was already in ChemicalEnergy or had already been emitted.
    // It still owes cell consumption but must not be published as chemical energy a second time.
    protected long legacyCellCredit;
    private long uncertainOutput;
    private boolean scexProcessing, scexOutputting;

    public mio_icif_electrolyzer_elc(BlockPos pos, BlockState state) {
        this(pos, state, mio_icif_block_entities.ELECTROLYZER.get());
    }

    private static final SlotLayout LAYOUT = SlotLayout.builder()
        .input(1)
        .output(1)
        .build();

    public mio_icif_electrolyzer_elc(BlockPos pos, BlockState state, BlockEntityType<?> type) {
        super(pos, state, type,
            DEFAULT_CAPACITY,
            DEFAULT_MAX_RECEIVE,
            DEFAULT_MAX_EXTRACT,
            20,
            LAYOUT,
            DEFAULT_ENERGY_PER_TICK,
            CableTier.LV);
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.mio_icif.electrolyzer");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new com.miophas.singularity_iteration.common.menu.producer.ElectrolyzerElcMenu(containerId, playerInventory, this);
    }

    @Override
    public boolean isItemValidForSlot(int slot, ItemStack stack) {
        return switch (slot) {
            case INPUT_SLOT -> mio_icif_cells.isCellContainingFluid(stack, net.minecraft.world.level.material.Fluids.WATER)
                || recipeOutput(stack.copyWithCount(1)).isPresent();
            case OUTPUT_SLOT -> false; // Players cannot put items into the output slot
            default -> false;
        };
    }

    @Override
    protected int[] getSlotsForDirection(Direction side) {
        // All sides can access all slots.
        return new int[]{INPUT_SLOT, OUTPUT_SLOT};
    }

    @Override
    protected int[] getInputSlots() {
        return new int[]{INPUT_SLOT};
    }

    @Override
    protected int[] getOutputSlots() {
        return new int[]{OUTPUT_SLOT};
    }

    @Override
    protected boolean canInsertItem(int slot, ItemStack stack, @Nullable Direction side) {
        if (!isItemValidForSlot(slot, stack)) {
            return false;
        }
        // Automation can insert only into the input slot.
        return slot == INPUT_SLOT;
    }

    @Override
    protected int getBatterySlot() {
        return -1; // The electrolyzer has no battery slot
    }

    @Override
    protected boolean canExtractItem(int slot, @Nullable Direction side) {
        // Automation can extract only from the output slot.
        return slot == OUTPUT_SLOT;
    }

    /**
     * Checks if the electrolyzer can work.
     * Conditions:
     * 1. Energy is available in the buffer or from paid credit.
     * 2. The input slot contains a water cell.
     * 3. The output slot can accept the empty cell.
     * 4. The chemical energy storage has space for one cell of energy.
     */
    @Override
    protected boolean canWork() {
        if (scexProcessing || scexOutputting || uncertainOutput > 0 || prepareCell().isEmpty()) return false;
        long freshNeeded = ENERGY_PER_WATER_CELL - Math.min(ENERGY_PER_WATER_CELL, legacyCellCredit);
        if (freshNeeded > Math.max(0, DEFAULT_CAPACITY - chemicalEnergy)) return false;
        return energyAccumulator >= freshNeeded || apiGetStoredEnergy() > 0;
    }

    /** The output of an electrolyzer recipe for {@code single}, if one matches. */
    protected Optional<ItemStack> recipeOutput(ItemStack single) {
        if (level == null || single.isEmpty()) return Optional.empty();
        var recipe = recipes.findValue(level, new com.miophas.singularity_iteration.common.recipe.mio_icif_SingleItemRecipeInput(single));
        return recipe == null || recipe.getResult().isEmpty() ? Optional.empty() : Optional.of(recipe.getResult().copy());
    }

    protected Optional<RecipeSlots.Prepared> prepareCell() {
        var input = itemHandler.getStackInSlot(INPUT_SLOT);
        if (input.isEmpty()) return Optional.empty();
        var single = input.copyWithCount(1);
        // A recipe for the item comes first (an add-on may electrolyze water cells differently).
        var output = recipeOutput(single);
        if (output.isPresent()) return RecipeSlots.prepare(itemHandler, INPUT_SLOT, 1, new int[]{OUTPUT_SLOT}, List.of(output.get()));
        if (!mio_icif_cells.isCellContainingFluid(single, net.minecraft.world.level.material.Fluids.WATER)) return Optional.empty();
        var contents = mio_icif_cells.getCellFluid(single);
        if (contents.isEmpty()) return Optional.empty();
        var empty = mio_icif_cells.getEmptyCellForStack(single);
        if (empty.isEmpty() || empty.getCount() != 1) return Optional.empty();
        return RecipeSlots.prepare(itemHandler, INPUT_SLOT, 1, new int[]{OUTPUT_SLOT}, List.of(empty));
    }

    /**
     * Uses electricity to process a water cell.
     * The machine adds energy to the accumulator each tick.
     * At 200 EU it converts one water cell into an empty cell and stores the energy as chemical energy.
     */
    @Override
    protected void doWork() {
        if (!canWork()) { stopWork(); return; }
        var prepared = prepareCell();
        if (prepared.isEmpty()) { stopWork(); return; }
        scexProcessing = true;
        try {
            long legacyUsed = Math.min(ENERGY_PER_WATER_CELL, legacyCellCredit);
            long freshNeeded = ENERGY_PER_WATER_CELL - legacyUsed;
            if (energyAccumulator < freshNeeded) {
                long debit = Math.min(freshNeeded - energyAccumulator,
                    Math.min(DEFAULT_ENERGY_PER_TICK, Math.max(0, apiGetStoredEnergy())));
                if (debit <= 0 || apiUseEnergy(debit, true) != debit) { stopWork(); return; }
                long paid = apiUseEnergy(debit, false);
                if (paid < 0 || paid > debit) throw new IllegalStateException("Invalid owned electrolyzer debit");
                energyAccumulator += paid;
                ContainerToTank.markUnsaved(this);
            }
            isWorking = true;
            if (energyAccumulator < freshNeeded) return;
            long oldChemical = chemicalEnergy, oldCredit = energyAccumulator, oldLegacy = legacyCellCredit;
            chemicalEnergy += freshNeeded; energyAccumulator -= freshNeeded; legacyCellCredit -= legacyUsed;
            progress = 0;
            // All account state is visible before owned inventory notifications.
            if (!prepared.get().commit()) {
                chemicalEnergy = oldChemical; energyAccumulator = oldCredit; legacyCellCredit = oldLegacy;
                stopWork();
            }
            ContainerToTank.markUnsaved(this);
        } finally { scexProcessing = false; }
    }

    @Override
    protected void updateProgress() {
        // The paid credit, rather than the generic scheduler, defines progress.
        long legacy = Math.min(ENERGY_PER_WATER_CELL, legacyCellCredit);
        long fresh = Math.min(ENERGY_PER_WATER_CELL - legacy, energyAccumulator);
        progress = (int) ((legacy + fresh) * 20 / ENERGY_PER_WATER_CELL);
    }

    /**
     * Runs one server tick.
     */
    public static void tick(Level level, BlockPos pos, BlockState state, mio_icif_electrolyzer_elc blockEntity) {
        if (level.isClientSide()) {
            return;
        }

        // The parent tick does the energy input and processing logic.
        AbstractProcessingMachineBlockEntity.tick(level, pos, state, blockEntity);

        // Send chemical energy to adjacent machines.
        blockEntity.outputChemicalEnergy();


    }

    /**
     * Sends chemical energy to adjacent machines.
     * The machine sends energy only to neighbors that are not full.
     */
    private void outputChemicalEnergy() {
        if (!(level instanceof ServerLevel server) || !server.getServer().isSameThread()
                || scexOutputting || scexProcessing || uncertainOutput > 0 || chemicalEnergy <= 0) return;
        long budget = DEFAULT_MAX_EXTRACT;
        for (Direction direction : Direction.values()) {
            if (budget <= 0 || chemicalEnergy <= 0) break;
            var adjacent = worldPosition.relative(direction);
            if (!server.getChunkSource().hasChunk(adjacent.getX() >> 4, adjacent.getZ() >> 4)) continue;
            var target = server.getCapability(EUApi.SIDED, adjacent, direction.getOpposite());
            if (target == null || target == energyStorage) continue;
            long capacity = target.getCapacity(), stored = target.getAmount();
            if (stored < 0 || capacity <= stored) continue;
            long delivered = offerChemical(target, Math.min(budget, capacity - stored));
            budget -= delivered;
            if (delivered > 0) {
                var neighbor = server.getBlockEntity(adjacent);
                if (neighbor != null) ContainerToTank.markUnsaved(neighbor);
            }
        }
    }

    protected long offerChemical(IEUEnergyStorage target, long limit) {
        if (scexOutputting || scexProcessing || uncertainOutput > 0 || limit <= 0 || chemicalEnergy <= 0) return 0;
        long offered = Math.min(chemicalEnergy, limit);
        scexOutputting = true;
        chemicalEnergy -= offered; uncertainOutput = offered;
        ContainerToTank.markUnsaved(this);
        try {
            long accepted = target.receive(offered, false);
            if (accepted < 0 || accepted > offered) throw new IllegalStateException("Invalid electrolyzer target acceptance");
            chemicalEnergy += offered - accepted; uncertainOutput = 0;
            ContainerToTank.markUnsaved(this);
            return accepted;
        } finally { scexOutputting = false; }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putLong("ChemicalEnergy", chemicalEnergy);
        tag.putLong("EnergyAccumulator", energyAccumulator);
        tag.putInt("scex_electrolysis_version", 1);
        tag.putLong("scex_legacy_cell_credit", legacyCellCredit);
        tag.putLong("scex_uncertain_output", uncertainOutput);
    }

    @Override
    public void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        chemicalEnergy = Math.max(0, tag.getLong("ChemicalEnergy"));
        if (tag.contains("scex_electrolysis_version")) {
            energyAccumulator = Math.max(0, tag.getLong("EnergyAccumulator"));
            legacyCellCredit = Math.max(0, tag.getLong("scex_legacy_cell_credit"));
        } else {
            energyAccumulator = 0;
            legacyCellCredit = Math.max(0, tag.getLong("EnergyAccumulator"));
        }
        uncertainOutput = Math.max(0, tag.getLong("scex_uncertain_output"));
        updateProgress();
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        // 0.1.10.17 (E-2): the client tag of the parent only. This used to add the whole save (inventory,
        // tanks, credits); the screen gets those from its menu data, and nothing else reads them on the client.
        return super.getUpdateTag(registries);
    }

    // ==================== Getters ====================

    public long getChemicalEnergy() {
        return chemicalEnergy;
    }

    public long getMaxChemicalEnergy() {
        return DEFAULT_CAPACITY;
    }

    public boolean isCharging() {
        return isWorking;
    }

    public boolean isDischarging() {
        return chemicalEnergy > 0;
    }
}