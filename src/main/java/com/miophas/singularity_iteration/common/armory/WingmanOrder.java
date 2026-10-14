// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.armory;

/**
 * Orders of the quantum command (0.1.10.2). An order overrides the tactical mode of a Wingman
 * until the owner gives "form up". An anchored order (move, hold, suppress) frees the Wingman from
 * the owner leash: it guards its order point instead, at any distance in the owner's dimension.
 */
public enum WingmanOrder {
    /** No order: the tactical mode applies (the Wingman flies with the owner). */
    NONE,
    /** Fly to the point, then hold it: engage the hostile creatures that come near the point. */
    MOVE,
    /** Hold the point (or the place where the order found the Wingman) and engage what comes near. */
    HOLD,
    /** Patrol a ring above the point and engage every hostile creature in the area. */
    SUPPRESS,
    /** Attack one creature until it is down, wherever it goes in the dimension; then form up. */
    FOCUS;

    public static final WingmanOrder[] VALUES = values();

    public static WingmanOrder byId(int id) {
        return id >= 0 && id < VALUES.length ? VALUES[id] : NONE;
    }

    /** True for the orders that keep the Wingman at a point. */
    public boolean anchored() {
        return this == MOVE || this == HOLD || this == SUPPRESS;
    }

    public String key() {
        return "gui.mio_icif.wingman.order." + name().toLowerCase(java.util.Locale.ROOT);
    }
}
