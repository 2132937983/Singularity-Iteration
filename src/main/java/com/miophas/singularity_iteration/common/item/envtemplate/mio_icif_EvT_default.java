package com.miophas.singularity_iteration.common.item.envtemplate;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * Base class for terrain transformer templates.
 * The terrain transformer uses templates to change the environment.
 * Maximum stack size: 1.
 */
@SuppressWarnings("null")
public class mio_icif_EvT_default extends Item {

    // 模板类型枚举
    @SuppressWarnings("null")
public enum TemplateType {
        EMPTY("empty", 0), // Blank template. The terrain transformer cannot use it.
        CULTIVATION("cultivation", 400), // Cultivation template: 400 EU/t
        DESERTIFICATION("desertification", 80), // Desertification template: 80 EU/t
        IRRIGATION("irrigation", 160), // Irrigation template: 160 EU/t
        CHILLING("chilling", 80), // Chilling template: 80 EU/t
        FLATIFICATION("flatification", 800), // Flatification template: 800 EU/t
        MUSHROOM("mushroom", 160); // Mushroom template: 160 EU/t

        private final String name;
        private final int euPerTick; // EU cost per tick

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
     * Creates a terrain conversion template.
     * @param properties the item properties
     * @param templateType the template type
     */
    public mio_icif_EvT_default(Item.Properties properties, TemplateType templateType) {
        super(properties.stacksTo(1)); // Maximum stack size: 1
        this.templateType = templateType;
    }

    /**
     * 获取模板类型
     */
    public TemplateType getTemplateType() {
        return templateType;
    }

    /**
     * Gets the EU cost per tick.
     */
    public int getEuPerTick() {
        return templateType.getEuPerTick();
    }

    /**
     * Checks if this is the blank template.
     * The terrain transformer cannot use the blank template.
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


