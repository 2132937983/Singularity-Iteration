package com.singularity_iteration.mio_icif.Menu.HUEntity;

import com.singularity_iteration.mio_icif.Blocks.entity.HUEntity.HuGenerator.mio_icif_heat_generator_elc;
import com.singularity_iteration.mio_icif.Menu.mio_icif_menus;
import com.singularity_iteration.mio_icif.api.MioIcifAPI;
import com.singularity_iteration.mio_icif.Items.Resource.mio_icif_resources;
import com.singularity_iteration.mio_icif.Menu.Base.mio_icif_machine_menu;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.SlotItemHandler;
import org.jetbrains.annotations.Nullable;

@SuppressWarnings("null") public class HeatGeneratorElcMenu extends mio_icif_machine_menu {
    public static final int BATTERY_SLOT = 0;
    public static final int COIL_SLOT_START = 1;
    public static final int COIL_SLOT_COUNT = 10;
    public static final int SLOT_COUNT = 11;

    public HeatGeneratorElcMenu(int containerId, Inventory playerInventory) {
        this(containerId, playerInventory, null);
    }

    public HeatGeneratorElcMenu(int containerId, Inventory playerInventory, @Nullable mio_icif_heat_generator_elc blockEntity) {
        super(mio_icif_menus.HEAT_GENERATOR_ELC_MENU_TYPE.get(), containerId, playerInventory, blockEntity, SLOT_COUNT, 9);
    }

    @Override
    protected void addMachineSlots() {
        this.addSlot(new SlotItemHandler(itemHandler, BATTERY_SLOT, 8, 62) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return MioIcifAPI.instance().getItemAPI().isBattery(stack);
            }
        });

        for (int row = 0; row < 2; row++) {
            for (int col = 0; col < 5; col++) {
                int slotIndex = COIL_SLOT_START + row * 5 + col;
                int slotX = 44 + col * 18;
                int slotY = 27 + row * 18;
                this.addSlot(new SlotItemHandler(itemHandler, slotIndex, slotX, slotY) {
                    @Override
                    public boolean mayPlace(ItemStack stack) {
                        return stack.is(mio_icif_resources.COIL.get());
                    }

                    @Override
                    public int getMaxStackSize() {
                        return 1;
                    }
                });
            }
        }
    }

    @Override
    protected int getDataSlotCount() { return 9; }

    public boolean isWorking() { return data.get(0) == 1; }
    public int getEnergy() { return data.get(1); }
    public int getMaxEnergy() { return data.get(2); }
    public int getHeat() { return data.get(3); }
    public int getMaxHeat() { return data.get(4); }
    public int getTemperature() { return data.get(5); }
    public int getCoilCount() { return data.get(6); }
    public int getHeatGeneration() { return data.get(7); }
    public int getMaxHeatGeneration() { return data.get(8); }

    @Override
    public mio_icif_heat_generator_elc getBlockEntity() {
        return this.blockEntity instanceof mio_icif_heat_generator_elc be ? be : null;
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
                if (MioIcifAPI.instance().getItemAPI().isBattery(itemStack1)) {
                    if (!this.moveItemStackTo(itemStack1, BATTERY_SLOT, BATTERY_SLOT + 1, false)) {
                        return ItemStack.EMPTY;
                    }
                } else if (itemStack1.is(mio_icif_resources.COIL.get())) {
                    // 线圈一个一个地放入空槽位
                    if (!moveCoilOneByOne(itemStack1)) {
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

    /**
     * 将线圈一个一个地放入空槽位
     * @return 是否成功移动了至少一个物品
     */
    private boolean moveCoilOneByOne(ItemStack stack) {
        if (stack.isEmpty() || !stack.is(mio_icif_resources.COIL.get())) {
            return false;
        }

        // 找到第一个空的线圈槽位
        for (int i = 0; i < COIL_SLOT_COUNT; i++) {
            int slotIndex = COIL_SLOT_START + i;
            Slot targetSlot = this.slots.get(slotIndex);
            if (targetSlot != null && !targetSlot.hasItem()) {
                // 只移动一个物品到该槽位
                ItemStack singleItem = stack.split(1);
                targetSlot.set(singleItem);
                return true;
            }
        }
        return false;
    }

    @Override
    public void broadcastChanges() {
        super.broadcastChanges();
        if (blockEntity instanceof mio_icif_heat_generator_elc gen) {
            this.setSyncData(0, gen.isWorking() ? 1 : 0);
            this.setSyncData(1, (int) gen.getEnergyStorage().getAmount());
            this.setSyncData(2, (int) gen.getEnergyStorage().getCapacity());
            this.setSyncData(3, (int) gen.getHeatStored());
            this.setSyncData(4, (int) gen.getHeatCapacity());
            this.setSyncData(5, gen.getTemperature());
            this.setSyncData(6, gen.getCoilCount());
            this.setSyncData(7, gen.getHeatGeneration());
            this.setSyncData(8, mio_icif_heat_generator_elc.getMaxHeatGeneration());
        }
    }
}