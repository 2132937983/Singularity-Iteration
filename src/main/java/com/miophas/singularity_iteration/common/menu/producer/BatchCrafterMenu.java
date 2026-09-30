package com.miophas.singularity_iteration.common.menu.producer;

import com.miophas.singularity_iteration.common.blockentity.producer.mio_icif_batch_crafter;
import com.miophas.singularity_iteration.common.menu.base.mio_icif_machine_menu;
import com.miophas.singularity_iteration.common.menu.slot.PhantomSlot;
import com.miophas.singularity_iteration.common.registry.mio_icif_menus;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.items.SlotItemHandler;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@SuppressWarnings("null")
public class BatchCrafterMenu extends mio_icif_machine_menu {

    public static final int BATTERY_SLOT = 0;
    public static final int UPGRADE_SLOT_START = 1;
    public static final int UPGRADE_SLOT_COUNT = 4;
    public static final int INGREDIENT_SLOT_START = 5;
    public static final int INGREDIENT_SLOT_COUNT = 9;
    public static final int OUTPUT_SLOT_START = 14;
    public static final int CRAFTING_OUTPUT_SLOT = 14;
    public static final int CONTAINER_OUTPUT_START = 15;
    public static final int CONTAINER_OUTPUT_COUNT = 9;
    public static final int SLOT_COUNT = 24;
    public static final int CRAFTING_GRID_SIZE = 9;
    public static final int TOTAL_MACHINE_SLOTS = SLOT_COUNT + CRAFTING_GRID_SIZE;

    public static final int MENU_PHANTOM_START = 1 + UPGRADE_SLOT_COUNT;
    public static final int MENU_CRAFTING_OUTPUT = MENU_PHANTOM_START + CRAFTING_GRID_SIZE;
    public static final int MENU_INGREDIENT_START = MENU_CRAFTING_OUTPUT + 1;
    public static final int MENU_CONTAINER_OUTPUT_START = MENU_INGREDIENT_START + INGREDIENT_SLOT_COUNT;

    public static final int GUI_HEIGHT = 206;

    @Nullable
    private final mio_icif_batch_crafter blockEntity;
    @Nullable
    private BlockPos blockPos;
    private final Level recipeLevel;
    private Collection<RecipeHolder<?>> recipeSnapshot;
    private List<RecipeHolder<CraftingRecipe>> menuRecipes = List.of();
    private final Map<ResourceLocation, Integer> menuRecipeIndices = new HashMap<>();
    private int synchronizedRecipeIndex;
    private int receivedRecipeParts;

    public BatchCrafterMenu(int containerId, Inventory playerInventory, RegistryFriendlyByteBuf data) {
        this(containerId, playerInventory, (mio_icif_batch_crafter) null);
        if (data != null) {
            this.blockPos = data.readBlockPos();
        }
    }

    public BatchCrafterMenu(int containerId, Inventory playerInventory) {
        this(containerId, playerInventory, (mio_icif_batch_crafter) null);
    }

    public BatchCrafterMenu(int containerId, Inventory playerInventory, @Nullable mio_icif_batch_crafter blockEntity) {
        this(containerId, playerInventory, blockEntity, blockEntity != null ? blockEntity.getContainerData() : null);
    }

    public BatchCrafterMenu(int containerId, Inventory playerInventory, @Nullable mio_icif_batch_crafter blockEntity, @Nullable ContainerData data) {
        super(mio_icif_menus.BATCH_CRAFTER_MENU_TYPE.get(), containerId, TOTAL_MACHINE_SLOTS, playerInventory,
            blockEntity != null ? blockEntity.getItemHandler() : null, data, 5, blockEntity);
        this.blockEntity = blockEntity;
        this.blockPos = blockEntity != null ? blockEntity.getBlockPos() : null;
        this.recipeLevel = playerInventory.player.level();
        // Append after the existing machine and binding data, preserving their wire indices.
        for (int part = 0; part < 2; part++) {
            final int shift = part * 16;
            final int partMask = 1 << part;
            addDataSlot(new DataSlot() {
                @Override
                public int get() {
                    return selectedRecipeIndex() >>> shift & 0xffff;
                }

                @Override
                public void set(int value) {
                    synchronizedRecipeIndex = (synchronizedRecipeIndex & ~(0xffff << shift))
                        | ((value & 0xffff) << shift);
                    receivedRecipeParts |= partMask;
                }
            });
        }
    }

