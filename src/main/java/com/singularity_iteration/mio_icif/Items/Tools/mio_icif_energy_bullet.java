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

import java.util.HashSet;
import java.util.Set;

@SuppressWarnings("null")
public class mio_icif_energy_bullet extends ThrowableProjectile {
    private float damage = 5.0F;
    private int maxLife = 200;
    private int life = 0;
    private float aoeRadius = 0.0F;
    private float aoeDamage = 0.0F;
    private float explosionPower = 0.0F;
    private int remainingPierces = 0;
    private net.minecraft.world.effect.MobEffectInstance potionEffect = null;
    private final Set<Entity> hitEntities = new HashSet<>();

    public mio_icif_energy_bullet(EntityType<? extends mio_icif_energy_bullet> entityType, Level level) {
        super(entityType, level);
        this.setNoGravity(true);
    }

    public mio_icif_energy_bullet(EntityType<? extends mio_icif_energy_bullet> entityType, LivingEntity owner, Level level) {
        super(entityType, owner, level);
        this.setNoGravity(true);
        this.setPos(owner.getX(), owner.getEyeY() - 0.1, owner.getZ());
    }

    public mio_icif_energy_bullet(EntityType<? extends mio_icif_energy_bullet> entityType, double x, double y, double z, Level level) {
        super(entityType, x, y, z, level);
        this.setNoGravity(true);
    }

    @Override
    protected void defineSynchedData(net.minecraft.network.syncher.SynchedEntityData.Builder builder) {
    }

    public void setDamage(float damage) {
        this.damage = damage;
    }

    public void setMaxLife(int maxLife) {
        this.maxLife = maxLife;
    }

    public void setAoe(float radius, float damage) {
        this.aoeRadius = radius;
        this.aoeDamage = damage;
    }

    public void setExplosionPower(float power) {
        this.explosionPower = power;
    }

    public void setPierceCount(int count) {
        this.remainingPierces = count;
    }

    public void setPotionEffect(net.minecraft.world.effect.MobEffectInstance effect) {
        this.potionEffect = effect;
    }

    public void setGravity(boolean gravity) {
        this.setNoGravity(!gravity);
    }

    @Override
    public void tick() {
        super.tick();
        this.life++;
        if (this.life > this.maxLife) {
            this.discard();
            return;
        }

        // 火焰特效已移除
    }

    @Override
    protected void onHit(HitResult result) {
        super.onHit(result);

        if (result.getType() == HitResult.Type.BLOCK) {
            onBlockHit((BlockHitResult) result);
        } else if (result.getType() == HitResult.Type.ENTITY) {
            onEntityHitResult((EntityHitResult) result);
        }
    }

    protected void onEntityHitResult(EntityHitResult result) {
        Entity entity = result.getEntity();
        if (this.hitEntities.contains(entity)) {
            return;
        }
        
        if (entity instanceof LivingEntity living) {
            LivingEntity owner = this.getOwner() instanceof LivingEntity livingOwner ? livingOwner : null;
            DamageSource source = owner != null ? this.damageSources().mobProjectile(this, owner) : this.damageSources().magic();
            living.hurt(source, this.damage);
            if (this.potionEffect != null) {
                living.addEffect(this.potionEffect);
            }
            this.hitEntities.add(entity);
            this.remainingPierces--;
            
            if (this.remainingPierces <= 0) {
                spawnImpactParticles();
                this.discard();
            }
        }
    }

    protected void onBlockHit(BlockHitResult result) {
        LivingEntity owner = this.getOwner() instanceof LivingEntity living ? living : null;

        if (this.aoeRadius > 0.0F && this.aoeDamage > 0.0F) {
            DamageSource source = owner != null ? this.damageSources().mobProjectile(this, owner) : this.damageSources().magic();
            AABB area = AABB.ofSize(this.position(), this.aoeRadius * 2, this.aoeRadius * 2, this.aoeRadius * 2);
            for (LivingEntity e : this.level().getEntitiesOfClass(LivingEntity.class, area,
                e -> e != this.getOwner() && e.distanceTo(this) <= this.aoeRadius)) {
                e.hurt(source, this.aoeDamage);
            }
        }

        if (this.explosionPower > 0.0F && owner != null) {
            this.level().explode(owner, this.getX(), this.getY(), this.getZ(),
                this.explosionPower, Level.ExplosionInteraction.TNT);
        }

        spawnImpactParticles();
        this.discard();
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
    }

    @Override
    protected void onHitBlock(net.minecraft.world.phys.BlockHitResult result) {
    }

    private void spawnImpactParticles() {
        // 击中粒子效果已移除
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        double d0 = this.getBoundingBox().getSize() * 10.0;
        if (Double.isNaN(d0)) d0 = 1.0;
        d0 *= 64.0 * getViewScale();
        return distance < d0 * d0;
    }
}