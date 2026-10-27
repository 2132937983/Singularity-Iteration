// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.exchange;

import com.miophas.singularity_iteration.common.item.normal.mio_icif_normal;
import com.miophas.singularity_iteration.common.recipe.generic.RecyclerRecipe;
import com.miophas.singularity_iteration.common.recipe.generic.RecyclerRecipes;
import com.miophas.singularity_iteration.common.recipe.mio_icif_SingleItemRecipeInput;
import com.miophas.singularity_iteration.core.runtime.processing.RecipeLookupCache;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.ItemStack;

/**
 * 0.1.10.28 (X4): the scrap sink in the game (rules in {@link ScrapRules}). The items an instant sale leaves go to the
 * recycler instead, when the market pays almost nothing for them: the SI recycler's recipe for the item (its product and
 * chance), else scrap at the IC2 chance of 1/8; an item the recycler takes for nothing (an empty product) stays. The items
 * are destroyed (they do not enter the treasury or the sell pressure); the ledger counts the items in and the products out.
 */
public final class ScrapSink {
    private ScrapSink() {}

    private static final RecipeLookupCache<mio_icif_SingleItemRecipeInput, RecyclerRecipe> RECIPES =
        new RecipeLookupCache<>(RecyclerRecipes.RECYCLER_TYPE);

    /** The recycler's product and chance for one item. */
    public record Yield(ItemStack product, double chance) { }

    /** The yield of {@code proto}, or null when the recycler gives nothing for it. */
    public static Yield yieldOf(MinecraftServer server, ItemStack proto) {
        if (proto.isEmpty()) return null;
        RecyclerRecipe recipe = RECIPES.findValue(server.overworld(), new mio_icif_SingleItemRecipeInput(proto.copyWithCount(1)));
        ItemStack product = recipe != null ? recipe.getResult().copy() : new ItemStack(mio_icif_normal.SCRAP.get());
        if (product.isEmpty()) return null;
        double chance = recipe != null && recipe.getChance() >= 0 ? recipe.getChance() : ScrapRules.RECYCLER_CHANCE;
        return chance > 0 ? new Yield(product, chance) : null;
    }

    /** True when the items a sale of {@code m} left for {@code seller} may go to the sink now (sink on, next bid below the floor). */
    public static boolean eligible(ExchangeData d, ExchangeService.Market m, UUID seller, long day) {
        if (m == null || !m.system()) return false;
        SellDepth.Curve c = ExchangeService.curve(m);
        double next = SellDepth.marginal(c, ExchangeService.soldToday(d, seller, m.id(), day), ExchangeService.pressure(d, m.id()));
        return ScrapRules.eligible(next, ScrapRules.floor(c.unit(), EconomySettings.scrapThreshold()));
    }

    /**
     * Recycles {@code counts[i]} items of {@code protos[i]}: the products go to {@code out} (whole stacks), the ledger of
     * {@code day} counts them. Returns the items that stay, per entry, and in {@code totals} {items consumed, products}.
     */
    public static long[] recycle(MinecraftServer server, ExchangeData d, List<ItemStack> protos, long[] counts, long day,
                                 Consumer<ItemStack> out, long[] totals) {
        long[] stay = counts.clone();
        for (int i = 0; i < protos.size(); i++) {
            if (counts[i] <= 0) continue;
            Yield y = yieldOf(server, protos.get(i));
            if (y == null) continue;
            long[] r = ScrapRules.yield(counts[i], y.chance());
            if (r[0] <= 0) continue;
            stay[i] = counts[i] - r[1];
            long products = Math.multiplyExact(r[0], (long) Math.max(1, y.product().getCount()));
            totals[0] += r[1];
            totals[1] += products;
            ExchangeData.LedgerDay l = d.ledger(day);
            l.scrapIn += r[1];
            l.scrapOut += products;
            for (ItemStack s : stacks(y.product(), products)) out.accept(s);
        }
        d.setDirty();
        return stay;
    }

    /** {@code amount} of {@code proto} as stacks of its largest size. */
    static List<ItemStack> stacks(ItemStack proto, long amount) {
        List<ItemStack> out = new ArrayList<>();
        int max = Math.max(1, proto.getMaxStackSize());
        while (amount > 0) {
            int n = (int) Math.min(amount, max);
            out.add(proto.copyWithCount(n));
            amount -= n;
        }
        return out;
    }
}
