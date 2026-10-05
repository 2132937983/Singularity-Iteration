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
 * Fire-control computer. Integrates the projectile of the held weapon tick by tick with the
 * vanilla integrator (move, then drag, then gravity), so the predicted path is the path the
 * projectile takes in this game, not an analytic parabola.
 */
public final class BallisticSolver {
    private BallisticSolver() {}

    public static final int MAX_STEPS = 240;

    /** Launch profile of one weapon. */
    public record Profile(String name, double speed, double gravity, double drag, double waterDrag, boolean ready) { }

    /** Solved path. {@code target} is the entity the path hits, if any. */
    public record Solution(Profile profile, List<Vec3> path, @Nullable Vec3 impact, @Nullable Vec3 normal,
                           @Nullable Entity target, int ticks, double drop) { }

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

    /** Integrates the path of a shot fired now. */
    public static Solution solve(LocalPlayer player, Profile profile) {
        Level level = player.level();
        float pitch = player.getXRot(), yaw = player.getYRot();
        double rp = Math.toRadians(pitch), ry = Math.toRadians(yaw);
        Vec3 dir = new Vec3(-Math.sin(ry) * Math.cos(rp), -Math.sin(rp), Math.cos(ry) * Math.cos(rp));
        Vec3 vel = dir.scale(profile.speed());
        Vec3 own = player.getDeltaMovement();
        vel = vel.add(own.x, player.onGround() ? 0.0 : own.y, own.z);
        Vec3 pos = new Vec3(player.getX(), player.getEyeY() - 0.1, player.getZ());
        Vec3 start = pos;
        List<Vec3> path = new ArrayList<>(64);
        path.add(pos);
        for (int t = 0; t < MAX_STEPS; t++) {
            Vec3 next = pos.add(vel);
            BlockHitResult block = level.clip(new ClipContext(pos, next, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
            Vec3 end = block.getType() != HitResult.Type.MISS ? block.getLocation() : next;
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
            boolean water = level.getFluidState(net.minecraft.core.BlockPos.containing(pos)).isSource();
            vel = vel.scale(water ? profile.waterDrag() : profile.drag());
            vel = vel.subtract(0, profile.gravity(), 0);
            if (pos.y < level.getMinBuildHeight() - 16) break;
        }
        return new Solution(profile, path, null, null, null, MAX_STEPS, 0);
    }

    /**
     * Lead point for a moving target: where the target will be when a shot fired now arrives,
     * from its current velocity and the time of flight to its range. Null when it cannot be hit.
     */
    @Nullable
    public static Vec3 lead(LocalPlayer player, Profile profile, LivingEntity target, Vec3 velocityPerTick) {
        Vec3 eye = player.getEyePosition();
        Vec3 aimPoint = target.position().add(0, target.getBbHeight() * 0.5, 0);
        double range = Math.sqrt(Math.pow(aimPoint.x - eye.x, 2) + Math.pow(aimPoint.z - eye.z, 2));
        // horizontal distance per tick with drag: speed * drag^t summed
        double travelled = 0, v = profile.speed() * Math.cos(Math.toRadians(player.getXRot()));
        int t = 0;
        while (travelled < range && t < MAX_STEPS) {
            travelled += v;
            v *= profile.drag();
            t++;
            if (v < 1.0E-3) return null;
        }
        if (t >= MAX_STEPS) return null;
        return aimPoint.add(velocityPerTick.scale(t));
    }
}
