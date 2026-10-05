package com.miophas.singularity_iteration.core.prefab.item;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

/**
 * 装备特性（开关型）状态存储。
 *
 * <p>状态存放于物品的 {@code CustomData} 组件，键为 {@code feat_<featureKey>}。
 * 该实现与具体物品类、注册表内容无关，因此 core、common 与附属模组可共用同一存储语义。
 *
 * <p><b>默认语义：未显式写入时为"启用"</b>（新物品默认打开）。
 */
public final class ArmorFeatureState {

    /** 特性键前缀。 */
    public static final String FEATURE_PREFIX = "feat_";

    /** 未显式写入时的默认启用状态。 */
    public static final boolean DEFAULT_ENABLED = true;

    private ArmorFeatureState() {
    }

    private static final java.util.Map<String, Boolean> DEFAULTS = new java.util.concurrent.ConcurrentHashMap<>();

    /** A feature that starts switched off (for behaviour a player has to opt into). */
    public static void registerDefault(String featureKey, boolean enabled) {
        DEFAULTS.put(featureKey, enabled);
    }

    public static boolean defaultFor(String featureKey) {
        return featureKey == null ? DEFAULT_ENABLED : DEFAULTS.getOrDefault(featureKey, DEFAULT_ENABLED);
    }

    /**
     * 读取开关型特性的启用状态。
     *
     * @param stack      物品堆
     * @param featureKey 特性键
     * @return 是否启用；空物品或空键返回 {@link #DEFAULT_ENABLED}
     */
    public static boolean isEnabled(ItemStack stack, String featureKey) {
        if (stack.isEmpty() || featureKey == null) {
            return defaultFor(featureKey);
        }
        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        if (customData == null || customData.isEmpty()) {
            return defaultFor(featureKey);
        }
        String key = FEATURE_PREFIX + featureKey;
        // CustomData#contains / #read avoid copying the whole tag on every check (HUD code asks each frame)
        CompoundTag tag = customData.getUnsafe();
        return tag.contains(key) ? tag.getBoolean(key) : defaultFor(featureKey);
    }

    /**
     * 写入开关型特性的启用状态。
     *
     * @param stack      物品堆
     * @param featureKey 特性键
     * @param enabled    是否启用
     */
    public static void setEnabled(ItemStack stack, String featureKey, boolean enabled) {
        if (stack.isEmpty() || featureKey == null) {
            return;
        }
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        tag.putBoolean(FEATURE_PREFIX + featureKey, enabled);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }

    /**
     * 取反开关型特性的启用状态。
     *
     * @return 切换后的状态
     */
    public static boolean toggle(ItemStack stack, String featureKey) {
        boolean next = !isEnabled(stack, featureKey);
        setEnabled(stack, featureKey, next);
        return next;
    }
}
