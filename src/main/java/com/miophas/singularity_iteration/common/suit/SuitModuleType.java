// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.suit;

import com.miophas.singularity_iteration.core.api.suit.SuitBurstSpec;
import com.miophas.singularity_iteration.core.api.suit.SuitClass;
import com.miophas.singularity_iteration.core.api.suit.SuitModuleTraits;
import java.util.EnumSet;
import java.util.Locale;
import java.util.Set;
import net.minecraft.world.entity.EquipmentSlot;
import org.jetbrains.annotations.Nullable;

/**
 * Upgrade units of the quantum suit (0.1.7.33) and, since 0.1.8.5, of the nano suit. Each unit is
 * installed into one suit piece in the Universal Suit Modification Station and draws power from
 * that piece while its feature switch is on.
 *
 * <p>Balance notes. A quantum piece stores 10 000 000 EU. The full sensor load of a helmet
 * (ESP + ballistic + predictor + holomap = 34 EU/t) empties a full helmet in about 4 hours of
 * continuous play; the solar helmets cover it in daylight. The ore scanner is the most expensive
 * sensor, because it gives the strongest advantage.
 *
 * <p>Nano units (0.1.8.5) are light: a fully loaded nano suit draws about 10 EU/t. Their strong
 * effects are bursts, paid from the nano reserve and limited by heat ({@link NanoBurstDefaults}).
 * A nano piece counts lattice points (LP) instead of units.
 */
public enum SuitModuleType {
    /** Seismic array in the boots: highlights ores around the wearer. */
    ORE_SCANNER("ore_scanner", 24, 0x3FD4FF, true, EquipmentSlot.FEET, EquipmentSlot.LEGS),
    /** Induction sensor: voltage tier and flow of the grid in view and in the current chunk. */
    GRID_TELEMETRY("grid_telemetry", 4, 0xFFD23F, true, EquipmentSlot.LEGS, EquipmentSlot.HEAD),
    /** Biometric scan: outlines and tags every living entity in range. */
    ENTITY_ESP("entity_esp", 12, 0x7CFFB2, true, EquipmentSlot.HEAD),
    /** Fire-control computer: projectile path, impact point and lead marker. */
    BALLISTIC("ballistic", 4, 0xFF9F1C, true, EquipmentSlot.HEAD),
    /** Fuse timer: countdown of primed TNT, TNT carts and swelling creepers. Shared with the nano suit (1 LP). */
    BLAST_WARNING("blast_warning", 2, 0xFF5A36, true, SuitModuleTraits.shared(1), EquipmentSlot.CHEST, EquipmentSlot.HEAD),
    /** Motion model: predicted paths and attack wind-ups of nearby creatures. */
    BEHAVIOR_PREDICTOR("behavior_predictor", 8, 0xC77DFF, true, EquipmentSlot.HEAD),
    /** Tactical 3D holomap of the terrain and contacts around the wearer. */
    HOLOMAP("holomap", 10, 0x4DE1C1, true, EquipmentSlot.HEAD),
    /** Threat sensor: who looks at or targets the wearer (yellow / red). Shared with the nano suit (1 LP). */
    THREAT_SENSOR("threat_sensor", 6, 0xFF3B30, true, SuitModuleTraits.shared(1), EquipmentSlot.CHEST),
    /** Deflector: spends EU to cancel the damage the armour lets through. */
    DEFLECTOR("deflector", 0, 0x5AB8FF, false, EquipmentSlot.CHEST),
    /**
     * Smart Trajectory Guidance (0.1.8.0): a lock frame on the HUD; locked creatures pull the
     * wearer's projectiles (vernier thrusters). Each guided projectile costs
     * {@link SmartGuidance#GUIDED_EU} EU in addition to the drain.
     */
    SMART_GUIDANCE("smart_guidance", 6, 0xFF4D6D, true, EquipmentSlot.HEAD),
    /**
     * Resonance Deconstruction Field (0.1.8.0): a standing-wave field at the feet breaks the
     * blocks around the body while the wearer walks and pulls the drops in. Each block costs
     * EU in proportion to its hardness ({@link ResonanceField}).
     */
    RESONANCE_FIELD("resonance_field", 2, 0x9B7CFF, false, EquipmentSlot.FEET, EquipmentSlot.LEGS),

