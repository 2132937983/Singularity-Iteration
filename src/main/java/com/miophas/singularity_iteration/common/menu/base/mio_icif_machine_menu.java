package com.miophas.singularity_iteration.common.menu.base;

import com.miophas.singularity_iteration.core.api.MioIcifAPI;
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
 * Base menu class for general machines.
 * Use it for standard processing machines that have input, output, battery and upgrade slots.
 */
@SuppressWarnings("null")
public abstract class mio_icif_machine_menu extends mio_icif_base_menu {

    protected final IItemHandler itemHandler;
    public final ContainerData data;
    @Nullable
    public final Object blockEntity;

    @Nullable
    public Object getBlockEntity() { return blockEntity; }
    public ContainerData getData() { return data; }

    /**
     * Standard constructor.
     * @param type              MenuType
     * @param containerId       the container ID
     * @param machineSlotCount  the total number of machine slots
     * @param playerInventory   the player inventory
     * @param itemHandler       the item handler. If null, the menu creates an empty handler.
     * @param data              the synced container data. If null, the menu creates empty data.
     * @param dataCount         the synced data size, used when data is null
     */
    protected mio_icif_machine_menu(MenuType<?> type, int containerId, int machineSlotCount,
                                     Inventory playerInventory, @Nullable IItemHandler itemHandler,
                                     @Nullable ContainerData data, int dataCount) {
        this(type, containerId, machineSlotCount, playerInventory, itemHandler, data, dataCount, null);
    }

    /**
     * Full constructor with a block entity.
     * @param type              MenuType
     * @param containerId       the container ID
     * @param machineSlotCount  the total number of machine slots
     * @param playerInventory   the player inventory
     * @param itemHandler       the item handler. If null, the menu creates an empty handler.
     * @param data              the synced container data. If null, the menu creates empty data.
     * @param dataCount         the synced data size, used when data is null
     * @param blockEntity       the block entity, can be null
     */
    protected mio_icif_machine_menu(MenuType<?> type, int containerId, int machineSlotCount,
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
     * Legacy constructor that takes a block entity.
     * Subclasses should override getDataSlotCount() and call this constructor through the 5-parameter version.
     */
    protected mio_icif_machine_menu(MenuType<?> type, int containerId, Inventory playerInventory,
                                     Object blockEntity, int machineSlotCount, int dataCount) {
        this(type, containerId, machineSlotCount, playerInventory, 
             blockEntity != null ? getItemHandlerFromBlockEntity(blockEntity) : null,
             null, dataCount, blockEntity);
    }

    /**
     * Legacy constructor. The default dataCount is 5.
     */
    protected mio_icif_machine_menu(MenuType<?> type, int containerId, Inventory playerInventory,
                                     Object blockEntity, int machineSlotCount) {
        this(type, containerId, machineSlotCount, playerInventory, 
             blockEntity != null ? getItemHandlerFromBlockEntity(blockEntity) : null,
             null, 5, blockEntity);
    }

    /** Subclasses must implement this method. It adds the machine slots. */
    protected abstract void addMachineSlots();

    /** Subclasses can override this method. It tells if the player inventory is added. The default is true. */
    protected boolean shouldAddPlayerInventory() { return true; }

    /** Subclasses can override this method. Player inventory start Y. The default is 84. */
    protected int getPlayerInventoryY() { return 84; }

    /** Subclasses can override this method. Player hotbar Y. The default is 142. */
    protected int getPlayerHotbarY() { return 142; }

    /** Subclasses can override this method. It returns the size of the data synchronizer. */
    protected int getDataSlotCount() { return 5; }

    // ===== Slot configuration for subclasses to implement =====

    /** Start index of the input slots. -1 means that there are no input slots. */
    protected int getInputSlotStart() {
        return -1;
    }

    /** Number of input slots. */
    protected int getInputSlotCount() {
        return 0;
    }

    /** Start index of the output slots. -1 means that there are no output slots. */
    protected int getOutputSlotStart() {
        return -1;
    }

    /** Number of output slots. */
    protected int getOutputSlotCount() {
        return 0;
    }

    // ===== 通用数据访问方法 =====

    public int getProgress() {
        return data.getCount() > 0 ? data.get(0) : 0;
    }

    public int getMaxProgress() {
        return data.getCount() > 1 ? data.get(1) : 0;
    }

    public boolean isWorking() {
        return data.getCount() > 2 && data.get(2) == 1;
    }

    public int getEnergy() {
        return data.getCount() > 3 ? data.get(3) : 0;
    }

    public int getMaxEnergy() {
        return data.getCount() > 4 ? data.get(4) : 0;
    }

    /**
     * Gets the progress bar width in pixels.
     *
     * @param maxPixels the maximum bar width in pixels
     * @return the bar width in pixels for the current progress
     */
    public int getProgressPixels(int maxPixels) {
        int progress = getProgress();
        int maxProgress = getMaxProgress();
        if (maxProgress == 0) return 0;
        return (progress * maxPixels) / maxProgress;
    }

    public int getProgressPixels() {
        return getProgressPixels(24);
    }

    /**
     * 获取能量条像素宽度
     *
     * @return 当前能量对应的像素宽度
     */
    public int getEnergyProgressPixels() {
        int energy = getEnergy();
        int maxEnergy = getMaxEnergy();
        if (maxEnergy <= 0) return 0;
        return (int) ((energy * ENERGY_BAR_WIDTH) / maxEnergy);
    }

    /**
     * Sets a synced data value. The Screen calls this from containerTick.
     */
    public void setSyncData(int index, int value) {
        // 0.1.10.6: an overclocked machine can finish an operation in the tick it starts it, so the
        // progress it reports is 0 at every sync; right after an operation the bar shows full
        if (index == progressDataIndex() && blockEntity instanceof com.miophas.singularity_iteration.core.prefab.blockentity.AbstractProcessingMachineBlockEntity machine
                && value == machine.getProgress()) {
            value = machine.getDisplayProgress();
        }
        if (index >= 0 && index < data.getCount()) {
            data.set(index, value);
        }
    }

    /** 0.1.10.6: index of the progress in the synced data (-1: the menu syncs no progress at index 0..n). */
    protected int progressDataIndex() { return 0; }

    // ===== 快速移动逻辑扩展 =====

    @Override
    protected boolean isBattery(ItemStack stack) {
        return MioIcifAPI.instance().getItemAPI().isBattery(stack)
            || stack.getItem() == net.minecraft.world.item.Items.REDSTONE
            || com.miophas.singularity_iteration.core.runtime.energy.FeMachineBridge.dischargeable(stack);
    }

    /**
     * 添加输入槽到容器
     */
    protected void addInputSlot(int index, int x, int y) {
        this.addSlot(new SlotItemHandler(itemHandler, index, x, y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return isValidInput(stack) && itemHandler.isItemValid(index, stack);
            }
        });
    }

