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
    private int cursor = Integer.MIN_VALUE;

    // This bounds returned lists and affected entities, not the engine's predicate visits.
    // Advance by entity ID so an unconsumable first page cannot permanently starve others.
    private <T extends Entity> List<T> next(ServerLevel level, BlockPos pos, int range, Class<T> type) {
        if (!level.getServer().isSameThread() || range < 0 || range > 16)
            throw new IllegalStateException("World fuel collection requires bounded server-thread access");
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
        return result;
    }
    public int items(ServerLevel level, BlockPos pos, int range, int fuel, ToIntFunction<ItemStack> value) {
        for (var item : next(level, pos, range, ItemEntity.class)) {
            if (item.isRemoved()) continue;
            if (pos.distToCenterSqr(item.getX(), item.getY(), item.getZ()) <= 1) {
                var stack = item.getItem();
                if (stack.isEmpty()) continue;
                int credit = FuelAdmission.accepted(fuel, value.applyAsInt(stack), 1);
                if (credit == 0) continue;
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
        for (var orb : next(level, pos, range, ExperienceOrb.class)) {
            if (orb.isRemoved()) continue;
            if (pos.distToCenterSqr(orb.getX(), orb.getY(), orb.getZ()) <= 1) {
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
