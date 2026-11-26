package com.miophas.singularity_iteration.common.block.energy_converter;

import com.miophas.singularity_iteration.common.blockentity.energy_converter.mio_icif_energy_converter_entity;
import com.miophas.singularity_iteration.common.block.mio_icif_entity_block;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
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
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * 0.1.11.1: the grid-tie interface (the Energy Converter until 0.1.11.0; same id). The front faces the network of
 * another mod, the other faces join the SI grid. Sneak + right-click cycles the mode (export, import, bidirectional
 * 10 / 25 / 50 %), right-click cycles the limit (32, 128, 512, 2,048 EU/t). See {@link mio_icif_energy_converter_entity}.
 * 0.1.11.216: the model shows both (SCXSYGL's models): the faces that send energy are lit, the faces that take it dark,
 * and the colour is the limit (grey 32, red 128, yellow 512, cyan 2,048 EU/t).
 */
@SuppressWarnings("null")
public class mio_icif_block_energy_converter extends mio_icif_entity_block {

    /**
     * 0.1.11.216: the mode (which faces of the model are lit). Until 0.1.11.215 a flag (true for import and
     * bidirectional); a saved flag reads as the default and the interface sets the state on its first tick.
     */
    public static final EnumProperty<mio_icif_energy_converter_entity.Mode> MODE =
        EnumProperty.create("mode", mio_icif_energy_converter_entity.Mode.class);
    /** 0.1.11.216: the limit as an index of {@link mio_icif_energy_converter_entity#LIMITS} (the colour of the model). */
    public static final IntegerProperty LIMIT = IntegerProperty.create("limit", 0, mio_icif_energy_converter_entity.LIMITS.length - 1);
    /** All six directions: the front faces the other network. */
    public static final DirectionProperty FACING = BlockStateProperties.FACING;

    public static final MapCodec<mio_icif_block_energy_converter> CODEC = RecordCodecBuilder.mapCodec(instance ->
        instance.group(propertiesCodec()).apply(instance, mio_icif_block_energy_converter::new));

    public mio_icif_block_energy_converter(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any()
            .setValue(FACING, Direction.NORTH)
            .setValue(MODE, mio_icif_energy_converter_entity.Mode.EXPORT)
            .setValue(LIMIT, mio_icif_energy_converter_entity.LIMITS.length - 1));   // the defaults of the interface
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, MODE, LIMIT);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        // the front faces away from the player (like a dispenser): place it against the other network
        return this.defaultBlockState().setValue(FACING, context.getNearestLookingDirection().getOpposite());
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (level.isClientSide) return InteractionResult.SUCCESS;
        if (!(level.getBlockEntity(pos) instanceof mio_icif_energy_converter_entity entity)) return InteractionResult.PASS;
        Component message;
        if (player.isShiftKeyDown()) {
            entity.cycleMode();
            message = switch (entity.mode()) {
                case EXPORT -> Component.translatable("message.mio_icif.energy_converter.mode_export");
                case IMPORT -> Component.translatable("message.mio_icif.energy_converter.mode_import");
                case BIDIRECTIONAL -> Component.translatable("message.mio_icif.energy_converter.mode_bidirectional", entity.band());
            };
        } else {
            entity.cycleLimit();
            message = Component.translatable("message.mio_icif.energy_converter.limit", entity.limit());
        }
        if (player instanceof ServerPlayer serverPlayer) serverPlayer.displayClientMessage(message, true);
        return InteractionResult.CONSUME;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new mio_icif_energy_converter_entity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state,
                                                                   BlockEntityType<T> type) {
        if (level.isClientSide) return null;
        return (lvl, p, st, blockEntity) -> {
            if (blockEntity instanceof mio_icif_energy_converter_entity entity) entity.tick();
        };
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip,
                                 TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        tooltip.add(Component.translatable("tooltip.mio_icif.energy_converter.desc"));
        tooltip.add(Component.translatable("tooltip.mio_icif.energy_converter.sneak_to_switch"));
        tooltip.add(Component.translatable("tooltip.mio_icif.energy_converter.limit"));
    }
}
