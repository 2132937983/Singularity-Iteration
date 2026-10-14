// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.suit.quantum;

import com.miophas.singularity_iteration.common.armory.WingmanEntity;
import com.miophas.singularity_iteration.common.armory.WingmanService;
import com.miophas.singularity_iteration.common.registry.mio_icif_sounds;
import com.miophas.singularity_iteration.common.suit.SuitPackets;
import com.miophas.singularity_iteration.common.suit.SuitRegistry;
import com.miophas.singularity_iteration.common.suit.nano.NanoPackets;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * Server side of the entanglement synergies and the chain combos (0.1.10.1). The client-only
 * synergies (Threat Sight, Ore Hologram, Ballistic Sync, Blast Forecast) live in the HUD and
 * effect code; Predictive Guidance lives in {@code SmartGuidance}.
 *
 * <ul>
 *   <li><b>Resonant Deflection</b>: {@link QuantumConfig#resonanceShare()} of every deflected hit
 *       charges the resonance; at {@link QuantumConfig#resonanceThreshold()} it bursts: the stored
 *       charge as damage to the hostile creatures within {@link QuantumConfig#resonanceRadius()}
 *       blocks, with a push.</li>
 *   <li><b>Afterimage Decoy</b>: a blink leaves a decoy at the start. For
 *       {@link QuantumConfig#decoyTicks()} ticks the creatures that hunted the wearer within 24
 *       blocks lose the wearer, walk to the decoy and cannot take the wearer back.</li>
 *   <li><b>Phase Counter</b>: the damage that passed through during a phase shift (at most
 *       {@link QuantumConfig#phaseCounterMax()}) returns as a shock wave when it ends.</li>
 *   <li><b>Fleet Datalink</b>: every 5 ticks the creature in the wearer's sights (6 degrees, 64
 *       blocks) is marked and the Wingmen take it first; every 10 ticks the hostile creatures within
 *       24 blocks of each Wingman go to the wearer's HUD.</li>
 *   <li>Combos: <b>Quantum Tunnel</b> (a phase shift right after a blink costs half),
 *       <b>Frozen Volley</b> (a volley inside one's own time dilation: no charge, more damage, the
 *       slowed creatures become targets), <b>Static Barrage</b> (a force field switched off by the
 *       key throws the held projectiles back at their shooters).</li>
 * </ul>
 */
public final class QuantumSynergies {
    private QuantumSynergies() {}

    public static final int COMBO_TUNNEL = 0, COMBO_FROZEN_VOLLEY = 1, COMBO_STATIC_BARRAGE = 2;
    static final double DECOY_RANGE = 24, DATALINK_RANGE = 24, MARK_RANGE = 64;

    // ------------------------------------------------------------------ Resonant Deflection

    /** Called from the deflector damage handler with the damage it stopped. */
    public static void deflected(ServerPlayer player, float stopped) {
        if (stopped <= 0 || !QuantumLinks.active(player, QuantumSynergy.RESONANT_DEFLECTION)) return;
        QuantumCoreState s = QuantumCore.state(player);
        s.resonance = Math.min(100, s.resonance + stopped * (float) QuantumConfig.resonanceShare());
        if (s.resonance >= QuantumConfig.resonanceThreshold()) burst(player, s);
    }

    static void burst(ServerPlayer player, QuantumCoreState s) {
        float damage = s.resonance;
        s.resonance = 0;
        ServerLevel level = player.serverLevel();
        double r = QuantumConfig.resonanceRadius();
        Vec3 c = player.position().add(0, 0.3, 0);
        IntArrayList hit = new IntArrayList();
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(r), e -> e.distanceToSqr(c) <= r * r)) {
            if (!QuantumActions.affects(e, player, QuantumConfig.synergyVsPlayers())) continue;
            e.hurt(player.damageSources().playerAttack(player), damage);
            Vec3 away = e.position().subtract(player.position());
            e.knockback(0.8, -away.x, -away.z);
            if (hit.size() < NanoPackets.Fx.MAX_TARGETS) hit.add(e.getId());
        }
        NanoPackets.broadcast(player, new NanoPackets.Fx(player.getId(), QuantumPackets.FX_RESONANCE, (float) c.x, (float) c.y, (float) c.z,
            (float) r, damage, 0, hit.toIntArray(), new float[0]), 96);
        level.playSound(null, c.x, c.y, c.z, mio_icif_sounds.RESONANCE_BURST.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
    }

    // ------------------------------------------------------------------ Afterimage Decoy

    /** Called by the blink with its start point. */
    static void decoy(ServerPlayer player, QuantumCoreState s, Vec3 at) {
        if (!QuantumLinks.active(player, QuantumSynergy.AFTERIMAGE_DECOY)) return;
        releaseDecoy(player, s);
        s.decoy = at;
        s.decoyTicks = QuantumConfig.decoyTicks();
        for (Mob mob : player.serverLevel().getEntitiesOfClass(Mob.class, player.getBoundingBox().inflate(DECOY_RANGE),
                m -> m.getTarget() == player && m instanceof Enemy)) {
            mob.setTarget(null);
            s.decoyed.add(mob.getId());
        }
        NanoPackets.broadcast(player, new NanoPackets.Fx(player.getId(), QuantumPackets.FX_DECOY, (float) at.x, (float) at.y, (float) at.z,
            s.decoyTicks, 0, 0, s.decoyed.toIntArray(), new float[0]), 128);
    }

    /** Each tick of a decoy: the drawn creatures walk to it; then they may hunt again. */
    static void tickDecoy(ServerPlayer player, QuantumCoreState s) {
        if (s.decoyTicks <= 0) {
            if (!s.decoyed.isEmpty()) releaseDecoy(player, s);
            return;
        }
        if (--s.decoyTicks <= 0) {
            releaseDecoy(player, s);
            return;
        }
        if (s.decoyTicks % 10 != 0 || s.decoy == null) return;
        ServerLevel level = player.serverLevel();
        for (int id : s.decoyed) {
            if (level.getEntity(id) instanceof Mob mob && mob.isAlive()) {
                if (mob.getTarget() == player) mob.setTarget(null);
                mob.getNavigation().moveTo(s.decoy.x, s.decoy.y, s.decoy.z, 1.1);
            }
        }
    }

    static void releaseDecoy(ServerPlayer player, QuantumCoreState s) {
        boolean was = s.decoyTicks > 0 || !s.decoyed.isEmpty();
        s.decoyTicks = 0;
        s.decoyed.clear();
        s.decoy = null;
        if (was) NanoPackets.broadcast(player, NanoPackets.Fx.simple(player, QuantumPackets.FX_DECOY_END, 0, 0, 0), 128);
    }

    /** True when {@code mob} is drawn by a decoy of {@code target} (it may not take the target). */
    public static boolean drawnByDecoy(Mob mob, ServerPlayer target) {
        QuantumCoreState s = target.getExistingDataOrNull(SuitRegistry.QUANTUM_CORE);
        return s != null && s.decoyTicks > 0 && s.decoyed.contains(mob.getId());
    }

    // ------------------------------------------------------------------ Phase Counter

    /** Damage that passed through a phase shift. */
    static void absorbed(ServerPlayer player, QuantumCoreState s, float amount) {
        if (amount > 0 && QuantumLinks.active(player, QuantumSynergy.PHASE_COUNTER)) {
            s.phaseAbsorbed = Math.min(QuantumConfig.phaseCounterMax(), s.phaseAbsorbed + amount);
        }
    }

    // ------------------------------------------------------------------ Fleet Datalink

    /**
     * Every tick: the mark (every 5 ticks, Fleet Datalink only) and the Wingman sightings (every 10
     * ticks). 0.1.10.2: the sightings also flow with the data link of the quantum command (an active
     * entanglement relay and the data link setting on), without the mark.
     */
    static void tickDatalink(ServerPlayer player, QuantumCoreState s) {
        boolean fdl = QuantumLinks.active(player, QuantumSynergy.FLEET_DATALINK);
        boolean on = fdl || com.miophas.singularity_iteration.common.armory.WingmanCommands.datalink(player);
        if (!fdl && s.markedTarget != -1) s.markedTarget = -1;
        if (!on) {
            if (s.markedTarget != -1) {
                s.markedTarget = -1;
                SuitPackets.send(player, new QuantumPackets.DataLink(-1, new int[0]));
            }
            return;
        }
        if (fdl && player.tickCount % 5 == 0) {
            LivingEntity aimed = aimed(player);
            s.markedTarget = aimed == null ? -1 : aimed.getId();
        }
        if (player.tickCount % 10 != 0) return;
        IntOpenHashSet seen = new IntOpenHashSet();
        for (WingmanEntity w : WingmanService.of(player)) {
            if (w.level() != player.level() || w.state() != WingmanEntity.ACTIVE) continue;
            for (LivingEntity e : w.level().getEntitiesOfClass(LivingEntity.class, w.getBoundingBox().inflate(DATALINK_RANGE),
                    e -> e instanceof Enemy && e.isAlive() && !e.isInvisible())) {
                seen.add(e.getId());
                if (seen.size() >= 64) break;
            }
        }
        SuitPackets.send(player, new QuantumPackets.DataLink(s.markedTarget, seen.toIntArray()));
    }

    /** The creature in the wearer's sights for the Fleet Datalink mark, or null. */
    @Nullable
    private static LivingEntity aimed(ServerPlayer player) {
        Vec3 eye = player.getEyePosition(), look = player.getViewVector(1.0F);
        Vec3 end = eye.add(look.scale(MARK_RANGE));
        var block = player.level().clip(new ClipContext(eye, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        if (block.getType() != HitResult.Type.MISS) end = block.getLocation();
        EntityHitResult hit = ProjectileUtil.getEntityHitResult(player.level(), player, eye, end, new AABB(eye, end).inflate(2.0),
            e -> e instanceof LivingEntity l && l.isAlive() && QuantumActions.affects(l, player, false), 1.2F);
        return hit != null && hit.getEntity() instanceof LivingEntity l ? l : null;
    }

    /** The Fleet Datalink mark of {@code owner}, when it is a valid creature, or null (Wingman targeting). */
    @Nullable
    public static LivingEntity mark(ServerPlayer owner) {
        QuantumCoreState s = owner.getExistingDataOrNull(SuitRegistry.QUANTUM_CORE);
        if (s == null || s.markedTarget < 0) return null;
        Entity e = owner.level().getEntity(s.markedTarget);
        return e instanceof LivingEntity l && l.isAlive() ? l : null;
    }

    // ------------------------------------------------------------------ combos

    /** Quantum Tunnel: a blink happened within the window. */
    static boolean tunnel(ServerPlayer player, QuantumCoreState s) {
        return player.level().getGameTime() - s.lastBlink <= QuantumConfig.tunnelWindow();
    }

    /** Shows a combo to the wearer and the players near (banner, flash, sound). */
    static void combo(ServerPlayer player, int combo) {
        NanoPackets.broadcast(player, NanoPackets.Fx.simple(player, QuantumPackets.FX_COMBO, combo, 0, 0), 64);
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(), mio_icif_sounds.COMBO.get(), SoundSource.PLAYERS, 0.9F,
            1.0F + combo * 0.08F);
    }
}
