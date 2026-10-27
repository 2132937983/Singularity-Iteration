// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.client.suit;

import com.miophas.singularity_iteration.common.registry.mio_icif_sounds;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;

/**
 * 0.1.10.32 (A-5 full, S-8 visor): the suit boot and the visor light-up.
 * <ul>
 *   <li>{@link #start}: when the Armory has locked the last piece of a suit-up (or a full nano / quantum suit comes on
 *       otherwise through the Armory): the suit light sweeps from the chest core again (the power-on sweep of every piece),
 *       a ripple runs from the core ({@link SuitRippleLayer#core}), the visor lights from its centre after
 *       {@link SuitFxRules#BOOT_VISOR_DELAY} seconds, with the visor boot sound.</li>
 *   <li>{@link #visor}: the visor light-up alone (the own HUD comes on).</li>
 * </ul>
 * The visor light-up also runs when a helmet comes on by itself (its power-on, {@code SuitGlowLayer}).
 */
public final class SuitBoot {
    private SuitBoot() {}

    /** Wall time (ms) the visor light-up of an entity starts. */
    private static final Map<LivingEntity, Long> VISOR = new WeakHashMap<>();

    /** The full boot of the suit of {@code e}. */
    public static void start(LivingEntity e) {
        if (SuitGlowLayer.modelOf(e.getItemBySlot(EquipmentSlot.CHEST)) == null && SuitGlowLayer.modelOf(e.getItemBySlot(EquipmentSlot.HEAD)) == null)
            return;
        SuitGlowLayer.restartPower(e);
        SuitRippleLayer.core(e);
        VISOR.put(e, System.currentTimeMillis() + Math.round(SuitFxRules.BOOT_VISOR_DELAY * 1000));
        e.level().playLocalSound(e.getX(), e.getY() + 1.6, e.getZ(), mio_icif_sounds.VISOR_BOOT.get(), SoundSource.PLAYERS, 0.6F, 1.1F, false);
    }

    /** The visor light-up of {@code e} now (the own HUD boots). */
    public static void visor(LivingEntity e) {
        if (FcsClientConfig.suitGimmicks()) VISOR.put(e, System.currentTimeMillis());
    }

    /**
     * Seconds since the visor light-up of {@code e} started (negative: none running). While a boot waits for its visor the
     * light-up stands at its start (0: only the centre line lit).
     */
    static float visorSince(LivingEntity e) {
        Long at = VISOR.get(e);
        if (at == null) return -1F;
        float s = (System.currentTimeMillis() - at) / 1000F;
        if (s > (SuitFxRules.VISOR_HALF + 2F) / SuitFxRules.VISOR_SPEED) {
            VISOR.remove(e);
            return -1F;
        }
        return Math.max(0F, s);
    }
}
