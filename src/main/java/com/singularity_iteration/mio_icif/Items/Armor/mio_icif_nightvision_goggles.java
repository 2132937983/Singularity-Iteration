package com.singularity_iteration.mio_icif.Items.Armor;

import com.singularity_iteration.mio_icif.api.item.INightVisionItem;
import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * 夜视?(Nightvision Goggles)
 * 参考IC2原版夜视镜实�?
 * - 最大电力? 200,000 EU (Tier 1)
 * - 充电速率: 200 EU/t
 * - 有电时自动提供夜视效果（类似纳米头盔�?
 * - 夜视时每tick消耗? EU
 * - 电量耗尽时移除夜视效果?
 */
@SuppressWarnings("null")
public class mio_icif_nightvision_goggles extends mio_icif_armor_elc implements INightVisionItem {

    // IC2原版参数
    public static final int MAX_ENERGY = 200000;
    public static final int CHARGE_RATE = 200;
    public static final int ENERGY_PER_TICK = 1;
    public static final int ARMOR_TIER = 1;

    // 夜视效果持续时间（tick），20秒避免闪�?
    public static final int EFFECT_DURATION = 400;

    public mio_icif_nightvision_goggles(Holder<ArmorMaterial> material, Properties properties) {
        super(material, Type.HELMET, properties, MAX_ENERGY, 0, "nightvision", CHARGE_RATE, ENERGY_PER_TICK, ARMOR_TIER);
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slotId, boolean isSelected) {
        super.inventoryTick(stack, level, entity, slotId, isSelected);

        if (level.isClientSide || !(entity instanceof Player player)) {
            return;
        }

        // 只在头盔槽位穿戴时生�?
        ItemStack helmet = player.getItemBySlot(EquipmentSlot.HEAD);
        if (helmet != stack || isEmpty(stack)) {
            // IC2原版行为：取下头盔时，不移除夜视效果，让效果自然过期
            return;
        }

        // 有电时消耗能量并提供夜视效果
        if (consumeEnergy(stack)) {
            player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, EFFECT_DURATION, 0, false, false, false));
        }
        // IC2原版行为：能量不足时，不移除夜视效果，让效果自然过期
    }
}