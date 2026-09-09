package com.singularity_iteration.mio_icif.entity.boat;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

@SuppressWarnings("null")
public abstract class mio_icif_boat extends Boat {

    public mio_icif_boat(EntityType<? extends Boat> entityType, Level level) {
        super(entityType, level);
    }

    public abstract Item getBoatDropItem();

    public abstract double getTopSpeed();

    public abstract double getBreakMotion();

    public abstract double getAccelerationFactor();

    @Override
    public Item getDropItem() {
        return getBoatDropItem();
    }

    @Override
    public ItemStack getPickResult() {
        return new ItemStack(getBoatDropItem());
    }
}

