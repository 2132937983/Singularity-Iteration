package com.miophas.singularity_iteration.common.menu.base;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.items.SlotItemHandler;
import org.jetbrains.annotations.Nullable;

/**
 * Base menu for generators.
 * It holds the shared slot setup and data sync for all generator types.
 */
@SuppressWarnings("null")
public abstract class mio_icif_generator_menu extends mio_icif_base_menu {

    protected final IItemHandler itemHandler;
    public final ContainerData data;
    @Nullable
    public final Object blockEntity;

    @Nullable
    public Object getBlockEntity() { return blockEntity; }
    public ContainerData getData() { return data; }

    /**
     * @param type            MenuType
     * @param containerId     the container ID
     * @param machineSlotCount the number of machine slots
     * @param playerInventory the player inventory
     * @param itemHandler     the machine item handler, can be null
     * @param data            the synced container data, can be null
     * @param dataCount       the size of the synced data
     */
    protected mio_icif_generator_menu(MenuType<?> type, int containerId, int machineSlotCount,
                                       Inventory playerInventory, @Nullable IItemHandler itemHandler,
                                       @Nullable ContainerData data, int dataCount) {
        this(type, containerId, machineSlotCount, playerInventory, itemHandler, data, dataCount, null);
    }

    /**
     * Full constructor with a block entity.
     */
    protected mio_icif_generator_menu(MenuType<?> type, int containerId, int machineSlotCount,
                                       Inventory playerInventory, @Nullable IItemHandler itemHandler,
                                       @Nullable ContainerData data, int dataCount, @Nullable Object blockEntity) {
        super(type, containerId, machineSlotCount, machineSlotCount);
        this.itemHandler = createMenuItemHandler(playerInventory, machineSlotCount, itemHandler, this::getBlockEntity);
        this.data = data != null ? data : new SimpleContainerData(dataCount);
        this.blockEntity = blockEntity;
        this.addDataSlots(this.data);
        this.addMachineBindingData();
        this.addMachineSlots();
        if (shouldAddPlayerInventory()) {
            this.addPlayerInventory(playerInventory, getPlayerInventoryY(), getPlayerHotbarY());
        }
    }

    /**
     * Client-side constructor with a specified dataCount.
     */
    protected mio_icif_generator_menu(int containerId, Inventory playerInventory, @Nullable IItemHandler itemHandler, int dataCount) {
        this(null, containerId, 0, playerInventory, itemHandler, null, dataCount, null);
    }

    /**
     * Client-side constructor. The default dataCount is 5.
     */
    protected mio_icif_generator_menu(int containerId, Inventory playerInventory, @Nullable IItemHandler itemHandler) {
        this(null, containerId, 0, playerInventory, itemHandler, null, 5, null);
    }

    /** Subclasses must implement this method to add the machine slots. */
    protected abstract void addMachineSlots();

    /** Subclasses can override this method to skip the player inventory. The default is true. */
    protected boolean shouldAddPlayerInventory() { return true; }

    /** Subclasses can override this method to set the player inventory start Y. The default is 84. */
    protected int getPlayerInventoryY() { return 84; }

    /** Subclasses can override this method to set the player hotbar Y. The default is 142. */
    protected int getPlayerHotbarY() { return 142; }

    /** Subclasses can override this method to set the synced data size. */
    protected int getDataSlotCount() { return 5; }

    // ===== Slot configuration that subclasses can override =====

    /** Fuel slot index. -1 means no fuel slot. */
    protected int getFuelSlotIndex() {
        return -1;
    }

    /** Coolant slot index. -1 means no coolant slot. */
    protected int getCoolantSlotIndex() {
        return -1;
    }

    // ===== Shared data accessors =====

    public int getEnergy() {
        return data.getCount() > 0 ? data.get(0) : 0;
    }

    public int getMaxEnergy() {
        return data.getCount() > 1 ? data.get(1) : 0;
    }

    public int getDisplayEnergy() {
        return getEnergy();
    }

    public int getDisplayMaxEnergy() {
        return getMaxEnergy();
    }

    public int getOutput() {
        return data.getCount() > 2 ? data.get(2) : 0;
    }

    public boolean isGenerating() {
        return data.getCount() > 3 && data.get(3) == 1;
    }

    public int getEnergyProgressPixels() {
        int energy = getEnergy();
        int maxEnergy = getMaxEnergy();
        if (maxEnergy <= 0) return 0;
        return (int) ((energy * ENERGY_BAR_WIDTH) / maxEnergy);
    }

    // ===== Shared slot helper methods =====

    protected void addFuelSlot(int index, int x, int y) {
        this.addSlot(new SlotItemHandler(itemHandler, index, x, y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return mio_icif_generator_menu.this.isFuel(stack) && itemHandler.isItemValid(index, stack);
            }
        });
    }

    protected void addCoolantSlot(int index, int x, int y) {
        this.addSlot(new SlotItemHandler(itemHandler, index, x, y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return mio_icif_generator_menu.this.isCoolant(stack) && itemHandler.isItemValid(index, stack);
            }
        });
    }

    protected boolean isCoolant(ItemStack stack) {
        return false;
    }

    protected boolean isFuel(ItemStack stack) {
        return false;
    }

    protected void addBatterySlot(int index, int x, int y) {
        this.addSlot(new SlotItemHandler(itemHandler, index, x, y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return mio_icif_generator_menu.this.isBattery(stack) && itemHandler.isItemValid(index, stack);
            }

            @Override
            public int getMaxStackSize(ItemStack stack) {
                if (stack.getItem() == net.minecraft.world.item.Items.REDSTONE) return 64;
                return 1;
            }
        });
    }

    protected boolean isBattery(ItemStack stack) {
        return false;
    }

    protected void addUpgradeSlot(int index, int x, int y) {
        super.addUpgradeSlot(itemHandler, index, x, y);
    }

    @Override
    public void setSyncData(int index, int value) {
        if (index >= 0 && index < data.getCount()) {
            data.set(index, value);
        }
    }

    @Override
    public boolean stillValid(net.minecraft.world.entity.player.Player player) {
        return isMachineMenuValid(player);
    }
}
