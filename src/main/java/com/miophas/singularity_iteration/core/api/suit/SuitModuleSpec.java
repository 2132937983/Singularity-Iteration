// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.core.api.suit;

import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;

/**
 * Description of a quantum suit upgrade unit of an addon (0.1.8.0, public API).
 *
 * <p>The quantum modification station installs the unit into a quantum suit piece that has a
 * slot in {@link #slots}. The Singularity Management Panel shows a switch for it. While the
 * switch is on, the unit costs {@link #drainPerTick} EU/t (collected once a second, the full-set
 * energy pool applies) and its {@link SuitModuleBehavior} runs.
 *
 * @param id           unit id; its namespace must be the addon mod id (built-in units have no namespace)
 * @param slots        suit slots that take the unit (HEAD, CHEST, LEGS, FEET)
 * @param drainPerTick EU/t while the unit is on (0 = only event costs)
 * @param color        signature colour 0xRRGGBB (station list, HUD accents)
 * @param needsVisor   true when the unit works only with a quantum helmet worn (HUD units)
 * @param unitItem     id of the unit item; the station gives it back when the unit is removed
 */
public record SuitModuleSpec(ResourceLocation id, Set<EquipmentSlot> slots, long drainPerTick, int color, boolean needsVisor,
                             ResourceLocation unitItem) {
    public SuitModuleSpec {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(unitItem, "unitItem");
        if (slots == null || slots.isEmpty()) throw new IllegalArgumentException("a unit needs at least one slot");
        for (EquipmentSlot s : slots) {
            if (s.getType() != EquipmentSlot.Type.HUMANOID_ARMOR) throw new IllegalArgumentException("not an armor slot: " + s);
        }
        if (drainPerTick < 0) throw new IllegalArgumentException("negative drain");
        slots = Set.copyOf(EnumSet.copyOf(slots));
    }

    /** Key stored in the suit piece data: the full id, for example {@code "myaddon:sonar"}. */
    public String key() { return id.toString(); }

    /** Switch key of the Singularity Management Panel: {@code fcs_<namespace>_<path>}. */
    public String featureKey() { return "fcs_" + id.getNamespace() + "_" + id.getPath().replace('/', '_'); }

    /** Translation key of the unit name: {@code module.<namespace>.<path>}; add {@code .desc} for the description. */
    public String nameKey() { return "module." + id.getNamespace() + "." + id.getPath().replace('/', '.'); }

    public boolean fits(EquipmentSlot slot) { return slots.contains(slot); }
}
