package com.miophas.singularity_iteration.common.item.tools;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;

/**
 * Electric plasma gun (T3): a slow green bolt that bursts on a block. 0.1.11.197 (T4, D5): 15 direct and 15 in a 3-block
 * radius for 3,000 EU (it was 20 and 20 in 4 blocks for 5,000); the small blast follows the weapon terrain setting.
 */
public class mio_icif_electric_plasma_gun extends AbstractEnergyGun {
    public static final int MAX_ENERGY = 1000000;
    public static final int CHARGE_RATE = 512;
    public static final int TIER = 3;
    public static final int ENERGY_PER_SHOT = 3000;
    public static final float BULLET_DAMAGE = 15.0F;
    public static final float AOE_RADIUS = 3.0F;
    public static final float AOE_DAMAGE = 15.0F;
    public static final float EXPLOSION_POWER = 2.0F;
    public static final float BULLET_SPEED = 1.5F;
    public static final int BULLET_MAX_LIFE = 500;
    public static final int COOLDOWN_TICKS = 10;

    public static EntityType<mio_icif_energy_bullet> RIFLE_BULLET_ENTITY;

    public static void setRifleBulletEntity(EntityType<mio_icif_energy_bullet> entityType) {
        RIFLE_BULLET_ENTITY = entityType;
    }

    public mio_icif_electric_plasma_gun(Properties properties) {
        super(properties, MAX_ENERGY, CHARGE_RATE, TIER, ENERGY_PER_SHOT,
            new Spec("electric_plasma_gun", "PLASMA", 0x7CFFB2, BULLET_DAMAGE, BULLET_SPEED, BULLET_MAX_LIFE, COOLDOWN_TICKS,
                () -> com.miophas.singularity_iteration.common.registry.mio_icif_sounds.PLASMA_SHOT.get(), 1.0F, 1.0F));
    }

    @Override protected EntityType<mio_icif_energy_bullet> bulletType() { return RIFLE_BULLET_ENTITY; }

    @Override
    protected void configure(mio_icif_energy_bullet bullet, ItemStack stack) {
        bullet.setAoe(AOE_RADIUS, AOE_DAMAGE);
        bullet.setExplosionPower(EXPLOSION_POWER);
    }

    @Override public boolean isEnchantable(ItemStack stack) { return false; }
}
