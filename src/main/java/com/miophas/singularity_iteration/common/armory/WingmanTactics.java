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
 * flight path is checked ahead; a wall makes the Wingman climb or slide along it.
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
        double score = 0;
        int now = owner.tickCount;
        boolean called = e == owner.getLastHurtMob() && now - owner.getLastHurtMobTimestamp() < 60;
        boolean attacker = e == owner.getLastHurtByMob() && now - owner.getLastHurtByMobTimestamp() < 100;
        boolean hunter = e instanceof Mob mob && mob.getTarget() == owner;
        // 0.1.10.2: the Fleet Datalink mark (the creature in the owner's sights) counts as a call
        boolean marked = e == com.miophas.singularity_iteration.common.suit.quantum.QuantumSynergies.mark(owner);
        if (marked) called = true;
        double toOwner = Math.sqrt(e.distanceToSqr(guard));
        if (e instanceof Enemy) score += 20;
        else if (!called && !attacker && !hunter) return Double.NEGATIVE_INFINITY;   // a neutral mob is a target only when it fights
        if (called) score += marked ? 90 : 120;
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
        score -= 1.8 * toOwner + 0.7 * e.distanceTo(self);
        score += 14 * (1 - e.getHealth() / Math.max(1, e.getMaxHealth()));
        if (e == current) score += 22;
        if (!called && !fusing && squad.engages(e)) score -= 28;
        return score;
    }

    /** True when another Wingman of the same owner fights {@code e}. */
    static boolean engagedByOther(WingmanEntity self, ServerPlayer owner, LivingEntity e) {
        for (WingmanEntity other : self.level().getEntitiesOfClass(WingmanEntity.class, self.getBoundingBox().inflate(48),
                w -> w != self && owner.getUUID().equals(w.owner()))) {
            if (other.currentTarget() == e) return true;
        }
        return false;
    }

    /** Best target among the mobs within {@code range} of {@code around}, or null. */
    @Nullable
    static LivingEntity select(WingmanEntity self, ServerPlayer owner, Vec3 around, double range, @Nullable LivingEntity current,
                               java.util.function.Predicate<LivingEntity> valid) {
        return select(self, owner, around, range, current, valid, owner.position());
    }

    /** 0.1.10.2: the same, scored from {@code guard} (see {@link #threat(WingmanEntity, ServerPlayer, LivingEntity, LivingEntity, Vec3)}). */
    @Nullable
    static LivingEntity select(WingmanEntity self, ServerPlayer owner, Vec3 around, double range, @Nullable LivingEntity current,
                               java.util.function.Predicate<LivingEntity> valid, Vec3 guard) {
        // 0.1.10.8: every candidate is scored without its line-of-sight term (an upper bound), then
        // the rays are cast from the best bound down until no bound can reach the best score. The
        // choice is the same as scoring every candidate in scan order (the first of equal scores wins).
        Squad squad = new Squad(self, owner);
        var candidates = new java.util.ArrayList<LivingEntity>();
        for (LivingEntity e : self.level().getEntitiesOfClass(LivingEntity.class, new AABB(around, around).inflate(range),
                e -> e instanceof Mob && e.isAlive())) {
            if (valid.test(e)) candidates.add(e);
        }
        int n = candidates.size();
        double[] bounds = new double[n];
        for (int i = 0; i < n; i++) bounds[i] = threatBeforeSight(self, owner, candidates.get(i), current, guard, squad);
        double[] top = new double[1];
        int pick = bestIndex(bounds, Double.NEGATIVE_INFINITY, i -> withSight(self, owner, candidates.get(i), bounds[i]), top);
        LivingEntity best = pick < 0 ? null : candidates.get(pick);
        double bestScore = top[0];
        // the owner's call may be outside the scan box (a long shot): keep it
        LivingEntity called = owner.getLastHurtMob();
        if (called != null && owner.tickCount - owner.getLastHurtMobTimestamp() < 60 && valid.test(called)) {
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
    static Vec3 station(WingmanEntity self, LivingEntity target, ServerPlayer owner, double[] band, @Nullable Vec3 previous) {
        Level level = self.level();
        Vec3 c = target.position().add(0, target.getBbHeight() * 0.6, 0);
        double mid = (band[0] + band[1]) * 0.5;
        double[] radii = {band[0] + 1.0, mid, band[1] - 1.0};
        double[] heights = {2.5, 4.5, 7.0};
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

    /**
     * Adds an avoidance term to a wanted velocity: probes the path 4 blocks ahead; a wall makes the
     * Wingman climb, else slide along it (whichever side is free).
     */
    static Vec3 avoid(WingmanEntity self, Vec3 want) {
        if (want.lengthSqr() < 1.0E-4) return want;
        Level level = self.level();
        Vec3 from = self.position().add(0, self.getBbHeight() * 0.5, 0);
        Vec3 dir = want.normalize();
        Vec3 ahead = from.add(dir.scale(Math.min(4.0, 1.5 + want.length() * 6)));
        if (clear(level, from, ahead, self)) return want;
        Vec3 up = from.add(0, 2.5, 0).add(dir.scale(2.5));
        if (clear(level, from, up, self)) return want.add(0, 0.35, 0);
        Vec3 side = new Vec3(-dir.z, 0, dir.x);
        Vec3 left = from.add(side.scale(2.5)).add(dir.scale(1.5)), right = from.add(side.scale(-2.5)).add(dir.scale(1.5));
        if (clear(level, from, left, self)) return want.add(side.scale(0.35));
        if (clear(level, from, right, self)) return want.add(side.scale(-0.35));
        return want.add(0, 0.3, 0).scale(0.5);
    }
}
