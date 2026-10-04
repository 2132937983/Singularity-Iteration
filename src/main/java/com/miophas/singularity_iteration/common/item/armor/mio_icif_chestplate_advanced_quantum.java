package com.miophas.singularity_iteration.common.item.armor;

import com.miophas.singularity_iteration.core.api.item.ArmorFeatureInfo;
import com.miophas.singularity_iteration.core.api.item.IEquipmentHudProvider;
import com.miophas.singularity_iteration.core.api.item.IJetpackItem;
import com.miophas.singularity_iteration.core.prefab.flight.JetpackFlightController;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
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
 * 进阶量子胸甲 (Advanced Quantum Chestplate)
 * 基于METS AdvancedQuantumSuit重构
 * - 最大电量: 100,000,000 EU (Tier 5)
 * - 充电速率: 8,192 EU/t
 * - 飞行(喷气/悬停模式)、生命恢复、击退抗性、防火
 */
@SuppressWarnings({"null", "deprecation"})
public class mio_icif_chestplate_advanced_quantum extends mio_icif_armor_elc implements IJetpackItem, IEquipmentHudProvider {

    public static final int MAX_ENERGY = 100000000;
    public static final int ENERGY_PER_DAMAGE = 10000;
    public static final int CHARGE_RATE = 8192;
    public static final int ARMOR_TIER = 5;

    public static final int HEAL_COST = 30000;
    public static final int JETPACK_CONSUME_PER_TICK = 8;
    public static final int HOVER_CONSUME_PER_TICK = 10;

    private static final String MODE_KEY = "JetpackMode";
    private static final String TOGGLE_TIMER_KEY = "AdvQuantumToggleTimer";
    public static final int MODE_JETPACK = 0;
    public static final int MODE_HOVER = 1;
    private static final int TOGGLE_COOLDOWN = 10;

    public static final float JETPACK_POWER = 1.0F;
    public static final float MAX_ASCENT_SPEED = 0.8F;
    public static final float HOVER_ASCENT_SPEED = 0.6F;
    public static final float HOVER_DESCENT_SPEED = -0.6F;
    public static final float WORLD_HEIGHT_DIVISOR = 1.28F;
    public static final float DROP_PERCENTAGE = 0.05F;

    private static final ResourceLocation KNOCKBACK_RESISTANCE_ID = ResourceLocation.fromNamespaceAndPath("mio_icif", "advanced_quantum_knockback_resistance");

    public mio_icif_chestplate_advanced_quantum(Holder<ArmorMaterial> material, Properties properties) {
        super(material, Type.CHESTPLATE, properties, MAX_ENERGY, 0, "advanced_quantum", CHARGE_RATE, 0, ARMOR_TIER);
    }

    public int getModeInternal(ItemStack stack) {
        net.minecraft.world.item.component.CustomData customData = stack.get(net.minecraft.core.component.DataComponents.CUSTOM_DATA);
        if (customData != null) {
            net.minecraft.nbt.CompoundTag tag = customData.copyTag();
            if (tag.contains(MODE_KEY)) {
                return tag.getInt(MODE_KEY);
            }
        }
        return MODE_JETPACK;
    }

    public void setModeInternal(ItemStack stack, int mode) {
        net.minecraft.nbt.CompoundTag tag = new net.minecraft.nbt.CompoundTag();
        net.minecraft.world.item.component.CustomData customData = stack.get(net.minecraft.core.component.DataComponents.CUSTOM_DATA);
        if (customData != null) {
            tag = customData.copyTag();
        }
        tag.putInt(MODE_KEY, mode);
        stack.set(net.minecraft.core.component.DataComponents.CUSTOM_DATA, net.minecraft.world.item.component.CustomData.of(tag));
    }

    public int getToggleTimer(ItemStack stack) {
        net.minecraft.world.item.component.CustomData customData = stack.get(net.minecraft.core.component.DataComponents.CUSTOM_DATA);
        if (customData != null) {
            net.minecraft.nbt.CompoundTag tag = customData.copyTag();
            if (tag.contains(TOGGLE_TIMER_KEY)) {
                return tag.getInt(TOGGLE_TIMER_KEY);
            }
        }
        return 0;
    }

    public void setToggleTimer(ItemStack stack, int timer) {
        net.minecraft.nbt.CompoundTag tag = new net.minecraft.nbt.CompoundTag();
        net.minecraft.world.item.component.CustomData customData = stack.get(net.minecraft.core.component.DataComponents.CUSTOM_DATA);
        if (customData != null) {
            tag = customData.copyTag();
        }
        tag.putInt(TOGGLE_TIMER_KEY, timer);
        stack.set(net.minecraft.core.component.DataComponents.CUSTOM_DATA, net.minecraft.world.item.component.CustomData.of(tag));
    }

