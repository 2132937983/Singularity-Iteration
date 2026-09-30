package com.miophas.singularity_iteration.core.prefab.item;

import com.miophas.singularity_iteration.core.api.item.ArmorFeatureInfo;
import com.miophas.singularity_iteration.core.api.item.IArmorModeFeatureItem;
import com.miophas.singularity_iteration.core.api.item.IElectricArmorItem;

import java.util.List;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * 装备特性统一分发入口。
 *
 * <p>集中处理"物品声明了哪些特性、当前状态、如何切换"，供工具提示、特性管理 GUI、
 * 命令与网络包共用。所有判定均基于 {@link IElectricArmorItem#getFeatures(ItemStack)}
 * 声明，不识别具体物品类，因此附属模组只需实现接口即可被完整支持。
 *
 * <p>语义：
 * <ul>
 *   <li>开关型（{@code isMode=false}）：状态由 {@link ArmorFeatureState} 存储，默认启用；</li>
 *   <li>模式型（{@code isMode=true}）：通过 {@link IArmorModeFeatureItem#cycleMode} 切换，
 *       当前模式名称由 {@link IElectricArmorItem#getFeatures(ItemStack)} 提供。</li>
 * </ul>
 */
public final class ArmorFeatures {

    private ArmorFeatures() {
    }

    /**
     * 获取物品声明的全部装备特性。
     */
    public static List<ArmorFeatureInfo> features(ItemStack stack) {
        if (stack.isEmpty() || !(stack.getItem() instanceof IElectricArmorItem armor)) {
            return List.of();
        }
        return armor.getFeatures(stack);
    }

    /**
     * 查找指定特性键的声明。
     *
     * @return 未声明时返回 {@code null}
     */
    @Nullable
    public static ArmorFeatureInfo find(ItemStack stack, String featureKey) {
        if (featureKey == null) {
            return null;
        }
        for (ArmorFeatureInfo info : features(stack)) {
            if (info.featureKey().equals(featureKey)) {
                return info;
            }
        }
        return null;
    }

    /**
     * 判断物品是否声明了指定特性（可按槽位过滤）。
     *
     * @param slot 为 {@code null} 时不限槽位
     */
    public static boolean isDeclared(ItemStack stack, @Nullable EquipmentSlot slot, String featureKey) {
        ArmorFeatureInfo info = find(stack, featureKey);
        return info != null && (slot == null || info.slot() == slot);
    }

    /**
     * 判断物品是否声明了指定<b>开关型</b>特性。
     */
    public static boolean isToggleFeature(ItemStack stack, @Nullable EquipmentSlot slot, String featureKey) {
        ArmorFeatureInfo info = find(stack, featureKey);
        return info != null && !info.isMode() && (slot == null || info.slot() == slot);
    }

    /**
     * 判断物品是否声明了指定<b>模式型</b>特性。
     */
    public static boolean isModeFeature(ItemStack stack, @Nullable EquipmentSlot slot, String featureKey) {
        ArmorFeatureInfo info = find(stack, featureKey);
        return info != null && info.isMode() && (slot == null || info.slot() == slot);
    }

    /**
     * 读取开关型特性的启用状态；模式型特性恒为 {@code true}。
     */
    public static boolean isEnabled(ItemStack stack, String featureKey) {
        ArmorFeatureInfo info = find(stack, featureKey);
        if (info == null || info.isMode()) {
            return true;
        }
        return ArmorFeatureState.isEnabled(stack, featureKey);
    }

    /**
     * 设置开关型特性的启用状态；非开关型特性无操作。
     */
    public static void setEnabled(ItemStack stack, String featureKey, boolean enabled) {
        if (isToggleFeature(stack, null, featureKey)) {
            ArmorFeatureState.setEnabled(stack, featureKey, enabled);
        }
    }

    /**
     * 切换开关型特性。
     *
     * @return 切换后的状态；非开关型特性返回当前状态
     */
    public static boolean toggle(ItemStack stack, String featureKey) {
        if (!isToggleFeature(stack, null, featureKey)) {
            return isEnabled(stack, featureKey);
        }
        return ArmorFeatureState.toggle(stack, featureKey);
    }

    /**
     * 切换到模式型特性的下一个模式。
     *
     * @return 是否发生了切换
     */
    public static boolean cycleMode(ItemStack stack, String featureKey) {
        if (stack.isEmpty() || !isModeFeature(stack, null, featureKey)) {
            return false;
        }
        if (!(stack.getItem() instanceof IArmorModeFeatureItem modeItem)) {
            return false;
        }
        return modeItem.cycleMode(stack, featureKey);
    }

    /**
     * 按特性声明类型自动切换（开关型取反 / 模式型循环）。
     *
     * @return 是否发生了变更
     */
    public static boolean toggleOrCycle(ItemStack stack, @Nullable EquipmentSlot slot, String featureKey) {
        ArmorFeatureInfo info = find(stack, featureKey);
        if (info == null || (slot != null && info.slot() != slot)) {
            return false;
        }
        if (info.isMode()) {
            return cycleMode(stack, featureKey);
        }
        ArmorFeatureState.toggle(stack, featureKey);
        return true;
    }

    /**
     * 获取特性名称翻译键。
     *
     * @return 未声明时返回 {@code null}
     */
    @Nullable
    public static String nameKey(ItemStack stack, String featureKey) {
        ArmorFeatureInfo info = find(stack, featureKey);
        return info == null ? null : info.featureNameKey();
    }
}
