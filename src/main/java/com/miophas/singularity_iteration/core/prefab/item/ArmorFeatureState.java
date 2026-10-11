package com.miophas.singularity_iteration.core.prefab.item;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

/**
 * State storage for equipment features (on/off type).
 *
 * <p>The state is stored in the {@code CustomData} component of the item. The key is {@code feat_<featureKey>}.
 * This implementation does not depend on specific item classes or registry contents. Thus core, common and add-on mods can share the same storage rules.
 *
 * <p><b>Default rule: a feature that has no written value is "enabled"</b> (new items are on by default). {@link #registerDefault} can change the default of one feature.
 */
public final class ArmorFeatureState {

    /** Feature key prefix. */
    public static final String FEATURE_PREFIX = "feat_";

    /** Default enabled state when no value is written. */
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

    private static final java.util.Map<String, String> KEYS = new java.util.concurrent.ConcurrentHashMap<>();

    /** "feat_" + key, built once per feature (checks run every tick and every frame). */
    static String storageKey(String featureKey) {
        return KEYS.computeIfAbsent(featureKey, k -> FEATURE_PREFIX + k);
    }

    /**
     * Reads the enabled state of an on/off feature.
     *
     * @param stack      the item stack
     * @param featureKey the feature key
     * @return true if enabled. For an empty stack, a null key or no written value, it returns {@link #defaultFor(String)}.
     */
    public static boolean isEnabled(ItemStack stack, String featureKey) {
        if (stack.isEmpty() || featureKey == null) {
            return defaultFor(featureKey);
        }
        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        if (customData == null || customData.isEmpty()) {
            return defaultFor(featureKey);
        }
        String key = storageKey(featureKey);
        // CustomData#contains / #read avoid copying the whole tag on every check (HUD code asks each frame)
        CompoundTag tag = customData.getUnsafe();
        return tag.contains(key) ? tag.getBoolean(key) : defaultFor(featureKey);
    }

    /**
     * Writes the enabled state of an on/off feature. An empty stack or a null key does nothing.
     *
     * @param stack      the item stack
     * @param featureKey the feature key
     * @param enabled    true to enable
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
     * Inverts the enabled state of an on/off feature.
     *
     * @return the state after the change
     */
    public static boolean toggle(ItemStack stack, String featureKey) {
        boolean next = !isEnabled(stack, featureKey);
        setEnabled(stack, featureKey, next);
        return next;
    }
}