    /**
     * Adds an output slot to the container. Players cannot put items into it.
     */
    protected void addOutputSlot(int index, int x, int y) {
        this.addSlot(new SlotItemHandler(itemHandler, index, x, y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
            }
        });
    }

    /**
     * 添加电池槽到容器
     */
    protected void addBatterySlot(int index, int x, int y) {
        this.addSlot(new SlotItemHandler(itemHandler, index, x, y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return isBattery(stack) && itemHandler.isItemValid(index, stack);
            }

            @Override
            public int getMaxStackSize(ItemStack stack) {
                if (stack.getItem() == net.minecraft.world.item.Items.REDSTONE) return 64;
                return 1;
            }
        });
    }

    protected void addUpgradeSlot(int index, int x, int y) {
        super.addUpgradeSlot(itemHandler, index, x, y);
    }

    /**
     * Checks whether an item can go into an input slot.
     * Subclasses can override this method.
     */
    protected boolean isValidInput(ItemStack stack) {
        return true;
    }

    /**
     * Registers the quick-move rules that are specific to the machine.
     * Subclasses call this method in their constructor.
     */
    protected void registerMachineMoveRules() {
        int inputStart = getInputSlotStart();
        int inputCount = getInputSlotCount();
        if (inputStart >= 0 && inputCount > 0) {
            addCustomMoveRule(this::isValidInput, inputStart, inputStart + inputCount);
        }
    }

    /** 电池槽索引，-1表示无电池槽 */
    protected int getBatterySlotIndex() {
        return -1;
    }

    // Upgrade slot range is derived automatically from addUpgradeSlot(...) calls
    // in the base menu; subclasses no longer need to re-declare it.  Overriding
    // getUpgradeSlotStart()/getUpgradeSlotCount() is still supported.

    @Override
    public boolean stillValid(net.minecraft.world.entity.player.Player player) {
        return isMachineMenuValid(player);
    }
}
