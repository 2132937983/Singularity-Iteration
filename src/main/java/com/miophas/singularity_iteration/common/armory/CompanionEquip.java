// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.armory;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * Remote equip of an Armory suit onto a companion (0.1.8.0).
 *
 * <p>Targets: living entities whose type is in the entity type tag
 * {@code mio_icif:armory/remote_equip} and that the player owns ({@link OwnableEntity}). The
 * shipped tag holds {@code touhou_little_maid:maid} as an optional entry, so Touhou Little Maid
 * maids work when that mod is present. Maids keep their armor and held items in the vanilla
 * equipment slots, so no class of that mod is used. A data pack can add more companion types.
 *
 * <p>Selection: the companion that the player looks at (up to {@link #LOOK_RANGE} blocks), else
 * the nearest companion within {@link #NEAR_RANGE} blocks.
 */
public final class CompanionEquip {
    private CompanionEquip() {}

    public static final TagKey<EntityType<?>> TARGETS = TagKey.create(Registries.ENTITY_TYPE,
        ResourceLocation.fromNamespaceAndPath("mio_icif", "armory/remote_equip"));
    static final double LOOK_RANGE = 48, NEAR_RANGE = 32;

    public static Component equip(ServerPlayer player, @Nullable mio_icif_armory armory, int set) {
        if (armory == null) return Component.translatable("message.mio_icif.armory.missing");
        LivingEntity wearer = find(player);
        if (wearer == null) return Component.translatable("message.mio_icif.companion.none", (int) NEAR_RANGE);
        return armory.summonTo(player, wearer, set).message();
    }

    static boolean eligible(Entity e, ServerPlayer player) {
        return e instanceof LivingEntity living && living.isAlive() && e.getType().is(TARGETS)
            && e instanceof OwnableEntity own && player.getUUID().equals(own.getOwnerUUID());
    }

    @Nullable
    static LivingEntity find(ServerPlayer player) {
        Vec3 eye = player.getEyePosition(), end = eye.add(player.getViewVector(1.0F).scale(LOOK_RANGE));
        EntityHitResult hit = ProjectileUtil.getEntityHitResult(player, eye, end,
            player.getBoundingBox().expandTowards(end.subtract(eye)).inflate(1), e -> eligible(e, player), LOOK_RANGE * LOOK_RANGE);
        if (hit != null && hit.getEntity() instanceof LivingEntity looked) return looked;
        LivingEntity best = null;
        double bestD = Double.MAX_VALUE;
        for (LivingEntity e : player.level().getEntitiesOfClass(LivingEntity.class, new AABB(player.position(), player.position()).inflate(NEAR_RANGE),
                e -> eligible(e, player))) {
            double d = e.distanceToSqr(player);
            if (d < bestD) { bestD = d; best = e; }
        }
        return best;
    }
}
