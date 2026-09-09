package com.singularity_iteration.mio_icif.Menu.Producer;

import com.singularity_iteration.mio_icif.Menu.mio_icif_menus;
import com.singularity_iteration.mio_icif.Blocks.entity.producer.mio_icif_condenser;
import com.singularity_iteration.mio_icif.Menu.Base.mio_icif_machine_menu;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ContainerData;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.SlotItemHandler;
import org.jetbrains.annotations.Nullable;

@SuppressWarnings({"null"}) public class CondenserMenu extends mio_icif_machine_menu {
    public static final int VENT_SLOT_1 = 0;
    public static final int VENT_SLOT_2 = 1;
    public static final int VENT_SLOT_3 = 2;
    public static final int VENT_SLOT_4 = 3;
    public static final int INPUT_BUCKET_SLOT = 4;
    public static final int EMPTY_BUCKET_SLOT = 5;
    public static final int BATTERY_SLOT = 6;
    public static final int UPGRADE_SLOT = 7;
    public static final int SLOT_COUNT = 8;

    public CondenserMenu(int containerId, Inventory playerInventory) {
        this(containerId, playerInventory, null, null);
    }

    public CondenserMenu(int containerId, Inventory playerInventory, @Nullable IItemHandler itemHandler) {
        this(containerId, playerInventory, itemHandler, null);
    }

    public CondenserMenu(int containerId, Inventory playerInventory, @Nullable IItemHandler itemHandler, @Nullable ContainerData data) {
        super(mio_icif_menus.CONDENSER_MENU_TYPE.get(), containerId, SLOT_COUNT, playerInventory, itemHandler, data, 6);
    }

    public CondenserMenu(int containerId, Inventory playerInventory, @Nullable mio_icif_condenser blockEntity) {
        this(containerId, playerInventory,
              blockEntity != null ? blockEntity.getItemHandler() : null,
              blockEntity != null ? blockEntity.getContainerData() : null, blockEntity);
    }

    public CondenserMenu(int containerId, Inventory playerInventory, @Nullable IItemHandler itemHandler,
                         @Nullable ContainerData data, @Nullable mio_icif_condenser blockEntity) {
        super(mio_icif_menus.CONDENSER_MENU_TYPE.get(), containerId, SLOT_COUNT, playerInventory,
              itemHandler, data, 6, blockEntity);
    }

    @Override
    protected void addMachineSlots() {
        this.addSlot(new SlotItemHandler(itemHandler, VENT_SLOT_1, 26, 26));
        this.addSlot(new SlotItemHandler(itemHandler, VENT_SLOT_2, 26, 44));
        this.addSlot(new SlotItemHandler(itemHandler, VENT_SLOT_3, 134, 26));
        this.addSlot(new SlotItemHandler(itemHandler, VENT_SLOT_4, 134, 44));
        this.addSlot(new SlotItemHandler(itemHandler, INPUT_BUCKET_SLOT, 26, 73));
        this.addSlot(new SlotItemHandler(itemHandler, EMPTY_BUCKET_SLOT, 134, 73));
        this.addSlot(new SlotItemHandler(itemHandler, BATTERY_SLOT, 8, 44));
        this.addUpgradeSlot(UPGRADE_SLOT, 152, 73);
    }

    // GUI高度184，按照原版IC2逻辑调整玩家物品栏位置?
    // 背包Y = 184 - 82 = 102, 快捷栏Y = 184 - 24 = 160
    @Override
    protected int getPlayerInventoryY() { return 102; }

    @Override
    protected int getPlayerHotbarY() { return 160; }

    public boolean isWorking() { return data.get(0) == 1; }
    public int getEnergy() { return data.get(1); }
    public int getMaxEnergy() { return data.get(2); }
    public int getProgress() { return data.get(3); }
    public int getSteamAmount() { return data.get(4); }
    public int getDistilledAmount() { return data.get(5); }

    @Override
    public void broadcastChanges() {
        super.broadcastChanges();
        if (blockEntity instanceof mio_icif_condenser condenser) {
            setSyncData(0, condenser.isWorking() ? 1 : 0);
            setSyncData(1, (int) condenser.getEnergyStorage().getAmount());
            setSyncData(2, (int) condenser.getEnergyStorage().getCapacity());
            setSyncData(3, condenser.getProgress());
            setSyncData(4, condenser.getSteamTank().getFluidAmount());
            setSyncData(5, condenser.getDistilledTank().getFluidAmount());
        }
    }
}