package com.singularity_iteration.mio_icif.Items.Tools;

import com.singularity_iteration.mio_icif.api.tool.ToggleableElectricTool;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * 等离子射线枪 - IC2风格的高级电力武�?
 * 继承电力工具基础值?
 *
 * 特性：
 * - 蹲下+右键切换开关状态?
 * - 右键发射等离子子弹（需开启状态）
 * - 开启时纹理循环播放动画，关闭时显示静态纹�?
 * - 仅发射时消耗电力，待机不消耗?
 * - 最大存储?0000EU，传输限制?28EU/t，等�?（HV�?
 */
@SuppressWarnings("null")
public class mio_icif_plasma_launcher extends mio_icif_tool_elc implements ToggleableElectricTool {

    public static final int PLASMA_MAX_ENERGY = 40000;
    public static final int PLASMA_CHARGE_RATE = 128;
    public static final int PLASMA_TIER = 3;
    public static final int PLASMA_ENERGY_PER_USE = 100;
    public static final int ACTIVATE_COST = 16;

    private static final String TAG_ACTIVE = "PlasmaLauncherActive";

    public static EntityType<mio_icif_plasma_bullet> PLASMA_BULLET_ENTITY;

    public mio_icif_plasma_launcher(Properties properties) {
        super(properties, PLASMA_MAX_ENERGY, PLASMA_MAX_ENERGY, "plasma_launcher", PLASMA_CHARGE_RATE, PLASMA_ENERGY_PER_USE, PLASMA_TIER);
    }

    public static void setPlasmaBulletEntity(EntityType<mio_icif_plasma_bullet> entityType) {
        PLASMA_BULLET_ENTITY = entityType;
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
            ? Component.translatable("hud.mio_icif.plasma_launcher.mode_active")
            : Component.translatable("hud.mio_icif.plasma_launcher.mode_inactive");
    }

    @Override
    public void toggleActive(ItemStack stack, Player player) {
        boolean currentActive = isActive(stack);
        if (currentActive) {
            setActive(stack, false);
            player.sendSystemMessage(Component.translatable("message.mio_icif.plasma_launcher.deactivated"));
        } else {
            if (hasEnoughEnergy(stack, ACTIVATE_COST)) {
                setActive(stack, true);
                player.sendSystemMessage(Component.translatable("message.mio_icif.plasma_launcher.activated"));
            } else {
                player.sendSystemMessage(Component.translatable("message.mio_icif.plasma_launcher.no_energy"));
            }
        }
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (!isActive(stack)) {
            if (!level.isClientSide) {
                player.sendSystemMessage(Component.translatable("message.mio_icif.plasma_launcher.not_active"));
            }
            return InteractionResultHolder.fail(stack);
        }

        if (!hasEnoughEnergy(stack, PLASMA_ENERGY_PER_USE)) {
            if (!level.isClientSide) {
                player.sendSystemMessage(Component.translatable("message.mio_icif.plasma_launcher.no_energy"));
            }
            return InteractionResultHolder.fail(stack);
        }

        if (!level.isClientSide) {
            if (PLASMA_BULLET_ENTITY == null) {
                return InteractionResultHolder.fail(stack);
            }

            consumeEnergy(stack, PLASMA_ENERGY_PER_USE);

            mio_icif_plasma_bullet bullet = new mio_icif_plasma_bullet(PLASMA_BULLET_ENTITY, player, level);
            bullet.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0F, (float) mio_icif_plasma_bullet.PLASMA_SPEED, 0.0F);
            level.addFreshEntity(bullet);

            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.FIREWORK_ROCKET_BLAST, SoundSource.PLAYERS, 1.0F, 1.5F);
        }

        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        boolean active = isActive(stack);
        if (active) {
            tooltip.add(Component.translatable("tooltip.mio_icif.plasma_launcher.active"));
        } else {
            tooltip.add(Component.translatable("tooltip.mio_icif.plasma_launcher.inactive"));
        }
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slotId, boolean isSelected) {
        super.inventoryTick(stack, level, entity, slotId, isSelected);

        if (!level.isClientSide && isSelected && entity instanceof Player player) {
            long currentEnergy = getEnergy(stack);
            Component statusName = getStatusName(stack);
            player.displayClientMessage(
                Component.translatable("hud.mio_icif.plasma_launcher.display",
                    statusName, currentEnergy, getMaxEnergy()),
                true
            );
        }
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.BOW;
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
    public boolean shouldCauseReequipAnimation(ItemStack oldStack, ItemStack newStack, boolean slotChanged) {
        return slotChanged || isActive(oldStack) != isActive(newStack);
    }
}