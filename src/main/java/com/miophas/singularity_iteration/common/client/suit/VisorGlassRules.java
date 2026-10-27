// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.client.suit;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * 0.1.10.31 (S-9, optional, off by default): the visor glass of a worn nano or quantum helmet in first person. Pure
 * (java.* only); {@code VisorGlass} draws it.
 * <ul>
 *   <li>a faint dark rim at the screen edge and a colour fringe in the corners;</li>
 *   <li>a reflection streak that slides across the glass as the head turns ({@link #streakX});</li>
 *   <li>raindrops on the glass in the rain under the open sky: they appear, grow, run down and dry ({@link Drops});</li>
 *   <li>cracks when the helmet charge falls below {@link #CRACK_CHARGE} ({@link #crack}).</li>
 * </ul>
 */
public final class VisorGlassRules {
    private VisorGlassRules() {}

    public static final float RIM_ALPHA = 0.28F;
    public static final float FRINGE_ALPHA = 0.18F;
    /** Screen widths the streak moves per full turn of the head. */
    public static final float STREAK_TURNS = 2.5F;
    public static final float CRACK_CHARGE = 0.1F;
    /** Charge at which the cracks are fully shown. */
    public static final float CRACK_FULL = 0.02F;
    public static final int MAX_DROPS = 24;
    /** Chance of a new drop each tick in the rain. */
    public static final float DROP_CHANCE = 0.35F;

    /** Left edge (fraction of the screen width, -streakWidth .. 1) of the reflection streak for the head yaw (degrees). */
    public static float streakX(float yaw, float streakWidth) {
        float span = 1F + streakWidth;
        float x = (float) (yaw / 360.0 * STREAK_TURNS) % span;
        if (x < 0) x += span;
        return x - streakWidth;
    }

    /** Crack alpha 0..1 for the helmet charge (0..1). */
    public static float crack(float charge) {
        if (charge >= CRACK_CHARGE) return 0F;
        return Math.min(1F, (CRACK_CHARGE - charge) / (CRACK_CHARGE - CRACK_FULL));
    }

    /** Raindrops on the glass (screen fractions, y down). */
    public static final class Drops {
        /** One drop: position, radius (fraction of the screen height), age and life in ticks, slide speed. */
        public static final class Drop {
            public float x, y, size, speed;
            public int age, life;
        }

        private final List<Drop> drops = new ArrayList<>();
        private final Random random;

        public Drops(long seed) {
            random = new Random(seed);
        }

        public List<Drop> drops() { return drops; }

        /** One tick: new drops in the rain, growth, sliding, drying. */
        public void tick(boolean raining) {
            for (int i = drops.size() - 1; i >= 0; i--) {
                Drop d = drops.get(i);
                d.age++;
                if (d.age > 20 && random.nextFloat() < 0.08F) d.speed = 0.002F + random.nextFloat() * 0.006F;   // starts to run
                d.y += d.speed;
                if (d.age >= d.life || d.y > 1.05F) drops.remove(i);
            }
            if (raining && drops.size() < MAX_DROPS && random.nextFloat() < DROP_CHANCE) {
                Drop d = new Drop();
                d.x = 0.04F + random.nextFloat() * 0.92F;
                d.y = random.nextFloat() * 0.85F;
                d.size = 0.008F + random.nextFloat() * 0.018F;
                d.life = 60 + random.nextInt(80);
                drops.add(d);
            }
        }

        /** Alpha of a drop: fades in over 4 ticks and dries in its last 20. */
        public static float alpha(Drop d) {
            float in = Math.min(1F, d.age / 4F);
            float out = Math.min(1F, (d.life - d.age) / 20F);
            return Math.max(0F, Math.min(in, out));
        }

        public void clear() { drops.clear(); }
    }
}
