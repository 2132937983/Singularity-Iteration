package com.singularity_iteration.mio_icif.Items.Tools;

import com.singularity_iteration.mio_icif.api.tool.ToggleableElectricTool;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * 电力伤害吸收仪
 * 手持时自动提供伤害吸收效果（吸收之心）
 * G键切换自动模式
 * 消耗一次电量提供5个吸收之心（10点吸收值），持续30秒
 */
@SuppressWarnings("null")
public class mio_icif_electric_force_field_generator extends mio_icif_tool_elc implements ToggleableElectricTool {

    private static final int MAX_ENERGY = 1000000;
    private static final int TRANSFER_SPEED = 512;
    private static final int ENERGY_COST = 5000;
    private static final float MAX_ABSORPTION = 10.0F;
    private static final int EFFECT_DURATION = 600;
    private static final int MIN_EFFECT_DURATION = 100;
    private static final String TAG_AUTO_SUPPLEMENT = "AutoSupplement";

    public mio_icif_electric_force_field_generator() {
        super(new Item.Properties(), MAX_ENERGY, MAX_ENERGY, "item_electric_force_field_generator", TRANSFER_SPEED, ENERGY_COST, 2);
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slotId, boolean isSelected) {
        super.inventoryTick(stack, level, entity, slotId, isSelected);

        if (level.isClientSide || !(entity instanceof Player player)) {
            return;
        }

        if (!getAutoSupplement(stack)) {
            return;
        }

        net.minecraft.world.effect.MobEffectInstance existingEffect = player.getEffect(net.minecraft.world.effect.MobEffects.ABSORPTION);
        if (existingEffect != null && existingEffect.getDuration() > MIN_EFFECT_DURATION) {
            return;
        }

        if (getEnergy(stack) >= ENERGY_COST) {
            extractEnergy(stack, ENERGY_COST);
            player.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                net.minecraft.world.effect.MobEffects.ABSORPTION,
                EFFECT_DURATION,
                2,
                false,
                false,
                true
            ));
            player.setAbsorptionAmount(MAX_ABSORPTION);
        }
    }

    @Override
    public void toggleActive(ItemStack stack, Player player) {
        boolean autoSupplement = !getAutoSupplement(stack);
        setAutoSupplement(stack, autoSupplement);

        if (autoSupplement) {
            player.sendSystemMessage(Component.translatable("message.mio_icif.electric_force_field_generator.auto_on"));
        } else {
            player.sendSystemMessage(Component.translatable("message.mio_icif.electric_force_field_generator.auto_off"));
        }
    }

    @Override
    public boolean isEnchantable(ItemStack stack) {
        return false;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);
        tooltipComponents.add(Component.translatable("tooltip.mio_icif.electric_force_field_generator.desc"));
        tooltipComponents.add(Component.translatable("tooltip.mio_icif.electric_force_field_generator.mode",
            getAutoSupplement(stack)
                ? Component.translatable("tooltip.mio_icif.electric_force_field_generator.auto_on")
                : Component.translatable("tooltip.mio_icif.electric_force_field_generator.auto_off")));
    }

    public static boolean getAutoSupplement(ItemStack stack) {
        var customData = stack.get(DataComponents.CUSTOM_DATA);
        if (customData != null && !customData.isEmpty()) {
            return customData.copyTag().getBoolean(TAG_AUTO_SUPPLEMENT);
        }
        return false;
    }

    public static void setAutoSupplement(ItemStack stack, boolean value) {
        var customData = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
        CompoundTag tag = customData.copyTag();
        tag.putBoolean(TAG_AUTO_SUPPLEMENT, value);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }
}
