// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.armory;

import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * 0.1.11.223 (cave overhaul G4, decisions four A and five A): what a Wingman knows about the creatures round its owner.
 *
 * <p>Before, every mob in a 24-block cube round the owner was a candidate, rock or no rock; one the Wingman and the owner
 * did not see lost only 12 points, and the distance was the straight line. In a cave a mob in the tunnel above, 9 blocks
 * away through the rock, outscored one 20 blocks down the owner's own tunnel, and the Wingman pressed against the ceiling
 * under it.
 *
 * <p>Now a creature is a candidate only when it is <em>known</em>:
 * <ul>
 *   <li>seen: a line from the Wingman's eyes, or from the owner's, to its eyes, its middle or its feet meets no block;</li>
 *   <li>seen by another Wingman of the owner within {@link #SQUAD_RANGE} blocks (the squad shares what it sees);</li>
 *   <li>or tracked: it hunts the owner or a Wingman of the owner, it hit the owner, the owner hit it, or the owner marks it.</li>
 * </ul>
 * A known creature is remembered at the place it was last seen for {@link #MEMORY_TICKS} ticks. Under a ceiling only the
 * creatures within {@link #CONFINED_VERTICAL} blocks up or down are looked at, unless they are tracked.
 *
 * <p>For the target choice each known creature also has a travel cost ({@link #travel}): the straight distance when the
 * body fits along the line, else the cost of a grid path ({@link WingmanPathing}, at most one search per
 * {@link #REACH_EVERY} ticks, kept {@link #REACH_TICKS} ticks), or infinity when there is no path. Decision five A
 * ({@link #rejects}): a creature without a path that nobody sees is not a target; one whose path is longer than
 * {@link #DETOUR} times the straight distance (and longer than {@link #DETOUR_MIN}) is in another tunnel and is a target
 * only while it is tracked (it comes for the owner).
 */
public final class WingmanPerception {
    public static final int MEMORY_TICKS = 160, SCAN_EVERY = 5, REACH_TICKS = 40, REACH_EVERY = 10;
    public static final double CONFINED_VERTICAL = 8, SQUAD_RANGE = 48, DETOUR = 2.5, DETOUR_MIN = 32;
    /** Nodes one travel search may expand (fewer than a navigation search: it only has to tell near from far). */
    public static final int REACH_NODES = 900;

    /** One known creature. */
    public static final class Contact {
        Vec3 pos;
        /** Game time of the last sighting (every Wingman measures by the same clock, so the squad can compare). */
        long seenAt;
        /** Seen (by the Wingman, the owner or the squad) at the last scan. */
        boolean visible;
        /** Tracked (hunts, hit or was hit, marked) at the last scan. */
        boolean tracked;
        double travel = Double.NaN;
        long travelAt = Long.MIN_VALUE / 2;

        Contact(Vec3 pos, long at) { this.pos = pos; this.seenAt = at; }

        public Vec3 pos() { return pos; }
        public boolean visible() { return visible; }
        public boolean tracked() { return tracked; }
    }

    private final WingmanEntity w;
    private final Int2ObjectOpenHashMap<Contact> contacts = new Int2ObjectOpenHashMap<>();
    private long scannedAt = Long.MIN_VALUE / 2, reachAt = Long.MIN_VALUE / 2;

    WingmanPerception(WingmanEntity w) { this.w = w; }

    /** The contact of {@code e}, or null when it is not known. */
    @Nullable public Contact contact(LivingEntity e) { return contacts.get(e.getId()); }

    public boolean known(LivingEntity e) { return contacts.containsKey(e.getId()); }

    /** Forgets {@code e} (searched at its last place and not found). */
    void forget(LivingEntity e) { contacts.remove(e.getId()); }

    /** 0.1.11.224 (G5): creatures given up after a stall, and the game time until which they are no target. */
    private final it.unimi.dsi.fastutil.ints.Int2LongOpenHashMap banned = new it.unimi.dsi.fastutil.ints.Int2LongOpenHashMap();
    public static final int BAN_TICKS = 200;

    /** 0.1.11.224 (G5): {@code e} is no target for {@link #BAN_TICKS} ticks (the Wingman stalled on the way to it). */
    void ban(LivingEntity e) {
        banned.put(e.getId(), w.level().getGameTime() + BAN_TICKS);
        forget(e);
    }

    /** True while {@code e} is given up. */
    public boolean banned(LivingEntity e) {
        if (banned.isEmpty()) return false;
        long until = banned.get(e.getId());
        if (until > w.level().getGameTime()) return true;
        if (until != 0) banned.remove(e.getId());
        return false;
    }

    /** A line from {@code eye} to the eyes, the middle or the feet of {@code e} meets no block. */
    static boolean sees(Level level, Vec3 eye, LivingEntity e, LivingEntity from) {
        Vec3[] points = {e.getEyePosition(), e.position().add(0, e.getBbHeight() * 0.5, 0), e.position().add(0, 0.2, 0)};
        for (Vec3 p : points) {
            if (level.clip(new ClipContext(eye, p, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, from)).getType() == HitResult.Type.MISS) return true;
        }
        return false;
    }

    /** True when {@code e} is tracked: it hunts the owner or one of the owner's Wingmen, fought the owner, or is marked. */
    static boolean tracked(LivingEntity e, ServerPlayer owner) {
        int now = owner.tickCount;
        if (e instanceof Mob m && m.getTarget() != null
            && (m.getTarget() == owner || m.getTarget() instanceof WingmanEntity t && owner.getUUID().equals(t.owner()))) return true;
        if (e == owner.getLastHurtByMob() && now - owner.getLastHurtByMobTimestamp() < 100) return true;
        if (e == owner.getLastHurtMob() && now - owner.getLastHurtMobTimestamp() < 60) return true;
        return e == com.miophas.singularity_iteration.common.suit.quantum.QuantumSynergies.mark(owner);
    }

    /**
     * Brings the contacts up to date: every {@link #SCAN_EVERY} ticks a scan of the mobs within {@code range} of
     * {@code around} (sight, squad, tracking); in between only the places of the tracked and visible ones ({@link #refresh}).
     */
    void update(ServerPlayer owner, Vec3 around, double range) {
        Level level = w.level();
        long now = level.getGameTime();
        if (now - scannedAt >= SCAN_EVERY || now < scannedAt) {
            scannedAt = now;
            boolean confined = WingmanSpace.of(w).kind().confined();
            AABB box = new AABB(around, around).inflate(range);
            Vec3 eye = w.getEyePosition(), ownerEye = owner.getEyePosition();
            var squad = level.getEntitiesOfClass(WingmanEntity.class, w.getBoundingBox().inflate(SQUAD_RANGE),
                o -> o != w && owner.getUUID().equals(o.owner()));
            for (Contact c : contacts.values()) { c.visible = false; c.tracked = false; }
            for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, box, e -> e instanceof Mob && e.isAlive() && !(e instanceof WingmanEntity))) {
                boolean track = tracked(e, owner);
                if (confined && !track && Math.abs(e.getY() - around.y) > CONFINED_VERTICAL) continue;
                boolean seen = sees(level, eye, e, w) || sees(level, ownerEye, e, owner);
                if (!seen) {
                    for (WingmanEntity o : squad) {
                        Contact theirs = o.perception.contact(e);
                        if (theirs != null && theirs.visible && now - theirs.seenAt <= SCAN_EVERY) { seen = true; break; }
                    }
                }
                if (!seen && !track) continue;
                Contact c = contacts.get(e.getId());
                if (c == null) contacts.put(e.getId(), c = new Contact(e.position(), now));
                c.pos = e.position();
                c.seenAt = now;
                c.visible = seen;
                c.tracked = track;
            }
            contacts.int2ObjectEntrySet().removeIf(en -> now - en.getValue().seenAt > MEMORY_TICKS
                || !(level.getEntity(en.getIntKey()) instanceof LivingEntity l) || !l.isAlive());
        } else refresh();
    }

    /** The places of the tracked and visible contacts, every tick between two scans. */
    void refresh() {
        Level level = w.level();
        long now = level.getGameTime();
        for (var en : contacts.int2ObjectEntrySet()) {
            Contact c = en.getValue();
            if (!c.visible && !c.tracked) continue;
            if (level.getEntity(en.getIntKey()) instanceof LivingEntity e) { c.pos = e.position(); c.seenAt = now; }
        }
    }

    /**
     * The travel cost to {@code e}: the straight distance when the body fits along the line, else the cost of a grid path
     * (searched at most once per {@link #REACH_EVERY} ticks for this Wingman, kept {@link #REACH_TICKS} ticks), infinity
     * when there is none; NaN while not measured yet.
     */
    double travel(LivingEntity e) {
        Contact c = contacts.get(e.getId());
        if (c == null) return Double.NaN;
        long now = w.level().getGameTime();
        if (!Double.isNaN(c.travel) && now - c.travelAt < REACH_TICKS && now >= c.travelAt) return c.travel;
        Vec3 goal = c.pos;
        double straight = goal.distanceTo(w.position());
        if (straight <= WingmanNav.DIRECT_MAX && WingmanSpace.reaches(w.level(), w, goal)) {
            c.travel = straight;
            c.travelAt = now;
            return straight;
        }
        if (now - reachAt < REACH_EVERY && now >= reachAt) return c.travel;   // NaN or the old value until the next slot
        reachAt = now;
        double heightCost = WingmanSpace.of(w).kind().confined() ? WingmanNav.HEIGHT_COST : 0;
        WingmanPathing.Search s = WingmanPathing.search(w.level(), w, goal, heightCost);
        if (s != null) s.step(REACH_NODES);
        c.travel = s != null && s.status() == WingmanPathing.Status.FOUND ? s.cost() : Double.POSITIVE_INFINITY;
        c.travelAt = now;
        return c.travel;
    }

    /** Decision five A: true when {@code e} is no target (no path and unseen, or in another tunnel and not tracked). */
    boolean rejects(LivingEntity e) {
        Contact c = contacts.get(e.getId());
        if (c == null) return true;
        double t = travel(e);
        if (Double.isNaN(t)) return false;                      // not measured yet: judge by the rest of the score
        if (Double.isInfinite(t)) return !c.visible;
        double straight = c.pos.distanceTo(w.position());
        return !c.tracked && t > Math.max(DETOUR_MIN, DETOUR * straight);
    }

    /** True when the Wingman should hunt {@code e} at its last known place: not seen and not tracked now. */
    boolean lost(LivingEntity e) {
        Contact c = contacts.get(e.getId());
        return c != null && !c.visible && !c.tracked;
    }

    /** Known creatures (for tests). */
    public int size() { return contacts.size(); }
}
