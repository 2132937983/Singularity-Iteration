package com.miophas.singularity_iteration.common.blockentity.producer;

import com.miophas.singularity_iteration.common.registry.mio_icif_block_entities;
import com.miophas.singularity_iteration.core.prefab.blockentity.AbstractProcessingMachineBlockEntity;
import com.miophas.singularity_iteration.core.prefab.inventory.SlotLayout;
import com.miophas.singularity_iteration.core.api.energy.storage.CableTier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Containers;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.TransientCraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

@SuppressWarnings("null")
public class mio_icif_batch_crafter extends AbstractProcessingMachineBlockEntity {
    // Per-call-site recipe memo: canWork() runs every tick, getRecipeFor is a linear scan.
    private final com.miophas.singularity_iteration.core.runtime.processing.RecipeLookupCache<net.minecraft.world.item.crafting.CraftingInput, net.minecraft.world.item.crafting.CraftingRecipe> recipeCache1 = com.miophas.singularity_iteration.core.runtime.processing.RecipeLookupCache.of(RecipeType.CRAFTING);

    private static final SlotLayout LAYOUT = SlotLayout.builder()
        .battery()
        .upgrade(4)
        .input(9)
        .output(10)
        .build();

    public static final int BATTERY_SLOT = 0;
    public static final int UPGRADE_SLOT_START = 1;
    public static final int UPGRADE_SLOT_COUNT = 4;
    public static final int INGREDIENT_SLOT_START = 5;
    public static final int INGREDIENT_SLOT_COUNT = 9;
    public static final int CRAFTING_OUTPUT_SLOT = 14;
    public static final int CONTAINER_OUTPUT_START = 15;
    public static final int CONTAINER_OUTPUT_COUNT = 9;

    public static final int CRAFTING_GRID_SIZE = 9;

    public static final long DEFAULT_CAPACITY = 20000L;
    public static final long DEFAULT_MAX_RECEIVE = 128L;
    public static final long DEFAULT_MAX_EXTRACT = 0L;
    public static final int DEFAULT_OPERATION_LENGTH = 40;
    public static final long DEFAULT_ENERGY_PER_TICK = 2L;

    private final ItemStack[] craftingGrid = new ItemStack[CRAFTING_GRID_SIZE];
    private RecipeHolder<CraftingRecipe> currentRecipe;
    private ItemStack recipeOutput = ItemStack.EMPTY;
    private boolean attemptToBalance = false;

    // ic2 TileEntityBatchCrafter: one paid-energy tick advances the cycle, and finishing a
    // cycle emits `operations()` crafts. Derived from the standard IC2 overclock formula.
    private com.miophas.singularity_iteration.core.runtime.upgrade.StandardProcessingTiming timing;
    private int previousOverclockers = -1;
    private boolean paidForThisTick;
    // Result of this tick's recipe check; reused by canOutput() and the per-operation craft.
    private ItemStack pendingOutput = ItemStack.EMPTY;
    // ic2 revalidates on change only ("newChange"), so the row is snapshotted instead of
    // rebuilding a crafting inventory every single tick.
    private final ItemStack[] rowSnapshot = new ItemStack[INGREDIENT_SLOT_COUNT];
    private boolean rowSnapshotValid = false;
    private boolean canCraftResult = false;

    private final ContainerData containerData = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> progress;
                case 1 -> maxProgress;
                case 2 -> isWorking ? 1 : 0;
                case 3 -> (int) energyStorage.getAmount();
                case 4 -> (int) energyStorage.getCapacity();
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {}

