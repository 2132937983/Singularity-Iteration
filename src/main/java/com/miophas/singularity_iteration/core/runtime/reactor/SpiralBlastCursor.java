// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.core.runtime.reactor;

/** Resumable traversal of the existing column spiral; empty columns consume bounded steps too. */
public final class SpiralBlastCursor {
    public enum Step { COLUMN, POSITION, DONE }
    private final long limit;
    private final double radiusSquared;
    private long n = 1;
    private int x, z, y, nextY, bottom;
    private boolean columnOpen, exhausted;

    public SpiralBlastCursor(int scanRadius, int effectRadius) {
        if (scanRadius < 0 || effectRadius < 0) throw new IllegalArgumentException("Negative blast radius");
        long square = (long) scanRadius * scanRadius;
        limit = square > Long.MAX_VALUE / 4 ? Long.MAX_VALUE : square * 4;
        radiusSquared = (double) effectRadius * effectRadius;
    }

    public Step next() {
        if (columnOpen && nextY >= bottom) {
            y = nextY--;
            return Step.POSITION;
        }
        columnOpen = false;
        if (exhausted || n > limit) return Step.DONE;
        long shell = (long) Math.floor((Math.sqrt(n) + 1) / 2);
        long shell2 = shell * 2;
        long offset = n - (shell2 - 1) * (shell2 - 1);
        long leg = offset / shell2;
        long element = offset - shell2 * leg - shell + 1;
        x = (int) (leg == 0 ? shell : leg == 1 ? -element : leg == 2 ? -shell : element);
        z = (int) (leg == 0 ? element : leg == 1 ? shell : leg == 2 ? -element : -shell);
        if (n == limit) exhausted = true;
        else n++;
        double heightSquared = radiusSquared - (double) x * x - (double) z * z;
        if (heightSquared > 0) {
            nextY = (int) Math.sqrt(heightSquared);
            bottom = -nextY;
            columnOpen = true;
        }
        return Step.COLUMN;
    }

    public boolean hasColumn() { return columnOpen; }
    public void skipColumn() { columnOpen = false; }
    public int x() { return x; }
    public int y() { return y; }
    public int z() { return z; }
}
