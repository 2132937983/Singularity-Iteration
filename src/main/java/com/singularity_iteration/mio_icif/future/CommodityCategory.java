package com.singularity_iteration.mio_icif.future;

import net.minecraft.network.chat.Component;

/**
 * 货品种类枚举
 * 定义期货机中货品的分配? */
@SuppressWarnings("null")
public enum CommodityCategory {
    MINERAL("mineral", "gui.mio_icif.future.category.mineral"),      // 矿产
    AGRICULTURE("agriculture", "gui.mio_icif.future.category.agriculture"),  // 农产�?    WOOD("wood", "gui.mio_icif.future.category.wood"),              // 木材
    FOOD("food", "gui.mio_icif.future.category.food"),              // 食物
    OTHER("other", "gui.mio_icif.future.category.other");           // 其他

    private final String id;
    private final String translationKey;

    CommodityCategory(String id, String translationKey) {
        this.id = id;
        this.translationKey = translationKey;
    }

    public String getId() {
        return id;
    }

    public String getTranslationKey() {
        return translationKey;
    }

    public Component getDisplayName() {
        return Component.translatable(translationKey);
    }

    /**
     * 根据ID获取种类
     */
    public static CommodityCategory fromId(String id) {
        for (CommodityCategory category : values()) {
            if (category.id.equalsIgnoreCase(id)) {
                return category;
            }
        }
        return OTHER; // 默认返回其他
    }

    /**
     * 获取所有种类的ID数组
     */
    public static String[] getAllIds() {
        String[] ids = new String[values().length];
        for (int i = 0; i < values().length; i++) {
            ids[i] = values()[i].id;
        }
        return ids;
    }
}


