// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.command;

import com.miophas.singularity_iteration.common.exchange.ArbitrageAudit;
import com.miophas.singularity_iteration.common.exchange.ExchangeData;
import com.miophas.singularity_iteration.common.exchange.ExchangeService;
import com.miophas.singularity_iteration.common.exchange.MarketDayRepair;
import com.miophas.singularity_iteration.common.exchange.SellDepth;
import com.miophas.singularity_iteration.common.future.FutureCommodity;
import com.miophas.singularity_iteration.common.future.FutureCommodityManager;
import com.miophas.singularity_iteration.common.future.FutureMarketData;
import com.miophas.singularity_iteration.common.future.MarketDay;
import com.miophas.singularity_iteration.common.future.MarketDayDriver;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.AbstractCookingRecipe;
import net.minecraft.world.item.crafting.Ingredient;

/**
 * 0.1.10.25 (X1): tools for operators and pack makers (permission level 2), next to {@code /mio_icif market audit}.
 * <ul>
 *   <li>{@code simulate <item> <quantity> [player]}: what an instant sale pays now (total, average, the bid of the last
 *       item), what the same sale would get alone at the daily auction, the sell pressure and the player's allowance.</li>
 *   <li>{@code arbitrage [count]}: ways to earn coins by buying from the market, processing with the server's recipes and
 *       selling back ({@link ArbitrageAudit}; energy is not priced, the EU per coin of profit is shown), and recipe cycles
 *       that multiply an item.</li>
 *   <li>{@code calibrate}: the base price of each commodity against its UU-matter value (the median ratio sets the coins
 *       per bucket used by player listings).</li>
 *   <li>{@code ledger [days]}: coins created and destroyed by the market per day.</li>
 *   <li>{@code day [repair]} (0.1.10.33): the market day against the world's day; {@code repair} moves a market day that
 *       went back under an older version up to the latest day stamp of the exchange (it also runs at every load).</li>
 * </ul>
 */
public final class MarketToolsCommand {
    private MarketToolsCommand() {}