    public Component getModeName(int mode) {
        return switch (mode) {
            case MODE_JETPACK -> Component.translatable("hud.mio_icif.jetpack.mode_jetpack");
            case MODE_HOVER -> Component.translatable("hud.mio_icif.jetpack.mode_hover");
            default -> Component.translatable("hud.mio_icif.jetpack.mode_jetpack");
        };
    }

    /** Stay visible while actually flying on the jetpack. */
    @Override
    public boolean keepEquipmentHudVisible(ItemStack stack, Player player) {
        return !player.onGround() && !player.getAbilities().flying && !player.isInWater();
    }

    @Override
    public Component getEquipmentHudText(ItemStack stack) {
        return Component.translatable("hud.mio_icif.jetpack.display",
            getModeName(getModeInternal(stack)), getEnergy(stack), MAX_ENERGY);
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slotId, boolean isSelected) {
        super.inventoryTick(stack, level, entity, slotId, isSelected);

        if (!(entity instanceof Player player)) return;

        ItemStack actualStack = player.getItemBySlot(EquipmentSlot.CHEST);
        boolean isWearing = !actualStack.isEmpty() && actualStack.getItem() == this;

        if (!isWearing) {
            return;
        }

        if (!level.isClientSide && ArmorFeatureToggle.isEnabled(actualStack, "healing")
                && player.getHealth() < player.getMaxHealth()) {
            if (consumeEnergy(actualStack, HEAL_COST)) {
                player.heal(0.5F);
            }
        }

        if (!level.isClientSide) {
            if (!isEmpty(actualStack) && ArmorFeatureToggle.isEnabled(actualStack, "fire_resistance")) {
                player.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 2, 0, false, false, false));
            } else {
                player.removeEffect(MobEffects.FIRE_RESISTANCE);
            }
        }

        if (!isEmpty(actualStack)) {
            AttributeInstance attr = player.getAttribute(Attributes.KNOCKBACK_RESISTANCE);
            if (ArmorFeatureToggle.isEnabled(actualStack, "knockback_resistance")) {
                if (attr != null && attr.getModifier(KNOCKBACK_RESISTANCE_ID) == null) {
                    attr.addTransientModifier(new AttributeModifier(
                        KNOCKBACK_RESISTANCE_ID,
                        0.5,
                        AttributeModifier.Operation.ADD_VALUE
                    ));
                }
            } else {
                if (attr != null) {
                    attr.removeModifier(KNOCKBACK_RESISTANCE_ID);
                }
            }
        }

        if (!ArmorFeatureToggle.isEnabled(actualStack, "flight")) {
            return;
        }

        JetpackFlightController.tick(player, actualStack, this,
            HOVER_CONSUME_PER_TICK, MAX_ASCENT_SPEED, HOVER_ASCENT_SPEED, HOVER_DESCENT_SPEED, true);
    }

    public long getEnergyPerDamage() {
        return ENERGY_PER_DAMAGE;
    }

    @Override
    public List<ArmorFeatureInfo> getFeatures(ItemStack stack) {
        return List.of(
            new ArmorFeatureInfo(EquipmentSlot.CHEST, "flight", "tooltip.mio_icif.armor.feature_flight"),
            new ArmorFeatureInfo(EquipmentSlot.CHEST, "jetpack_mode", "tooltip.mio_icif.armor.feature_jetpack_mode", getModeName(getModeInternal(stack))),
            new ArmorFeatureInfo(EquipmentSlot.CHEST, "fire_resistance", "tooltip.mio_icif.armor.feature_fire_resistance"),
            new ArmorFeatureInfo(EquipmentSlot.CHEST, "healing", "tooltip.mio_icif.armor.feature_healing"),
            new ArmorFeatureInfo(EquipmentSlot.CHEST, "knockback_resistance", "tooltip.mio_icif.armor.feature_knockback_resistance")
        );
    }

    // ==================== IJetpackItem API ====================

    @Override
    public float getThrust() {
        return JETPACK_POWER;
    }

    @Override
    public long getEnergyPerTickFlying() {
        return JETPACK_CONSUME_PER_TICK;
    }

    @Override
    public boolean hasHeightLimit() {
        return false;
    }

    @Override
    public JetpackMode getMode(ItemStack stack) {
        int internal = getModeInternal(stack);
        return internal == MODE_HOVER ? JetpackMode.HOVER : JetpackMode.FLIGHT;
    }

    @Override
    public void setMode(ItemStack stack, JetpackMode mode) {
        int internal = switch (mode) {
            case OFF -> MODE_JETPACK;
            case NORMAL -> MODE_JETPACK;
            case HOVER -> MODE_HOVER;
            case FLIGHT -> MODE_JETPACK;
        };
        setModeInternal(stack, internal);
    }
}