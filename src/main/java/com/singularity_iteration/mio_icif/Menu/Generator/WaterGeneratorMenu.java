package com.singularity_iteration.mio_icif.Menu.Generator;

import com.singularity_iteration.mio_icif.Blocks.entity.generator.mio_icif_water_generator;
import com.singularity_iteration.mio_icif.Menu.mio_icif_menus;
import com.singularity_iteration.mio_icif.api.MioIcifAPI;
import com.singularity_iteration.mio_icif.Menu.Base.mio_icif_machine_menu;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.SlotItemHandler;
import org.jetbrains.annotations.Nullable;

@SuppressWarnings("null") public class WaterGeneratorMenu extends mio_icif_machine_menu {
    public static final int BUCKET_SLOT = 0;
    public static final int BATTERY_SLOT = 1;
    public static final int SLOT_COUNT = 2;

    public WaterGeneratorMenu(int containerId, Inventory playerInventory) {
        this(containerId, playerInventory, null, null);
    }

    public WaterGeneratorMenu(int containerId, Inventory playerInventory, @Nullable IItemHandler itemHandler) {
        this(containerId, playerInventory, itemHandler, null);
    }

    public WaterGeneratorMenu(int containerId, Inventory playerInventory, @Nullable IItemHandler itemHandler, @Nullable ContainerData data) {
        super(mio_icif_menus.WATER_GENERATOR_MENU_TYPE.get(), containerId, SLOT_COUNT, playerInventory, itemHandler, data, 7);
    }

    public WaterGeneratorMenu(int containerId, Inventory playerInventory, @Nullable mio_icif_water_generator blockEntity) {
        super(mio_icif_menus.WATER_GENERATOR_MENU_TYPE.get(), containerId, SLOT_COUNT, playerInventory,
              blockEntity != null ? blockEntity.getItemHandler() : null, null, 7, blockEntity);
    }

    @Override
    protected void addMachineSlots() {
        this.addSlot(new SlotItemHandler(itemHandler, BUCKET_SLOT, 80, 53) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.is(Items.WATER_BUCKET);
            }
        });
        this.addSlot(new SlotItemHandler(itemHandler, BATTERY_SLOT, 80, 17) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return MioIcifAPI.instance().getItemAPI().isBattery(stack);
            }
        });
    }

    public int getEnergy() { return data.get(0); }
    public int getMaxEnergy() { return data.get(1); }
    public boolean isGenerating() { return data.get(2) == 1; }
    public int getCurrentOutput() { return data.get(3); }
    public int getWaterAmount() { return data.get(4); }
    public int getMaxWaterAmount() { return data.get(5); }
    public boolean hasWaterInFront() { return data.get(6) == 1; }

    @Override
    public void broadcastChanges() {
        if (blockEntity instanceof mio_icif_water_generator generator) {
            setSyncData(0, (int) generator.getEnergyStorage().getAmount());
            setSyncData(1, (int) generator.getEnergyStorage().getCapacity());
            setSyncData(2, generator.isGenerating() ? 1 : 0);
            setSyncData(3, (int) generator.getCurrentEnergyOutput());
            setSyncData(4, generator.getWaterAmount());
            setSyncData(5, generator.getMaxWaterAmount());
            setSyncData(6, generator.hasWaterInFront() ? 1 : 0);
        }
        super.broadcastChanges();
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack itemstack = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack itemStack1 = slot.getItem();
            itemstack = itemStack1.copy();
            if (index < SLOT_COUNT) {
                if (!this.moveItemStackTo(itemStack1, SLOT_COUNT, this.slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else {
                if (itemStack1.is(Items.WATER_BUCKET)) {
                    if (!this.moveItemStackTo(itemStack1, BUCKET_SLOT, BUCKET_SLOT + 1, false)) {
                        if (MioIcifAPI.instance().getItemAPI().isBattery(itemStack1)) {
                            if (!this.moveItemStackTo(itemStack1, BATTERY_SLOT, BATTERY_SLOT + 1, false)) {
                                return ItemStack.EMPTY;
                            }
                        } else {
                            return ItemStack.EMPTY;
                        }
                    }
                } else if (MioIcifAPI.instance().getItemAPI().isBattery(itemStack1)) {
                    if (!this.moveItemStackTo(itemStack1, BATTERY_SLOT, BATTERY_SLOT + 1, false)) {
                        return ItemStack.EMPTY;
                    }
                } else {
                    return ItemStack.EMPTY;
                }
            }
            if (itemStack1.isEmpty()) {
                slot.set(ItemStack.EMPTY);
            } else {
                slot.set(itemStack1);
            }
        }
        return itemstack;
    }
}