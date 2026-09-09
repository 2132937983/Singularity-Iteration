package com.singularity_iteration.mio_icif.Items.Tools;

import com.singularity_iteration.mio_icif.api.item.ILighterItem;

import com.singularity_iteration.mio_icif.Blocks.mio_icif_blocks;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

/**
 * 电力光源产生器
 * 右键点击方块侧面放置电力光源方块
 * 对齐原版打火石逻辑
 */
@SuppressWarnings("null")
public class mio_icif_electric_lighter extends mio_icif_tool_elc implements ILighterItem {

    private static final int MAX_ENERGY = 200000;
    private static final int TRANSFER_SPEED = 128;
    private static final int ENERGY_COST = 500;

    public mio_icif_electric_lighter() {
        super(new Item.Properties(), MAX_ENERGY, MAX_ENERGY, "item_electric_lighter", TRANSFER_SPEED, ENERGY_COST, 1);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        Player player = context.getPlayer();
        ItemStack stack = context.getItemInHand();

        if (player == null) {
            return InteractionResult.FAIL;
        }

        if (context.getClickedFace() == null) {
            return InteractionResult.FAIL;
        }

        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }

        if (getEnergy(stack) < ENERGY_COST) {
            player.sendSystemMessage(Component.translatable("message.mio_icif.electric_lighter.no_energy"));
            return InteractionResult.FAIL;
        }

        BlockPos placePos = pos;
        BlockState clickedState = level.getBlockState(pos);
        if (!clickedState.canBeReplaced()) {
            placePos = pos.relative(context.getClickedFace());
        }

        if (!player.mayUseItemAt(placePos, context.getClickedFace(), stack)) {
            return InteractionResult.FAIL;
        }

        BlockState placeState = level.getBlockState(placePos);
        if (!placeState.canBeReplaced()) {
            return InteractionResult.FAIL;
        }

        BlockState lightState = mio_icif_blocks.ELECTRIC_LIGHT.get().defaultBlockState();
        if (!lightState.canSurvive(level, placePos)) {
            return InteractionResult.FAIL;
        }

        if (level.setBlockAndUpdate(placePos, lightState)) {
            extractEnergy(stack, ENERGY_COST);
            level.playSound(player, pos, SoundEvents.FLINTANDSTEEL_USE, SoundSource.BLOCKS, 1.0F, 0.5F);
            return InteractionResult.SUCCESS;
        }

        return InteractionResult.FAIL;
    }

    @Override
    public boolean canPerformAction(ItemStack stack, net.neoforged.neoforge.common.ItemAbility itemAbility) {
        return true;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);
        tooltipComponents.add(Component.translatable("tooltip.mio_icif.electric_lighter.desc"));
    }
}
