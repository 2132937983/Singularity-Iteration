package com.singularity_iteration.mio_icif.Menu.Producer;

import com.singularity_iteration.mio_icif.Menu.mio_icif_menus;
import com.singularity_iteration.mio_icif.Blocks.entity.producer.mio_icif_solar_distiller;
import com.singularity_iteration.mio_icif.api.MioIcifAPI;
import com.singularity_iteration.mio_icif.Menu.Base.mio_icif_machine_menu;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ContainerData;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.SlotItemHandler;
import org.jetbrains.annotations.Nullable;

@SuppressWarnings({"null"}) public class SolarDistillerMenu extends mio_icif_machine_menu {
    public static final int WATER_INPUT_SLOT = 0;
    public static final int DISTILLED_INPUT_SLOT = 1;
    public static final int WATER_OUTPUT_SLOT = 2;
    public static final int DISTILLED_OUTPUT_SLOT = 3;
    public static final int UPGRADE_SLOT_1 = 4;
    public static final int UPGRADE_SLOT_2 = 5;
    public static final int SLOT_COUNT = 6;

    public SolarDistillerMenu(int containerId, Inventory playerInventory) {
        this(containerId, playerInventory, null, null);
    }

    public SolarDistillerMenu(int containerId, Inventory playerInventory, @Nullable IItemHandler itemHandler) {
        this(containerId, playerInventory, itemHandler, null);
    }

    public SolarDistillerMenu(int containerId, Inventory playerInventory, @Nullable IItemHandler itemHandler, @Nullable ContainerData data) {
        super(mio_icif_menus.SOLAR_DISTILLER_MENU_TYPE.get(), containerId, SLOT_COUNT, playerInventory, itemHandler, data, 4);
    }

    public SolarDistillerMenu(int containerId, Inventory playerInventory, @Nullable mio_icif_solar_distiller blockEntity) {
        this(containerId, playerInventory,
              blockEntity != null ? blockEntity.getItemHandler() : null,
              blockEntity != null ? blockEntity.getContainerData() : null, blockEntity);
    }

    public SolarDistillerMenu(int containerId, Inventory playerInventory, @Nullable IItemHandler itemHandler,
                              @Nullable ContainerData data, @Nullable mio_icif_solar_distiller blockEntity) {
        super(mio_icif_menus.SOLAR_DISTILLER_MENU_TYPE.get(), containerId, SLOT_COUNT, playerInventory,
              itemHandler, data, 4, blockEntity);
    }

    @Override
    protected void addMachineSlots() {
        this.addSlot(new SlotItemHandler(itemHandler, WATER_INPUT_SLOT, 17, 27));
        this.addSlot(new SlotItemHandler(itemHandler, DISTILLED_INPUT_SLOT, 136, 64));
        this.addSlot(new SlotItemHandler(itemHandler, WATER_OUTPUT_SLOT, 17, 45));
        this.addSlot(new SlotItemHandler(itemHandler, DISTILLED_OUTPUT_SLOT, 136, 82));
        this.addUpgradeSlot(UPGRADE_SLOT_1, 152, 8);
        this.addUpgradeSlot(UPGRADE_SLOT_2, 152, 26);
    }

    // GUI高度184，按照原版IC2逻辑调整玩家物品栏位置?
    // 背包Y = 184 - 82 = 102, 快捷栏Y = 184 - 24 = 160
    @Override
    protected int getPlayerInventoryY() { return 102; }

    @Override
    protected int getPlayerHotbarY() { return 160; }

    @Override
    protected int getDataSlotCount() { return 4; }

    public boolean isWorking() { return data.get(0) == 1; }
    public int getProgress() { return data.get(1); }
    public int getWaterAmount() { return data.get(2); }
    public int getDistilledAmount() { return data.get(3); }

    @Override
    public void broadcastChanges() {
        super.broadcastChanges();
        if (blockEntity instanceof mio_icif_solar_distiller distiller) {
            setSyncData(0, distiller.isWorking() ? 1 : 0);
            setSyncData(1, distiller.getProgress());
            setSyncData(2, distiller.getWaterTank().getFluidAmount());
            setSyncData(3, distiller.getDistilledTank().getFluidAmount());
        }
    }

    protected void addCustomMoveRules() {
        addCustomMoveRule(stack -> MioIcifAPI.instance().getItemAPI().isUpgrade(stack), UPGRADE_SLOT_1, SLOT_COUNT);
    }
}