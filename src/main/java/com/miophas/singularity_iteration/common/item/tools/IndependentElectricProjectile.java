// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.item.tools;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableProjectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/** Shared projectile lifecycle; all mutations are server authoritative and chunk bounded. */
abstract class IndependentElectricProjectile extends ThrowableProjectile {
    private int remainingTicks = 200;
    private double remainingRange = 64.0D;
    private Vec3 resumeAt;

    protected IndependentElectricProjectile(EntityType<? extends IndependentElectricProjectile> type, Level level) {
        super(type, level);
        setNoGravity(true);
    }
    protected IndependentElectricProjectile(EntityType<? extends IndependentElectricProjectile> type, LivingEntity owner, Level level) {
        super(type, owner, level);
        setNoGravity(true);
        setPos(owner.getX(), owner.getEyeY() - 0.1D, owner.getZ());
    }
    @Override protected void defineSynchedData(SynchedEntityData.Builder builder) {}

    protected final void setRange(double range) {
        remainingRange = Double.isFinite(range) ? Math.clamp(range, 0.0D, 256.0D) : 0.0D;
    }
    protected final void resumeAtImpact(Vec3 location) { resumeAt = location; }

    @Override public void tick() {
        Vec3 start = position();
        Vec3 next = start.add(getDeltaMovement());
        if (!level().isClientSide && (--remainingTicks < 0 || remainingRange <= 0.0D
                || !level().hasChunksAt(BlockPos.containing(start), BlockPos.containing(next)))) {
            discard();
            return;
        }
        double travel = getDeltaMovement().length();
        if (!level().isClientSide && travel > remainingRange && travel > 0.0D) {
            setDeltaMovement(getDeltaMovement().scale(remainingRange / travel));
        }
        // ThrowableProjectile owns collision and movement. Resume from a mined block's face,
        // so its remaining movement cannot skip another block in the same tick.
        super.tick();
        if (resumeAt != null && !isRemoved()) {
            setPos(resumeAt);
            resumeAt = null;
        }
        if (!level().isClientSide) remainingRange -= position().distanceTo(start);
    }

    protected final void damage(Entity target, float amount) {
        if (!(level() instanceof ServerLevel) || amount <= 0.0F) return;
        LivingEntity owner = getOwner() instanceof LivingEntity living ? living : null;
        target.hurt(owner == null ? damageSources().magic() : damageSources().mobProjectile(this, owner), amount);
    }

    protected final void explode(Vec3 center, float strength, boolean terrain) {
        if (!(level() instanceof ServerLevel level) || !(getOwner() instanceof ServerPlayer player)
                || !player.mayBuild() || strength <= 0.0F) return;
        BlockPos pos = BlockPos.containing(center);
        int radius = (int)Math.ceil(strength * 2.0D) + 1;
        if (!level.mayInteract(player, pos)
                || !level.hasChunksAt(pos.offset(-radius, 0, -radius), pos.offset(radius, 0, radius))) return;
        // Vanilla/NeoForge explosion events remain available to protection mods.
        level.explode(player, center.x, center.y, center.z, strength,
                terrain ? Level.ExplosionInteraction.TNT : Level.ExplosionInteraction.NONE);
    }

    @Override protected void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("ScexRemainingTicks", remainingTicks);
        tag.putDouble("ScexRemainingRange", remainingRange);
    }
    @Override protected void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        remainingTicks = Math.clamp(tag.getInt("ScexRemainingTicks"), 0, 200);
        setRange(tag.getDouble("ScexRemainingRange"));
        setNoGravity(true);
    }
}
