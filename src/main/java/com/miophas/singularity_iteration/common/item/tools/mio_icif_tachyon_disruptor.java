package com.miophas.singularity_iteration.common.item.tools;

import com.miophas.singularity_iteration.core.api.item.IWeaponItem;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

@SuppressWarnings("null")
public class mio_icif_tachyon_disruptor extends mio_icif_tool_elc implements IWeaponItem, com.miophas.singularity_iteration.common.item.tuning.ITunableItem {
    public static final int MAX_ENERGY = 400000000;
    public static final int CHARGE_RATE = 8192;
    public static final int TIER = 5;
    public static final int ENERGY_PER_SHOT = 50000;
    public static final float BULLET_DAMAGE = 120.0F;
    public static final float BULLET_SPEED = 4.0F;
    public static final int BULLET_MAX_LIFE = 600;
    public static final int PIERCE_COUNT = 3;
    public static final int COOLDOWN_TICKS = 5;

    public static EntityType<mio_icif_energy_bullet> RIFLE_BULLET_ENTITY;

    public static void setRifleBulletEntity(EntityType<mio_icif_energy_bullet> entityType) {
        RIFLE_BULLET_ENTITY = entityType;
    }

    public mio_icif_tachyon_disruptor(Properties properties) {
        super(properties, MAX_ENERGY, MAX_ENERGY, "tachyon_disruptor", CHARGE_RATE, ENERGY_PER_SHOT, TIER);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        long cost = shotCost(stack);

        if (!hasEnoughEnergy(stack, cost)) {
            if (!level.isClientSide) {
                player.sendSystemMessage(Component.translatable("message.mio_icif.tachyon_disruptor.no_energy"));
            }
            return InteractionResultHolder.fail(stack);
        }

        if (!level.isClientSide) {
            if (RIFLE_BULLET_ENTITY == null) {
                return InteractionResultHolder.fail(stack);
            }

            consumeEnergy(stack, cost);

            mio_icif_energy_bullet bullet = new mio_icif_energy_bullet(RIFLE_BULLET_ENTITY, player, level);
            bullet.setDamage(damage(stack));
            bullet.setMaxLife(BULLET_MAX_LIFE);
            bullet.setPierceCount(PIERCE_COUNT + 2 * tuning(stack, "pierce"));
            bullet.setTachyon(tuning(stack, "multi"), 2 * tuning(stack, "pierce"), tuning(stack, "true_damage") / 100F);
            bullet.setPotionEffect(new net.minecraft.world.effect.MobEffectInstance(
                net.minecraft.world.effect.MobEffects.WEAKNESS, 600, 2));
            bullet.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0F, BULLET_SPEED, 0.0F);
            level.addFreshEntity(bullet);

            float load = (float) Math.min(1.0, Math.log10(cost / (double) ENERGY_PER_SHOT) / 3.0);
            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                com.miophas.singularity_iteration.common.registry.mio_icif_sounds.TACHYON_SHOT.get(), net.minecraft.sounds.SoundSource.PLAYERS,
                1.0F + load * 0.6F, 1.0F - load * 0.25F);
        }

        player.getCooldowns().addCooldown(this, COOLDOWN_TICKS);
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    // ---- tuning: baseline (100%) is the stock disruptor; every upgrade multiplies the EU per shot
    private static final java.util.List<Spec> SPECS = java.util.List.of(
        new Spec("output", 100, 300, 25, 100, "%"),
        new Spec("multi", 0, 4, 1, 0, ""),
        new Spec("pierce", 0, 5, 1, 0, ""),
        new Spec("true_damage", 0, 100, 25, 0, "%"));

    @Override public java.util.List<Spec> tuningSpecs(ItemStack stack) { return SPECS; }

    /** Damage of one hit: base x output. */
    public float damage(ItemStack stack) { return BULLET_DAMAGE * tuning(stack, "output") / 100F; }

    /**
     * EU per shot: 50k x output^2 x (1+multi)^1.4 x (1+pierce/2)^1.25 x (1+2 x true).
     * All maxed this is ~1,230x the stock cost (~61.5M EU, six shots per full charge).
     */
    public long shotCost(ItemStack stack) {
        double out = tuning(stack, "output") / 100.0;
        double c = ENERGY_PER_SHOT * out * out
            * Math.pow(1 + tuning(stack, "multi"), 1.4)
            * Math.pow(1 + tuning(stack, "pierce") / 2.0, 1.25)
            * (1 + 2.0 * tuning(stack, "true_damage") / 100.0);
        return Math.round(c);
    }

    @Override
    public java.util.List<Component> tuningSummary(ItemStack stack) {
        long cost = shotCost(stack);
        int hits = 1 + tuning(stack, "multi");
        return java.util.List.of(
            Component.translatable("tuning.mio_icif.tachyon.damage", String.format("%.0f", damage(stack)), hits,
                tuning(stack, "true_damage")),
            Component.translatable("tuning.mio_icif.tachyon.pierce", PIERCE_COUNT + 2 * tuning(stack, "pierce"), 2 * tuning(stack, "pierce")),
            Component.translatable("tuning.mio_icif.tachyon.cost", String.format("%,d", cost),
                String.format("%.1f", cost / (double) ENERGY_PER_SHOT)),
            Component.translatable("tuning.mio_icif.tachyon.shots", String.format("%,d", Math.max(0, getEnergy(stack) / Math.max(1, cost)))));
    }

    @Override
    public float getAttackDamageBonus(Entity target, float damage, net.minecraft.world.damagesource.DamageSource damageSource) {
        return 30.0F;
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
        tooltip.add(Component.translatable("tooltip.mio_icif.tachyon_disruptor.damage", String.format("%.0f", damage(stack))));
        for (Component line : tuningSummary(stack)) tooltip.add(line.copy().withStyle(net.minecraft.ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.mio_icif.tuning_hint").withStyle(net.minecraft.ChatFormatting.DARK_AQUA));
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
