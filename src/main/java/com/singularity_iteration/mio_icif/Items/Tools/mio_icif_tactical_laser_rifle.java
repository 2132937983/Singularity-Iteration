package com.singularity_iteration.mio_icif.Items.Tools;

import com.singularity_iteration.mio_icif.api.item.IWeaponItem;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;

import java.util.List;

@SuppressWarnings("null")
public class mio_icif_tactical_laser_rifle extends mio_icif_tool_elc implements IWeaponItem {
    public static final int RIFLE_MAX_ENERGY = 100000000;
    public static final int RIFLE_CHARGE_RATE = 8192;
    public static final int RIFLE_TIER = 5;
    public static final int ENERGY_PER_SHOT = 10000;
    public static final float BULLET_DAMAGE = 50.0F;
    public static final float MELEE_DAMAGE = 20.0F;
    public static final float BULLET_SPEED = 3.0F;
    public static final int BULLET_MAX_LIFE = 360;
    public static final int COOLDOWN_TICKS = 3;

    public static EntityType<mio_icif_energy_bullet> RIFLE_BULLET_ENTITY;

    public static void setRifleBulletEntity(EntityType<mio_icif_energy_bullet> entityType) {
        RIFLE_BULLET_ENTITY = entityType;
    }

    public mio_icif_tactical_laser_rifle(Properties properties) {
        super(properties, RIFLE_MAX_ENERGY, RIFLE_MAX_ENERGY, "tactical_laser_rifle", RIFLE_CHARGE_RATE, ENERGY_PER_SHOT, RIFLE_TIER);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (!hasEnoughEnergy(stack, ENERGY_PER_SHOT)) {
            if (!level.isClientSide) {
                player.sendSystemMessage(Component.translatable("message.mio_icif.tactical_laser_rifle.no_energy"));
            }
            return InteractionResultHolder.fail(stack);
        }

        if (!level.isClientSide) {
            if (RIFLE_BULLET_ENTITY == null) {
                return InteractionResultHolder.fail(stack);
            }

            consumeEnergy(stack, ENERGY_PER_SHOT);

            mio_icif_energy_bullet bullet = new mio_icif_energy_bullet(RIFLE_BULLET_ENTITY, player, level);
            bullet.setDamage(BULLET_DAMAGE);
            bullet.setMaxLife(BULLET_MAX_LIFE);
            bullet.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0F, BULLET_SPEED, 0.0F);
            level.addFreshEntity(bullet);

            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                net.minecraft.sounds.SoundEvents.FIREWORK_ROCKET_BLAST, net.minecraft.sounds.SoundSource.PLAYERS, 1.0F, 1.5F);
        }

        player.getCooldowns().addCooldown(this, COOLDOWN_TICKS);
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public float getAttackDamageBonus(Entity target, float damage, DamageSource damageSource) {
        Entity directEntity = damageSource.getDirectEntity();
        if (directEntity instanceof LivingEntity living) {
            ItemStack mainHand = living.getMainHandItem();
            if (!mainHand.isEmpty() && mainHand.getItem() == this) {
                int sharpness = 0;
                net.minecraft.world.item.enchantment.ItemEnchantments itemenchantments = mainHand.getOrDefault(net.minecraft.core.component.DataComponents.ENCHANTMENTS, net.minecraft.world.item.enchantment.ItemEnchantments.EMPTY);
                for (var entry : itemenchantments.entrySet()) {
                    if (entry.getKey().is(Enchantments.SHARPNESS)) {
                        sharpness = entry.getIntValue();
                        break;
                    }
                }
                return MELEE_DAMAGE * (sharpness > 0 ? sharpness + 1 : 1.0F);
            }
        }
        return MELEE_DAMAGE;
    }

    @Override
    public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        if (!attacker.level().isClientSide) {
            int meleeCost = ENERGY_PER_SHOT / 5;
            if (consumeEnergy(stack, meleeCost)) {
                target.knockback(0.5F, attacker.getLookAngle().x, attacker.getLookAngle().z);
            }
        }
        return true;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        tooltip.add(Component.translatable("tooltip.mio_icif.tactical_laser_rifle.damage", BULLET_DAMAGE));
        tooltip.add(Component.translatable("tooltip.mio_icif.tactical_laser_rifle.melee", MELEE_DAMAGE));
    }

    @Override
    public boolean isEnchantable(ItemStack stack) {
        return true;
    }

    @Override
    public int getEnchantmentValue() {
        return 15;
    }

    @Override
    public float getDamage() {
        return BULLET_DAMAGE;
    }

    @Override
    public float getRange() {
        return BULLET_SPEED * 20;
    }

    @Override
    public long getEnergyPerShot() {
        return ENERGY_PER_SHOT;
    }

    @Override
    public boolean isRanged() {
        return true;
    }
}
