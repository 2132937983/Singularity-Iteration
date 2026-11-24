package com.miophas.singularity_iteration.common.item.tools;

import com.miophas.singularity_iteration.core.api.item.IShieldItem;

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
    /** EU needed to raise the shield. */
    public static final int BLOCK_COST = 100;
    /** 0.1.11.197 (T4, D5): EU per point of damage the shield stops; it used to block without limit. */
    public static final int EU_PER_BLOCKED_DAMAGE = 50;
    /** 0.1.11.197 (T4, D5): a flat shield cannot be raised for this long, as a shield struck by an axe. */
    public static final int FLAT_COOLDOWN = 100;

    public mio_icif_steel_shield(Properties properties) {
        super(properties, STEEL_SHIELD_MAX_ENERGY, 0L, "steel_shield", STEEL_SHIELD_CHARGE_RATE, STEEL_SHIELD_ENERGY_PER_USE, STEEL_SHIELD_TIER);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!hasEnoughEnergy(stack, BLOCK_COST)) {
            if (!level.isClientSide) {
                player.displayClientMessage(Component.translatable("message.mio_icif.steel_shield.no_energy"), true);
            }
            return InteractionResultHolder.pass(stack);
        }
        player.startUsingItem(hand);
        return InteractionResultHolder.success(stack);
    }

    /**
     * 0.1.11.197 (T4, D5): a blocked hit costs {@link #EU_PER_BLOCKED_DAMAGE} EU a point. The shield used to charge 100 EU
     * only when it was lowered, so it blocked without limit. A shield that cannot pay lets the hit through and cannot be
     * raised for {@link #FLAT_COOLDOWN} ticks. Called from {@code LivingShieldBlockEvent}.
     */
    public static void onShieldBlock(net.neoforged.neoforge.event.entity.living.LivingShieldBlockEvent event) {
        LivingEntity blocker = event.getEntity();
        if (!event.getBlocked() || blocker.level().isClientSide) return;
        ItemStack stack = blocker.getUseItem();
        if (!(stack.getItem() instanceof mio_icif_steel_shield shield)) return;
        long cost = (long) Math.ceil(event.getBlockedDamage()) * EU_PER_BLOCKED_DAMAGE;
        event.setShieldDamage(0);
        if (cost <= 0 || shield.consumeEnergy(stack, cost)) return;
        event.setBlocked(false);
        blocker.stopUsingItem();
        if (blocker instanceof Player player) {
            player.getCooldowns().addCooldown(shield, FLAT_COOLDOWN);
            player.displayClientMessage(Component.translatable("message.mio_icif.steel_shield.no_energy"), true);
        }
        blocker.level().playSound(null, blocker.getX(), blocker.getY(), blocker.getZ(),
            net.minecraft.sounds.SoundEvents.SHIELD_BREAK, net.minecraft.sounds.SoundSource.PLAYERS, 0.8F, 0.8F);
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