    // ---------------------------------------------------------------- nano units (0.1.8.5)

    /**
     * Nano myofiber (legs, 2 LP, 3 EU/t): sprint +15 %, steps up one block. Burst: Kinetic Dash, a
     * horizontal dash of about 8 blocks. With an active nanosaber in hand: HF sync (attack speed,
     * light armour penetration).
     */
    MYOFIBER("myofiber", 3, 0x3DFFB4, false, SuitModuleTraits.nano(2, NanoBurstDefaults.KINETIC_DASH), EquipmentSlot.LEGS),
    /** Thermoregulating membrane (chest, 2 LP, 2 EU/t): no freezing, fire -50 %, lava -30 %, heat sheds twice as fast. */
    THERMOREGULATOR("thermoregulator", 2, 0xFF8A3D, false, SuitModuleTraits.nano(2, null), EquipmentSlot.CHEST),
    /**
     * EMP emitter (chest, 3 LP). Burst: a 6-block pulse that erases hostile projectiles in flight,
     * stops creeper fuses, stuns mechanical creatures and breaks guidance locks on the wearer.
     */
    EMP_EMITTER("emp_emitter", 0, 0x6FA8FF, false, SuitModuleTraits.nano(3, NanoBurstDefaults.EMP), EquipmentSlot.CHEST),
    /** Optical camouflage (chest, 3 LP). Sustained burst: up to 15 s unseen; attacking, a hit or an EMP ends it. */
    OPTICAL_CAMO("optical_camo", 0, 0xBFF6FF, false, SuitModuleTraits.nano(3, NanoBurstDefaults.OPTICAL_CAMO), EquipmentSlot.CHEST),
    /** Toxin and radiation filter (head, 1 LP, 1 EU/t): no poison or wither, radiation -60 %. Shared with the quantum suit. */
    TOXIN_FILTER("toxin_filter", 1, 0x9CFF57, false, SuitModuleTraits.shared(1), EquipmentSlot.HEAD),
    /** Micro-capacitor lattice (any piece, 1 LP): +250 000 EU storage in that piece. Passive hardware, no switch. */
    MICRO_CAPACITOR("micro_capacitor", 0, 0xFFD45A, false, SuitModuleTraits.nano(1, null),
        EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET),

    // ---------------------------------------------------------------- nano units, phase 2 (0.1.8.6)

