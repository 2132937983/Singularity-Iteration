package com.singularity_iteration.mio_icif.Menu.HUEntity;

import com.singularity_iteration.mio_icif.Blocks.Environment.fluid.mio_icif_fluids;
import com.singularity_iteration.mio_icif.Blocks.entity.HUEntity.HuGenerator.mio_icif_fluid_heat_generator;
import com.singularity_iteration.mio_icif.Menu.mio_icif_menus;
import com.singularity_iteration.mio_icif.Items.Cell.mio_icif_cells;
import com.singularity_iteration.mio_icif.Menu.Base.mio_icif_machine_menu;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.SlotItemHandler;
import org.jetbrains.annotations.Nullable;

@SuppressWarnings({"null"}) public class FluidHeatGeneratorMenu extends mio_icif_machine_menu {
    public static final int FUEL_BUCKET_SLOT = 0;
    public static final int EMPTY_BUCKET_SLOT = 1;
    public static final int SLOT_COUNT = 2;

    public FluidHeatGeneratorMenu(int containerId, Inventory playerInventory) {
        this(containerId, playerInventory, null, null);
    }

    public FluidHeatGeneratorMenu(int containerId, Inventory playerInventory, @Nullable IItemHandler itemHandler) {
        this(containerId, playerInventory, itemHandler, null);
    }

    public FluidHeatGeneratorMenu(int containerId, Inventory playerInventory, @Nullable IItemHandler itemHandler, @Nullable ContainerData data) {
        super(mio_icif_menus.FLUID_HEAT_GENERATOR_MENU_TYPE.get(), containerId, SLOT_COUNT, playerInventory, itemHandler, data, 4);
    }

    public FluidHeatGeneratorMenu(int containerId, Inventory playerInventory, @Nullable mio_icif_fluid_heat_generator blockEntity) {
        super(mio_icif_menus.FLUID_HEAT_GENERATOR_MENU_TYPE.get(), containerId, SLOT_COUNT, playerInventory,
              blockEntity != null ? blockEntity.getItemHandler() : null,
              blockEntity != null ? blockEntity.createContainerData() : null, 4, blockEntity);
    }

    @Override
    protected void addMachineSlots() {
        this.addSlot(new SlotItemHandler(itemHandler, FUEL_BUCKET_SLOT, 27, 21) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.is(mio_icif_fluids.BIOGAS_BUCKET.get()) || mio_icif_cells.isCellContainingFluid(stack, mio_icif_fluids.BIOGAS.get());
            }
        });
        this.addSlot(new SlotItemHandler(itemHandler, EMPTY_BUCKET_SLOT, 27, 54) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
            }
        });
    }

    public int getBurnTime() { return data.get(0); }
    public int getMaxBurnTime() { return data.get(1); }
    public int getFuelAmount() { return data.get(2); }
    public int getFuelCapacity() { return data.get(3); }

    public int getBurnProgress() {
        int maxBurnTime = getMaxBurnTime();
        if (maxBurnTime <= 0) return 0;
        return ((maxBurnTime - getBurnTime()) * 100) / maxBurnTime;
    }

    public boolean isWorking() {
        return getBurnTime() > 0;
    }
}