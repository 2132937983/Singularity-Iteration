package com.miophas.singularity_iteration.common.item.tools;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;

/** Electric rifle (T2): 6 a bolt, 100 EU, 2 ticks. 0.1.11.197 (T4): on {@link AbstractEnergyGun}; numbers unchanged. */
public class mio_icif_electric_rifle extends AbstractEnergyGun {
    public static final int RIFLE_MAX_ENERGY = 50000;
    public static final int RIFLE_CHARGE_RATE = 128;
    public static final int RIFLE_TIER = 2;
    public static final int ENERGY_PER_SHOT = 100;
    public static final float BULLET_DAMAGE = 6.0F;
    public static final float BULLET_SPEED = 3.0F;
    public static final int BULLET_MAX_LIFE = 250;
    public static final int COOLDOWN_TICKS = 2;

    public static EntityType<mio_icif_energy_bullet> RIFLE_BULLET_ENTITY;

    public static void setRifleBulletEntity(EntityType<mio_icif_energy_bullet> entityType) {
        RIFLE_BULLET_ENTITY = entityType;
    }

    public mio_icif_electric_rifle(Properties properties) {
        super(properties, RIFLE_MAX_ENERGY, RIFLE_CHARGE_RATE, RIFLE_TIER, ENERGY_PER_SHOT,
            new Spec("electric_rifle", "RIFLE", 0x5AB8FF, BULLET_DAMAGE, BULLET_SPEED, BULLET_MAX_LIFE, COOLDOWN_TICKS,
                () -> com.miophas.singularity_iteration.common.registry.mio_icif_sounds.RIFLE_SHOT.get(), 0.9F, 1.15F));
    }

    @Override protected EntityType<mio_icif_energy_bullet> bulletType() { return RIFLE_BULLET_ENTITY; }

    @Override public boolean isEnchantable(ItemStack stack) { return false; }
}
