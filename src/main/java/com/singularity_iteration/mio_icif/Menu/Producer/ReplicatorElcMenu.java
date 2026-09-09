package com.singularity_iteration.mio_icif.Menu.Producer;

import com.singularity_iteration.mio_icif.Menu.mio_icif_menus;
import com.singularity_iteration.mio_icif.Blocks.entity.producer.mio_icif_replicator_elc;
import com.singularity_iteration.mio_icif.Menu.Base.mio_icif_machine_menu;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.SlotItemHandler;
import org.jetbrains.annotations.Nullable;

/**
 * 复制机的容器菜单独?
 */
@SuppressWarnings({"null"}) public class ReplicatorElcMenu extends mio_icif_machine_menu {

    // 槽位索引
    public static final int UU_CELL_SLOT = 0;
    public static final int EMPTY_CELL_SLOT = 1;
    public static final int MEMORY_SLOT = 2;
    public static final int OUTPUT_SLOT = 3;
    public static final int BATTERY_SLOT = 4;
    public static final int UPGRADE_SLOT_START = 5;
    public static final int SLOT_COUNT = 9;

    // 槽位位置
    private static final int UU_CELL_X = 8;
    private static final int UU_CELL_Y = 27;
    private static final int EMPTY_CELL_X = 8;
    private static final int EMPTY_CELL_Y = 72;
    private static final int MEMORY_X = 91;
    private static final int MEMORY_Y = 17;
    private static final int OUTPUT_X = 91;
    private static final int OUTPUT_Y = 59;
    private static final int BATTERY_X = 152;
    private static final int BATTERY_Y = 83;
    private static final int UPGRADE_BASE_X = 152;
    private static final int UPGRADE_BASE_Y = 8;

    // 数据同步索引
    private static final int DATA_PROGRESS = 0;
    private static final int DATA_MAX_PROGRESS = 1;
    private static final int DATA_IS_REPLICATING = 2;
    private static final int DATA_ENERGY = 3;
    private static final int DATA_MAX_ENERGY = 4;
    private static final int DATA_FLUID_AMOUNT = 5;
    private static final int DATA_FLUID_CAPACITY = 6;
    private static final int DATA_WORK_MODE = 7;
    private static final int DATA_COUNT = 8;

    public ReplicatorElcMenu(int containerId, Inventory playerInventory) {
        this(containerId, playerInventory, null, null);
    }

    public ReplicatorElcMenu(int containerId, Inventory playerInventory, @Nullable IItemHandler itemHandler) {
        this(containerId, playerInventory, itemHandler, null);
    }

    // GUI高度184，按照原版IC2逻辑调整玩家物品栏位置?
    // 背包Y = 184 - 82 = 102, 快捷栏Y = 184 - 24 = 160
    @Override
    protected int getPlayerInventoryY() { return 102; }

    @Override
    protected int getPlayerHotbarY() { return 160; }

    public ReplicatorElcMenu(int containerId, Inventory playerInventory, @Nullable IItemHandler itemHandler, @Nullable ContainerData data) {
        super(mio_icif_menus.REPLICATOR_ELC_MENU_TYPE.get(), containerId, SLOT_COUNT, playerInventory, itemHandler, data, DATA_COUNT);
    }

    public ReplicatorElcMenu(int containerId, Inventory playerInventory, @Nullable mio_icif_replicator_elc blockEntity) {
        super(mio_icif_menus.REPLICATOR_ELC_MENU_TYPE.get(), containerId, SLOT_COUNT, playerInventory,
              blockEntity != null ? blockEntity.getItemHandler() : null,
              blockEntity != null ? blockEntity.getContainerData() : null, DATA_COUNT, blockEntity);
    }

    public void setSyncData(int progress, int maxProgress, boolean isReplicating, int energy, int maxEnergy, int fluidAmount, int fluidCapacity, int workMode) {
        this.data.set(DATA_PROGRESS, progress);
        this.data.set(DATA_MAX_PROGRESS, maxProgress);
        this.data.set(DATA_IS_REPLICATING, isReplicating ? 1 : 0);
        this.data.set(DATA_ENERGY, energy);
        this.data.set(DATA_MAX_ENERGY, maxEnergy);
        this.data.set(DATA_FLUID_AMOUNT, fluidAmount);
        this.data.set(DATA_FLUID_CAPACITY, fluidCapacity);
        this.data.set(DATA_WORK_MODE, workMode);
    }

    public int getProgress() {
        return this.data.get(DATA_PROGRESS);
    }

    public int getMaxProgress() {
        return this.data.get(DATA_MAX_PROGRESS);
    }

    public boolean isReplicating() {
        return this.data.get(DATA_IS_REPLICATING) == 1;
    }

    public int getEnergy() {
        return this.data.get(DATA_ENERGY);
    }

    public int getMaxEnergy() {
        return this.data.get(DATA_MAX_ENERGY);
    }

    public int getFluidAmount() {
        return this.data.get(DATA_FLUID_AMOUNT);
    }

    public int getFluidCapacity() {
        return this.data.get(DATA_FLUID_CAPACITY);
    }

    public int getWorkMode() {
        return this.data.get(DATA_WORK_MODE);
    }

    @Override
    public boolean clickMenuButton(Player player, int buttonId) {
        if (this.blockEntity instanceof mio_icif_replicator_elc replicator) {
            switch (buttonId) {
                case 0: // 停止按钮
                    replicator.setWorkMode(0);
                    return true;
                case 1: // 单次按钮
                    replicator.setWorkMode(1);
                    return true;
                case 2: // 循环按钮
                    replicator.setWorkMode(2);
                    return true;
                default:
                    return false;
            }
        }
        return super.clickMenuButton(player, buttonId);
    }

    @Override
    protected void addMachineSlots() {
        // 单元输入�?(8, 27)
        this.addSlot(new SlotItemHandler(itemHandler, UU_CELL_SLOT, UU_CELL_X, UU_CELL_Y));
        // 空单元输出槽 (8, 72)
        this.addSlot(new SlotItemHandler(itemHandler, EMPTY_CELL_SLOT, EMPTY_CELL_X, EMPTY_CELL_Y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
            }
        });
        // 记忆水晶�?(91, 17)
        this.addSlot(new SlotItemHandler(itemHandler, MEMORY_SLOT, MEMORY_X, MEMORY_Y));
        // 输出�?(91, 59)
        this.addSlot(new SlotItemHandler(itemHandler, OUTPUT_SLOT, OUTPUT_X, OUTPUT_Y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
            }
        });
        // 电池�?(152, 83)
        this.addSlot(new SlotItemHandler(itemHandler, BATTERY_SLOT, BATTERY_X, BATTERY_Y));
        // 升级�?(152, 8) 开始，排成一�?
        for (int i = 0; i < 4; i++) {
            addUpgradeSlot(itemHandler, UPGRADE_SLOT_START + i, UPGRADE_BASE_X, UPGRADE_BASE_Y + i * 18);
        }
    }
}