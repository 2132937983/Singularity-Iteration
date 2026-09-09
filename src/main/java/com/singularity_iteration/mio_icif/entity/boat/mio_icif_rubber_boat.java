package com.singularity_iteration.mio_icif.entity.boat;

import com.singularity_iteration.mio_icif.Items.Normal.mio_icif_normal;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;

@SuppressWarnings("null")
public class mio_icif_rubber_boat extends mio_icif_boat {

    private static final double BREAK_RUBBER_THRESHOLD = 0.26;

    public mio_icif_rubber_boat(EntityType<? extends mio_icif_rubber_boat> entityType, Level level) {
        super(entityType, level);
    }

    @Override
    public Item getBoatDropItem() {
        return mio_icif_normal.ENTITY_RUBBER_BOAT.get();
    }

    @Override
    public double getTopSpeed() {
        return 0.35;
    }

    @Override
    public double getBreakMotion() {
        return 0.23;
    }

    @Override
    public double getAccelerationFactor() {
        return 1.0;
    }

    @Override
    public void destroy(DamageSource source) {
        this.kill();
        if (this.level().getGameRules().getBoolean(net.minecraft.world.level.GameRules.RULE_DOENTITYDROPS)) {
            double speed = getDeltaMovement().horizontalDistance();
            level().playSound(null, blockPosition(), SoundEvents.ITEM_BREAK, SoundSource.BLOCKS, 1.0F, 2.0F);
            if (speed > BREAK_RUBBER_THRESHOLD) {
                this.spawnAtLocation(mio_icif_normal.BROKEN_RUBBER_BOAT.get());
            } else {
                this.spawnAtLocation(mio_icif_normal.ENTITY_RUBBER_BOAT.get());
            }
        }
        this.gameEvent(GameEvent.ENTITY_DIE);
    }
}

