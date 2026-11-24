package com.miophas.singularity_iteration.common.item.tools;

import com.miophas.singularity_iteration.common.blockentity.kuentity.kugenerator.mio_icif_Wind_Kinetic_Generator;
import com.miophas.singularity_iteration.core.api.item.IWindMeterItem;
import com.miophas.singularity_iteration.core.runtime.world.WindSim;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

import java.util.List;

@SuppressWarnings("null")
public class mio_icif_windmeter extends mio_icif_tool_elc implements IWindMeterItem {

    private static final int ENERGY_PER_USE = 50;

    public mio_icif_windmeter(Properties properties) {
        super(properties, 10000, 0L, "item_tool_windmeter", 100, ENERGY_PER_USE, 1);
    }

    /**
     * 右键方块：对准风力动能发生机时，报告其所在位置的有效风力
     * （含全局风场、高度系数与转子遮挡衰减，对应 IC2 风力计的 onItemUseFirst）；
     * 对准其它方块时返回 PASS，随后回落到手持测风（use）。
     */
    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        // 0.1.11.200 (T5c, U-08): both sides decide on the block first. The client answered SUCCESS for any block, so a
        // click on a block that is no wind generator never fell back to the hand-held reading.
        if (!(level.getBlockEntity(pos) instanceof mio_icif_Wind_Kinetic_Generator generator)) {
            return InteractionResult.PASS;
        }
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }

        Player player = context.getPlayer();
        ItemStack stack = context.getItemInHand();
        if (player == null) return InteractionResult.PASS;

        if (!hasEnoughEnergy(stack, ENERGY_PER_USE)) {
            player.displayClientMessage(Component.translatable("message.mio_icif.windmeter.no_energy").withStyle(ChatFormatting.RED), true);
            return InteractionResult.FAIL;
        }
        // 0.1.11.200 (T5c): the reading the multimeter gives too; charged only when there was something to read
        if (MeterReadings.windOf(player, generator)) consumeEnergy(stack, ENERGY_PER_USE);
        return InteractionResult.SUCCESS;
    }
    /**
     * 手持右键空气：测量玩家当前位置的全局风场风力
     * （对应 IC2 风力计的 itemUse：windSim.getWindAt(玩家高度)）。
     */
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

        if (consumeEnergy(stack, ENERGY_PER_USE)) MeterReadings.windHere(player);

        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
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
