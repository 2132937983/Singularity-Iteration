package com.singularity_iteration.mio_icif.Items.Tools;

import com.singularity_iteration.mio_icif.api.item.IWindMeterItem;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

@SuppressWarnings("null")
public class mio_icif_windmeter extends mio_icif_tool_elc implements IWindMeterItem {

    private static final int ENERGY_PER_USE = 50;

    public mio_icif_windmeter(Properties properties) {
        super(properties, 10000, 10000, "item_tool_windmeter", 100, ENERGY_PER_USE, 1);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (level.isClientSide) {
            return InteractionResultHolder.success(stack);
        }

        if (!hasEnoughEnergy(stack, ENERGY_PER_USE)) {
            player.displayClientMessage(
                Component.translatable("message.mio_icif.windmeter.no_energy")
                    .withStyle(ChatFormatting.RED),
                true
            );
            return InteractionResultHolder.fail(stack);
        }

        consumeEnergy(stack, ENERGY_PER_USE);

        double windStrength = calculateWindStrength(level, player);
        String formatted = String.format("%.2f", windStrength);

        player.sendSystemMessage(
            Component.translatable("message.mio_icif.windmeter.info", formatted)
                .withStyle(ChatFormatting.AQUA)
        );

        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    private double calculateWindStrength(Level level, Player player) {
        int y = player.blockPosition().getY();

        int heightBonus = Math.max(0, (y - 64) / 2);
        heightBonus = Math.min(heightBonus, 93);

        int weatherBonus = 0;
        if (level.isThundering()) {
            weatherBonus = 35;
        } else if (level.isRaining()) {
            weatherBonus = 15;
        }

        java.util.Random random = new java.util.Random();
        int baseFluctuation = random.nextInt(11) - 5;
        int baseWind = Math.max(5, Math.min(15, 10 + baseFluctuation));

        int totalWind = baseWind + heightBonus + weatherBonus;
        totalWind = Math.min(130, totalWind);

        return (double) totalWind;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);
        tooltipComponents.add(
            Component.translatable("tooltip.mio_icif.windmeter.usage")
                .withStyle(ChatFormatting.GRAY)
        );
    }
}

