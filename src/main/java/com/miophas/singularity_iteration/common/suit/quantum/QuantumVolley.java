// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.suit.quantum;

import com.miophas.singularity_iteration.common.armory.WingmanEntity;
import com.miophas.singularity_iteration.common.armory.WingmanService;
import com.miophas.singularity_iteration.common.registry.mio_icif_sounds;
import com.miophas.singularity_iteration.common.suit.SmartGuidance;
import com.miophas.singularity_iteration.common.suit.nano.NanoPackets;
import it.unimi.dsi.fastutil.floats.FloatArrayList;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * Entangled volley (0.1.10.0), the action of the entanglement relay: every Wingman of the wearer
 * in the same dimension fires a salvo of entangled bolts, timed so that all of them strike at the
 * same tick.
 *
 * <ul>
 *   <li>Targets: the Smart Guidance locks of the wearer (spread over the Wingmen), else the
 *       creature in view (up to {@link QuantumConfig#volleyRange()} blocks). Other players only
 *       with the PvP switch.</li>
 *   <li>Timing: a short charge ({@link #CHARGE} ticks), then each Wingman fires so that its bolts
 *       arrive at the common impact tick (flight time from its distance). The bolts are resolved on
 *       the server at impact (no projectile entities); the clients draw them.</li>
 *   <li>Damage: {@link QuantumConfig#volleyDamage()} per Wingman, credited to the wearer, as
 *       projectile damage; armour applies.</li>
 * </ul>
 * The order survives nothing: a Wingman or a target that is gone at its time is skipped.
 */
public final class QuantumVolley {
    private QuantumVolley() {}

    /** Charge before the first shot, ticks; bolt speed in blocks per tick; flight time limits. */
    static final int CHARGE = 12, MIN_FLIGHT = 6, MAX_FLIGHT = 40;
    static final double BOLT_SPEED = 1.5;
    /** Bolts drawn per Wingman. */
    static final int BOLTS = 3;

    private static final class Strike {
        final UUID owner;
        final ResourceKey<Level> level;
        final int wingman, target;
        final long fireAt, impactAt;
        final float damage;
        boolean fired;

        Strike(UUID owner, ResourceKey<Level> level, int wingman, int target, long fireAt, long impactAt, float damage) {
            this.owner = owner;
            this.level = level;
            this.wingman = wingman;
            this.target = target;
            this.fireAt = fireAt;
            this.impactAt = impactAt;
            this.damage = damage;
        }
    }

    private static final List<Strike> PENDING = new ArrayList<>();

    /** Server: the volley order. False (with a refusal sent) without Wingmen or without a target. */
    static boolean order(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        List<WingmanEntity> wingmen = new ArrayList<>();
        for (WingmanEntity w : WingmanService.of(player)) if (w.state() == WingmanEntity.ACTIVE && w.level() == level) wingmen.add(w);
        if (wingmen.isEmpty()) { QuantumActions.deny(player, QuantumActions.DENY_NO_WINGMAN); return false; }
        List<LivingEntity> targets = targets(player);
        // 0.1.10.1 Frozen Volley combo: inside one's own time dilation the slowed creatures are locked at once
        QuantumCoreState state = QuantumCore.state(player);
        boolean frozen = state.dilationTicks > 0;
        if (frozen) {
            for (int id : state.slowed) {
                if (targets.size() >= 8) break;
                if (level.getEntity(id) instanceof LivingEntity l && l.isAlive() && !targets.contains(l)) targets.add(l);
            }
        }
        if (targets.isEmpty()) { QuantumActions.deny(player, QuantumActions.DENY_NO_TARGET); return false; }
        double range = QuantumConfig.volleyRange();
        long now = level.getGameTime();
        int[] flight = new int[wingmen.size()];
        LivingEntity[] aim = new LivingEntity[wingmen.size()];
        int longest = MIN_FLIGHT;
        for (int i = 0; i < wingmen.size(); i++) {
            aim[i] = targets.get(i % targets.size());
            double d = wingmen.get(i).distanceTo(aim[i]);
            if (d > range) { aim[i] = null; continue; }
            flight[i] = Math.max(MIN_FLIGHT, Math.min(MAX_FLIGHT, (int) Math.ceil(d / BOLT_SPEED)));
            longest = Math.max(longest, flight[i]);
        }
        long impact = now + (frozen ? 0 : CHARGE) + longest;            // Frozen Volley: no charge
        IntArrayList pairs = new IntArrayList();
        FloatArrayList timing = new FloatArrayList();
        float damage = (float) (QuantumConfig.volleyDamage() * (frozen ? QuantumConfig.frozenVolleyDamage() : 1.0));
        for (int i = 0; i < wingmen.size(); i++) {
            if (aim[i] == null) continue;
            WingmanEntity w = wingmen.get(i);
            long fireAt = impact - flight[i];
            PENDING.add(new Strike(player.getUUID(), level.dimension(), w.getId(), aim[i].getId(), fireAt, impact, damage));
            if (pairs.size() + 2 <= NanoPackets.Fx.MAX_TARGETS && timing.size() + 3 <= NanoPackets.Fx.MAX_POINTS) {
                pairs.add(w.getId());
                pairs.add(aim[i].getId());
                timing.add(fireAt - now);
                timing.add(impact - now);
                timing.add(BOLTS);
            }
        }
        if (pairs.isEmpty()) { QuantumActions.deny(player, QuantumActions.DENY_NO_TARGET); return false; }
        NanoPackets.broadcast(player, new NanoPackets.Fx(player.getId(), QuantumPackets.FX_VOLLEY, (float) player.getX(), (float) player.getY(),
            (float) player.getZ(), pairs.size() / 2F, 0, 0, pairs.toIntArray(), timing.toFloatArray()), 192);
        level.playSound(null, player.getX(), player.getY(), player.getZ(), mio_icif_sounds.VOLLEY_ORDER.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
        if (frozen) QuantumSynergies.combo(player, QuantumSynergies.COMBO_FROZEN_VOLLEY);
        return true;
    }

    /** The locks of the wearer (valid ones), else the creature in view. */
    private static List<LivingEntity> targets(ServerPlayer player) {
        List<LivingEntity> out = new ArrayList<>();
        boolean pvp = QuantumConfig.volleyVsPlayers();
        for (int id : SmartGuidance.locks(player)) {
            Entity e = player.level().getEntity(id);
            if (e instanceof LivingEntity living && SmartGuidance.valid(player, e) && (!(e instanceof net.minecraft.world.entity.player.Player) || pvp)
                    && !out.contains(living)) out.add(living);
        }
        if (!out.isEmpty()) return out;
        LivingEntity aimed = aimed(player, QuantumConfig.volleyRange());
        if (aimed != null) out.add(aimed);
        return out;
    }

    @Nullable
    private static LivingEntity aimed(ServerPlayer player, double range) {
        Vec3 eye = player.getEyePosition(), look = player.getViewVector(1.0F);
        Vec3 end = eye.add(look.scale(range));
        var block = player.level().clip(new ClipContext(eye, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        if (block.getType() != HitResult.Type.MISS) end = block.getLocation();
        boolean pvp = QuantumConfig.volleyVsPlayers();
        EntityHitResult hit = ProjectileUtil.getEntityHitResult(player.level(), player, eye, end, new AABB(eye, end).inflate(1.5),
            e -> e instanceof LivingEntity && e.isAlive() && !e.isSpectator() && SmartGuidance.valid(player, e)
                && (!(e instanceof net.minecraft.world.entity.player.Player) || pvp), 0.6F);
        return hit != null && hit.getEntity() instanceof LivingEntity living ? living : null;
    }

    /** Server tick: shots at their time, impacts at the common tick. */
    static void tick(MinecraftServer server) {
        if (PENDING.isEmpty()) return;
        for (Iterator<Strike> it = PENDING.iterator(); it.hasNext(); ) {
            Strike s = it.next();
            ServerLevel level = server.getLevel(s.level);
            if (level == null) { it.remove(); continue; }
            long now = level.getGameTime();
            if (!s.fired && now >= s.fireAt) {
                s.fired = true;
                Entity w = level.getEntity(s.wingman), t = level.getEntity(s.target);
                if (w instanceof WingmanEntity wingman && wingman.isAlive() && t != null) {
                    wingman.entangledShot(t.position().add(0, t.getBbHeight() * 0.55, 0));
                    level.playSound(null, w.getX(), w.getY(), w.getZ(), mio_icif_sounds.WINGMAN_BOOST.get(), SoundSource.NEUTRAL, 0.5F, 1.6F);
                }
            }
            if (now < s.impactAt) continue;
            it.remove();
            Entity t = level.getEntity(s.target);
            ServerPlayer owner = server.getPlayerList().getPlayer(s.owner);
            if (!(t instanceof LivingEntity target) || !target.isAlive() || owner == null || owner.level() != level || !s.fired) continue;
            Entity w = level.getEntity(s.wingman);
            Entity direct = w instanceof WingmanEntity && w.isAlive() ? w : owner;
            target.invulnerableTime = 0;                       // every Wingman's bolts land, not only the first
            if (s.damage > 0) target.hurt(level.damageSources().mobProjectile(direct, owner), s.damage);
            Vec3 from = direct.position().subtract(target.position());
            target.knockback(0.35, from.x, from.z);
            level.playSound(null, target.getX(), target.getY(), target.getZ(), mio_icif_sounds.VOLLEY_IMPACT.get(), SoundSource.PLAYERS, 0.8F,
                0.9F + level.random.nextFloat() * 0.2F);
        }
    }

    /** Drops the strikes of a player who leaves. */
    static void forget(UUID owner) {
        PENDING.removeIf(s -> s.owner.equals(owner));
    }

    static void clear() {
        PENDING.clear();
    }
}
