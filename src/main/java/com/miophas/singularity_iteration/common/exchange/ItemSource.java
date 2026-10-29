// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.exchange;

import net.minecraft.world.item.Item;

/**
 * 0.1.10.27 (X2): where a delivery to a commission or a bounty takes its items from: the player's main inventory (plain
 * stacks, as before) or a settlement container ({@link SettlementStorage#source()}). Only plain items count: an enchanted,
 * named or damaged item is never delivered as a plain one.
 */
public interface ItemSource {
    /** Plain items of {@code item} available. */
    long count(Item item);

    /** Takes {@code amount} plain items; false (and nothing taken) when there are fewer. */
    boolean take(Item item, long amount);

    /** 0.1.10.36: plain items accepted by {@code match} (the requested item and the items of its tags). */
    long count(ItemMatch match);

    /** 0.1.10.36: takes {@code amount} plain items accepted by {@code match}, the requested item first; false (and nothing taken) when there are fewer. */
    boolean take(ItemMatch match, long amount);

    /** The planned inventory of a player (the plan is committed by the action). */
    static ItemSource of(InventoryPlan plan) {
        return new ItemSource() {
            @Override
            public long count(Item item) { return plan.count(item, true); }

            @Override
            public boolean take(Item item, long amount) { return plan.removeMain(item, amount, true); }

            @Override
            public long count(ItemMatch match) { return plan.count(match); }

            @Override
            public boolean take(ItemMatch match, long amount) { return plan.removeMain(match, amount); }
        };
    }
}
