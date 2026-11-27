// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.armory;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.monster.RangedAttackMob;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * Tactical brain of the Wingman (0.1.9.1).
 *
 * <h2>Target priority</h2>
 * Each candidate gets a threat score; the highest score is the target. In order of weight:
 * <ol>
 *   <li>the mob the owner hit in the last 3 s (the owner's call: every Wingman focuses it);</li>
 *   <li>a creeper near the owner, much more when it fuses;</li>
 *   <li>the mob that hit the owner in the last 5 s;</li>
 *   <li>mobs that hunt the owner, ranged attackers more, mobs closing in more;</li>
 *   <li>any other hostile mob in range.</li>
 * </ol>
 * Distance to the owner and to the Wingman lowers the score, a wounded mob raises it (finish it),
 * the current target keeps a bonus (no flicker), and a target that another Wingman of the same
 * owner already fights is less attractive, except for the owner's call and a fusing creeper
 * (focus fire).
 *
 * <p>0.1.10.2 quantum command: the creature in the owner's sights (Fleet Datalink mark) weighs
 * like a strong call; a Wingman on an anchored order measures "distance to the owner" from its
 * order point instead (the guard point), so it defends the point, not the far-away owner.
 *
 * <h2>Firing position</h2>
 * A ranged Wingman picks its station from 36 candidates on three rings and three heights around
 * the target, scored by: clear line of fire (blocks), its weapon band, the far side of the target
 * as seen from the owner (flanking), height, distance from its current place (smooth moves), no
 * owner in the line of fire, room to fly, the other Wingmen and other hostiles nearby.
 *
 * <h2>Evasion and obstacles</h2>
 * A hostile that comes within the danger radius triggers a thruster boost away from it. The
 * flight path is checked ahead with the whole body; when it is blocked the Wingman takes the best free heading (context
 * steering, 0.1.11.221), and under a ceiling going up costs extra.
 */
public final class WingmanTactics {
    private WingmanTactics() {}

    // ------------------------------------------------------------------ targets

    /** Score of {@code e} as a target for {@code self}. Higher is more urgent; {@link Double#NEGATIVE_INFINITY} is "ignore". */
    static double threat(WingmanEntity self, ServerPlayer owner, LivingEntity e, @Nullable LivingEntity current) {
        return threat(self, owner, e, current, owner.position());
    }

    /** 0.1.10.2: the same, with the distance terms measured from {@code guard} (the owner, or the order point). */
    static double threat(WingmanEntity self, ServerPlayer owner, LivingEntity e, @Nullable LivingEntity current, Vec3 guard) {
        return withSight(self, owner, e, threatBeforeSight(self, owner, e, current, guard, new Squad(self, owner)));
    }

    /**
     * 0.1.10.8: the line-of-sight term of {@link #threat}, applied last as before: a target that
     * neither the Wingman nor the owner sees loses 12. It only lowers the score, so the score before
     * it is an upper bound (the target choice casts the rays only for the best candidates).
     */
    private static double withSight(WingmanEntity self, ServerPlayer owner, LivingEntity e, double score) {
        if (score == Double.NEGATIVE_INFINITY) return score;
        if (!self.hasLineOfSight(e) && !owner.hasLineOfSight(e)) score -= 12;
        return score;
    }

    /**
     * 0.1.10.8: the targets of the owner's other Wingmen within 48 blocks, fetched once per target
     * choice on first use (before, every candidate fetched them again).
     */
    private static final class Squad {
        private final WingmanEntity self;
        private final ServerPlayer owner;
        @Nullable private java.util.Set<LivingEntity> engaged;

        Squad(WingmanEntity self, ServerPlayer owner) { this.self = self; this.owner = owner; }

        boolean engages(LivingEntity e) {
            if (engaged == null) {
                engaged = java.util.Collections.newSetFromMap(new java.util.IdentityHashMap<>());
                for (WingmanEntity other : self.level().getEntitiesOfClass(WingmanEntity.class, self.getBoundingBox().inflate(48),
                        w -> w != self && owner.getUUID().equals(w.owner()))) {
                    LivingEntity t = other.currentTarget();
                    if (t != null) engaged.add(t);
                }
            }
            return engaged.contains(e);
        }
    }

    /** The score of {@link #threat} before its line-of-sight term. */
    private static double threatBeforeSight(WingmanEntity self, ServerPlayer owner, LivingEntity e, @Nullable LivingEntity current,
                                            Vec3 guard, Squad squad) {
        return threatBeforeSight(self, owner, e, current, guard, squad, e.distanceTo(self), true);
    }

    /**
     * 0.1.11.223 (G4): the same, with the distance to the Wingman given ({@code toSelf}: the travel cost of a grid path in a
     * cave) and the bonus of the current target only when {@code keep}.
     */
    private static double threatBeforeSight(WingmanEntity self, ServerPlayer owner, LivingEntity e, @Nullable LivingEntity current,
                                            Vec3 guard, Squad squad, double toSelf, boolean keep) {
        double score = 0;
        int now = owner.tickCount;
        // 0.1.11.43 (G-7): the owner's call and the Fleet Datalink mark count for hostile creatures only; a neutral one
        // is a target when it attacks or hunts the owner (or by an explicit focus order)
        boolean called = e instanceof Enemy && e == owner.getLastHurtMob() && now - owner.getLastHurtMobTimestamp() < 60;
        boolean attacker = e == owner.getLastHurtByMob() && now - owner.getLastHurtByMobTimestamp() < 100;
        boolean hunter = e instanceof Mob mob && mob.getTarget() == owner;
        // 0.1.10.2: the Fleet Datalink mark (the creature in the owner's sights) counts as a call
        boolean marked = e instanceof Enemy && e == com.miophas.singularity_iteration.common.suit.quantum.QuantumSynergies.mark(owner);
        if (marked) called = true;
        double toOwner = Math.sqrt(e.distanceToSqr(guard));
        if (e instanceof Enemy) score += 20;
        else if (!called && !attacker && !hunter) return Double.NEGATIVE_INFINITY;   // a neutral mob is a target only when it fights
        // 0.1.10.42: the owner's call outweighs a fusing creeper beside the owner (up to about 210 before the distance terms), as the
        // class comment ranks it; it was 120, so every Wingman left the owner's target for the creeper. The sight mark stays 90.
        if (called) score += marked ? 90 : 240;
        if (attacker) score += 70;
        if (hunter) score += 35;
        if (hunter && e instanceof RangedAttackMob) score += 18;
        boolean fusing = false;
        if (e instanceof Creeper creeper) {
            fusing = creeper.getSwellDir() > 0;
            score += 25 + Math.max(0, 8 - toOwner) * 9 + (fusing ? 90 : 0);
        }
        // closing in on the owner
        Vec3 v = e.getDeltaMovement();
        Vec3 dir = guard.subtract(e.position());
        if (dir.lengthSqr() > 1.0E-4 && v.horizontalDistanceSqr() > 1.0E-4) {
            double closing = new Vec3(v.x, 0, v.z).normalize().dot(new Vec3(dir.x, 0, dir.z).normalize());
            if (closing > 0.3) score += 12 * closing;
        }
        score -= 1.8 * toOwner + 0.7 * toSelf;
        score += 14 * (1 - e.getHealth() / Math.max(1, e.getMaxHealth()));
        if (e == current && keep) score += 22;
        if (!called && !fusing && squad.engages(e)) score -= 28;
        return score;
    }

    /** Best target among the mobs within {@code range} of {@code around}, or null. */
    @Nullable
    static LivingEntity select(WingmanEntity self, ServerPlayer owner, Vec3 around, double range, @Nullable LivingEntity current,
                               java.util.function.Predicate<LivingEntity> valid) {
        return select(self, owner, around, range, current, valid, owner.position());
    }

    /** Score lost by a known creature that nobody sees now (it is hunted at its last place). */
    static final double UNSEEN = 6;

    /**
     * 0.1.10.2: the same, scored from {@code guard} (see {@link #threat(WingmanEntity, ServerPlayer, LivingEntity, LivingEntity, Vec3)}).
     *
     * <p>0.1.11.223 (cave overhaul G4): only the creatures the Wingman knows are candidates ({@link WingmanPerception}: seen
     * by it, its owner or its squad, or tracked), and the ones decision five A rejects are not (no path and unseen, or in
     * another tunnel and not coming for the owner). The distance to the Wingman is its travel cost (a grid path under a
     * ceiling); a known creature nobody sees now loses {@link #UNSEEN}; the current target keeps its bonus only while it is
     * seen, tracked or reachable. The owner's call outside the box is kept as before.
     */
    @Nullable
    static LivingEntity select(WingmanEntity self, ServerPlayer owner, Vec3 around, double range, @Nullable LivingEntity current,
                               java.util.function.Predicate<LivingEntity> valid, Vec3 guard) {
        WingmanPerception known = self.perception;
        known.update(owner, around, range);
        Squad squad = new Squad(self, owner);
        LivingEntity best = null;
        double bestScore = Double.NEGATIVE_INFINITY;
        for (LivingEntity e : self.level().getEntitiesOfClass(LivingEntity.class, new AABB(around, around).inflate(range),
                e -> e instanceof Mob && e.isAlive())) {
            if (!valid.test(e) || known.banned(e)) continue;
            WingmanPerception.Contact c = known.contact(e);
            if (c == null || known.rejects(e)) continue;
            double travel = known.travel(e);
            double toSelf = Double.isNaN(travel) || Double.isInfinite(travel) ? e.distanceTo(self) : travel;
            boolean keep = c.visible() || c.tracked() || Double.isFinite(travel);
            double s = threatBeforeSight(self, owner, e, current, guard, squad, toSelf, keep);
            if (s == Double.NEGATIVE_INFINITY) continue;
            if (!c.visible() && !c.tracked()) s -= UNSEEN;
            if (s > bestScore) { bestScore = s; best = e; }
        }
        // the owner's call may be outside the scan box (a long shot): keep it
        LivingEntity called = owner.getLastHurtMob();
        if (called instanceof Enemy && owner.tickCount - owner.getLastHurtMobTimestamp() < 60 && valid.test(called) && called != best
                && !known.banned(called)) {
            double s = withSight(self, owner, called, threatBeforeSight(self, owner, called, current, guard, squad));
            if (s > bestScore) best = called;
        }
        return best;
    }

    // ------------------------------------------------------------------ positions

    /**
     * Firing station around {@code target} for a weapon band {@code band} (min, max). Returns the
     * best of 36 candidates, or {@code previous} when it still scores within 8 points of the best
     * (no jitter between equal places).
     */
    /** 0.1.11.226 (G3): station heights above the target's middle: in flight (indoor-friendly), and on foot (the Wingman stands on the floor below). */
    static final double[] HEIGHTS_AIR = {0.8, 2.2, 4.5}, HEIGHTS_FOOT = {0, 0, 0};

    static Vec3 station(WingmanEntity self, LivingEntity target, ServerPlayer owner, double[] band, @Nullable Vec3 previous) {
        return station(self, target, owner, band, previous, HEIGHTS_AIR);
    }

    /** 0.1.11.226: the same with the station heights given ({@link #HEIGHTS_AIR}, {@link #HEIGHTS_FOOT}). */
    static Vec3 station(WingmanEntity self, LivingEntity target, ServerPlayer owner, double[] band, @Nullable Vec3 previous, double[] heights) {
        Level level = self.level();
        Vec3 c = target.position().add(0, target.getBbHeight() * 0.6, 0);
        double mid = (band[0] + band[1]) * 0.5;
        double[] radii = {band[0] + 1.0, mid, band[1] - 1.0};
        // Indoor-friendly heights (0.8, 2.2, 4.5) so ranged Wingmen find clear firing stations in 3-4 block caves/rooms too
        // (0.1.11.226: on foot all at the target's middle, and the Wingman stands on the floor below)
        Vec3 fromOwner = new Vec3(c.x - owner.getX(), 0, c.z - owner.getZ());
        Vec3 flank = fromOwner.lengthSqr() < 1.0E-4 ? Vec3.ZERO : fromOwner.normalize();
        Vec3[] points = new Vec3[36], dirs = new Vec3[36];
        double[] hs = new double[36];
        int n = 0;
        for (int k = 0; k < 12; k++) {
            double a = k * Math.PI * 2 / 12 + (self.getId() % 7) * 0.13;
            Vec3 dir = new Vec3(Math.cos(a), 0, Math.sin(a));
            for (int ri = 0; ri < radii.length; ri++) {
                double r = radii[ri];
                double h = heights[(k + ri) % heights.length];
                points[n] = c.add(dir.scale(r)).add(0, h, 0); dirs[n] = dir; hs[n] = h; n++;
            }
        }
        Vec3 pdir = null;
        if (previous != null) {
            Vec3 pd = previous.subtract(c);
            pdir = new Vec3(pd.x, 0, pd.z);
        }
        // 0.1.10.8: the Wingmen and hostiles near any station are fetched once (before: two box
        // queries per station). A station counts exactly the ones whose box meets its own box.
        AABB around = new AABB(points[0], points[0]);
        for (int i = 1; i < n; i++) around = around.minmax(new AABB(points[i], points[i]));
        if (previous != null) around = around.minmax(new AABB(previous, previous));
        var near = new StationNeighbours(
            level.getEntitiesOfClass(WingmanEntity.class, around.inflate(3.0), w -> w != self),
            level.getEntitiesOfClass(Mob.class, around.inflate(4.0), m -> m instanceof Enemy && m != target));
        Vec3 look = target.getViewVector(1.0F);
        Vec3 lookFlat = new Vec3(look.x, 0, look.z).normalize();
        // Every station is scored with a clear line of fire (an upper bound: a blocked line only
        // subtracts), then the rays are cast from the best bound down until no bound can reach the
        // best score. The result is the same as before (the first of equal scores wins).
        double[] bounds = new double[n];
        boolean[] room = new boolean[n];
        for (int i = 0; i < n; i++) {
            room[i] = free(level, points[i]);
            bounds[i] = room[i] ? stationRest(0, self, owner, c, points[i], flank, dirs[i], hs[i], lookFlat, near) : -1.0E6;
        }
        double[] top = new double[1];
        int pick = bestIndex(bounds, -1.0E9, i -> !room[i] || clear(level, points[i].add(0, 0.2, 0), c, self) ? bounds[i]
            : stationRest(-120, self, owner, c, points[i], flank, dirs[i], hs[i], lookFlat, near), top);
        Vec3 best = pick < 0 ? null : points[pick];
        double bestScore = top[0];
        if (previous != null && best != null) {
            double prevScore = pdir.lengthSqr() < 1.0E-4 ? -1.0E9
                : stationScore(self, level, owner, c, previous, flank, pdir.normalize(), previous.subtract(c).y, lookFlat, near);
            if (prevScore > bestScore - 8) return previous;
        }
        return best == null ? c.add(0, 4, 0) : best;
    }

    /** 0.1.10.8: the Wingmen (not this one) and the hostiles (not the target) near the stations of one choice. */
    private record StationNeighbours(java.util.List<WingmanEntity> squad, java.util.List<Mob> hostiles) { }

    private static double stationScore(WingmanEntity self, Level level, ServerPlayer owner, Vec3 c, Vec3 p,
                                       Vec3 flank, Vec3 dir, double h, Vec3 lookFlat, StationNeighbours near) {
        if (!free(level, p)) return -1.0E6;
        return stationRest(clear(level, p.add(0, 0.2, 0), c, self) ? 0 : -120, self, owner, c, p, flank, dir, h, lookFlat, near);
    }

    /**
     * The terms of a station after its line-of-fire term, added to {@code s} in the order of
     * 0.1.9.1 (0 for a clear line, -120 for a blocked one).
     */
    private static double stationRest(double s, WingmanEntity self, ServerPlayer owner, Vec3 c, Vec3 p,
                                      Vec3 flank, Vec3 dir, double h, Vec3 lookFlat, StationNeighbours near) {
        // flanking: the far side of the target as seen from the owner (behind its back)
        s += 22 * dir.dot(flank);
        // the target faces somebody: behind its head is better
        s += 8 * -lookFlat.dot(dir);
        // height: high ground, but not out of reach
        s += Math.min(h, 6) * 2.2;
        // owner not in the shot line, and not standing in the owner's line of fire
        if (segmentDistance(owner.getBoundingBox().getCenter(), p, c) < 1.6) s -= 70;
        if (segmentDistance(p, owner.getEyePosition(), c) < 1.8) s -= 35;
        // smooth: stay near the current place
        s -= 0.55 * p.distanceTo(self.position());
        // the squad spreads out
        AABB squadBox = new AABB(p, p).inflate(3.0);
        for (WingmanEntity other : near.squad()) if (other.getBoundingBox().intersects(squadBox)) s -= 22;
        // keep clear of other hostiles
        AABB hostileBox = new AABB(p, p).inflate(4.0);
        for (Mob mob : near.hostiles()) if (mob.getBoundingBox().intersects(hostileBox)) s -= 14;
        return s;
    }

    /**
     * 0.1.10.8: the index that a scan in index order picks with {@code if (s > best)} from the start
     * value {@code floor} (the first of equal best scores; -1 when no score is above the floor), when
     * every {@code exact(i) <= bounds[i]}. The exact scores (the rays) are computed from the highest
     * bound down, and only while a bound can still reach the best score. {@code top[0]} receives
     * the best score ({@code floor} for -1).
     */
    static int bestIndex(double[] bounds, double floor, java.util.function.IntToDoubleFunction exact, double[] top) {
        double bestScore = floor;
        int best = -1;
        for (int i : byBound(bounds)) {
            if (bounds[i] < bestScore) break;
            double s = exact.applyAsDouble(i);
            if (s > bestScore || s == bestScore && best >= 0 && i < best) { bestScore = s; best = i; }
        }
        top[0] = bestScore;
        return best;
    }

    /** Indexes by bound, highest first; equal bounds in index order. */
    private static int[] byBound(double[] bounds) {
        Integer[] order = new Integer[bounds.length];
        for (int i = 0; i < order.length; i++) order[i] = i;
        java.util.Arrays.sort(order, (x, y) -> {
            int c = Double.compare(bounds[y], bounds[x]);
            return c != 0 ? c : Integer.compare(x, y);
        });
        int[] result = new int[order.length];
        for (int i = 0; i < order.length; i++) result[i] = order[i];
        return result;
    }

    /** Room for the Wingman's body at {@code p} (two blocks of air). */
    static boolean free(Level level, Vec3 p) {
        BlockPos feet = BlockPos.containing(p);
        if (!level.isLoaded(feet)) return false;
        return level.getBlockState(feet).getCollisionShape(level, feet).isEmpty()
            && level.getBlockState(feet.above()).getCollisionShape(level, feet.above()).isEmpty();
    }

    /** No block between {@code a} and {@code b}. */
    static boolean clear(Level level, Vec3 a, Vec3 b, WingmanEntity self) {
        return level.clip(new ClipContext(a, b, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, self)).getType() == HitResult.Type.MISS;
    }

    /** Distance from {@code point} to the segment {@code a}-{@code b}. */
    static double segmentDistance(Vec3 point, Vec3 a, Vec3 b) {
        Vec3 seg = b.subtract(a);
        double len2 = seg.lengthSqr();
        if (len2 < 1.0E-6) return point.distanceTo(a);
        double t = Mth.clamp(point.subtract(a).dot(seg) / len2, 0, 1);
        return a.add(seg.scale(t)).distanceTo(point);
    }

    // ------------------------------------------------------------------ evasion and avoidance

    /** Nearest hostile mob (other than the target of a melee pass) within {@code radius}, or null. */
    @Nullable
    static Mob danger(WingmanEntity self, double radius) {
        Mob best = null;
        double bestD = radius * radius;
        for (Mob mob : self.level().getEntitiesOfClass(Mob.class, self.getBoundingBox().inflate(radius),
                m -> m instanceof Enemy && m.isAlive() && !self.isAlliedTo(m))) {
            double d = mob.distanceToSqr(self);
            if (d < bestD) { bestD = d; best = mob; }
        }
        return best;
    }

    /** Nearest fusing Creeper within {@code radius} (so Wingmen can thruster-boost clear of the blast), or null. */
    @Nullable
    static Creeper fusingCreeper(WingmanEntity self, double radius) {
        Creeper best = null;
        double bestD = radius * radius;
        for (Creeper c : self.level().getEntitiesOfClass(Creeper.class, self.getBoundingBox().inflate(radius),
                cr -> cr.isAlive() && (cr.isIgnited() || cr.getSwellDir() > 0))) {
            double d = c.distanceToSqr(self);
            if (d < bestD) { bestD = d; best = c; }
        }
        return best;
    }

    /** 0.1.11.221 (G1): headings of the context steering, degrees from the wanted heading, and pitches (degrees, up positive). */
    static final int HEADINGS = 16;
    static final double[] PITCHES = {-35, 0, 35, 60};
    /** Interest lost per unit of upward direction in a tunnel and in a room (the open sky costs nothing). */
    static final double UP_COST_TUNNEL = 0.45, UP_COST_INDOOR = 0.2;
    /** Below this score no heading is worth taking: the Wingman brakes instead of drifting. */
    static final double NO_WAY = 0.05;

    /**
     * Turns a wanted velocity into one the Wingman's body can fly. 0.1.11.221 (cave overhaul G1): context steering.
     *
     * <p>Before, a thin ray from the middle of the body looked ahead; when it was blocked the Wingman tried up first, then
     * down, then the sides, and when everything was blocked it still drifted up. In a cave the way ahead is nearly always
     * blocked, so it climbed to the ceiling and stuck there. Now the whole body box is swept along the wanted heading
     * ({@link WingmanSpace#sweep}); when it does not fit, {@link #HEADINGS} headings at each of {@link #PITCHES} are scored:
     * the interest is how close a heading is to the wanted one, scaled by how far the body gets along it, less a cost for
     * going up under a ceiling ({@link #UP_COST_TUNNEL}, {@link #UP_COST_INDOOR}). The best one keeps the wanted speed,
     * slower when its way is short. When no heading is worth taking the Wingman brakes (no upward drift).
     */
    static Vec3 avoid(WingmanEntity self, Vec3 want) {
        if (want.lengthSqr() < 1.0E-4) { WingmanNavDebug.record(self, WingmanNavDebug.Probe.CLEAR); return want; }
        Level level = self.level();
        double speed = want.length();
        Vec3 dir = want.scale(1 / speed);
        // 0.1.11.50 (F1): the look-ahead covers about 4 ticks of flight at escort speed
        double look = Math.min(Math.max(4.0, speed * 4), 1.5 + speed * 6);
        AABB box = self.getBoundingBox();
        if (WingmanSpace.sweep(level, self, box, dir, look) >= look) { WingmanNavDebug.record(self, WingmanNavDebug.Probe.CLEAR); return want; }
        WingmanSpace.Room room = WingmanSpace.of(self);
        double upCost = room.kind() == WingmanSpace.Kind.TUNNEL ? UP_COST_TUNNEL : room.kind() == WingmanSpace.Kind.INDOOR ? UP_COST_INDOOR : 0;
        if (room.headroom() < 1.0) upCost += 0.5;
        Vec3 flat = new Vec3(dir.x, 0, dir.z);
        if (flat.lengthSqr() < 1.0E-4) {
            double yaw = Math.toRadians(self.getYRot());
            flat = new Vec3(-Math.sin(yaw), 0, Math.cos(yaw));
        }
        flat = flat.normalize();
        int n = HEADINGS * PITCHES.length;
        Vec3[] dirs = new Vec3[n];
        double[] interest = new double[n], cost = new double[n], bounds = new double[n];
        int[] heading = new int[n];
        for (int h = 0; h < HEADINGS; h++) {
            double a = Math.toRadians(h * 360.0 / HEADINGS);
            double cx = flat.x * Math.cos(a) - flat.z * Math.sin(a), cz = flat.x * Math.sin(a) + flat.z * Math.cos(a);
            for (int p = 0; p < PITCHES.length; p++) {
                int i = h * PITCHES.length + p;
                double pitch = Math.toRadians(PITCHES[p]);
                Vec3 d = new Vec3(cx * Math.cos(pitch), Math.sin(pitch), cz * Math.cos(pitch));
                dirs[i] = d;
                heading[i] = h;
                interest[i] = (1 + d.dot(dir)) * 0.5;
                cost[i] = d.y > 0.2 ? upCost * d.y : 0;
                bounds[i] = interest[i] - cost[i];
            }
        }
        double[] top = new double[1];
        double[] reach = new double[n];
        int pick = bestIndex(bounds, NO_WAY, i -> {
            reach[i] = WingmanSpace.sweep(level, self, box, dirs[i], look) / look;
            return interest[i] * reach[i] - cost[i];
        }, top);
        if (pick < 0) {
            WingmanNavDebug.record(self, WingmanNavDebug.Probe.BLOCKED);
            Vec3 slow = want.scale(0.3);
            return room.kind().confined() && slow.y > 0 ? new Vec3(slow.x, 0, slow.z) : slow;
        }
        Vec3 d = dirs[pick];
        boolean ahead = heading[pick] == 0;
        WingmanNavDebug.record(self, ahead && d.y > dir.y + 0.15 ? WingmanNavDebug.Probe.CLIMB
            : ahead && d.y < dir.y - 0.15 ? WingmanNavDebug.Probe.DUCK : WingmanNavDebug.Probe.SLIDE);
        return d.scale(speed * Math.max(0.35, reach[pick]));
    }
}
