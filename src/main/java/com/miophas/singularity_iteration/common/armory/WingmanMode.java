// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.armory;

/**
 * Tactical modes of a Wingman (0.1.8.0). The owner sets the mode for all Wingmen at once in
 * the Armory section of the Singularity Management Panel.
 */
public enum WingmanMode {
    /** Attacks the target of the owner, else the nearest hostile mob near the owner. */
    OFFENSIVE,
    /**
     * Each Wingman holds one sector of a wide ring around the owner, attacks the hostile mobs in
     * its sector and pulls the mobs that target the owner (decoy).
     */
    DISPERSAL,
    /**
     * Stays close behind the owner, destroys projectiles that fly at the owner and attacks the
     * mobs that hurt the owner.
     */
    ESCORT,
    /**
     * Fulton recovery: takes the marked mob (the last mob that the owner hit, or the mob that the
     * owner looks at while crouching) up and to the paired Armory.
     */
    FULTON;

    public static final WingmanMode[] VALUES = values();

    public static WingmanMode byId(int id) {
        return VALUES[Math.floorMod(id, VALUES.length)];
    }

    public String key() {
        return "gui.mio_icif.wingman.mode." + name().toLowerCase(java.util.Locale.ROOT);
    }
}