    /** Gecko setae (boots, 2 LP, 1 EU/t): climb walls while sneaking; no slow-down on soul sand, honey or powder snow; grip on ice. */
    GECKO_SETAE("gecko_setae", 1, 0x9BE15D, false, SuitModuleTraits.nano(2, null), EquipmentSlot.FEET),
    /** Shock gel (boots, 1 LP): the fall protection of the nano boots costs 70 % less; a jump right after a landing rebounds higher. */
    SHOCK_GEL("shock_gel", 0, 0x4FC3F7, false, SuitModuleTraits.nano(1, null), EquipmentSlot.FEET),
    /** Heat-sink fins (chest or legs, 1 LP): heat cap +50; an overheat no longer slows the wearer. */
    HEAT_SINK_FINS("heat_sink_fins", 0, 0xFF6E40, false, SuitModuleTraits.nano(1, null), EquipmentSlot.CHEST, EquipmentSlot.LEGS),
    /** Thermal camouflage (chest, 2 LP, 2 EU/t): while sneaking, creatures notice the wearer at half the distance. */
    THERMAL_CAMO("thermal_camo", 2, 0x9575CD, false, SuitModuleTraits.nano(2, null), EquipmentSlot.CHEST),
    /** Jammer (helmet, 2 LP, 4 EU/t): hostile creatures beyond 12 blocks lose track; guided weapons cannot lock the wearer. */
    JAMMER("jammer", 4, 0xE040FB, false, SuitModuleTraits.nano(2, null), EquipmentSlot.HEAD),
    /** Nano recon cloud (helmet, 2 LP). Burst: creatures within 24 blocks ahead glow for 10 s (seen by the wearer only). */
    RECON_CLOUD("recon_cloud", 0, 0x64FFDA, false, SuitModuleTraits.nano(2, NanoBurstDefaults.RECON_CLOUD), EquipmentSlot.HEAD),
    /** Kinetic armor mesh (chest, 2 LP). Burst: 6 s of a further 30 % damage cut (total absorption at most 95 %). Also an HF sync source. */
    KINETIC_MESH("kinetic_mesh", 0, 0xFFB300, false, SuitModuleTraits.nano(2, NanoBurstDefaults.KINETIC_MESH), EquipmentSlot.CHEST),
    /** Self-repair coat (any piece, 1 LP, shared): out of combat for 5 s, the wearer heals 0.5 health every 3 s (2 000 EU each). */
    SELF_REPAIR("self_repair", 0, 0x69F0AE, false, SuitModuleTraits.shared(1),
        EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET),
    /** Hydrophobic coat (chest or legs, 1 LP, shared): no water drag, swimming +30 %. */
    HYDROPHOBIC("hydrophobic", 0, 0x40C4FF, false, SuitModuleTraits.shared(1), EquipmentSlot.CHEST, EquipmentSlot.LEGS),
    /** Kinetic generator (legs, 1 LP, shared): 1 EU per block walked, 2 EU per block sprinted. Works with an empty suit. */
    KINETIC_GENERATOR("kinetic_generator", 0, 0xFFEE58, false, SuitModuleTraits.shared(1), EquipmentSlot.LEGS),
    /** Solar skin (helmet or chest, 1 LP): 2 EU/t under open daylight. Works with an empty suit. */
    SOLAR_SKIN("solar_skin", 0, 0xFFD740, false, SuitModuleTraits.nano(1, null), EquipmentSlot.HEAD, EquipmentSlot.CHEST),

    // ---------------------------------------------------------------- quantum action units (0.1.10.0)

    /** Phase jump drive (leggings or boots). Action: Quantum Blink, a jump to the point in view (12 blocks; Mk.II 20 blocks, two charges). */
    PHASE_JUMP_DRIVE("phase_jump_drive", 0, 0x8F7CFF, false, EquipmentSlot.LEGS, EquipmentSlot.FEET),
    /** Phase modulator (chestplate). Action: Phase Shift, 1 s out of phase (Mk.II 1.5 s and a shock wave when it ends). */
    PHASE_MODULATOR("phase_modulator", 0, 0xD06BFF, false, EquipmentSlot.CHEST),
    /** Coherence capacitor (leggings or boots): coherence maximum +50, regeneration +25 %. Passive. */
    COHERENCE_CAPACITOR("coherence_capacitor", 0, 0x5CE1E6, false, EquipmentSlot.LEGS, EquipmentSlot.FEET),
    /** Entanglement relay (chestplate or leggings). Action: Entangled Volley, every Wingman hits the lock targets at the same moment. */
    ENTANGLEMENT_RELAY("entanglement_relay", 0, 0x6FF3D6, false, EquipmentSlot.CHEST, EquipmentSlot.LEGS),

    // ---------------------------------------------------------------- quantum phase 2 (0.1.10.1)

    /** Compute core (chestplate or leggings, 6 EU/t): compute bandwidth +8 QB and one more entanglement link. */
    COMPUTE_CORE("compute_core", 6, 0x7FD8FF, false, EquipmentSlot.CHEST, EquipmentSlot.LEGS);

    private final String id;
    private final long drainPerTick;
    private final int color;
    private final boolean needsVisor;
    private final SuitModuleTraits traits;
    private final Set<EquipmentSlot> slots;
    /** The same slots as an array: the per-tick checks iterate it without an iterator object. */
    private final EquipmentSlot[] slotArray;
    private final String featureKey, nameKey, descriptionKey;
    /** values() copies the array on each call; the tick paths use this shared copy (do not modify). */
    public static final SuitModuleType[] VALUES = values();

