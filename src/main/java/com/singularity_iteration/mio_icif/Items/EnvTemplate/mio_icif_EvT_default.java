package com.singularity_iteration.mio_icif.Items.EnvTemplate;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * 地形转换模板基类
 * 用于地形转换机中改造环境的模板
 * 最大堆叠：1�?�?
 */
@SuppressWarnings("null")
public class mio_icif_EvT_default extends Item {

    // 模板类型枚举
    @SuppressWarnings("null")
public enum TemplateType {
        EMPTY("empty", 0),           // 空白模板 - 不能用于地形转换�?
        CULTIVATION("cultivation", 400),   // 耕地模板 - 400 EU/�?
        DESERTIFICATION("desertification", 80),  // 沙漠模板 - 80 EU/�?
        IRRIGATION("irrigation", 160),   // 灌溉模板 - 160 EU/�?
        CHILLING("chilling", 80),        // 冰原模板 - 80 EU/�?
        FLATIFICATION("flatification", 800), // 平地模板 - 800 EU/�?
        MUSHROOM("mushroom", 160);       // 蘑菇模板 - 160 EU/�?

        private final String name;
        private final int euPerTick;  // EU消耗?�?(实际为EU/tick)

        TemplateType(String name, int euPerTick) {
            this.name = name;
            this.euPerTick = euPerTick;
        }

        public String getName() {
            return name;
        }

        public int getEuPerTick() {
            return euPerTick;
        }
    }

    private final TemplateType templateType;

    /**
     * 创建地形转换模板
     * @param properties 物品属性?
     * @param templateType 模板类型
     */
    public mio_icif_EvT_default(Item.Properties properties, TemplateType templateType) {
        super(properties.stacksTo(1));  // 最大堆�?�?
        this.templateType = templateType;
    }

    /**
     * 获取模板类型
     */
    public TemplateType getTemplateType() {
        return templateType;
    }

    /**
     * 获取EU消耗（每tick�?
     */
    public int getEuPerTick() {
        return templateType.getEuPerTick();
    }

    /**
     * 检查是否是空白模板
     * 空白模板不能用于地形转换�?
     */
    public boolean isEmpty() {
        return templateType == TemplateType.EMPTY;
    }

    /**
     * 检查是否可以用于地形转换机
     */
    public boolean isUsable() {
        return templateType != TemplateType.EMPTY;
    }

    @Override
    public boolean isEnchantable(ItemStack stack) {
        return false;  // 不可附魔
    }

    @Override
    public boolean isRepairable(ItemStack stack) {
        return false;  // 不可修复
    }
}


