package com.miophas.singularity_iteration.common.block.energycontainer;

import com.miophas.singularity_iteration.common.blockentity.batbox.GesuInductionPortBlockEntity;
import com.mojang.serialization.MapCodec;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

/**
 * 0.1.11.206: the GESU wireless induction port, a GESU module that charges the players near it ({@link GesuInductionPortBlockEntity}).
 * Any face of the core takes it, as the other modules; a click opens the core's screen.
 */
@SuppressWarnings("null")
public class mio_icif_gesu_induction_port extends mio_icif_Block_GESU_Multiblock_Base {
    public static final MapCodec<mio_icif_gesu_induction_port> CODEC = simpleCodec(mio_icif_gesu_induction_port::new);
    public static final DirectionProperty FACING = DirectionalBlock.FACING;

    public mio_icif_gesu_induction_port(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState().setValue(FACING, context.getNearestLookingDirection().getOpposite());
    }

    @Override public RenderShape getRenderShape(BlockState state) { return RenderShape.MODEL; }
    @Override protected MapCodec<? extends BaseEntityBlock> codec() { return CODEC; }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new GesuInductionPortBlockEntity(pos, state); }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide()) return null;
        return (lvl, pos, blockState, be) -> {
            if (be instanceof GesuInductionPortBlockEntity port) GesuInductionPortBlockEntity.tick(lvl, port);
        };
    }

    /** A click opens the formed core's screen, as the other modules do. */
    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof GesuInductionPortBlockEntity port) {
            if (port.core() instanceof MenuProvider core) player.openMenu(core);
            else player.displayClientMessage(Component.translatable("message.mio_icif.gesu.induction.off"), true);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected void addBlockTooltip(ItemStack stack, List<Component> tooltip) {
        tooltip.add(Component.translatable("tooltip.mio_icif.gesu.induction", GesuInductionPortBlockEntity.RANGE,
            String.format(java.util.Locale.ROOT, "%,d", GesuInductionPortBlockEntity.PER_ITEM)).withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.mio_icif.gesu.induction_module").withStyle(ChatFormatting.YELLOW));
    }
}
