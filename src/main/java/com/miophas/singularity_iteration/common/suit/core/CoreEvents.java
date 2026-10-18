// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.suit.core;

import com.miophas.singularity_iteration.common.Singularity_Iteration;
import com.miophas.singularity_iteration.common.registry.mio_icif_sounds;
import com.miophas.singularity_iteration.common.suit.SuitModules;
import com.miophas.singularity_iteration.common.suit.SuitPackets;
import com.miophas.singularity_iteration.common.suit.nano.NanoCoreState;
import com.miophas.singularity_iteration.common.suit.nano.NanoPackets;
import com.miophas.singularity_iteration.common.suit.nano.NanoSuit;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingFallEvent;
import net.neoforged.neoforge.event.entity.living.LivingKnockBackEvent;

/**
 * Events and the burst of the frame cores (0.1.9.0): the gravity anchor of the Graviton Bulwark
 * core (no fall damage, no knockback of any kind) and the arc discharge of the Advanced
 * Propulsion core (core burst key).
 */
@EventBusSubscriber(modid = Singularity_Iteration.MOD_ID)
public final class CoreEvents {
    private CoreEvents() {}

    /** Cooldown key of the arc discharge in the suit state. */
    public static final String DISCHARGE_KEY = "core_discharge";
    /** Refusal reasons sent with {@code NanoPackets.FX_CORE_DENIED}. */
    public static final int DENY_NO_CORE = 0, DENY_COOLDOWN = 1, DENY_ENERGY = 2;

    @SubscribeEvent
    public static void onFall(LivingFallEvent event) {
        if (!event.getEntity().level().isClientSide && FrameCores.anchored(event.getEntity())) event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onKnockBack(LivingKnockBackEvent event) {
        if (FrameCores.anchored(event.getEntity())) event.setCanceled(true);
    }

    /**
     * Server: the core burst key. The Advanced Propulsion core releases a high-voltage arc discharge:
     * each creature in range takes an arc and is thrown outwards by the shock wave; projectiles in
     * range are turned away. Paid from the suit energy pool; cooldown from the config.
     */
    public static void discharge(ServerPlayer player) {
        if (player.isSpectator() || !player.isAlive()) return;
        if (!FrameCores.active(player, FrameCore.PROPULSION)) { deny(player, DENY_NO_CORE); return; }
        NanoCoreState s = NanoSuit.state(player);
        if (s.cooldown(DISCHARGE_KEY) > 0) { deny(player, DENY_COOLDOWN); return; }
        var chest = FrameCores.wornWith(player, FrameCore.PROPULSION);
        long cost = CoreConfig.dischargeEu();
        if (!SuitModules.canPay(player, chest, cost)) { deny(player, DENY_ENERGY); return; }
        SuitModules.extract(player, chest, cost);
        s.cooldown.put(DISCHARGE_KEY, CoreConfig.dischargeCooldown());
        ServerLevel level = player.serverLevel();
        double r = CoreConfig.dischargeRadius();
        Vec3 c = player.position().add(0, player.getBbHeight() * 0.5, 0);
        AABB box = new AABB(c, c).inflate(r);
        boolean pvp = CoreConfig.dischargeVsPlayers() && player.server.isPvpAllowed();
        float damage = (float) CoreConfig.dischargeDamage();
        double push = CoreConfig.dischargePush();
        IntArrayList targets = new IntArrayList();
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, box, e -> e != player && e.isAlive() && !e.isSpectator()
                && e.distanceToSqr(c) <= r * r)) {
            if (player.isAlliedTo(e)) continue;
            if (e instanceof OwnableEntity own && player.getUUID().equals(own.getOwnerUUID())) continue;
            if (e instanceof Player other && (!pvp || other.isCreative())) continue;
            Vec3 away = e.position().subtract(player.position());
            Vec3 flat = new Vec3(away.x, 0, away.z);
            double d = Math.max(0.5, flat.length());
            double falloff = 1.0 - Math.min(1.0, d / (r + 1));
            if (damage > 0) e.hurt(player.damageSources().playerAttack(player), damage * (float) (0.6 + 0.4 * falloff));
            Vec3 dir = flat.lengthSqr() < 1.0E-4 ? player.getViewVector(1.0F).multiply(1, 0, 1) : flat.scale(1 / d);
            if (!FrameCores.anchored(e)) {
                e.push(dir.x * push * (0.4 + 0.6 * falloff), 0.35 + 0.25 * falloff, dir.z * push * (0.4 + 0.6 * falloff));
                e.hurtMarked = true;
            }
            level.sendParticles(ParticleTypes.ELECTRIC_SPARK, e.getX(), e.getY() + e.getBbHeight() * 0.5, e.getZ(), 12, 0.3, 0.4, 0.3, 0.4);
            if (targets.size() < NanoPackets.Fx.MAX_TARGETS) targets.add(e.getId());
        }
        for (Projectile p : level.getEntitiesOfClass(Projectile.class, box, p -> p.isAlive() && p.getOwner() != player)) {
            if (p.getOwner() instanceof Player && !pvp) continue;
            Vec3 away = p.position().subtract(c);
            if (away.lengthSqr() < 1.0E-4) continue;
            p.setDeltaMovement(away.normalize().scale(Math.max(0.6, p.getDeltaMovement().length())));
            p.hurtMarked = true;
        }
        NanoPackets.broadcast(player, new NanoPackets.Fx(player.getId(), NanoPackets.FX_DISCHARGE, (float) c.x, (float) c.y, (float) c.z,
            (float) r, 0, 0, targets.toIntArray(), new float[0]), 96);
        level.playSound(null, c.x, c.y, c.z, mio_icif_sounds.CORE_DISCHARGE.get(), SoundSource.PLAYERS, 1.3F, 1.0F);
    }

    private static void deny(ServerPlayer player, int reason) {
        SuitPackets.send(player, NanoPackets.Fx.simple(player, NanoPackets.FX_CORE_DENIED, reason, 0, 0));
    }
}
