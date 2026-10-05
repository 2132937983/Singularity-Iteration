// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.suit;

import java.util.EnumSet;
import java.util.Locale;
import java.util.Set;
import net.minecraft.world.entity.EquipmentSlot;
import org.jetbrains.annotations.Nullable;

/**
 * Upgrade units for the quantum suit (0.1.7.33). Each unit is installed into one suit piece in the
 * quantum modification station and draws power from that piece while its feature switch is on.
 *
 * <p>Balance notes. A quantum piece stores 10 000 000 EU. The full sensor load of a helmet
 * (ESP + ballistic + predictor + holomap = 34 EU/t) empties a full helmet in about 4 hours of
 * continuous play; the solar helmets cover it in daylight. The ore scanner is the most expensive
 * sensor, because it gives the strongest advantage.
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
    /** Fuse timer: countdown of primed TNT, TNT carts and swelling creepers. */
    BLAST_WARNING("blast_warning", 2, 0xFF5A36, true, EquipmentSlot.CHEST, EquipmentSlot.HEAD),
    /** Motion model: predicted paths and attack wind-ups of nearby creatures. */
    BEHAVIOR_PREDICTOR("behavior_predictor", 8, 0xC77DFF, true, EquipmentSlot.HEAD),
    /** Tactical 3D holomap of the terrain and contacts around the wearer. */
    HOLOMAP("holomap", 10, 0x4DE1C1, true, EquipmentSlot.HEAD),
    /** Threat sensor: who looks at or targets the wearer (yellow / red). */
    THREAT_SENSOR("threat_sensor", 6, 0xFF3B30, true, EquipmentSlot.CHEST),
    /** Deflector: spends EU to cancel the damage the armour lets through. */
    DEFLECTOR("deflector", 0, 0x5AB8FF, false, EquipmentSlot.CHEST);

    private final String id;
    private final long drainPerTick;
    private final int color;
    private final boolean needsVisor;
    private final Set<EquipmentSlot> slots;
    /** The same slots as an array: the per-tick checks iterate it without an iterator object. */
    private final EquipmentSlot[] slotArray;
    private final String featureKey, nameKey, descriptionKey;
    /** values() copies the array on each call; the tick paths use this shared copy (do not modify). */
    public static final SuitModuleType[] VALUES = values();

    SuitModuleType(String id, long drainPerTick, int color, boolean needsVisor, EquipmentSlot first, EquipmentSlot... rest) {
        this.id = id;
        this.drainPerTick = drainPerTick;
        this.color = color;
        this.needsVisor = needsVisor;
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
    /** A HUD unit shows its data only through a quantum helmet visor. */
    public boolean needsVisor() { return needsVisor; }
    public Set<EquipmentSlot> slots() { return slots; }
    public boolean fits(EquipmentSlot slot) { return slots.contains(slot); }
    /** Equipment console switch key. */
    public String featureKey() { return featureKey; }
    public String nameKey() { return nameKey; }
    public String descriptionKey() { return descriptionKey; }
    EquipmentSlot[] slotArray() { return slotArray; }

    @Nullable
    public static SuitModuleType byId(String id) {
        if (id == null) return null;
        String key = id.toLowerCase(Locale.ROOT);
        for (SuitModuleType type : values()) if (type.id.equals(key)) return type;
        return null;
    }
}
