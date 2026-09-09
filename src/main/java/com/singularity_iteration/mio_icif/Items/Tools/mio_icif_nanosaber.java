package com.singularity_iteration.mio_icif.Items.Tools;

import com.singularity_iteration.mio_icif.api.tool.ToggleableElectricTool;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * 纳米�?- IC2风格的高级电力武�?
 * 继承电力工具基础值?
 *
 * 特性：
 * - 右键切换开关状态?
 * - 开启时攻击伤害20，关闭时攻击伤害4
 * - 攻击时消耗?00EU
 * - 开启时对纳�?量子护甲造成额外电力伤害
 * - 最大存储?60000EU，传输限制?00EU/t，等�?（HV�?
 */
@SuppressWarnings("null")
public class mio_icif_nanosaber extends mio_icif_tool_elc implements ToggleableElectricTool {

    // 纳米剑最大能量?(IC2原版: 160000 EU)
    public static final int NANOSABER_MAX_ENERGY = 160000;
    // 纳米剑传输限制?(IC2原版: 500 EU/t)
    public static final int NANOSABER_CHARGE_RATE = 500;
    // 纳米剑等�?(IC2原版: 3 - HV)
    public static final int NANOSABER_TIER = 3;
    // 纳米剑每次使用消耗?(IC2原版: 100 EU)
    public static final int NANOSABER_ENERGY_PER_USE = 100;

    // 纳米剑攻击消耗?(IC2原版: 400 EU)
    public static final int ATTACK_COST = 400;
    // 纳米剑激活消耗?(IC2原版: 16 EU)
    public static final int ACTIVATE_COST = 16;

    // 纳米剑关闭状态伤�?(IC2原版: 4.0)
    public static final float DAMAGE_INACTIVE = 4.0F;
    // 纳米剑开启状态伤�?(IC2原版: 20.0)
    public static final float DAMAGE_ACTIVE = 20.0F;

    private static final String TAG_ACTIVE = "NanoSaberActive";

    public mio_icif_nanosaber(Properties properties) {
        super(properties, NANOSABER_MAX_ENERGY, NANOSABER_MAX_ENERGY, "nanosaber", NANOSABER_CHARGE_RATE, NANOSABER_ENERGY_PER_USE, NANOSABER_TIER);
    }

    public static boolean isActive(ItemStack stack) {
        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        if (customData != null && !customData.isEmpty()) {
            return customData.copyTag().getBoolean(TAG_ACTIVE);
        }
        return false;
    }

    public static void setActive(ItemStack stack, boolean active) {
        CustomData customData = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
        CompoundTag tag = customData.copyTag();
        tag.putBoolean(TAG_ACTIVE, active);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }

    public static Component getStatusName(ItemStack stack) {
        return isActive(stack)
            ? Component.translatable("hud.mio_icif.nanosaber.mode_active")
            : Component.translatable("hud.mio_icif.nanosaber.mode_inactive");
    }

    @Override
    public void toggleActive(ItemStack stack, Player player) {
        boolean currentActive = isActive(stack);
        if (currentActive) {
            setActive(stack, false);
            player.sendSystemMessage(Component.translatable("message.mio_icif.nanosaber.deactivated"));
        } else {
            if (hasEnoughEnergy(stack, ACTIVATE_COST)) {
                setActive(stack, true);
                player.sendSystemMessage(Component.translatable("message.mio_icif.nanosaber.activated"));
            } else {
                player.sendSystemMessage(Component.translatable("message.mio_icif.nanosaber.no_energy"));
            }
        }
    }

    @Override
    public float getAttackDamageBonus(Entity target, float damage, DamageSource damageSource) {
        Entity directEntity = damageSource.getDirectEntity();
        if (directEntity instanceof LivingEntity living) {
            return isActive(living.getMainHandItem()) ? DAMAGE_ACTIVE : DAMAGE_INACTIVE;
        }
        return DAMAGE_INACTIVE;
    }

    @Override
    public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        if (!attacker.level().isClientSide && isActive(stack)) {
            if (!consumeEnergy(stack, ATTACK_COST)) {
                setActive(stack, false);
            }
        }
        return true;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        boolean active = isActive(stack);
        if (active) {
            tooltip.add(Component.translatable("tooltip.mio_icif.nanosaber.active"));
        } else {
            tooltip.add(Component.translatable("tooltip.mio_icif.nanosaber.inactive"));
        }
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slotId, boolean isSelected) {
        super.inventoryTick(stack, level, entity, slotId, isSelected);

        if (!level.isClientSide && isSelected && entity instanceof Player player) {
            long currentEnergy = getEnergy(stack);
            Component statusName = getStatusName(stack);
            player.displayClientMessage(
                Component.translatable("hud.mio_icif.nanosaber.display",
                    statusName, currentEnergy, getMaxEnergy()),
                true
            );
        }
    }

    @Override
    public boolean isEnchantable(ItemStack stack) {
        return true;
    }

    @Override
    public int getEnchantmentValue() {
        return 15;
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.BLOCK;
    }

    @Override
    public boolean shouldCauseReequipAnimation(ItemStack oldStack, ItemStack newStack, boolean slotChanged) {
        return slotChanged || isActive(oldStack) != isActive(newStack);
    }
}