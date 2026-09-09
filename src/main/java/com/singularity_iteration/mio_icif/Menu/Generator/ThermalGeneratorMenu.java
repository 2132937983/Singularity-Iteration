package com.singularity_iteration.mio_icif.Menu.Generator;

import com.singularity_iteration.mio_icif.Blocks.entity.mio_icif_Thermal_Generator;
import com.singularity_iteration.mio_icif.Menu.mio_icif_menus;
import com.singularity_iteration.mio_icif.Menu.Base.mio_icif_generator_menu;
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
              playerInventory, itemHandler, data, 6, blockEntity);
    }

    @Override
    protected void addMachineSlots() {
        // 燃料槽在上方（火焰图标上方）
        addFuelSlot(FUEL_SLOT, 65, 17);
        // 电池槽在下方（火焰图标下方）
        addBatterySlot(BATTERY_SLOT, 65, 53);
    }

    @Override
    protected int getFuelSlotIndex() { return FUEL_SLOT; }

    @Override
    protected boolean isFuel(ItemStack stack) {
        if (blockEntity instanceof mio_icif_Thermal_Generator gen && !stack.isEmpty()) {
            return gen.getFuelBurnTime(stack) > 0;
        }
        return false;
    }

    protected boolean isValidFuel(ItemStack stack) {
        return isFuel(stack);
    }

    @Override
    protected boolean isBattery(ItemStack stack) {
        return stack.getItem() instanceof com.singularity_iteration.mio_icif.Items.Normal.mio_icif_bat;
    }

    public int getBurnProgressPixels() {
        if (data.getCount() > 1) return data.get(1);
        return 0;
    }

    public int getEnergyProgressPixels() {
        if (data.getCount() > 3) {
            int energy = data.get(2);
            int maxEnergy = data.get(3);
            if (maxEnergy > 0) return (int) ((float) energy / maxEnergy * ENERGY_BAR_WIDTH);
        }
        return 0;
    }

    public int getEnergy() {
        if (data.getCount() > 2) return data.get(2);
        return 0;
    }

    public int getMaxEnergy() {
        if (data.getCount() > 3) return data.get(3);
        return 100000;
    }

    public boolean isBurning() {
        return data.get(0) == 1;
    }

    public int getBurnTime() {
        if (data.getCount() > 4) return data.get(4);
        return 0;
    }

    public int getBurnDuration() {
        if (data.getCount() > 5) return data.get(5);
        return 0;
    }

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
    public void broadcastChanges() {
        super.broadcastChanges();
        if (blockEntity instanceof mio_icif_Thermal_Generator gen) {
            this.setSyncData(0, gen.isBurning() ? 1 : 0);
            int burnPixels = 0;
            if (gen.isBurning() && gen.burnDuration > 0) {
                burnPixels = Math.max(0, Math.min(13, (int)((float)gen.burnTime / (float)gen.burnDuration * 13)));
            }
            this.setSyncData(1, burnPixels);
            this.setSyncData(2, (int) gen.getEnergyStorage().getAmount());
            this.setSyncData(3, (int) gen.getEnergyStorage().getCapacity());
            this.setSyncData(4, gen.burnTime);
            this.setSyncData(5, gen.burnDuration);
        }
    }

    @Override
    public boolean stillValid(Player player) {
        return blockEntity instanceof mio_icif_Thermal_Generator gen ? gen.stillValid(player) : true;
    }
}