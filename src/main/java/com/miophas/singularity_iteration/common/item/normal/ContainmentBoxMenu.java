// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.item.normal;

import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;

/**
 * Server menu of the {@link ContainmentBoxItem} (0.1.10.5). It has the slot layout of a vanilla
 * two-row chest ({@link MenuType#GENERIC_9x2}), so the client uses the vanilla chest screen.
 *
 * <p>The contents live in the box item: every change is written to its {@code minecraft:container}
 * component at once. The box itself cannot be moved while it is open (no click on its slot, no
 * number-key swap, no off-hand swap), and the menu closes when the box leaves the hand. A box or
 * a shulker box cannot go into a box.
 */
public class ContainmentBoxMenu extends AbstractContainerMenu {
    public static final int ROWS = 2, SLOTS = ROWS * 9;

    private final ItemStack box;
    private final InteractionHand hand;
    private final int lockedHotbar;
    private final Container contents;

    public ContainmentBoxMenu(int id, Inventory inventory, ItemStack box, InteractionHand hand) {
        super(MenuType.GENERIC_9x2, id);
        this.box = box;
        this.hand = hand;
        this.lockedHotbar = hand == InteractionHand.MAIN_HAND ? inventory.selected : -1;
        this.contents = new Container(box);
        // the order and the positions of a vanilla ChestMenu with two rows
        for (int row = 0; row < ROWS; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new BoxSlot(contents, col + row * 9, 8 + col * 18, 18 + row * 18));
            }
        }
        int shift = (ROWS - 4) * 18;
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(inventory, col + row * 9 + 9, 8 + col * 18, 103 + row * 18 + shift));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(inventory, col, 8 + col * 18, 161 + shift));
        }
    }

    /** True for an item that may go into a box (not a box, not a shulker box). */
    public static boolean allowed(ItemStack stack) {
        return !(stack.getItem() instanceof ContainmentBoxItem) && stack.getItem().canFitInsideContainerItems();
    }

    @Override
    public boolean stillValid(Player player) {
        return player.isAlive() && player.getItemInHand(hand) == box && !box.isEmpty();
    }

    @Override
    public void clicked(int slotId, int button, ClickType type, Player player) {
        if (lockedHotbar >= 0 && slotId == SLOTS + 27 + lockedHotbar) return;    // the open box
        if (type == ClickType.SWAP && (button == lockedHotbar || hand == InteractionHand.OFF_HAND && button == Inventory.SLOT_OFFHAND)) return;
        super.clicked(slotId, button, type, player);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack moved = ItemStack.EMPTY;
        Slot slot = slots.get(index);
        if (slot.hasItem()) {
            ItemStack stack = slot.getItem();
            moved = stack.copy();
            if (index < SLOTS) {
                if (!moveItemStackTo(stack, SLOTS, slots.size(), true)) return ItemStack.EMPTY;
            } else if (!allowed(stack) || !moveItemStackTo(stack, 0, SLOTS, false)) {
                return ItemStack.EMPTY;
            }
            if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
            else slot.setChanged();
        }
        return moved;
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        contents.save();
    }

    /** The box contents; writes the component of the box on every change. */
    private static final class Container extends SimpleContainer {
        private final ItemStack box;
        private boolean loading;

        Container(ItemStack box) {
            super(SLOTS);
            this.box = box;
            loading = true;
            NonNullList<ItemStack> items = NonNullList.withSize(SLOTS, ItemStack.EMPTY);
            box.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY).copyInto(items);
            for (int i = 0; i < SLOTS; i++) setItem(i, items.get(i));
            loading = false;
        }

        @Override
        public void setChanged() {
            super.setChanged();
            if (!loading) save();
        }

        void save() {
            if (box.isEmpty()) return;
            box.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(getItems()));
        }

        @Override
        public boolean canPlaceItem(int slot, ItemStack stack) {
            return allowed(stack);
        }
    }

    private static final class BoxSlot extends Slot {
        BoxSlot(net.minecraft.world.Container container, int index, int x, int y) {
            super(container, index, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return allowed(stack);
        }
    }
}
