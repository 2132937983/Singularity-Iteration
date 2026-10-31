package com.miophas.singularity_iteration.common.exchange;

import com.miophas.singularity_iteration.common.Singularity_Iteration_Config;
import net.minecraft.server.level.ServerPlayer;

/**
 * 0.1.10.38: the data plan of the Portable Futures Trader. Opening the trader (or acting with it) after the paid period took
 * {@code portableTraderPlanCoins} Industrial Coins (default 200) from the coin account and the inventory, like any payment, and
 * pays the next {@code portableTraderPlanDays} market days (default 30, a "month"). The coins go to the treasury reserve. The
 * plan belongs to the player, not to the item; 0 coins = free.
 */
public final class DataPlan {
    private DataPlan() {}

    public static long price() { return Singularity_Iteration_Config.PORTABLE_TRADER_PLAN_COINS.get(); }

    public static int days() { return Singularity_Iteration_Config.PORTABLE_TRADER_PLAN_DAYS.get(); }

    /** First market day the plan no longer covers (Long.MIN_VALUE: never paid). */
    public static long paidUntil(ServerPlayer player) {
        return ExchangeService.data(player.server).dataPlanUntil.getOrDefault(player.getUUID(), Long.MIN_VALUE);
    }

    /** The plan covers today, after paying the next period if it had run out. False when the coins are missing. */
    public static boolean ensure(ServerPlayer player) {
        long price = price();
        if (price <= 0) return true;
        ExchangeData d = ExchangeService.data(player.server);
        long today = ExchangeService.today(player.server);
        if (today < paidUntil(player)) return true;
        InventoryPlan plan = new InventoryPlan(player.getInventory());
        if (!ExchangeService.takeCoins(plan, d, player.getUUID(), price)) return false;
        plan.commit();
        d.dataPlanUntil.put(player.getUUID(), today + days());
        ExchangeService.toReserve(player.server, d, today, price);
        d.setDirty();
        return true;
    }
}
