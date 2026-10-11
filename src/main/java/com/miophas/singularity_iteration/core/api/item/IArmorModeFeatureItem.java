package com.miophas.singularity_iteration.core.api.item;

import net.minecraft.world.item.ItemStack;

/**
 * Interface for armor features that have modes.
 *
 * <p>When {@link ArmorFeatureInfo#isMode()} is {@code true}, a "current mode" describes the feature.
 * This interface changes the feature to the next mode (for example, the hover / flight toggle of a jetpack).
 *
 * <p>How to declare it:
 * <ul>
 *   <li>the item implements {@link IElectricArmorItem#getFeatures(ItemStack)} and returns
 *       an {@link ArmorFeatureInfo} with {@code isMode=true} (it holds the current mode name);</li>
 *   <li>the item also implements this interface to supply the toggle logic.</li>
 * </ul>
 *
 * <p>{@link IJetpackItem} already extends this interface and supplies a default implementation. Thus a jetpack needs no more code.
 * The GUI, commands and tooltip panels all use this interface to change modes. They no longer check for specific item types.
 */
public interface IArmorModeFeatureItem {

    /**
     * Changes the given mode feature to the next mode.
     *
     * <p>The implementation must change the mode state of {@code stack} in place. After this call,
     * {@link IElectricArmorItem#getFeatures(ItemStack)} must show the new mode name.
     *
     * @param stack      the item stack
     * @param featureKey the key of the mode feature
     * @return true if the mode changed
     */
    boolean cycleMode(ItemStack stack, String featureKey);
}
