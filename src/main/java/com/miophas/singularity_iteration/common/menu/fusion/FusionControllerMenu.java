package com.miophas.singularity_iteration.common.menu.fusion;

import com.miophas.singularity_iteration.common.blockentity.fusion.FusionControllerBlockEntity;
import com.miophas.singularity_iteration.common.fusion.FusionBlanket;
import com.miophas.singularity_iteration.common.registry.mio_icif_menus;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.items.SlotItemHandler;
import org.jetbrains.annotations.Nullable;

/**
 * 0.1.11.92: the fusion reactor controller's screen: the blanket (9 x 6 component slots, column 1 next to the plasma), the fuel
 * slots (deuterium, tritium, the two empty-cell outputs), the disruption mitigation slot (0.1.11.94: coolant cells, was ice) and
 * the player's inventory.
 * {@link #VALUES} longs from {@link FusionControllerBlockEntity#screenValues()} are sent as four 16-bit data slots each.
 * Buttons: 0 feed -1, 1 feed +1, 2 feed -4, 3 feed +4.
 */
public class FusionControllerMenu extends AbstractContainerMenu {
    public static final int VALUES = 52;   // 0.1.11.136: + fuel injected and used;   // 0.1.11.135: + heat, multiplication, fissions per neutron, TBR, engineering Q;   // 0.1.11.134: + Greenwald, beta, H-mode, disruption cause, ELMs;   // 0.1.11.133: + density, confinement time, triple product, alpha, losses
    public static final int GRID_X = 8, GRID_Y = 18, FUEL_Y = 134, INV_X = 69, INV_Y = 172, HOTBAR_Y = 230;
    public static final int[] FUEL_X = {8, 26, 62, 80, 116};
    private static final int MACHINE = FusionControllerBlockEntity.SLOTS;

    private final @Nullable FusionControllerBlockEntity controller;
    private final long[] cache = new long[VALUES];
    private final ContainerData data;

    public FusionControllerMenu(int id, Inventory inventory) {
        this(id, inventory, null);
    }

    public FusionControllerMenu(int id, Inventory inventory, @Nullable FusionControllerBlockEntity controller) {
        super(mio_icif_menus.FUSION_CONTROLLER_MENU_TYPE.get(), id);
        this.controller = controller;
        IItemHandler items = controller != null ? controller.getItems() : new ItemStackHandler(MACHINE);
        this.data = new ContainerData() {
            private final int[] client = new int[VALUES * 4];
            @Override public int get(int i) {
                if (FusionControllerMenu.this.controller == null) return client[i];
                return (int) ((cache[i / 4] >>> (16 * (i % 4))) & 0xFFFF);
            }
            @Override public void set(int i, int value) { client[i] = value & 0xFFFF; }
            @Override public int getCount() { return VALUES * 4; }
        };
        addDataSlots(data);
        for (int row = 0; row < FusionBlanket.ROWS; row++)
            for (int col = 0; col < FusionBlanket.COLUMNS; col++)
                addSlot(new SlotItemHandler(items, row * FusionBlanket.COLUMNS + col, GRID_X + col * 18, GRID_Y + row * 18) {
                    @Override public int getMaxStackSize() { return 1; }
                });
        int[] slots = {FusionControllerBlockEntity.SLOT_DEUTERIUM, FusionControllerBlockEntity.SLOT_TRITIUM, FusionControllerBlockEntity.SLOT_EMPTY_CELL,
            FusionControllerBlockEntity.SLOT_EMPTY_ROD, FusionControllerBlockEntity.SLOT_MITIGATION};
        for (int i = 0; i < slots.length; i++) {
            boolean output = slots[i] == FusionControllerBlockEntity.SLOT_EMPTY_CELL || slots[i] == FusionControllerBlockEntity.SLOT_EMPTY_ROD;
            addSlot(new SlotItemHandler(items, slots[i], FUEL_X[i], FUEL_Y) {
                @Override public boolean mayPlace(ItemStack stack) { return !output && super.mayPlace(stack); }
            });
        }
        for (int row = 0; row < 3; row++)
            for (int col = 0; col < 9; col++)
                addSlot(new Slot(inventory, col + row * 9 + 9, INV_X + col * 18, INV_Y + row * 18));
        for (int col = 0; col < 9; col++) addSlot(new Slot(inventory, col, INV_X + col * 18, HOTBAR_Y));
    }

    /** A value sent by the controller (see {@link FusionControllerBlockEntity#screenValues()}). */
    public long value(int index) {
        long v = 0;
        for (int k = 0; k < 4; k++) v |= ((long) (data.get(index * 4 + k) & 0xFFFF)) << (16 * k);
        return v;
    }

    @Override
    public void broadcastChanges() {
        if (controller != null) {
            long[] now = controller.screenValues();
            System.arraycopy(now, 0, cache, 0, Math.min(now.length, VALUES));
        }
        super.broadcastChanges();
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (controller == null) return false;
        int feed = controller.getFeed();
        switch (id) {
            case 0 -> controller.setFeed(feed - 1);
            case 1 -> controller.setFeed(feed + 1);
            case 2 -> controller.setFeed(feed - 4);
            case 3 -> controller.setFeed(feed + 4);
            default -> { return false; }
        }
        return true;
    }

    @Override
    public boolean stillValid(Player player) {
        return controller == null || !controller.isRemoved() && player.distanceToSqr(controller.getBlockPos().getCenter()) <= 64;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem();
        ItemStack copy = stack.copy();
        int playerStart = FusionBlanket.SLOTS + 5;
        if (index < playerStart) {
            if (!moveItemStackTo(stack, playerStart, slots.size(), true)) return ItemStack.EMPTY;
        } else {
            // fuel and ice first, then the blanket (one component per slot)
            boolean moved = moveItemStackTo(stack, FusionBlanket.SLOTS, FusionBlanket.SLOTS + 2, false)
                || moveItemStackTo(stack, FusionBlanket.SLOTS + 4, FusionBlanket.SLOTS + 5, false);
            if (!moved) {
                for (int i = 0; i < FusionBlanket.SLOTS && !stack.isEmpty(); i++) {
                    Slot target = slots.get(i);
                    if (!target.hasItem() && target.mayPlace(stack)) {
                        target.set(stack.split(1));
                        moved = true;
                    }
                }
            }
            if (!moved) return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) slot.set(ItemStack.EMPTY);
        else slot.setChanged();
        return copy;
    }
}
