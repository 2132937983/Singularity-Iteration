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
    /** 0.1.11.193 (T0, W-13): passes through the shooter's allies and a Wingman's owner. */
    @Override
    protected boolean canHitEntity(net.minecraft.world.entity.Entity target) {
        return super.canHitEntity(target) && !ProjectileFriendlyFire.spares(getOwner(), target);
    }

    private float damage = 5.0F;
    private int maxLife = 200;
    private int life = 0;
    private float aoeRadius = 0.0F;
    private float aoeDamage = 0.0F;
    private float explosionPower = 0.0F;
    /** 0.1.8.4: false for AI shooters (Wingman): the explosion hurts entities but breaks no blocks. Not saved. */
    private boolean terrainDamage = true;
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

    /** 0.1.8.3: beam colour (0xRRGGBB) and power (0..1, from the shot energy) for the client renderer. */
    private static final net.minecraft.network.syncher.EntityDataAccessor<Integer> DATA_COLOR =
        net.minecraft.network.syncher.SynchedEntityData.defineId(mio_icif_energy_bullet.class, net.minecraft.network.syncher.EntityDataSerializers.INT);
    private static final net.minecraft.network.syncher.EntityDataAccessor<Float> DATA_POWER =
        net.minecraft.network.syncher.SynchedEntityData.defineId(mio_icif_energy_bullet.class, net.minecraft.network.syncher.EntityDataSerializers.FLOAT);

    @Override
    protected void defineSynchedData(net.minecraft.network.syncher.SynchedEntityData.Builder builder) {
        builder.define(DATA_COLOR, 0x5AB8FF);
        builder.define(DATA_POWER, 0.6F);
    }

    /** Sets the beam look: colour 0xRRGGBB and the shot energy in EU (0.1.8.3). Call before spawning. */
    public void setBeam(int color, long energyPerShot) {
        this.entityData.set(DATA_COLOR, color & 0xFFFFFF);
        this.entityData.set(DATA_POWER, WeaponChargeFx.power(energyPerShot));
    }

    public int beamColor() {
        return this.entityData.get(DATA_COLOR);
    }

    public float beamPower() {
        return this.entityData.get(DATA_POWER);
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

    public void setTerrainDamage(boolean terrain) {
        this.terrainDamage = terrain;
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

    // 0.1.11.3: the shot's settings are saved (a bullet in a chunk that unloaded came back with damage 5 and lost its
    // explosion, area damage, pierce, effect and tachyon settings).
    @Override
    public void addAdditionalSaveData(net.minecraft.nbt.CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putFloat("Damage", damage);
        tag.putInt("MaxLife", maxLife);
        tag.putInt("Life", life);
        tag.putFloat("AoeRadius", aoeRadius);
        tag.putFloat("AoeDamage", aoeDamage);
        tag.putFloat("ExplosionPower", explosionPower);
        tag.putBoolean("TerrainDamage", terrainDamage);
        tag.putInt("Pierces", remainingPierces);
        tag.putInt("ExtraHits", extraHits);
        tag.putInt("BlockPierce", blockPierce);
        tag.putFloat("TrueFraction", trueFraction);
        tag.putInt("BeamColor", beamColor());
        tag.putFloat("BeamPower", beamPower());
        if (potionEffect != null) tag.put("Effect", potionEffect.save());
    }

    @Override
    public void readAdditionalSaveData(net.minecraft.nbt.CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (!tag.contains("Damage")) return;   // saved before 0.1.11.3
        damage = tag.getFloat("Damage");
        maxLife = tag.getInt("MaxLife");
        life = tag.getInt("Life");
        aoeRadius = tag.getFloat("AoeRadius");
        aoeDamage = tag.getFloat("AoeDamage");
        explosionPower = tag.getFloat("ExplosionPower");
        terrainDamage = tag.getBoolean("TerrainDamage");
        remainingPierces = tag.getInt("Pierces");
        extraHits = tag.getInt("ExtraHits");
        blockPierce = tag.getInt("BlockPierce");
        trueFraction = tag.getFloat("TrueFraction");
        this.entityData.set(DATA_COLOR, tag.getInt("BeamColor") & 0xFFFFFF);
        this.entityData.set(DATA_POWER, tag.getFloat("BeamPower"));
        potionEffect = tag.contains("Effect") ? net.minecraft.world.effect.MobEffectInstance.load(tag.getCompound("Effect")) : null;
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
        // 0.1.11.197 (T4, W-10): hits are resolved on the server only; the client ran them too (a second blast, double knockback)
        if (this.level().isClientSide) return;

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
        
        if (!(entity instanceof LivingEntity)) {
            // 0.1.11.197 (T4, W-11): an ender dragon part, an end crystal, a boat or a cart takes the hit too
            LivingEntity owner = this.getOwner() instanceof LivingEntity livingOwner ? livingOwner : null;
            entity.hurt(owner != null ? this.damageSources().mobProjectile(this, owner) : this.damageSources().magic(), this.damage);
            this.hitEntities.add(entity);
            if (--this.remainingPierces <= 0) {
                spawnImpactParticles();
                this.discard();
            }
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
                // 0.1.11.197 (T4, W-12): a copy per target; one shared instance counted down once per target each tick
                living.addEffect(new net.minecraft.world.effect.MobEffectInstance(this.potionEffect));
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
                e -> e != this.getOwner() && e.distanceTo(this) <= this.aoeRadius && !ProjectileFriendlyFire.spares(this.getOwner(), e))) {
                e.hurt(source, this.aoeDamage);
            }
        }

        if (this.explosionPower > 0.0F && owner != null) {
            this.level().explode(owner, this.getX(), this.getY(), this.getZ(),
                this.explosionPower, terrainDamage && com.miophas.singularity_iteration.common.Singularity_Iteration_Config.weaponsDamageTerrain()
                    ? Level.ExplosionInteraction.TNT : Level.ExplosionInteraction.NONE);   // 0.1.11.197 (D6)
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

    /**
     * Impact burst (0.1.8.3; the 0.1.7 effect had been removed): electric sparks and dust in the
     * beam colour. Sent by the server, so every player near the impact sees it.
     */
    private void spawnImpactParticles() {
        if (!(this.level() instanceof net.minecraft.server.level.ServerLevel server)) return;
        int c = beamColor();
        var dust = new net.minecraft.core.particles.DustParticleOptions(
            new org.joml.Vector3f((c >> 16 & 255) / 255F, (c >> 8 & 255) / 255F, (c & 255) / 255F), 0.9F + 0.8F * beamPower());
        server.sendParticles(net.minecraft.core.particles.ParticleTypes.ELECTRIC_SPARK, getX(), getY(), getZ(), 8, 0.12, 0.12, 0.12, 0.35);
        server.sendParticles(dust, getX(), getY(), getZ(), 6, 0.15, 0.15, 0.15, 0.0);
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        double d0 = this.getBoundingBox().getSize() * 10.0;
        if (Double.isNaN(d0)) d0 = 1.0;
        d0 *= 64.0 * getViewScale();
        return distance < d0 * d0;
    }
}