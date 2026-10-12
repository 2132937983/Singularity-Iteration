// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.suit;

import com.miophas.singularity_iteration.core.api.suit.ISuitModuleItem;
import com.miophas.singularity_iteration.core.api.suit.SuitModuleRegistry;
import com.miophas.singularity_iteration.core.api.suit.SuitModuleSpec;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * One suit upgrade unit: a built-in {@link SuitModuleType} or an addon unit of the public API
 * ({@link SuitModuleSpec}, 0.1.8.0). The station, the rules and the panel use this view, so both
 * kinds behave the same.
 */
public record UnitRef(@Nullable SuitModuleType builtin, @Nullable SuitModuleSpec addon) {
    public static UnitRef of(SuitModuleType type) { return new UnitRef(type, null); }
    public static UnitRef of(SuitModuleSpec spec) { return new UnitRef(null, spec); }

    /** The unit of a stored key: a built-in id ("ballistic") or a registered addon id ("addon:unit"); null when unknown. */
    @Nullable
    public static UnitRef byKey(String key) {
        SuitModuleType type = SuitModuleType.byId(key);
        if (type != null) return of(type);
        var entry = SuitModuleRegistry.get(key);
        return entry == null ? null : of(entry.spec());
    }

    /** The unit that {@code stack} installs, or null (an addon unit must be registered). */
    @Nullable
    public static UnitRef ofItem(ItemStack stack) {
        if (stack.getItem() instanceof SuitModuleItem item) return of(item.type());
        if (stack.getItem() instanceof ISuitModuleItem item) {
            SuitModuleSpec spec = item.suitModule(stack);
            return spec != null && SuitModuleRegistry.get(spec.key()) != null ? of(spec) : null;
        }
        return null;
    }

    public String key() { return builtin != null ? builtin.id() : addon.key(); }
    public boolean fits(EquipmentSlot slot) { return builtin != null ? builtin.fits(slot) : addon.fits(slot); }
    public long drainPerTick() { return builtin != null ? builtin.drainPerTick() : addon.drainPerTick(); }
    public int color() { return builtin != null ? builtin.color() : addon.color(); }
    public boolean needsVisor() { return builtin != null ? builtin.needsVisor() : addon.needsVisor(); }
    public String featureKey() { return builtin != null ? builtin.featureKey() : addon.featureKey(); }
    public String nameKey() { return builtin != null ? builtin.nameKey() : addon.nameKey(); }
    public String descriptionKey() { return builtin != null ? builtin.descriptionKey() : addon.nameKey() + ".desc"; }

    /** Slots in a fixed order (HEAD, CHEST, LEGS, FEET) that take the unit. */
    public EquipmentSlot[] slots() {
        if (builtin != null) return builtin.slotArray();
        return addon.slots().stream().sorted((a, b) -> Integer.compare(b.getIndex(), a.getIndex())).toArray(EquipmentSlot[]::new);
    }

    /** A new item of the unit (the station gives it back on removal). */
    public ItemStack unitStack() {
        if (builtin != null) return new ItemStack(SuitRegistry.unitItem(builtin));
        return new ItemStack(BuiltInRegistries.ITEM.get(addon.unitItem()));
    }
}
