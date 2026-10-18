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

/**
 * Shared projectile lifecycle; all mutations are server authoritative and chunk bounded.
 *
 * <p>0.1.10.4: the exact velocity and the launch point travel with the spawn data. The vanilla
 * entity packets clamp each velocity axis to 3.9 blocks per tick, so a plasma bolt (8 blocks per
 * tick) flew off at an angle on the client: aimed 30 degrees to the side, x stayed 3.9 while z was
 * cut from 6.9 to 3.9, and the bolt drifted away from the crosshair until the next position
 * update pulled it back. The client now keeps the exact direction and ignores the clamped motion
 * packets (both sides apply the same drag).
 */
abstract class IndependentElectricProjectile extends ThrowableProjectile implements net.neoforged.neoforge.entity.IEntityWithComplexSpawn {
    private int remainingTicks = 200;
    private double remainingRange = 64.0D;
    private Vec3 resumeAt;
    /** 0.1.10.4: launch point (shooter eye), for the trail of the renderers. */
    private Vec3 origin = Vec3.ZERO;
    /** 0.1.10.4 client: the exact velocity came with the spawn data; clamped motion packets are ignored. */
    private boolean exactMotion;

    protected IndependentElectricProjectile(EntityType<? extends IndependentElectricProjectile> type, Level level) {
        super(type, level);
        setNoGravity(true);
    }
    protected IndependentElectricProjectile(EntityType<? extends IndependentElectricProjectile> type, LivingEntity owner, Level level) {
        super(type, owner, level);
        setNoGravity(true);
        setPos(owner.getX(), owner.getEyeY() - 0.1D, owner.getZ());
        origin = position();
    }
    @Override protected void defineSynchedData(SynchedEntityData.Builder builder) {}

    /** 0.1.10.4: the launch point (client: from the spawn data). */
    public Vec3 origin() { return origin; }

    @Override
    public void writeSpawnData(net.minecraft.network.RegistryFriendlyByteBuf buffer) {
        Vec3 v = getDeltaMovement();
        buffer.writeDouble(v.x); buffer.writeDouble(v.y); buffer.writeDouble(v.z);
        buffer.writeDouble(origin.x); buffer.writeDouble(origin.y); buffer.writeDouble(origin.z);
    }

    @Override
    public void readSpawnData(net.minecraft.network.RegistryFriendlyByteBuf buffer) {
        Vec3 v = new Vec3(buffer.readDouble(), buffer.readDouble(), buffer.readDouble());
        origin = new Vec3(buffer.readDouble(), buffer.readDouble(), buffer.readDouble());
        if (!Double.isFinite(v.x) || !Double.isFinite(v.y) || !Double.isFinite(v.z)) return;
        setDeltaMovement(v);
        double flat = v.horizontalDistance();
        setYRot((float) (net.minecraft.util.Mth.atan2(v.x, v.z) * (180F / Math.PI)));
        setXRot((float) (net.minecraft.util.Mth.atan2(v.y, flat) * (180F / Math.PI)));
        yRotO = getYRot();
        xRotO = getXRot();
        exactMotion = true;
    }

    @Override
    public void lerpMotion(double x, double y, double z) {
        if (exactMotion) return;            // a motion packet would clamp the direction again
        super.lerpMotion(x, y, z);
    }

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

    /** 0.1.10.4: an AI shooter (Wingman) never changes terrain; only a player who may build does. */
    protected final boolean terrainAllowed() {
        return getOwner() instanceof ServerPlayer player && player.mayBuild();
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
