package com.singularity_iteration.mio_icif.Items.Tools;

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
    private float damage = 0.0F;
    private float explosionPower = 5.0F;
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

    public void setExplosionPower(float power) {
        this.explosionPower = power;
    }

    @Override
    public void tick() {
        super.tick();
        this.life++;
        if (this.life > this.maxLife) {
            this.discard();
            return;
        }

        if (!this.level().isClientSide) {
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
                entity -> entity != this.getOwner() && entity.isPickable() && entity.isAlive()
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
            } else {
                this.setPos(end);
            }
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
            AABB area = AABB.ofSize(this.position(), this.explosionPower * 2, this.explosionPower * 2, this.explosionPower * 2);
            for (LivingEntity e : this.level().getEntitiesOfClass(LivingEntity.class, area,
                e -> e != this.getOwner() && e.distanceTo(this) <= this.explosionPower)) {
                e.hurt(source, this.explosionPower * 2);
            }
            this.level().explode(owner, this.getX(), this.getY(), this.getZ(),
                this.explosionPower, Level.ExplosionInteraction.TNT);
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