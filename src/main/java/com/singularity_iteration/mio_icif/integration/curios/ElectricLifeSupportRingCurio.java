package com.singularity_iteration.mio_icif.integration.curios;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodData;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import top.theillusivec4.curios.api.SlotContext;

public class ElectricLifeSupportRingCurio extends MioIcifTrinketBase {

    private static final int STORAGE_ENERGY = 100000000;
    private static final int TIER = 5;

    private static final int HEAL_COST = 10000;
    private static final int FOOD_COST = 200;
    private static final int ABSORB_COST = 5000;

    public ElectricLifeSupportRingCurio() {
        super(new Item.Properties().stacksTo(1), STORAGE_ENERGY, 0, "electric_life_support_ring", STORAGE_ENERGY, 0, TIER, "ring");
    }

    @Override
    public void curioTick(SlotContext slotContext, ItemStack stack) {
        LivingEntity entity = slotContext.entity();
        if (!(entity instanceof Player player) || entity.level().isClientSide()) return;

        if (!player.hasEffect(MobEffects.ABSORPTION)) {
            player.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, Integer.MAX_VALUE, 4, false, false, false));
        }

        boolean updated = false;

        float currentHealth = player.getHealth();
        if (currentHealth < player.getMaxHealth()) {
            if (consumeEnergy(stack, HEAL_COST)) {
                player.setHealth(currentHealth + 1.0f);
                updated = true;
            }
        }

        FoodData foodData = player.getFoodData();
        if (foodData.needsFood()) {
            if (consumeEnergy(stack, FOOD_COST)) {
                foodData.eat(1, 0.2f);
                updated = true;
            }
        }

        float currentAbsorb = player.getAbsorptionAmount();
        if (currentAbsorb < 20.0f) {
            if (consumeEnergy(stack, ABSORB_COST)) {
                player.setAbsorptionAmount(currentAbsorb + 0.5f);
                updated = true;
            }
        }

        if (updated) {
            player.containerMenu.broadcastChanges();
        }
    }

    @Override
    protected void onTrinketEquipped(ItemStack stack, LivingEntity entity) {
        if (entity.level().isClientSide()) return;
        if (entity instanceof Player player) {
            player.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, Integer.MAX_VALUE, 4, false, false, false));
        }
    }

    @Override
    protected void onTrinketUnequipped(ItemStack stack, LivingEntity entity) {
        if (entity.level().isClientSide()) return;
        if (entity instanceof Player player) {
            player.removeEffect(MobEffects.ABSORPTION);
            player.setAbsorptionAmount(0.0f);
        }
    }
}