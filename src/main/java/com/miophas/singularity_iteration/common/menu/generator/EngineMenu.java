package com.miophas.singularity_iteration.common.menu.generator;

import com.miophas.singularity_iteration.common.blockentity.generator.SemifluidFuels;
import com.miophas.singularity_iteration.common.energy.EngineGovernor;
import com.miophas.singularity_iteration.common.menu.base.mio_icif_generator_menu;
import com.miophas.singularity_iteration.core.prefab.blockentity.AbstractGeneratorBlockEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.SlotItemHandler;
import org.jetbrains.annotations.Nullable;

/**
 * 0.1.11.15: the menu of the engines (semifluid, advanced semifluid, diesel generators), 248 x 204: fuel container in,
 * empty container out and battery on the left, the engine room monitor in the window, the player inventory below.
 */
@SuppressWarnings("null")
public class EngineMenu extends mio_icif_generator_menu {

    public static final int GUI_WIDTH = 248, GUI_HEIGHT = 204;
    public static final int SLOT_X = 10, FUEL_Y = 22, EMPTY_Y = 48, BATTERY_Y = 82;
    public static final int INVENTORY_X = 44, INVENTORY_Y = 122, HOTBAR_Y = 180;
    public static final int FUEL_SLOT = 0, EMPTY_SLOT = 1, BATTERY_SLOT = 2, SLOT_COUNT = 3;

    private final ContainerLevelAccess access;
    public final @Nullable AbstractGeneratorBlockEntity blockEntity;

    protected EngineMenu(MenuType<?> type, int containerId, Inventory playerInventory, @Nullable AbstractGeneratorBlockEntity blockEntity,
                         @Nullable ContainerData data) {
        super(type, containerId, SLOT_COUNT, playerInventory, blockEntity != null ? blockEntity.getItemHandlerCapability(null) : null,
            data, EngineGovernor.DATA_COUNT, blockEntity);
        this.access = ContainerLevelAccess.create(playerInventory.player.level(),
            blockEntity != null ? blockEntity.getBlockPos() : playerInventory.player.blockPosition());
        this.blockEntity = blockEntity;
    }

    /** Synced value {@code i} (see EngineGovernor#data). */
    public int value(int i) { return data.get(i); }

    /** The fuel in the tank (synced id and amount). */
    public net.neoforged.neoforge.fluids.FluidStack getFuelFluid() {
        int id = data.get(11), amount = data.get(9);
        if (id < 0 || amount <= 0) return net.neoforged.neoforge.fluids.FluidStack.EMPTY;
        return new net.neoforged.neoforge.fluids.FluidStack(net.minecraft.core.registries.BuiltInRegistries.FLUID.byId(id), amount);
    }

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
        this.addSlot(new SlotItemHandler(itemHandler, FUEL_SLOT, SLOT_X, FUEL_Y) {
            @Override public boolean mayPlace(ItemStack stack) { return isValidFuel(stack) && itemHandler.isItemValid(FUEL_SLOT, stack); }
        });
        this.addSlot(new SlotItemHandler(itemHandler, EMPTY_SLOT, SLOT_X, EMPTY_Y) {
            @Override public boolean mayPlace(ItemStack stack) { return false; }
        });
        addBatterySlot(BATTERY_SLOT, SLOT_X, BATTERY_Y);
    }

    /** A container of a fuel this engine burns (by default any semifluid fuel). */
    protected boolean isValidFuel(ItemStack stack) {
        if (stack.isEmpty()) return false;
        if (stack.getItem() instanceof net.minecraft.world.item.BucketItem bucket && SemifluidFuels.accepts(bucket.content)) return true;
        var cell = com.miophas.singularity_iteration.common.item.cell.mio_icif_cells.getCellFluid(stack);
        return !cell.isEmpty() && SemifluidFuels.accepts(cell.getFluid()) || holdsFluid(stack, SemifluidFuels::accepts);
    }

    @Override
    protected boolean isBattery(ItemStack stack) {
        return com.miophas.singularity_iteration.core.api.MioIcifAPI.instance().getItemAPI().isBattery(stack);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = this.slots.get(index);
        if (slot == null || !slot.hasItem()) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem(), copy = stack.copy();
        if (index < SLOT_COUNT) {
            if (!this.moveItemStackTo(stack, SLOT_COUNT, this.slots.size(), true)) return ItemStack.EMPTY;
        } else if (isValidFuel(stack)) {
            if (!this.moveItemStackTo(stack, FUEL_SLOT, FUEL_SLOT + 1, false)) return ItemStack.EMPTY;
        } else if (isBattery(stack)) {
            if (!this.moveItemStackTo(stack, BATTERY_SLOT, BATTERY_SLOT + 1, false)) return ItemStack.EMPTY;
        } else return ItemStack.EMPTY;
        if (stack.isEmpty()) slot.set(ItemStack.EMPTY); else slot.setChanged();
        return copy;
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(this.access, player, blockEntity != null ? blockEntity.getBlockState().getBlock() : null);
    }
}