    private static final String K = "command.mio_icif.market_tools.";

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("mio_icif").then(Commands.literal("market")
            .then(Commands.literal("simulate").requires(s -> s.hasPermission(2))
                .then(Commands.argument("item", ResourceLocationArgument.id())
                    .suggests((c, b) -> SharedSuggestionProvider.suggestResource(FutureCommodityManager.getCommodities().stream()
                        .map(x -> ResourceLocation.parse(x.getItemId())), b))
                    .then(Commands.argument("quantity", IntegerArgumentType.integer(1, 10_000_000))
                        .executes(c -> simulate(c, null))
                        .then(Commands.argument("player", EntityArgument.player())
                            .executes(c -> simulate(c, EntityArgument.getPlayer(c, "player")))))))
            .then(Commands.literal("arbitrage").requires(s -> s.hasPermission(2))
                .executes(c -> arbitrage(c.getSource(), 10))
                .then(Commands.argument("count", IntegerArgumentType.integer(1, 50))
                    .executes(c -> arbitrage(c.getSource(), IntegerArgumentType.getInteger(c, "count")))))
            .then(Commands.literal("calibrate").requires(s -> s.hasPermission(2))
                .executes(c -> calibrate(c.getSource())))
            .then(Commands.literal("ledger").requires(s -> s.hasPermission(2))
                .executes(c -> ledger(c.getSource(), 7))
                .then(Commands.argument("days", IntegerArgumentType.integer(1, 60))
                    .executes(c -> ledger(c.getSource(), IntegerArgumentType.getInteger(c, "days")))))
            // 0.1.10.33: the market day (it no longer follows /time set back) and its repair
            .then(Commands.literal("day").requires(s -> s.hasPermission(2))
                .executes(c -> day(c.getSource()))
                .then(Commands.literal("repair").executes(c -> repairDay(c.getSource()))))));
    }

    /** 0.1.10.33: the market day, the world's day, the world day the market saw last and the latest stamp of the exchange. */
    private static int day(CommandSourceStack source) {
        MinecraftServer server = source.getServer();
        FutureMarketData fm = FutureMarketData.get(server.overworld());
        long world = server.overworld().getDayTime() / 24000L;
        long latest = Math.max(MarketDayRepair.latestStamp(ExchangeData.get(server)), fm.latestStamp());
        long seen = fm.getRawDay();
        source.sendSuccess(() -> Component.translatable(K + "day", fm.getCurrentDay() + 1, world + 1, seen == MarketDay.UNSEEN ? "-" : Long.toString(seen + 1),
            latest == MarketDay.UNSEEN ? "-" : Long.toString(latest + 1)), false);
        return (int) Math.max(0, Math.min(Integer.MAX_VALUE, fm.getCurrentDay()));
    }

    /** 0.1.10.33: runs the repair now (it also runs once at every load). */
    private static int repairDay(CommandSourceStack source) {
        MinecraftServer server = source.getServer();
        MarketDayRepair.Report r = MarketDayDriver.repair(server.overworld(), FutureMarketData.get(server.overworld()));
        if (r.changed()) source.sendSuccess(() -> Component.translatable(K + "day_repaired", r.oldDay() + 1, r.newDay() + 1, r.moved()), true);
        else source.sendSuccess(() -> Component.translatable(K + "day_ok", r.oldDay() + 1), false);
        return r.changed() ? 1 : 0;
    }

    private static String f(double v) { return String.format(Locale.ROOT, "%,.1f", v); }

    private static int simulate(CommandContext<CommandSourceStack> c, ServerPlayer player) throws CommandSyntaxException {
        CommandSourceStack source = c.getSource();
        MinecraftServer server = source.getServer();
        String id = ResourceLocationArgument.getId(c, "item").toString();
        int quantity = IntegerArgumentType.getInteger(c, "quantity");
        ExchangeService.Market m = ExchangeService.market(server, id);
        if (m == null || !m.system()) {
            source.sendFailure(Component.translatable(K + "not_commodity", id));
            return 0;
        }
        ExchangeData d = ExchangeData.get(server);
        long day = ExchangeService.today(server);
        SellDepth.Curve curve = ExchangeService.curve(m);
        double pressure = ExchangeService.pressure(d, id);
        long mine = player == null ? 0 : ExchangeService.soldToday(d, player.getUUID(), id, day);
        double coins = Math.floor(SellDepth.payout(curve, mine, pressure, quantity));
        double last = SellDepth.marginal(curve, mine + quantity, pressure + quantity);
        long alone = SellDepth.clear(curve, pressure, List.of(new SellDepth.Bid(mine, quantity, 0, Double.POSITIVE_INFINITY))).get(0).coins();
        source.sendSuccess(() -> Component.translatable(K + "simulate", id, m.price(), f(curve.unit()), (long) curve.lp(), (long) curve.lg(),
            f(pressure), mine), false);
        source.sendSuccess(() -> Component.translatable(K + "simulate_sale", quantity, f(coins), f(coins / quantity), f(last), alone,
            f(SellDepth.ceiling(curve, mine, pressure))), false);
        if (player != null) {
            long allowance = ExchangeService.allowanceLeft(server, d, player.getUUID(), day);
            source.sendSuccess(() -> Component.translatable(K + "simulate_allowance", player.getGameProfile().getName(),
                allowance == Long.MAX_VALUE ? "-" : Long.toString(allowance)), false);
        }
        return quantity;
    }

    private static int arbitrage(CommandSourceStack source, int count) {
        MinecraftServer server = source.getServer();
        ExchangeData d = ExchangeData.get(server);
        long day = ExchangeService.today(server);
        Map<Item, Double> ask = new HashMap<>(), bid = new HashMap<>();
        for (ExchangeService.Market m : ExchangeService.markets(server)) {
            if (!m.system()) continue;
            if (ExchangeService.supply(d, m.id(), day) > 0) ask.put(m.item(), (double) ExchangeService.askOne(d, m));
            bid.put(m.item(), ExchangeService.curve(m).unit());
        }
        double fuel = 0;
        Item coal = BuiltInRegistries.ITEM.get(ResourceLocation.withDefaultNamespace("coal"));
        if (ask.containsKey(coal)) fuel = ask.get(coal) / 8.0;
        List<ArbitrageAudit.Recipe<Item>> recipes = new ArrayList<>();
        for (var holder : server.getRecipeManager().getRecipes()) {
            var recipe = holder.value();
            ItemStack result = com.miophas.singularity_iteration.common.util.RecipeResults.of(recipe, server.registryAccess());
            if (result.isEmpty()) continue;
            List<Set<Item>> slots = new ArrayList<>();
            try {
                for (Ingredient ingredient : recipe.getIngredients()) {
                    if (ingredient.isEmpty()) continue;
                    Set<Item> choices = new HashSet<>();
                    for (ItemStack s : ingredient.getItems()) if (!s.isEmpty()) choices.add(s.getItem());
                    if (choices.isEmpty()) { slots.clear(); break; }
                    slots.add(choices);
                }
            } catch (RuntimeException e) {
                continue;
            }
            if (slots.isEmpty()) continue;
            double energy = 0;
            if (recipe instanceof com.miophas.singularity_iteration.core.api.recipe.IRecipeProcessingInfo info) {
                energy = info.getTotalEnergyCost();
                if (slots.size() == 1 && info.getIngredientCount() > 1) {
                    Set<Item> only = slots.get(0);
                    for (int i = 1; i < info.getIngredientCount(); i++) slots.add(only);
                }
            }
            double extra = recipe instanceof AbstractCookingRecipe ? fuel : 0;
            recipes.add(new ArbitrageAudit.Recipe<>(holder.id().toString(), result.getItem(), result.getCount(), slots, extra, energy));
        }
        var loops = ArbitrageAudit.loops(ask, bid, recipes);
        var cycles = ArbitrageAudit.cycles(recipes);
        source.sendSuccess(() -> Component.translatable(K + "arbitrage", recipes.size(), ask.size(), loops.size(), cycles.size()), false);
        for (int i = 0; i < Math.min(count, loops.size()); i++) {
            var l = loops.get(i);
            String inputs = String.join(", ", l.inputs().stream().map(x -> BuiltInRegistries.ITEM.getKey(x).getPath()).distinct().toList());
            source.sendSuccess(() -> Component.translatable(K + "loop", BuiltInRegistries.ITEM.getKey(l.item()).toString(), f(l.bid()), f(l.cost()),
                f(l.profit()), f(l.energy()), f(l.energyPerCoin()), l.via(), inputs), false);
        }
        if (!cycles.isEmpty()) {
            List<String> names = new ArrayList<>();
            for (Item item : cycles) names.add(BuiltInRegistries.ITEM.getKey(item).toString());
            names.sort(null);
            String shown = String.join(", ", names.subList(0, Math.min(12, names.size())));
            source.sendSuccess(() -> Component.translatable(K + "cycles", shown), false);
        }
        return loops.size();
    }

    private static int calibrate(CommandSourceStack source) {
        MinecraftServer server = source.getServer();
        double perBucket = ExchangeService.coinsPerBucket(server);
        List<double[]> rows = new ArrayList<>();
        List<String> names = new ArrayList<>();
        int without = 0;
        for (FutureCommodity c : FutureCommodityManager.getCommodities()) {
            double buckets = ExchangeService.uuBuckets(server, c.getItem());
            if (!(buckets > 0)) { without++; continue; }
            rows.add(new double[] {c.getBasePrice(), buckets, c.getBasePrice() / (buckets * perBucket)});
            names.add(c.getItemId());
        }
        final int none = without;
        source.sendSuccess(() -> Component.translatable(K + "calibrate", f(perBucket), rows.size(), none), false);
        List<Integer> order = new ArrayList<>();
        for (int i = 0; i < rows.size(); i++) order.add(i);
        order.sort((a, b) -> Double.compare(rows.get(a)[2], rows.get(b)[2]));
        for (int i : order) {
            double[] r = rows.get(i);
            String name = names.get(i);
            source.sendSuccess(() -> Component.translatable(K + "calibrate_row", name, (long) r[0], String.format(Locale.ROOT, "%.5f", r[1]),
                f(r[1] * perBucket), String.format(Locale.ROOT, "%.2f", r[2])), false);
        }
        return rows.size();
    }

    private static int ledger(CommandSourceStack source, int days) {
        MinecraftServer server = source.getServer();
        ExchangeData d = ExchangeData.get(server);
        long day = ExchangeService.today(server);
        long minted = 0, burned = 0;
        for (ExchangeData.LedgerDay l : d.ledger) {
            if (day - l.day >= days) continue;
            minted += l.created();   // 0.1.10.35: into / out of circulation
            burned += l.destroyed();
            final long dd = l.day + 1, mm = l.created(), bb = l.destroyed();
            source.sendSuccess(() -> Component.translatable(K + "ledger_day", dd, mm, bb, mm - bb), false);
        }
        double pressure = 0;
        for (double p : d.pressure.values()) pressure += p;
        final long m = minted, b = burned;
        final String p = f(pressure);
        source.sendSuccess(() -> Component.translatable(K + "ledger", days, m, b, m - b, p, d.auction.size()), false);
        return (int) Math.max(Integer.MIN_VALUE, Math.min(Integer.MAX_VALUE, m - b));
    }
}
