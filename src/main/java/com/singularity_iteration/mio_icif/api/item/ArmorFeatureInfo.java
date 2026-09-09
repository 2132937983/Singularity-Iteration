package com.singularity_iteration.mio_icif.api.item;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EquipmentSlot;

/**
 * 电力装甲特性信息记录。
 * <p>用于描述电力装甲的可切换特性（如夜视、飞行等），
 * 供 Tooltip 显示和特性切换 API 使用。
 *
 * @param slot          装备槽位
 * @param featureKey    特性键（用于 NBT 存储和切换命令）
 * @param featureNameKey 特性名称的翻译键
 * @param isMode        是否为模式切换（true）或开关切换（false）
 * @param currentModeName 当前模式名称（仅 isMode=true 时有效）
 */
public record ArmorFeatureInfo(EquipmentSlot slot, String featureKey, String featureNameKey, boolean isMode, Component currentModeName) {

    /**
     * 创建一个开关型特性信息
     */
    public ArmorFeatureInfo(EquipmentSlot slot, String featureKey, String featureNameKey) {
        this(slot, featureKey, featureNameKey, false, null);
    }

    /**
     * 创建一个模式型特性信息
     */
    public ArmorFeatureInfo(EquipmentSlot slot, String featureKey, String featureNameKey, Component currentModeName) {
        this(slot, featureKey, featureNameKey, true, currentModeName);
    }
}