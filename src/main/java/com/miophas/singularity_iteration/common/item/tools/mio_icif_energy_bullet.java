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
    // Tachyon disfission upgrades: extra hits per target, blocks it may pass through, share of true damage
    private int extraHits;
    private int blockPierce;
    private float trueFraction;
    private final Set<net.minecraft.core.BlockPos> piercedBlocks = new HashSet<>();

    public static final net.minecraft.resources.ResourceKey<net.minecraft.world.damagesource.DamageType> TACHYON_TRUE =
        net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.DAMAGE_TYPE,
            net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("mio_icif", "tachyon_true"));

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

    public void setTachyon(int extraHits, int blockPierce, float trueFraction) {
        this.extraHits = Math.max(0, extraHits);
        this.blockPierce = Math.max(0, blockPierce);
        this.trueFraction = Math.max(0, Math.min(1, trueFraction));
    }

    /** One impact: normal part through armor, true part bypassing armor/resistance/enchantments. */
    private void strike(LivingEntity living, DamageSource source, float amount) {
        float trueDamage = amount * trueFraction, normal = amount - trueDamage;
        if (normal > 0) {
            living.invulnerableTime = 0;
            living.hurt(source, normal);
        }
        if (trueDamage > 0 && living.isAlive()) {
            living.invulnerableTime = 0;
            living.hurt(this.damageSources().source(TACHYON_TRUE, this, this.getOwner()), trueDamage);
        }
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
            if (extraHits > 0 || trueFraction > 0) {
                strike(living, source, this.damage);
                // multi-hit: each follow-up pulse lands at 60% and ignores the hurt cooldown
                for (int k = 0; k < extraHits && living.isAlive(); k++) strike(living, source, this.damage * 0.6F);
                if (this.level() instanceof net.minecraft.server.level.ServerLevel server) {
                    server.sendParticles(net.minecraft.core.particles.ParticleTypes.ELECTRIC_SPARK, living.getX(), living.getY(0.5), living.getZ(),
                        6 + 4 * extraHits, 0.3, 0.4, 0.3, 0.4);
                    server.playSound(null, living.getX(), living.getY(), living.getZ(),
                        com.miophas.singularity_iteration.common.registry.mio_icif_sounds.TACHYON_HIT.get(),
                        net.minecraft.sounds.SoundSource.PLAYERS, 0.9F, 0.9F + 0.05F * extraHits);
                }
            } else {
                living.hurt(source, this.damage);
                this.level().playSound(null, living.getX(), living.getY(), living.getZ(),
                    com.miophas.singularity_iteration.common.registry.mio_icif_sounds.ENERGY_HIT.get(),
                    net.minecraft.sounds.SoundSource.PLAYERS, 0.7F, 1.0F + this.random.nextFloat() * 0.2F);
            }
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
        // tachyon penetration: phase through solid blocks without breaking them
        if (piercedBlocks.contains(result.getBlockPos())) return;
        if (blockPierce > 0) {
            piercedBlocks.add(result.getBlockPos().immutable());
            blockPierce--;
            return;
        }
        if (!this.level().isClientSide) {
            this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                com.miophas.singularity_iteration.common.registry.mio_icif_sounds.ENERGY_HIT.get(),
                net.minecraft.sounds.SoundSource.PLAYERS, 0.5F, 0.8F + this.random.nextFloat() * 0.2F);
        }

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