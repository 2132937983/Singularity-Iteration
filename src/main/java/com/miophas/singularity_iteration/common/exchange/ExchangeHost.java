package com.miophas.singularity_iteration.common.exchange;

import java.util.function.Supplier;
import net.minecraft.server.level.ServerPlayer;

/**
 * 0.1.10.38: what serves a player's futures panel ({@code FutureElcMenu}): the Futures Machine or a Portable Futures Trader.
 * The exchange packets act only through the host of the player's open menu.
 */
public interface ExchangeHost {
    /** The player may act on the exchange through this host now (open menu, in reach, live host). */
    boolean serves(ServerPlayer player);

    /** The host can pay the energy fee of one action (shown on the panel). */
    boolean hasEnoughEnergy();

    /** Runs one exchange action; the energy fee is kept only when it succeeds. The result message goes to the action bar. */
    boolean exchangeAction(ServerPlayer player, Supplier<ExchangeService.Result> action);
}
