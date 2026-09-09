package com.singularity_iteration.mio_icif.Items.Tools;

import com.singularity_iteration.mio_icif.api.item.IFishingRodItem;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * 电动鱼竿
 * 使用电力代替耐久度进行钓鱼
 */
@SuppressWarnings("null")
public class mio_icif_electric_fishing_rod extends mio_icif_tool_elc implements IFishingRodItem {

    private static final int MAX_ENERGY = 12000;
    private static final int TRANSFER_SPEED = 32;
    private static final int ENERGY_COST = 100;

    public mio_icif_electric_fishing_rod() {
        super(new Item.Properties(), MAX_ENERGY, MAX_ENERGY, "item_electric_fishing_rod", TRANSFER_SPEED, ENERGY_COST, 1);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (!hasEnoughEnergy(stack, ENERGY_COST)) {
            return InteractionResultHolder.fail(stack);
        }

        if (player.fishing != null) {
            // 收杆
            if (!level.isClientSide) {
                player.fishing.retrieve(stack);
                extractEnergy(stack, ENERGY_COST);
            }

            level.playSound(
                null,
                player.getX(),
                player.getY(),
                player.getZ(),
                SoundEvents.FISHING_BOBBER_RETRIEVE,
                SoundSource.NEUTRAL,
                1.0F,
                0.4F / (level.getRandom().nextFloat() * 0.4F + 0.8F)
            );
            player.gameEvent(net.minecraft.world.level.gameevent.GameEvent.ITEM_INTERACT_FINISH);
        } else {
            // 抛竿
            level.playSound(
                null,
                player.getX(),
                player.getY(),
                player.getZ(),
                SoundEvents.FISHING_BOBBER_THROW,
                SoundSource.NEUTRAL,
                0.5F,
                0.4F / (level.getRandom().nextFloat() * 0.4F + 0.8F)
            );
            if (level instanceof ServerLevel serverlevel) {
                int j = (int)(EnchantmentHelper.getFishingTimeReduction(serverlevel, stack, player) * 20.0F);
                int k = EnchantmentHelper.getFishingLuckBonus(serverlevel, stack, player);
                level.addFreshEntity(new FishingHook(player, level, k, j));
            }

            player.awardStat(Stats.ITEM_USED.get(this));
            player.gameEvent(net.minecraft.world.level.gameevent.GameEvent.ITEM_INTERACT_START);
        }

        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    @Override
    public boolean isEnchantable(ItemStack stack) {
        return true;
    }

    @Override
    public int getEnchantmentValue() {
        return 3;
    }

    @Override
    public boolean canPerformAction(ItemStack stack, net.neoforged.neoforge.common.ItemAbility itemAbility) {
        return net.neoforged.neoforge.common.ItemAbilities.DEFAULT_FISHING_ROD_ACTIONS.contains(itemAbility);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);
        tooltipComponents.add(Component.translatable("tooltip.mio_icif.electric_fishing_rod.desc"));
    }

    public boolean hasEnoughEnergy(ItemStack stack, int amount) {
        return getEnergy(stack) >= amount;
    }
}
