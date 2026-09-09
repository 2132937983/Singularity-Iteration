package com.singularity_iteration.mio_icif.Menu.Producer;

import com.singularity_iteration.mio_icif.Menu.mio_icif_menus;
import com.singularity_iteration.mio_icif.Blocks.entity.producer.mio_icif_pattern_storage;
import com.singularity_iteration.mio_icif.Menu.Base.mio_icif_machine_menu;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.SlotItemHandler;
import org.jetbrains.annotations.Nullable;

/**
 * 模式存储机的容器菜单独?
 */
@SuppressWarnings({"null"}) public class PatternStorageMenu extends mio_icif_machine_menu {

    public static final int MEMORY_SLOT = 0;
    public static final int SLOT_COUNT = 1;

    private static final int MEMORY_X = 80;
    private static final int MEMORY_Y = 35;

    private static final int DATA_ENERGY = 0;
    private static final int DATA_MAX_ENERGY = 1;
    private static final int DATA_CURRENT_INDEX = 2;
    private static final int DATA_MAX_INDEX = 3;
    private static final int DATA_UU_COST_LOW = 4;
    private static final int DATA_UU_COST_HIGH = 5;
    private static final int DATA_EU_COST_LOW = 6;
    private static final int DATA_EU_COST_HIGH = 7;
    private static final int DATA_COUNT = 8;

    public PatternStorageMenu(int containerId, Inventory playerInventory) {
        this(containerId, playerInventory, null, null);
    }

    public PatternStorageMenu(int containerId, Inventory playerInventory, @Nullable IItemHandler itemHandler) {
        this(containerId, playerInventory, itemHandler, null);
    }

    public PatternStorageMenu(int containerId, Inventory playerInventory, @Nullable IItemHandler itemHandler, @Nullable ContainerData data) {
        super(mio_icif_menus.PATTERN_STORAGE_MENU_TYPE.get(), containerId, SLOT_COUNT, playerInventory, itemHandler, data, DATA_COUNT);
    }

    public PatternStorageMenu(int containerId, Inventory playerInventory, @Nullable mio_icif_pattern_storage blockEntity) {
        super(mio_icif_menus.PATTERN_STORAGE_MENU_TYPE.get(), containerId, SLOT_COUNT, playerInventory,
              blockEntity != null ? blockEntity.getItemHandler() : null,
              blockEntity != null ? blockEntity.getContainerData() : null, DATA_COUNT, blockEntity);
    }

    @Override
    public mio_icif_pattern_storage getBlockEntity() {
        return this.blockEntity instanceof mio_icif_pattern_storage be ? be : null;
    }

    public int getCurrentIndex() {
        return this.data.get(DATA_CURRENT_INDEX);
    }

    public int getMaxIndex() {
        return this.data.get(DATA_MAX_INDEX);
    }

    public ItemStack getCurrentPattern() {
        var be = getBlockEntity();
        if (be != null) {
            return be.getCurrentPattern();
        }
        return ItemStack.EMPTY;
    }

    public double getCurrentUuCost() {
        long bits = ((long) this.data.get(DATA_UU_COST_HIGH) << 32) | (this.data.get(DATA_UU_COST_LOW) & 0xFFFFFFFFL);
        return Double.longBitsToDouble(bits);
    }

    public long getCurrentEuCost() {
        return (long) this.data.get(DATA_EU_COST_HIGH) << 16 | (this.data.get(DATA_EU_COST_LOW) & 0xFFFFL);
    }

    public void setSyncData(int energy, int maxEnergy) {
        this.data.set(DATA_ENERGY, energy);
        this.data.set(DATA_MAX_ENERGY, maxEnergy);
    }

    public int getEnergy() {
        return this.data.get(DATA_ENERGY);
    }

    public int getMaxEnergy() {
        return this.data.get(DATA_MAX_ENERGY);
    }

    @Override
    protected void addMachineSlots() {
        this.addSlot(new SlotItemHandler(itemHandler, MEMORY_SLOT, MEMORY_X, MEMORY_Y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.getItem() instanceof com.singularity_iteration.mio_icif.Items.Resource.mio_icif_memory;
            }

            @Override
            public int getMaxStackSize() {
                return 1;
            }
        });
    }
}