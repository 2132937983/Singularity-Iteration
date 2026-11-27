// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.armory;

import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 0.1.11.220 (cave overhaul G0): what the Wingman's obstacle avoidance did this tick, for the cave GameTests and for a
 * debug view. With {@code -Dsi.wingman.navDebug=true} every Wingman logs its place, target and last avoidance once a second,
 * draws a line of particles to its target, its grid path (0.1.11.222) and a red mark over its head while the avoidance pushes
 * it up or finds no way.
 */
public final class WingmanNavDebug {
    private WingmanNavDebug() {}

    private static final Logger LOG = LoggerFactory.getLogger("mio_icif/wingman-nav");
    static final boolean ENABLED = Boolean.getBoolean("si.wingman.navDebug");

    /** The outcome of one call of {@link WingmanTactics#avoid} (or of the wall climb of a steer). */
    public enum Probe {
        /** The look-ahead was clear. */
        CLEAR,
        /** The way ahead was blocked; the up-and-forward probe was clear: the Wingman climbs. */
        CLIMB,
        /** Ahead and up were blocked; down was clear: the Wingman ducks. */
        DUCK,
        /** Slid left or right past the wall. */
        SLIDE,
        /** Every probe was blocked: the fallback (slow down, drift up). */
        BLOCKED,
        /** It ran into a wall this tick and the steer added the climb of 0.3. */
        WALL_CLIMB;

        /** True when this outcome pushes the Wingman upwards. */
        public boolean upward() { return this == CLIMB || this == BLOCKED || this == WALL_CLIMB; }
    }

    static void record(WingmanEntity w, Probe probe) { w.navProbe = probe; }

    /** One server tick of the debug view; nothing unless {@link #ENABLED}. */
    static void tick(WingmanEntity w, ServerLevel world) {
        if (!ENABLED) return;
        LivingEntity target = w.currentTarget();
        Vec3 eye = w.getEyePosition();
        if (w.tickCount % 20 == 0) {
            LOG.info("wingman {} at {} target {} probe {} v={}", w.getId(), fmt(w.position()),
                target == null ? "-" : target.getType().toShortString() + "@" + fmt(target.position()), w.navProbe,
                String.format("%.2f", w.getDeltaMovement().length()));
        }
        if (w.tickCount % 5 == 0 && target != null) {
            Vec3 to = target.getEyePosition();
            for (int i = 1; i < 8; i++) {
                Vec3 p = eye.lerp(to, i / 8.0);
                world.sendParticles(ParticleTypes.END_ROD, p.x, p.y, p.z, 1, 0, 0, 0, 0);
            }
        }
        var path = w.nav.path();
        if (w.tickCount % 5 == 0 && path != null) {
            for (int i = w.nav.index(); i < path.size(); i++) {
                Vec3 p = path.get(i);
                world.sendParticles(ParticleTypes.HAPPY_VILLAGER, p.x, p.y + 0.2, p.z, 1, 0, 0, 0, 0);
            }
        }
        if (w.navProbe.upward()) {
            world.sendParticles(new DustParticleOptions(new Vector3f(1, 0.15F, 0.1F), 1.0F), w.getX(), w.getY() + w.getBbHeight() + 0.3, w.getZ(),
                1, 0, 0, 0, 0);
        }
    }

    private static String fmt(Vec3 p) { return String.format("(%.1f, %.1f, %.1f)", p.x, p.y, p.z); }
}
