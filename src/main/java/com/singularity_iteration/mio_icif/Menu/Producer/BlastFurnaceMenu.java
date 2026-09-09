package com.singularity_iteration.mio_icif.Menu.Producer;

import com.singularity_iteration.mio_icif.Menu.mio_icif_menus;
import com.singularity_iteration.mio_icif.Blocks.entity.producer.mio_icif_blast_furnace;
import com.singularity_iteration.mio_icif.Items.Cell.mio_icif_cells;
import com.singularity_iteration.mio_icif.Blocks.Environment.fluid.mio_icif_fluids;
import com.singularity_iteration.mio_icif.Menu.Base.mio_icif_machine_menu;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.SlotItemHandler;
import org.jetbrains.annotations.Nullable;

@SuppressWarnings({"null"}) public class BlastFurnaceMenu extends mio_icif_machine_menu {
    public static final int INPUT_SLOT = 0;
    public static final int OUTPUT_SLOT_1 = 1;
    public static final int OUTPUT_SLOT_2 = 2;
    public static final int AIR_CELL_SLOT = 3;
    public static final int EMPTY_CELL_SLOT = 4;
    public static final int UPGRADE_SLOT_START = 5;
    public static final int SLOT_COUNT = 7;

    public BlastFurnaceMenu(int containerId, Inventory playerInventory) {
        this(containerId, playerInventory, null, null);
    }

    public BlastFurnaceMenu(int containerId, Inventory playerInventory, @Nullable net.neoforged.neoforge.items.IItemHandler itemHandler) {
        this(containerId, playerInventory, itemHandler, null);
    }

    public BlastFurnaceMenu(int containerId, Inventory playerInventory, @Nullable net.neoforged.neoforge.items.IItemHandler itemHandler, @Nullable ContainerData data) {
        super(mio_icif_menus.BLAST_FURNACE_MENU_TYPE.get(), containerId, SLOT_COUNT, playerInventory, itemHandler, data, 6);
    }

    public BlastFurnaceMenu(int containerId, Inventory playerInventory, @Nullable mio_icif_blast_furnace blockEntity) {
        super(mio_icif_menus.BLAST_FURNACE_MENU_TYPE.get(), containerId, SLOT_COUNT, playerInventory,
              blockEntity != null ? blockEntity.getItemHandler() : null,
              blockEntity != null ? blockEntity.getContainerData() : null, 6, blockEntity);
    }

    @Override
    protected void addMachineSlots() {
        this.addSlot(new SlotItemHandler(itemHandler, INPUT_SLOT, 35, 33));
        this.addSlot(new SlotItemHandler(itemHandler, OUTPUT_SLOT_1, 134, 56) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
            }
        });
        this.addSlot(new SlotItemHandler(itemHandler, OUTPUT_SLOT_2, 152, 56) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
            }
        });
        this.addSlot(new SlotItemHandler(itemHandler, AIR_CELL_SLOT, 26, 56) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return mio_icif_cells.isCellContainingFluid(stack, mio_icif_fluids.AIR.get());
            }
        });
        this.addSlot(new SlotItemHandler(itemHandler, EMPTY_CELL_SLOT, 44, 56) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
            }
        });
        this.addUpgradeSlot(UPGRADE_SLOT_START, 152, 8);
        this.addUpgradeSlot(UPGRADE_SLOT_START + 1, 152, 26);
    }

    @Override
    protected int getBatterySlotIndex() { return -1; }

    @Override
    protected int getDataSlotCount() { return 6; }

    public int getHeat() { return data.get(0); }
    public int getMaxHeat() { return data.get(1); }
    public int getProgress() { return data.get(2); }
    public int getMaxProgress() { return data.get(3); }
    public int getAirAmount() { return data.get(4); }
    public int getAirCapacity() { return data.get(5); }

    public boolean isHot() { return getHeat() >= getMaxHeat(); }

    @Override
    public void broadcastChanges() {
        super.broadcastChanges();
    }
}