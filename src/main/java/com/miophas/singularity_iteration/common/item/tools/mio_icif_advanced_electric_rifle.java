package com.miophas.singularity_iteration.common.item.tools;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;

/**
 * Advanced electric rifle (T4). 0.1.11.197 (T4, D5): 30 a bolt for 3,000 EU (it was 25 for 5,000): 60 damage a second on
 * one target, 100 EU a point.
 */
public class mio_icif_advanced_electric_rifle extends AbstractEnergyGun {
    public static final int RIFLE_MAX_ENERGY = 5000000;
    public static final int RIFLE_CHARGE_RATE = 2048;
    public static final int RIFLE_TIER = 4;
    public static final int ENERGY_PER_SHOT = 3000;
    public static final float BULLET_DAMAGE = 30.0F;
    public static final float BULLET_SPEED = 3.0F;
    public static final int BULLET_MAX_LIFE = 360;
    public static final int COOLDOWN_TICKS = 3;

    public static EntityType<mio_icif_energy_bullet> RIFLE_BULLET_ENTITY;

    public static void setRifleBulletEntity(EntityType<mio_icif_energy_bullet> entityType) {
        RIFLE_BULLET_ENTITY = entityType;
    }

    public mio_icif_advanced_electric_rifle(Properties properties) {
        super(properties, RIFLE_MAX_ENERGY, RIFLE_CHARGE_RATE, RIFLE_TIER, ENERGY_PER_SHOT,
            new Spec("advanced_electric_rifle", "A-RIFLE", 0x7C8CFF, BULLET_DAMAGE, BULLET_SPEED, BULLET_MAX_LIFE, COOLDOWN_TICKS,
                () -> com.miophas.singularity_iteration.common.registry.mio_icif_sounds.RIFLE_SHOT.get(), 0.9F, 0.95F));
    }

    @Override protected EntityType<mio_icif_energy_bullet> bulletType() { return RIFLE_BULLET_ENTITY; }

    @Override public boolean isEnchantable(ItemStack stack) { return false; }
}
