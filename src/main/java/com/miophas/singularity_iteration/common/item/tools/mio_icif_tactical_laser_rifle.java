package com.miophas.singularity_iteration.common.item.tools;

import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.ItemAttributeModifiers;

/**
 * Tactical laser rifle (T5). 0.1.11.197 (T4, D5): 60 a bolt for 12,000 EU (it was 50 for 10,000): 120 damage a second on
 * one target, 200 EU a point. Melee: +20 while a hit can be paid (1,000 EU a hit), a sword's attack speed; the +20 was
 * a constant that needed no energy, at attack speed 4.
 */
public class mio_icif_tactical_laser_rifle extends AbstractEnergyGun {
    public static final int RIFLE_MAX_ENERGY = 100000000;
    public static final int RIFLE_CHARGE_RATE = 8192;
    public static final int RIFLE_TIER = 5;
    public static final int ENERGY_PER_SHOT = 12000;
    public static final float BULLET_DAMAGE = 60.0F;
    public static final float MELEE_DAMAGE = 20.0F;
    public static final int MELEE_COST = 1000;
    public static final float BULLET_SPEED = 3.0F;
    public static final int BULLET_MAX_LIFE = 360;
    public static final int COOLDOWN_TICKS = 3;

    private static final ItemAttributeModifiers MELEE = meleeAttributes(MELEE_DAMAGE);
    private static final ItemAttributeModifiers MELEE_FLAT = meleeAttributes(0);

    public static EntityType<mio_icif_energy_bullet> RIFLE_BULLET_ENTITY;

    public static void setRifleBulletEntity(EntityType<mio_icif_energy_bullet> entityType) {
        RIFLE_BULLET_ENTITY = entityType;
    }

    public mio_icif_tactical_laser_rifle(Properties properties) {
        super(properties, RIFLE_MAX_ENERGY, RIFLE_CHARGE_RATE, RIFLE_TIER, ENERGY_PER_SHOT,
            new Spec("tactical_laser_rifle", "T-LASER", 0xFF4D4D, BULLET_DAMAGE, BULLET_SPEED, BULLET_MAX_LIFE, COOLDOWN_TICKS,
                () -> com.miophas.singularity_iteration.common.registry.mio_icif_sounds.RIFLE_SHOT.get(), 0.9F, 0.85F));
    }

    @Override protected EntityType<mio_icif_energy_bullet> bulletType() { return RIFLE_BULLET_ENTITY; }

    @Override
    public ItemAttributeModifiers getDefaultAttributeModifiers(ItemStack stack) {
        return hasEnoughEnergy(stack, MELEE_COST) ? MELEE : MELEE_FLAT;
    }

    @Override
    public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        if (!attacker.level().isClientSide && consumeEnergy(stack, MELEE_COST)) {
            target.knockback(0.5F, attacker.getLookAngle().x, attacker.getLookAngle().z);
        }
        return true;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        tooltip.add(Component.translatable("tooltip.mio_icif.tactical_laser_rifle.melee", String.format("%.0f", MELEE_DAMAGE)));
    }

    @Override public boolean isEnchantable(ItemStack stack) { return true; }
    @Override public int getEnchantmentValue() { return 15; }
}
