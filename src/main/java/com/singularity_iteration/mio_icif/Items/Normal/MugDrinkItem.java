package com.singularity_iteration.mio_icif.Items.Normal;

import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;

import javax.annotation.Nonnull;

@SuppressWarnings("null")
public class MugDrinkItem extends Item {

    private final MugType mugType;

    public MugDrinkItem(MugType mugType, Properties properties) {
        super(properties.food(createFoodProperties(mugType)));
        this.mugType = mugType;
    }

    @Nonnull
    private static FoodProperties createFoodProperties(MugType mugType) {
        FoodProperties.Builder builder = new FoodProperties.Builder();
        builder.alwaysEdible();

        switch (mugType) {
            case COLD_COFFEE:
                builder.nutrition(1).saturationModifier(0.1f);
                break;
            case DARK_COFFEE:
                builder.nutrition(2).saturationModifier(0.2f);
                break;
            case COFFEE:
                builder.nutrition(3).saturationModifier(0.3f);
                break;
            case BLACK_TEA:
                builder.nutrition(2).saturationModifier(0.2f);
                break;
            default:
                builder.nutrition(0).saturationModifier(0.0f);
                break;
        }

        return builder.build();
    }

    @Override
    @Nonnull
    public ItemStack finishUsingItem(@Nonnull ItemStack stack, @Nonnull Level level, @Nonnull LivingEntity entity) {
        if (!level.isClientSide() && entity instanceof Player player) {
            applyEffects(player);
        }

        ItemStack result = super.finishUsingItem(stack, level, entity);

        if (entity instanceof Player player) {
            ItemStack emptyMug = new ItemStack(mio_icif_normal.EMPTY_MUG.get());
            if (!player.getInventory().add(emptyMug)) {
                player.drop(emptyMug, false);
            }
        }

        return result;
    }

    @Override
    @Nonnull
    public UseAnim getUseAnimation(@Nonnull ItemStack stack) {
        return UseAnim.DRINK;
    }

    private void applyEffects(Player player) {
        int maxAmplifier;
        int extraDuration;

        switch (mugType) {
            case COLD_COFFEE:
                maxAmplifier = 1;
                extraDuration = 600;
                break;
            case DARK_COFFEE:
                maxAmplifier = 5;
                extraDuration = 1200;
                break;
            case COFFEE:
                maxAmplifier = 6;
                extraDuration = 1200;
                break;
            case BLACK_TEA:
                maxAmplifier = 3;
                extraDuration = 800;
                break;
            default:
                return;
        }

        int highest = 0;

        int x = amplifyEffect(player, MobEffects.DIG_SPEED, maxAmplifier, extraDuration);
        if (x > highest) highest = x;

        x = amplifyEffect(player, MobEffects.MOVEMENT_SPEED, maxAmplifier, extraDuration);
        if (x > highest) highest = x;

        if (mugType == MugType.COFFEE) highest -= 2;

        if (highest >= 3) {
            player.addEffect(new MobEffectInstance(MobEffects.CONFUSION, (highest - 2) * 200, 0));
            if (highest >= 4) {
                player.addEffect(new MobEffectInstance(MobEffects.WITHER, 200, highest - 3));
            }
        }
    }

    private int amplifyEffect(Player player, Holder<MobEffect> effectHolder, int maxAmplifier, int extraDuration) {
        MobEffectInstance existing = player.getEffect(effectHolder);

        if (existing != null) {
            int newAmplifier = existing.getAmplifier();
            int newDuration = existing.getDuration();
            if (newAmplifier < maxAmplifier) newAmplifier++;
            newDuration += extraDuration;
            player.addEffect(new MobEffectInstance(effectHolder, newDuration, newAmplifier));
            return newAmplifier;
        }

        player.addEffect(new MobEffectInstance(effectHolder, 300, 0));
        return 1;
    }

    public enum MugType {
        COLD_COFFEE,
        DARK_COFFEE,
        COFFEE,
        BLACK_TEA
    }
}

