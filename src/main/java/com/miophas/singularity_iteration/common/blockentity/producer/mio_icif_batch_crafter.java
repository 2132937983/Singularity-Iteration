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
        if (currentRecipe == null || !currentRecipe.value().matches(createCraftingContainer().asCraftInput(), level)) {
            currentRecipe = findRecipe().orElse(null);
        }
        recipeOutput = currentRecipe != null
            ? currentRecipe.value().assemble(createCraftingContainer().asCraftInput(), level.registryAccess())
            : ItemStack.EMPTY;
    }

    void ingredientChanged(int slot) {
        attemptToBalance = true;
    }

    private void balanceIngredients() {
        for (int i = 0; i < INGREDIENT_SLOT_COUNT; i++) {
            ItemStack hologram = craftingGrid[i];
            if (hologram.isEmpty()) continue;

            ItemStack current = itemHandler.getStackInSlot(INGREDIENT_SLOT_START + i);
            if (current.isEmpty()) continue;
            if (!ItemStack.isSameItemSameComponents(current, hologram)) continue;

            for (int j = i + 1; j < INGREDIENT_SLOT_COUNT; j++) {
                ItemStack otherHologram = craftingGrid[j];
                if (!ItemStack.isSameItemSameComponents(hologram, otherHologram)) continue;

                ItemStack other = itemHandler.getStackInSlot(INGREDIENT_SLOT_START + j);
                if (other.isEmpty()) continue;
                if (!ItemStack.isSameItemSameComponents(other, hologram)) continue;

                int diff = current.getCount() - other.getCount();
                if (diff > 1) {
                    int transfer = diff / 2;
                    current.shrink(transfer);
                    other.grow(transfer);
                } else if (diff < -1) {
                    int transfer = (-diff) / 2;
                    current.grow(transfer);
                    other.shrink(transfer);
                }
            }
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
        return level.getRecipeManager().getRecipeFor(RecipeType.CRAFTING, container.asCraftInput(), level);
    }

    private boolean canCraft() {
        if (currentRecipe == null) {
            currentRecipe = findRecipe().orElse(null);
        }
        if (currentRecipe == null) return false;

        CraftingInput input = createIngredientContainer().asCraftInput();
        if (!currentRecipe.value().matches(input, level)) return false;
        ItemStack result = currentRecipe.value().assemble(input, level.registryAccess());
        return insertCraftingOutput(result, true);
    }

    /** Plan every output change first, so a rejected craft never leaves a partial output. */
    private boolean insertCraftingOutput(ItemStack result, boolean simulate) {
        if (result.isEmpty()) return false;
        ItemStack primary = itemHandler.getStackInSlot(CRAFTING_OUTPUT_SLOT);
        if (!primary.isEmpty() && !ItemStack.isSameItemSameComponents(primary, result)) return false;

        ItemStack[] planned = simulate ? null : new ItemStack[1 + CONTAINER_OUTPUT_COUNT];
        int remaining = result.getCount();
        for (int i = 0; i <= CONTAINER_OUTPUT_COUNT && remaining > 0; i++) {
            int slot = i == 0 ? CRAFTING_OUTPUT_SLOT : CONTAINER_OUTPUT_START + i - 1;
            ItemStack existing = itemHandler.getStackInSlot(slot);
            if (!existing.isEmpty() && !ItemStack.isSameItemSameComponents(existing, result)) continue;
            int limit = Math.min(itemHandler.getSlotLimit(slot), result.getMaxStackSize());
            int added = Math.min(remaining, Math.max(0, limit - existing.getCount()));
            if (added > 0) {
                if (!simulate) planned[i] = result.copyWithCount(existing.getCount() + added);
                remaining -= added;
            }
        }
        if (remaining != 0) return false;
        if (!simulate) {
            for (int i = 0; i < planned.length; i++) {
                int slot = i == 0 ? CRAFTING_OUTPUT_SLOT : CONTAINER_OUTPUT_START + i - 1;
                if (planned[i] != null) itemHandler.setStackInSlot(slot, planned[i]);
            }
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
        ItemStack result = currentRecipe.value().assemble(input, level.registryAccess());
        NonNullList<ItemStack> compactRemaining = currentRecipe.value().getRemainingItems(input);
        NonNullList<ItemStack> remaining = NonNullList.withSize(INGREDIENT_SLOT_COUNT, ItemStack.EMPTY);
        for (int i = 0; i < compactRemaining.size(); i++) {
            int x = i % input.width() + positioned.left();
            int y = i / input.width() + positioned.top();
            remaining.set(x + y * 3, compactRemaining.get(i));
        }
        if (!insertCraftingOutput(result, false)) return;

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

        currentRecipe = null;
        recipeOutput = ItemStack.EMPTY;
        attemptToBalance = true;
        finishWork();
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
        if (attemptToBalance) {
            balanceIngredients();
            attemptToBalance = false;
        }

        if (!consumeEnergy()) {
            stopWork();
            return;
        }

        isWorking = true;

        if (progress >= maxProgress) {
            finishCrafting();

            if (canCraft()) {
                isWorking = true;
            }
        }
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
            CONTAINER_OUTPUT_START, CONTAINER_OUTPUT_START + 1, CONTAINER_OUTPUT_START + 2,
            CONTAINER_OUTPUT_START + 3, CONTAINER_OUTPUT_START + 4, CONTAINER_OUTPUT_START + 5,
            CONTAINER_OUTPUT_START + 6, CONTAINER_OUTPUT_START + 7, CONTAINER_OUTPUT_START + 8,
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
        return new int[]{
            CRAFTING_OUTPUT_SLOT,
            CONTAINER_OUTPUT_START, CONTAINER_OUTPUT_START + 1, CONTAINER_OUTPUT_START + 2,
            CONTAINER_OUTPUT_START + 3, CONTAINER_OUTPUT_START + 4, CONTAINER_OUTPUT_START + 5,
            CONTAINER_OUTPUT_START + 6, CONTAINER_OUTPUT_START + 7, CONTAINER_OUTPUT_START + 8
        };
    }

    @Override
    protected int getBatterySlot() {
        return BATTERY_SLOT;
    }

    @Override
    protected boolean canExtractItem(int slot, @Nullable Direction side) {
        return slot == CRAFTING_OUTPUT_SLOT
            || (slot >= CONTAINER_OUTPUT_START && slot < CONTAINER_OUTPUT_START + CONTAINER_OUTPUT_COUNT);
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
    }

    @Override
    public void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
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
