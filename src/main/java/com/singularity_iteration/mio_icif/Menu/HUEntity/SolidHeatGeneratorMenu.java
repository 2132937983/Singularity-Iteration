package com.singularity_iteration.mio_icif.Menu.HUEntity;

import com.singularity_iteration.mio_icif.Blocks.entity.HUEntity.HuGenerator.mio_icif_solid_heat_generator;
import com.singularity_iteration.mio_icif.Menu.mio_icif_menus;
import com.singularity_iteration.mio_icif.Menu.Base.mio_icif_machine_menu;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.tags.ItemTags;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.SlotItemHandler;
import org.jetbrains.annotations.Nullable;

@SuppressWarnings({"null"}) public class SolidHeatGeneratorMenu extends mio_icif_machine_menu {
    public static final int FUEL_SLOT = 0;
    public static final int ASH_SLOT = 1;
    public static final int SLOT_COUNT = 2;

    public SolidHeatGeneratorMenu(int containerId, Inventory playerInventory) {
        this(containerId, playerInventory, null, null);
    }

    public SolidHeatGeneratorMenu(int containerId, Inventory playerInventory, @Nullable IItemHandler itemHandler) {
        this(containerId, playerInventory, itemHandler, null);
    }

    public SolidHeatGeneratorMenu(int containerId, Inventory playerInventory, @Nullable IItemHandler itemHandler, @Nullable ContainerData data) {
        super(mio_icif_menus.SOLID_HEAT_GENERATOR_MENU_TYPE.get(), containerId, SLOT_COUNT, playerInventory, itemHandler, data, 3);
    }

    public SolidHeatGeneratorMenu(int containerId, Inventory playerInventory, @Nullable mio_icif_solid_heat_generator blockEntity) {
        super(mio_icif_menus.SOLID_HEAT_GENERATOR_MENU_TYPE.get(), containerId, SLOT_COUNT, playerInventory,
              blockEntity != null ? blockEntity.getItemHandler() : null,
              blockEntity != null ? blockEntity.getContainerData() : null, 3, blockEntity);
    }

    @Override
    protected void addMachineSlots() {
        this.addSlot(new SlotItemHandler(itemHandler, FUEL_SLOT, 80, 45) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return isFuel(stack);
            }
        });
        this.addSlot(new SlotItemHandler(itemHandler, ASH_SLOT, 113, 45) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
            }
        });
    }

    public boolean isWorking() { return data.get(0) == 1; }
    public int getBurnTime() { return data.get(1); }
    public int getMaxBurnTime() { return data.get(2); }

    @Override
    public mio_icif_solid_heat_generator getBlockEntity() {
        return this.blockEntity instanceof mio_icif_solid_heat_generator be ? be : null;
    }

    public int getBurnProgress() {
        int maxBurnTime = getMaxBurnTime();
        if (maxBurnTime <= 0) return 0;
        return ((maxBurnTime - getBurnTime()) * 100) / maxBurnTime;
    }

    @Override
    protected boolean isFuel(ItemStack stack) {
        if (stack.isEmpty()) return false;
        if (stack.is(Items.LAVA_BUCKET)) return false;
        if (stack.is(Items.COAL)) return true;
        if (stack.is(Items.CHARCOAL)) return true;
        if (stack.is(Items.COAL_BLOCK)) return true;
        if (stack.is(ItemTags.PLANKS)) return true;
        if (stack.is(ItemTags.LOGS)) return true;
        if (stack.is(Items.STICK)) return true;
        return false;
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
                if (isFuel(itemStack1)) {
                    if (!this.moveItemStackTo(itemStack1, FUEL_SLOT, FUEL_SLOT + 1, false)) {
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