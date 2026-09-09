package com.singularity_iteration.mio_icif.integration.curios;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import top.theillusivec4.curios.api.SlotContext;

public class ElectricFireProofNecklaceCurio extends MioIcifTrinketBase {

    private static final int STORAGE_ENERGY = 100000;
    private static final int TIER = 2;

    public ElectricFireProofNecklaceCurio() {
        super(new Item.Properties().stacksTo(1), STORAGE_ENERGY, 0, "electric_fire_proof_necklace", STORAGE_ENERGY, 0, TIER, "necklace");
    }

    @Override
    public void curioTick(SlotContext slotContext, ItemStack stack) {
        LivingEntity entity = slotContext.entity();
        if (!entity.level().isClientSide()) {
            if (isEmpty(stack)) return;

            if (entity.isOnFire() || entity.isInLava()) {
                consumeEnergy(stack, 200);
                entity.clearFire();
            }

            if (!entity.hasEffect(MobEffects.FIRE_RESISTANCE)) {
                entity.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 40, 0, false, false));
            }
        }
    }

    @Override
    protected void onTrinketUnequipped(ItemStack stack, LivingEntity entity) {
        if (entity.level().isClientSide()) return;
        entity.removeEffect(MobEffects.FIRE_RESISTANCE);
    }
}