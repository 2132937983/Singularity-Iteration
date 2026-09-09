package com.singularity_iteration.mio_icif.Items.Armor;

import com.singularity_iteration.mio_icif.api.item.ArmorFeatureInfo;
import com.singularity_iteration.mio_icif.effect.mio_icif_effects;
import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

@SuppressWarnings("null")
public class mio_icif_helmet_quantum extends mio_icif_armor_elc {

    public static final int MAX_ENERGY = 10000000;

    public static final int EFFECT_DURATION = 400;

    public static final int OXYGEN_COST = 1000;
    public static final int TIN_CAN_FOOD_COST = 10000;
    public static final int NIGHT_VISION_COST = 5;

    private static final Map<Holder<MobEffect>, Integer> POTION_REMOVAL_COST = new IdentityHashMap<>();

    static {
        POTION_REMOVAL_COST.put(MobEffects.POISON, 10000);
        POTION_REMOVAL_COST.put(mio_icif_effects.RADIATION, 0);
        POTION_REMOVAL_COST.put(MobEffects.WITHER, 25000);
    }

    public mio_icif_helmet_quantum(Holder<ArmorMaterial> material, Properties properties) {
        super(material, Type.HELMET, properties, MAX_ENERGY, 0, "quantum", 12000, 0, 6);
    }

    @Override
    public long getEnergyPerDamage() {
        return 20000;
    }

    @Override
    public float getDamageAbsorptionRatio(EquipmentSlot slot) {
        return slot == EquipmentSlot.HEAD ? 0.15F : 0.0F;
    }

    @Override
    public List<ArmorFeatureInfo> getFeatures(ItemStack stack) {
        return List.of(
            new ArmorFeatureInfo(EquipmentSlot.HEAD, "night_vision", "tooltip.mio_icif.armor.feature_night_vision"),
            new ArmorFeatureInfo(EquipmentSlot.HEAD, "water_breathing", "tooltip.mio_icif.armor.feature_water_breathing"),
            new ArmorFeatureInfo(EquipmentSlot.HEAD, "auto_food", "tooltip.mio_icif.armor.feature_auto_food")
        );
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slotId, boolean isSelected) {
        super.inventoryTick(stack, level, entity, slotId, isSelected);

        if (level.isClientSide || !(entity instanceof Player player)) {
            return;
        }

        ItemStack helmet = player.getItemBySlot(EquipmentSlot.HEAD);
        if (helmet != stack || isEmpty(stack)) {
            // IC2原版行为：取下头盔时，不移除效果，让效果自然过期
            return;
        }

        if (ArmorFeatureToggle.isEnabled(stack, "water_breathing")) {
            int airSupply = player.getAirSupply();
            int maxAir = player.getMaxAirSupply();
            if (airSupply < maxAir) {
                if (consumeEnergy(stack, OXYGEN_COST)) {
                    player.setAirSupply(Math.min(maxAir, airSupply + 200));
                }
            }
            player.addEffect(new MobEffectInstance(MobEffects.WATER_BREATHING, EFFECT_DURATION, 0, false, false, false));
        }
        // IC2原版行为：水下呼吸功能关闭时，不移除效果，让效果自然过期

        if (ArmorFeatureToggle.isEnabled(stack, "night_vision")) {
            if (consumeEnergy(stack, NIGHT_VISION_COST)) {
                int skylight = level.getMaxLocalRawBrightness(player.blockPosition());
                if (skylight > 8) {
                    player.removeEffect(MobEffects.BLINDNESS);
                    player.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 100, 0, true, true, false));
                } else {
                    player.removeEffect(MobEffects.CONFUSION);
                    player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 300, 0, true, true, false));
                }
            }
            // IC2原版行为：能量不足或夜视关闭时，不移除夜视效果，让效果自然过期
        }

        if (ArmorFeatureToggle.isEnabled(stack, "auto_food")) {
            if (player.getFoodData().needsFood()) {
                // 查找背包中的食物
                ItemStack foodStack = findFoodInInventory(player);
                if (foodStack != null && !foodStack.isEmpty() && consumeEnergy(stack, TIN_CAN_FOOD_COST)) {
                    // 获取食物属性
                    FoodProperties foodProperties = foodStack.getItem().getFoodProperties(foodStack, player);
                    if (foodProperties != null) {
                        // 消耗食物并恢复饱食度
                        player.getFoodData().eat(foodProperties.nutrition(), foodProperties.saturation());
                        // 减少物品数量
                        foodStack.shrink(1);
                    }
                }
            }
        }

        List<Holder<MobEffect>> toRemove = new ArrayList<>();
        for (MobEffectInstance effect : player.getActiveEffects()) {
            Holder<MobEffect> potionHolder = effect.getEffect();
            Integer cost = POTION_REMOVAL_COST.get(potionHolder);
            if (cost != null) {
                int totalCost = cost * (effect.getAmplifier() + 1);
                if (consumeEnergy(stack, totalCost)) {
                    toRemove.add(potionHolder);
                }
            }
        }
        for (Holder<MobEffect> potionHolder : toRemove) {
            player.removeEffect(potionHolder);
        }
    }

    /**
     * 在玩家背包中查找可食用的食物
     * @param player 玩家
     * @return 找到的食物物品堆，如果没有则返回null
     */
    private ItemStack findFoodInInventory(Player player) {
        // 遍历主背包（0-35槽位）
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack itemStack = player.getInventory().getItem(i);
            if (!itemStack.isEmpty() && itemStack.getFoodProperties(player) != null) {
                return itemStack;
            }
        }
        return null;
    }
}