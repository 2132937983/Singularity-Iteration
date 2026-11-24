package com.miophas.singularity_iteration.common.item.tools;

import com.miophas.singularity_iteration.core.api.tool.ToggleableElectricTool;

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
public class mio_icif_electric_force_field_generator extends mio_icif_tool_elc implements PassiveDeviceRules.WornDevice, com.miophas.singularity_iteration.core.api.tool.IToolModeProvider {

    private static final int MAX_ENERGY = 1000000;
    private static final int TRANSFER_SPEED = 512;
    private static final int ENERGY_COST = 5000;
    private static final float MAX_ABSORPTION = 10.0F;
    private static final int EFFECT_DURATION = 600;
    private static final int MIN_EFFECT_DURATION = 100;
    private static final String TAG_AUTO_SUPPLEMENT = "AutoSupplement";

    public mio_icif_electric_force_field_generator() {
        super(new Item.Properties(), MAX_ENERGY, 0L, "item_electric_force_field_generator", TRANSFER_SPEED, ENERGY_COST, 2);
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slotId, boolean isSelected) {
        super.inventoryTick(stack, level, entity, slotId, isSelected);

        if (level.isClientSide || !(entity instanceof Player player)) {
            return;
        }
        // 0.1.11.201 (T5d, decision D7): with Curios the device works from an accessory slot only (PassiveDeviceCurios ticks
        // it there); without Curios it still works from the inventory
        if (!PassiveDeviceRules.fromInventory()) {
            return;
        }
        tickWorn(stack, player);
    }

    /** One tick of the device for its wearer (an accessory slot, or the inventory without Curios). */
    @Override
    public void tickWorn(ItemStack stack, Player player) {
        if (player.level().isClientSide) {
            return;
        }

        if (!getAutoSupplement(stack)) {
            return;
        }

        // 0.1.11.198 (T5, U-09): once a second, from one copy, refill the shield when it is below its 10 points or the effect
        // (which holds the absorption cap) runs out. Before, a shield shot away waited about 25 s for the effect to expire,
        // and a refill cut a golden apple's larger absorption down to 10.
        if (!PassiveDeviceRules.onInterval(player, PassiveDeviceRules.FIRST_AID_INTERVAL)) return;
        net.minecraft.world.effect.MobEffectInstance existingEffect = player.getEffect(net.minecraft.world.effect.MobEffects.ABSORPTION);
        boolean effectHolds = existingEffect != null && existingEffect.getDuration() > MIN_EFFECT_DURATION;
        if (effectHolds && player.getAbsorptionAmount() >= MAX_ABSORPTION) return;
        if (!PassiveDeviceRules.isActingCopy(player, stack, s -> getAutoSupplement(s) && hasEnoughEnergy(s, ENERGY_COST))) return;

        if (consumeEnergy(stack, ENERGY_COST)) {   // 0.1.11.194 (T1, I-09): shared atomic debit
            float before = player.getAbsorptionAmount();
            if (!effectHolds || existingEffect.getAmplifier() < 2) {
                player.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                    net.minecraft.world.effect.MobEffects.ABSORPTION, EFFECT_DURATION, 2, false, false, true));
            }
            player.setAbsorptionAmount(Math.max(before, MAX_ABSORPTION));
        }
    }

    @Override
    public void toggleActive(ItemStack stack, Player player) {
        boolean autoSupplement = !getAutoSupplement(stack);
        setAutoSupplement(stack, autoSupplement);

        if (autoSupplement) {
            player.displayClientMessage(Component.translatable("message.mio_icif.electric_force_field_generator.auto_on"), true);   // 0.1.11.198 (T5, U-20)
        } else {
            player.displayClientMessage(Component.translatable("message.mio_icif.electric_force_field_generator.auto_off"), true);   // 0.1.11.198 (T5, U-20)
        }
    }

    @Override
    public boolean isEnchantable(ItemStack stack) {
        return false;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);
        PassiveDeviceRules.appendWornHint(tooltipComponents, "necklace");   // 0.1.11.201 (T5d)
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

    // ---- equipment console (IToolModeProvider)
    @Override public java.util.List<net.minecraft.network.chat.Component> toolModes(ItemStack stack) {
        return java.util.List.of(net.minecraft.network.chat.Component.translatable("tool_mode.mio_icif.auto_off"), net.minecraft.network.chat.Component.translatable("tool_mode.mio_icif.auto_on"));
    }
    @Override public int toolModeIndex(ItemStack stack) { return getAutoSupplement(stack) ? 1 : 0; }
}
