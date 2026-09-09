package com.singularity_iteration.mio_icif.Menu.Base;

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
 * ??�电???Menu ?���?
 * ????��于�??种�?�电设置??�???��?��???��??��???��??��?��??
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
     * @param containerId     容器ID
     * @param machineSlotCount ?��?��槽位?��?�数
     * @param playerInventory ?��家�?��?????
     * @param itemHandler     ??��??�?????���??��为null�?
     * @param data            ?��?��??�步?���??��为null�?
     * @param dataCount       ?��?��??�步?��大�??
     */
    protected mio_icif_generator_menu(MenuType<?> type, int containerId, int machineSlotCount,
                                       Inventory playerInventory, @Nullable IItemHandler itemHandler,
                                       @Nullable ContainerData data, int dataCount) {
        this(type, containerId, machineSlotCount, playerInventory, itemHandler, data, dataCount, null);
    }

    /**
     * 完整?????�函?���?带blockEntity�?
     */
    protected mio_icif_generator_menu(MenuType<?> type, int containerId, int machineSlotCount,
                                       Inventory playerInventory, @Nullable IItemHandler itemHandler,
                                       @Nullable ContainerData data, int dataCount, @Nullable Object blockEntity) {
        super(type, containerId, machineSlotCount, machineSlotCount);
        this.itemHandler = itemHandler != null ? itemHandler : new ItemStackHandler(machineSlotCount);
        this.data = data != null ? data : new SimpleContainerData(dataCount);
        this.blockEntity = blockEntity;
        this.addDataSlots(this.data);
        this.addMachineSlots();
        if (shouldAddPlayerInventory()) {
            this.addPlayerInventory(playerInventory, getPlayerInventoryY(), getPlayerHotbarY());
        }
    }

    /**
     * ??�容?���????????????�函?���?�???��??�?带dataCount�?
     */
    protected mio_icif_generator_menu(int containerId, Inventory playerInventory, @Nullable IItemHandler itemHandler, int dataCount) {
        this(null, containerId, 0, playerInventory, itemHandler, null, dataCount, null);
    }

    /**
     * ??�容?���????????????�函?���?�???��??，�?�认dataCount=5�?
     */
    protected mio_icif_generator_menu(int containerId, Inventory playerInventory, @Nullable IItemHandler itemHandler) {
        this(null, containerId, 0, playerInventory, itemHandler, null, 5, null);
    }

    /** 子类�?须�?�现：添??�机?��槽位??*/
    protected abstract void addMachineSlots();

    /** 子类?��以�?��?��?�是?��添�?�玩家�?????，�?�认true */
    protected boolean shouldAddPlayerInventory() { return true; }

    /** 子类?��以�?��?��?�玩家�?????起�?�Y??��??，�?��??4 */
    protected int getPlayerInventoryY() { return 84; }

    /** 子类?��以�?��?��?�玩家快?��??�Y??��??，�?��??42 */
    protected int getPlayerHotbarY() { return 142; }

    /** 子类?��以�?�现：�?��?�数?��??�步?��大�?? */
    protected int getDataSlotCount() { return 5; }

    // ===== 子类?��??��?��??槽位?��?�置 =====

    /** ?????�槽索引?��??-1表示??��????�槽 */
    protected int getFuelSlotIndex() {
        return -1;
    }

    /** ??�却???槽索引�??1表示??��?�却???�?*/
    protected int getCoolantSlotIndex() {
        return -1;
    }

    // ===== ??�用?��?��访问?���? =====

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

    // ===== ??�用槽位?�添??�方法? =====

    protected void addFuelSlot(int index, int x, int y) {
        this.addSlot(new SlotItemHandler(itemHandler, index, x, y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return mio_icif_generator_menu.this.isFuel(stack);
            }
        });
    }

    protected void addCoolantSlot(int index, int x, int y) {
        this.addSlot(new SlotItemHandler(itemHandler, index, x, y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return mio_icif_generator_menu.this.isCoolant(stack);
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
                return mio_icif_generator_menu.this.isBattery(stack);
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
        return true;
    }
}