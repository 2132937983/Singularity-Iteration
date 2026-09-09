package com.singularity_iteration.mio_icif.Menu.Producer;

import com.singularity_iteration.mio_icif.Menu.mio_icif_menus;
import com.singularity_iteration.mio_icif.Blocks.entity.producer.mio_icif_recycler_elc;
import com.singularity_iteration.mio_icif.Menu.Base.mio_icif_machine_menu;
import com.singularity_iteration.mio_icif.api.MioIcifAPI;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.SlotItemHandler;
import org.jetbrains.annotations.Nullable;

/**
 * 回收机的容器菜单
 */
@SuppressWarnings({"null"}) public class RecyclerElcMenu extends mio_icif_machine_menu {

    public static final int INPUT_SLOT = 0;
    public static final int BATTERY_SLOT = 1;
    public static final int OUTPUT_SLOT = 2;
    public static final int UPGRADE_SLOT_START = 3;
    public static final int UPGRADE_SLOT_COUNT = 4;
    public static final int SLOT_COUNT = 7;

    private static final int INPUT_X = 56;
    private static final int INPUT_Y = 17;
    private static final int BATTERY_X = 56;
    private static final int BATTERY_Y = 53;
    private static final int OUTPUT_X = 116;
    private static final int OUTPUT_Y = 35;
    private static final int UPGRADE_X = 152;
    private static final int[] UPGRADE_Y = {8, 26, 44, 62};

    private static final int DATA_PROGRESS = 0;
    private static final int DATA_MAX_PROGRESS = 1;
    private static final int DATA_ENERGY = 2;
    private static final int DATA_MAX_ENERGY = 3;
    private static final int DATA_COUNT = 4;

    public RecyclerElcMenu(int containerId, Inventory playerInventory) {
        this(containerId, playerInventory, null, null);
    }

    public RecyclerElcMenu(int containerId, Inventory playerInventory, @Nullable IItemHandler itemHandler) {
        this(containerId, playerInventory, itemHandler, null);
    }

    public RecyclerElcMenu(int containerId, Inventory playerInventory, @Nullable IItemHandler itemHandler, @Nullable ContainerData data) {
        super(mio_icif_menus.RECYCLER_ELC_MENU_TYPE.get(), containerId, SLOT_COUNT, playerInventory, itemHandler, data, DATA_COUNT);
    }

    public RecyclerElcMenu(int containerId, Inventory playerInventory, @Nullable mio_icif_recycler_elc blockEntity) {
        this(containerId, playerInventory,
              blockEntity != null ? blockEntity.getItemHandler() : null,
              blockEntity != null ? blockEntity.getContainerData() : null, blockEntity);
    }

    public RecyclerElcMenu(int containerId, Inventory playerInventory, @Nullable IItemHandler itemHandler,
                           @Nullable ContainerData data, @Nullable mio_icif_recycler_elc blockEntity) {
        super(mio_icif_menus.RECYCLER_ELC_MENU_TYPE.get(), containerId, SLOT_COUNT, playerInventory,
              itemHandler, data, DATA_COUNT, blockEntity);
    }

    public void setSyncData(int progress, int maxProgress, int energy, int maxEnergy) {
        this.data.set(DATA_PROGRESS, progress);
        this.data.set(DATA_MAX_PROGRESS, maxProgress);
        this.data.set(DATA_ENERGY, energy);
        this.data.set(DATA_MAX_ENERGY, maxEnergy);
    }

    public int getProgress() {
        return this.data.get(DATA_PROGRESS);
    }

    public int getMaxProgress() {
        return this.data.get(DATA_MAX_PROGRESS);
    }

    public int getEnergy() {
        return this.data.get(DATA_ENERGY);
    }

    public int getMaxEnergy() {
        return this.data.get(DATA_MAX_ENERGY);
    }

    @Override
    public void broadcastChanges() {
        super.broadcastChanges();
        if (blockEntity instanceof mio_icif_recycler_elc recycler) {
            setSyncData(DATA_PROGRESS, recycler.getProgress());
            setSyncData(DATA_MAX_PROGRESS, recycler.getMaxProgress());
            setSyncData(DATA_ENERGY, (int) recycler.getEnergyStorage().getAmount());
            setSyncData(DATA_MAX_ENERGY, (int) recycler.getEnergyStorage().getCapacity());
        }
    }

    @Override
    protected void addMachineSlots() {
        this.addSlot(new SlotItemHandler(itemHandler, INPUT_SLOT, INPUT_X, INPUT_Y));
        this.addSlot(new SlotItemHandler(itemHandler, BATTERY_SLOT, BATTERY_X, BATTERY_Y));
        this.addSlot(new SlotItemHandler(itemHandler, OUTPUT_SLOT, OUTPUT_X, OUTPUT_Y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
            }
        });
        for (int i = 0; i < UPGRADE_SLOT_COUNT; i++) {
            addUpgradeSlot(itemHandler, UPGRADE_SLOT_START + i, UPGRADE_X, UPGRADE_Y[i]);
        }
    }

    @Override
    protected void registerMachineMoveRules() {
        super.registerMachineMoveRules();
        addCustomMoveRule(stack -> MioIcifAPI.instance().getItemAPI().isUpgrade(stack), UPGRADE_SLOT_START, UPGRADE_SLOT_START + UPGRADE_SLOT_COUNT);
    }

    @Override
    protected int getUpgradeSlotStart() { return UPGRADE_SLOT_START; }

    @Override
    protected int getUpgradeSlotCount() { return UPGRADE_SLOT_COUNT; }
}