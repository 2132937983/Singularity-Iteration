// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.suit.core;

import com.miophas.singularity_iteration.core.api.suit.SuitClass;
import java.util.EnumSet;
import java.util.Locale;
import java.util.Set;
import net.minecraft.world.entity.EquipmentSlot;
import org.jetbrains.annotations.Nullable;

/**
 * Frame cores (0.1.9.0): one core per helmet or chestplate, in the core socket next to the unit
 * slots (quantum) or the lattice (nano). The cores replace the retired solar helmets, the
 * advanced quantum chestplate and the heavy quantum chestplate.
 *
 * <table>
 *   <caption>Cores</caption>
 *   <tr><th>Core</th><th>Socket</th><th>Effect</th></tr>
 *   <tr><td>Solar Array I/II/III</td><td>helmet (III: quantum only)</td><td>8/1, 64/8, 512/64 EU/t day/night under open sky; feeds the suit, then the carried equipment</td></tr>
 *   <tr><td>Graviton Bulwark</td><td>quantum chestplate</td><td>armour +1, emergency repair below 20 % health, gravity anchor (no fall damage, no knockback)</td></tr>
 *   <tr><td>Advanced Propulsion</td><td>quantum chestplate</td><td>+90 000 000 EU, no flight ceiling, thrust +15 %, sharper maneuver vectoring, armour +1, knockback resistance +0.5, repair 2.0 health/s, arc discharge burst</td></tr>
 * </table>
 */
public enum FrameCore {
    SOLAR_ARRAY_1("solar_array_1", EquipmentSlot.HEAD, 0xFFD740, EnumSet.of(SuitClass.NANO, SuitClass.QUANTUM), 8, 1, 3_000),
    SOLAR_ARRAY_2("solar_array_2", EquipmentSlot.HEAD, 0xFFC400, EnumSet.of(SuitClass.NANO, SuitClass.QUANTUM), 64, 8, 10_000),
    SOLAR_ARRAY_3("solar_array_3", EquipmentSlot.HEAD, 0xFFAB00, EnumSet.of(SuitClass.QUANTUM), 512, 64, 10_000),
    GRAVITON("graviton", EquipmentSlot.CHEST, 0xB388FF, EnumSet.of(SuitClass.QUANTUM), 0, 0, 0),
    PROPULSION("propulsion", EquipmentSlot.CHEST, 0x4FC3F7, EnumSet.of(SuitClass.QUANTUM), 0, 0, 0);

    /** Feature keys of the cores (panel switches, stored per piece). */
    public static final String KEY_SOLAR = "core_solar", KEY_SOLAR_ALL = "core_solar_all", KEY_HEALING = "healing",
        KEY_ANCHOR = "core_anchor", KEY_KNOCKBACK = "knockback_resistance", KEY_VISUAL = "core_visual";

    public static final FrameCore[] VALUES = values();

    private final String id;
    private final EquipmentSlot slot;
    private final int color;
    private final Set<SuitClass> classes;
    private final int solarDay, solarNight, transferLimit;

    FrameCore(String id, EquipmentSlot slot, int color, Set<SuitClass> classes, int solarDay, int solarNight, int transferLimit) {
        this.id = id;
        this.slot = slot;
        this.color = color;
        this.classes = classes;
        this.solarDay = solarDay;
        this.solarNight = solarNight;
        this.transferLimit = transferLimit;
    }

    public String id() { return id; }
    public EquipmentSlot slot() { return slot; }
    public int color() { return color; }
    public boolean allows(SuitClass suitClass) { return classes.contains(suitClass); }
    public boolean isSolar() { return solarDay > 0; }
    public int solarDay() { return solarDay; }
    public int solarNight() { return solarNight; }
    public int transferLimit() { return transferLimit; }
    /** 1..3 for the solar arrays, else 1. */
    public int tier() { return this == SOLAR_ARRAY_2 ? 2 : this == SOLAR_ARRAY_3 ? 3 : 1; }
    public String nameKey() { return "core.mio_icif." + id; }
    public String descriptionKey() { return "core.mio_icif." + id + ".desc"; }
    /** Registry path of the core item. */
    public String itemPath() { return "module/item_core_" + id; }

    @Nullable
    public static FrameCore byId(@Nullable String id) {
        if (id == null || id.isEmpty()) return null;
        String key = id.toLowerCase(Locale.ROOT);
        for (FrameCore core : VALUES) if (core.id.equals(key)) return core;
        return null;
    }
}
