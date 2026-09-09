package com.singularity_iteration.mio_icif.Menu.Generator;

import com.singularity_iteration.mio_icif.Blocks.Environment.fluid.mio_icif_fluids;
import com.singularity_iteration.mio_icif.Blocks.entity.generator.mio_icif_Semifluid_generator;
import com.singularity_iteration.mio_icif.Menu.mio_icif_menus;
import com.singularity_iteration.mio_icif.Items.Cell.mio_icif_cells;
import com.singularity_iteration.mio_icif.Menu.Base.mio_icif_generator_menu;
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

    public SemifluidGeneratorMenu(int containerId, Inventory playerInventory) {
        this(containerId, playerInventory, null, null, null);
    }

    public SemifluidGeneratorMenu(int containerId, Inventory playerInventory,
                                   @Nullable mio_icif_Semifluid_generator blockEntity,
                                   @Nullable IItemHandler itemHandler,
                                   @Nullable ContainerData data) {
        super(mio_icif_menus.SEMIFLUID_GENERATOR_MENU_TYPE.get(), containerId, SLOT_COUNT,
              playerInventory, itemHandler, data, 4, blockEntity);
    }

    @Override
    protected void addMachineSlots() {
        this.addSlot(new SlotItemHandler(itemHandler, FUEL_BUCKET_SLOT, 27, 21) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                boolean isFuel = stack.is(mio_icif_fluids.BIOGAS_BUCKET.get()) || mio_icif_cells.isCellContainingFluid(stack, mio_icif_fluids.BIOGAS.get());
                if (!isFuel) return false;
                if (blockEntity instanceof mio_icif_Semifluid_generator gen && gen.getFuelAmount() >= gen.getFuelCapacity()) return false;
                return true;
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

    protected boolean isValidFuel(ItemStack stack) {
        return stack.is(mio_icif_fluids.BIOGAS_BUCKET.get()) || mio_icif_cells.isCellContainingFluid(stack, mio_icif_fluids.BIOGAS.get());
    }

    @Override
    protected boolean isBattery(ItemStack stack) {
        return stack.getItem() instanceof com.singularity_iteration.mio_icif.Items.Normal.mio_icif_bat;
    }

    @Override
    public void broadcastChanges() {
        super.broadcastChanges();
        if (blockEntity instanceof mio_icif_Semifluid_generator gen) {
            setSyncData(0, (int) gen.getEnergyStorage().getAmount());
            setSyncData(1, (int) gen.getEnergyStorage().getCapacity());
            setSyncData(2, gen.getFuelAmount());
            setSyncData(3, gen.getFuelCapacity());
        }
    }
}