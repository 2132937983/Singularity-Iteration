// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.exchange;

/**
 * 0.1.10.29: the coin account of a player (the coins of the exchange mailbox, {@link ExchangeData.Mailbox#coins}). Pure
 * arithmetic, no game types (the offline harness runs it).
 *
 * <p>A payment (a purchase, the escrow of a buy order, a commission and its fee, the collateral of a contract) takes coins from
 * the account and the inventory together: in bank mode the account first, else the inventory first. Receipts go to the
 * account in bank mode, else to the inventory and what does not fit to the account (as before 0.1.10.29).
 */
public final class CoinAccount {
    private CoinAccount() {}

    /**
     * {from the account, from the inventory} for a payment of {@code amount}, or null when the account and the inventory together
     * hold less (nothing is taken then).
     */
    public static long[] split(long amount, long account, long inventory, boolean accountFirst) {
        if (amount <= 0) return new long[] {0, 0};
        long a = Math.max(0, account), i = Math.max(0, inventory);
        if (a > Long.MAX_VALUE - i || a + i >= amount) {
            long fromAccount = accountFirst ? Math.min(a, amount) : amount - Math.min(i, amount);
            return new long[] {fromAccount, amount - fromAccount};
        }
        return null;
    }

    /** Coins a deposit moves: {@code request} (0 = all) of the {@code held} coins. */
    public static long deposit(long request, long held) {
        long h = Math.max(0, held);
        return request <= 0 ? h : Math.min(request, h);
    }

    /** Coins a withdrawal asks for: {@code request} (0 = all) of the {@code balance}. */
    public static long withdraw(long request, long balance) {
        long b = Math.max(0, balance);
        return request <= 0 ? b : Math.min(request, b);
    }
}