    private void refreshMenuRecipes() {
        Collection<RecipeHolder<?>> current = recipeLevel.getRecipeManager().getRecipes();
        if (current == recipeSnapshot) return;
        recipeSnapshot = current;
        menuRecipes = recipeLevel.getRecipeManager().getAllRecipesFor(RecipeType.CRAFTING).stream()
            .sorted(Comparator.comparing(recipe -> recipe.id().toString())).toList();
        menuRecipeIndices.clear();
        for (int i = 0; i < menuRecipes.size(); i++) {
            menuRecipeIndices.put(menuRecipes.get(i).id(), i + 1);
        }
    }

    private int selectedRecipeIndex() {
        if (blockEntity == null) return synchronizedRecipeIndex;
        refreshMenuRecipes();
        RecipeHolder<CraftingRecipe> selected = blockEntity.selectedRecipeForMenu();
        return selected == null ? 0 : menuRecipeIndices.getOrDefault(selected.id(), 0);
    }

    private boolean acceptsMenuIngredient(int gridIndex, ItemStack ingredient) {
        if (blockEntity != null) return blockEntity.acceptsIngredient(gridIndex, ingredient);
        if (receivedRecipeParts != 3 || synchronizedRecipeIndex <= 0) return false;
        refreshMenuRecipes();
        if (synchronizedRecipeIndex > menuRecipes.size()) return false;
        ItemStack[] template = new ItemStack[CRAFTING_GRID_SIZE];
        for (int i = 0; i < template.length; i++) {
            template[i] = slots.get(MENU_PHANTOM_START + i).getItem().copy();
        }
        return mio_icif_batch_crafter.acceptsIngredient(recipeLevel, template,
            menuRecipes.get(synchronizedRecipeIndex - 1), gridIndex, ingredient);
    }

    @Nullable
    public BlockPos getBlockPos() {
        return blockPos;
    }