    SuitModuleType(String id, long drainPerTick, int color, boolean needsVisor, EquipmentSlot first, EquipmentSlot... rest) {
        this(id, drainPerTick, color, needsVisor, SuitModuleTraits.QUANTUM_ONLY, first, rest);
    }

    SuitModuleType(String id, long drainPerTick, int color, boolean needsVisor, SuitModuleTraits traits, EquipmentSlot first, EquipmentSlot... rest) {
        this.id = id;
        this.drainPerTick = drainPerTick;
        this.color = color;
        this.needsVisor = needsVisor;
        this.traits = traits;
        this.slots = EnumSet.of(first, rest);
        this.slotArray = this.slots.toArray(new EquipmentSlot[0]);
        this.featureKey = "fcs_" + id;
        this.nameKey = "module.mio_icif." + id;
        this.descriptionKey = "module.mio_icif." + id + ".desc";
    }

    public String id() { return id; }
    /** Continuous draw while the unit is on. */
    public long drainPerTick() { return drainPerTick; }
    /** Signature colour: item tint, HUD accents, station list. */
    public int color() { return color; }
    /** A HUD unit shows its data only through a suit visor (quantum helmet; nano helmet for shared units). */
    public boolean needsVisor() { return needsVisor; }
    public Set<EquipmentSlot> slots() { return slots; }
    public boolean fits(EquipmentSlot slot) { return slots.contains(slot); }
    /** Equipment console switch key. */
    public String featureKey() { return featureKey; }
    public String nameKey() { return nameKey; }
    public String descriptionKey() { return descriptionKey; }
    EquipmentSlot[] slotArray() { return slotArray; }

    /** 0.1.8.5: suit classes, lattice points and burst. */
    public SuitModuleTraits traits() { return traits; }
    /** Lattice points on a nano piece. */
    public int latticePoints() { return traits.latticePoints(); }
    public boolean allows(SuitClass suitClass) { return traits.allows(suitClass); }
    public boolean nanoOnly() { return traits.allows(SuitClass.NANO) && !traits.allows(SuitClass.QUANTUM); }
    @Nullable public SuitBurstSpec burst() { return traits.burst(); }
    /** False for passive hardware that has no panel switch (the micro-capacitor). */
    public boolean switchable() { return this != MICRO_CAPACITOR; }
    /** False for units that work without suit energy (the generators and the capacitor). */
    public boolean needsPower() { return this != MICRO_CAPACITOR && this != SOLAR_SKIN && this != KINETIC_GENERATOR; }
    /**
     * 0.1.8.6: the unit has a Mk.II item (stronger effect, -20 % drain). Every unit made for the
     * nano lattice has one; the quantum sensors shared with the nano suit do not.
     */
    public boolean hasMk2() { return allows(SuitClass.NANO) && this != BLAST_WARNING && this != THREAT_SENSOR || quantumMk2(); }

    /**
     * 0.1.10.0: the quantum units with a Mk.II item. The deflector (force field action), the behaviour
     * predictor (time dilation action) and the two movement action units; Mk.I keeps its old effect.
     * 0.1.10.1: also the ore scanner, grid telemetry, entity ESP, ballistic computer, smart guidance
     * and resonance field. 0.1.10.2: the holomap (the Mk.II projects the 3D holo table).
     */
    public boolean quantumMk2() {
        return switch (this) {
            case DEFLECTOR, BEHAVIOR_PREDICTOR, PHASE_JUMP_DRIVE, PHASE_MODULATOR,
                 ORE_SCANNER, GRID_TELEMETRY, ENTITY_ESP, BALLISTIC, SMART_GUIDANCE, RESONANCE_FIELD, HOLOMAP -> true;
            default -> false;
        };
    }

    @Nullable
    public static SuitModuleType byId(String id) {
        if (id == null) return null;
        String key = id.toLowerCase(Locale.ROOT);
        for (SuitModuleType type : VALUES) if (type.id.equals(key)) return type;
        return null;
    }
}
