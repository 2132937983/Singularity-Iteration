// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.core.runtime.reactor;

/**
 * 0.1.10.36: one cycle of a steam reactor (IC2 Classic's steam reactor, re-done on SI's 20-tick reactor cycle). The rods heat
 * as in a fluid reactor (no EU); the steam vents turn heat into steam, as much as the water and the room in the steam tank
 * allow; ordinary vents still vent their heat. Rates follow SI's existing heat economy: one unit of vent heat is
 * {@link FluidReactorCycle#HU_OUTPUT_MODIFIER} HU, and 1 HU makes 1 mB of steam from 1/100 mB of water (as the Steam Generator:
 * 100 HU and 1 mB of water make 100 mB of steam).
 */
public final class SteamReactorCycle {
    private SteamReactorCycle() {}

    /** mB of steam made from 1 mB of water. */
    public static final int STEAM_PER_WATER = 100;

    /** {@code waterUsed} mB of water became {@code steamMade} mB of steam. */
    public record Result(ReactorCycle.Result cycle, int waterUsed, int steamMade) { }

    /** Units of vent heat that {@code water} mB of water and {@code steamRoom} mB of free steam tank can take this cycle. */
    public static long budget(int water, int steamRoom) {
        if (water < 0 || steamRoom < 0) throw new IllegalArgumentException("Invalid steam reactor tanks");
        long steam = Math.min(steamRoom, (long) water * STEAM_PER_WATER);
        return steam / FluidReactorCycle.HU_OUTPUT_MODIFIER;
    }

    public static Result step(ReactorCycle.Part[] parts, int columns, long heat, boolean enabled, int water, int steamRoom) {
        var cycle = ReactorCycle.step(parts, columns, heat, enabled, true, budget(water, steamRoom));
        long steam = Math.multiplyExact(cycle.steamHeat(), FluidReactorCycle.HU_OUTPUT_MODIFIER);
        int steamMade = (int) Math.min(steam, steamRoom);
        int waterUsed = (int) Math.min(water, (steamMade + STEAM_PER_WATER - 1) / STEAM_PER_WATER);
        return new Result(cycle, waterUsed, steamMade);
    }
}
