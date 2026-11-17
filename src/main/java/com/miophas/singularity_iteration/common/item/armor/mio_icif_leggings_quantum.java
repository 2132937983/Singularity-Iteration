package com.miophas.singularity_iteration.common.item.armor;

import com.miophas.singularity_iteration.core.api.item.ArmorFeatureInfo;
import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.List;

@SuppressWarnings("null")
public class mio_icif_leggings_quantum extends mio_icif_armor_elc {

    public static final int MAX_ENERGY = 10000000;

    public static final int EFFECT_DURATION = 100;
    public static final int SPEED_ENERGY_COST = 10;
    public static final int SPEED_TICK_INTERVAL = 1;

    public mio_icif_leggings_quantum(Holder<ArmorMaterial> material, Properties properties) {
        super(material, Type.LEGGINGS, properties, MAX_ENERGY, 0, "quantum", 12000, 0, 6);
    }

    @Override
    public long getEnergyPerDamage() {
        return 20000;
    }

    @Override
    public float getDamageAbsorptionRatio(EquipmentSlot slot) {
        return slot == EquipmentSlot.LEGS ? 0.30F : 0.0F;
    }

    @Override
    public List<ArmorFeatureInfo> getFeatures(ItemStack stack) {
        return List.of(
            new ArmorFeatureInfo(EquipmentSlot.LEGS, "speed_boost", "tooltip.mio_icif.armor.feature_speed_boost")
        );
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slotId, boolean isSelected) {
        super.inventoryTick(stack, level, entity, slotId, isSelected);

        if (level.isClientSide || !(entity instanceof Player player)) {
            return;
        }

        ItemStack leggings = player.getItemBySlot(EquipmentSlot.LEGS);
        if (leggings != stack) {
            // 0.1.11.3: a pair that is not worn leaves Speed alone when other leggings are worn (it took away what the
            // worn pair gave each tick), and otherwise removes only its own effect (never a potion or a beacon)
            if (!(leggings.getItem() instanceof mio_icif_leggings_quantum)) removeOwnSpeed(player);
            return;
        }
        if (isEmpty(stack)) {
            removeOwnSpeed(player);
            return;
        }

        if (ArmorFeatureToggle.isEnabled(stack, "speed_boost")) {
            if ((player.onGround() || player.isInWater()) && player.zza > 0) {
                if (player.tickCount % SPEED_TICK_INTERVAL == 0) {
                    consumeEnergy(stack, SPEED_ENERGY_COST);
                }
                player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, EFFECT_DURATION, 1, false, false, false));
            } else {
                player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, EFFECT_DURATION, 1, false, false, false));
            }
        } else {
            removeOwnSpeed(player);
        }
    }

    /**
     * 0.1.11.3: removes Speed only when it is the leggings' own effect (amplifier 1, no particles, no icon, at most
     * {@link #EFFECT_DURATION} ticks left). It used to remove any Speed, so potions and beacons were stripped.
     */
    static void removeOwnSpeed(Player player) {
        MobEffectInstance speed = player.getEffect(MobEffects.MOVEMENT_SPEED);
        if (speed != null && speed.getAmplifier() == 1 && !speed.isVisible() && !speed.showIcon() && !speed.isAmbient()
                && speed.getDuration() <= EFFECT_DURATION) {
            player.removeEffect(MobEffects.MOVEMENT_SPEED);
        }
    }

}