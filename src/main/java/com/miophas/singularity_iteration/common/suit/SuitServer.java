// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.suit;

import com.miophas.singularity_iteration.common.Singularity_Iteration;
import com.miophas.singularity_iteration.common.blockentity.wiring.terminal.EnergyNetworkSnapshot;
import com.miophas.singularity_iteration.common.blockentity.wiring.terminal.NetworkWalker;
import com.miophas.singularity_iteration.core.prefab.blockentity.AbstractEnergyBlockEntity;
import com.miophas.singularity_iteration.core.runtime.energy.grid.EnergyNetGlobal;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.NeutralMob;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/** Server side of the upgrade units: power drain, threat sensor, deflector, chunk grid read-out. */
@EventBusSubscriber(modid = Singularity_Iteration.MOD_ID)
public final class SuitServer {
    private SuitServer() {}

    /** Threat sensor range and update period. */
    public static final double THREAT_RANGE = 32.0;
    public static final int THREAT_INTERVAL = 5;
    /** cos(15 deg): a creature whose view axis is this close to the wearer "looks at" the wearer. */
    public static final double LOOK_COS = 0.966;

    private static final Map<UUID, Boolean> threatSent = new HashMap<>();
    private static final Map<UUID, Long> gridAsked = new HashMap<>();

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        int age = player.tickCount;
        if (age % SuitModules.DRAIN_INTERVAL == 0) SuitModules.drain(player);
        if (age % THREAT_INTERVAL == 0) tickThreats(player);
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        threatSent.remove(event.getEntity().getUUID());
        gridAsked.remove(event.getEntity().getUUID());
    }

    // ------------------------------------------------------------------ threat sensor

    private static void tickThreats(ServerPlayer player) {
        boolean active = SuitModules.isActive(player, SuitModuleType.THREAT_SENSOR);
        if (!active) {
            if (threatSent.remove(player.getUUID()) != null) SuitPackets.send(player, new SuitPackets.Threats(new int[0], new byte[0]));
            return;
        }
        IntArrayList ids = new IntArrayList();
        it.unimi.dsi.fastutil.bytes.ByteArrayList levels = new it.unimi.dsi.fastutil.bytes.ByteArrayList();
        for (LivingEntity other : player.serverLevel().getEntitiesOfClass(LivingEntity.class,
                player.getBoundingBox().inflate(THREAT_RANGE), e -> e != player && e.isAlive() && !e.isSpectator())) {
            int level = threatLevel(other, player);
            if (level > 0 && ids.size() < 256) { ids.add(other.getId()); levels.add((byte) level); }
        }
        threatSent.put(player.getUUID(), Boolean.TRUE);
        SuitPackets.send(player, new SuitPackets.Threats(ids.toIntArray(), levels.toByteArray()));
    }

    /**
     * 2 (red): a mob targets the wearer, or a hostile mob looks at the wearer.
     * 1 (yellow): a player or a neutral mob looks at the wearer.
     */
    public static int threatLevel(LivingEntity other, Player wearer) {
        if (other instanceof Mob mob && mob.getTarget() == wearer) return 2;
        if (other.distanceToSqr(wearer) > THREAT_RANGE * THREAT_RANGE) return 0;
        boolean hostile = other instanceof Enemy;
        boolean watcherKind = hostile || other instanceof Player || other instanceof NeutralMob;
        if (!watcherKind || !looksAt(other, wearer)) return 0;
        return hostile ? 2 : 1;
    }

    public static boolean looksAt(LivingEntity watcher, Entity target) {
        Vec3 eye = watcher.getEyePosition();
        Vec3 to = target.getEyePosition().subtract(eye);
        double len = to.length();
        if (len < 1.0E-3) return true;
        double dot = watcher.getViewVector(1.0F).dot(to.scale(1.0 / len));
        // close up the cone is wider: the target fills more of the view
        double cos = len < 4 ? 0.85 : LOOK_COS;
        return dot >= cos && watcher.hasLineOfSight(target);
    }

    // ------------------------------------------------------------------ deflector

    /** Runs after the armour handler (normal priority) has taken its share. */
    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onDamage(LivingDamageEvent.Pre event) {
        if (!(event.getEntity() instanceof Player player) || player.level().isClientSide) return;
        if (!SuitModules.isActive(player, SuitModuleType.DEFLECTOR)) return;
        DamageSource source = event.getSource();
        if (source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) return;
        boolean explosion = source.is(DamageTypeTags.IS_EXPLOSION);
        if (!explosion && (source.is(DamageTypeTags.BYPASSES_ARMOR) || source.is(DamageTypeTags.BYPASSES_EFFECTS))) return;
        float original = event.getOriginalDamage();
        float damage = event.getNewDamage();
        var piece = SuitModules.wornPieceWith(player, SuitModuleType.DEFLECTOR);
        float cancel = Math.min(Math.max(0, damage), SuitModules.DEFLECTOR_MAX_PER_HIT);
        cancel = Math.min(cancel, SuitModules.energy(piece) / (float) SuitModules.DEFLECTOR_EU_PER_DAMAGE);
        if (cancel > 0) {
            SuitModules.extract(piece, (long) Math.ceil(cancel * SuitModules.DEFLECTOR_EU_PER_DAMAGE));
            event.setNewDamage(damage - cancel);
        }
        float stopped = original - event.getNewDamage();
        if (stopped <= 0) return;
        Vec3 dir = hitDirection(player, source);
        float strength = Math.min(1.0F, 0.25F + stopped / 10.0F);
        var hit = new SuitPackets.ShieldHit(player.getId(), (float) dir.x, (float) dir.y, (float) dir.z, strength);
        // the wearer and everyone close enough to see the effect (channel-checked: no fake players)
        for (ServerPlayer viewer : ((ServerLevel) player.level()).players()) {
            if (viewer.distanceToSqr(player) <= 96 * 96) SuitPackets.send(viewer, hit);
        }
    }

    /** Unit vector from the wearer's centre towards the hit. */
    static Vec3 hitDirection(Player player, DamageSource source) {
        Vec3 centre = player.position().add(0, player.getBbHeight() * 0.55, 0);
        Vec3 from = source.getSourcePosition();
        if (from == null && source.getDirectEntity() != null) from = source.getDirectEntity().position();
        if (from == null && source.getEntity() != null) from = source.getEntity().getEyePosition();
        Vec3 dir = from == null ? player.getViewVector(1.0F) : from.subtract(centre);
        if (dir.lengthSqr() < 1.0E-6) dir = player.getViewVector(1.0F);
        return dir.normalize();
    }

    // ------------------------------------------------------------------ chunk grid read-out

    public static void answerChunkGrid(ServerPlayer player) {
        if (!SuitModules.isActive(player, SuitModuleType.GRID_TELEMETRY)) return;
        long now = player.serverLevel().getGameTime();
        Long last = gridAsked.get(player.getUUID());
        if (last != null && now - last < 15 && now >= last) return;
        gridAsked.put(player.getUUID(), now);
        SuitPackets.send(player, chunkGrid(player.serverLevel(), player.chunkPosition()));
    }

    public static SuitPackets.ChunkGrid chunkGrid(ServerLevel level, ChunkPos pos) {
        LevelChunk chunk = level.getChunkSource().getChunkNow(pos.x, pos.z);
        int gens = 0, cons = 0, stores = 0, cables = 0, over = 0;
        double generated = 0, consumed = 0, maxVoltage = 0;
        long stored = 0, capacity = 0;
        if (chunk != null) {
            int scanned = 0;
            for (BlockEntity be : chunk.getBlockEntities().values()) {
                if (++scanned > 2048) break;
                if (NetworkWalker.isConductor(be)) { cables++; continue; }
                EnergyNetworkSnapshot.Category category = NetworkWalker.categorize(be);
                if (category == null) continue;
                var stats = EnergyNetGlobal.getCurrentTickNodeStats(level, be.getBlockPos());
                double in = stats == null ? 0 : stats.getEnergyIn(), out = stats == null ? 0 : stats.getEnergyOut();
                double voltage = stats == null ? 0 : stats.getVoltage();
                maxVoltage = Math.max(maxVoltage, voltage);
                switch (category) {
                    case GENERATOR -> { gens++; generated += out; }
                    case CONSUMER -> { cons++; consumed += in; }
                    case STORAGE -> stores++;
                    case TRANSFORMER -> { }
                }
                if (be instanceof AbstractEnergyBlockEntity machine) {
                    stored += machine.getEnergyStorage().getAmount();
                    capacity += machine.getEnergyStorage().getCapacity();
                    if (category == EnergyNetworkSnapshot.Category.CONSUMER && voltage > 0) {
                        int sink = machine.getSinkTier();
                        if (sink < Integer.MAX_VALUE / 2 && voltage > EnergyNetGlobal.getPowerFromTier(sink)) over++;
                    }
                }
            }
        }
        return new SuitPackets.ChunkGrid(pos.x, pos.z, gens, cons, stores, cables, (float) generated, (float) consumed,
            (float) maxVoltage, stored, capacity, over);
    }
}
