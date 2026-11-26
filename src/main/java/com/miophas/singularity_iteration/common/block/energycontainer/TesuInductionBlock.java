package com.miophas.singularity_iteration.common.block.energycontainer;

import com.miophas.singularity_iteration.common.blockentity.batbox.TesuInductionPortBlockEntity;
import com.miophas.singularity_iteration.common.blockentity.batbox.TesuWireless;
import com.mojang.serialization.MapCodec;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

/**
 * 0.1.11.206: the TESU wireless induction port: a wireless output port (relays link to it, the connector's click goes to the
 * connector, removal lets the controller go) that also charges players and machines without the wireless upgrade near it
 * ({@link TesuInductionPortBlockEntity}).
 */
@SuppressWarnings("null")
public class TesuInductionBlock extends TesuWirelessBlock {
    public static final MapCodec<TesuInductionBlock> INDUCTION_CODEC = simpleCodec(TesuInductionBlock::new);

    public TesuInductionBlock(Properties properties) { super(properties); }

    @Override protected MapCodec<? extends BaseEntityBlock> codec() { return INDUCTION_CODEC; }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new TesuInductionPortBlockEntity(pos, state); }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide()) return null;
        return (lvl, pos, blockState, be) -> {
            if (be instanceof TesuInductionPortBlockEntity port) TesuInductionPortBlockEntity.tick(lvl, port);
        };
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.isClientSide) return InteractionResult.SUCCESS;
        if (!(level.getBlockEntity(pos) instanceof TesuInductionPortBlockEntity port)) return InteractionResult.PASS;
        player.displayClientMessage(Component.translatable(port.controller() != null ? "message.mio_icif.tesu.induction.port_on"
            : "message.mio_icif.tesu.induction.port_off", TesuWireless.RANGE), true);
        return InteractionResult.CONSUME;
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.mio_icif.tesu.induction_port", TesuWireless.RANGE).withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.mio_icif.tesu.port_slot").withStyle(ChatFormatting.YELLOW));
    }
}
