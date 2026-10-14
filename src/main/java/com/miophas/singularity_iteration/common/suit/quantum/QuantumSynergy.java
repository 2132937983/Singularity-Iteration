// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.suit.quantum;

import com.miophas.singularity_iteration.common.suit.SuitModuleType;
import org.jetbrains.annotations.Nullable;

/**
 * The nine built-in entanglement synergies (0.1.10.1). A synergy works when the wearer has linked
 * it on the quantum chestplate ({@link QuantumLinks}) and both units are active.
 *
 * <table>
 *   <caption>Synergies</caption>
 *   <tr><th>Synergy</th><th>Units</th><th>Effect</th></tr>
 *   <tr><td>Predictive Guidance</td><td>smart guidance + behaviour predictor</td><td>guided shots turn 4 degrees more per tick; 2 more locks</td></tr>
 *   <tr><td>Threat Sight</td><td>entity ESP + threat sensor</td><td>creatures that hunt you glow red through walls with an impact countdown</td></tr>
 *   <tr><td>Ore Hologram</td><td>holomap + ore scanner</td><td>scanned ores appear as voxels in the holomap</td></tr>
 *   <tr><td>Ballistic Sync</td><td>ballistic computer + smart guidance</td><td>locks take 30 % less time; the guided paths are drawn</td></tr>
 *   <tr><td>Blast Forecast</td><td>blast warning + behaviour predictor</td><td>a 3D blast sphere and an escape arrow</td></tr>
 *   <tr><td>Resonant Deflection</td><td>deflector + resonance field</td><td>25 % of the deflected damage charges a resonance burst</td></tr>
 *   <tr><td>Afterimage Decoy</td><td>phase jump drive + behaviour predictor</td><td>a blink leaves a decoy that draws the hunters for 2 s</td></tr>
 *   <tr><td>Phase Counter</td><td>phase modulator + deflector</td><td>damage that passed through returns as a shock wave</td></tr>
 *   <tr><td>Fleet Datalink</td><td>entanglement relay + entity ESP</td><td>what the Wingmen see is marked on the HUD; they take the target in your sights</td></tr>
 * </table>
 */
public enum QuantumSynergy {
    PREDICTIVE_GUIDANCE("predictive_guidance", "PGD", SuitModuleType.SMART_GUIDANCE, SuitModuleType.BEHAVIOR_PREDICTOR, 0xFF6FA0),
    THREAT_SIGHT("threat_sight", "TSI", SuitModuleType.ENTITY_ESP, SuitModuleType.THREAT_SENSOR, 0xFF5A4C),
    ORE_HOLO("ore_holo", "OHL", SuitModuleType.HOLOMAP, SuitModuleType.ORE_SCANNER, 0x3FD4FF),
    BALLISTIC_SYNC("ballistic_sync", "BSY", SuitModuleType.BALLISTIC, SuitModuleType.SMART_GUIDANCE, 0xFFB347),
    BLAST_FORECAST("blast_forecast", "BFC", SuitModuleType.BLAST_WARNING, SuitModuleType.BEHAVIOR_PREDICTOR, 0xFF7A3C),
    RESONANT_DEFLECTION("resonant_deflection", "RDF", SuitModuleType.DEFLECTOR, SuitModuleType.RESONANCE_FIELD, 0x9B7CFF),
    AFTERIMAGE_DECOY("afterimage_decoy", "DCY", SuitModuleType.PHASE_JUMP_DRIVE, SuitModuleType.BEHAVIOR_PREDICTOR, 0xB98CFF),
    PHASE_COUNTER("phase_counter", "PCT", SuitModuleType.PHASE_MODULATOR, SuitModuleType.DEFLECTOR, 0xD06BFF),
    FLEET_DATALINK("fleet_datalink", "FDL", SuitModuleType.ENTANGLEMENT_RELAY, SuitModuleType.ENTITY_ESP, 0x6FF3D6);

    public final String id, code;
    public final SuitModuleType a, b;
    public final int color;
    public static final QuantumSynergy[] VALUES = values();

    QuantumSynergy(String id, String code, SuitModuleType a, SuitModuleType b, int color) {
        this.id = id;
        this.code = code;
        this.a = a;
        this.b = b;
        this.color = color;
    }

    public String nameKey() { return "quantum.mio_icif.synergy." + id; }
    public String descKey() { return nameKey() + ".desc"; }

    @Nullable
    public static QuantumSynergy byId(String id) {
        for (QuantumSynergy s : VALUES) if (s.id.equals(id)) return s;
        return null;
    }
}
