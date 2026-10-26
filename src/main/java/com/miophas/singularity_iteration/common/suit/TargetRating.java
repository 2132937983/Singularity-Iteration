// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.suit;

import com.miophas.singularity_iteration.common.fcs.TargetPriority;
import net.minecraft.world.entity.FlyingMob;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.NeutralMob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.FlyingAnimal;
import net.minecraft.world.entity.monster.Blaze;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.monster.Ghast;
import net.minecraft.world.entity.monster.Guardian;
import net.minecraft.world.entity.monster.RangedAttackMob;
import net.minecraft.world.entity.monster.Shulker;
import net.minecraft.world.entity.monster.Vex;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

/**
 * 0.1.10.23 (FCS P6): the {@link TargetPriority.Rating} of a creature, on both sides (the server assigns guided shots,
 * the client ranks the behaviour predictor tags). Uses only data that both sides have: the default attack damage of
 * the type (attack damage modifiers are not sent to clients), the kind of creature and its motion.
 */
public final class TargetRating {
    private TargetRating() {}

    /** Damage of one hit: the attack damage attribute, at least the typical hit of a ranged attacker; 0 for creepers. */
    public static double damage(LivingEntity e) {
        if (e instanceof Creeper) return 0;
        double d = e.getAttributes().hasAttribute(Attributes.ATTACK_DAMAGE) ? e.getAttributeValue(Attributes.ATTACK_DAMAGE) : 0;
        if (e instanceof Ghast) return Math.max(d, 6);
        if (e instanceof Guardian) return Math.max(d, 6);
        if (e instanceof Blaze || e instanceof RangedAttackMob) return Math.max(d, 4);
        if (e instanceof Shulker) return Math.max(d, 4);
        if (e instanceof Player) return Math.max(d, 4);
        return d;
    }

    /** Explosion radius: 3 for a creeper (6 when charged), 1 for a ghast fireball, else 0. */
    public static double blast(LivingEntity e) {
        if (e instanceof Creeper creeper) return creeper.isPowered() ? 6 : 3;
        if (e instanceof Ghast) return 1;
        return 0;
    }

    /** True for a creature that attacks from range. */
    public static boolean ranged(LivingEntity e) {
        return e instanceof RangedAttackMob || e instanceof Ghast || e instanceof Blaze || e instanceof Guardian || e instanceof Shulker;
    }

    /** Blocks from which it can attack: 15 for ranged attackers, else about arm's length. */
    public static double reach(LivingEntity e) {
        return ranged(e) ? 15 : 1.5 + e.getBbWidth();
    }

    /**
     * 0 passive, 1 neutral, 2 hostile, 3 watches the wearer, 4 targets the wearer.
     *
     * @param threatLevel 0, 1 (watches) or 2 (targets) from the threat sensor; 0 when unknown
     */
    public static int hostility(LivingEntity e, int threatLevel) {
        if (threatLevel >= 2) return 4;
        if (threatLevel == 1) return 3;
        if (e instanceof Enemy) return 2;
        if (e instanceof NeutralMob || e instanceof Player) return 1;
        return 0;
    }

    /** True for a creature that flies (the guided shot keeps pursuit against it, see {@code fcs.Guidance#choose}). */
    public static boolean flies(LivingEntity e) {
        return e.isNoGravity() || e instanceof FlyingMob || e instanceof FlyingAnimal || e instanceof Vex || e instanceof Blaze;
    }

    /**
     * The rating of {@code e} for a wearer at {@code wearer}.
     *
     * @param velocity    its velocity estimate (blocks per tick)
     * @param threatLevel see {@link #hostility}
     * @param attackTicks ticks to its attack when known, else -1
     */
    public static TargetPriority.Rating rating(LivingEntity e, Vec3 wearer, Vec3 velocity, int threatLevel, double attackTicks) {
        Vec3 to = wearer.subtract(e.position());
        double d = to.length();
        double closing = d < 1.0E-3 ? 0 : velocity.dot(to.scale(1 / d));
        return new TargetPriority.Rating(d, closing, damage(e), blast(e), hostility(e, threatLevel), reach(e), attackTicks);
    }
}
