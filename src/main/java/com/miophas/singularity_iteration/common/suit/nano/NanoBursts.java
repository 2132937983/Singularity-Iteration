// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.suit.nano;

import com.miophas.singularity_iteration.common.Singularity_Iteration;
import com.miophas.singularity_iteration.common.registry.mio_icif_sounds;
import com.miophas.singularity_iteration.common.suit.SuitModuleType;
import com.miophas.singularity_iteration.common.suit.SuitModules;
import com.miophas.singularity_iteration.common.suit.SuitRegistry;
import com.miophas.singularity_iteration.common.suit.UnitRef;
import com.miophas.singularity_iteration.core.api.suit.SuitBurstSpec;
import com.miophas.singularity_iteration.core.api.suit.SuitModuleRegistry;
import it.unimi.dsi.fastutil.floats.FloatArrayList;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ThrownTrident;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Burst abilities of the nano units (0.1.8.5): Kinetic Dash (nano myofiber), EMP pulse (EMP
 * emitter) and optical camouflage, plus addon bursts of the public API. The server checks and pays;
 * the clients draw the effects ({@link NanoPackets.Fx}).
 */
public final class NanoBursts {
    private NanoBursts() {}

    /** Creatures that an EMP stuns for the full time (mechanical or energy-bound). */
    public static final TagKey<EntityType<?>> EMP_SENSITIVE = TagKey.create(Registries.ENTITY_TYPE,
        ResourceLocation.fromNamespaceAndPath(Singularity_Iteration.MOD_ID, "emp_sensitive"));

    /** Denied reasons (sent to the wearer for the HUD and the buzzer). */
    public static final int DENY_NONE = 0, DENY_OFF = 1, DENY_OVERHEAT = 2, DENY_COOLDOWN = 3, DENY_RESERVE = 4, DENY_CONFLICT = 5;
    /** Dash length in ticks (the client drives the motion). */
    public static final int DASH_TICKS = 6;

    /** Server: burst key {@code slot} pressed. */
    static void request(ServerPlayer player, int slot, float forward, float strafe) {
        if (player.isSpectator() || !player.isAlive()) return;
        NanoCoreState s = NanoSuit.state(player);
        UnitRef unit = NanoSuit.effectiveBurst(player, s.primary, s.secondary, slot);
        if (unit == null) { deny(player, DENY_NONE); return; }
        SuitModuleType builtin = unit.builtin();
        // a second press ends a running camouflage, whatever its state
        if (builtin == SuitModuleType.OPTICAL_CAMO && s.camoActive()) { endCamo(player, s, 0); return; }
        ItemStack piece = SuitModules.wornPieceWith(player, unit);
        boolean on = builtin != null ? NanoSuit.active(player, builtin) : SuitModules.isActive(player, unit);
        if (piece.isEmpty() || !SuitModules.isNanoPiece(piece) || !on) { deny(player, DENY_OFF); return; }
        SuitBurstSpec spec = NanoSuit.burstSpec(unit);
        if (spec == null) { deny(player, DENY_NONE); return; }
        if (s.overheated) { deny(player, DENY_OVERHEAT); return; }
        if (s.cooldown(unit.key()) > 0) { deny(player, DENY_COOLDOWN); return; }
        if (s.reserve < spec.reserveCost()) { deny(player, DENY_RESERVE); return; }
        // 0.1.8.6: the kinetic armor mesh and the optical camouflage never run together
        if (builtin == SuitModuleType.OPTICAL_CAMO && s.meshTicks > 0 || builtin == SuitModuleType.KINETIC_MESH && s.camoActive()) {
            deny(player, DENY_CONFLICT);
            return;
        }
        Vec3 input = input(player, forward, strafe);
        int tier = Math.max(1, SuitModules.tier(piece, unit.key()));
        boolean fired;
        if (builtin == SuitModuleType.MYOFIBER) fired = dash(player, s, input, tier);
        else if (builtin == SuitModuleType.EMP_EMITTER) fired = emp(player, s, tier);
        else if (builtin == SuitModuleType.OPTICAL_CAMO) fired = startCamo(player, s);
        else if (builtin == SuitModuleType.RECON_CLOUD) fired = recon(player, s, tier);
        else if (builtin == SuitModuleType.KINETIC_MESH) fired = startMesh(player, s, tier);
        else {
            var entry = SuitModuleRegistry.get(unit.key());
            fired = false;
            if (entry != null && entry.burst() != null) {
                try {
                    fired = entry.burst().fire(player, piece, input);
                } catch (RuntimeException e) {
                    Singularity_Iteration.LOGGER.error("Addon burst {} failed", unit.key(), e);
                }
            }
        }
        if (!fired) return;
        if (!spec.sustained()) {
            s.reserve = Math.max(0, s.reserve - spec.reserveCost());
            if (spec.cooldownTicks() > 0) s.cooldown.put(unit.key(), spec.cooldownTicks());
        }
        NanoSuit.addHeat(player, s, spec.heat());
        s.sentHash = 0;
    }

