// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.suit.quantum;

import com.miophas.singularity_iteration.common.Singularity_Iteration;
import com.miophas.singularity_iteration.common.suit.SuitRegistry;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.EntityHitResult;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.ProjectileImpactEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingKnockBackEvent;
import net.neoforged.neoforge.event.entity.player.ArrowLooseEvent;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * Server events of the quantum actions (0.1.10.0): the phase shift rules (creature, projectile
 * and blast damage pass through; projectiles fly through; no knockback; no attacks, shots or
 * mining while out of phase), the fall protection after a blink, the volley clock, and the clean-up
 * when a player leaves, dies or changes dimension (the field drops its projectiles, the dilation
 * takes its modifiers back, a real slow motion ends).
 */
@EventBusSubscriber(modid = Singularity_Iteration.MOD_ID)
public final class QuantumEvents {
    private QuantumEvents() {}

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onIncomingDamage(LivingIncomingDamageEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        QuantumCoreState s = player.getExistingDataOrNull(SuitRegistry.QUANTUM_CORE);
        if (s == null) return;
        DamageSource source = event.getSource();
        if (source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) return;           // void and /kill always apply
        if (s.blinkSafeTicks > 0 && source.is(DamageTypeTags.IS_FALL)) { event.setCanceled(true); return; }
        if (s.phased() && passesThrough(source)) {
            QuantumSynergies.absorbed(player, s, event.getAmount());      // 0.1.10.1 Phase Counter synergy
            event.setCanceled(true);
        }
    }

    /** 0.1.10.1 Afterimage Decoy: a creature drawn by the decoy cannot take the wearer while it lasts. */
    @SubscribeEvent
    public static void onChangeTarget(net.neoforged.neoforge.event.entity.living.LivingChangeTargetEvent event) {
        if (event.getNewAboutToBeSetTarget() instanceof ServerPlayer player && event.getEntity() instanceof net.minecraft.world.entity.Mob mob
                && QuantumSynergies.drawnByDecoy(mob, player)) {
            event.setCanceled(true);
        }
    }

    /** Out of phase: anything a creature or a projectile does, and blasts. Lava, fire, drowning, starving and falling still apply. */
    static boolean passesThrough(DamageSource source) {
        return source.getEntity() != null || source.getDirectEntity() != null || source.is(DamageTypeTags.IS_PROJECTILE)
            || source.is(DamageTypeTags.IS_EXPLOSION);
    }

    @SubscribeEvent
    public static void onProjectileImpact(ProjectileImpactEvent event) {
        if (event.getRayTraceResult() instanceof EntityHitResult hit && hit.getEntity() instanceof Player player
                && !player.level().isClientSide && QuantumCore.phased(player)) {
            event.setCanceled(true);                                            // it flies on through the body
        }
    }

    @SubscribeEvent
    public static void onKnockBack(LivingKnockBackEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && QuantumCore.phased(player)) event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onAttack(AttackEntityEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && QuantumCore.phased(player)) event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onArrowLoose(ArrowLooseEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && QuantumCore.phased(player)) event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onLeftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
        if (event.getEntity() instanceof ServerPlayer player && QuantumCore.phased(player)) event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onBreakSpeed(PlayerEvent.BreakSpeed event) {
        if (event.getEntity() instanceof ServerPlayer player && QuantumCore.phased(player)) event.setNewSpeed(0);
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        QuantumVolley.tick(event.getServer());
    }

    // ------------------------------------------------------------------ clean-up

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            settle(player);
            QuantumVolley.forget(player.getUUID());
        }
    }

    @SubscribeEvent
    public static void onDeath(LivingDeathEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) settle(player);
    }

    @SubscribeEvent
    public static void onChangeDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) settle(player);
    }

    @SubscribeEvent
    public static void onServerStopping(ServerStoppingEvent event) {
        QuantumDilation.restoreTickRate(event.getServer());
        QuantumVolley.clear();
    }

    /** Ends every running action of the player and undoes its changes in the world. */
    static void settle(ServerPlayer player) {
        QuantumCoreState s = player.getExistingDataOrNull(SuitRegistry.QUANTUM_CORE);
        if (s == null) return;
        if (s.fieldOn) QuantumField.stop(player, s, 1);
        else if (!s.frozen.isEmpty()) QuantumField.release(player, s);
        if (s.dilationTicks > 0 || !s.slowed.isEmpty() || !s.projectileSpeed.isEmpty() || s.realSlow) QuantumDilation.end(player, s, false);
        if (!s.addonKey.isEmpty()) QuantumActions.endAddon(player, s, false);
        s.phaseTicks = 0;
        s.blinkSafeTicks = 0;
        s.phaseAbsorbed = 0;
        QuantumSynergies.releaseDecoy(player, s);               // 0.1.10.1
        s.markedTarget = -1;
    }
}
