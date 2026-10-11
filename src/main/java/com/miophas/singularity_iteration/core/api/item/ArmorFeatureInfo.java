package com.miophas.singularity_iteration.core.api.item;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EquipmentSlot;

/**
 * Record that describes one feature of an electric armor piece.
 * <p>It describes a feature that the player can toggle (for example night vision or flight).
 * The tooltip and the feature toggle API use it.
 *
 * @param slot          the equipment slot
 * @param featureKey    the feature key (used for NBT storage and toggle commands)
 * @param featureNameKey the translation key of the feature name
 * @param isMode        true for a mode switch, false for an on/off toggle
 * @param currentModeName the current mode name (valid only when isMode is true)
 */
public record ArmorFeatureInfo(EquipmentSlot slot, String featureKey, String featureNameKey, boolean isMode, Component currentModeName) {

    /**
     * Creates feature info for an on/off toggle.
     */
    public ArmorFeatureInfo(EquipmentSlot slot, String featureKey, String featureNameKey) {
        this(slot, featureKey, featureNameKey, false, null);
    }

    /**
     * Creates feature info for a mode switch.
     */
    public ArmorFeatureInfo(EquipmentSlot slot, String featureKey, String featureNameKey, Component currentModeName) {
        this(slot, featureKey, featureNameKey, true, currentModeName);
    }
}