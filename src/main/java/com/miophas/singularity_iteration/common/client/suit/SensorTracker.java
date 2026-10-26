// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.client.suit;

import com.miophas.singularity_iteration.common.suit.SuitSensorData;
import com.miophas.singularity_iteration.common.suit.TargetRating;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import com.miophas.singularity_iteration.common.fcs.AttackTimer;
import com.miophas.singularity_iteration.common.fcs.DeadReckoning;
import com.miophas.singularity_iteration.common.fcs.PathFollower;
import com.miophas.singularity_iteration.common.fcs.TargetPriority;
import com.miophas.singularity_iteration.common.fcs.TargetTracker;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.CrossbowItem;
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
    /**
     * 0.1.10.23 (P5): the path interval of a far creature (beyond {@link #NEAR_RANGE}, not after the wearer, not in the
     * top three ranks). In the offline test this cut the path computations of an even crowd to 63 %; such a path moved
     * between refreshes stays within 0.6 blocks of a fresh one (0.58 for a zigzag walker, 0.29 circling, 0 straight).
     */
    static final int FAR_PATH_INTERVAL = 4;
    /** 0.1.10.23 (P5): creatures nearer than this keep {@link #PATH_INTERVAL}. */
    static final double NEAR_RANGE = 12;
    /** 0.1.10.23 (P6): ranks shown on the tags ("#1" to "#3"). */
    static final int SHOWN_RANKS = 3;

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
     * @param countdown 0.1.10.23 (P7): ticks to its attack ({@code fcs.AttackTimer}: bow draw, crossbow load, ghast
     *                  charge, creeper swell, learned melee swing), or -1.
     * @param rank      0.1.10.23 (P6): 1 to {@link #SHOWN_RANKS} for the highest {@code fcs.TargetPriority} scores of
     *                  the hostile creatures in the predictor range, else 0.
     * @param linked    0.1.10.23 (P2): the path follows the path nodes the server sent (Behavior Predictor Mk.II).
     */
    public record Track(LivingEntity entity, Kind kind, Vec3 velocity, List<Vec3> path, @Nullable String intent, boolean intentHot,
                        int countdown, int rank, boolean linked) {
        /** 0.1.10.23: the intent with its countdown ("AIM 0.6s"), or the intent alone. */
        @Nullable
        public String intentText() {
            if (intent == null) return null;
            return countdown >= 0 ? String.format(java.util.Locale.ROOT, "%s %.1fs", intent, countdown / 20F) : intent;
        }
    }

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
        boolean linked;
        /** 0.1.10.23 (P2): the motion it keeps under its own power (its velocity while it is not hurt). */
        double driveX, driveZ;
        boolean driveKnown;
        /** 0.1.10.23 (P7): its learned melee swing interval and the rise of a ghast's charge flag. */
        final AttackTimer.Swings swings = new AttackTimer.Swings();
        final AttackTimer.Edge charge = new AttackTimer.Edge();
    }

    /** 0.1.10.23 (P6): the priority score of each tracked creature in the last tick. */
    private static final it.unimi.dsi.fastutil.ints.Int2DoubleOpenHashMap priorities = new it.unimi.dsi.fastutil.ints.Int2DoubleOpenHashMap();

    /** 0.1.10.23 (P6): the priority score of a creature tracked in the last tick, or NaN. */
    public static double priority(int entityId) {
        return priorities.containsKey(entityId) ? priorities.get(entityId) : Double.NaN;
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
        priorities.clear();
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
            priorities.clear();
        } else {
            double range = esp ? espRange() : PREDICT_RANGE;
            // the value is the feet position of the player
            Vec3 eye = player.position();
            List<LivingEntity> candidates = level.getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(range),
                e -> e != player && e.isAlive() && !e.isSpectator() && (!e.isInvisible() || e.isCurrentlyGlowing()));
            candidates.sort((a, b) -> Double.compare(a.distanceToSqr(eye), b.distanceToSqr(eye)));
            int count = Math.min(MAX_TRACKS, candidates.size());
            Int2ObjectOpenHashMap<Memory> seen = new Int2ObjectOpenHashMap<>(count);
            // pass 1 (0.1.10.23): motion, intent, countdown and priority of every track
            Vec3[] vs = new Vec3[count];
            String[] intents = new String[count];
            int[] countdowns = new int[count];
            double[] scores = new double[count];
            boolean[] nears = new boolean[count];
            double[] vel = new double[3];
            priorities.clear();
            for (int i = 0; i < count; i++) {
                LivingEntity e = candidates.get(i);
                Memory m = memory.get(e.getId());
                if (m == null) m = new Memory();
                Vec3 at = e.position();
                m.tracker.update(at.x, at.y, at.z);
                seen.put(e.getId(), m);
                m.tracker.velocity(vel);
                vs[i] = new Vec3(vel[0], vel[1], vel[2]);
                if (e.hurtTime <= 0) { m.driveX = vel[0]; m.driveZ = vel[2]; m.driveKnown = true; }
                m.swings.observe(ticks, e.swinging);
                if (e instanceof Ghast ghast) m.charge.observe(ticks, ghast.isCharging());
                nears[i] = e.distanceToSqr(player) <= PREDICT_RANGE * PREDICT_RANGE;
                intents[i] = predict && nears[i] ? intent(e, player) : null;
                countdowns[i] = intents[i] == null ? -1 : countdown(e, m, intents[i]);
                int threat = SuitSensorData.threatLevel(e.getId());
                scores[i] = TargetPriority.score(TargetRating.rating(e, player.position(), vs[i], threat, countdowns[i]));
                priorities.put(e.getId(), scores[i]);
            }
            // ranks: the highest scores among the hostile creatures in the predictor range
            int[] ranks = new int[count];
            Integer[] order = new Integer[count];
            for (int i = 0; i < count; i++) order[i] = i;
            java.util.Arrays.sort(order, (a, b) -> Double.compare(scores[b], scores[a]));
            int rank = 0;
            for (int k = 0; k < count && rank < SHOWN_RANKS; k++) {
                LivingEntity e = candidates.get(order[k]);
                if (!nears[order[k]] || TargetRating.hostility(e, SuitSensorData.threatLevel(e.getId())) < 2) continue;
                ranks[order[k]] = ++rank;
            }
            // pass 2: paths (near, threatening and ranked creatures every PATH_INTERVAL ticks, the others every FAR_PATH_INTERVAL)
            List<Track> out = new ArrayList<>(count);
            for (int i = 0; i < count; i++) {
                LivingEntity e = candidates.get(i);
                Memory m = seen.get(e.getId());
                int threat = SuitSensorData.threatLevel(e.getId());
                boolean hot = intents[i] != null && (threat == 2 || intents[i].equals("DET"));
                boolean close = e.distanceToSqr(player) <= NEAR_RANGE * NEAR_RANGE;
                int interval = close || threat > 0 || hot || ranks[i] > 0 ? PATH_INTERVAL : FAR_PATH_INTERVAL;
                List<Vec3> path = predict && nears[i] ? path(e, m, vs[i], player, threat, interval) : List.of();
                out.add(new Track(e, kind(e), vs[i], path, intents[i], hot, countdowns[i], ranks[i], predict && nears[i] && m.linked));
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
     * <p>0.1.10.23 (P2): {@code DeadReckoning#walk}, the vanilla movement: the friction of the block under its feet
     * (ice), water drag, a step up to 0.6 blocks and a jump onto a 1-block ledge (before, the path stopped at slabs and
     * at every ledge), falling off an edge; a hit creature slides with the server's velocity and comes back to its own
     * motion with the friction; a melee creature after the wearer stops at {@code DeadReckoning#HUNTER_STOP}. With path
     * nodes from the server (Behavior Predictor Mk.II) the path follows them ({@code PathFollower}). Far creatures that
     * do not threaten the wearer get a new path every {@link #FAR_PATH_INTERVAL} ticks (P5).
     *
     * @return the predicted positions, or an empty list when the horizontal speed is below 0.02
     *         blocks per tick and the vertical speed is below 0.02 blocks per tick.
     */
    private static List<Vec3> path(LivingEntity e, Memory m, Vec3 v, LocalPlayer player, int threat, int interval) {
        // 0.1.10.20 (P8): computed every interval ticks; in between the last path moves with the creature
        if (m.pathOrigin != null && ticks - m.pathTick < interval) {
            Vec3 shift = e.position().subtract(m.pathOrigin);
            if (shift.lengthSqr() < 1.0E-10) return m.path;
            List<Vec3> moved = new ArrayList<>(m.path.size());
            for (Vec3 point : m.path) moved.add(point.add(shift));
            return moved;
        }
        Vec3 p = e.position();
        double[] flat;
        double[] nodes = SuitSensorData.pathNodes(e.getId());
        m.linked = nodes != null;
        if (nodes != null) {
            flat = PathFollower.follow(nodes, p.x, p.y, p.z, Math.hypot(v.x, v.z), PREDICT_TICKS);
        } else {
            DeadReckoning.Body body = new DeadReckoning.Body();
            boolean hit = e.hurtTime > 0 && m.driveKnown;
            Vec3 server = e.getDeltaMovement();
            body.vx = hit ? server.x : v.x; body.vy = hit ? server.y : v.y; body.vz = hit ? server.z : v.z;
            body.packet = hit;
            body.driveX = m.driveKnown ? m.driveX : v.x; body.driveZ = m.driveKnown ? m.driveZ : v.z;
            body.onGround = e.onGround();
            body.noGravity = e.isNoGravity();
            body.height = e.getBbHeight();
            body.turnRate = m.tracker.turnRate();
            boolean hunter = threat == 2 || e instanceof Mob mob && mob.isAggressive() && e instanceof Enemy;
            if (hunter && !TargetRating.ranged(e)) {
                body.stopX = player.getX(); body.stopZ = player.getZ(); body.stopRadius = DeadReckoning.HUNTER_STOP;
            }
            flat = DeadReckoning.walk(blockCache, p.x, p.y, p.z, body, PREDICT_TICKS);
        }
        List<Vec3> pts = new ArrayList<>(flat.length / 3);
        for (int i = 0; i < flat.length; i += 3) pts.add(new Vec3(flat[i], flat[i + 1], flat[i + 2]));
        m.path = List.copyOf(pts);
        m.pathOrigin = p;
        m.pathTick = ticks;
        return m.path;
    }

    /**
     * 0.1.10.23 (P7): ticks to the attack shown with the intent: the bow draw, the crossbow load, the ghast charge, the
     * creeper swell, or the learned melee swing (ATK); -1 when unknown.
     */
    private static int countdown(LivingEntity e, Memory m, String intent) {
        if (e instanceof Creeper creeper && "DET".equals(intent)) return Math.round((1 - creeper.getSwelling(1.0F)) * 30);
        if (e instanceof Ghast && "FIRE".equals(intent)) return m.charge.left(ticks, AttackTimer.GHAST_AFTER_FLAG);
        if (e.isUsingItem()) {
            var stack = e.getUseItem();
            if (stack.getItem() instanceof BowItem) return AttackTimer.bowLeft(e.getTicksUsingItem());
            if (stack.getItem() instanceof CrossbowItem) return AttackTimer.crossbowLeft(e.getTicksUsingItem(), CrossbowItem.getChargeDuration(stack, e));
        }
        if ("ATK".equals(intent)) return m.swings.left(ticks);
        return -1;
    }

    /** 0.1.10.20: the client level as the predictor sees it (collision shape, air; 0.1.10.23: shape top, water, friction). */
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

        @Override
        public double top(int x, int y, int z) {
            at.set(x, y, z);
            var shape = level.getBlockState(at).getCollisionShape(level, at);
            return shape.isEmpty() ? 0 : Math.max(0, shape.max(net.minecraft.core.Direction.Axis.Y));
        }

        @Override
        public boolean water(int x, int y, int z) {
            at.set(x, y, z);
            return level.getFluidState(at).is(net.minecraft.tags.FluidTags.WATER);
        }

        @Override
        public double friction(int x, int y, int z) {
            at.set(x, y, z);
            return level.getBlockState(at).getFriction(level, at, null);
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
