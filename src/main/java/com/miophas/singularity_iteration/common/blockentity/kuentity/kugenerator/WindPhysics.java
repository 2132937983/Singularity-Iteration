// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.blockentity.kuentity.kugenerator;

import net.minecraft.core.Direction;

/**
 * 0.1.11.9: the physics of the wind machines, shared by the machines, the screens and the tests.
 *
 * <ul>
 *   <li><b>Bearing.</b> The wind field's direction is the compass bearing the wind blows <i>from</i>: 0 north, 90 east,
 *       180 south, 270 west. A rotor's bearing is its facing (north 0, east 90, ...).</li>
 *   <li><b>Facing factor</b> {@code 0.7 + 0.3 |cos(error)|}: a rotor square to the wind (front or back) takes all of it,
 *       one side-on takes 70 %. A rotor facing up or down always takes 70 %.</li>
 *   <li><b>Wake</b> (Jensen's model, used to lay out real wind farms): behind a rotor of radius r the wind is short by
 *       {@code (2/3) / (1 + 0.075 x / r)^2} at distance x; 50 % right behind it, 32 % three diameters on.</li>
 *   <li><b>Power curve</b> of a rotor: nothing below its minimum wind (cut-in), {@code wind x efficiency x 10} KU/t up to its
 *       maximum (cut-out), as before. Above cut-out a rotor with storm protection feathers (stops, takes no wear); one
 *       without keeps turning and wears four times as fast, as before.</li>
 * </ul>
 */
public final class WindPhysics {
    private WindPhysics() {}

    public static final double CROSSWIND_FACTOR = 0.7;
    public static final float OUTPUT_MODIFIER = 10.0F;

    /** The compass bearing of a facing, or -1 for up and down. */
    public static int bearing(Direction facing) {
        return switch (facing) {
            case NORTH -> 0;
            case EAST -> 90;
            case SOUTH -> 180;
            case WEST -> 270;
            default -> -1;
        };
    }

    /** Signed error in degrees (-180 .. 180) between the wind's bearing and a rotor's bearing. */
    public static int yawError(int windBearing, int rotorBearing) {
        if (rotorBearing < 0) return 90;
        return Math.floorMod(windBearing - rotorBearing + 180, 360) - 180;
    }

    public static double facingFactor(int windBearing, int rotorBearing) {
        if (rotorBearing < 0) return CROSSWIND_FACTOR;
        double err = Math.toRadians(yawError(windBearing, rotorBearing));
        return CROSSWIND_FACTOR + (1 - CROSSWIND_FACTOR) * Math.abs(Math.cos(err));
    }

    /** Jensen wake deficit at {@code distance} blocks behind a rotor of {@code diameter} blocks (0 .. 2/3). */
    public static double wakeDeficit(double distance, double diameter) {
        double r = Math.max(0.5, diameter / 2.0);
        double d = 1 + 0.075 * Math.max(0, distance) / r;
        return (2.0 / 3.0) / (d * d);
    }

    /** Status of a rotor in a wind: weak, generating, feathered (storm protection) or overloaded. */
    public enum State { WEAK, GENERATING, FEATHERED, OVERLOAD }

    public static State state(double wind, int minWind, int maxWind, boolean stormProtection) {
        if (wind < minWind) return State.WEAK;
        if (wind <= maxWind) return State.GENERATING;
        return stormProtection ? State.FEATHERED : State.OVERLOAD;
    }

    /** KU/t of a rotor in a wind (the power curve above). */
    public static int output(double wind, int minWind, int maxWind, float efficiency, boolean stormProtection) {
        return switch (state(wind, minWind, maxWind, stormProtection)) {
            case GENERATING, OVERLOAD -> Math.max(0, (int) (wind * efficiency * OUTPUT_MODIFIER));
            default -> 0;
        };
    }

    /** The compass point (N, NE, ...) of a bearing, as a translation key suffix. */
    public static String point(int bearing) {
        String[] p = {"n", "ne", "e", "se", "s", "sw", "w", "nw"};
        return p[Math.floorMod(Math.round(bearing / 45F), 8)];
    }
}
