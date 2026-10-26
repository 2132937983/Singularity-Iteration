// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.exchange;

import com.miophas.singularity_iteration.common.block.environment.fluid.mio_icif_fluids;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidUtil;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

/**
 * 0.1.10.21 (futures M3): UU-matter between fluid containers of a player and the exchange account (mB). Any
 * container with a fluid handler works (buckets, cells, universal cells). A deposit empties the containers and
 * gives the empty containers back; a withdrawal needs empty containers (the account never makes a container).
 */
final class UuAccount {
    private UuAccount() {}

    private static Fluid uu() { return mio_icif_fluids.UUMATTER.get(); }

    /** Empties the UU-matter of the main inventory into the account; returns the mB moved. */
    static long deposit(ServerPlayer player) {
        var items = player.getInventory().items;
        long total = 0;
        List<ItemStack> empties = new ArrayList<>();
        for (int slot = 0; slot < items.size(); slot++) {
            ItemStack stack = items.get(slot);
            if (stack.isEmpty() || !FluidUtil.getFluidContained(stack).map(f -> f.getFluid().isSame(uu())).orElse(false)) continue;
            int used = 0;
            for (int k = 0; k < stack.getCount(); k++) {
                var handler = FluidUtil.getFluidHandler(stack.copyWithCount(1));
                if (handler.isEmpty()) break;
                FluidStack drained = handler.get().drain(new FluidStack(uu(), Integer.MAX_VALUE), IFluidHandler.FluidAction.EXECUTE);
                if (drained.isEmpty()) break;
                total += drained.getAmount();
                empties.add(handler.get().getContainer());
                used++;
            }
            if (used > 0) stack.shrink(used);
        }
        give(player, empties);
        return total;
    }

    /** Fills empty containers of the main inventory from {@code balance} mB; returns the mB moved. */
    static long withdraw(ServerPlayer player, long balance) {
        var items = player.getInventory().items;
        long left = balance;
        List<ItemStack> filled = new ArrayList<>();
        for (int slot = 0; slot < items.size() && left > 0; slot++) {
            ItemStack stack = items.get(slot);
            // only empty containers and containers of UU-matter (not tanks of tools or armour with another fluid)
            if (stack.isEmpty() || !FluidUtil.getFluidContained(stack).map(f -> f.getFluid().isSame(uu())).orElse(true)) continue;
            int used = 0;
            for (int k = 0; k < stack.getCount() && left > 0; k++) {
                var handler = FluidUtil.getFluidHandler(stack.copyWithCount(1));
                if (handler.isEmpty()) break;
                int amount = handler.get().fill(new FluidStack(uu(), (int) Math.min(Integer.MAX_VALUE, left)), IFluidHandler.FluidAction.EXECUTE);
                if (amount <= 0) break;
                left -= amount;
                filled.add(handler.get().getContainer());
                used++;
            }
            if (used > 0) stack.shrink(used);
        }
        give(player, filled);
        return balance - left;
    }

    private static void give(ServerPlayer player, List<ItemStack> stacks) {
        for (ItemStack s : stacks) {
            if (s.isEmpty()) continue;
            if (!player.getInventory().add(s)) player.drop(s, false);
        }
        player.getInventory().setChanged();
    }
}
