// SCEX 2026-09-12: repaired malformed UTF-8 bytes in comments only.
package com.miophas.singularity_iteration.common.future;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;

/**
 * 期货货品??? * 从配置文件加载的货品种类
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

    public FutureCommodity(String itemId, int basePrice, float volatility, CommodityCategory category,
                           int periodDays, @org.jetbrains.annotations.Nullable ResourceLocation unlockAdvancement) {
        this.itemId = itemId;
        this.item = BuiltInRegistries.ITEM.get(ResourceLocation.parse(itemId));
        this.name = item.getDescriptionId();
        this.basePrice = basePrice;
        this.volatility = volatility;
        this.category = category;
        this.periodDays = Math.max(1, periodDays);
        this.unlockAdvancement = unlockAdvancement;
    }

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


