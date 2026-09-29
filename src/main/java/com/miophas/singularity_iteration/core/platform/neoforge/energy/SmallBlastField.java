// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.core.platform.neoforge.energy;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Independently authored small overvoltage field. The R25 frozen 96-case
 * forward experiment supports these finite spatial samples, not an original
 * implementation or a rule for other explosion strengths/materials.
 * Contains no world, entity, registry or task references.
 */
public final class SmallBlastField {
    @FunctionalInterface public interface Occupancy { boolean blocked(int x, int y, int z); }
    private record Point(double x, double y, double z) { }
    private final List<Point> points;

    public SmallBlastField(double x, double y, double z, Occupancy occupancy) {
        finite(x, y, z); Objects.requireNonNull(occupancy);
        if (Math.abs(x) > 30_000_000 || Math.abs(y) > 30_000_000 || Math.abs(z) > 30_000_000)
            throw new IllegalArgumentException("Field outside supported world coordinates");
        var result = new ArrayList<Point>(15);
        for (int latitude = 0; latitude < 3; latitude++) for (int longitude = 0; longitude < 5; longitude++) {
            double theta = latitude * 2 * Math.PI / 5, phi = longitude * 2 * Math.PI / 5;
            double dx = Math.sin(theta) * Math.cos(phi), dy = Math.cos(theta), dz = Math.sin(theta) * Math.sin(phi);
            double px = x, py = y, pz = z; boolean blocked = false;
            // Accumulate absolute positions. Replacing this with origin+4*step
            // changes the measured exact-boundary behavior at some coordinates.
            for (int step = 0; step < 4; step++) {
                px += dx; py += dy; pz += dz;
                if (occupancy.blocked((int)Math.floor(px), (int)Math.floor(py), (int)Math.floor(pz))) {
                    blocked = true; break;
                }
            }
            if (!blocked) result.add(new Point(px, py, pz));
        }
        points = List.copyOf(result);
    }
    /** Call only for entities included by the public 13-block-wide query box. */
    public int damageAt(double x, double y, double z) {
        finite(x, y, z); int result = 0;
        for (Point point : points) {
            double dx = x - point.x, dy = y - point.y, dz = z - point.z;
            if (dx * dx + dy * dy + dz * dz <= 25) result += 2;
        }
        return result;
    }
    private static void finite(double x, double y, double z) {
        if (!Double.isFinite(x) || !Double.isFinite(y) || !Double.isFinite(z))
            throw new IllegalArgumentException("Non-finite field coordinates");
    }
}
