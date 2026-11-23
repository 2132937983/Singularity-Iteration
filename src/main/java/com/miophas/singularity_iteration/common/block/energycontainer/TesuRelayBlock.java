package com.miophas.singularity_iteration.common.block.energycontainer;

import com.miophas.singularity_iteration.common.blockentity.batbox.TesuRelayBlockEntity;
import com.miophas.singularity_iteration.common.blockentity.batbox.TesuWireless;
import com.miophas.singularity_iteration.common.item.normal.TesuConnectorItem;
import com.mojang.serialization.MapCodec;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

/**
 * 0.1.11.167: the TESU relay as a pylon three blocks tall. This block is its base (a full block, with the relay's block entity,
 * its item and its drop); the shaft and the head above are the relay column ({@link TesuRelayColumnBlock}), placed with it and
 * taken with it. It needs the two blocks above free to be placed; a relay placed before 0.1.11.167 grows its column when it first
 * ticks, if the two blocks above are free. It works as before, without power of its own: it lights (powered) while its links reach
 * a formed TESU, and the particle stream runs between the emitters at the pylons' tops ({@link #EMITTER_Y}).
 *
 * <p>From 0.1.11.158 (it was a {@code TesuWirelessBlock}): a right click with an empty hand tells its state (linked or not, and
 * whether its links reach a TESU); with the TESU wireless connector the click goes to the connector.
 */
@SuppressWarnings("null")
public class TesuRelayBlock extends BaseEntityBlock {
    public static final MapCodec<TesuRelayBlock> CODEC = simpleCodec(TesuRelayBlock::new);
    public static final BooleanProperty POWERED = BlockStateProperties.POWERED;
    /** The emitter's middle, in blocks above the relay (tools/tesu_relay_pillar_111167.py). */
    public static final double EMITTER_Y = 2.953125;

    public TesuRelayBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(POWERED, false));
    }

    @Override protected MapCodec<? extends BaseEntityBlock> codec() { return CODEC; }
    @Override protected RenderShape getRenderShape(BlockState state) { return RenderShape.MODEL; }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(POWERED);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new TesuRelayBlockEntity(pos, state); }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide()) return null;
        return (l, p, s, be) -> { if (be instanceof TesuRelayBlockEntity r) TesuRelayBlockEntity.tick(l, p, s, r); };
    }

    /** The pylon needs the two blocks above free (and inside the world). */
    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        if (pos.getY() + 2 >= level.getMaxBuildHeight()) return null;
        if (!level.getBlockState(pos.above()).canBeReplaced(context) || !level.getBlockState(pos.above(2)).canBeReplaced(context)) return null;
        return defaultBlockState();
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        TesuRelayColumnBlock.place(level, pos, state.getValue(POWERED));
    }

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
        if (!(level.getBlockEntity(pos) instanceof TesuRelayBlockEntity r)) return InteractionResult.PASS;
        BlockPos p = r.parent();
        Component status = p == null ? Component.translatable("message.mio_icif.tesu.wireless.relay_unlinked")
            : Component.translatable(r.feed() != null ? "message.mio_icif.tesu.wireless.relay_on" : "message.mio_icif.tesu.wireless.relay_dead",
                p.getX(), p.getY(), p.getZ(), TesuWireless.RANGE);
        player.displayClientMessage(status, true);
        return InteractionResult.CONSUME;
    }

    /** The column goes with the relay. */
    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moved) {
        if (!level.isClientSide() && !state.is(newState.getBlock())) TesuRelayColumnBlock.clear(level, pos);
        super.onRemove(state, level, pos, newState, moved);
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        tooltip.add(Component.translatable("tooltip.mio_icif.tesu.relay", TesuWireless.RANGE).withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.mio_icif.tesu.relay.height").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.mio_icif.tesu.relay.link").withStyle(ChatFormatting.YELLOW));
    }
}
