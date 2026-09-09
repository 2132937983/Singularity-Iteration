package com.singularity_iteration.mio_icif.Menu.Producer;

import com.singularity_iteration.mio_icif.Blocks.entity.producer.mio_icif_batch_crafter;
import com.singularity_iteration.mio_icif.Menu.Base.mio_icif_machine_menu;
import com.singularity_iteration.mio_icif.Menu.Slot.PhantomSlot;
import com.singularity_iteration.mio_icif.Menu.mio_icif_menus;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.SlotItemHandler;
import org.jetbrains.annotations.Nullable;

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
                    if (blockEntity != null) {
                        return blockEntity.acceptsIngredient(ingredientIndex, stack);
                    }
                    return !isUpgrade(stack);
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
                    if (this.moveItemStackTo(itemStack1, MENU_INGREDIENT_START, MENU_INGREDIENT_START + INGREDIENT_SLOT_COUNT, false)) {
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