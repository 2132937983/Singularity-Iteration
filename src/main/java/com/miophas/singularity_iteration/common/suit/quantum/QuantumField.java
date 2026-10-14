// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.suit.quantum;

import com.miophas.singularity_iteration.common.registry.mio_icif_sounds;
import com.miophas.singularity_iteration.common.suit.SuitModuleType;
import com.miophas.singularity_iteration.common.suit.SuitModules;
import com.miophas.singularity_iteration.common.suit.nano.NanoCoreState;
import com.miophas.singularity_iteration.common.suit.nano.NanoPackets;
import com.miophas.singularity_iteration.common.suit.nano.NanoSuit;
import it.unimi.dsi.fastutil.floats.FloatArrayList;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.AbstractHurtingProjectile;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Force field (0.1.10.0), the action of a Mk.II deflector: a dome of {@link QuantumConfig#fieldRadius()}
 * blocks around the wearer while the key is on and the coherence lasts.
 *
 * <ul>
 *   <li>Hostile projectiles that enter stop in the air and hang there (no gravity) until the field
 *       falls; then they drop. Fireballs and other self-propelled shots cannot be held and burst at
 *       the dome.</li>
 *   <li>Hostile creatures (and creatures that hunt the wearer) inside are pushed out each tick.
 *       Other players only with the PvP switch.</li>
 *   <li>Cost: {@link QuantumConfig#fieldStartCost()} to raise, {@link QuantumConfig#fieldPerSecond()}
 *       per second; no regeneration meanwhile. The cooldown starts when it falls.</li>
 * </ul>
 */
public final class QuantumField {
    private QuantumField() {}

    static boolean start(ServerPlayer player, QuantumCoreState s, int tier) {
        s.fieldOn = true;
        s.fieldTicks = 0;
        s.sentHash = 0;
        NanoPackets.broadcast(player, NanoPackets.Fx.simple(player, QuantumPackets.FX_FIELD_ON, QuantumConfig.fieldRadius(), tier, 0), 128);
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(), mio_icif_sounds.FIELD_ON.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
        return true;
    }

    /** Server, each tick while the field stands. */
    static void tick(ServerPlayer player, QuantumCoreState s) {
        ItemStack piece = SuitModules.wornPieceWith(player, SuitModuleType.DEFLECTOR);
        if (piece.isEmpty() || !SuitModules.isQuantumPiece(piece) || SuitModules.tier(piece, SuitModuleType.DEFLECTOR.id()) < 2
                || !SuitModules.isActive(player, SuitModuleType.DEFLECTOR) || !QuantumConfig.enabled("force_field") || player.isSpectator()) {
            stop(player, s, 1);
            return;
        }
        s.fieldTicks++;
        if (!QuantumCore.drain(player, s, QuantumConfig.fieldPerSecond() / 20F)) return;     // decoherence took it down
        ServerLevel level = player.serverLevel();
        double r = QuantumConfig.fieldRadius();
        Vec3 c = centre(player);
        AABB box = new AABB(c, c).inflate(r);
        boolean pvp = QuantumConfig.fieldVsPlayers();
        FloatArrayList caught = new FloatArrayList();
        for (Projectile p : level.getEntitiesOfClass(Projectile.class, box, e -> e.isAlive() && e.distanceToSqr(c) <= r * r)) {
            if (QuantumActions.friendly(p, player, pvp)) continue;
            if (p instanceof AbstractHurtingProjectile) {
                // a self-propelled shot cannot be held: it bursts at the dome
                if (caught.size() < NanoPackets.Fx.MAX_POINTS) { caught.add((float) p.getX()); caught.add((float) p.getY()); caught.add((float) p.getZ()); }
                level.sendParticles(ParticleTypes.ELECTRIC_SPARK, p.getX(), p.getY(), p.getZ(), 10, 0.15, 0.15, 0.15, 0.25);
                p.discard();
                continue;
            }
            if (!s.frozen.containsKey(p.getId())) {
                // only shots in flight: an arrow stuck in the ground or a resting projectile is left alone
                if (p.getDeltaMovement().lengthSqr() < 0.01) continue;
                s.frozen.put(p.getId(), p.isNoGravity());
                s.frozenVelocity.put(p.getId(), p.getDeltaMovement());       // 0.1.10.1 Static Barrage
                if (caught.size() < NanoPackets.Fx.MAX_POINTS) { caught.add((float) p.getX()); caught.add((float) p.getY()); caught.add((float) p.getZ()); }
            }
            p.setDeltaMovement(Vec3.ZERO);
            p.setNoGravity(true);
            p.hurtMarked = true;
        }
        if (!caught.isEmpty()) {
            NanoPackets.broadcast(player, new NanoPackets.Fx(player.getId(), QuantumPackets.FX_FIELD_CATCH, (float) c.x, (float) c.y, (float) c.z,
                (float) r, 0, 0, new int[0], caught.toFloatArray()), 96);
            level.playSound(null, c.x, c.y, c.z, mio_icif_sounds.FIELD_CATCH.get(), SoundSource.PLAYERS, 0.7F, 0.9F + level.random.nextFloat() * 0.2F);
        }
        // creatures inside are pushed out (every second tick)
        if (s.fieldTicks % 2 == 0) {
            for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, box, e -> e.distanceToSqr(c) < r * r)) {
                if (!QuantumActions.affects(e, player, pvp)) continue;
                Vec3 away = e.position().subtract(player.position());
                away = new Vec3(away.x, 0, away.z);
                if (away.lengthSqr() < 1.0E-4) away = new Vec3(1, 0, 0);
                double depth = 1 - Math.sqrt(e.distanceToSqr(c)) / r;            // stronger near the centre
                Vec3 push = away.normalize().scale(0.35 + 0.55 * depth);
                e.setDeltaMovement(e.getDeltaMovement().scale(0.3).add(push.x, 0.12, push.z));
                e.hurtMarked = true;
            }
        }
        if (s.fieldTicks % 20 == 0) {
            NanoPackets.broadcast(player, NanoPackets.Fx.simple(player, QuantumPackets.FX_FIELD_KEEP, (float) r,
                SuitModules.tier(piece, SuitModuleType.DEFLECTOR.id()), 0), 128);
        }
    }

    /** The field falls. {@code reason}: 0 key, 1 the unit stopped, 2 the coherence ran out. */
    static void stop(ServerPlayer player, QuantumCoreState s, int reason) {
        if (!s.fieldOn) return;
        s.fieldOn = false;
        s.sentHash = 0;
        // 0.1.10.1 Static Barrage combo: switched off by the key, the field throws the held shots back
        if (reason == 0 && !s.frozen.isEmpty()) barrage(player, s);
        release(player, s);
        NanoCoreState keys = NanoSuit.state(player);
        int cd = QuantumConfig.fieldCooldown();
        if (cd > 0) keys.cooldown.put(SuitModuleType.DEFLECTOR.id(), cd);
        keys.dirty();
        NanoPackets.broadcast(player, NanoPackets.Fx.simple(player, QuantumPackets.FX_FIELD_OFF, reason, 0, 0), 128);
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(), mio_icif_sounds.FIELD_OFF.get(), SoundSource.PLAYERS,
            0.9F, reason == 2 ? 0.8F : 1.0F);
    }

    /** The held projectiles get their gravity back and drop. */
    static void release(ServerPlayer player, QuantumCoreState s) {
        ServerLevel level = player.serverLevel();
        for (var e : s.frozen.int2BooleanEntrySet()) {
            Entity entity = level.getEntity(e.getIntKey());
            if (entity instanceof Projectile p && p.isAlive()) {
                p.setNoGravity(e.getBooleanValue());
                p.setDeltaMovement(0, -0.05, 0);
                p.hurtMarked = true;
            }
        }
        s.frozen.clear();
        s.frozenVelocity.clear();
    }

    /**
     * Static Barrage: each held projectile flies back at its catch speed, toward its shooter when
     * the shooter is still there (else straight back), now owned by the wearer. The projectiles that
     * flew off are no longer held (release() only drops the rest).
     */
    private static void barrage(ServerPlayer player, QuantumCoreState s) {
        ServerLevel level = player.serverLevel();
        it.unimi.dsi.fastutil.floats.FloatArrayList points = new it.unimi.dsi.fastutil.floats.FloatArrayList();
        for (var it = s.frozen.int2BooleanEntrySet().iterator(); it.hasNext(); ) {
            var e = it.next();
            Vec3 v = s.frozenVelocity.get(e.getIntKey());
            if (!(level.getEntity(e.getIntKey()) instanceof Projectile p) || !p.isAlive() || v == null) continue;
            double speed = Math.max(0.6, v.length());
            Entity shooter = p.getOwner();
            Vec3 dir = shooter != null && shooter.isAlive() && shooter.level() == level && shooter.distanceToSqr(p) < 64 * 64
                ? shooter.getBoundingBox().getCenter().subtract(p.position()) : v.scale(-1);
            if (dir.lengthSqr() < 1.0E-6) continue;
            p.setNoGravity(e.getBooleanValue());
            p.setOwner(player);
            p.setDeltaMovement(dir.normalize().scale(speed));
            p.hurtMarked = true;
            if (points.size() < NanoPackets.Fx.MAX_POINTS) { points.add((float) p.getX()); points.add((float) p.getY()); points.add((float) p.getZ()); }
            it.remove();
            s.frozenVelocity.remove(e.getIntKey());
        }
        if (points.isEmpty()) return;
        NanoPackets.broadcast(player, new NanoPackets.Fx(player.getId(), QuantumPackets.FX_BARRAGE, (float) player.getX(), (float) player.getY(),
            (float) player.getZ(), points.size() / 3F, 0, 0, new int[0], points.toFloatArray()), 96);
        QuantumSynergies.combo(player, QuantumSynergies.COMBO_STATIC_BARRAGE);
    }

    static Vec3 centre(ServerPlayer player) {
        return player.position().add(0, player.getBbHeight() * 0.5, 0);
    }
}
