package com.miophas.singularity_iteration.common.menu.generator;

import com.miophas.singularity_iteration.common.blockentity.generator.SemifluidFuels;
import com.miophas.singularity_iteration.common.blockentity.generator.mio_icif_Semifluid_generator;
import com.miophas.singularity_iteration.common.registry.mio_icif_menus;
import com.miophas.singularity_iteration.common.item.cell.mio_icif_cells;
import com.miophas.singularity_iteration.common.menu.base.mio_icif_generator_menu;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.SlotItemHandler;
import org.jetbrains.annotations.Nullable;

@SuppressWarnings("null") public class SemifluidGeneratorMenu extends mio_icif_generator_menu {
    public static final int FUEL_BUCKET_SLOT = 0;
    public static final int EMPTY_BUCKET_SLOT = 1;
    public static final int BATTERY_SLOT = 2;
    public static final int SLOT_COUNT = 3;
    /** Menu data values (0.1.10.19: 5, the fuel fluid id at index 4). */
    public static final int DATA_COUNT = 5;

    public SemifluidGeneratorMenu(int containerId, Inventory playerInventory) {
        this(containerId, playerInventory, null, null, null);
    }

    public SemifluidGeneratorMenu(int containerId, Inventory playerInventory,
                                   @Nullable mio_icif_Semifluid_generator blockEntity,
                                   @Nullable IItemHandler itemHandler,
                                   @Nullable ContainerData data) {
        super(mio_icif_menus.SEMIFLUID_GENERATOR_MENU_TYPE.get(), containerId, SLOT_COUNT,
              playerInventory, itemHandler, data, DATA_COUNT, blockEntity);
    }

    @Override
    protected void addMachineSlots() {
        this.addSlot(new SlotItemHandler(itemHandler, FUEL_BUCKET_SLOT, 27, 21) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                boolean isFuel = isValidFuel(stack);
                if (!isFuel) return false;
                if (!itemHandler.isItemValid(FUEL_BUCKET_SLOT, stack)) return false;
                int amount = blockEntity instanceof mio_icif_Semifluid_generator gen ? gen.getFuelAmount() : getFuelAmount();
                int capacity = blockEntity instanceof mio_icif_Semifluid_generator gen ? gen.getFuelCapacity() : getFuelCapacity();
                return capacity > 0 && amount < capacity;
            }
        });
        this.addSlot(new SlotItemHandler(itemHandler, EMPTY_BUCKET_SLOT, 27, 54) {
            @Override
            public boolean mayPlace(ItemStack stack) { return false; }
        });
        addBatterySlot(BATTERY_SLOT, 117, 49);
    }

    public int getFuelAmount() { return data.get(2); }
    public int getFuelCapacity() { return data.get(3); }
    /** 0.1.10.19: the fuel in the tank (synced fluid id at index 4); the screen drew biogas for every fuel. */
    public net.neoforged.neoforge.fluids.FluidStack getFuelFluid() {
        return com.miophas.singularity_iteration.common.menu.base.FluidIds.stackOf(data.get(4), getFuelAmount());
    }

    protected boolean isValidFuel(ItemStack stack) {
        if (stack.isEmpty()) return false;
        if (stack.getItem() instanceof net.minecraft.world.item.BucketItem bucket) {
            return SemifluidFuels.accepts(bucket.content);
        }
        var content = mio_icif_cells.getCellFluid(stack);
        return !content.isEmpty() && SemifluidFuels.accepts(content.getFluid());
    }

    @Override
    protected boolean isBattery(ItemStack stack) {
        return stack.getItem() instanceof com.miophas.singularity_iteration.common.item.normal.mio_icif_bat;
    }

    @Override
    public void broadcastChanges() {
        super.broadcastChanges();
        if (blockEntity instanceof mio_icif_Semifluid_generator gen) {
            setSyncData(0, (int) gen.getEnergyStorage().getAmount());
            setSyncData(1, (int) gen.getEnergyStorage().getCapacity());
            setSyncData(2, gen.getFuelAmount());
            setSyncData(3, gen.getFuelCapacity());
            setSyncData(4, com.miophas.singularity_iteration.common.menu.base.FluidIds.idOf(gen.getFuel()));
        }
    }
}