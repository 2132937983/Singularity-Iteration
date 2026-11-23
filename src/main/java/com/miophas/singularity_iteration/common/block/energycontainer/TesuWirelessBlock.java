package com.miophas.singularity_iteration.common.block.energycontainer;

import com.miophas.singularity_iteration.common.blockentity.batbox.TesuControllerBlockEntity;
import com.miophas.singularity_iteration.common.blockentity.batbox.TesuWireless;
import com.miophas.singularity_iteration.common.blockentity.batbox.TesuWirelessPortBlockEntity;
import com.miophas.singularity_iteration.common.item.normal.TesuConnectorItem;
import com.mojang.serialization.MapCodec;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/**
 * 0.1.11.167: the TESU relay is its own block now (TesuRelayBlock, a pylon three blocks tall); this block is the wireless
 * output port only.
 *
 * <p>0.1.11.158: the TESU wireless output port (in a TESU port slot) and the TESU relay, one class for the two. A right click with
 * an empty hand tells the block's state (bound to a formed TESU or not; linked or not, and whether its links reach a TESU); with
 * the TESU wireless connector the click goes to the connector (it would otherwise stop at the status and never link).
 */
@SuppressWarnings("null")
public class TesuWirelessBlock extends BaseEntityBlock {
    public static final MapCodec<TesuWirelessBlock> CODEC = simpleCodec(TesuWirelessBlock::new);

    public TesuWirelessBlock(Properties properties) { super(properties); }

    @Override protected MapCodec<? extends BaseEntityBlock> codec() { return CODEC; }
    @Override protected RenderShape getRenderShape(BlockState state) { return RenderShape.MODEL; }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new TesuWirelessPortBlockEntity(pos, state); }

    /** The connector's click goes to the connector, not to the status. */
    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand,
                                              BlockHitResult hit) {
        if (stack.getItem() instanceof TesuConnectorItem) return ItemInteractionResult.SKIP_DEFAULT_BLOCK_INTERACTION;
        return super.useItemOn(stack, state, level, pos, player, hand, hit);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.isClientSide) return InteractionResult.SUCCESS;
        if (!(level.getBlockEntity(pos) instanceof TesuWirelessPortBlockEntity port)) return InteractionResult.PASS;
        player.displayClientMessage(Component.translatable(port.controller() != null ? "message.mio_icif.tesu.wireless.port_on"
            : "message.mio_icif.tesu.wireless.port_off", TesuWireless.RANGE), true);
        return InteractionResult.CONSUME;
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moved) {
        if (!level.isClientSide() && !state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof TesuWirelessPortBlockEntity port) {
            TesuControllerBlockEntity core = port.controller();
            port.bind(null);
            if (core != null) core.check();
        }
        super.onRemove(state, level, pos, newState, moved);
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        tooltip.add(Component.translatable("tooltip.mio_icif.tesu.wireless_port", TesuWireless.RANGE).withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.mio_icif.tesu.port_slot").withStyle(ChatFormatting.YELLOW));
    }
}
