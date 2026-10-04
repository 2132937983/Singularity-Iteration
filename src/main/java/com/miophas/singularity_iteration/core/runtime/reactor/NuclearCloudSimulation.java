// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.core.runtime.reactor;

import java.util.Random;

/** Independent, bounded cloud simulation. Coordinates are local to the detonation.
 * Persistent parcels circulate through a rising stem and a toroidal cap; no particle-engine allocations.
 */
public final class NuclearCloudSimulation {
    public static final int MAX_PARCELS = 1536;
    public static final int LIFETIME = 2000;
    public static final double SHOCK_SPEED = 2.25;
    public static final class Parcel {
        public double x, y, z, oldX, oldY, oldZ;
        public float size, alpha, red, green, blue;
        private final double azimuth, phase, spread;
        private final int kind;
        private Parcel(Random random, int kind) {
            this.kind = kind;
            azimuth = random.nextDouble() * Math.PI * 2;
            phase = random.nextDouble() * Math.PI * 2;
            spread = random.nextDouble();
        }
    }
    private final Parcel[] parcels;
    private final double scale;
    private int age;

    public NuclearCloudSimulation(int radius, double density, long seed) {
        if (radius < 1 || radius > 2000 || !Double.isFinite(density) || density < 0)
            throw new IllegalArgumentException("Invalid nuclear cloud parameters");
        scale = Math.max(1, Math.min(6, Math.sqrt(radius / 24.0)));
        int count = (int) Math.min(MAX_PARCELS, 768 * density);
        parcels = new Parcel[count];
        Random random = new Random(seed);
        for (int i = 0; i < count; i++) {
            parcels[i] = new Parcel(random, i % 12 < 7 ? 0 : i % 12 < 10 ? 1 : i % 12 == 10 ? 2 : 3);
            position(parcels[i]);
            parcels[i].oldX = parcels[i].x; parcels[i].oldY = parcels[i].y; parcels[i].oldZ = parcels[i].z;
        }
    }
    public void tick() {
        if (isComplete()) return;
        age++;
        for (Parcel p : parcels) {
            p.oldX = p.x; p.oldY = p.y; p.oldZ = p.z;
            position(p);
        }
    }
    private void position(Parcel p) {
        double growth = 1 - Math.exp(-age / 170.0);
        double height = scale * (5 + 33 * growth);
        double width = scale * (3 + 19 * growth);
        double theta = p.azimuth + age * .0006;
        double radial;
        if (p.kind == 0) { // Rolling cap: outward at the top, inward underneath.
            double phase = p.phase + age * .013 / scale;
            if (p.spread < .42) {
                // Fill the crown above the torus; an empty ring projects as two smoke loops.
                double fill = Math.sqrt(p.spread / .42);
                radial = width * fill * .9;
                p.y = height + width * (.18 + .24 * (1 - fill)) + Math.sin(phase) * width * .12;
            } else {
                double tube = width * .4 * Math.sqrt((p.spread - .42) / .58);
                radial = width + Math.cos(phase) * tube;
                p.y = height + Math.sin(phase) * tube;
            }
        } else if (p.kind == 1) { // Narrow rising stem feeds the cap.
            double cycle = (p.spread + age * .0025 / scale) % 1;
            radial = scale * (2 + 3 * Math.sin(cycle * Math.PI)) * (.3 + p.spread);
            p.y = height * cycle;
        } else if (p.kind == 2) { // Surface dust follows the pressure front, then settles.
            radial = shockRadius(Math.min(age, 160)) * (.88 + p.spread * .12);
            p.y = scale * (1 + p.spread * 2);
        } else { // Condensation halo, short-lived and pale.
            radial = width * (1.5 + p.spread * .2);
            p.y = height * (.58 + .2 * p.spread);
        }
        p.x = Math.cos(theta) * radial;
        p.z = Math.sin(theta) * radial;
        p.size = (float) (scale * (2 + p.spread * 3) * (.5 + growth));
        double fade = Math.min(1, (LIFETIME - age) / 360.0);
        if (p.kind == 2) fade *= Math.max(0, 1 - age / 280.0);
        if (p.kind == 3) fade *= Math.min(1, age / 20.0) * Math.max(0, 1 - age / 220.0);
        p.alpha = (float) (Math.max(0, fade) * (p.kind == 3 ? .17 : .36));
        double heat = Math.exp(-age / 90.0) * (p.kind == 0 ? 1 : .5);
        double grey = p.kind >= 2 ? .72 : .19 + .22 * p.spread;
        p.red = (float) (grey + (1 - grey) * heat);
        p.green = (float) (grey + (.48 - grey) * heat);
        p.blue = (float) (grey * (1 - heat));
    }
    public Parcel[] parcels() { return parcels; }
    public int age() { return age; }
    public boolean isComplete() { return age >= LIFETIME; }
    public static double shockRadius(double age) { return Math.max(0, age) * SHOCK_SPEED; }
    public static double flash(double age) { return age < 0 || age >= 60 ? 0 : Math.exp(-age / 9.0); }
    public static double shake(double sinceArrival) {
        return sinceArrival < 0 || sinceArrival >= 60 ? 0 : Math.exp(-sinceArrival / 16.0);
    }
}
