// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.client.suit;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.EggItem;
import net.minecraft.world.item.EnderpearlItem;
import net.minecraft.world.item.ExperienceBottleItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.SnowballItem;
import net.minecraft.world.item.ThrowablePotionItem;
import net.minecraft.world.item.TridentItem;
import net.minecraft.world.item.component.ChargedProjectiles;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * Fire-control computer of the suit HUD. It predicts the path of the projectile that the held
 * weapon fires.
 *
 * <p><b>Integration model.</b> The solver steps the projectile one game tick at a time. Each step
 * does these operations in this order:
 * <ol>
 *   <li>Move: {@code pos = pos + vel}.</li>
 *   <li>Drag: {@code vel = vel * drag}. The solver uses {@code waterDrag} in place of
 *       {@code drag} when the new position is in a fluid source block.</li>
 *   <li>Gravity: {@code vel.y = vel.y - gravity}.</li>
 * </ol>
 * Vanilla arrows and thrown projectiles use the same order. Thus the predicted path is the path
 * that the projectile really takes in this game. An analytic parabola does not include drag, and it
 * has a different error for each tick. The solver does not use one for this reason.
 *
 * <p><b>Units.</b> Positions are world coordinates in blocks. Velocities are in blocks per tick.
 * Gravity is in blocks per tick per tick. Drag values are dimensionless multipliers per tick.
 * Times are in game ticks (20 ticks = 1 second).
 *
 * <p><b>Limits.</b> The solver does at most {@link #MAX_STEPS} steps (12 seconds of flight).
 * It does not model random inaccuracy, wind, or firework thrust.
 *
 * <p><b>Thread.</b> Call all methods on the client thread only. The player must be in a level.
 */
public final class BallisticSolver {
    private BallisticSolver() {}

    /**
     * Maximum number of integration steps (ticks) for one path. 240 ticks = 12 seconds. This limit
     * also limits the size of {@link Solution#path()} to {@code MAX_STEPS + 1} points.
     */
    public static final int MAX_STEPS = 240;

    /**
     * Launch profile of one weapon. The values copy the vanilla projectile constants.
     *
     * @param name      short HUD label of the weapon (for example "BOW").
     * @param speed     launch speed in blocks per tick, along the view axis.
     * @param gravity   downward acceleration in blocks per tick per tick. 0 = no gravity.
     * @param drag      velocity multiplier per tick in air. 1 = no drag.
     * @param waterDrag velocity multiplier per tick in a fluid source block.
     * @param ready     true when a shot released now fires at this speed (bow fully drawn,
     *                  crossbow loaded, trident charged). The HUD uses it to show "ready".
     */
    public record Profile(String name, double speed, double gravity, double drag, double waterDrag, boolean ready) { }

    /**
     * Result of one solve.
     *
     * @param profile the launch profile that the solver used.
     * @param path    the projectile position at the start and after each step, in world blocks.
     *                The first point is the launch point. The last point is the impact point when
     *                there is an impact. The list has at most {@link #MAX_STEPS} + 1 points.
     * @param impact  the point where the path hits a block or an entity. Null when the path hits
     *                nothing in {@link #MAX_STEPS} steps.
     * @param normal  the unit normal of the block face that the path hits. Null for an entity hit
     *                and for no hit.
     * @param target  the entity that the path hits. Null for a block hit and for no hit.
     * @param ticks   the time of flight to the impact in ticks. It is {@link #MAX_STEPS} when there
     *                is no impact.
     * @param drop    launch height minus impact height, in blocks. It is positive when the impact
     *                is below the launch point. It is 0 when there is no impact. It is not the drop
     *                below the line of sight.
     */
    public record Solution(Profile profile, List<Vec3> path, @Nullable Vec3 impact, @Nullable Vec3 normal,
                           @Nullable Entity target, int ticks, double drop) { }

    /**
     * Gets the launch profile for a held item.
     *
     * <p>The bow speed is 3 x draw power. The draw power is the vanilla value from the use time
     * (0..1). The solver uses a minimum power of 0.1, so a short draw still shows a path. When the
     * player does not draw the bow, the profile uses full power, so the HUD shows the full-draw
     * path. A crossbow with a firework charge uses a straight, drag-free path.
     *
     * @param player the local player that holds the item.
     * @param stack  the item stack in one hand.
     * @return the profile, or null when the item does not fire a projectile that the solver knows.
     */
    @Nullable
    public static Profile profile(LocalPlayer player, ItemStack stack) {
        if (stack.isEmpty()) return null;
        if (stack.getItem() instanceof BowItem) {
            boolean drawing = player.isUsingItem() && player.getUseItem() == stack;
            float power = drawing ? BowItem.getPowerForTime(stack.getUseDuration(player) - player.getUseItemRemainingTicks()) : 1.0F;
            return new Profile("BOW", Math.max(0.1F, power) * 3.0, 0.05, 0.99, 0.6, drawing && power >= 1.0F);
        }
        if (stack.getItem() instanceof CrossbowItem) {
            ChargedProjectiles charged = stack.get(DataComponents.CHARGED_PROJECTILES);
            boolean firework = charged != null && charged.contains(Items.FIREWORK_ROCKET);
            boolean loaded = charged != null && !charged.isEmpty();
            return firework ? new Profile("XBOW-FW", 1.6, 0.0, 1.0, 1.0, loaded) : new Profile("XBOW", 3.15, 0.05, 0.99, 0.6, loaded);
        }
        if (stack.getItem() instanceof TridentItem) {
            boolean drawing = player.isUsingItem() && player.getUseItem() == stack;
            // vanilla releases a trident only after 10 ticks of charge
            return new Profile("TRIDENT", 2.5, 0.05, 0.99, 0.99, drawing && player.getTicksUsingItem() >= 10);
        }
        if (stack.getItem() instanceof SnowballItem || stack.getItem() instanceof EggItem || stack.getItem() instanceof EnderpearlItem) {
            return new Profile(stack.getItem() instanceof EnderpearlItem ? "PEARL" : "THROW", 1.5, 0.03, 0.99, 0.8, true);
        }
        if (stack.getItem() instanceof ThrowablePotionItem) return new Profile("POTION", 0.5, 0.05, 0.99, 0.8, true);
        if (stack.getItem() instanceof ExperienceBottleItem) return new Profile("XP", 0.7, 0.07, 0.99, 0.8, true);
        if (stack.is(Items.WIND_CHARGE)) return new Profile("WIND", 1.5, 0.0, 1.0, 1.0, true);
        return null;
    }

    /**
     * Integrates the path of a shot that the player fires now.
     *
     * <p>The launch point is 0.1 blocks below the eye, as in vanilla. The launch velocity is the
     * view direction x {@link Profile#speed()} plus the player velocity. The vertical part of the
     * player velocity is not added when the player is on the ground. Vanilla does the same.
     *
     * <p>Each step tests the segment from the old to the new position. A block hit cuts the
     * segment at the block. Then the step tests entities on the cut segment. Thus an entity behind
     * a wall does not count as a hit. Entity boxes grow by 0.3 blocks, as in the vanilla projectile
     * hit test. The nearest entity on the segment wins. The solver ignores the player. It also
     * ignores the player's vehicle for the first 2 steps, because the shot starts inside it.
     *
     * <p>The loop stops early when the projectile falls 16 blocks below the bottom of the world.
     *
     * <p>Cost: one block ray cast and one entity query per step. The worst case is
     * {@link #MAX_STEPS} steps per call.
     *
     * @param player the local player. Its level must not be null.
     * @param profile the launch profile from {@link #profile}.
     * @return the solved path. It is never null.
     */
    public static Solution solve(LocalPlayer player, Profile profile) {
        Level level = player.level();
        float pitch = player.getXRot(), yaw = player.getYRot();
        double rp = Math.toRadians(pitch), ry = Math.toRadians(yaw);
        // Minecraft view vector: yaw 0 looks to +Z (south), positive pitch looks down
        Vec3 dir = new Vec3(-Math.sin(ry) * Math.cos(rp), -Math.sin(rp), Math.cos(ry) * Math.cos(rp));
        Vec3 vel = dir.scale(profile.speed());
        Vec3 own = player.getDeltaMovement();
        vel = vel.add(own.x, player.onGround() ? 0.0 : own.y, own.z);
        Vec3 pos = new Vec3(player.getX(), player.getEyeY() - 0.1, player.getZ());
        Vec3 start = pos;
        List<Vec3> path = new ArrayList<>(64);
        path.add(pos);
        for (int t = 0; t < MAX_STEPS; t++) {
            // 1. move: test the segment pos -> next against blocks first
            Vec3 next = pos.add(vel);
            BlockHitResult block = level.clip(new ClipContext(pos, next, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
            Vec3 end = block.getType() != HitResult.Type.MISS ? block.getLocation() : next;
            // then test entities on the segment that the block test cut
            Entity hit = null;
            double best = Double.MAX_VALUE;
            Vec3 hitAt = null;
            AABB sweep = new AABB(pos, end).inflate(1.0);
            for (Entity e : level.getEntities(player, sweep, EntitySelector.NO_SPECTATORS.and(Entity::isPickable))) {
                if (t < 2 && e == player.getVehicle()) continue;
                var clip = e.getBoundingBox().inflate(0.3).clip(pos, end);
                if (clip.isPresent()) {
                    double d = pos.distanceToSqr(clip.get());
                    if (d < best) { best = d; hit = e; hitAt = clip.get(); }
                }
            }
            if (hit != null) {
                path.add(hitAt);
                return new Solution(profile, path, hitAt, null, hit, t + 1, start.y - hitAt.y);
            }
            if (block.getType() != HitResult.Type.MISS) {
                path.add(end);
                Vec3 normal = Vec3.atLowerCornerOf(block.getDirection().getNormal());
                return new Solution(profile, path, end, normal, null, t + 1, start.y - end.y);
            }
            pos = next;
            path.add(pos);
            // 2. drag: water drag when the new position is in a fluid source block
            boolean water = level.getFluidState(net.minecraft.core.BlockPos.containing(pos)).isSource();
            vel = vel.scale(water ? profile.waterDrag() : profile.drag());
            // 3. gravity
            vel = vel.subtract(0, profile.gravity(), 0);
            if (pos.y < level.getMinBuildHeight() - 16) break;
        }
        return new Solution(profile, path, null, null, null, MAX_STEPS, 0);
    }

    /**
     * Calculates the lead (intercept) point for a moving target. The lead point is the position
     * of the target at the time when a shot fired now arrives at the target range.
     *
     * <p>Method:
     * <ol>
     *   <li>The aim point is the centre of the target box, at half its height.</li>
     *   <li>The range is the horizontal distance from the eye to the aim point.</li>
     *   <li>The horizontal launch speed is {@code speed * cos(pitch)}. The pitch is the current
     *       view pitch of the player.</li>
     *   <li>The loop adds the horizontal speed for each tick and multiplies the speed by the air
     *       drag. It counts the ticks {@code t} until the sum reaches the range. This is the time of
     *       flight.</li>
     *   <li>The lead point is {@code aimPoint + velocityPerTick * t}.</li>
     * </ol>
     *
     * <p>Assumptions and limits:
     * <ul>
     *   <li>The target velocity stays constant during the time of flight.</li>
     *   <li>The method does one pass only. It does not iterate the intercept. The time of flight is
     *       for the current range, not for the range of the lead point. Thus the error increases
     *       when the target moves towards or away from the shooter.</li>
     *   <li>The method ignores gravity, water drag, and the player velocity. It also ignores the
     *       pitch that the shot really needs to hit the target.</li>
     * </ul>
     *
     * @param player          the local player.
     * @param profile         the launch profile.
     * @param target          the target creature.
     * @param velocityPerTick the target velocity in blocks per tick.
     * @return the lead point in world blocks, or null when the shot cannot reach the range in
     *         {@link #MAX_STEPS} ticks, or when the horizontal speed falls below 0.001 blocks per
     *         tick.
     */
    @Nullable
    public static Vec3 lead(LocalPlayer player, Profile profile, LivingEntity target, Vec3 velocityPerTick) {
        Vec3 eye = player.getEyePosition();
        Vec3 aimPoint = target.position().add(0, target.getBbHeight() * 0.5, 0);
        double range = Math.sqrt(Math.pow(aimPoint.x - eye.x, 2) + Math.pow(aimPoint.z - eye.z, 2));
        // horizontal distance after t ticks with drag: sum of speed * drag^i for i = 0 .. t-1
        double travelled = 0, v = profile.speed() * Math.cos(Math.toRadians(player.getXRot()));
        int t = 0;
        while (travelled < range && t < MAX_STEPS) {
            travelled += v;
            v *= profile.drag();
            t++;
            // with drag < 1 the speed goes to 0. Stop when the shot cannot go further.
            if (v < 1.0E-3) return null;
        }
        if (t >= MAX_STEPS) return null;
        return aimPoint.add(velocityPerTick.scale(t));
    }
}
