// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.armory;

import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * Adaptive SlashBlade combat style selector and ground-skating kinematics for Wingmen:
 * <ul>
 *   <li><b>{@link Style#DRAGON} (Swarm Dragon)</b>: engages crowds (&ge; {@value #SWARM_COUNT}
 *       hostiles within {@value #SWARM_RADIUS} blocks, non-boss target, no explicit FOCUS order)
 *       with fast sweeping chains ({@code GROUND_C / GROUND_A}) and long ground thruster drifts
 *       that weave between different targets.</li>
 *   <li><b>{@link Style#DUELIST} (Disciplined Duelist)</b>: engages single targets, bosses/elites
 *       (max HP &ge; {@value #BOSS_HEALTH}), or owner-focused targets with high-burst grounded
 *       chains ({@code GROUND_B / GROUND_A}) and tight 105&deg; back-orbit thruster drifts.</li>
 * </ul>
 * Pure math &amp; state machine so GameTests can verify transitions, hysteresis, and skate vectors.
 */
public final class WingmanBladeStyle {
    public enum Style { DUELIST, DRAGON }

    /** Hostiles within {@link #SWARM_RADIUS} required to trigger Swarm Dragon style. */
    public static final int SWARM_COUNT = 3;
    public static final double SWARM_RADIUS = 6.0;
    /** Target max health at or above which the Wingman always commits to Duelist style. */
    public static final float BOSS_HEALTH = 60.0F;
    /** Minimum ticks a style holds before switching back (hysteresis prevents flicker). */
    public static final int MIN_HOLD_TICKS = 40;

    private Style current = Style.DUELIST;
    private long switchedAt = Long.MIN_VALUE / 2;

    public Style current() { return current; }

    /**
     * Updates and returns the active combat style. An explicit FOCUS order bypasses hysteresis and
     * forces {@link Style#DUELIST} immediately.
     */
    public Style update(long now, int nearbyHostiles, float targetMaxHealth, boolean focused) {
        if (focused) {
            if (current != Style.DUELIST) {
                current = Style.DUELIST;
                switchedAt = now;
            }
            return current;
        }
        Style want = (nearbyHostiles >= SWARM_COUNT && targetMaxHealth < BOSS_HEALTH) ? Style.DRAGON : Style.DUELIST;
        if (want != current && now - switchedAt >= MIN_HOLD_TICKS) {
            current = want;
            switchedAt = now;
        }
        return current;
    }

    /** Picks the next grounded SlashBlade chain suited to {@code style}. */
    public static BladeChain.Kind pickGroundChain(Style style, int step) {
        if (style == Style.DRAGON) {
            return (step & 1) == 0 ? BladeChain.Kind.GROUND_C : BladeChain.Kind.GROUND_A;
        }
        return (step & 1) == 0 ? BladeChain.Kind.GROUND_B : BladeChain.Kind.GROUND_A;
    }

    /**
     * Ground-hugging footwork velocity during a grounded combo chain: glides horizontally toward
     * {@code desiredDist} from the enemy while applying gentle downward stickiness ({@code -0.1})
     * so {@code onGround} stays true and knocked-back targets don't break the chain.
     */
    public static Vec3 groundFootwork(Vec3 selfPos, Vec3 enemyPos, double desiredDist) {
        Vec3 flat = new Vec3(enemyPos.x - selfPos.x, 0, enemyPos.z - selfPos.z);
        double d = flat.length();
        if (d < 1.0E-4) return new Vec3(0, -0.1, 0);
        double err = d - desiredDist;
        if (Math.abs(err) < 0.25) return new Vec3(0, -0.1, 0);
        double speed = Math.max(-0.22, Math.min(0.32, err * 0.28));
        Vec3 dir = flat.scale(1.0 / d);
        return new Vec3(dir.x * speed, -0.1, dir.z * speed);
    }

    /**
     * Unit horizontal direction for a post-finisher ground thruster drift:
     * <ul>
     *   <li>{@link Style#DRAGON}: weaves toward the flank of {@code nextEnemyPos} (biasing outward
     *       away from {@code ownerPos} when in Escort mode), or sweeps laterally out of a crowd.</li>
     *   <li>{@link Style#DUELIST}: tight 105&deg; lateral-behind orbit around {@code enemyPos}.</li>
     * </ul>
     */
    public static Vec3 finisherDriftDir(Style style, Vec3 selfPos, Vec3 enemyPos, @Nullable Vec3 nextEnemyPos,
                                        @Nullable Vec3 ownerPos, boolean escort, int strafeSign) {
        double sign = strafeSign >= 0 ? 1.0 : -1.0;
        if (style == Style.DRAGON && nextEnemyPos != null) {
            Vec3 toNext = new Vec3(nextEnemyPos.x - selfPos.x, 0, nextEnemyPos.z - selfPos.z);
            if (toNext.lengthSqr() > 1.0E-4) {
                Vec3 dir = toNext.normalize();
                Vec3 perp = new Vec3(-dir.z * sign, 0, dir.x * sign);
                Vec3 blended = dir.scale(0.82).add(perp.scale(0.45));
                if (escort && ownerPos != null) {
                    Vec3 awayFromOwner = new Vec3(selfPos.x - ownerPos.x, 0, selfPos.z - ownerPos.z);
                    if (awayFromOwner.lengthSqr() > 1.0E-4 && blended.dot(awayFromOwner.normalize()) < -0.25) {
                        blended = dir.scale(0.75).add(awayFromOwner.normalize().scale(0.55));
                    }
                }
                return blended.lengthSqr() > 1.0E-4 ? blended.normalize() : dir;
            }
        }
        // Duelist tight back-orbit (or Dragon fallback lateral sweep)
        Vec3 toEnemy = new Vec3(enemyPos.x - selfPos.x, 0, enemyPos.z - selfPos.z);
        Vec3 fwd = toEnemy.lengthSqr() > 1.0E-4 ? toEnemy.normalize() : new Vec3(1, 0, 0);
        Vec3 side = new Vec3(-fwd.z * sign, 0, fwd.x * sign);
        if (style == Style.DUELIST) {
            // Orbit around and behind the enemy (tight flank-behind vector)
            return fwd.scale(0.45).add(side.scale(0.90)).normalize();
        }
        // Dragon lateral break-out
        Vec3 drift = side.scale(0.85).add(fwd.scale(-0.35));
        if (escort && ownerPos != null) {
            Vec3 away = new Vec3(selfPos.x - ownerPos.x, 0, selfPos.z - ownerPos.z);
            if (away.lengthSqr() > 1.0E-4 && drift.dot(away.normalize()) < 0) {
                drift = side.scale(-0.85).add(fwd.scale(-0.35));
            }
        }
        return drift.normalize();
    }
}
