package com.miophas.singularity_iteration.core.prefab.item;

import com.miophas.singularity_iteration.core.api.item.ArmorFeatureInfo;
import com.miophas.singularity_iteration.core.api.item.IArmorModeFeatureItem;
import com.miophas.singularity_iteration.core.api.item.IElectricArmorItem;

import java.util.List;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * Single entry point for equipment features.
 *
 * <p>This class handles which features an item declares, their current state, and how to switch them.
 * Tooltips, the feature management GUI, commands and network packets use it.
 * All checks use the declarations from {@link IElectricArmorItem#getFeatures(ItemStack)}.
 * This class does not check for specific item classes. Thus an addon mod only needs to implement the interface to get full support.
 *
 * <p>Semantics:
 * <ul>
 *   <li>Toggle feature ({@code isMode=false}): {@link ArmorFeatureState} stores the state. The default is enabled.</li>
 *   <li>Mode feature ({@code isMode=true}): {@link IArmorModeFeatureItem#cycleMode} switches the mode.
 *       {@link IElectricArmorItem#getFeatures(ItemStack)} gives the name of the current mode.</li>
 * </ul>
 */
public final class ArmorFeatures {

    private ArmorFeatures() {
    }

    /**
     * Gets all equipment features that the item declares, plus the features from registered extensions.
     * Returns an empty list for an empty stack.
     */
    public static List<ArmorFeatureInfo> features(ItemStack stack) {
        if (stack.isEmpty()) return List.of();
        List<ArmorFeatureInfo> base;
        if (stack.getItem() instanceof com.miophas.singularity_iteration.core.api.item.IFeatureToolItem tool) base = tool.getFeatures(stack);
        else if (stack.getItem() instanceof IElectricArmorItem armor) base = armor.getFeatures(stack);
        else base = List.of();
        if (EXTENSIONS.isEmpty()) return base;
        List<ArmorFeatureInfo> out = null;
        for (Extension extension : EXTENSIONS) {
            List<ArmorFeatureInfo> extra = extension.extraFeatures(stack);
            if (extra == null || extra.isEmpty()) continue;
            if (out == null) out = new java.util.ArrayList<>(base);
            out.addAll(extra);
        }
        return out == null ? base : List.copyOf(out);
    }

    /**
     * Extra features that do not come from the item class, e.g. installed upgrade units.
     * Registered once during mod construction.
     */
    @FunctionalInterface
    public interface Extension {
        List<ArmorFeatureInfo> extraFeatures(ItemStack stack);
    }

    private static final List<Extension> EXTENSIONS = new java.util.concurrent.CopyOnWriteArrayList<>();

    public static void registerExtension(Extension extension) {
        EXTENSIONS.add(java.util.Objects.requireNonNull(extension));
    }

    /**
     * Finds the declaration of the given feature key.
     *
     * @return the declaration, or {@code null} if the feature is not declared
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
     * Checks if the item declares the given feature (with an optional slot filter).
     *
     * @param slot the slot filter. If {@code null}, all slots match.
     */
    public static boolean isDeclared(ItemStack stack, @Nullable EquipmentSlot slot, String featureKey) {
        ArmorFeatureInfo info = find(stack, featureKey);
        return info != null && (slot == null || info.slot() == slot);
    }

    /**
     * Checks if the item declares the given <b>toggle</b> feature.
     */
    public static boolean isToggleFeature(ItemStack stack, @Nullable EquipmentSlot slot, String featureKey) {
        ArmorFeatureInfo info = find(stack, featureKey);
        return info != null && !info.isMode() && (slot == null || info.slot() == slot);
    }

    /**
     * Checks if the item declares the given <b>mode</b> feature.
     */
    public static boolean isModeFeature(ItemStack stack, @Nullable EquipmentSlot slot, String featureKey) {
        ArmorFeatureInfo info = find(stack, featureKey);
        return info != null && info.isMode() && (slot == null || info.slot() == slot);
    }

    /**
     * Reads the enabled state of a toggle feature.
     * For a mode feature or an undeclared feature, the result is always {@code true}.
     */
    public static boolean isEnabled(ItemStack stack, String featureKey) {
        ArmorFeatureInfo info = find(stack, featureKey);
        if (info == null || info.isMode()) {
            return true;
        }
        return ArmorFeatureState.isEnabled(stack, featureKey);
    }

    /**
     * Sets the enabled state of a toggle feature.
     * For other features, this method does nothing.
     */
    public static void setEnabled(ItemStack stack, String featureKey, boolean enabled) {
        if (isToggleFeature(stack, null, featureKey)) {
            ArmorFeatureState.setEnabled(stack, featureKey, enabled);
        }
    }

    /**
     * Toggles a toggle feature.
     *
     * @return the state after the toggle. For other features, the current state.
     */
    public static boolean toggle(ItemStack stack, String featureKey) {
        if (!isToggleFeature(stack, null, featureKey)) {
            return isEnabled(stack, featureKey);
        }
        return ArmorFeatureState.toggle(stack, featureKey);
    }

    /**
     * Switches a mode feature to the next mode.
     *
     * @return true if the mode changed
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
     * Switches the feature according to its declared type.
     * A toggle feature changes to the opposite state. A mode feature goes to the next mode.
     *
     * @return true if a change occurred
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
     * Gets the translation key of the feature name.
     *
     * @return the translation key, or {@code null} if the feature is not declared
     */
    @Nullable
    public static String nameKey(ItemStack stack, String featureKey) {
        ArmorFeatureInfo info = find(stack, featureKey);
        return info == null ? null : info.featureNameKey();
    }
}
