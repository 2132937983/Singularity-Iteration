package com.miophas.singularity_iteration.common.menu.generator;

import com.miophas.singularity_iteration.common.blockentity.mio_icif_Thermal_Generator;
import com.miophas.singularity_iteration.common.registry.mio_icif_menus;
import com.miophas.singularity_iteration.common.menu.base.mio_icif_generator_menu;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;
import org.jetbrains.annotations.Nullable;

@SuppressWarnings("null") public class ThermalGeneratorMenu extends mio_icif_generator_menu {
    public static final int FUEL_SLOT = 0;
    public static final int BATTERY_SLOT = 1;
    public static final int SLOT_COUNT = 2;

    public ThermalGeneratorMenu(int id, Inventory playerInventory) {
        this(id, playerInventory, null, null, null);
    }

    public ThermalGeneratorMenu(int id, Inventory playerInventory,
                                 @Nullable mio_icif_Thermal_Generator blockEntity,
                                 @Nullable IItemHandler itemHandler,
                                 @Nullable ContainerData data) {
        super(mio_icif_menus.THERMAL_GENERATOR_MENU_TYPE.get(), id, SLOT_COUNT,
              playerInventory, itemHandler, data, mio_icif_Thermal_Generator.DATA_COUNT, blockEntity);
    }

    /** 0.1.11.10: 248 x 204 with the chart recorder; fuel and battery slots on the left. 0.1.11.181: 176 wide, the fuel slot
     *  in the furnace's feed door, the battery under the furnace. */
    public static final int GUI_WIDTH = 176, GUI_HEIGHT = 244;
    public static final int FUEL_X = 15, FUEL_Y = 33, BATTERY_X = 15, BATTERY_Y = 88;
    public static final int INVENTORY_X = 8, INVENTORY_Y = 162, HOTBAR_Y = 220;

    /** Synced value {@code i} (see the block entity's getContainerData). */
    public int value(int i) { return data.get(i); }

    @Override
    protected int getPlayerInventoryY() { return INVENTORY_Y; }

    @Override
    protected int getPlayerHotbarY() { return HOTBAR_Y; }

    @Override
    protected void addPlayerInventory(Inventory playerInventory, int startY, int hotbarY) {
        for (int i = 0; i < 3; ++i) for (int j = 0; j < 9; ++j)
            this.addSlot(new Slot(playerInventory, j + i * 9 + 9, INVENTORY_X + j * 18, startY + i * 18));
        for (int i = 0; i < 9; ++i) this.addSlot(new Slot(playerInventory, i, INVENTORY_X + i * 18, hotbarY));
    }

    @Override
    protected void addMachineSlots() {
        addFuelSlot(FUEL_SLOT, FUEL_X, FUEL_Y);
        addBatterySlot(BATTERY_SLOT, BATTERY_X, BATTERY_Y);
    }

    @Override
    protected int getFuelSlotIndex() { return FUEL_SLOT; }

    @Override
    protected boolean isFuel(ItemStack stack) {
        if (blockEntity instanceof mio_icif_Thermal_Generator gen) {
            return gen.getFuelBurnTime(stack) > 0;
        }
        return mio_icif_Thermal_Generator.fuelBurnTime(stack,
                com.miophas.singularity_iteration.core.runtime.energy.engine.IndependentEnergyMode.enabled()) > 0;
    }

    protected boolean isValidFuel(ItemStack stack) {
        return isFuel(stack);
    }

    @Override
    protected boolean isBattery(ItemStack stack) {
        return com.miophas.singularity_iteration.core.api.MioIcifAPI.instance().getItemAPI().isBattery(stack);   // 0.1.11.11: any chargeable item, as the block takes
    }








    public int getEnergy() { return data.get(3); }

    public int getMaxEnergy() { return data.get(4); }

    public boolean isBurning() { return data.get(0) == 1; }

    public mio_icif_Thermal_Generator getBlockEntity() {
        return blockEntity instanceof mio_icif_Thermal_Generator ? (mio_icif_Thermal_Generator) blockEntity : null;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack itemstack = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack itemstack1 = slot.getItem();
            itemstack = itemstack1.copy();
            if (index < SLOT_COUNT) {
                if (!this.moveItemStackTo(itemstack1, SLOT_COUNT, this.slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else {
                if (isBattery(itemstack1)) {
                    if (!this.moveItemStackTo(itemstack1, BATTERY_SLOT, BATTERY_SLOT + 1, false)) {
                        return ItemStack.EMPTY;
                    }
                } else if (isValidFuel(itemstack1)) {
                    if (!this.moveItemStackTo(itemstack1, FUEL_SLOT, FUEL_SLOT + 1, false)) {
                        return ItemStack.EMPTY;
                    }
                } else {
                    return ItemStack.EMPTY;
                }
            }
            if (itemstack1.isEmpty()) {
                slot.set(ItemStack.EMPTY);
            } else {
                slot.set(itemstack1);
            }
        }
        return itemstack;
    }


    @Override
    public boolean stillValid(Player player) {
        return blockEntity instanceof mio_icif_Thermal_Generator gen ? gen.stillValid(player) : true;
    }
}
