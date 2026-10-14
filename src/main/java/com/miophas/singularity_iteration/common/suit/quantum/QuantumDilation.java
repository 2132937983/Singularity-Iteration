// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.suit.quantum;

import com.miophas.singularity_iteration.common.Singularity_Iteration;
import com.miophas.singularity_iteration.common.registry.mio_icif_sounds;
import com.miophas.singularity_iteration.common.suit.nano.NanoPackets;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * Time dilation (0.1.10.0), the action of a Mk.II behaviour predictor: for
 * {@link QuantumConfig#dilationTicks()} ticks a field of {@link QuantumConfig#dilationRadius()}
 * blocks moves with the wearer. Inside, hostile creatures move {@link QuantumConfig#dilationSlow()}
 * slower (bosses half as much) and hostile projectiles fly at {@link QuantumConfig#dilationProjectile()}
 * of their speed (with the same share of their gravity, so they drift instead of dropping); a
 * projectile that leaves the field gets its speed back. The wearer moves 20 % faster.
 *
 * <p>The game has one clock per server, so a true slow motion would slow every player. This is a
 * local slow; the optional real slow motion ({@link QuantumConfig#realSlowMotion()}, off by default)
 * changes the tick rate only in single player with nobody else online, and restores it at the end,
 * when the player leaves and when the server stops.
 */
public final class QuantumDilation {
    private QuantumDilation() {}

    static final ResourceLocation SLOW_ID = ResourceLocation.fromNamespaceAndPath(Singularity_Iteration.MOD_ID, "quantum_dilation_slow");
    static final ResourceLocation FLY_ID = ResourceLocation.fromNamespaceAndPath(Singularity_Iteration.MOD_ID, "quantum_dilation_fly");
    static final ResourceLocation HASTE_ID = ResourceLocation.fromNamespaceAndPath(Singularity_Iteration.MOD_ID, "quantum_dilation_haste");

    /** The server whose tick rate a real slow motion changed (restored on stop), or null. */
    @Nullable private static MinecraftServer slowedServer;
    private static float slowedFrom = 20;

    static boolean start(ServerPlayer player, QuantumCoreState s) {
        s.dilationTicks = s.dilationMax = QuantumConfig.dilationTicks();
        s.sentHash = 0;
        modifier(player.getAttribute(Attributes.MOVEMENT_SPEED), HASTE_ID, 0.2);
        realSlowStart(player, s);
        NanoPackets.broadcast(player, NanoPackets.Fx.simple(player, QuantumPackets.FX_DILATION_ON, QuantumConfig.dilationRadius(), s.dilationTicks,
            s.dilationMax), 128);
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(), mio_icif_sounds.DILATION_ON.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
        return true;
    }

    /** Server, each tick of the field. */
    static void tick(ServerPlayer player, QuantumCoreState s) {
        ServerLevel level = player.serverLevel();
        double r = QuantumConfig.dilationRadius();
        Vec3 c = player.position().add(0, player.getBbHeight() * 0.5, 0);
        AABB box = new AABB(c, c).inflate(r);
        boolean pvp = QuantumConfig.dilationVsPlayers();
        double slow = QuantumConfig.dilationSlow();
        // creatures: movement modifiers while inside; free-flying motion is damped too
        IntOpenHashSet inside = new IntOpenHashSet();
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, box, e -> e.distanceToSqr(c) <= r * r)) {
            if (!QuantumActions.affects(e, player, pvp)) continue;
            boolean boss = e.getType().is(net.neoforged.neoforge.common.Tags.EntityTypes.BOSSES);
            double k = boss ? slow * 0.5 : slow;
            inside.add(e.getId());
            modifier(e.getAttribute(Attributes.MOVEMENT_SPEED), SLOW_ID, -k);
            modifier(e.getAttribute(Attributes.FLYING_SPEED), FLY_ID, -k);
            if (!(e instanceof Player)) {
                Vec3 v = e.getDeltaMovement();
                double damp = 1 - k * 0.5;
                e.setDeltaMovement(v.x * damp, e.isNoGravity() ? v.y * damp : v.y, v.z * damp);
            }
        }
        for (var it = s.slowed.iterator(); it.hasNext(); ) {
            int id = it.nextInt();
            if (inside.contains(id)) continue;
            clear(level.getEntity(id));
            it.remove();
        }
        s.slowed.addAll(inside);
        // projectiles: a share of their entry speed while inside, the entry speed again outside
        double factor = QuantumConfig.dilationProjectile();
        IntOpenHashSet flying = new IntOpenHashSet();
        for (Projectile p : level.getEntitiesOfClass(Projectile.class, box, e -> e.isAlive() && e.distanceToSqr(c) <= r * r)) {
            if (QuantumActions.friendly(p, player, pvp)) continue;
            Vec3 v = p.getDeltaMovement();
            if (!s.projectileSpeed.containsKey(p.getId())) s.projectileSpeed.put(p.getId(), v.length());
            flying.add(p.getId());
            double target = s.projectileSpeed.get(p.getId()) * factor, now = v.length();
            if (now > target && now > 1.0E-4) v = v.scale(target / now);
            double g = p.getGravity();
            if (g > 0) v = v.add(0, g * (1 - factor), 0);            // gravity at the same share
            p.setDeltaMovement(v);
            p.hurtMarked = true;
        }
        for (var it = s.projectileSpeed.int2DoubleEntrySet().iterator(); it.hasNext(); ) {
            var e = it.next();
            if (flying.contains(e.getIntKey())) continue;
            restore(level.getEntity(e.getIntKey()), e.getDoubleValue());
            it.remove();
        }
        if (--s.dilationTicks <= 0) {
            end(player, s, true);
            return;
        }
        if (s.dilationTicks % 20 == 0) {
            NanoPackets.broadcast(player, NanoPackets.Fx.simple(player, QuantumPackets.FX_DILATION_KEEP, (float) r, s.dilationTicks, s.dilationMax), 128);
        }
    }

    /** The field ends (time up, or the player leaves): every change is undone. */
    static void end(ServerPlayer player, QuantumCoreState s, boolean announce) {
        ServerLevel level = player.serverLevel();
        for (int id : s.slowed) clear(level.getEntity(id));
        s.slowed.clear();
        for (var e : s.projectileSpeed.int2DoubleEntrySet()) restore(level.getEntity(e.getIntKey()), e.getDoubleValue());
        s.projectileSpeed.clear();
        modifier(player.getAttribute(Attributes.MOVEMENT_SPEED), HASTE_ID, 0);
        realSlowEnd(player, s);
        boolean was = s.dilationTicks > 0 || announce;
        s.dilationTicks = 0;
        s.sentHash = 0;
        if (!was) return;
        NanoPackets.broadcast(player, NanoPackets.Fx.simple(player, QuantumPackets.FX_DILATION_OFF, 0, 0, 0), 128);
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(), mio_icif_sounds.DILATION_OFF.get(), SoundSource.PLAYERS, 0.9F, 1.0F);
    }

    private static void clear(@Nullable Entity entity) {
        if (!(entity instanceof LivingEntity e)) return;
        modifier(e.getAttribute(Attributes.MOVEMENT_SPEED), SLOW_ID, 0);
        modifier(e.getAttribute(Attributes.FLYING_SPEED), FLY_ID, 0);
    }

    private static void restore(@Nullable Entity entity, double speed) {
        if (!(entity instanceof Projectile p) || !p.isAlive()) return;
        Vec3 v = p.getDeltaMovement();
        if (v.lengthSqr() > 1.0E-8 && speed > 0) {
            p.setDeltaMovement(v.normalize().scale(speed));
            p.hurtMarked = true;
        }
    }

    /** Sets (amount != 0) or removes (amount == 0) a transient multiplier; transient modifiers are never saved. */
    static void modifier(@Nullable AttributeInstance attribute, ResourceLocation id, double amount) {
        if (attribute == null) return;
        if (amount == 0) {
            if (attribute.hasModifier(id)) attribute.removeModifier(id);
            return;
        }
        AttributeModifier current = attribute.getModifier(id);
        if (current == null || current.amount() != amount) {
            attribute.addOrUpdateTransientModifier(new AttributeModifier(id, amount, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
        }
    }

    // ------------------------------------------------------------------ single-player real slow motion

    private static void realSlowStart(ServerPlayer player, QuantumCoreState s) {
        MinecraftServer server = player.server;
        if (!QuantumConfig.realSlowMotion() || !server.isSingleplayer() || server.getPlayerCount() != 1 || s.realSlow) return;
        var rate = server.tickRateManager();
        if (slowedServer == null) slowedFrom = rate.tickrate();
        rate.setTickRate(QuantumConfig.realSlowTickRate());
        slowedServer = server;
        s.realSlow = true;
    }

    private static void realSlowEnd(ServerPlayer player, QuantumCoreState s) {
        if (!s.realSlow) return;
        s.realSlow = false;
        restoreTickRate(player.server);
    }

    /** Puts the tick rate back (end of the field, logout, server stop). */
    static void restoreTickRate(@Nullable MinecraftServer server) {
        if (server == null || slowedServer != server) return;
        server.tickRateManager().setTickRate(slowedFrom);
        slowedServer = null;
    }
}
