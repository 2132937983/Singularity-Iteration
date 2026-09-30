package com.miophas.singularity_iteration.common.item.armor;

import com.miophas.singularity_iteration.core.api.item.ArmorFeatureInfo;
import com.miophas.singularity_iteration.core.prefab.item.ArmorFeatureState;
import com.miophas.singularity_iteration.core.prefab.item.ArmorFeatureTooltip;
import com.miophas.singularity_iteration.core.prefab.item.ArmorFeatures;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/**
 * 装备特性开关（兼容层）。
 *
 * <p>已迁移至 core 的 {@link ArmorFeatureState} / {@link ArmorFeatures} / {@link ArmorFeatureTooltip}，
 * 本类仅保留旧调用点所需的转发方法，行为与 core 实现完全一致（默认启用）。
 *
 * <p>新代码（含附属模组）请直接使用公开 API：{@code IItemAPI} 的
 * {@code isArmorFeatureEnabled} / {@code setArmorFeatureEnabled} / {@code toggleArmorFeature} /
 * {@code cycleArmorFeature}，或继承 {@code AbstractElectricArmor} 使用统一 Tooltip。
 *
 * @deprecated 使用 {@link ArmorFeatures} / {@link ArmorFeatureState} / {@link ArmorFeatureTooltip}。
 */
@Deprecated
@SuppressWarnings("null")
public class ArmorFeatureToggle {

    /**
     * @deprecated 使用 {@link ArmorFeatureInfo} 代替。
     */
    @Deprecated
    public static record FeatureInfo(EquipmentSlot slot, String featureKey, String featureNameKey, boolean isMode, Component currentModeName) {
        public FeatureInfo(EquipmentSlot slot, String featureKey, String featureNameKey) {
            this(slot, featureKey, featureNameKey, false, null);
        }

        public FeatureInfo(EquipmentSlot slot, String featureKey, String featureNameKey, Component currentModeName) {
            this(slot, featureKey, featureNameKey, true, currentModeName);
        }

        public ArmorFeatureInfo toApi() {
            return new ArmorFeatureInfo(slot, featureKey, featureNameKey, isMode, currentModeName);
        }
    }

    public static ArmorFeatureInfo toApi(FeatureInfo info) {
        return new ArmorFeatureInfo(info.slot, info.featureKey, info.featureNameKey, info.isMode, info.currentModeName);
    }

    public static FeatureInfo fromApi(ArmorFeatureInfo info) {
        return new FeatureInfo(info.slot(), info.featureKey(), info.featureNameKey(), info.isMode(), info.currentModeName());
    }

    public static boolean isEnabled(ItemStack stack, String featureKey) {
        return ArmorFeatureState.isEnabled(stack, featureKey);
    }

    public static void setEnabled(ItemStack stack, String featureKey, boolean enabled) {
        if (!isToggleFeature(stack, null, featureKey)) return;
        ArmorFeatureState.setEnabled(stack, featureKey, enabled);
    }

    public static boolean toggle(ItemStack stack, String featureKey) {
        return ArmorFeatures.toggle(stack, featureKey);
    }

    /** 仅物品声明的开关型特性可创建持久化特性数据。 */
    public static boolean isToggleFeature(ItemStack stack, EquipmentSlot slot, String featureKey) {
        return ArmorFeatures.isToggleFeature(stack, slot, featureKey);
    }

    public static void addToggleLine(List<Component> tooltip, ItemStack stack, EquipmentSlot slot, String featureKey, String featureNameKey) {
        ArmorFeatureTooltip.append(tooltip, stack, new ArmorFeatureInfo(slot, featureKey, featureNameKey));
    }

    public static void addModeLine(List<Component> tooltip, ItemStack stack, EquipmentSlot slot, String featureKey, String featureNameKey, Component currentModeName) {
        ArmorFeatureTooltip.append(tooltip, stack, new ArmorFeatureInfo(slot, featureKey, featureNameKey, currentModeName));
    }

    public static void addFeaturesToTooltip(List<Component> tooltip, ItemStack stack, List<FeatureInfo> features) {
        for (FeatureInfo feature : features) {
            ArmorFeatureTooltip.append(tooltip, stack, toApi(feature));
        }
    }
}
