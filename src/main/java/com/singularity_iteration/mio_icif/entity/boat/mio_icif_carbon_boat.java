package com.singularity_iteration.mio_icif.entity.boat;

import com.singularity_iteration.mio_icif.Items.Normal.mio_icif_normal;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;

@SuppressWarnings("null")
public class mio_icif_carbon_boat extends mio_icif_boat {

    private static final double BREAK_MOTION = 0.4;

    public mio_icif_carbon_boat(EntityType<? extends mio_icif_carbon_boat> entityType, Level level) {
        super(entityType, level);
    }

    @Override
    public Item getBoatDropItem() {
        return mio_icif_normal.ENTITY_COAL_BOAT.get();
    }

    @Override
    public double getTopSpeed() {
        return 0.35;
    }

    @Override
    public double getBreakMotion() {
        return BREAK_MOTION;
    }

    @Override
    public double getAccelerationFactor() {
        return 1.0;
    }
}

