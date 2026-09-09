package com.singularity_iteration.mio_icif.future;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;

/**
 * 期货货品�? * 从配置文件加载的货品种类
 */
@SuppressWarnings("null")
public class FutureCommodity {
    private final Item item;
    private final String name;
    private final String itemId;
    private final int basePrice;
    private final float volatility;
    private final CommodityCategory category;

    public FutureCommodity(String itemId, int basePrice, float volatility, CommodityCategory category) {
        this.itemId = itemId;
        this.item = BuiltInRegistries.ITEM.get(ResourceLocation.parse(itemId));
        this.name = item.getDescriptionId();
        this.basePrice = basePrice;
        this.volatility = volatility;
        this.category = category;
    }

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
     * 获取显示名称（用于GUI�?     */
    public String getDisplayName() {
        return item.getDescription().getString();
    }
}


