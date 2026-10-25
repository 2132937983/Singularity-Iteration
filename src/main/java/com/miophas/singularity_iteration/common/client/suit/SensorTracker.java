// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.client.suit;

import com.miophas.singularity_iteration.common.suit.SuitSensorData;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import com.miophas.singularity_iteration.common.fcs.DeadReckoning;
import com.miophas.singularity_iteration.common.fcs.TargetTracker;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.NeutralMob;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.monster.EnderMan;
import net.minecraft.world.entity.monster.Ghast;
import net.minecraft.world.entity.monster.Guardian;
import net.minecraft.world.entity.monster.Pillager;
import net.minecraft.world.entity.monster.SpellcasterIllager;
import net.minecraft.world.entity.monster.Vex;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * Sensor picture for one client tick. The ESP unit, the behaviour predictor unit and the blast
 * warning unit share it. It holds the creatures around the wearer, their velocity, their predicted
 * path, their attack intent, and the explosives that count down.
 *
 * <p><b>Update rate.</b> {@code FcsClient} calls {@link #tick} one time per client tick (20 Hz).
 * Renderers read {@link #tracks()} and {@link #blasts()} in each frame. The lists are immutable
 * snapshots. A tick replaces them and does not change them.
 *
 * <p><b>Why on the client.</b> All inputs are on the client already. The client has the entity
 * positions and the synced animation flags (creeper swell, ghast charge, spell cast and so on).
 * Thus the tracker needs no packets and puts no load on the server. The output is for the HUD of
 * this client only.
 *
 * <p><b>Ranges.</b> The entity query uses an axis-aligned box around the player, not a sphere.
 * Thus a corner of the box is at up to range x 1.73 from the player. Path and intent need the
 * entity to be in a sphere of {@link #PREDICT_RANGE}.
 *
 * <p><b>Thread.</b> Call all methods on the client thread only.
 */
public final class SensorTracker {
    private SensorTracker() {}

    /** Half size of the ESP query box, in blocks. The tracker uses it while the ESP unit is on. */
    public static final double ESP_RANGE = 32;
    /** 0.1.10.1: the same for a Mk.II ESP unit (+50 %). */
    public static final double ESP_RANGE_MK2 = 48;

    /** 0.1.10.1: the ESP range of the local wearer (Mk.II: {@link #ESP_RANGE_MK2}). */
    public static double espRange() {
        var p = net.minecraft.client.Minecraft.getInstance().player;
        return p != null && com.miophas.singularity_iteration.common.suit.SuitModules.tier(p,
            com.miophas.singularity_iteration.common.suit.SuitModuleType.ENTITY_ESP) >= 2 ? ESP_RANGE_MK2 : ESP_RANGE;
    }
    /**
     * Predictor range in blocks. It is the half size of the query box when ESP is off. It is also
     * the sphere radius in which the tracker calculates path and intent.
     */
    public static final double PREDICT_RANGE = 24;
    /** Half size of the explosive query box, in blocks. */
    public static final double BLAST_RANGE = 28;
    /** Dead reckoning horizon in ticks (20 ticks = 1 second). */
    public static final int PREDICT_TICKS = 20;
    /** Maximum number of tracks per tick: the nearest creatures win. */
    static final int MAX_TRACKS = 64;
    /** 0.1.10.20 (P8): a predicted path is computed again after this many ticks; in between it moves with its creature. */
    static final int PATH_INTERVAL = 2;

    /** Classification of a creature. It sets the ESP colour. */
    public enum Kind { HOSTILE, NEUTRAL, PASSIVE, PLAYER }

    /**
     * One tracked creature.
     *
     * @param entity    the creature.
     * @param kind      the class of the creature.
     * @param velocity  the estimated velocity in blocks per tick (see {@link #velocity}).
     * @param path      the predicted positions, every 2 ticks for {@link #PREDICT_TICKS} ticks.
     *                  The first point is the current position. The list is empty when the
     *                  predictor unit is off, when the creature is out of {@link #PREDICT_RANGE},
     *                  or when the creature does not move.
     * @param intent    the short intent code from {@link #intent}, or null.
     * @param intentHot true when the intent is urgent: the server marks the creature as threat
     *                  level 2, or a creeper is about to explode ("DET").
     */
    public record Track(LivingEntity entity, Kind kind, Vec3 velocity, List<Vec3> path, @Nullable String intent, boolean intentHot) { }

    /**
     * One explosive that counts down.
     *
     * @param entity  the explosive entity.
     * @param seconds the time to the explosion, in seconds; -1 for a charge without a timer
     *                (0.1.10.5: a remote charge, an explosive of another mod without a fuse).
     * @param radius  the explosion power. The HUD uses it as the danger radius in blocks.
     * @param label   the HUD label.
     * @param fuse    the full fuse length in seconds. {@link #burnt()} uses it.
     */
    public record Blast(Entity entity, float seconds, float radius, String label, float fuse) {
        /** Burnt part of the fuse, from 0 (just lit) to 1 (explosion). It is 1 when {@code fuse <= 0}, 0 without a timer. */
        public float burnt() {
            if (!timed()) return 0F;
            return fuse <= 0 ? 1F : Math.max(0F, Math.min(1F, 1F - seconds / fuse));
        }

        /** 0.1.10.5: false for a charge without a timer. */
        public boolean timed() { return seconds >= 0; }

        /** 0.1.10.5: less than a second to go (the HUD flashes red). */
        public boolean urgent() { return timed() && seconds < 1; }

        /** 0.1.10.5: the countdown text, "--" without a timer. */
        public String secondsText() { return timed() ? String.format(java.util.Locale.ROOT, "%.1f", seconds) : "--"; }
    }

    /**
     * 0.1.10.20: the memory of one tracked creature: its motion estimate ({@link TargetTracker}: the three-step
     * velocity of 0.1.7.36 and the turn rate of a circling creature) and its last computed path.
     */
    private static final class Memory {
        final TargetTracker tracker = new TargetTracker();
        List<Vec3> path = List.of();
        Vec3 pathOrigin;
        long pathTick = Long.MIN_VALUE;
    }

    /**
     * The memory per entity id. The tracker keeps only the entities that it saw in the last tick. Thus the map has
     * at most 64 entries.
     */
    private static final Int2ObjectOpenHashMap<Memory> memory = new Int2ObjectOpenHashMap<>();
    /** 0.1.10.20 (P8): block answers of this tick per cell, shared by all paths (cleared every tick). */
    private static final LevelBlocks blocks = new LevelBlocks();
    private static final DeadReckoning.Cache blockCache = new DeadReckoning.Cache(blocks);
    private static long ticks;
    private static List<Track> tracks = List.of();
    private static List<Blast> blasts = List.of();
    /** The level of the last tick. A new level clears all state, because entity ids are per level. */
    private static ClientLevel boundLevel;

    /** Tracked creatures of the last tick, nearest first. At most 64 entries. */
    public static List<Track> tracks() { return tracks; }
    /** Explosives of the last tick, soonest explosion first. */
    public static List<Blast> blasts() { return blasts; }

    /** Removes all tracks, explosives and position history. */
    public static void clear() {
        memory.clear();
        blockCache.clear();
        tracks = List.of();
        blasts = List.of();
        BlastDetector.invalidate();
    }

    /** Classifies an entity. The order of the tests is important: a player is never HOSTILE. */
    public static Kind kind(Entity e) {
        if (e instanceof Player) return Kind.PLAYER;
        if (e instanceof Enemy) return Kind.HOSTILE;
        if (e instanceof NeutralMob) return Kind.NEUTRAL;
        return Kind.PASSIVE;
    }

    /** ESP colour of a kind, as 0xRRGGBB (no alpha). */
    public static int color(Kind kind) {
        return switch (kind) {
            case HOSTILE -> 0xFF4A3D;
            case NEUTRAL -> 0xFFD23F;
            case PASSIVE -> 0x7CFFB2;
            case PLAYER -> 0x6FE6FF;
        };
    }

    /**
     * Updates the sensor picture. Call it one time per client tick.
     *
     * <p>When ESP and predictor are both off, the tracker removes all tracks and the history.
     * Then the velocity estimate starts again from zero when a unit comes on.
     *
     * <p>The tracker skips invisible creatures that do not glow. It sorts the candidates by
     * distance first and keeps the nearest {@link #MAX_TRACKS}. Only these get the expensive
     * path and intent work (0.1.7.36: before, the cap came before the sort, so a far creature
     * could take the place of a near one).
     *
     * @param level   the client level. It must not be null.
     * @param player  the local player.
     * @param esp     true when the ESP unit is on.
     * @param predict true when the behaviour predictor unit is on.
     * @param blast   true when the blast warning unit is on.
     */
    public static void tick(ClientLevel level, LocalPlayer player, boolean esp, boolean predict, boolean blast) {
        if (level != boundLevel) { clear(); boundLevel = level; }
        ticks++;
        blocks.level = level;
        blockCache.clear();
        if (!esp && !predict) {
            tracks = List.of();
            memory.clear();
        } else {
            double range = esp ? espRange() : PREDICT_RANGE;
            // the value is the feet position of the player
            Vec3 eye = player.position();
            List<LivingEntity> candidates = level.getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(range),
                e -> e != player && e.isAlive() && !e.isSpectator() && (!e.isInvisible() || e.isCurrentlyGlowing()));
            candidates.sort((a, b) -> Double.compare(a.distanceToSqr(eye), b.distanceToSqr(eye)));
            int count = Math.min(MAX_TRACKS, candidates.size());
            List<Track> out = new ArrayList<>(count);
            Int2ObjectOpenHashMap<Memory> seen = new Int2ObjectOpenHashMap<>(count);
            double[] vel = new double[3];
            for (int i = 0; i < count; i++) {
                LivingEntity e = candidates.get(i);
                Memory m = memory.get(e.getId());
                if (m == null) m = new Memory();
                Vec3 at = e.position();
                m.tracker.update(at.x, at.y, at.z);
                seen.put(e.getId(), m);
                m.tracker.velocity(vel);
                Vec3 v = new Vec3(vel[0], vel[1], vel[2]);
                boolean near = e.distanceToSqr(player) <= PREDICT_RANGE * PREDICT_RANGE;
                List<Vec3> path = predict && near ? path(e, m, v) : List.of();
                String intent = predict && near ? intent(e, player) : null;
                boolean hot = intent != null && (SuitSensorData.threatLevel(e.getId()) == 2 || intent.equals("DET"));
                out.add(new Track(e, kind(e), v, path, intent, hot));
            }
            // keep the memory of the creatures seen in this tick only: this bounds the memory
            memory.clear();
            memory.putAll(seen);
            tracks = out;                       // already nearest first
        }
        blasts = blast ? scanBlasts(level, player) : List.of();
    }


    /**
     * Dead reckoning for {@link #PREDICT_TICKS} ticks (1 second).
     *
     * <p>Model for each tick:
     * <ul>
     *   <li>On the ground: the velocity stays constant.</li>
     *   <li>In the air (and gravity applies): {@code vel.y = (vel.y - 0.08) * 0.98} and
     *       {@code vel.xz *= 0.91}. These are the vanilla creature gravity, vertical air drag and
     *       horizontal air drag (0.1.7.36: before, 0.98 also applied to the horizontal speed).</li>
     *   <li>Wall: the path stops when the block at the new position, 0.3 blocks above the feet,
     *       has a collision shape.</li>
     *   <li>Landing: when the creature falls and the block 0.05 blocks below the new feet position
     *       is not air, the creature is on the ground. The vertical speed becomes 0. A creature on
     *       the ground stays on the ground for the rest of the path. It does not fall off edges.</li>
     * </ul>
     * The method stores every second point. Thus the result has at most 11 points.
     *
     * <p>0.1.10.20: the rules live in {@code common.fcs.DeadReckoning} (with block answers cached per cell for the
     * tick); a circling creature gets an arc ({@code TargetTracker#turnRate}); the path is computed every
     * {@link #PATH_INTERVAL} ticks and moves with the creature in between.
     *
     * @return the predicted positions, or an empty list when the horizontal speed is below 0.02
     *         blocks per tick and the vertical speed is below 0.02 blocks per tick.
     */
    private static List<Vec3> path(LivingEntity e, Memory m, Vec3 v) {
        // 0.1.10.20 (P8): computed every PATH_INTERVAL ticks; in between the last path moves with the creature
        if (m.pathOrigin != null && ticks - m.pathTick < PATH_INTERVAL) {
            Vec3 shift = e.position().subtract(m.pathOrigin);
            if (shift.lengthSqr() < 1.0E-10) return m.path;
            List<Vec3> moved = new ArrayList<>(m.path.size());
            for (Vec3 point : m.path) moved.add(point.add(shift));
            return moved;
        }
        Vec3 p = e.position();
        double[] flat = DeadReckoning.path(blockCache, p.x, p.y, p.z, v.x, v.y, v.z, e.onGround(), e.isNoGravity(), PREDICT_TICKS,
            m.tracker.turnRate());
        List<Vec3> pts = new ArrayList<>(flat.length / 3);
        for (int i = 0; i < flat.length; i += 3) pts.add(new Vec3(flat[i], flat[i + 1], flat[i + 2]));
        m.path = List.copyOf(pts);
        m.pathOrigin = p;
        m.pathTick = ticks;
        return m.path;
    }

    /** 0.1.10.20: the client level as the predictor sees it (collision shape, air). */
    private static final class LevelBlocks implements DeadReckoning.Blocks {
        ClientLevel level;
        private final BlockPos.MutableBlockPos at = new BlockPos.MutableBlockPos();

        @Override
        public boolean solid(int x, int y, int z) {
            at.set(x, y, z);
            return !level.getBlockState(at).getCollisionShape(level, at).isEmpty();
        }

        @Override
        public boolean air(int x, int y, int z) {
            at.set(x, y, z);
            return level.getBlockState(at).isAir();
        }
    }

    /**
     * Short attack-intent code from the synced animation state. The server syncs these flags to all
     * clients for the vanilla animations. Thus the intent costs no packets.
     *
     * <p>Codes: DET (creeper swells), FIRE (ghast charges), BEAM (guardian has a beam target),
     * CAST (illager casts a spell), DIVE (vex charges), AIM (pillager loads a crossbow, or any
     * creature uses an item), AGGRO (enderman is angry), ATK (aggressive mob nearer than 3 blocks to
     * the wearer), ENGAGE (aggressive mob at 3 blocks or more). The first test that matches wins.
     *
     * @param e      the creature.
     * @param wearer the player that wears the suit. The ATK and ENGAGE codes use its distance.
     * @return the code, or null when the creature shows no intent.
     */
    @Nullable
    public static String intent(LivingEntity e, Player wearer) {
        if (e instanceof Creeper creeper && creeper.getSwelling(1.0F) > 0.02F) return "DET";
        if (e instanceof Ghast ghast && ghast.isCharging()) return "FIRE";
        if (e instanceof Guardian guardian && guardian.hasActiveAttackTarget()) return "BEAM";
        if (e instanceof SpellcasterIllager caster && caster.isCastingSpell()) return "CAST";
        if (e instanceof Vex vex && vex.isCharging()) return "DIVE";
        if (e instanceof Pillager pillager && pillager.isChargingCrossbow()) return "AIM";
        if (e instanceof EnderMan enderman && enderman.isCreepy()) return "AGGRO";
        if (e.isUsingItem()) return "AIM";
        if (e instanceof Mob mob && mob.isAggressive()) {
            double d = mob.distanceToSqr(wearer);
            return d < 9 ? "ATK" : "ENGAGE";
        }
        return null;
    }

    /**
     * Finds the explosives near the player (0.1.10.5: {@link BlastDetector} has the rules; before,
     * only vanilla TNT, TNT minecarts and creepers counted).
     *
     * <p>TNT and TNT minecarts: the time is the fuse in ticks / 20. The full fuse is 4 seconds
     * (80 ticks, the vanilla default). The power is 4.
     *
     * <p>Creepers: the swell value goes from 0 to 1 during the vanilla swell time of 30 ticks. The
     * time left is {@code (1 - swell) * 30 / 20} seconds. Thus the full fuse is 1.5 seconds. The
     * power is 3, or 6 for a charged creeper.
     *
     * @return the explosives, soonest explosion first, then the ones without a timer.
     */
    private static List<Blast> scanBlasts(ClientLevel level, LocalPlayer player) {
        return BlastDetector.scan(level, player);
    }
}
