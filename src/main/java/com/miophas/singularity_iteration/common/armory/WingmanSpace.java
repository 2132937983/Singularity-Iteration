// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.armory;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * 0.1.11.221 (cave overhaul G1): what the room round a Wingman looks like, measured with its whole body.
 *
 * <p>Before, the avoidance looked ahead along one thin ray from the middle of the body: it took gaps the 0.6 x 1.8 body does
 * not fit through for open, and it never asked how much room there was above the head. Here every check moves the body's
 * box ({@link #sweep}), the room above the head and below the feet is measured ({@link #headroom}, {@link #floorGap}), and
 * the place is sorted into {@link Kind#OPEN}, {@link Kind#INDOOR} or {@link Kind#TUNNEL} by the height from floor to ceiling.
 * The measures of the Wingman's own place are kept for {@link #CACHE_TICKS} ticks ({@link #of}).
 */
public final class WingmanSpace {
    private WingmanSpace() {}

    /** How confined a place is. */
    public enum Kind {
        /** No ceiling within {@link #CEILING_SCAN} blocks above the head, or floor to ceiling above {@link #INDOOR_SPAN}. */
        OPEN,
        /** A room or a cavern: floor to ceiling from {@link #TUNNEL_SPAN} to {@link #INDOOR_SPAN} blocks. */
        INDOOR,
        /** A tunnel or a low room: floor to ceiling at most {@link #TUNNEL_SPAN} blocks. */
        TUNNEL;

        /** True for a place with a ceiling near enough to matter. */
        public boolean confined() { return this != OPEN; }
    }

    /** Floor-to-ceiling heights (blocks) up to which a place is a tunnel, and a room. */
    public static final double TUNNEL_SPAN = 4.0, INDOOR_SPAN = 12.0;
    /** How far the ceiling is looked for above the head, and the floor below the feet (blocks). */
    public static final int CEILING_SCAN = 12, FLOOR_SCAN = 8;
    /** Step of a body sweep (blocks). */
    public static final double SWEEP_STEP = 0.4;
    /** Ticks the measures of a Wingman's place are kept. */
    public static final int CACHE_TICKS = 5;
    /** Most headroom measured exactly (blocks); more counts as this. */
    public static final double HEADROOM_MAX = 3.0;

    /** The measures of one place. */
    public record Room(Kind kind, double headroom, double floorGap, double span) { }

    /**
     * How far the box {@code box} of {@code who} moves along {@code dir} (a unit vector) before it meets a block, at most
     * {@code length}, in steps of {@link #SWEEP_STEP} (the last step is exact to the length).
     */
    public static double sweep(Level level, Entity who, AABB box, Vec3 dir, double length) {
        double done = 0;
        while (done < length) {
            double next = Math.min(length, done + SWEEP_STEP);
            if (!level.noCollision(who, box.move(dir.scale(next)))) return done;
            done = next;
        }
        return length;
    }

    /** True when the body of {@code who} fits all along the straight line from its place to {@code to}. */
    public static boolean reaches(Level level, Entity who, Vec3 to) {
        Vec3 d = to.subtract(who.position());
        double len = d.length();
        if (len < 1.0E-4) return true;
        return sweep(level, who, who.getBoundingBox(), d.scale(1 / len), len) >= len;
    }

    /** Air above the head of {@code who} before a block, at most {@link #HEADROOM_MAX} (in quarter blocks). */
    public static double headroom(Level level, Entity who) {
        AABB box = who.getBoundingBox();
        double free = 0;
        for (double d = 0.25; d <= HEADROOM_MAX + 1.0E-6; d += 0.25) {
            if (!level.noCollision(who, box.move(0, d, 0))) return free;
            free = d;
        }
        return HEADROOM_MAX;
    }

    /** Air below the feet at {@code feet} (blocks, to the top of the first block with a collision shape), at most {@link #FLOOR_SCAN}. */
    public static double floorGap(Level level, Vec3 feet) {
        BlockPos pos = BlockPos.containing(feet);
        for (int i = 0; i <= FLOOR_SCAN; i++) {
            BlockPos p = pos.below(i);
            if (!level.isLoaded(p)) return FLOOR_SCAN;
            var shape = level.getBlockState(p).getCollisionShape(level, p);
            if (!shape.isEmpty()) {
                double top = p.getY() + shape.max(net.minecraft.core.Direction.Axis.Y);
                if (top <= feet.y + 1.0E-6) return feet.y - top;
            }
        }
        return FLOOR_SCAN;
    }

    /** The first block with a collision shape above {@code y} in the column of {@code at}, as a height above {@code y}, or -1. */
    static double ceilingAbove(Level level, Vec3 at, double y) {
        BlockPos pos = BlockPos.containing(at.x, y, at.z);
        for (int i = 0; i <= CEILING_SCAN; i++) {
            BlockPos p = pos.above(i);
            if (!level.isLoaded(p)) return -1;
            var shape = level.getBlockState(p).getCollisionShape(level, p);
            if (!shape.isEmpty()) {
                double bottom = p.getY() + shape.min(net.minecraft.core.Direction.Axis.Y);
                if (bottom >= y - 1.0E-6) return bottom - y;
            }
        }
        return -1;
    }

    /** The measures of the place of {@code who} (not cached). */
    public static Room measure(Level level, Entity who) {
        double head = headroom(level, who);
        double floor = floorGap(level, who.position());
        double top = who.getY() + who.getBbHeight();
        double ceiling = ceilingAbove(level, who.position(), top);
        double span = ceiling < 0 ? Double.POSITIVE_INFINITY : floor + who.getBbHeight() + ceiling;
        Kind kind = span <= TUNNEL_SPAN ? Kind.TUNNEL : span <= INDOOR_SPAN ? Kind.INDOOR : Kind.OPEN;
        return new Room(kind, head, floor, span);
    }

    /** The measures of the Wingman's place, kept for {@link #CACHE_TICKS} ticks. */
    static Room of(WingmanEntity w) {
        if (w.room == null || w.tickCount - w.roomAt >= CACHE_TICKS || w.tickCount < w.roomAt) {
            w.room = measure(w.level(), w);
            w.roomAt = w.tickCount;
        }
        return w.room;
    }

    /** Most upward speed (blocks per tick) a velocity may keep with {@code headroom} blocks above the head. */
    public static double upLimit(double headroom) {
        return Math.max(0, headroom - 0.05);
    }

    /** {@code up} cut so that a point that far above the Wingman stays under the ceiling (with 0.3 blocks spare). */
    public static double clipRise(double up, double headroom) {
        return up <= 0 ? up : Math.min(up, Math.max(0, headroom - 0.3));
    }
}
