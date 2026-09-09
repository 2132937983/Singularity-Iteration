package com.singularity_iteration.mio_icif.entity.boat;

import com.singularity_iteration.mio_icif.Items.Normal.mio_icif_normal;
import com.singularity_iteration.mio_icif.api.MioIcifAPI;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

@SuppressWarnings("null")
public class mio_icif_electric_boat extends mio_icif_boat {

    private static final double EU_CONSUME = 1.0;
    private static final double TOP_SPEED = 0.7;
    private static final double BREAK_MOTION = 0.5;
    private static final float BOOST_ACCEL = 0.02F;

    private static final EntityDataAccessor<Boolean> DATA_ACCELERATED =
        SynchedEntityData.defineId(mio_icif_electric_boat.class, EntityDataSerializers.BOOLEAN);

    public mio_icif_electric_boat(EntityType<? extends mio_icif_boat> entityType, Level level) {
        super(entityType, level);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_ACCELERATED, false);
    }

    @Override
    public Item getBoatDropItem() {
        return mio_icif_normal.ENTITY_ELECTRIC_BOAT.get();
    }

    @Override
    public double getTopSpeed() {
        return TOP_SPEED;
    }

    @Override
    public double getBreakMotion() {
        return BREAK_MOTION;
    }

    @Override
    public double getAccelerationFactor() {
        return entityData.get(DATA_ACCELERATED) ? 1.5 : 0.25;
    }

    @Override
    public void tick() {
        if (!level().isClientSide) {
            boolean accelerated = false;
            if (getControllingPassenger() instanceof Player player) {
                if (!player.getAbilities().flying && player.zza > 0.0F) {
                    for (int i = 0; i < 4; i++) {
                        ItemStack stack = player.getInventory().getArmor(i);
                        if (!stack.isEmpty() && tryDischarge(stack)) {
                            accelerated = true;
                            break;
                        }
                    }
                    if (!accelerated) {
                        for (int i = 0; i < 9; i++) {
                            ItemStack stack = player.getInventory().getItem(i);
                            if (!stack.isEmpty() && tryDischarge(stack)) {
                                accelerated = true;
                                break;
                            }
                        }
                    }
                }
            }
            entityData.set(DATA_ACCELERATED, accelerated);
        }

        super.tick();

        if (level().isClientSide && isControlledByLocalInstance() && entityData.get(DATA_ACCELERATED)) {
            Vec3 motion = getDeltaMovement();
            float yaw = getYRot();
            double dx = (double) (Mth.sin(-yaw * ((float) Math.PI / 180.0F)) * BOOST_ACCEL);
            double dz = (double) (Mth.cos(yaw * ((float) Math.PI / 180.0F)) * BOOST_ACCEL);
            setDeltaMovement(motion.x + dx, motion.y, motion.z + dz);

            Vec3 newMotion = getDeltaMovement();
            double horizontalSpeed = Math.sqrt(newMotion.x * newMotion.x + newMotion.z * newMotion.z);
            if (horizontalSpeed > TOP_SPEED) {
                double scale = TOP_SPEED / horizontalSpeed;
                setDeltaMovement(newMotion.x * scale, newMotion.y, newMotion.z * scale);
            }
        }
    }

    private boolean tryDischarge(ItemStack stack) {
        var api = MioIcifAPI.instance().getItemAPI();
        if (api.isElectricArmor(stack)) {
            long energy = api.getElectricArmorStored(stack);
            if (energy >= EU_CONSUME) {
                api.dischargeElectricArmor(stack, (int) EU_CONSUME, false);
                return true;
            }
        } else if (api.isBattery(stack)) {
            long energy = api.getBatteryStored(stack);
            if (energy >= EU_CONSUME) {
                api.dischargeBattery(stack, (int) EU_CONSUME, false);
                return true;
            }
        }
        return false;
    }
}