package com.singularity_iteration.mio_icif.Blocks.reactor;

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
public class mio_icif_Entity_IC_TNT_Primed extends Entity implements TraceableEntity {
    private static final EntityDataAccessor<Integer> DATA_FUSE_ID = SynchedEntityData.defineId(mio_icif_Entity_IC_TNT_Primed.class, EntityDataSerializers.INT);
    @SuppressWarnings("unused")
    private static final int DEFAULT_FUSE_TIME = 80;
    public static final String TAG_FUSE = "fuse";
    @Nullable
    private LivingEntity owner;

    public mio_icif_Entity_IC_TNT_Primed(EntityType<? extends mio_icif_Entity_IC_TNT_Primed> entityType, Level level) {
        super(entityType, level);
        this.blocksBuilding = true;
    }

    public mio_icif_Entity_IC_TNT_Primed(Level level, double x, double y, double z, @Nullable LivingEntity owner) {
        this(mio_icif_entities.IC_TNT_PRIMED.get(), level);
        this.setPos(x, y, z);
        double d0 = level.random.nextDouble() * (float) (Math.PI * 2);
        this.setDeltaMovement(-Math.sin(d0) * 0.02, 0.2F, -Math.cos(d0) * 0.02);
        this.setFuse(80);
        this.xo = x;
        this.yo = y;
        this.zo = z;
        this.owner = owner;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_FUSE_ID, 80);
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

        int i = this.getFuse() - 1;
        this.setFuse(i);
        if (i <= 0) {
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
        float explosionPower = 8.0F;
        this.level().explode(
            this,
            Explosion.getDefaultDamageSource(this.level(), this),
            null,
            this.getX(),
            this.getY(0.0625),
            this.getZ(),
            explosionPower,
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
    public LivingEntity getOwner() {
        return this.owner;
    }

    public void setFuse(int fuse) {
        this.entityData.set(DATA_FUSE_ID, fuse);
    }

    public int getFuse() {
        return this.entityData.get(DATA_FUSE_ID);
    }

    public net.minecraft.world.level.block.state.BlockState getBlockState() {
        return com.singularity_iteration.mio_icif.Blocks.mio_icif_blocks.IC_TNT.get().defaultBlockState();
    }
}

