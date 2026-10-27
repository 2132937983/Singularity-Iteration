// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.client.suit;

import com.miophas.singularity_iteration.common.Singularity_Iteration;
import com.miophas.singularity_iteration.common.client.flight.JetpackSoundClient;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * 0.1.10.32 (S-8): the moving parts of a worn quantum chestplate, per player, eased once a tick ({@link SuitFxRules#ease}).
 * <ul>
 *   <li><b>Vents:</b> the two fin assemblies beside the spine of the back unit slide out ({@link SuitFxRules#VENT_SLIDE}
 *       pixels) and the slots behind them glow while the suit flies (jetpack thrust or Special Maneuver flight).</li>
 *   <li><b>Shoulders:</b> the upper shoulder plates slide up and back a little while sprinting.</li>
 * </ul>
 * {@code BakedArmorModel} moves the quads, {@code SuitGlowLayer} draws the vent light. Option {@code suit_gimmicks}.
 */
@EventBusSubscriber(modid = Singularity_Iteration.MOD_ID, value = Dist.CLIENT)
public final class SuitGimmicks {
    private SuitGimmicks() {}

    static final ResourceLocation CHEST = ResourceLocation.fromNamespaceAndPath(Singularity_Iteration.MOD_ID, "armor/item_armor_quantum_chestplate");

    private static final class State {
        float vent, ventO, shoulder, shoulderO;
    }

    private static final Map<LivingEntity, State> STATES = new WeakHashMap<>();

    static boolean quantumChest(LivingEntity e) {
        ItemStack chest = e.getItemBySlot(EquipmentSlot.CHEST);
        return !chest.isEmpty() && CHEST.equals(BuiltInRegistries.ITEM.getKey(chest.getItem())) && SuitGlowLayer.charge(chest) > 0F;
    }

    @SubscribeEvent
    static void onPlayerTick(PlayerTickEvent.Post event) {
        Player p = event.getEntity();
        if (!p.level().isClientSide) return;
        State s = STATES.get(p);
        boolean worn = FcsClientConfig.suitGimmicks() && quantumChest(p);
        if (!worn && s == null) return;
        if (s == null) STATES.put(p, s = new State());
        boolean flying = worn && (JetpackSoundClient.throttle(p) > 0F || ManeuverModeClient.inManeuver(p));
        boolean sprint = worn && p.isSprinting() && p.onGround();
        s.ventO = s.vent;
        s.shoulderO = s.shoulder;
        s.vent = SuitFxRules.ease(s.vent, flying);
        s.shoulder = SuitFxRules.ease(s.shoulder, sprint);
        if (!worn && s.vent == 0F && s.shoulder == 0F && s.ventO == 0F && s.shoulderO == 0F) STATES.remove(p);
    }

    /** Vent opening of {@code e} (0..1, smoothed). */
    public static float vent(LivingEntity e, float partial) {
        State s = STATES.get(e);
        return s == null ? 0F : SuitFxRules.smooth(Mth.lerp(partial, s.ventO, s.vent));
    }

    /** Shoulder slide of {@code e} (0..1, smoothed). */
    public static float shoulder(LivingEntity e, float partial) {
        State s = STATES.get(e);
        return s == null ? 0F : SuitFxRules.smooth(Mth.lerp(partial, s.shoulderO, s.shoulder));
    }
}
