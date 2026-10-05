package com.miophas.singularity_iteration.common.item.tools;

import com.miophas.singularity_iteration.core.api.item.ILighterItem;

import com.miophas.singularity_iteration.common.registry.mio_icif_blocks;
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
        if (player.isShiftKeyDown()) {
            clearArea(level, player);
            return InteractionResult.SUCCESS;
        }

        if (getEnergy(stack) < ENERGY_COST) {
            player.sendSystemMessage(Component.translatable("message.mio_icif.electric_lighter.no_energy"));
            return InteractionResult.FAIL;
        }

        BlockState clickedState = level.getBlockState(pos);
        // clicking an existing electric light switches it off (removes it)
        if (clickedState.is(mio_icif_blocks.ELECTRIC_LIGHT.get())) {
            level.removeBlock(pos, false);
            level.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_BREAK, SoundSource.BLOCKS, 0.8F, 1.6F);
            return InteractionResult.SUCCESS;
        }
        BlockPos placePos = pos;
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
            extractEnergy(stack, com.miophas.singularity_iteration.core.api.item.EnergySaving.apply(stack, ENERGY_COST));
            level.playSound(player, pos, SoundEvents.FLINTANDSTEEL_USE, SoundSource.BLOCKS, 1.0F, 0.5F);
            return InteractionResult.SUCCESS;
        }

        return InteractionResult.FAIL;
    }

    public static final int CLEAR_RADIUS = 8;

    /** Shift + right-click (on a block or in the air): removes every electric light within 8 blocks. */
    @Override
    public net.minecraft.world.InteractionResultHolder<ItemStack> use(Level level, Player player, net.minecraft.world.InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!player.isShiftKeyDown()) return net.minecraft.world.InteractionResultHolder.pass(stack);
        if (!level.isClientSide) clearArea(level, player);
        return net.minecraft.world.InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    public static int clearArea(Level level, Player player) {
        BlockPos center = player.blockPosition();
        int removed = 0;
        for (BlockPos p : BlockPos.betweenClosed(center.offset(-CLEAR_RADIUS, -CLEAR_RADIUS, -CLEAR_RADIUS),
                center.offset(CLEAR_RADIUS, CLEAR_RADIUS, CLEAR_RADIUS))) {
            if (level.getBlockState(p).is(mio_icif_blocks.ELECTRIC_LIGHT.get()) && player.mayUseItemAt(p, net.minecraft.core.Direction.UP, ItemStack.EMPTY)) {
                level.removeBlock(p, false);
                removed++;
            }
        }
        level.playSound(null, center, SoundEvents.BEACON_DEACTIVATE, SoundSource.PLAYERS, 0.6F, 1.6F);
        player.displayClientMessage(Component.translatable("message.mio_icif.electric_lighter.cleared", removed), true);
        return removed;
    }

    @Override
    public boolean canPerformAction(ItemStack stack, net.neoforged.neoforge.common.ItemAbility itemAbility) {
        return true;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);
        tooltipComponents.add(Component.translatable("tooltip.mio_icif.electric_lighter.desc"));
        tooltipComponents.add(Component.translatable("tooltip.mio_icif.electric_lighter.remove").withStyle(net.minecraft.ChatFormatting.GRAY));
    }
}
