package com.miophas.singularity_iteration.core.api.upgrade.tile;

/**
 * Upgradable property.
 * <p>
 * This is the equivalent of {@code UpgradableProperty} in IC2 for 1.12.2.
 * It defines the upgrade property types that a machine can support.
 */
public enum UpgradableProperty {
    /**
     * Processing speed upgrade (overclocker).
     */
    PROCESSING,

    /**
     * The machine accepts augment modules (augment slot).
     */
    AUGMENTABLE,

    /**
     * Redstone signal control (redstone signal inversion).
     */
    REDSTONE_SENSITIVE,

    /**
     * Transformer upgrade (increases the input voltage tier).
     */
    TRANSFORMER,

    /**
     * Energy storage upgrade (increases the energy capacity).
     */
    ENERGY_STORAGE,

    /**
     * Item consuming (the machine uses items).
     */
    ITEM_CONSUMING,

    /**
     * Item producing (the machine makes items).
     */
    ITEM_PRODUCING,

    /**
     * Fluid consuming (the machine uses fluids).
     */
    FLUID_CONSUMING,

    /**
     * Fluid producing (the machine makes fluids).
     */
    FLUID_PRODUCING,

    /**
     * Remotely accessible (remote tools can access the machine).
     */
    REMOTELY_ACCESSIBLE
}