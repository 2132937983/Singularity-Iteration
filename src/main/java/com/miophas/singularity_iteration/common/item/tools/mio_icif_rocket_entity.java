package com.miophas.singularity_iteration.common.item.tools;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableProjectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@SuppressWarnings("null")
public class mio_icif_rocket_entity extends ThrowableProjectile {
    /** 0.1.11.193 (T0, W-13): passes through the shooter's allies and a Wingman's owner. */
    @Override
    protected boolean canHitEntity(net.minecraft.world.entity.Entity target) {
        return super.canHitEntity(target) && !ProjectileFriendlyFire.spares(getOwner(), target);
    }

    private float damage = 0.0F;
    private float explosionPower = mio_icif_rocket_launcher.EXPLOSION_POWER;
    private int maxLife = 200;
    private int life = 0;
    private final Set<Entity> hitEntities = new HashSet<>();

    public mio_icif_rocket_entity(EntityType<? extends mio_icif_rocket_entity> entityType, Level level) {
        super(entityType, level);
        this.setNoGravity(false);
    }

    public mio_icif_rocket_entity(EntityType<? extends mio_icif_rocket_entity> entityType, LivingEntity owner, Level level) {
        super(entityType, owner, level);
        this.setNoGravity(false);
        this.setPos(owner.getX(), owner.getEyeY() - 0.1, owner.getZ());
    }

    @Override
    protected void defineSynchedData(net.minecraft.network.syncher.SynchedEntityData.Builder builder) {
    }

    public void setDamage(float damage) {
        this.damage = damage;
    }

    /** 0.1.8.4: false for AI shooters (Wingman): the blast breaks no blocks. Saved since 0.1.11.3. */
    private boolean terrainDamage = true;

    public void setTerrainDamage(boolean terrain) {
        this.terrainDamage = terrain;
    }

    public void setExplosionPower(float power) {
        this.explosionPower = power;
    }

    @Override
    public void tick() {
        // 0.1.11.3: the collision check runs before the move. It used to run after super.tick() had already moved the
        // rocket, and then moved it again (setPos(end)): twice the velocity per tick, with every other segment never
        // checked, so rockets went through walls and mobs; the client, which moved it once, drifted.
        if (!this.level().isClientSide && this.life < this.maxLife) {
            Vec3 start = this.position();
            Vec3 velocity = this.getDeltaMovement();
            Vec3 end = start.add(velocity);

            BlockHitResult blockHit = this.level().clip(
                new net.minecraft.world.level.ClipContext(start, end,
                    net.minecraft.world.level.ClipContext.Block.COLLIDER,
                    net.minecraft.world.level.ClipContext.Fluid.NONE, this)
            );

            boolean hitBlock = blockHit.getType() != HitResult.Type.MISS;

            AABB searchBox = this.getBoundingBox().expandTowards(velocity).inflate(1.0D);
            List<Entity> entitiesInPath = this.level().getEntities(this, searchBox,
                entity -> entity != this.getOwner() && entity.isPickable() && entity.isAlive() && !ProjectileFriendlyFire.spares(this.getOwner(), entity)
            );

            Entity closestHitEntity = null;
            double closestEntityDist = Double.MAX_VALUE;

            for (Entity entity : entitiesInPath) {
                AABB entityBox = entity.getBoundingBox().inflate(0.3D);
                java.util.Optional<Vec3> hitVec = entityBox.clip(start, end);
                if (hitVec.isPresent() && !this.hitEntities.contains(entity)) {
                    double dist = start.distanceTo(hitVec.get());
                    if (dist < closestEntityDist) {
                        closestHitEntity = entity;
                        closestEntityDist = dist;
                    }
                }
            }

            if (hitBlock) {
                double blockDist = start.distanceTo(blockHit.getLocation());
                if (blockDist < closestEntityDist) {
                    this.setPos(blockHit.getLocation());
                    onImpact(blockHit);
                    this.discard();
                    return;
                }
            }

            if (closestHitEntity != null) {
                this.setPos(start.add(velocity.normalize().scale(closestEntityDist)));
                onEntityHit(closestHitEntity);
                this.discard();
                return;
            }
        }

        super.tick();   // the one move of this tick
        this.life++;
        if (this.life > this.maxLife) {
            this.discard();
            return;
        }

        if (this.level().isClientSide) {
            float velocity = (float) this.getDeltaMovement().length();
            this.level().addParticle(net.minecraft.core.particles.ParticleTypes.SMOKE,
                this.getX(), this.getY(), this.getZ(),
                -this.getDeltaMovement().x * velocity * 0.5,
                -this.getDeltaMovement().y * velocity * 0.5,
                -this.getDeltaMovement().z * velocity * 0.5);
        }
    }

    // 0.1.11.3: the shot's settings are saved. A rocket in a chunk that unloaded came back with the defaults: terrain
    // damage on and power 5, so a Wingman rocket (no terrain damage) made a TNT-class blast; its life started again.
    @Override
    public void addAdditionalSaveData(net.minecraft.nbt.CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putFloat("Damage", damage);
        tag.putFloat("ExplosionPower", explosionPower);
        tag.putInt("MaxLife", maxLife);
        tag.putInt("Life", life);
        tag.putBoolean("TerrainDamage", terrainDamage);
    }

    @Override
    public void readAdditionalSaveData(net.minecraft.nbt.CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("Damage")) damage = tag.getFloat("Damage");
        if (tag.contains("ExplosionPower")) explosionPower = tag.getFloat("ExplosionPower");
        if (tag.contains("MaxLife")) maxLife = tag.getInt("MaxLife");
        if (tag.contains("Life")) life = tag.getInt("Life");
        if (tag.contains("TerrainDamage")) terrainDamage = tag.getBoolean("TerrainDamage");
    }

    protected void onEntityHit(Entity entity) {
        LivingEntity owner = this.getOwner() instanceof LivingEntity living ? living : null;
        DamageSource source = owner != null ? this.damageSources().mobProjectile(this, owner) : this.damageSources().explosion(null, this);

        if (entity instanceof LivingEntity living) {
            living.hurt(source, this.damage);
            this.hitEntities.add(entity);
        }

        explode(source, owner);
    }

    protected void onImpact(BlockHitResult result) {
        LivingEntity owner = this.getOwner() instanceof LivingEntity living ? living : null;
        DamageSource source = owner != null ? this.damageSources().mobProjectile(this, owner) : this.damageSources().explosion(null, this);

        explode(source, owner);
    }

    private void explode(DamageSource source, LivingEntity owner) {
        if (this.explosionPower > 0.0F && owner != null) {
            // 0.1.11.197 (T4, D5): the blast alone (the extra ring of 2 × power damage is gone); D6: terrain by config
            this.level().explode(owner, this.getX(), this.getY(), this.getZ(), this.explosionPower,
                terrainDamage && com.miophas.singularity_iteration.common.Singularity_Iteration_Config.weaponsDamageTerrain()
                    ? Level.ExplosionInteraction.TNT : Level.ExplosionInteraction.NONE);
        }

        spawnImpactParticles();
    }

    @Override
    protected void onHit(HitResult result) {
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
    }

    @Override
    protected void onHitBlock(net.minecraft.world.phys.BlockHitResult result) {
    }

    private void spawnImpactParticles() {
        // 爆炸火焰粒子效果已移除
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        double d0 = this.getBoundingBox().getSize() * 10.0;
        if (Double.isNaN(d0)) d0 = 1.0;
        d0 *= 64.0 * getViewScale();
        return distance < d0 * d0;
    }
}