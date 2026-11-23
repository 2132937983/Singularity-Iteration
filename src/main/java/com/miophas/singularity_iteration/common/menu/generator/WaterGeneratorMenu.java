package com.miophas.singularity_iteration.common.menu.generator;

import com.miophas.singularity_iteration.common.blockentity.generator.mio_icif_water_generator;
import com.miophas.singularity_iteration.common.registry.mio_icif_menus;
import com.miophas.singularity_iteration.core.api.MioIcifAPI;
import com.miophas.singularity_iteration.common.menu.base.mio_icif_machine_menu;
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
    /** 0.1.11.181: the bucket over the tank's filler, the battery at the end of the output cable. */
    public static final int BUCKET_X = 10, BUCKET_Y = 17, BATTERY_X = 146, BATTERY_Y = 58;
    public static final int SLOT_COUNT = 2;
    /** 0.1.11.18: 0 energy, 1 capacity, 2 generating, 3 EU/t, 4 water mB, 5 tank, 6 water in front, 7 EU/t from the water in front, 8 its speed x1000, 9 source. */
    public static final int DATA_COUNT = 10;

    public WaterGeneratorMenu(int containerId, Inventory playerInventory) {
        this(containerId, playerInventory, null, null);
    }

    public WaterGeneratorMenu(int containerId, Inventory playerInventory, @Nullable IItemHandler itemHandler) {
        this(containerId, playerInventory, itemHandler, null);
    }

    public WaterGeneratorMenu(int containerId, Inventory playerInventory, @Nullable IItemHandler itemHandler, @Nullable ContainerData data) {
        super(mio_icif_menus.WATER_GENERATOR_MENU_TYPE.get(), containerId, SLOT_COUNT, playerInventory, itemHandler, data, DATA_COUNT);
    }

    public WaterGeneratorMenu(int containerId, Inventory playerInventory, @Nullable mio_icif_water_generator blockEntity) {
        super(mio_icif_menus.WATER_GENERATOR_MENU_TYPE.get(), containerId, SLOT_COUNT, playerInventory,
              blockEntity != null ? blockEntity.getItemHandler() : null, null, DATA_COUNT, blockEntity);
    }

    @Override
    protected void addMachineSlots() {
        this.addSlot(new SlotItemHandler(itemHandler, BUCKET_SLOT, BUCKET_X, BUCKET_Y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.is(Items.WATER_BUCKET);
            }
        });
        this.addSlot(new SlotItemHandler(itemHandler, BATTERY_SLOT, BATTERY_X, BATTERY_Y) {
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
            setSyncData(7, generator.getFaceOutput());
            setSyncData(8, (int) Math.round(generator.getFaceSpeed() * 1000));
            setSyncData(9, generator.getLevel() == null ? 0 : (generator.getFaceSpeed() > 0 && com.miophas.singularity_iteration.common.blockentity.kuentity.kugenerator.WaterPhysics.biomeSource(
                generator.getLevel(), generator.getBlockPos()) == com.miophas.singularity_iteration.common.blockentity.kuentity.kugenerator.WaterPhysics.Source.NONE ? 3
                : com.miophas.singularity_iteration.common.blockentity.kuentity.kugenerator.WaterPhysics.biomeSource(generator.getLevel(), generator.getBlockPos()).ordinal()));
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

    /** 0.1.10.6: index 0 of the synced data is not a progress. */
    @Override protected int progressDataIndex() { return -1; }

    // 0.1.11.18: the 248 x 204 rotor cross-section screen
    public static final int INVENTORY_X = 8, INVENTORY_Y = 162, HOTBAR_Y = 220;

    @Override
    protected int getPlayerInventoryY() { return INVENTORY_Y; }

    @Override
    protected int getPlayerHotbarY() { return HOTBAR_Y; }

    @Override
    protected void addPlayerInventory(Inventory playerInventory, int startY, int hotbarY) {
        for (int i = 0; i < 3; ++i) for (int j = 0; j < 9; ++j)
            this.addSlot(new net.minecraft.world.inventory.Slot(playerInventory, j + i * 9 + 9, INVENTORY_X + j * 18, startY + i * 18));
        for (int i = 0; i < 9; ++i) this.addSlot(new net.minecraft.world.inventory.Slot(playerInventory, i, INVENTORY_X + i * 18, hotbarY));
    }

    public int value(int i) { return data.get(i); }
}
