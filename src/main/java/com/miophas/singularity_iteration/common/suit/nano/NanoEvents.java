// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.suit.nano;

import com.miophas.singularity_iteration.common.Singularity_Iteration;
import com.miophas.singularity_iteration.common.suit.SuitModuleType;
import com.miophas.singularity_iteration.common.suit.SuitModules;
import com.miophas.singularity_iteration.common.suit.SuitRegistry;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.damagesource.DamageContainer;
import net.neoforged.neoforge.event.entity.living.LivingChangeTargetEvent;
import net.neoforged.neoforge.event.entity.living.LivingEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.living.MobEffectEvent;
import net.neoforged.neoforge.event.entity.player.ArrowLooseEvent;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;

/**
 * Server events of the nano units (0.1.8.5): damage filters (thermoregulating membrane, toxin
 * filter), HF sync armour penetration, the camouflage rules (creatures, attack and hit) and the
 * Kinetic Dash fall protection.
 */
@EventBusSubscriber(modid = Singularity_Iteration.MOD_ID)
public final class NanoEvents {
    private NanoEvents() {}

    /** A creature closer than this still senses a camouflaged wearer. */
    public static final double CAMO_SENSE_RANGE = 2.5;

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onIncomingDamage(LivingIncomingDamageEvent event) {
        LivingEntity victim = event.getEntity();
        if (victim.level().isClientSide) return;
        DamageSource source = event.getSource();
        // ---- attacker side: HF sync armour penetration, camouflage broken by attacking
        if (source.getEntity() instanceof ServerPlayer attacker && attacker != victim) {
            NanoCoreState as = attacker.getExistingDataOrNull(SuitRegistry.NANO_CORE);
            if (as != null && as.camoActive()) NanoBursts.endCamo(attacker, as, 1);
            int hf = source.getDirectEntity() == attacker ? NanoSuit.hfLevel(attacker) : 0;
            if (hf > 0 && (!(victim instanceof Player) || NanoConfig.hfVsPlayers())) {
                float keep = (float) (1.0 - NanoSuit.hfArmorPen(hf));
                event.addReductionModifier(DamageContainer.Reduction.ARMOR, (container, reduction) -> reduction * keep);
                if (victim.level() instanceof net.minecraft.server.level.ServerLevel level) {
                    level.sendParticles(net.minecraft.core.particles.ParticleTypes.ELECTRIC_SPARK, victim.getX(), victim.getY() + victim.getBbHeight() * 0.6,
                        victim.getZ(), 5 + 3 * hf, victim.getBbWidth() * 0.3, victim.getBbHeight() * 0.25, victim.getBbWidth() * 0.3, 0.35);
                }
            }
        }
        if (!(victim instanceof ServerPlayer player)) return;
        NanoCoreState s = player.getExistingDataOrNull(SuitRegistry.NANO_CORE);
        // 0.1.8.6: self-repair waits for quiet; remember every hit
        if (event.getAmount() > 0 && (s != null || !SuitModules.wornPieceWith(player, SuitModuleType.SELF_REPAIR).isEmpty())) {
            NanoSuit.state(player).lastHit = player.level().getGameTime();
            if (s == null) s = player.getExistingDataOrNull(SuitRegistry.NANO_CORE);
        }
        // ---- Kinetic Dash: no fall damage while it runs
        if (s != null && s.dashTicks > 0 && source.is(DamageTypeTags.IS_FALL)) {
            event.setCanceled(true);
            return;
        }
        // ---- thermoregulating membrane
        int thermo = NanoSuit.level(player, SuitModuleType.THERMOREGULATOR);
        if (thermo > 0) {
            if (source.is(DamageTypeTags.IS_FREEZING)) { event.setCanceled(true); return; }
            // Mk.I lava -30 % / fire -50 %; Mk.II lava -50 % / fire -70 %
            if (source.is(DamageTypes.LAVA)) event.setAmount(event.getAmount() * (thermo >= 2 ? 0.5F : 0.7F));
            else if (source.is(DamageTypeTags.IS_FIRE)) event.setAmount(event.getAmount() * (thermo >= 2 ? 0.3F : 0.5F));
        }
        // ---- toxin and radiation filter: radiation damage -60 % (Mk.II -85 %)
        int toxin = NanoSuit.level(player, SuitModuleType.TOXIN_FILTER);
        if (toxin > 0 && source.is(com.miophas.singularity_iteration.common.event.mio_icif_DamageTypes.RADIATION)) {
            event.setAmount(event.getAmount() * (toxin >= 2 ? 0.15F : 0.4F));
        }
        // ---- a hit ends the camouflage
        if (s != null && s.camoActive() && event.getAmount() > 0) NanoBursts.endCamo(player, s, 1);
    }

