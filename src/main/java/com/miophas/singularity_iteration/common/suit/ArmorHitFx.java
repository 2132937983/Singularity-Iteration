// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.suit;

import com.miophas.singularity_iteration.common.suit.nano.NanoPackets;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

/**
 * 0.1.10.32 (S-6), server side: when the electric armour of a wearer with a quantum suit piece absorbs part of a hit, the
 * players near it get {@link NanoPackets#FX_ARMOR_HIT} (direction of the hit, absorbed share) for the hit ripple over the
 * armour. Works with or without the deflector. Hits without a place (fire, drowning, magic without a source) send nothing;
 * a fall comes from below. At most one message per wearer every {@link #GAP} ticks.
 */
public final class ArmorHitFx {
    private ArmorHitFx() {}

    static final int GAP = 4;
    static final double RANGE = 64;
    private static final Map<LivingEntity, Long> LAST = new WeakHashMap<>();

    /** True when the item is a quantum suit piece (also the advanced and heavy quantum chestplates). */
    public static boolean quantum(ItemStack stack) {
        if (stack.isEmpty()) return false;
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        String p = id.getPath();
        return "mio_icif".equals(id.getNamespace()) && (p.startsWith("armor/item_armor_quantum_") || p.startsWith("armor/item_armor_advanced_quantum_")
            || p.startsWith("armor/item_armor_heavy_quantum_"));
    }

    /** Called after the electric armour absorbed {@code absorbed} of {@code damage}. */
    public static void absorbed(LivingEntity entity, DamageSource source, float damage, float absorbed) {
        if (!(absorbed > 0) || !(damage > 0) || entity.level().isClientSide) return;
        boolean quantum = false;
        for (EquipmentSlot slot : SuitEnergyPool.ARMOR_SLOTS) quantum |= quantum(entity.getItemBySlot(slot));
        if (!quantum) return;
        long now = entity.level().getGameTime();
        Long last = LAST.get(entity);
        if (last != null && now - last < GAP) return;
        Vec3 dir = direction(entity, source);
        if (dir == null) return;
        LAST.put(entity, now);
        NanoPackets.broadcast(entity, new NanoPackets.Fx(entity.getId(), NanoPackets.FX_ARMOR_HIT, Math.min(1F, absorbed / damage), 0F, 0F,
            (float) dir.x, (float) dir.y, (float) dir.z, new int[0], new float[0]), RANGE);
    }

    /** Unit direction from the body centre to the hit, or null for a hit without a place. */
    static Vec3 direction(LivingEntity entity, DamageSource source) {
        if (source.is(DamageTypeTags.IS_FALL)) return new Vec3(0, -1, 0);
        Vec3 centre = entity.position().add(0, entity.getBbHeight() * 0.55, 0);
        Vec3 from = source.getSourcePosition();
        if (from == null && source.getDirectEntity() != null) from = source.getDirectEntity().position();
        if (from == null && source.getEntity() != null) from = source.getEntity().getEyePosition();
        if (from == null) return null;
        Vec3 dir = from.subtract(centre);
        return dir.lengthSqr() < 1.0E-6 ? new Vec3(0, 0, 1) : dir.normalize();
    }
}