    private static void deny(ServerPlayer player, int reason) {
        com.miophas.singularity_iteration.common.suit.SuitPackets.send(player,
            NanoPackets.Fx.simple(player, NanoPackets.FX_DENIED, reason, 0, 0));
    }

    /** Movement input in world space (horizontal, length 0..1). */
    static Vec3 input(Player player, float forward, float strafe) {
        Vec3 in = new Vec3(strafe, 0, forward);
        if (in.lengthSqr() < 1.0E-4) return Vec3.ZERO;
        if (in.lengthSqr() > 1) in = in.normalize();
        float yaw = player.getYRot() * Mth.DEG_TO_RAD;
        double sin = Mth.sin(yaw), cos = Mth.cos(yaw);
        return new Vec3(in.x * cos - in.z * sin, 0, in.z * cos + in.x * sin);
    }

    // ------------------------------------------------------------------ Kinetic Dash

    private static boolean dash(ServerPlayer player, NanoCoreState s, Vec3 input, int tier) {
        if (player.isPassenger() || player.isFallFlying()) return false;
        Vec3 dir = input.lengthSqr() > 0.01 ? input.normalize() : horizontalLook(player);
        if (dir.lengthSqr() < 1.0E-4) return false;
        s.dashTicks = DASH_TICKS + 4;
        player.fallDistance = 0;
        int distance = NanoConfig.dashDistance() + (tier >= 2 ? 2 : 0);        // Mk.II: +2 blocks
        NanoPackets.broadcast(player, new NanoPackets.Fx(player.getId(), NanoPackets.FX_DASH, (float) player.getX(), (float) player.getY(),
            (float) player.getZ(), (float) dir.x, (float) dir.z, distance, new int[]{tier}, new float[0]), 96);
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(), mio_icif_sounds.NANO_DASH.get(), SoundSource.PLAYERS, 0.9F,
            0.95F + player.getRandom().nextFloat() * 0.1F);
        return true;
    }

    static Vec3 horizontalLook(Player player) {
        Vec3 look = player.getViewVector(1.0F);
        Vec3 flat = new Vec3(look.x, 0, look.z);
        return flat.lengthSqr() < 1.0E-4 ? Vec3.ZERO : flat.normalize();
    }

    // ------------------------------------------------------------------ EMP

    private static boolean emp(ServerPlayer player, NanoCoreState s, int tier) {
        ServerLevel level = player.serverLevel();
        double r = NanoConfig.empRadius() + (tier >= 2 ? 2 : 0);                // Mk.II: +2 blocks
        Vec3 c = player.position().add(0, player.getBbHeight() * 0.5, 0);
        AABB box = new AABB(c, c).inflate(r);
        boolean pvp = NanoConfig.empVsPlayers() && player.server.isPvpAllowed();
        FloatArrayList points = new FloatArrayList();
        for (Projectile p : level.getEntitiesOfClass(Projectile.class, box, e -> e.isAlive() && e.distanceToSqr(c) <= r * r)) {
            if (p.getOwner() == player) continue;
            if (p.getOwner() instanceof Player && !pvp) continue;
            if (points.size() < NanoPackets.Fx.MAX_POINTS) { points.add((float) p.getX()); points.add((float) p.getY()); points.add((float) p.getZ()); }
            level.sendParticles(ParticleTypes.ELECTRIC_SPARK, p.getX(), p.getY(), p.getZ(), 8, 0.15, 0.15, 0.15, 0.3);
            if (p instanceof ThrownTrident) {
                p.setDeltaMovement(p.getDeltaMovement().scale(-0.1));   // a trident drops (its owner picks it up)
            } else {
                p.discard();
            }
        }
        IntArrayList targets = new IntArrayList();
        int stun = NanoConfig.empStunTicks() * (tier >= 2 ? 3 : 2) / 2;          // Mk.II: x1.5
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, box, e -> e != player && e.isAlive() && e.distanceToSqr(c) <= r * r)) {
            boolean hit = false;
            if (e instanceof Creeper creeper) {
                creeper.setSwellDir(-1);
                hit = true;
            }
            if (e.getType().is(EMP_SENSITIVE) || e instanceof Creeper) {
                if (stun > 0) e.addEffect(new MobEffectInstance(SuitRegistry.EMP_STUN, stun, 0, false, true), player);
                if (e instanceof Mob mob && mob.getTarget() == player) mob.setTarget(null);
                hit = true;
            }
            if (e instanceof ServerPlayer other && pvp && !other.isCreative()) {
                NanoCoreState os = other.getExistingDataOrNull(SuitRegistry.NANO_CORE);
                if (os != null && os.camoActive()) endCamo(other, os, 1);
                NanoSuit.state(other).guidanceJamUntil = level.getGameTime() + 60;
                hit = true;
            }
            if (hit && targets.size() < NanoPackets.Fx.MAX_TARGETS) targets.add(e.getId());
        }
        // own camouflage drops; locks on the wearer break for 3 s
        if (s.camoActive()) endCamo(player, s, 1);
        s.lockJamUntil = level.getGameTime() + 60;
        NanoPackets.broadcast(player, new NanoPackets.Fx(player.getId(), NanoPackets.FX_EMP, (float) c.x, (float) c.y, (float) c.z,
            (float) r, tier, 0, targets.toIntArray(), points.toFloatArray()), 96);
        level.playSound(null, c.x, c.y, c.z, mio_icif_sounds.NANO_EMP.get(), SoundSource.PLAYERS, 1.2F, 1.0F);
        return true;
    }

    // ------------------------------------------------------------------ optical camouflage

    private static boolean startCamo(ServerPlayer player, NanoCoreState s) {
        s.camoTicks = 1;
        // creatures that hunt the wearer lose it unless they are right next to it
        for (Mob mob : player.serverLevel().getEntitiesOfClass(Mob.class, player.getBoundingBox().inflate(48), m -> m.getTarget() == player)) {
            if (mob.distanceToSqr(player) > NanoEvents.CAMO_SENSE_RANGE * NanoEvents.CAMO_SENSE_RANGE) mob.setTarget(null);
        }
        NanoPackets.broadcast(player, NanoPackets.Fx.simple(player, NanoPackets.FX_CAMO_ON, NanoConfig.camoVsPlayers() ? 1 : 0, 0, 0), 128);
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(), mio_icif_sounds.NANO_CAMO_ON.get(), SoundSource.PLAYERS, 0.7F, 1.0F);
        return true;
    }

    /** Server, each tick while the camouflage runs: reserve drain, limits, refresh for late viewers. */
    static void tickCamo(ServerPlayer player, NanoCoreState s) {
        SuitBurstSpec spec = NanoConfig.burst(SuitModuleType.OPTICAL_CAMO);
        s.camoTicks++;
        float perSecond = (spec == null ? 6 : spec.reserveCost()) * (SuitModules.tier(player, SuitModuleType.OPTICAL_CAMO) >= 2 ? 0.75F : 1F);
        s.reserve = Math.max(0, s.reserve - perSecond / 20F);
        if (s.reserve <= 0 || s.camoTicks > NanoSuit.camoMaxTicks(player)) { endCamo(player, s, 2); return; }
        if (!NanoSuit.active(player, SuitModuleType.OPTICAL_CAMO) || s.overheated || player.isSpectator()) { endCamo(player, s, 1); return; }
        if (s.camoTicks % 20 == 0) {
            NanoPackets.broadcast(player, NanoPackets.Fx.simple(player, NanoPackets.FX_CAMO_KEEP, NanoConfig.camoVsPlayers() ? 1 : 0, 0, 0), 128);
        }
    }

    // ------------------------------------------------------------------ nano recon cloud (0.1.8.6)

    /**
     * Glows the creatures in range for the wearer: Mk.I 24 blocks in a 90 degree cone ahead for
     * 10 s, Mk.II 32 blocks all around for 15 s. Other players glow only when the PvP switch allows.
     */
    private static boolean recon(ServerPlayer player, NanoCoreState s, int tier) {
        ServerLevel level = player.serverLevel();
        double range = NanoConfig.reconRange() * (tier >= 2 ? 4.0 / 3.0 : 1.0);
        boolean sphere = tier >= 2;
        int ticks = tier >= 2 ? 300 : 200;
        Vec3 eye = player.getEyePosition(), look = player.getViewVector(1.0F);
        boolean players = NanoConfig.reconVsPlayers() && player.server.isPvpAllowed();
        IntArrayList targets = new IntArrayList();
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(range),
                e -> e != player && e.isAlive() && !e.isSpectator())) {
            if (e instanceof Player && !players) continue;
            Vec3 to = e.getBoundingBox().getCenter().subtract(eye);
            double d = to.length();
            if (d > range || d < 1.0E-3) continue;
            if (!sphere && look.dot(to.scale(1 / d)) < 0.707) continue;
            targets.add(e.getId());
            if (targets.size() >= NanoPackets.Fx.MAX_TARGETS) break;
        }
        s.reconTicks = ticks;
        s.reconMax = ticks;
        com.miophas.singularity_iteration.common.suit.SuitPackets.send(player, new NanoPackets.Fx(player.getId(), NanoPackets.FX_RECON,
            (float) eye.x, (float) eye.y, (float) eye.z, ticks, tier, 0, targets.toIntArray(), new float[0]));
        NanoPackets.broadcast(player, new NanoPackets.Fx(player.getId(), NanoPackets.FX_RECON_CLOUD, (float) eye.x, (float) eye.y, (float) eye.z,
            (float) look.x, (float) look.y, (float) look.z, new int[]{(int) Math.round(range), sphere ? 1 : 0}, new float[0]), 96);
        level.playSound(null, player.getX(), player.getY(), player.getZ(), mio_icif_sounds.NANO_RECON.get(), SoundSource.PLAYERS, 0.8F, 1.0F);
        return true;
    }

    // ------------------------------------------------------------------ kinetic armor mesh (0.1.8.6)

    /** Hardens the nano weave: Mk.I 6 s / Mk.II 8 s of extra damage cut ({@code NanoEvents.onDamageMesh}). */
    private static boolean startMesh(ServerPlayer player, NanoCoreState s, int tier) {
        int ticks = tier >= 2 ? 160 : 120;
        s.meshTicks = ticks;
        s.meshMax = ticks;
        s.sentHash = 0;
        NanoPackets.broadcast(player, NanoPackets.Fx.simple(player, NanoPackets.FX_MESH_ON, ticks, tier, 0), 128);
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(), mio_icif_sounds.NANO_MESH_ON.get(), SoundSource.PLAYERS, 0.9F, 1.0F);
        return true;
    }

    /** The mesh run ends (time is up). */
    static void endMesh(ServerPlayer player, NanoCoreState s) {
        s.meshTicks = 0;
        s.sentHash = 0;
        NanoPackets.broadcast(player, NanoPackets.Fx.simple(player, NanoPackets.FX_MESH_OFF, 0, 0, 0), 128);
    }

    /** Ends the camouflage. {@code reason}: 0 manual, 1 broken (attack, hit, EMP, overheat), 2 depleted. */
    public static void endCamo(ServerPlayer player, NanoCoreState s, int reason) {
        if (!s.camoActive()) return;
        s.camoTicks = 0;
        SuitBurstSpec spec = NanoConfig.burst(SuitModuleType.OPTICAL_CAMO);
        if (spec != null && spec.cooldownTicks() > 0) s.cooldown.put(SuitModuleType.OPTICAL_CAMO.id(), spec.cooldownTicks());
        s.sentHash = 0;
        NanoPackets.broadcast(player, NanoPackets.Fx.simple(player, NanoPackets.FX_CAMO_OFF, reason, 0, 0), 128);
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(), mio_icif_sounds.NANO_CAMO_OFF.get(), SoundSource.PLAYERS,
            0.7F, reason == 1 ? 0.8F : 1.0F);
    }
}