    @Override
    protected void addMachineSlots() {
        this.addSlot(new SlotItemHandler(itemHandler, BATTERY_SLOT, 8, 62) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return isBattery(stack);
            }
            @Override
            public int getMaxStackSize(ItemStack stack) {
                return 1;
            }
        });

        for (int i = 0; i < UPGRADE_SLOT_COUNT; i++) {
            addUpgradeSlot(itemHandler, UPGRADE_SLOT_START + i, 152, 8 + i * 18);
        }

        for (int y = 0; y < 3; y++) {
            for (int x = 0; x < 3; x++) {
                final int gridIndex = x + y * 3;
                this.addSlot(new PhantomSlot(gridIndex, 30 + x * 18, 17 + y * 18, 1) {
                    @Override
                    public ItemStack getItem() {
                        if (blockEntity != null) return blockEntity.getCraftingGridStack(gridIndex);
                        return super.getItem();
                    }
                    @Override
                    public void set(ItemStack stack) {
                        if (blockEntity != null) {
                            blockEntity.setCraftingGridStack(gridIndex, stack.isEmpty() ? ItemStack.EMPTY : stack.copy());
                        } else {
                            super.set(stack);
                        }
                    }
                });
            }
        }

        this.addSlot(new SlotItemHandler(itemHandler, CRAFTING_OUTPUT_SLOT, 124, 35) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
            }
        });

        for (int i = 0; i < INGREDIENT_SLOT_COUNT; i++) {
            final int ingredientIndex = i;
            this.addSlot(new SlotItemHandler(itemHandler, INGREDIENT_SLOT_START + i, 8 + i * 18, 84) {
                @Override
                public boolean mayPlace(ItemStack stack) {
                    return acceptsMenuIngredient(ingredientIndex, stack);
                }
            });
        }

        for (int i = 0; i < CONTAINER_OUTPUT_COUNT; i++) {
            this.addSlot(new SlotItemHandler(itemHandler, CONTAINER_OUTPUT_START + i, 8 + i * 18, 102) {
                @Override
                public boolean mayPlace(ItemStack stack) {
                    return false;
                }
            });
        }
    }

    /**
     * ic2 ContainerBatchCrafter.handlePlayerSlotShiftClick spreads the shift-clicked stack over
     * every accepting row slot instead of stuffing slots in order.
     */
    private boolean moveIngredientsBalanced(ItemStack stack) {
        if (itemHandler == null || stack.isEmpty()) return false;
        java.util.List<Integer> targets = new java.util.ArrayList<>();
        for (int i = 0; i < INGREDIENT_SLOT_COUNT; i++) {
            if (acceptsMenuIngredient(i, stack)) targets.add(INGREDIENT_SLOT_START + i);
        }
        if (targets.isEmpty()) return false;

        ItemStack one = stack.copyWithCount(1);
        int remaining = stack.getCount();
        boolean moved = false;
        // Fill one level per round so every accepting slot grows evenly.
        while (remaining > 0) {
            targets.sort(Comparator.comparingInt(slot -> {
                ItemStack content = itemHandler.getStackInSlot(slot);
                return content.isEmpty() ? 0 : content.getCount();
            }));
            int insertedThisRound = 0;
            for (int slot : targets) {
                if (remaining <= 0) break;
                if (itemHandler.insertItem(slot, one.copy(), false).isEmpty()) {
                    remaining--;
                    insertedThisRound++;
                    moved = true;
                }
            }
            if (insertedThisRound == 0) break;
        }
        stack.shrink(stack.getCount() - remaining);
        return moved;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack itemstack = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack itemStack1 = slot.getItem();
            itemstack = itemStack1.copy();

            if (isMachineSlot(index)) {
                if (!this.moveItemStackTo(itemStack1, getPlayerInventoryStart(), this.slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else {
                boolean moved = false;
                if (isUpgrade(itemStack1)) {
                    if (this.moveItemStackTo(itemStack1, UPGRADE_SLOT_START, UPGRADE_SLOT_START + UPGRADE_SLOT_COUNT, false)) {
                        moved = true;
                    }
                }
                if (!moved) {
                    if (moveIngredientsBalanced(itemStack1)) {
                        moved = true;
                    }
                }
                if (!moved && isBattery(itemStack1)) {
                    if (this.moveItemStackTo(itemStack1, BATTERY_SLOT, BATTERY_SLOT + 1, false)) {
                        moved = true;
                    }
                }
                if (!moved) {
                    if (!this.moveItemStackTo(itemStack1, getPlayerInventoryStart(), this.slots.size(), true)) {
                        return ItemStack.EMPTY;
                    }
                }
            }

            if (itemStack1.isEmpty()) {
                slot.setByPlayer(ItemStack.EMPTY);
            } else {
                slot.setByPlayer(itemStack1);
            }

            if (itemStack1.getCount() == itemstack.getCount()) {
                return ItemStack.EMPTY;
            }
        }
        return itemstack;
    }

    @Override
    protected int getPlayerInventoryY() { return 124; }

    @Override
    protected int getPlayerHotbarY() { return 182; }

    @Override
    public void clicked(int slotId, int dragType, ClickType clickType, Player player) {
        if (slotId >= 0 && slotId < slots.size()) {
            Slot slot = slots.get(slotId);
            if (slot instanceof PhantomSlot phantomSlot) {
                if (clickType == ClickType.PICKUP && (dragType == 0 || dragType == 1)) {
                    phantomSlot.onSlotClick(dragType, player);
                }
                return;
            }
        }
        super.clicked(slotId, dragType, clickType, player);
    }

    @Override
    protected int getBatterySlotIndex() { return BATTERY_SLOT; }

    @Override
    protected int getUpgradeSlotStart() { return UPGRADE_SLOT_START; }

    @Override
    protected int getUpgradeSlotCount() { return UPGRADE_SLOT_COUNT; }

    @Nullable
    public mio_icif_batch_crafter getBlockEntity() {
        return blockEntity;
    }

    public ItemStack getRecipeOutput() {
        if (blockEntity != null) return blockEntity.getRecipeOutput();
        return ItemStack.EMPTY;
    }
}
