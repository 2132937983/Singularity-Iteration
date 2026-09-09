package com.singularity_iteration.mio_icif.Items.Armor;

import com.singularity_iteration.mio_icif.api.item.ArmorFeatureInfo;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * 重型量子胸甲 (Heavy Quantum Chestplate)
 * 基于METS HeavyQuantumSuit重构
 * - 最大电量: 10,000,000 EU (Tier 4)
 * - 充电速率: 4,096 EU/t
 * - 生命恢复(低血量时)、击退抗性、防火，无飞行能力
 */
@SuppressWarnings("null")
public class mio_icif_chestplate_heavy_quantum extends mio_icif_armor_elc {

    public static final int MAX_ENERGY = 10000000;
    public static final int ENERGY_PER_DAMAGE = 10000;
    public static final int CHARGE_RATE = 4096;
    public static final int ARMOR_TIER = 4;

    public static final int HEAL_COST = 5000;
    public static final float HEAL_THRESHOLD = 0.2F;

    private static final ResourceLocation KNOCKBACK_RESISTANCE_ID = ResourceLocation.fromNamespaceAndPath("mio_icif", "heavy_quantum_knockback_resistance");

    public mio_icif_chestplate_heavy_quantum(Holder<ArmorMaterial> material, Properties properties) {
        super(material, Type.CHESTPLATE, properties, MAX_ENERGY, 0, "heavy_quantum", CHARGE_RATE, 0, ARMOR_TIER);
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slotId, boolean isSelected) {
        super.inventoryTick(stack, level, entity, slotId, isSelected);

        if (!(entity instanceof Player player)) return;

        ItemStack actualStack = player.getItemBySlot(EquipmentSlot.CHEST);
        boolean isWearing = !actualStack.isEmpty() && actualStack.getItem() == this;

        if (!isWearing) return;

        // 生命恢复（血量低于20%时）
        if (!level.isClientSide && ArmorFeatureToggle.isEnabled(actualStack, "healing")
                && player.getHealth() < player.getMaxHealth() * HEAL_THRESHOLD) {
            if (consumeEnergy(actualStack, HEAL_COST)) {
                player.heal(1.0F);
            }
        }

        // 防火效果
        if (!level.isClientSide) {
            if (!isEmpty(actualStack) && ArmorFeatureToggle.isEnabled(actualStack, "fire_resistance")) {
                player.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 2, 0, false, false, false));
            } else {
                player.removeEffect(MobEffects.FIRE_RESISTANCE);
            }
        }

        // 击退抗性
        if (!isEmpty(actualStack)) {
            AttributeInstance attr = player.getAttribute(Attributes.KNOCKBACK_RESISTANCE);
            if (ArmorFeatureToggle.isEnabled(actualStack, "knockback_resistance")) {
                if (attr != null && attr.getModifier(KNOCKBACK_RESISTANCE_ID) == null) {
                    attr.addTransientModifier(new AttributeModifier(
                        KNOCKBACK_RESISTANCE_ID,
                        1.0,
                        AttributeModifier.Operation.ADD_VALUE
                    ));
                }
            } else {
                if (attr != null) {
                    attr.removeModifier(KNOCKBACK_RESISTANCE_ID);
                }
            }
        }
    }

    public long getEnergyPerDamage() {
        return ENERGY_PER_DAMAGE;
    }

    @Override
    public List<ArmorFeatureInfo> getFeatures(ItemStack stack) {
        return List.of(
            new ArmorFeatureInfo(EquipmentSlot.CHEST, "healing", "tooltip.mio_icif.armor.feature_healing"),
            new ArmorFeatureInfo(EquipmentSlot.CHEST, "fire_resistance", "tooltip.mio_icif.armor.feature_fire_resistance"),
            new ArmorFeatureInfo(EquipmentSlot.CHEST, "knockback_resistance", "tooltip.mio_icif.armor.feature_knockback_resistance")
        );
    }
}