        @Override
        public int getCount() { return 5; }
    };

    public mio_icif_batch_crafter(BlockPos pos, BlockState state) {
        this(pos, state, mio_icif_block_entities.BATCH_CRAFTER.get());
    }

    public mio_icif_batch_crafter(BlockPos pos, BlockState state, BlockEntityType<?> type) {
        super(pos, state, type,
            DEFAULT_CAPACITY,
            DEFAULT_MAX_RECEIVE,
            DEFAULT_MAX_EXTRACT,
            DEFAULT_OPERATION_LENGTH,
            LAYOUT,
            DEFAULT_ENERGY_PER_TICK,
            CableTier.MV);
        for (int i = 0; i < CRAFTING_GRID_SIZE; i++) {
            craftingGrid[i] = ItemStack.EMPTY;
        }
        java.util.Arrays.fill(rowSnapshot, ItemStack.EMPTY);
    }

    public ItemStack getCraftingGridStack(int index) {
        if (index < 0 || index >= CRAFTING_GRID_SIZE) return ItemStack.EMPTY;
        return craftingGrid[index];
    }

    public ItemStack getRecipeOutput() {
        return recipeOutput;
    }

    public void setCraftingGridStack(int index, ItemStack stack) {
        if (index < 0 || index >= CRAFTING_GRID_SIZE) return;
        craftingGrid[index] = stack;
        matrixChange(index);
        setChanged();
    }

    private void matrixChange(int slot) {
        rowSnapshotValid = false;
        if (currentRecipe == null || !currentRecipe.value().matches(createCraftingContainer().asCraftInput(), level)) {
            currentRecipe = findRecipe().orElse(null);
        }
        recipeOutput = currentRecipe != null
            ? currentRecipe.value().assemble(createCraftingContainer().asCraftInput(), level.registryAccess())
            : ItemStack.EMPTY;
        attemptToBalance = true;
    }

    void ingredientChanged(int slot) {
        attemptToBalance = true;
    }

    /**
     * ic2 StackUtil.balanceStacks equivalent: spread each ingredient evenly over every row slot
     * whose hologram cell accepts it, so no single slot starves a cycle while others overflow.
     */
    private void balanceIngredients() {
        for (int i = 0; i < INGREDIENT_SLOT_COUNT; i++) {
            ItemStack hologram = craftingGrid[i];
            if (hologram.isEmpty()) continue;

            java.util.List<Integer> group = new java.util.ArrayList<>();
            int total = 0;
            for (int j = i; j < INGREDIENT_SLOT_COUNT; j++) {
                if (!ItemStack.isSameItemSameComponents(craftingGrid[j], hologram)) continue;
                ItemStack content = itemHandler.getStackInSlot(INGREDIENT_SLOT_START + j);
                // A row cell holding a foreign stack keeps its place, exactly as IC2 accepts() filtering does.
                if (!content.isEmpty() && !ItemStack.isSameItemSameComponents(content, hologram)) continue;
                group.add(INGREDIENT_SLOT_START + j);
                total += content.getCount();
            }
            if (group.size() < 2 || total < 2) continue;

            int base = total / group.size();
            int extra = total % group.size();
            boolean changed = false;
            for (int index = 0; index < group.size(); index++) {
                int slot = group.get(index);
                int limit = Math.min(itemHandler.getSlotLimit(slot), hologram.getMaxStackSize());
                int target = Math.min(limit, base + (index < extra ? 1 : 0));
                ItemStack content = itemHandler.getStackInSlot(slot);
                if (content.getCount() == target) continue;
                ItemStack copy = content.isEmpty() ? hologram.copy() : content.copy();
                copy.setCount(target);
                itemHandler.setStackInSlot(slot, copy);
                changed = true;
            }
            if (changed) setChanged();
        }
    }

    private TransientCraftingContainer createCraftingContainer() {
        return createCraftingContainer(craftingGrid);
    }

    private static TransientCraftingContainer createCraftingContainer(ItemStack[] template) {
        TransientCraftingContainer container = new TransientCraftingContainer(
            new AbstractContainerMenu(null, -1) {
                @Override
                public ItemStack quickMoveStack(net.minecraft.world.entity.player.Player player, int index) {
                    return ItemStack.EMPTY;
                }
                @Override
                public boolean stillValid(net.minecraft.world.entity.player.Player player) {
                    return true;
                }
            }, 3, 3);
        for (int i = 0; i < CRAFTING_GRID_SIZE; i++) {
            container.setItem(i, template[i].copy());
        }
        return container;
    }

    private TransientCraftingContainer createIngredientContainer() {
        TransientCraftingContainer container = new TransientCraftingContainer(
            new AbstractContainerMenu(null, -1) {
                @Override
                public ItemStack quickMoveStack(net.minecraft.world.entity.player.Player player, int index) {
                    return ItemStack.EMPTY;
                }
                @Override
                public boolean stillValid(net.minecraft.world.entity.player.Player player) {
                    return true;
                }
            }, 3, 3);
        for (int i = 0; i < INGREDIENT_SLOT_COUNT; i++) {
            container.setItem(i, itemHandler.getStackInSlot(INGREDIENT_SLOT_START + i).copy());
        }
        return container;
    }

    private Optional<RecipeHolder<CraftingRecipe>> findRecipe() {
        if (level == null) return Optional.empty();
        TransientCraftingContainer container = createCraftingContainer();
        return recipeCache1.find(level, container.asCraftInput());
    }

    private boolean canCraft() {
        // ic2 only recomputes after an inventory change ("newChange"); the steady tick stays O(9).
        if (rowSnapshotValid && currentRecipe != null && rowUnchanged()) return canCraftResult;

        rowSnapshotValid = false;
        if (currentRecipe == null) {
            currentRecipe = findRecipe().orElse(null);
        }
        if (currentRecipe == null) {
            pendingOutput = ItemStack.EMPTY;
            canCraftResult = false;
        } else {
            // The template grid selects the recipe; only the matched real inventory is consumed.
            CraftingInput input = createIngredientContainer().asCraftInput();
            if (currentRecipe.value().matches(input, level)) {
                pendingOutput = currentRecipe.value().assemble(input, level.registryAccess());
                canCraftResult = true;
            } else {
                pendingOutput = ItemStack.EMPTY;
                canCraftResult = false;
            }
        }
        cacheRowSnapshot();
        return canCraftResult;
    }

    private boolean rowUnchanged() {
        for (int i = 0; i < INGREDIENT_SLOT_COUNT; i++) {
            ItemStack current = itemHandler.getStackInSlot(INGREDIENT_SLOT_START + i);
            ItemStack last = rowSnapshot[i];
            if (current.getCount() != last.getCount() || !ItemStack.isSameItemSameComponents(current, last)) return false;
        }
        return true;
    }

    private void cacheRowSnapshot() {
        for (int i = 0; i < INGREDIENT_SLOT_COUNT; i++) {
            ItemStack content = itemHandler.getStackInSlot(INGREDIENT_SLOT_START + i);
            rowSnapshot[i] = content.isEmpty() ? ItemStack.EMPTY : content.copy();
        }
        rowSnapshotValid = true;
    }

    /** True when a hologram grid itself satisfies the recipe; used by the JEI layout transfer. */
    public static boolean gridMatches(@Nullable Level level, @Nullable RecipeHolder<CraftingRecipe> recipe, ItemStack[] grid) {
        if (level == null || recipe == null || grid == null || grid.length != CRAFTING_GRID_SIZE) return false;
        return recipe.value().matches(createCraftingContainer(grid).asCraftInput(), level);
    }

    /** True while the crafting output slot can still hold the pending result. */
    private boolean canOutput() {
        return canAcceptOutput(pendingOutput);
    }

    /**
     * ic2 TileEntityBatchCrafter guards the whole cycle on craftingOutput.canAdd(): products only
     * ever enter the single output slot, never the container return slots.
     */
    private boolean canAcceptOutput(ItemStack result) {
        if (result.isEmpty()) return false;
        ItemStack existing = itemHandler.getStackInSlot(CRAFTING_OUTPUT_SLOT);
        if (!existing.isEmpty() && !ItemStack.isSameItemSameComponents(existing, result)) return false;
        int limit = Math.min(itemHandler.getSlotLimit(CRAFTING_OUTPUT_SLOT), result.getMaxStackSize());
        return existing.getCount() + result.getCount() <= limit;
    }

    private boolean insertCraftingOutput(ItemStack result) {
        if (!canAcceptOutput(result)) return false;
        ItemStack existing = itemHandler.getStackInSlot(CRAFTING_OUTPUT_SLOT);
        if (existing.isEmpty()) {
            itemHandler.setStackInSlot(CRAFTING_OUTPUT_SLOT, result.copy());
        } else {
            existing.grow(result.getCount());
        }
        return true;
    }

    private void finishCrafting() {
        if (currentRecipe == null) {
            currentRecipe = findRecipe().orElse(null);
        }
        if (currentRecipe == null) return;

        CraftingInput.Positioned positioned = createIngredientContainer().asPositionedCraftInput();
        CraftingInput input = positioned.input();
        if (!currentRecipe.value().matches(input, level)) return;
        ItemStack result = pendingOutput.isEmpty()
            ? currentRecipe.value().assemble(input, level.registryAccess())
            : pendingOutput;
        NonNullList<ItemStack> compactRemaining = currentRecipe.value().getRemainingItems(input);
        NonNullList<ItemStack> remaining = NonNullList.withSize(INGREDIENT_SLOT_COUNT, ItemStack.EMPTY);
        for (int i = 0; i < compactRemaining.size(); i++) {
            int x = i % input.width() + positioned.left();
            int y = i / input.width() + positioned.top();
            remaining.set(x + y * 3, compactRemaining.get(i));
        }
        if (!insertCraftingOutput(result)) return;

        for (int i = 0; i < INGREDIENT_SLOT_COUNT; i++) {
            ItemStack ingredient = itemHandler.getStackInSlot(INGREDIENT_SLOT_START + i);
            // The template selects a recipe; only the matched real inventory is consumed.
            if (!ingredient.isEmpty()) {
                ingredient.shrink(1);
                if (ingredient.isEmpty()) {
                    itemHandler.setStackInSlot(INGREDIENT_SLOT_START + i, ItemStack.EMPTY);
                }
            }

            if (i < remaining.size()) {
                ItemStack remainder = remaining.get(i);
                if (!remainder.isEmpty()) {
                    boolean placed = false;
                    ItemStack afterShrink = itemHandler.getStackInSlot(INGREDIENT_SLOT_START + i);
                    if (afterShrink.isEmpty()) {
                        itemHandler.setStackInSlot(INGREDIENT_SLOT_START + i, remainder.copy());
                        placed = true;
                    }
                    if (!placed && !afterShrink.isEmpty() && ItemStack.isSameItemSameComponents(afterShrink, remainder)) {
                        afterShrink.grow(remainder.getCount());
                        placed = true;
                    }
                    if (!placed) {
                        for (int j = 0; j < CONTAINER_OUTPUT_COUNT; j++) {
                            ItemStack containerSlot = itemHandler.getStackInSlot(CONTAINER_OUTPUT_START + j);
                            if (containerSlot.isEmpty()) {
                                itemHandler.setStackInSlot(CONTAINER_OUTPUT_START + j, remainder.copy());
                                placed = true;
                                break;
                            } else if (ItemStack.isSameItemSameComponents(containerSlot, remainder)
                                       && containerSlot.getCount() + remainder.getCount() <= containerSlot.getMaxStackSize()) {
                                containerSlot.grow(remainder.getCount());
                                placed = true;
                                break;
                            }
                        }
                    }
                    if (!placed && level != null) {
                        Containers.dropItemStack(level,
                            worldPosition.getX(), worldPosition.getY() + 1, worldPosition.getZ(), remainder.copy());
                    }
                }
            }
        }

        pendingOutput = ItemStack.EMPTY;
        rowSnapshotValid = false;
        canCraftResult = false;
        currentRecipe = null;
        recipeOutput = ItemStack.EMPTY;
        // IC2 marks the row dirty after crafting so the next tick rebalances what is left.
        attemptToBalance = true;
        setChanged();
    }

    public boolean acceptsIngredient(int gridIndex, ItemStack ingredient) {
        return acceptsIngredient(level, craftingGrid, currentRecipe, gridIndex, ingredient);
    }

    /** Reads the selected recipe without changing this machine or its phantom grid. */
    @Nullable
    public RecipeHolder<CraftingRecipe> selectedRecipeForMenu() {
        return currentRecipe != null ? currentRecipe : findRecipe().orElse(null);
    }

    /** Checks a copied template against the selected recipe, without mutating a block entity. */
    public static boolean acceptsIngredient(@Nullable Level level, ItemStack[] template,
            @Nullable RecipeHolder<CraftingRecipe> selectedRecipe, int gridIndex, ItemStack ingredient) {
        if (level == null || template.length != CRAFTING_GRID_SIZE
                || gridIndex < 0 || gridIndex >= CRAFTING_GRID_SIZE) return false;
        TransientCraftingContainer testContainer = createCraftingContainer(template);
        RecipeHolder<CraftingRecipe> recipe = selectedRecipe != null ? selectedRecipe
            : level.getRecipeManager().getRecipeFor(RecipeType.CRAFTING, testContainer.asCraftInput(), level).orElse(null);
        if (recipe == null) return false;
        testContainer.setItem(gridIndex, ingredient.copy());
        return recipe.value().matches(testContainer.asCraftInput(), level);
    }

    @Override
    public long getEnergy() {
        return energyStorage.getAmount();
    }

    @Override
    public boolean useEnergy(long amount) {
        return apiConsumeEnergy(amount);
    }

    @Override
    public java.util.Set<com.miophas.singularity_iteration.core.api.upgrade.tile.UpgradableProperty> getUpgradableProperties() {
        return java.util.EnumSet.of(
            com.miophas.singularity_iteration.core.api.upgrade.tile.UpgradableProperty.PROCESSING,
            com.miophas.singularity_iteration.core.api.upgrade.tile.UpgradableProperty.TRANSFORMER,
            com.miophas.singularity_iteration.core.api.upgrade.tile.UpgradableProperty.ENERGY_STORAGE,
            com.miophas.singularity_iteration.core.api.upgrade.tile.UpgradableProperty.ITEM_CONSUMING,
            com.miophas.singularity_iteration.core.api.upgrade.tile.UpgradableProperty.ITEM_PRODUCING
        );
    }

    @Override
    protected boolean canWork() {
        return canCraft();
    }

    @Override
    protected void doWork() {
        finishCrafting();
    }

    /** IC2 rebalances the ingredient rows on every tick that saw an inventory change. */
    @Override
    protected void onTick() {
        if (attemptToBalance) {
            balanceIngredients();
            attemptToBalance = false;
        }
    }

    @Override
    protected void checkInputChanged() {
        // IC2 keeps a running cycle when the row contents change; only a missing recipe clears it.
    }

    @Override
    protected void updateProcessingParameters() {
        int count = upgradeStats.getOverclockerCount();
        if (timing != null && previousOverclockers == count) return;
        // Identical math to ic2 InvSlotUpgrade: ops = ceil(1/duration), ticks = round(duration*ops).
        timing = com.miophas.singularity_iteration.core.runtime.upgrade.StandardProcessingTiming.calculate(
            baseMaxProgress, energyPerTick,
            upgradeStats.getProcessTimeMultiplier(), upgradeStats.getEnergyUsageMultiplier());
        previousOverclockers = count;
        int previousLength = maxProgress;
        maxProgress = Math.max(1, timing.ticks());
        if (previousLength > 0 && previousLength != maxProgress) {
            progress = (int) Math.min(maxProgress - 1L, (long) ((double) progress * maxProgress / previousLength));
        }
    }

    @Override
    protected long getProcessingCapacity() {
        // ic2 InvSlotUpgrade.getEnergyStorage: base + operationLength * energyDemand + storage upgrades.
        return timing == null ? baseCapacity
            : timing.bufferWithStorage(baseCapacity, upgradeStats.getEnergyStorageCount());
    }

    @Override
    public long getEffectiveCapacity() {
        return getProcessingCapacity();
    }

    @Override
    public long getEffectiveEnergyPerTick() {
        return timing == null ? energyPerTick : timing.energyPerTick();
    }

    @Override
    protected boolean consumeEnergy() {
        return paidForThisTick || super.consumeEnergy();
    }

    @Override
    protected boolean hasEnoughEnergy() {
        return paidForThisTick || super.hasEnoughEnergy();
    }

    /**
     * ic2 TileEntityBatchCrafter.updateEntityServer: pay once per tick, advance the cycle, then run
     * every scheduled operation. Progress is only cleared when the hologram grid has no recipe;
     * a blocked output or an empty energy buffer holds the elapsed cycle.
     */
    @Override
    protected void tickProduction() {
        if (!canWork()) {
            if (!hasValidRecipe()) progress = 0;
            stopWork();
            return;
        }
        if (!canOutput() || !consumeEnergy()) {
            stopWork();
            return;
        }
        isWorking = true;
        if (++progress < maxProgress) return;
        paidForThisTick = true;
        try {
            for (int i = 0; i < batchOperations() && canWork() && canOutput(); i++) {
                progress = maxProgress;
                finishCrafting();
            }
        } finally {
            paidForThisTick = false;
            progress = 0;
            isWorking = true;
            setChanged();
        }
    }

    private int batchOperations() {
        return timing == null ? 1 : Math.max(1, timing.operations());
    }

    /** ic2 comparator.setUpdate(progress * 15 / operationLength). */
    public int getComparatorLevel() {
        if (maxProgress <= 0) return 0;
        return Math.max(0, Math.min(15, progress * 15 / maxProgress));
    }

    @Override
    protected boolean hasValidRecipe() {
        if (currentRecipe == null) {
            currentRecipe = findRecipe().orElse(null);
        }
        return currentRecipe != null;
    }

    @Override
    protected int[] getSlotsForDirection(@Nullable Direction side) {
        return new int[]{
            INGREDIENT_SLOT_START, INGREDIENT_SLOT_START + 1, INGREDIENT_SLOT_START + 2,
            INGREDIENT_SLOT_START + 3, INGREDIENT_SLOT_START + 4, INGREDIENT_SLOT_START + 5,
            INGREDIENT_SLOT_START + 6, INGREDIENT_SLOT_START + 7, INGREDIENT_SLOT_START + 8,
            CRAFTING_OUTPUT_SLOT,
            BATTERY_SLOT
        };
    }

    @Override
    protected int[] getInputSlots() {
        return new int[]{
            INGREDIENT_SLOT_START, INGREDIENT_SLOT_START + 1, INGREDIENT_SLOT_START + 2,
            INGREDIENT_SLOT_START + 3, INGREDIENT_SLOT_START + 4, INGREDIENT_SLOT_START + 5,
            INGREDIENT_SLOT_START + 6, INGREDIENT_SLOT_START + 7, INGREDIENT_SLOT_START + 8
        };
    }

    @Override
    protected int[] getOutputSlots() {
        // ic2 containerOutput is InvSlot.InvSide.NOTSIDE: only the crafting result is automatable.
        return new int[]{ CRAFTING_OUTPUT_SLOT };
    }

    @Override
    protected int getBatterySlot() {
        return BATTERY_SLOT;
    }

    @Override
    protected boolean canExtractItem(int slot, @Nullable Direction side) {
        return slot == CRAFTING_OUTPUT_SLOT;
    }

    @Override
    protected boolean canInsertItem(int slot, ItemStack stack, @Nullable Direction side) {
        if (!isItemValidForSlot(slot, stack)) return false;
        if (slot == BATTERY_SLOT) return isBattery(stack);
        if (slot == CRAFTING_OUTPUT_SLOT) return false;
        if (slot >= CONTAINER_OUTPUT_START && slot < CONTAINER_OUTPUT_START + CONTAINER_OUTPUT_COUNT) return false;
        if (isUpgradeSlot(slot)) return false;
        if (slot >= INGREDIENT_SLOT_START && slot < INGREDIENT_SLOT_START + INGREDIENT_SLOT_COUNT) {
            int gridIndex = slot - INGREDIENT_SLOT_START;
            return acceptsIngredient(gridIndex, stack);
        }
        return true;
    }

    @Override
    protected boolean isItemValidForSlot(int slot, ItemStack stack) {
        if (slot >= INGREDIENT_SLOT_START && slot < INGREDIENT_SLOT_START + INGREDIENT_SLOT_COUNT) {
            return true;
        }
        if (slot == BATTERY_SLOT) {
            return isBattery(stack);
        }
        if (isUpgradeSlot(slot)) {
            return stack.getItem() instanceof com.miophas.singularity_iteration.common.item.upgrade.mio_icif_upgrade;
        }
        if (slot == CRAFTING_OUTPUT_SLOT || (slot >= CONTAINER_OUTPUT_START && slot < CONTAINER_OUTPUT_START + CONTAINER_OUTPUT_COUNT)) {
            return false;
        }
        return super.isItemValidForSlot(slot, stack);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, mio_icif_batch_crafter blockEntity) {
        if (level.isClientSide()) return;
        AbstractProcessingMachineBlockEntity.tick(level, pos, state, blockEntity);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        CompoundTag gridTag = new CompoundTag();
        for (int i = 0; i < CRAFTING_GRID_SIZE; i++) {
            if (!craftingGrid[i].isEmpty()) {
                gridTag.put("slot" + i, craftingGrid[i].save(registries));
            }
        }
        tag.put("craftingGrid", gridTag);
        tag.putInt("scex_operation_ticks", maxProgress);
    }

    @Override
    public void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        // The saved progress belongs to the cycle length it was written with.
        if (tag.contains("scex_operation_ticks", net.minecraft.nbt.Tag.TAG_INT)) {
            int savedLength = Math.max(1, tag.getInt("scex_operation_ticks"));
            if (savedLength != maxProgress) {
                progress = (int) Math.min(Math.max(0, maxProgress - 1L),
                    (long) Math.max(0, progress) * maxProgress / savedLength);
            }
        }
        CompoundTag gridTag = tag.getCompound("craftingGrid");
        for (int i = 0; i < CRAFTING_GRID_SIZE; i++) {
            if (gridTag.contains("slot" + i)) {
                craftingGrid[i] = ItemStack.parseOptional(registries, gridTag.getCompound("slot" + i));
            } else {
                craftingGrid[i] = ItemStack.EMPTY;
            }
        }
        currentRecipe = null;
        recipeOutput = ItemStack.EMPTY;
        pendingOutput = ItemStack.EMPTY;
        rowSnapshotValid = false;
        canCraftResult = false;
        attemptToBalance = true;
    }

    /** Row changes queue an IC2 style rebalance; the deputy in onTick() performs it. */
    @Override
    protected com.miophas.singularity_iteration.core.prefab.inventory.MachineItemHandler createItemHandler(
            com.miophas.singularity_iteration.core.prefab.inventory.SlotLayout layout) {
        com.miophas.singularity_iteration.core.prefab.inventory.MachineItemHandler handler =
            new com.miophas.singularity_iteration.core.prefab.inventory.MachineItemHandler(layout) {
                @Override
                protected void onContentsChanged(int slot) {
                    if (layout.isType(slot, com.miophas.singularity_iteration.core.prefab.inventory.SlotType.INPUT)) {
                        attemptToBalance = true;
                    }
                    mio_icif_batch_crafter.this.onInventoryChanged(slot);
                }
            };
        handler.setValidator(this);
        return handler;
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, net.minecraft.world.entity.player.Player player) {
        return new com.miophas.singularity_iteration.common.menu.producer.BatchCrafterMenu(containerId, playerInventory, this, this.containerData);
    }

    public ContainerData getContainerData() { return containerData; }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.mio_icif.batch_crafter");
    }

    @Override
    public net.neoforged.neoforge.items.IItemHandler getItemHandlerCapability(@Nullable Direction side) {
        return new BatchCrafterSidedHandler(side);
    }

    protected class BatchCrafterSidedHandler extends SidedItemHandler {
        public BatchCrafterSidedHandler(@Nullable Direction side) {
            super(side);
        }

        @Override
        public ItemStack insertItem(int slot,  ItemStack stack, boolean simulate) {
            if (!canInsertItem(slot, stack, side)) {
                return stack;
            }
            if (slot >= INGREDIENT_SLOT_START && slot < INGREDIENT_SLOT_START + INGREDIENT_SLOT_COUNT) {
                return distributeIngredientInsert(slot, stack, simulate);
            }
            return itemHandler.insertItem(slot, stack, simulate);
        }

        private ItemStack distributeIngredientInsert(int requestedSlot, ItemStack stack, boolean simulate) {
            ItemStack remaining = stack.copy();

            java.util.List<Integer> matchingSlots = new java.util.ArrayList<>();
            for (int i = 0; i < INGREDIENT_SLOT_COUNT; i++) {
                int slotIdx = INGREDIENT_SLOT_START + i;
                if (acceptsIngredient(i, stack)) {
                    matchingSlots.add(slotIdx);
                }
            }
            if (matchingSlots.isEmpty()) {
                return stack;
            }

            matchingSlots.sort((a, b) -> {
                int countA = itemHandler.getStackInSlot(a).isEmpty() ? 0 : itemHandler.getStackInSlot(a).getCount();
                int countB = itemHandler.getStackInSlot(b).isEmpty() ? 0 : itemHandler.getStackInSlot(b).getCount();
                return Integer.compare(countA, countB);
            });

            if (simulate) {
                ItemStack[] simStacks = new ItemStack[itemHandler.getSlots()];
                for (int i = 0; i < simStacks.length; i++) {
                    simStacks[i] = itemHandler.getStackInSlot(i).copy();
                }

                ItemStack simRemaining = remaining.copy();
                int pass = 0;
                while (!simRemaining.isEmpty() && pass < matchingSlots.size() + 1) {
                    boolean insertedAny = false;
                    for (int slotIdx : matchingSlots) {
                        if (simRemaining.isEmpty()) break;
                        ItemStack existing = simStacks[slotIdx];
                        int limit = Math.min(itemHandler.getSlotLimit(slotIdx), simRemaining.getMaxStackSize());
                        int canInsert;
                        if (existing.isEmpty()) {
                            canInsert = Math.min(limit, simRemaining.getCount());
                            simStacks[slotIdx] = simRemaining.copyWithCount(canInsert);
                        } else {
                            canInsert = Math.min(limit - existing.getCount(), simRemaining.getCount());
                            if (canInsert > 0) existing.grow(canInsert);
                        }
                        if (canInsert > 0) {
                            simRemaining.shrink(canInsert);
                            insertedAny = true;
                        }
                    }
                    if (!insertedAny) break;
                    pass++;
                }
                return simRemaining;
            }

            int pass = 0;
            while (!remaining.isEmpty() && pass < matchingSlots.size() + 1) {
                boolean insertedAny = false;
                for (int slotIdx : matchingSlots) {
                    if (remaining.isEmpty()) break;
                    ItemStack result = itemHandler.insertItem(slotIdx, remaining, false);
                    if (result.getCount() < remaining.getCount()) {
                        insertedAny = true;
                    }
                    remaining = result;
                }
                if (!insertedAny) break;
                pass++;
            }
            return remaining;
        }
    }
}
