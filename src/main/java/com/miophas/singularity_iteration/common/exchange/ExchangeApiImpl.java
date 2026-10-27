// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.exchange;

import com.miophas.singularity_iteration.common.future.FutureCommodity;
import com.miophas.singularity_iteration.common.future.FutureCommodityManager;
import com.miophas.singularity_iteration.common.future.FutureMarketData;
import com.miophas.singularity_iteration.core.api.exchange.BountyInfo;
import com.miophas.singularity_iteration.core.api.exchange.BountySpec;
import com.miophas.singularity_iteration.core.api.exchange.ExchangeApi;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;

/**
 * 0.1.10.26 (X3): {@link ExchangeApi.Service} on the exchange: the same rules as the panel (quotes do not change anything;
 * deposits and withdrawals go through the ledger with their reason code). Server thread only: a call from another thread
 * throws.
 */
public final class ExchangeApiImpl implements ExchangeApi.Service {

    static void serverThread(MinecraftServer server) {
        if (!server.isSameThread()) throw new IllegalStateException("the exchange API runs on the server thread");
    }

    @Override
    public List<ExchangeApi.Commodity> commodities(MinecraftServer server) {
        serverThread(server);
        FutureMarketData fm = FutureMarketData.get(server.overworld());
        List<ExchangeApi.Commodity> out = new ArrayList<>();
        for (FutureCommodity c : FutureCommodityManager.getCommodities())
            out.add(new ExchangeApi.Commodity(c.getItemId(), c.getCategory().getId(), c.getBasePrice(), fm.getPrice(c.getItemId(), c.getBasePrice())));
        return out;
    }

    @Override
    public ExchangeApi.Quote quoteSell(MinecraftServer server, UUID player, String item, long quantity) {
        serverThread(server);
        ExchangeService.Market m = ExchangeService.market(server, item);
        if (m == null || !m.system()) return new ExchangeApi.Quote(0, 0);
        ExchangeData d = ExchangeData.get(server);
        long day = ExchangeService.today(server);
        SellDepth.Curve c = ExchangeService.curve(m);
        long before = ExchangeService.soldToday(d, player, m.id(), day);
        double pressure = ExchangeService.pressure(d, m.id());
        long take = SellDepth.maxForItemPrice(c, before, pressure, 1, quantity);
        long allowance = ExchangeService.allowanceLeft(server, d, player, day);
        if (allowance != Long.MAX_VALUE) take = SellDepth.maxForCoins(c, before, pressure, allowance, take);
        return new ExchangeApi.Quote(take, take <= 0 ? 0 : (long) Math.floor(SellDepth.payout(c, before, pressure, take)));
    }

    @Override
    public ExchangeApi.Quote quoteBuy(MinecraftServer server, UUID player, String item, long quantity) {
        serverThread(server);
        ExchangeService.Market m = ExchangeService.market(server, item);
        if (m == null || !m.system()) return new ExchangeApi.Quote(0, 0);
        ExchangeData d = ExchangeData.get(server);
        long day = ExchangeService.today(server);
        long n = Math.min(Math.min(quantity, ExchangeService.MAX_QUANTITY), Math.min(ExchangeService.buyLeft(server, player),
            ExchangeService.supply(d, m.id(), day)));
        return n <= 0 ? new ExchangeApi.Quote(0, 0) : new ExchangeApi.Quote(n, ExchangeService.systemCost(d, m, (int) n));
    }

    @Override
    public Map<String, Long> treasury(MinecraftServer server) {
        serverThread(server);
        return new TreeMap<>(ExchangeData.get(server).treasury);
    }

    @Override
    public Map<String, Long> vault(MinecraftServer server) {
        serverThread(server);
        return new TreeMap<>(ExchangeData.get(server).board.vault);
    }

    @Override
    public List<ExchangeApi.LedgerDay> ledger(MinecraftServer server, int days) {
        serverThread(server);
        List<ExchangeData.LedgerDay> all = new ArrayList<>(ExchangeData.get(server).ledger);
        List<ExchangeApi.LedgerDay> out = new ArrayList<>();
        for (int i = Math.max(0, all.size() - Math.max(0, Math.min(ExchangeService.LEDGER_DAYS, days))); i < all.size(); i++) {
            ExchangeData.LedgerDay l = all.get(i);
            Map<String, Long> in = new HashMap<>(), out2 = new HashMap<>();
            l.external.forEach((k, v) -> { if (v[0] > 0) in.put(k, v[0]); if (v[1] > 0) out2.put(k, v[1]); });
            out.add(new ExchangeApi.LedgerDay(l.day, l.minted, l.burned, l.bountyCoins, l.bountyItems, l.bountyUu, in, out2));
        }
        return out;
    }

    @Override
    public long mailboxCoins(MinecraftServer server, UUID player) {
        serverThread(server);
        ExchangeData.Mailbox box = ExchangeData.get(server).mail.get(player);
        return box == null ? 0 : box.coins;
    }

    @Override
    public long deposit(MinecraftServer server, UUID player, long coins, String reason) {
        serverThread(server);
        ExchangeData d = ExchangeData.get(server);
        ExchangeData.Mailbox box = d.mailbox(player);
        box.coins = Math.addExact(box.coins, coins);
        ExchangeData.LedgerDay l = d.ledger(ExchangeService.today(server));
        l.minted += coins;
        l.external.computeIfAbsent(reason, k -> new long[2])[0] += coins;
        d.setDirty();
        return box.coins;
    }

    @Override
    public boolean withdraw(MinecraftServer server, UUID player, long coins, String reason) {
        serverThread(server);
        ExchangeData d = ExchangeData.get(server);
        ExchangeData.Mailbox box = d.mail.get(player);
        if (box == null || box.coins < coins) return false;
        box.coins -= coins;
        ExchangeData.LedgerDay l = d.ledger(ExchangeService.today(server));
        l.burned += coins;
        l.external.computeIfAbsent(reason, k -> new long[2])[1] += coins;
        d.setDirty();
        return true;
    }

    @Override
    public List<BountyInfo> bounties(MinecraftServer server) {
        serverThread(server);
        List<BountyInfo> out = new ArrayList<>();
        for (BountyBoard.Bounty b : ExchangeData.get(server).board.bounties) out.add(Bounties.info(b));
        return out;
    }

    @Override
    public long postBounty(MinecraftServer server, ResourceLocation table) {
        serverThread(server);
        return Bounties.post(server, table.toString());
    }

    @Override
    public long postBounty(MinecraftServer server, BountySpec spec) {
        serverThread(server);
        return Bounties.post(server, spec);
    }

    @Override
    public boolean cancelBounty(MinecraftServer server, long id) {
        serverThread(server);
        return Bounties.cancel(server, id);
    }
}
