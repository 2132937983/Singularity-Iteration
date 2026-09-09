package com.singularity_iteration.mio_icif.future;

import com.singularity_iteration.mio_icif.Singularity_Iteration_Config;
import net.minecraft.world.item.Item;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 期货货品管理�? * 负责从配置文件加载和管理货品种类
 */
@SuppressWarnings("null")
public class FutureCommodityManager {
    private static final List<FutureCommodity> COMMODITIES = new ArrayList<>();
    private static final Map<CommodityCategory, List<FutureCommodity>> CATEGORY_MAP = new HashMap<>();
    private static boolean initialized = false;

    /**
     * 初始化货品列表（从配置文件加载）
     */
    public static void init() {
        if (initialized) {
            return;
        }

        COMMODITIES.clear();
        CATEGORY_MAP.clear();
        
        // 初始化所有种类的空列�
    for (CommodityCategory category : CommodityCategory.values()) {
            CATEGORY_MAP.put(category, new ArrayList<>());
        }
        
        List<Singularity_Iteration_Config.CommodityConfig> configs = Singularity_Iteration_Config.getCommodityConfigs();
        for (Singularity_Iteration_Config.CommodityConfig config : configs) {
            CommodityCategory category = CommodityCategory.fromId(config.getCategoryId());
            FutureCommodity commodity = new FutureCommodity(
                config.getItemId(),
                config.getBasePrice(),
                config.getVolatility(),
                category
            );
            
            // 验证物品是否有效
            if (commodity.getItem() != null && commodity.getItem() != net.minecraft.world.item.Items.AIR) {
                COMMODITIES.add(commodity);
                CATEGORY_MAP.get(category).add(commodity);
            }
        }
        
        initialized = true;
    }

    /**
     * 重新加载货品配置（配置变更时调用�
 */
    public static void reload() {
        initialized = false;
        init();
    }

    /**
     * 获取所有货�
 */
    public static List<FutureCommodity> getCommodities() {
        if (!initialized) {
            init();
        }
        return Collections.unmodifiableList(COMMODITIES);
    }

    /**
     * 获取指定种类的所有货�
 */
    public static List<FutureCommodity> getCommoditiesByCategory(CommodityCategory category) {
        if (!initialized) {
            init();
        }
        return Collections.unmodifiableList(CATEGORY_MAP.getOrDefault(category, Collections.emptyList()));
    }

    /**
     * 获取所有有货品的种类列�
 */
    public static List<CommodityCategory> getCategoriesWithCommodities() {
        if (!initialized) {
            init();
        }
        List<CommodityCategory> categories = new ArrayList<>();
        for (CommodityCategory category : CommodityCategory.values()) {
            if (!CATEGORY_MAP.get(category).isEmpty()) {
                categories.add(category);
            }
        }
        return categories;
    }

    /**
     * 获取货品数量
     */
    public static int getCommodityCount() {
        return getCommodities().size();
    }

    /**
     * 获取指定种类的货品数据
 */
    public static int getCommodityCountByCategory(CommodityCategory category) {
        return getCommoditiesByCategory(category).size();
    }

    /**
     * 根据索引获取货品
     */
    public static FutureCommodity getCommodity(int index) {
        List<FutureCommodity> commodities = getCommodities();
        if (index >= 0 && index < commodities.size()) {
            return commodities.get(index);
        }
        return null;
    }

    /**
     * 根据种类和索引获取货�
 */
    public static FutureCommodity getCommodityByCategory(CommodityCategory category, int index) {
        List<FutureCommodity> commodities = getCommoditiesByCategory(category);
        if (index >= 0 && index < commodities.size()) {
            return commodities.get(index);
        }
        return null;
    }

    /**
     * 根据物品获取货品
     */
    public static FutureCommodity getCommodityByItem(Item item) {
        for (FutureCommodity commodity : getCommodities()) {
            if (commodity.getItem() == item) {
                return commodity;
            }
        }
        return null;
    }

    /**
     * 根据物品ID获取货品
     */
    public static FutureCommodity getCommodityById(String itemId) {
        for (FutureCommodity commodity : getCommodities()) {
            if (commodity.getItemId().equals(itemId)) {
                return commodity;
            }
        }
        return null;
    }

    /**
     * 检查是否包含指定货�
 */
    public static boolean hasCommodity(String itemId) {
        return getCommodityById(itemId) != null;
    }
}


