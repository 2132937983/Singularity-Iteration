package com.singularity_iteration.mio_icif.Menu.Generator;

import com.singularity_iteration.mio_icif.Blocks.entity.generator.mio_icif_geo_generator;
import com.singularity_iteration.mio_icif.Menu.mio_icif_menus;
import com.singularity_iteration.mio_icif.Items.Cell.mio_icif_cells;
import com.singularity_iteration.mio_icif.Menu.Base.mio_icif_generator_menu;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.SlotItemHandler;
import org.jetbrains.annotations.Nullable;

public class GeoGeneratorMenu extends mio_icif_generator_menu {
    public static final int LAVA_BUCKET_SLOT = 0;
    public static final int EMPTY_BUCKET_SLOT = 1;
    public static final int BATTERY_SLOT = 2;
    public static final int SLOT_COUNT = 3;

    public GeoGeneratorMenu(int containerId, Inventory playerInventory) {
        this(containerId, playerInventory, null, null, null);
    }

    public GeoGeneratorMenu(int containerId, Inventory playerInventory,
                             @Nullable mio_icif_geo_generator blockEntity,
                             @Nullable IItemHandler itemHandler,
                             @Nullable ContainerData data) {
        super(mio_icif_menus.GEO_GENERATOR_MENU_TYPE.get(), containerId, SLOT_COUNT,
              playerInventory, itemHandler, data, 4, blockEntity);
    }

    @Override
    protected void addMachineSlots() {
        this.addSlot(new SlotItemHandler(itemHandler, LAVA_BUCKET_SLOT, 27, 21) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                boolean isLavaContainer = stack.is(Items.LAVA_BUCKET) || mio_icif_cells.isCellContainingFluid(stack, Fluids.LAVA);
                if (!isLavaContainer) return false;
                if (blockEntity instanceof mio_icif_geo_generator gen && gen.getLavaAmount() >= gen.getLavaCapacity()) return false;
                return true;
            }
        });
        this.addSlot(new SlotItemHandler(itemHandler, EMPTY_BUCKET_SLOT, 27, 54) {
            @Override
            public boolean mayPlace(ItemStack stack) { return false; }
        });
        addBatterySlot(BATTERY_SLOT, 117, 49);
    }

    public int getLavaAmount() { return data.get(2); }
    public int getLavaCapacity() { return data.get(3); }

    protected boolean isValidFuel(ItemStack stack) {
        return stack.is(Items.LAVA_BUCKET) || mio_icif_cells.isCellContainingFluid(stack, Fluids.LAVA);
    }

    @Override
    public void broadcastChanges() {
        super.broadcastChanges();
        if (blockEntity instanceof mio_icif_geo_generator gen) {
            setSyncData(0, (int) gen.getEnergyStorage().getAmount());
            setSyncData(1, (int) gen.getEnergyStorage().getCapacity());
            setSyncData(2, gen.getLavaAmount());
            setSyncData(3, gen.getLavaCapacity());
        }
    }
}