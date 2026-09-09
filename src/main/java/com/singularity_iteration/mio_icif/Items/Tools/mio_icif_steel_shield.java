package com.singularity_iteration.mio_icif.Items.Tools;

import com.singularity_iteration.mio_icif.api.item.IShieldItem;

import net.minecraft.network.chat.Component;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.common.ItemAbilities;

import java.util.List;

@SuppressWarnings("null")
public class mio_icif_steel_shield extends mio_icif_tool_elc implements net.minecraft.world.item.Equipable, IShieldItem {
    public static final int STEEL_SHIELD_MAX_ENERGY = 50000;
    public static final int STEEL_SHIELD_CHARGE_RATE = 128;
    public static final int STEEL_SHIELD_TIER = 2;
    public static final int STEEL_SHIELD_ENERGY_PER_USE = 100;
    public static final int BLOCK_COST = 100;

    public mio_icif_steel_shield(Properties properties) {
        super(properties, STEEL_SHIELD_MAX_ENERGY, STEEL_SHIELD_MAX_ENERGY, "steel_shield", STEEL_SHIELD_CHARGE_RATE, STEEL_SHIELD_ENERGY_PER_USE, STEEL_SHIELD_TIER);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!hasEnoughEnergy(stack, BLOCK_COST)) {
            if (!level.isClientSide) {
                player.sendSystemMessage(Component.translatable("message.mio_icif.steel_shield.no_energy"));
            }
            return InteractionResultHolder.pass(stack);
        }
        player.startUsingItem(hand);
        return InteractionResultHolder.success(stack);
    }

    @Override
    public void releaseUsing(ItemStack stack, Level level, LivingEntity entity, int timeLeft) {
        // 停止举盾时消耗能量
        if (!level.isClientSide && entity instanceof Player) {
            if (hasEnoughEnergy(stack, BLOCK_COST)) {
                consumeEnergy(stack, BLOCK_COST);
            }
        }
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return 72000;
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.BLOCK;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        tooltip.add(Component.translatable("tooltip.mio_icif.steel_shield.energy", getEnergy(stack), STEEL_SHIELD_MAX_ENERGY));
    }

    @Override
    public boolean isValidRepairItem(ItemStack toRepair, ItemStack repair) {
        return repair.is(ItemTags.PLANKS) || super.isValidRepairItem(toRepair, repair);
    }

    public boolean isShield(ItemStack stack, net.minecraft.world.entity.LivingEntity entity) {
        return true;
    }

    @Override
    public EquipmentSlot getEquipmentSlot() {
        return EquipmentSlot.OFFHAND;
    }

    @Override
    public boolean canPerformAction(ItemStack stack, net.neoforged.neoforge.common.ItemAbility itemAbility) {
        return ItemAbilities.DEFAULT_SHIELD_ACTIONS.contains(itemAbility);
    }
}