    @SubscribeEvent
    public static void onAttack(AttackEntityEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            NanoCoreState s = player.getExistingDataOrNull(SuitRegistry.NANO_CORE);
            if (s != null && s.camoActive()) NanoBursts.endCamo(player, s, 1);
        }
    }

    @SubscribeEvent
    public static void onArrowLoose(ArrowLooseEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            NanoCoreState s = player.getExistingDataOrNull(SuitRegistry.NANO_CORE);
            if (s != null && s.camoActive()) NanoBursts.endCamo(player, s, 1);
        }
    }

    /**
     * Camouflage: creatures see the wearer only from very close. 0.1.8.6 thermal camouflage: while
     * sneaking at half the distance (Mk.II 30 %, and 75 % while walking).
     */
    @SubscribeEvent
    public static void onVisibility(LivingEvent.LivingVisibilityEvent event) {
        if (!(event.getEntity() instanceof Player player) || player.level().isClientSide) return;
        if (NanoSuit.camoActive(player)) {
            event.modifyVisibility(0.05);
            return;
        }
        int thermal = NanoSuit.level(player, SuitModuleType.THERMAL_CAMO);
        if (thermal > 0) {
            if (player.isShiftKeyDown()) event.modifyVisibility(thermal >= 2 ? 0.3 : 0.5);
            else if (thermal >= 2) event.modifyVisibility(0.75);
        }
    }

    @SubscribeEvent
    public static void onChangeTarget(LivingChangeTargetEvent event) {
        if (!(event.getNewAboutToBeSetTarget() instanceof Player player) || player.level().isClientSide) return;
        double d2 = event.getEntity().distanceToSqr(player);
        if (NanoSuit.camoActive(player) && d2 > CAMO_SENSE_RANGE * CAMO_SENSE_RANGE) {
            event.setCanceled(true);
            return;
        }
        // 0.1.8.6 jammer: hostile creatures beyond the range cannot pick the wearer
        if (event.getEntity() instanceof net.minecraft.world.entity.monster.Enemy) {
            int jammer = NanoSuit.level(player, SuitModuleType.JAMMER);
            double range = NanoSuit.jammerRange(jammer);
            if (jammer > 0 && d2 > range * range) event.setCanceled(true);
        }
    }

    // ------------------------------------------------------------------ 0.1.8.6

    /** Kinetic armor mesh: after the armour (normal priority), a further cut; total absorption stays at most 95 %. */
    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onDamageMesh(net.neoforged.neoforge.event.entity.living.LivingDamageEvent.Pre event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        NanoCoreState s = player.getExistingDataOrNull(SuitRegistry.NANO_CORE);
        if (s == null || s.meshTicks <= 0) return;
        DamageSource source = event.getSource();
        if (source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) return;
        float current = event.getNewDamage();
        if (current <= 0) return;
        int tier = Math.max(1, com.miophas.singularity_iteration.common.suit.SuitModules.tier(player, SuitModuleType.KINETIC_MESH));
        double cut = Math.min(0.9, NanoConfig.meshReduction() + (tier >= 2 ? 0.1 : 0));
        float floor = event.getOriginalDamage() * 0.05F;
        float next = Math.max((float) (current * (1 - cut)), floor);
        if (next < current) event.setNewDamage(next);
        net.minecraft.world.phys.Vec3 dir = com.miophas.singularity_iteration.common.suit.SuitServer.hitDirection(player, source);
        NanoPackets.broadcast(player, new NanoPackets.Fx(player.getId(), NanoPackets.FX_MESH_HIT, (float) player.getX(), (float) player.getY(),
            (float) player.getZ(), (float) dir.x, (float) dir.y, (float) dir.z, new int[0], new float[0]), 96);
        if (player.tickCount % 4 == 0) {
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(), com.miophas.singularity_iteration.common.registry.mio_icif_sounds.NANO_MESH_HIT.get(),
                net.minecraft.sounds.SoundSource.PLAYERS, 0.7F, 0.9F + player.getRandom().nextFloat() * 0.2F);
        }
    }

    /** Shock gel: a landing from 2 blocks or more opens the rebound window. */
    @SubscribeEvent
    public static void onFall(net.neoforged.neoforge.event.entity.living.LivingFallEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && event.getDistance() >= 2 && NanoSuit.level(player, SuitModuleType.SHOCK_GEL) > 0) {
            NanoSuit.state(player).landedAt = player.level().getGameTime();
        }
    }

    /** Shock gel rebound (the client lifts its own jump; the server shows it to everyone). */
    @SubscribeEvent
    public static void onJump(LivingEvent.LivingJumpEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        NanoCoreState s = player.getExistingDataOrNull(SuitRegistry.NANO_CORE);
        if (s == null || player.level().getGameTime() - s.landedAt > REBOUND_WINDOW) return;
        int gel = NanoSuit.level(player, SuitModuleType.SHOCK_GEL);
        if (gel == 0) return;
        s.landedAt = -100;
        NanoPackets.broadcast(player, NanoPackets.Fx.simple(player, NanoPackets.FX_REBOUND, gel, 0, 0), 64);
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(), com.miophas.singularity_iteration.common.registry.mio_icif_sounds.NANO_REBOUND.get(),
            net.minecraft.sounds.SoundSource.PLAYERS, 0.8F, 1.0F);
    }

    /** Ticks after a landing in which a jump rebounds. */
    public static final int REBOUND_WINDOW = 8;

    /** Toxin filter: poison and wither do not take hold. */
    @SubscribeEvent
    public static void onEffect(MobEffectEvent.Applicable event) {
        if (!(event.getEntity() instanceof Player player) || player.level().isClientSide) return;
        var effect = event.getEffectInstance().getEffect();
        boolean toxic = effect.is(MobEffects.POISON) || effect.is(MobEffects.WITHER);
        boolean mk2 = effect.is(MobEffects.HUNGER) || effect.is(MobEffects.CONFUSION);
        if (!toxic && !mk2) return;
        int toxin = NanoSuit.level(player, SuitModuleType.TOXIN_FILTER);
        if (toxin > 0 && (toxic || toxin >= 2)) event.setResult(MobEffectEvent.Applicable.Result.DO_NOT_APPLY);
    }

    /** Hook of the radiation effect: with the toxin filter 60 % (Mk.II 85 %) of the radiation ticks do nothing. */
    public static boolean filtersRadiationTick(LivingEntity entity) {
        if (!(entity instanceof Player player) || player.level().isClientSide) return false;
        int toxin = NanoSuit.level(player, SuitModuleType.TOXIN_FILTER);
        return toxin > 0 && player.getRandom().nextFloat() < (toxin >= 2 ? 0.85F : 0.6F);
    }
}
