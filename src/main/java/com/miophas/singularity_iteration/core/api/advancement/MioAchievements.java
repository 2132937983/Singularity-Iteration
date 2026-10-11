// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.core.api.advancement;

import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Entry point to grant advancements.
 * <p>
 * Most world events behind advancements (overpressure explosion, nuclear explosion, reactor meltdown and start, replicator output)
 * occur in block entities, multiblock managers or energy networks that have no player context. Thus this class uses
 * the same method as vanilla {@code CrafterBlock}: it grants the advancement to all online players in {@link #RADIUS} blocks of the trigger point.
 * <p>
 * If the advancement or criterion name does not exist, the call fails silently. Thus callers do not need a null check.
 * <p>
 * This class is in the {@code core} source set. Thus, as other classes in that source set do ({@code EUApi.MOD_ID},
 * {@code CoreSounds}), it writes the namespace as an inline literal. It does not refer to the main mod class in {@code common}.
 */
public final class MioAchievements {
    /** Namespace of this mod. */
    public static final String MOD_ID = "mio_icif";

    /** Grant radius, in blocks. */
    public static final double RADIUS = 16.0D;

    private MioAchievements() {}

    /** Grants by block position. */
    public static void at(Level level, BlockPos pos, String path, String criterion) {
        if (pos == null) {
            return;
        }
        at(level, Vec3.atCenterOf(pos), path, criterion);
    }

    /** Grants by world coordinates. */
    public static void at(Level level, Vec3 center, String path, String criterion) {
        if (!(level instanceof ServerLevel server)) {
            return;
        }
        double limit = RADIUS * RADIUS;
        for (ServerPlayer player : server.players()) {
            if (player.distanceToSqr(center) <= limit) {
                grant(player, path, criterion);
            }
        }
    }

    /** Grants to the given player only. */
    public static void grant(ServerPlayer player, String path, String criterion) {
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
        AdvancementHolder holder = player.server.getAdvancements().get(id);
        if (holder != null) {
            player.getAdvancements().award(holder, criterion);
        }
    }
}
