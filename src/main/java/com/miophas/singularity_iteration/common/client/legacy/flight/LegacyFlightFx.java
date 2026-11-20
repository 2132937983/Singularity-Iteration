// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.client.legacy.flight;

/**
 * 0.1.11.124 (armour and flight overhaul E1): the archive of the reaction-mass flight effects. The quantum suit now flies
 * electromagnetically (no flames, smoke, gas jets or jet ignition), so everything that drew exhaust is kept here as source
 * but never runs: {@link #ENABLED} is a compile-time {@code false} and there is no option that turns it on.
 *
 * <p>Archived in this package:
 * <ul>
 *   <li>{@link SuitThrustLayer}: the jets of the quantum chestplate's jetpack flight, the boot burst of a boost jump and the
 *       first-person palm light. Its layer is no longer registered.</li>
 *   <li>{@link ThrusterFlame} and {@link FlameRules}: the classic flame (glow cone, animated strip, sparks).</li>
 *   <li>{@link LegacyManeuverJets}: the special flight's nozzle drawing from before E1: the classic flames, shock diamonds and
 *       deflection shell, and the phase warp style's cones of light, nozzle flares, boost rings and RCS blinks.</li>
 * </ul>
 * Switched off where they stay (their code behind this flag or behind {@code FcsClientConfig.classicFlightFx()}, which now
 * returns this flag): the classic wake ribbons, vapour cone, Mach shell, shock rings and condensation puffs of
 * {@code ManeuverModeClient}; the take-off and landing dust and clouds; the heat haze, sparks and dust of
 * {@code nano.ThrusterFx}; the Wingmen's thruster cones, nozzle flares and attitude jets of {@code WingmanFx}; the
 * ignition and shutdown cues of the quantum chestplate's jetpack sound. The options {@code classic_flight_fx} and
 * {@code thruster_fx} were removed (a value left in a config file is ignored).
 *
 * <p>Kept, as they carry no reaction mass: the light trails, the star streams, the warp at the sound barrier and its screen
 * effect, the light rings of take-off and landing, the impact cracks, the graviton field rings.
 */
public final class LegacyFlightFx {
    private LegacyFlightFx() {}

    /** The archived reaction-mass flight effects run only when this is true. It is not an option. */
    public static final boolean ENABLED = false;
}
