package com.singularity_iteration.mio_icif.Menu.Slot;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

@SuppressWarnings("null")
public class PhantomSlot extends Slot {

    private ItemStack phantomStack = ItemStack.EMPTY;
    private final int stackSizeLimit;

    public PhantomSlot(int index, int x, int y, int stackSizeLimit) {
        super(new net.minecraft.world.SimpleContainer(1), index, x, y);
        this.stackSizeLimit = stackSizeLimit;
    }

    @Override
    public boolean mayPickup(Player player) {
        return false;
    }

    @Override
    public int getMaxStackSize() {
        return stackSizeLimit;
    }

    @Override
    public int getMaxStackSize(ItemStack stack) {
        return stackSizeLimit;
    }

    @Override
    public boolean mayPlace(ItemStack stack) {
        return false;
    }

    @Override
    public ItemStack getItem() {
        return phantomStack;
    }

    @Override
    public void set(ItemStack stack) {
        this.phantomStack = stack.copy();
        setChanged();
    }

    @Override
    public void setChanged() {
    }

    @Override
    public ItemStack remove(int amount) {
        return ItemStack.EMPTY;
    }

    public void setPhantomItem(ItemStack stack) {
        this.phantomStack = stack.copy();
    }

    public void onSlotClick(int dragType, Player player) {
        ItemStack playerStack = player.containerMenu.getCarried();
        ItemStack slotStack = getItem();

        if (!playerStack.isEmpty()) {
            int curSize = slotStack.getCount();
            int extraSize = (dragType == 0) ? playerStack.getCount() : 1;
            int limit = Math.min(playerStack.getMaxStackSize(), this.stackSizeLimit);

            if (curSize + extraSize > limit) extraSize = Math.max(0, limit - curSize);

            if (curSize == 0) {
                set(playerStack.copyWithCount(extraSize));
            } else if (ItemStack.isSameItemSameComponents(playerStack, slotStack)) {
                set(slotStack.copyWithCount(Math.min(curSize + extraSize, limit)));
            } else {
                set(playerStack.copyWithCount(Math.min(playerStack.getCount(), limit)));
            }
        } else if (!slotStack.isEmpty()) {
            if (dragType == 0) {
                set(ItemStack.EMPTY);
            } else {
                int newSize = slotStack.getCount() / 2;
                if (newSize <= 0) {
                    set(ItemStack.EMPTY);
                } else {
                    set(slotStack.copyWithCount(newSize));
                }
            }
        }
    }
}

