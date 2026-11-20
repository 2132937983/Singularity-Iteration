package com.miophas.singularity_iteration.common.menu.generator;

import com.miophas.singularity_iteration.common.blockentity.generator.mio_icif_solar_generator;
import com.miophas.singularity_iteration.common.menu.base.mio_icif_generator_menu;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * 0.1.11.14: the menu of every solar panel (each panel type keeps its own menu type, a subclass of this one). 248 x 204:
 * the charge slots in a column on the left, the pyranometer record in the window, the player inventory below.
 */
@SuppressWarnings("null")
public class SolarPanelMenu extends mio_icif_generator_menu {

    public static final int GUI_WIDTH = 248, GUI_HEIGHT = 244;
    public static final int SLOT_X = 10, SLOT_Y = 18, SLOT_STEP = 18;
    public static final int INVENTORY_X = 44, INVENTORY_Y = 162, HOTBAR_Y = 220;

    private final ContainerLevelAccess access;
    public final @Nullable mio_icif_solar_generator blockEntity;
    private final int chargeSlots;

    protected SolarPanelMenu(MenuType<?> type, int containerId, Inventory playerInventory, @Nullable mio_icif_solar_generator blockEntity, int chargeSlots) {
        super(type, containerId, chargeSlots, playerInventory,
            blockEntity != null ? blockEntity.getItemHandlerCapability(null) : null,
            blockEntity != null ? blockEntity.getContainerData() : null, mio_icif_solar_generator.DATA_COUNT, blockEntity);
        this.access = ContainerLevelAccess.create(playerInventory.player.level(),
            blockEntity != null ? blockEntity.getBlockPos() : playerInventory.player.blockPosition());
        this.blockEntity = blockEntity;
        this.chargeSlots = chargeSlots;
    }

    /** Synced value {@code i} (see the block entity's getContainerData). */
    public int value(int i) { return data.get(i); }

    public int chargeSlots() { return chargeSlots; }

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
        // called from the super constructor: the slot count is the machine slot count given there
        int count = machineSlotCountForLayout();
        for (int i = 0; i < count; i++) addBatterySlot(i, SLOT_X, SLOT_Y + i * SLOT_STEP);
    }

    /** The number of machine slots (the item handler of the menu has exactly that many). */
    private int machineSlotCountForLayout() {
        return itemHandler == null ? 1 : itemHandler.getSlots() >= 4 && isFourSlotMenu() ? 4 : 1;
    }

    /** The advanced panels have four charge slots; the others one. */
    protected boolean isFourSlotMenu() { return false; }

    @Override
    protected boolean isBattery(ItemStack stack) {
        return com.miophas.singularity_iteration.core.api.MioIcifAPI.instance().getItemAPI().isBattery(stack);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = this.slots.get(index);
        if (slot == null || !slot.hasItem()) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem(), copy = stack.copy();
        int machine = machineSlotCountForLayout();
        if (index < machine) {
            if (!this.moveItemStackTo(stack, machine, this.slots.size(), true)) return ItemStack.EMPTY;
        } else if (isBattery(stack)) {
            if (!this.moveItemStackTo(stack, 0, machine, false)) return ItemStack.EMPTY;
        } else return ItemStack.EMPTY;
        if (stack.isEmpty()) slot.set(ItemStack.EMPTY); else slot.setChanged();
        return copy;
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(this.access, player, blockEntity != null ? blockEntity.getBlockState().getBlock() : null);
    }
}
