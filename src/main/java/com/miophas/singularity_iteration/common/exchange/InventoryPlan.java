// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.exchange;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * 0.1.10.21: a staged change of a player inventory for one exchange action: every removal and insertion is
 * planned on copies, checked against the live inventory, and written in one step (the method of the futures trade
 * since 0.1.10.15, here shared by all exchange actions). Commodity items must be plain (no custom components):
 * an enchanted or damaged item is not sold or delivered as a plain one. Coins match by item, as before.
 */
public final class InventoryPlan {
    private final Inventory inventory;
    private final ItemStack[] references = new ItemStack[41];
    private final ItemStack[] before = new ItemStack[41];
    private final ItemStack[] after = new ItemStack[41];
    private final boolean[] changed = new boolean[41];
    private final int selected;
    private final int revision;

    public InventoryPlan(Inventory inventory) {
        if (inventory.items.size() != 36 || inventory.armor.size() != 4 || inventory.offhand.size() != 1)
            throw new IllegalStateException("Unsupported player inventory layout");
        this.inventory = inventory;
        this.selected = inventory.selected;
        this.revision = inventory.getTimesChanged();
        for (int i = 0; i < 41; i++) {
            references[i] = actual(i);
            before[i] = references[i].copy();
            after[i] = before[i].copy();
        }
    }

    private ItemStack actual(int slot) {
        return slot < 36 ? inventory.items.get(slot) : slot < 40 ? inventory.armor.get(slot - 36) : inventory.offhand.get(0);
    }

    /** A plain stack of {@code item}: the item with its default components only. */
    public static boolean plain(ItemStack stack, Item item) {
        return !stack.isEmpty() && ItemStack.isSameItemSameComponents(stack, new ItemStack(item));
    }

    /** Removes {@code amount} of {@code item} from the main inventory; {@code plainOnly} for commodity items. */
    public boolean removeMain(Item item, long amount, boolean plainOnly) {
        if (amount <= 0) return amount == 0;
        if (count(item, plainOnly) < amount) return false;
        long remaining = amount;
        for (int i = 0; i < 36 && remaining > 0; i++) {
            ItemStack stack = after[i];
            if (stack.isEmpty() || !(plainOnly ? plain(stack, item) : stack.is(item))) continue;
            int take = (int) Math.min(remaining, stack.getCount());
            stack.shrink(take); changed[i] = true; remaining -= take;
            if (stack.isEmpty()) after[i] = ItemStack.EMPTY;
        }
        return remaining == 0;
    }

    /** 0.1.10.36: plain items accepted by {@code match} in the planned main inventory. */
    public long count(ItemMatch match) {
        long result = 0;
        for (int i = 0; i < 36; i++) if (match.test(after[i])) result += after[i].getCount();
        return result;
    }

    /** 0.1.10.36: removes {@code amount} plain items accepted by {@code match}, the requested item first. */
    public boolean removeMain(ItemMatch match, long amount) {
        if (amount <= 0) return amount == 0;
        if (count(match) < amount) return false;
        long remaining = amount;
        for (int pass = 0; pass < 2 && remaining > 0; pass++) {
            for (int i = 0; i < 36 && remaining > 0; i++) {
                ItemStack stack = after[i];
                if (!match.test(stack) || match.exact(stack) != (pass == 0)) continue;
                int take = (int) Math.min(remaining, stack.getCount());
                stack.shrink(take); changed[i] = true; remaining -= take;
                if (stack.isEmpty()) after[i] = ItemStack.EMPTY;
            }
        }
        return remaining == 0;
    }

    /** Inserts {@code amount} plain items; returns the amount that did not fit (nothing is dropped). */
    public long insert(Item item, long amount) {
        ItemStack template = new ItemStack(item);
        long remaining = amount;
        if (selected >= 0 && selected < 9) remaining = merge(selected, template, remaining);
        remaining = merge(40, template, remaining);
        for (int i = 0; i < 36 && remaining > 0; i++) remaining = merge(i, template, remaining);
        for (int i = 0; i < 36 && remaining > 0; i++) {
            if (!after[i].isEmpty()) continue;
            int maximum = inventory.getMaxStackSize(template);
            if (maximum <= 0) break;
            int add = (int) Math.min(remaining, maximum);
            after[i] = template.copyWithCount(add);
            after[i].setPopTime(5); changed[i] = true; remaining -= add;
        }
        return remaining;
    }

    private long merge(int slot, ItemStack template, long remaining) {
        if (remaining <= 0) return 0;
        ItemStack target = after[slot];
        if (target.isEmpty() || !ItemStack.isSameItemSameComponents(target, template) || !target.isStackable()) return remaining;
        int maximum = inventory.getMaxStackSize(target);
        long room = (long) maximum - target.getCount();
        if (room <= 0) return remaining;
        int add = (int) Math.min(remaining, room);
        target.grow(add); target.setPopTime(5); changed[slot] = true;
        return remaining - add;
    }

    /** Items of {@code item} in the planned main inventory. */
    public long count(Item item, boolean plainOnly) {
        long result = 0;
        for (int i = 0; i < 36; i++) if (plainOnly ? plain(after[i], item) : after[i].is(item)) result += after[i].getCount();
        return result;
    }

    /** True when the live inventory still equals the state the plan started from. */
    public boolean unchanged() {
        if (inventory.items.size() != 36 || inventory.armor.size() != 4 || inventory.offhand.size() != 1
                || inventory.selected != selected || inventory.getTimesChanged() != revision) return false;
        for (int i = 0; i < 41; i++) if (actual(i) != references[i] || !ItemStack.matches(before[i], actual(i))) return false;
        return true;
    }

    /** Writes the plan. Call only after {@link #unchanged()}. */
    public void commit() {
        for (int i = 0; i < 36; i++) if (changed[i]) inventory.items.set(i, after[i]);
        if (changed[40]) inventory.offhand.set(0, after[40]);
        inventory.setChanged();
    }
}
