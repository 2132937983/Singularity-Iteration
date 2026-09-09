package com.singularity_iteration.mio_icif.entity.dynamite;

import com.singularity_iteration.mio_icif.entity.mio_icif_entities;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.TraceableEntity;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

@SuppressWarnings("null")
public class mio_icif_dynamite_entity extends Entity implements TraceableEntity {

    private static final EntityDataAccessor<Integer> DATA_FUSE = SynchedEntityData.defineId(mio_icif_dynamite_entity.class, EntityDataSerializers.INT);
    private static final int DEFAULT_FUSE = 50;
    private static final float EXPLOSION_POWER = 3.0F;

    @Nullable
    private LivingEntity owner;

    public mio_icif_dynamite_entity(EntityType<? extends mio_icif_dynamite_entity> entityType, Level level) {
        super(entityType, level);
        this.blocksBuilding = true;
    }

    public mio_icif_dynamite_entity(Level level, double x, double y, double z, @Nullable LivingEntity owner) {
        this(mio_icif_entities.DYNAMITE_ENTITY.get(), level);
        this.setPos(x, y, z);
        this.setFuse(DEFAULT_FUSE);
        this.owner = owner;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_FUSE, DEFAULT_FUSE);
    }

    @Override
    protected Entity.MovementEmission getMovementEmission() {
        return Entity.MovementEmission.NONE;
    }

    @Override
    public boolean isPickable() {
        return !this.isRemoved();
    }

    @Override
    protected double getDefaultGravity() {
        return 0.04;
    }

    @Override
    public void tick() {
        this.applyGravity();
        this.move(MoverType.SELF, this.getDeltaMovement());
        this.setDeltaMovement(this.getDeltaMovement().scale(0.98));
        if (this.onGround()) {
            this.setDeltaMovement(this.getDeltaMovement().multiply(0.7, -0.5, 0.7));
        }

        int fuse = this.getFuse() - 1;
        this.setFuse(fuse);
        if (fuse <= 0) {
            this.discard();
            if (!this.level().isClientSide) {
                this.explode();
            }
        } else {
            this.updateInWaterStateAndDoFluidPushing();
            if (this.level().isClientSide) {
                this.level().addParticle(ParticleTypes.SMOKE, this.getX(), this.getY() + 0.5, this.getZ(), 0.0, 0.0, 0.0);
            }
        }
    }

    protected void explode() {
        this.level().explode(
            this,
            Explosion.getDefaultDamageSource(this.level(), this),
            null,
            this.getX(),
            this.getY(0.0625),
            this.getZ(),
            EXPLOSION_POWER,
            false,
            Level.ExplosionInteraction.TNT
        );
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag compound) {
        compound.putShort("fuse", (short) this.getFuse());
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag compound) {
        this.setFuse(compound.getShort("fuse"));
    }

    @Nullable
    @Override
    public LivingEntity getOwner() {
        return this.owner;
    }

    public void setFuse(int fuse) {
        this.entityData.set(DATA_FUSE, fuse);
    }

    public int getFuse() {
        return this.entityData.get(DATA_FUSE);
    }
}

