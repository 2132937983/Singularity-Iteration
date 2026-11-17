// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.core.runtime.energy;

import com.miophas.singularity_iteration.core.runtime.energy.engine.FuelAdmission;
import java.util.ArrayList;
import java.util.List;
import java.util.function.ToIntFunction;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.phys.AABB;

/** Public world entities only. One collector per block; no global entity or chunk references. */
public final class WorldFuelCollection {
    public static final int MAX_ENTITIES_PER_TICK = 64;
    /** 0.1.10.7: ticks without a search after a search that found nothing (was: two entity searches every tick). */
    public static final int IDLE_TICKS = 10;
    private int cursor = Integer.MIN_VALUE;
    private long idleUntil = Long.MIN_VALUE;

    // This bounds returned lists and affected entities, not the engine's predicate visits.
    // Advance by entity ID so an unconsumable first page cannot permanently starve others.
    private <T extends Entity> List<T> next(ServerLevel level, BlockPos pos, int range, Class<T> type) {
        if (!level.getServer().isSameThread() || range < 0 || range > 16)
            throw new IllegalStateException("World fuel collection requires bounded server-thread access");
        long now = level.getGameTime();
        if (now < idleUntil) return List.of();
        var bounds = new AABB(pos).inflate(range);
        var result = new ArrayList<T>(MAX_ENTITIES_PER_TICK);
        var test = EntityTypeTest.<Entity, T>forClass(type);
        int previous = cursor;
        level.getEntities(test, bounds, e -> !e.isRemoved() && e.getId() > previous,
            result, MAX_ENTITIES_PER_TICK);
        if (result.size() < MAX_ENTITIES_PER_TICK)
            level.getEntities(test, bounds, e -> !e.isRemoved() && e.getId() <= previous,
                result, MAX_ENTITIES_PER_TICK);
        if (!result.isEmpty()) cursor = result.getLast().getId();
        else idleUntil = now + IDLE_TICKS;
        return result;
    }
    public int items(ServerLevel level, BlockPos pos, int range, int fuel, ToIntFunction<ItemStack> value) {
        return items(level, pos, range, fuel, value, Integer.MAX_VALUE);
    }
    /**
     * 0.1.11.11: with a fuel limit. At or above it nothing more is taken (the items still drift in and wait), so a
     * generator with a full storage no longer eats the drops around it for nothing.
     */
    public int items(ServerLevel level, BlockPos pos, int range, int fuel, ToIntFunction<ItemStack> value, int limit) {
        return items(level, pos, range, fuel, value, limit, stack -> false, stack -> {});
    }

    /**
     * 0.1.11.19: with an incinerator's hooks. An item worth nothing is taken too when {@code takeWorthless} says so (it
     * burns no fuel), and {@code eaten} hears of every item taken (before the stack shrinks).
     */
    public int items(ServerLevel level, BlockPos pos, int range, int fuel, ToIntFunction<ItemStack> value, int limit,
                     java.util.function.Predicate<ItemStack> takeWorthless, java.util.function.Consumer<ItemStack> eaten) {
        // 0.1.11.22: at the limit nothing would be taken; do not search the world for entities only to skip them all
        // (the drops then stay where they are instead of being drawn in to wait)
        if (fuel >= limit) return fuel;
        for (var item : next(level, pos, range, ItemEntity.class)) {
            if (item.isRemoved()) continue;
            if (pos.distToCenterSqr(item.getX(), item.getY(), item.getZ()) <= 1) {
                if (fuel >= limit) continue;
                var stack = item.getItem();
                if (stack.isEmpty()) continue;
                int worth = value.applyAsInt(stack);
                int credit = FuelAdmission.accepted(fuel, worth, 1);
                if (credit == 0 && !(worth <= 0 && takeWorthless.test(stack))) continue;
                eaten.accept(stack);
                var remaining = stack.copy();
                remaining.shrink(1);
                item.setItem(remaining);
                if (remaining.isEmpty()) item.discard();
                fuel += credit;
            } else pull(item, pos);
        }
        return fuel;
    }
    public int experience(ServerLevel level, BlockPos pos, int range, int fuel) {
        return experience(level, pos, range, fuel, Integer.MAX_VALUE);
    }
    /** 0.1.11.11: with a fuel limit, as {@link #items(ServerLevel, BlockPos, int, int, ToIntFunction, int)}. */
    public int experience(ServerLevel level, BlockPos pos, int range, int fuel, int limit) {
        if (fuel >= limit) return fuel;   // 0.1.11.22, as items
        for (var orb : next(level, pos, range, ExperienceOrb.class)) {
            if (orb.isRemoved()) continue;
            if (pos.distToCenterSqr(orb.getX(), orb.getY(), orb.getZ()) <= 1) {
                if (fuel >= limit) continue;
                // 1.21.1's ordinary public save data stores the merged count separately from getValue().
                var saved = new CompoundTag();
                orb.addAdditionalSaveData(saved);
                int count = saved.contains("Count", Tag.TAG_INT) ? saved.getInt("Count") : 1;
                int credit = FuelAdmission.accepted(fuel, orb.getValue(), count);
                if (credit == 0) continue;
                orb.discard();
                if (orb.isRemoved()) fuel += credit;
            } else pull(orb, pos);
        }
        return fuel;
    }
    private static void pull(Entity entity, BlockPos pos) {
        double x = (pos.getX() + 0.5 - entity.getX()) / 8;
        double y = (pos.getY() + 0.5 - entity.getY()) / 8;
        double z = (pos.getZ() + 0.5 - entity.getZ()) / 8;
        double distance = Math.sqrt(x * x + y * y + z * z);
        if (!(distance > 0 && distance < 1)) return;
        double force = (1 - distance) * (1 - distance) * 0.05 / distance;
        entity.setDeltaMovement(entity.getDeltaMovement().add(x * force, y * force, z * force));
    }
}
