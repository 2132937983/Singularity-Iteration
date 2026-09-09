package com.singularity_iteration.mio_icif.Blocks.OilRig;

import com.singularity_iteration.mio_icif.Blocks.entity.oilrig.mio_icif_oil_rig_input_entity;
import com.singularity_iteration.mio_icif.api.MioIcifAPI;
import com.singularity_iteration.mio_icif.api.energy.ICableTier;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
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
import com.mojang.serialization.MapCodec;
import org.jetbrains.annotations.Nullable;

import java.util.List;

@SuppressWarnings("null")
public class mio_icif_block_oil_rig_input extends mio_icif_Block_OilRig_Multiblock_Base {

    private static final long CAPACITY = 50000L;
    private static final ICableTier TIER = MioIcifAPI.instance().getEnergyNetAPI().getCableTier("hv");
    private static final long INPUT_RATE = TIER.getPowerRating();

    public static final MapCodec<mio_icif_block_oil_rig_input> CODEC = simpleCodec(mio_icif_block_oil_rig_input::new);
    public static final DirectionProperty FACING = DirectionalBlock.FACING;

    public mio_icif_block_oil_rig_input(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction direction = context.getNearestLookingDirection().getOpposite();
        return this.defaultBlockState().setValue(FACING, direction);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new mio_icif_oil_rig_input_entity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> blockEntityType) {
        if (level.isClientSide()) {
            return null;
        }
        return (lvl, pos, blockState, blockEntity) -> {
            if (blockEntity instanceof mio_icif_oil_rig_input_entity entity) {
                mio_icif_oil_rig_input_entity.tick(lvl, pos, blockState, entity);
            }
        };
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (!level.isClientSide) {
            player.displayClientMessage(
                Component.translatable("gui.mio_icif.oil_rig.use_panel")
                    .withStyle(ChatFormatting.YELLOW), true);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected void addBlockTooltip(ItemStack stack, List<Component> tooltip) {
        tooltip.add(Component.translatable("tooltip.mio_icif.oil_rig.input_module")
                .withStyle(ChatFormatting.AQUA));
        tooltip.add(Component.translatable("tooltip.mio_icif.oil_rig.input_rate", INPUT_RATE)
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.mio_icif.oil_rig.capacity", CAPACITY)
                .withStyle(ChatFormatting.GRAY));
    }
}