package com.miophas.singularity_iteration.common.menu.generator;

import com.miophas.singularity_iteration.common.blockentity.generator.mio_icif_geo_generator;
import com.miophas.singularity_iteration.common.item.cell.mio_icif_cells;
import com.miophas.singularity_iteration.common.registry.mio_icif_menus;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.items.SlotItemHandler;
import org.jetbrains.annotations.Nullable;

/**
 * Geothermal generator menu. 0.1.11.19 (the recorder screen): the lava slots and the battery on the left. Values: 0 energy,
 * 1 capacity, 2 lava mB, 3 tank, 4 EU/t from the lava around x100, 5 lava sources, 6 flowing lava, 7 depth factor x1000,
 * 8 weighted lava x100, 9 Y, 10 in the Nether, 11 the tank burning.
 */
public class GeoGeneratorMenu extends MimicGeneratorMenu {
    public static final int LAVA_BUCKET_SLOT = 0;
    public static final int EMPTY_BUCKET_SLOT = 1;
    public static final int BATTERY_SLOT = 2;
    public static final int SLOT_COUNT = 3;
    public static final int DATA_COUNT = 12;

    public GeoGeneratorMenu(int containerId, Inventory playerInventory) { this(containerId, playerInventory, null); }

    public GeoGeneratorMenu(int containerId, Inventory playerInventory, @Nullable mio_icif_geo_generator be) {
        super(mio_icif_menus.GEO_GENERATOR_MENU_TYPE.get(), containerId, SLOT_COUNT, playerInventory, be != null ? be.getItemHandler() : null, DATA_COUNT, be);
    }

    @Override
    protected void addMachineSlots() {
        this.addSlot(new SlotItemHandler(itemHandler, LAVA_BUCKET_SLOT, 10, 22) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                if (!isValidFuel(stack) || !itemHandler.isItemValid(LAVA_BUCKET_SLOT, stack)) return false;
                int amount = blockEntity instanceof mio_icif_geo_generator gen ? gen.getLavaAmount() : value(2);
                int capacity = blockEntity instanceof mio_icif_geo_generator gen ? gen.getLavaCapacity() : value(3);
                return capacity > 0 && amount < capacity;
            }
        });
        this.addSlot(new SlotItemHandler(itemHandler, EMPTY_BUCKET_SLOT, 10, 50) {
            @Override
            public boolean mayPlace(ItemStack stack) { return false; }
        });
        addBatterySlot(BATTERY_SLOT, 10, 82);
    }

    public int getLavaAmount() { return value(2); }
    public int getLavaCapacity() { return value(3); }

    protected boolean isValidFuel(ItemStack stack) {
        return stack.is(Items.LAVA_BUCKET) || mio_icif_cells.isCellContainingFluid(stack, Fluids.LAVA) || holdsFluid(stack, f -> f.isSame(Fluids.LAVA));
    }

    @Override
    protected int[] values() {
        var gen = (mio_icif_geo_generator) blockEntity;
        Level level = gen.getLevel();
        boolean nether = level != null && level.dimension() == Level.NETHER;
        boolean tank = gen.getLavaAmount() > 0 && gen.getEnergyStorage().getAmount() < gen.getEnergyStorage().getCapacity();
        return new int[]{(int) gen.getEnergyStorage().getAmount(), (int) gen.getEnergyStorage().getCapacity(), gen.getLavaAmount(), gen.getLavaCapacity(),
            (int) Math.round(gen.getWorldHeat() * 100), gen.getLavaSources(), gen.getLavaFlowing(), (int) Math.round(gen.getDepthFactor() * 1000),
            (int) Math.round(gen.getScanRaw() * 100), gen.getBlockPos().getY(), nether ? 1 : 0, tank ? 1 : 0};
    }
}
