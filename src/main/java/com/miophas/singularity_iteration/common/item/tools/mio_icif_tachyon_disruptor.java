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
/**
 * Tachyon disruptor (T5): the boss weapon. 120 a bolt, piercing, tunable (output, multi-hit, piercing, true damage) at a
 * steeply rising cost per shot. 0.1.11.197 (T4): on {@link AbstractEnergyGun}, shot numbers unchanged (D5 keeps it the top
 * weapon). Melee: +30 while a hit can be paid (1,500 EU a hit), a sword's attack speed; the +30 was a constant that needed
 * no energy, at attack speed 4.
 */
public class mio_icif_tachyon_disruptor extends AbstractEnergyGun implements com.miophas.singularity_iteration.common.item.tuning.ITunableItem {
    public static final int MAX_ENERGY = 400000000;
    public static final int CHARGE_RATE = 8192;
    public static final int TIER = 5;
    public static final int ENERGY_PER_SHOT = 50000;
    public static final float BULLET_DAMAGE = 120.0F;
    public static final float BULLET_SPEED = 4.0F;
    public static final int BULLET_MAX_LIFE = 600;
    public static final int PIERCE_COUNT = 3;
    public static final int COOLDOWN_TICKS = 5;
    public static final float MELEE_DAMAGE = 30.0F;
    public static final int MELEE_COST = 1500;

    private static final net.minecraft.world.item.component.ItemAttributeModifiers MELEE = meleeAttributes(MELEE_DAMAGE);
    private static final net.minecraft.world.item.component.ItemAttributeModifiers MELEE_FLAT = meleeAttributes(0);

    public static EntityType<mio_icif_energy_bullet> RIFLE_BULLET_ENTITY;

    public static void setRifleBulletEntity(EntityType<mio_icif_energy_bullet> entityType) {
        RIFLE_BULLET_ENTITY = entityType;
    }

    public mio_icif_tachyon_disruptor(Properties properties) {
        super(properties, MAX_ENERGY, CHARGE_RATE, TIER, ENERGY_PER_SHOT,
            new AbstractEnergyGun.Spec("tachyon_disruptor", "TACHYON", 0xC77DFF, BULLET_DAMAGE, BULLET_SPEED, BULLET_MAX_LIFE, COOLDOWN_TICKS,
                () -> com.miophas.singularity_iteration.common.registry.mio_icif_sounds.TACHYON_SHOT.get(), 1.0F, 1.0F));
    }

    @Override protected EntityType<mio_icif_energy_bullet> bulletType() { return RIFLE_BULLET_ENTITY; }

    @Override
    protected void configure(mio_icif_energy_bullet bullet, ItemStack stack) {
        bullet.setPierceCount(PIERCE_COUNT + 2 * tuning(stack, "pierce"));
        bullet.setTachyon(tuning(stack, "multi"), 2 * tuning(stack, "pierce"), tuning(stack, "true_damage") / 100F);
        bullet.setPotionEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.WEAKNESS, 600, 2));
    }

    /** A heavier (tuned) shot sounds louder and lower. */
    @Override
    protected void playShot(Level level, Player player, ItemStack stack, long cost) {
        float load = (float) Math.min(1.0, Math.log10(cost / (double) ENERGY_PER_SHOT) / 3.0);
        level.playSound(null, player.getX(), player.getY(), player.getZ(), com.miophas.singularity_iteration.common.registry.mio_icif_sounds.TACHYON_SHOT.get(),
            net.minecraft.sounds.SoundSource.PLAYERS, 1.0F + load * 0.6F, 1.0F - load * 0.25F);
    }

    // ---- tuning: baseline (100%) is the stock disruptor; every upgrade multiplies the EU per shot
    private static final java.util.List<com.miophas.singularity_iteration.common.item.tuning.ITunableItem.Spec> SPECS = java.util.List.of(
        new com.miophas.singularity_iteration.common.item.tuning.ITunableItem.Spec("output", 100, 300, 25, 100, "%"),
        new com.miophas.singularity_iteration.common.item.tuning.ITunableItem.Spec("multi", 0, 4, 1, 0, ""),
        new com.miophas.singularity_iteration.common.item.tuning.ITunableItem.Spec("pierce", 0, 5, 1, 0, ""),
        new com.miophas.singularity_iteration.common.item.tuning.ITunableItem.Spec("true_damage", 0, 100, 25, 0, "%"));
    @Override public java.util.List<com.miophas.singularity_iteration.common.item.tuning.ITunableItem.Spec> tuningSpecs(ItemStack stack) { return SPECS; }

    /** Damage of one hit: base x output. */
    @Override
    public float damage(ItemStack stack) { return BULLET_DAMAGE * tuning(stack, "output") / 100F; }

    /**
     * EU per shot: 50k x output^2 x (1+multi)^1.4 x (1+pierce/2)^1.25 x (1+2 x true).
     * All maxed this is ~1,230x the stock cost (~61.5M EU, six shots per full charge).
     */
    @Override
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
    public net.minecraft.world.item.component.ItemAttributeModifiers getDefaultAttributeModifiers(ItemStack stack) {
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
        for (Component line : tuningSummary(stack)) tooltip.add(line.copy().withStyle(net.minecraft.ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.mio_icif.tuning_hint").withStyle(net.minecraft.ChatFormatting.DARK_AQUA));
    }

    @Override public boolean isEnchantable(ItemStack stack) { return true; }
    @Override public int getEnchantmentValue() { return 15; }
}
