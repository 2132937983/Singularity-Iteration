// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.energy;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Candidate SI extended-solar configuration. ASP steady generation is backed by
 * R30 public-game observations and generated configuration. Packet/charging
 * behavior and the MET settings still require reference/runtime acceptance.
 */
public record SolarGeneratorProfile(String registryId, boolean discrete,
        int dayPower, int nightPower, long capacity, long outputPacket,
        int chargeSlots, int refreshTicks, float minimumBrightness) {
    public SolarGeneratorProfile {
        if (registryId == null || registryId.isBlank() || dayPower < 0 || nightPower < 0
                || capacity <= 0 || outputPacket <= 0 || chargeSlots <= 0 || refreshTicks <= 0
                || !Float.isFinite(minimumBrightness) || minimumBrightness < 0 || minimumBrightness > 1)
            throw new IllegalArgumentException("Invalid solar profile");
    }

    private static final List<SolarGeneratorProfile> ALL = List.of(
        asp("block_advanced_solar_panel", 8, 1, 32_000, 32),
        asp("block_hybrid_solar_panel", 64, 8, 100_000, 128),
        asp("block_ultimate_hybrid_solar_panel", 512, 64, 1_000_000, 512),
        asp("block_quantum_solar_panel", 4096, 2048, 10_000_000, 8192),
        mets("block_mets_advanced_solar_generator", 64, 200_000, 128, 128, .1F),
        mets("block_photon_resonance_solar_generator", 512, 40_000_000, 2048, 128, .1F),
        mets("block_ultimate_photon_resonance_solar_generator", 4096, 400_000_000, 8192, 64, .125F));
    private static final Map<String, SolarGeneratorProfile> BY_ID = ALL.stream()
        .collect(Collectors.toUnmodifiableMap(SolarGeneratorProfile::registryId, Function.identity()));

    private static SolarGeneratorProfile asp(String path, int day, int night, long capacity, long packet) {
        // One-tick environment refresh is a candidate choice, not an observed ASP cadence.
        return new SolarGeneratorProfile("mio_icif:generator/" + path, true, day, night, capacity, packet, 4, 1, 0);
    }
    private static SolarGeneratorProfile mets(String path, int day, long capacity, long packet, int refresh, float minimum) {
        return new SolarGeneratorProfile("mio_icif:generator/" + path, false, day, 0, capacity, packet, 4, refresh, minimum);
    }
    public static List<SolarGeneratorProfile> all() { return ALL; }
    public static Optional<SolarGeneratorProfile> find(String registryId) { return Optional.ofNullable(BY_ID.get(registryId)); }

    /** Environment predicates come from the public world adapter, not an IC2 API. */
    public long discreteOutput(boolean clearSky, boolean daytime, boolean wet) {
        if (!discrete) throw new IllegalStateException("MET uses brightness-scaled generation");
        return clearSky ? daytime && !wet ? dayPower : nightPower : 0;
    }
    public long scaledOutput(float brightness) {
        if (discrete) throw new IllegalStateException("ASP uses day/night generation");
        return (long) (dayPower * safeBrightness(brightness));
    }
    public static float safeBrightness(float value) {
        return Float.isFinite(value) ? Math.max(0, Math.min(1, value)) : 0;
    }
}
