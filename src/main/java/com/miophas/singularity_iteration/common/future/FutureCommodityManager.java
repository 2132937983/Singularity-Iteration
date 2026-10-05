package com.miophas.singularity_iteration.common.future;

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
    /** Immutable snapshot; replaced as a whole so the render thread never sees a half-applied reload. */
    private record Snapshot(List<FutureCommodity> all, Map<CommodityCategory, List<FutureCommodity>> byCategory) { }
    private static volatile Snapshot snapshot = build(List.of());

    private static Snapshot build(List<FutureCommodity> commodities) {
        List<FutureCommodity> all = new ArrayList<>();
        Map<CommodityCategory, List<FutureCommodity>> map = new java.util.EnumMap<>(CommodityCategory.class);
        for (CommodityCategory category : CommodityCategory.values()) map.put(category, new ArrayList<>());
        for (FutureCommodity commodity : commodities) {
            if (commodity.getItem() == null || commodity.getItem() == net.minecraft.world.item.Items.AIR) continue;
            all.add(commodity);
            map.get(commodity.getCategory()).add(commodity);
        }
        map.replaceAll((c, list) -> List.copyOf(list));
        return new Snapshot(List.copyOf(all), Map.copyOf(map));
    }

    /**
     * Commodities come from datapacks ({@code data/<namespace>/si_futures/*.json}, see
     * {@link FutureCommodityLoader}) and reach clients through {@link FutureCommoditySync}; the
     * old config list is gone. Until data arrives the list is empty.
     */
    public static void init() { }

    /** Replaces the commodity list (datapack reload on the server, sync packet on the client). */
    public static void apply(List<FutureCommodity> commodities) {
        snapshot = build(commodities);
    }

    /** Kept for callers of the old config reload: the list only changes with datapacks now. */
    public static void reload() { }

    // ------------------------------------------------------------------ unlocks
    /** Client: commodities the local player has not unlocked (sent with the sync packet). */
    private static final java.util.Set<String> CLIENT_LOCKED = java.util.concurrent.ConcurrentHashMap.newKeySet();

    public static void setClientLocked(java.util.Collection<String> ids) {
        CLIENT_LOCKED.clear();
        CLIENT_LOCKED.addAll(ids);
    }

    /** Server: has {@code player} completed the commodity's unlock advancement (always true without one)? */
    public static boolean isUnlocked(net.minecraft.server.level.ServerPlayer player, FutureCommodity commodity) {
        var id = commodity.getUnlockAdvancement();
        if (id == null) return true;
        var holder = player.server.getAdvancements().get(id);
        return holder == null || player.getAdvancements().getOrStartProgress(holder).isDone();
    }

    /** Either side: locked for this player (server checks advancements, client uses the synced set). */
    public static boolean isLocked(net.minecraft.world.entity.player.Player player, FutureCommodity commodity) {
        if (player instanceof net.minecraft.server.level.ServerPlayer sp) return !isUnlocked(sp, commodity);
        return CLIENT_LOCKED.contains(commodity.getItemId());
    }

    public static List<FutureCommodity> getCommodities() {
        return snapshot.all();
    }

    public static List<FutureCommodity> getCommoditiesByCategory(CommodityCategory category) {
        return snapshot.byCategory().getOrDefault(category, List.of());
    }

    public static List<CommodityCategory> getCategoriesWithCommodities() {
        List<CommodityCategory> categories = new ArrayList<>();
        for (CommodityCategory category : CommodityCategory.values()) {
            if (!getCommoditiesByCategory(category).isEmpty()) categories.add(category);
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


