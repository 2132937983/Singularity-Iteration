// SCEX 2026-09-12: repaired malformed UTF-8 bytes in comments only.
package com.miophas.singularity_iteration.common.future;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;

/**
 * Futures commodity.
 * A commodity type that the mod loads from the configuration file.
 */
@SuppressWarnings("null")
public class FutureCommodity {
    private final Item item;
    private final String name;
    private final String itemId;
    private final int basePrice;
    private final float volatility;
    private final CommodityCategory category;

    /** Days between price changes (datapack {@code period_days}, at least 1). */
    private final int periodDays;
    /** Advancement a player needs before trading this commodity (datapack {@code unlock.advancement}), or null. */
    @org.jetbrains.annotations.Nullable private final ResourceLocation unlockAdvancement;

    public FutureCommodity(String itemId, int basePrice, float volatility, CommodityCategory category) {
        this(itemId, basePrice, volatility, category, 1, null);
    }

    /**
     * 0.1.10.25 (X1): the market of this commodity from the datapack ({@code depth.player}, {@code depth.server},
     * {@code export_per_day}, {@code import_per_day}, {@code import_premium}); -1 = the config value. Server side only (not
     * synced: the client gets quotes in the exchange view).
     */
    public record Market(int depthPlayer, int depthServer, double exportPerDay, int importPerDay, double importPremium) {
        public static final Market DEFAULT = new Market(-1, -1, -1, -1, -1);
    }

    private final Market market;

    /**
     * 0.1.10.26 (X3): how the sell box and the daily auction take this commodity (datapack {@code tag},
     * {@code accept_components}). {@code tag}: an item tag whose items are also bought as this commodity ("" = none);
     * {@code acceptComponents}: stacks with changed components (name, damage, enchantments) are bought too. Synced to clients.
     */
    public record Selling(String tag, boolean acceptComponents) {
        public static final Selling DEFAULT = new Selling("", false);
    }

    private Selling selling = Selling.DEFAULT;

    /** 0.1.10.26: the sell-box settings of this commodity. */
    public Selling getSelling() { return selling; }

    /** 0.1.10.26: sets the sell-box settings (loader and sync only). */
    public FutureCommodity withSelling(Selling selling) {
        this.selling = selling == null ? Selling.DEFAULT : selling;
        return this;
    }

    public FutureCommodity(String itemId, int basePrice, float volatility, CommodityCategory category,
                           int periodDays, @org.jetbrains.annotations.Nullable ResourceLocation unlockAdvancement) {
        this(itemId, basePrice, volatility, category, periodDays, unlockAdvancement, Market.DEFAULT);
    }

    public FutureCommodity(String itemId, int basePrice, float volatility, CommodityCategory category,
                           int periodDays, @org.jetbrains.annotations.Nullable ResourceLocation unlockAdvancement, Market market) {
        this.itemId = itemId;
        this.item = BuiltInRegistries.ITEM.get(ResourceLocation.parse(itemId));
        this.name = item.getDescriptionId();
        this.basePrice = basePrice;
        this.volatility = volatility;
        this.category = category;
        this.periodDays = Math.max(1, periodDays);
        this.unlockAdvancement = unlockAdvancement;
        this.market = market == null ? Market.DEFAULT : market;
    }

    /** 0.1.10.25: the datapack market settings (-1 = the config value). */
    public Market getMarket() { return market; }

    public int getPeriodDays() { return periodDays; }

    @org.jetbrains.annotations.Nullable
    public ResourceLocation getUnlockAdvancement() { return unlockAdvancement; }

    /** True on the days this commodity's price moves (every {@link #getPeriodDays()} days). */
    public boolean repricesOn(long day) { return periodDays <= 1 || Math.floorMod(day, periodDays) == 0; }

    public Item getItem() {
        return item;
    }

    public String getName() {
        return name;
    }

    public String getItemId() {
        return itemId;
    }

    public int getBasePrice() {
        return basePrice;
    }

    public float getVolatility() {
        return volatility;
    }

    public CommodityCategory getCategory() {
        return category;
    }

    /**
     * 获取显示名称（用于GUI???     */
    public String getDisplayName() {
        return item.getDescription().getString();
    }
}


